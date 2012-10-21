/* GainsAverageCalc.java
 * Copyright 2012 ${author} . All rights reserved.
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR ANY 
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.moneydance.modules.features.invextension;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.SortedSet;

import com.moneydance.apps.md.model.CurrencyType;
import com.moneydance.apps.md.model.ParentTxn;
import com.moneydance.apps.md.model.SplitTxn;
import com.moneydance.apps.md.model.TxnUtil;

/**
 * Implementation of Average Cost Method
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class GainsLotMatchCalc implements GainsCalc {
    BulkSecInfo currentInfo;
    TransactionValues currentTrans;
    TransactionValues prevTransValues;
    double adjPrevPos;
    Hashtable<Long, Double> matchTable;
    private static final double positionThreshold = 0.00001;
    
    
    
    public GainsLotMatchCalc() {
    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.GainsCalc#getLongBasis()
     */
    @Override
    public double getLongBasis() {
	

	if (currentTrans.getPosition() <= positionThreshold) {// position short or closed
	    return 0.0;
	} else if (currentTrans.getPosition() >= (prevTransValues == null ? 0
		: adjPrevPos)) {
	    // first trans or subsequent larger position
	    // add current buy to previous long basis
	    return -currentTrans.getBuy()
		    - currentTrans.getCommision()
		    + (prevTransValues == null ? 0.0
			    : prevTransValues.getLongBasis());
	} else { // subsequent pos smaller than previous
		 // implies prev long basis must exist
	    double wtAvgUnitCost;
	    if(matchTable == null){//use average cost
		wtAvgUnitCost = prevTransValues.getLongBasis() / adjPrevPos;
		
	    } else { //use lot-weighted average cost
		wtAvgUnitCost = getWeightedCost(matchTable);
	    }
	    
	    return prevTransValues.getLongBasis() + wtAvgUnitCost
		    * currentTrans.getSecQuantity();
	}
    }

    /**Gets weighted average unit cost from match table
     * @param thisMatchTable
     * @return
     */
    private double getWeightedCost(Hashtable<Long, Double> thisMatchTable) {
	double totWeightedNumerator = 0.0;
	double totalAllocatedQtyAdjust = 0.0;
	for (Iterator<Long> iterator = thisMatchTable.keySet().iterator(); iterator
		.hasNext();) {
	    //split transaction number
	    Long allocationSplitTransNum = iterator.next();
	    //parent transaction of associated split
	    ParentTxn allocationParentTrans =  BulkSecInfo.getTransactionSet()
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
	    Double allocationQtyAdjust = thisMatchTable
		    .get(allocationSplitTransNum);
	    //add to total quantity (will use as denominator later)
	    totalAllocatedQtyAdjust += allocationQtyAdjust;
	    
	    //get unit cost (transaction amt + commission divided by adjusted shares)
	    Double secQtyUnAdjust = allocationTransValues.getSecQuantity();
	    Double secQtyAdjust = secQtyUnAdjust * splitAdjust;
	    Double unitCostAdjust = (-allocationTransValues.getBuy() - allocationTransValues.getCommision())
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
    public double getShortBasis() {
	if (currentTrans.getPosition() >= -positionThreshold) { // position long or closed
	    return 0.0;
	} else if (currentTrans.getPosition() <= (prevTransValues == null ? 0.0
		: adjPrevPos)) {
	    // first trans or subsequent larger (more negative) position
	    // add current short sale to previous short basis
	    return -currentTrans.getShortSell()
		    - currentTrans.getCommision()
		    + (prevTransValues == null ? 0.0
			    : +prevTransValues.getShortBasis());
	} else { // subsequent pos smaller (closer to 0) than previous
		 // implies previous short basis must exist
	    double histAvgUnitCost = prevTransValues.getShortBasis() / adjPrevPos;
	    return prevTransValues.getShortBasis() + histAvgUnitCost
		    * currentTrans.getSecQuantity();
	}
    }

    @Override
    public void intializeGainsCalc(BulkSecInfo thisCurrentInfo,
	    TransactionValues thisTrans, SortedSet<TransactionValues> prevTranses) {
	this.currentInfo = thisCurrentInfo;
	this.currentTrans = thisTrans;
	this.prevTransValues = prevTranses.isEmpty() ? null : prevTranses.last();

	int currentDateInt = thisTrans.getParentTxn().getDateInt();
	CurrencyType cur = thisTrans.getReferenceAccount().getCurrencyType();
	double currentRate = cur == null ? 1.0
		: cur.getUserRateByDateInt(currentDateInt);
	int prevDateInt = prevTransValues == null ? Integer.MIN_VALUE
		: prevTransValues.getParentTxn().getDateInt();
	double splitAdjust = (cur == null ? 1.0 : cur.adjustRateForSplitsInt(
		prevDateInt, currentRate, currentDateInt) / currentRate);
	this.adjPrevPos = prevTransValues == null ? 0.0 : prevTransValues.getPosition()
		* splitAdjust;
	this.matchTable = getLotMatchTable();
	
	
    }
    
    /**populates lot matching table if available in transaction.  if
     * not available, return null
     * @return
     */
    public Hashtable<Long, Double> getLotMatchTable() {
	Hashtable<Long, Double> lotMatchTable = new Hashtable<Long, Double>();
	SplitTxn securitySplit = currentTrans.getReferenceAccount().getCurrencyType()
		.equals(BulkSecInfo.getCashCurrencyWrapper()) ? null : TxnUtil
		.getSecurityPart(currentTrans.getParentTxn());
	Hashtable<String, String> splitTable = null;
	if (securitySplit != null)
	    splitTable = TxnUtil.parseCostBasisTag(securitySplit);
	if (splitTable != null) {
	    for (String key : splitTable.keySet()) {
		Long keyLong = Long.parseLong(key);
		Long valueLong = Long.parseLong(splitTable.get(key));
		Double valueDouble = (Long.valueOf(valueLong).doubleValue()) / 10000.0;
		lotMatchTable.put(keyLong, valueDouble);
	    }
	}
	if (lotMatchTable.size() > 0) {
	    return lotMatchTable;
	} else {
	    return null;
	}
    }
    
    /**Split adjust prior transvalue with respect to current trans value
     * @param thisTrans
     * @param priorTrans
     * @return
     */
    public static Double getSplitAdjust(TransactionValues thisTrans,
	    TransactionValues priorTrans) {
	int currentDateInt = thisTrans.getParentTxn().getDateInt();
	CurrencyType cur = thisTrans.getReferenceAccount().getCurrencyType();
	double currentRate = cur == null ? 1.0 : cur
		.getUserRateByDateInt(currentDateInt);
	int prevDateInt = priorTrans == null ? Integer.MIN_VALUE
		: priorTrans.getParentTxn().getDateInt();
	double splitAdjust = (cur == null ? 1.0 : cur.adjustRateForSplitsInt(
		prevDateInt, currentRate, currentDateInt) / currentRate);
	return priorTrans == null ? 0.0 : splitAdjust;

    }

}
