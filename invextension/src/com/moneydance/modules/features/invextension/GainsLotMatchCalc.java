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


import com.infinitekind.moneydance.model.CurrencyType;
import com.infinitekind.moneydance.model.SplitTxn;
import com.infinitekind.moneydance.model.TxnUtil;

import java.util.Hashtable;
import java.util.LinkedHashMap;

/**
 * Implementation of Lot Matching Method
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */
public final class GainsLotMatchCalc implements GainsCalc {
    private static final double positionThreshold = 0.00001;
    TransactionValues currentTransValues;
    TransactionValues prevTransValues;
    long adjPrevPos;



    public GainsLotMatchCalc() {
    }

    /**
     * Split adjust prior TransValue with respect to current trans value
     *
     * @param thisTrans  current transaction
     * @param prevTransValues prior transaction
     * @return split adjust factor as ratio
     */
    public static Double getSplitAdjust(TransactionValues thisTrans,
                                        TransactionValues prevTransValues) {
        double splitAdjust = calcSplitAdjust(thisTrans, prevTransValues);
        return prevTransValues == null ? 0.0 : splitAdjust;

    }

    private static double calcSplitAdjust(TransactionValues thisTrans, TransactionValues prevTransValues) {
        int currentDateInt = thisTrans.getParentTxn().getDateInt();
        CurrencyType cur = thisTrans.getReferenceAccount().getCurrencyType();
        double currentRate = cur.getRate(null, currentDateInt);
        int prevDateInt = prevTransValues == null ? Integer.MIN_VALUE
                : prevTransValues.getParentTxn().getDateInt();
        return cur.adjustRateForSplitsInt( prevDateInt, currentRate, currentDateInt) / currentRate;
    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.GainsCalc#getLongBasis()
     */
    @Override
    public long getLongBasis() {
        if (currentTransValues.getPosition() <= positionThreshold) {// position short or closed
            return 0;
        } else if (currentTransValues.getPosition() > (prevTransValues == null ? 0 : adjPrevPos)) {
            // first trans or subsequent larger position
            // add current buy to previous long basis
            return -currentTransValues.getBuy()
                    - currentTransValues.getCommission()
                    - currentTransValues.getExpense()
                    + (prevTransValues == null ? 0
                    : prevTransValues.getLongBasis());
        } else if (currentTransValues.getPosition() < (prevTransValues == null ? 0 : adjPrevPos)) { // subsequent pos smaller than previous
            // implies prev long basis must exist
            double wtAvgUnitCost;
            Hashtable<String, Long> matchTable = getLotMatchTable();
            if (matchTable == null) {//use average cost
                wtAvgUnitCost = ((double)prevTransValues.getLongBasis()) / adjPrevPos;

            } else { //use lot-weighted average cost
                wtAvgUnitCost = getWeightedCost(matchTable);
            }

            return prevTransValues.getLongBasis()
                    + Math.round(wtAvgUnitCost * currentTransValues.getSecQuantity());
        }
        else {
            return prevTransValues == null ? 0 : prevTransValues.getLongBasis();
        }
    }

    /**
     * Gets weighted average unit cost from match table
     *
     * @param thisMatchTable match table from security
     * @return weighted cost of security
     */
    private double getWeightedCost(Hashtable<String, Long> thisMatchTable) {
        double totWeightedNumerator = 0.0;
        double totalAllocatedQtyAdjust = 0.0;
        for (String allocationSplitTransId : thisMatchTable.keySet()) {
            //Transvalue associated with parent transaction number
            TransactionValues allocationTransValues = currentTransValues.getSecurityAccountWrapper()
                    .getParentTransValuesFromSplitId(allocationSplitTransId);
            //Split-adjustment for shares (adjusts previous shares to current)
            Double splitAdjust = getSplitAdjust(currentTransValues,
                    allocationTransValues);
            //Lots to include in weighted average
            Long allocationQtyAdjust = thisMatchTable.get(allocationSplitTransId);
            //add to total quantity (will use as denominator later)
            totalAllocatedQtyAdjust += allocationQtyAdjust;

            //get unit cost (transaction amt + commission divided by adjusted shares)
            long secQtyUnAdjust = allocationTransValues.getSecQuantity();
            double secQtyAdjust = secQtyUnAdjust * splitAdjust;
            Double unitCostAdjust = (-allocationTransValues.getBuy() -
                    allocationTransValues.getCommission() - allocationTransValues.getExpense())
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
        if (currentTransValues.getPosition() >= -positionThreshold) { // position long or closed
            return 0;
        } else if (currentTransValues.getPosition() < (prevTransValues == null ? 0 : adjPrevPos)) {
            // first trans or subsequent larger (more negative) position
            // add current short sale to previous short basis
            return -currentTransValues.getShortSell()
                    - currentTransValues.getCommission()
                    - currentTransValues.getExpense()
                    + (prevTransValues == null ? 0
                    : +prevTransValues.getShortBasis());
        } else if (currentTransValues.getPosition() > (prevTransValues == null ? 0 : adjPrevPos)) {
            // subsequent pos smaller (closer to 0) than previous
            // implies previous short basis must exist
            assert prevTransValues != null;
            double histAvgUnitCost = ((double)prevTransValues.getShortBasis()) / adjPrevPos;
            return (prevTransValues.getShortBasis()
                    + Math.round(histAvgUnitCost * currentTransValues.getSecQuantity()));
        } else {
            return prevTransValues == null ? 0 : prevTransValues.getShortBasis();
        }
    }

    @Override
    public void initializeGainsCalc(TransactionValues thisTrans,
                                    LinkedHashMap<String, TransactionValues> prevTranses) {
        this.currentTransValues = thisTrans;
        this.prevTransValues = prevTranses.isEmpty() ? null : prevTranses.lastEntry().getValue();
        double splitAdjust = calcSplitAdjust(thisTrans, prevTransValues);
        this.adjPrevPos = prevTransValues == null ? 0
                : Math.round(prevTransValues.getPosition() * splitAdjust);
    }

    /**
     * populates lot matching table if available in transaction.  if
     * not available, return null
     *
     * @return lot match table for security
     */
    public Hashtable<String, Long> getLotMatchTable() {
        SplitTxn securitySplit = currentTransValues.isCash() ? null
                : TxnUtil.getSecurityPart(currentTransValues.getParentTxn());
        Hashtable<String, Long> splitTable = null;
        if (securitySplit != null) {
            splitTable = TxnUtil.parseCostBasisTag(securitySplit);
    }
        if (splitTable != null && !splitTable.isEmpty()) {
            return splitTable;
        } else {
            return null;
        }
    }

}
