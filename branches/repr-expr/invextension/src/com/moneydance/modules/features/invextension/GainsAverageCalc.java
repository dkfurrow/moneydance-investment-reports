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

import com.moneydance.apps.md.model.CurrencyType;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Implementation of Average Cost Method
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class GainsAverageCalc implements GainsCalc {
    private static final BigDecimal positionThreshold = BigDecimal.valueOf(0.00001);
    TransactionValues currentTrans;
    TransactionValues prevTransValues;
    BigDecimal adjPrevPos;


    public GainsAverageCalc() {
    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.GainsCalc#getLongBasis()
     */
    @Override
    public BigDecimal getLongBasis() {
        if (currentTrans.getPosition().compareTo(positionThreshold) <= 0) {// position short or closed
            return BigDecimal.ZERO;
        } else if (currentTrans.getPosition().compareTo((prevTransValues == null ? BigDecimal.ZERO : adjPrevPos))
                >= 0) {
            // first trans or subsequent larger position
            // add current buy to previous long basis
            return currentTrans.getBuy().negate()
                    .subtract(currentTrans.getCommission())
                    .add(prevTransValues == null ? BigDecimal.ZERO
                            : prevTransValues.getLongBasis());
        } else {
            // subsequent pos smaller than previous
            // implies prev long basis must exist
            BigDecimal histAvgUnitCost = prevTransValues.getLongBasis().divide(adjPrevPos, BigDecimal.ROUND_HALF_EVEN)
                    .setScale(SecurityReport.moneyScale, BigDecimal.ROUND_HALF_EVEN);
            return prevTransValues.getLongBasis().add(histAvgUnitCost.multiply(currentTrans.getSecQuantity()));
        }
    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.GainsCalc#getShortBasis()
     */
    @Override
    public BigDecimal getShortBasis() {
        if (currentTrans.getPosition().compareTo(positionThreshold.negate()) >= 0) { // position long or closed
            return BigDecimal.ZERO;
        } else if (currentTrans.getPosition().compareTo(prevTransValues == null ? BigDecimal.ZERO : adjPrevPos) <= 0) {
            // first trans or subsequent larger (more negative) position
            // add current short sale to previous short basis
            return currentTrans.getShortSell().negate().subtract(currentTrans.getCommission())
                    .add(prevTransValues == null ? BigDecimal.ZERO : prevTransValues.getShortBasis());
        } else { // subsequent pos smaller (closer to 0) than previous
            // implies previous short basis must exist
            BigDecimal histAvgUnitCost = prevTransValues.getShortBasis().divide(adjPrevPos, BigDecimal.ROUND_HALF_EVEN)
                    .setScale(SecurityReport.moneyScale, BigDecimal.ROUND_HALF_EVEN);
            return prevTransValues.getShortBasis()
                .add(currentTrans.getSecQuantity().multiply(histAvgUnitCost)
                        .setScale(SecurityReport.moneyScale, BigDecimal.ROUND_HALF_EVEN));
        }
    }

    @Override
    public void initializeGainsCalc(BulkSecInfo currentInfo,
                                    TransactionValues thisTrans, ArrayList<TransactionValues> prevTranses) {
        this.currentTrans = thisTrans;
        this.prevTransValues = prevTranses.isEmpty() ? null : prevTranses.get(prevTranses.size() - 1);

        int currentDateInt = thisTrans.getParentTxn().getDateInt();
        CurrencyType cur = thisTrans.getReferenceAccount().getCurrencyType();
        double currentRate = cur == null ? 1.0 : cur.getUserRateByDateInt(currentDateInt);

        int prevDateInt = prevTransValues == null ? Integer.MIN_VALUE
                : prevTransValues.getParentTxn().getDateInt();

        double splitAdjust = cur == null ? 1.0
                : cur.adjustRateForSplitsInt(prevDateInt, currentRate, currentDateInt) / currentRate;

        this.adjPrevPos = prevTransValues == null ? BigDecimal.ZERO
            : prevTransValues.getPosition().multiply(BigDecimal.valueOf(splitAdjust))
                .setScale(SecurityReport.quantityScale, BigDecimal.ROUND_HALF_EVEN);
    }

}
