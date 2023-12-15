/*
 * GainsAverageCalc.java
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

import java.util.LinkedHashMap;

/**
 * Implementation of Average Cost Method
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */
public final class GainsAverageCalc implements GainsCalc {
    private static final double positionThreshold = 0.00001;
    TransactionValues currentTrans;
    TransactionValues prevTransValues;
    long adjPrevPos;


    public GainsAverageCalc() {
    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.GainsCalc#getLongBasis()
     */
    @Override
    public long getLongBasis() {
        if (currentTrans.getPosition() <= positionThreshold) {// position short or closed
            return 0;
        } else if (currentTrans.getPosition() > (prevTransValues == null ? 0 : adjPrevPos)) {
            // first trans or subsequent larger position
            // add current buy to previous long basis
            return -currentTrans.getBuy()
                    - currentTrans.getCommission()
                    - currentTrans.getExpense()
                    + (prevTransValues == null ? 0
                    : prevTransValues.getLongBasis());
        } else if (currentTrans.getPosition() < (prevTransValues == null ? 0 : adjPrevPos)) {
            // subsequent pos smaller than previous
            // implies prev long basis must exist
            assert prevTransValues != null;
            double histAvgUnitCost = ((double)prevTransValues.getLongBasis()) / adjPrevPos;
            return prevTransValues.getLongBasis()
                    + Math.round(histAvgUnitCost * currentTrans.getSecQuantity());
        } else {
            return  prevTransValues == null ? 0 : prevTransValues.getLongBasis();
        }
    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.GainsCalc#getShortBasis()
     */
    @Override
    public long getShortBasis() {
        if (currentTrans.getPosition() >= -positionThreshold) { // position long or closed
            return 0;
        } else if (currentTrans.getPosition() < (prevTransValues == null ? 0 : adjPrevPos)) {
            // first trans or subsequent larger (more negative) position
            // add current short sale to previous short basis
            return -currentTrans.getShortSell()
                    - currentTrans.getCommission()
                    - currentTrans.getExpense()
                    + (prevTransValues == null ? 0
                    : +prevTransValues.getShortBasis());
        } else if (currentTrans.getPosition() > (prevTransValues == null ? 0 : adjPrevPos)) {
            // subsequent pos smaller (closer to 0) than previous
            // implies previous short basis must exist
            assert prevTransValues != null;
            double histAvgUnitCost = ((double)prevTransValues.getShortBasis()) / adjPrevPos;
            return prevTransValues.getShortBasis()
                    + Math.round(histAvgUnitCost * currentTrans.getSecQuantity());
        } else {
            return prevTransValues == null ? 0 : prevTransValues.getShortBasis();
        }
    }

    @Override
    public void initializeGainsCalc(TransactionValues thisTransactionValues,
                                    LinkedHashMap<String, TransactionValues> previousTransactionValues) {
        this.currentTrans = thisTransactionValues;
        this.prevTransValues = previousTransactionValues.isEmpty() ? null :
                previousTransactionValues.lastEntry().getValue();

        int currentDateInt = thisTransactionValues.getParentTxn().getDateInt();
        CurrencyType cur = thisTransactionValues.getReferenceAccount().getCurrencyType();
        double currentRate = cur.getRate(null, currentDateInt);
        int prevDateInt = prevTransValues == null ? Integer.MIN_VALUE
                : prevTransValues.getParentTxn().getDateInt();
        double splitAdjust = cur.adjustRateForSplitsInt(prevDateInt, currentRate,
                currentDateInt) / currentRate;
        this.adjPrevPos = prevTransValues == null ? 0
                : Math.round(prevTransValues.getPosition() * splitAdjust);
    }

}
