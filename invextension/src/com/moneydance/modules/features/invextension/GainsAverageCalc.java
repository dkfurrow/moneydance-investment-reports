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

import java.util.SortedSet;

import com.moneydance.apps.md.model.CurrencyType;

/**
 * Implementation of Average Cost Method
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class GainsAverageCalc implements GainsCalc {
    TransValues currentTrans;
    TransValues prevTransValues;
    double adjPrevPos;
    
    
    
    public GainsAverageCalc() {
    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.GainsCalc#getLongBasis()
     */
    @Override
    public double getLongBasis() {
	

	if (currentTrans.position <= 0.00001) {// position short or closed
	    return 0.0;
	} else if (currentTrans.position >= (prevTransValues == null ? 0
		: adjPrevPos)) {
	    // first trans or subsequent larger position
	    // add current buy to previous long basis
	    return -currentTrans.buy
		    - currentTrans.commision
		    + (prevTransValues == null ? 0.0
			    : prevTransValues.longBasis);
	} else { // subsequent pos smaller than previous
		 // implies prev long basis must exist
	    double histAvgUnitCost = prevTransValues.longBasis / adjPrevPos;
	    return prevTransValues.longBasis + histAvgUnitCost
		    * currentTrans.secQuantity;
	}
    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.GainsCalc#getShortBasis()
     */
    @Override
    public double getShortBasis() {
	if (currentTrans.position >= -0.00001) { // position long or closed
	    return 0.0;
	} else if (currentTrans.position <= (prevTransValues == null ? 0.0
		: adjPrevPos)) {
	    // first trans or subsequent larger (more negative) position
	    // add current short sale to previous short basis
	    return -currentTrans.shortSell
		    - currentTrans.commision
		    + (prevTransValues == null ? 0.0
			    : +prevTransValues.shortBasis);
	} else { // subsequent pos smaller (closer to 0) than previous
		 // implies previous short basis must exist
	    double histAvgUnitCost = prevTransValues.shortBasis / adjPrevPos;
	    return prevTransValues.shortBasis + histAvgUnitCost
		    * currentTrans.secQuantity;
	}
    }

    @Override
    public void intializeGainsCalc(BulkSecInfo currentInfo,
	    TransValues thisTrans, SortedSet<TransValues> prevTranses) {
	this.currentTrans = thisTrans;
	this.prevTransValues = prevTranses.isEmpty() ? null : prevTranses.last();

	int currentDateInt = thisTrans.parentTxn.getDateInt();
	CurrencyType cur = thisTrans.accountRef.getCurrencyType();
	double currentRate = cur == null ? 1.0
		: cur.getUserRateByDateInt(currentDateInt);
	int prevDateInt = prevTransValues == null ? Integer.MIN_VALUE
		: prevTransValues.parentTxn.getDateInt();
	double splitAdjust = (cur == null ? 1.0 : cur.adjustRateForSplitsInt(
		prevDateInt, currentRate, currentDateInt) / currentRate);
	this.adjPrevPos = prevTransValues == null ? 0.0 : prevTransValues.position
		* splitAdjust;
	
    }

}
