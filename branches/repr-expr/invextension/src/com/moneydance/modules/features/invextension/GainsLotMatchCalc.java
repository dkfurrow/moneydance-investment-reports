/*
 * GainsLotMatchCalc.java
 * Copyright (c) 2014, Dale K. Furrow
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.moneydance.modules.features.invextension;

import com.moneydance.apps.md.model.CurrencyType;
import com.moneydance.apps.md.model.ParentTxn;
import com.moneydance.apps.md.model.SplitTxn;
import com.moneydance.apps.md.model.TxnUtil;

import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Implementation of Lot Matching Method
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class GainsLotMatchCalc implements GainsCalc {
    private static final double positionThreshold = 0.00001;
    BulkSecInfo currentInfo;
    TransactionValues currentTrans;
    TransactionValues prevTransValues;
    long adjPrevPos;
    Hashtable<Long, Long> matchTable;


    public GainsLotMatchCalc() {
    }

    /**
     * Split adjust prior transvalue with respect to current trans value
     *
     * @param thisTrans  current transaction
     * @param priorTrans prior transaction
     * @return split adjust factor as ratio
     */
    public static Double getSplitAdjust(TransactionValues thisTrans,
                                        TransactionValues priorTrans) {
        int currentDateInt = thisTrans.getParentTxn().getDateInt();
        CurrencyType cur = thisTrans.getReferenceAccount().getCurrencyType();
        double currentRate = cur == null ? 1.0 : cur.getUserRateByDateInt(currentDateInt);
        int prevDateInt = priorTrans == null ? Integer.MIN_VALUE
                : priorTrans.getParentTxn().getDateInt();
        double splitAdjust = (cur == null ? 1.0 : cur.adjustRateForSplitsInt(
                prevDateInt, currentRate, currentDateInt) / currentRate);
        return priorTrans == null ? 0.0 : splitAdjust;

    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.GainsCalc#getLongBasis()
     */
    @Override
    public long getLongBasis() {
        if (currentTrans.getPosition() <= positionThreshold) {// position short or closed
            return 0;
        } else if (currentTrans.getPosition() >= (prevTransValues == null ? 0
                : adjPrevPos)) {
            // first trans or subsequent larger position
            // add current buy to previous long basis
            return -currentTrans.getBuy()
                    - currentTrans.getCommission()
                    + (prevTransValues == null ? 0
                    : prevTransValues.getLongBasis());
        } else { // subsequent pos smaller than previous
            // implies prev long basis must exist
            double wtAvgUnitCost;
            if (matchTable == null) {//use average cost
                wtAvgUnitCost = ((double)prevTransValues.getLongBasis()) / adjPrevPos;

            } else { //use lot-weighted average cost
                wtAvgUnitCost = getWeightedCost(matchTable);
            }

            return prevTransValues.getLongBasis()
                    + Math.round(wtAvgUnitCost * currentTrans.getSecQuantity());
        }
    }

    /**
     * Gets weighted average unit cost from match table
     *
     * @param thisMatchTable match table from security
     * @return weighted cost of security
     */
    private double getWeightedCost(Hashtable<Long, Long> thisMatchTable) {
        double totWeightedNumerator = 0.0;
        double totalAllocatedQtyAdjust = 0.0;
        for (Long allocationSplitTransNum : thisMatchTable.keySet()) {
            //split transaction number
            //parent transaction of associated split
            ParentTxn allocationParentTrans = currentInfo.getTransactionSet()
                    .getTxnByID(allocationSplitTransNum).getParentTxn();
            //parent transaction number
            Double allocationParentTransNum = (Long
                    .valueOf(allocationParentTrans.getTxnId()).doubleValue());
            //Transvalue associated with parent transaction number
            TransactionValues allocationTransValues = currentInfo.getSecurityTransactionValues()
                    .get(allocationParentTransNum);
            //Split-adjustment for shares (adjusts previous shares to current)
            Double splitAdjust = getSplitAdjust(currentTrans,
                    allocationTransValues);
            //Lots to include in weighted average
            Long allocationQtyAdjust = thisMatchTable.get(allocationSplitTransNum);
            //add to total quantity (will use as denominator later)
            totalAllocatedQtyAdjust += allocationQtyAdjust;

            //get unit cost (transaction amt + commission divided by adjusted shares)
            long secQtyUnAdjust = allocationTransValues.getSecQuantity();
            Double secQtyAdjust = secQtyUnAdjust * splitAdjust;
            Double unitCostAdjust = (-allocationTransValues.getBuy() - allocationTransValues.getCommission())
                    / secQtyAdjust;
            //add weight
            totWeightedNumerator += unitCostAdjust * allocationQtyAdjust;
        }
        //Divide by total adjusted shares for weighted average
        return totWeightedNumerator / totalAllocatedQtyAdjust;
    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.GainsCalc#getShortBasis()
     */
    //short basis is same as average calc--no provision in MD for short positions
    @Override
    public long getShortBasis() {
        if (currentTrans.getPosition() >= -positionThreshold) { // position long or closed
            return 0;
        } else if (currentTrans.getPosition() <= (prevTransValues == null ? 0
                : adjPrevPos)) {
            // first trans or subsequent larger (more negative) position
            // add current short sale to previous short basis
            return -currentTrans.getShortSell()
                    - currentTrans.getCommission()
                    + (prevTransValues == null ? 0
                    : +prevTransValues.getShortBasis());
        } else { // subsequent pos smaller (closer to 0) than previous
            // implies previous short basis must exist
            double histAvgUnitCost = ((double)prevTransValues.getShortBasis()) / adjPrevPos;
            return (prevTransValues.getShortBasis()
                    + Math.round(histAvgUnitCost * currentTrans.getSecQuantity()));
        }
    }

    @Override
    public void initializeGainsCalc(BulkSecInfo thisCurrentInfo,
                                    TransactionValues thisTrans, ArrayList<TransactionValues> prevTranses) {
        this.currentInfo = thisCurrentInfo;
        this.currentTrans = thisTrans;
        this.prevTransValues = prevTranses.isEmpty() ? null : prevTranses.get(prevTranses.size() - 1);

        int currentDateInt = thisTrans.getParentTxn().getDateInt();
        CurrencyType cur = thisTrans.getReferenceAccount().getCurrencyType();
        double currentRate = cur == null ? 1.0
                : cur.getUserRateByDateInt(currentDateInt);
        int prevDateInt = prevTransValues == null ? Integer.MIN_VALUE
                : prevTransValues.getParentTxn().getDateInt();
        double splitAdjust = (cur == null ? 1.0 : cur.adjustRateForSplitsInt(
                prevDateInt, currentRate, currentDateInt) / currentRate);
        this.adjPrevPos = prevTransValues == null ? 0
                : Math.round(prevTransValues.getPosition() * splitAdjust);
        this.matchTable = getLotMatchTable();
    }

    /**
     * populates lot matching table if available in transaction.  if
     * not available, return null
     *
     * @return lot match table for security
     */
    public Hashtable<Long, Long> getLotMatchTable() {
        Hashtable<Long, Long> lotMatchTable = new Hashtable<>();
        SplitTxn securitySplit = currentTrans.getReferenceAccount().getCurrencyType()
                .equals(currentInfo.getCashCurrencyWrapper().getCurrencyType()) ? null
                : TxnUtil.getSecurityPart(currentTrans.getParentTxn());
        Hashtable<String, String> splitTable = null;
        if (securitySplit != null) {
            splitTable = TxnUtil.parseCostBasisTag(securitySplit);
        }
        if (splitTable != null) {
            for (String key : splitTable.keySet()) {
                Long keyLong = Long.parseLong(key);
                Long valueLong = Long.parseLong(splitTable.get(key));
                lotMatchTable.put(keyLong, valueLong);
            }
        }
        if (lotMatchTable.size() > 0) {
            return lotMatchTable;
        } else {
            return null;
        }
    }

}
