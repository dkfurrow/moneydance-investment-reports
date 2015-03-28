/*
 * ExtractorTotalReturn.java
 * Copyright (c) 2014, James R. Larus, Dale K. Furrow
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


/**
 * Created by larus on 11/27/14.
 * <p/>
 * Compute Modified-Dietz return calculation for an investment over a given time interval.
 */
@SuppressWarnings("ALL")
public class ExtractorModifiedDietzReturn extends ExtractorReturnBase {


    // Returns are not defined over an range in which the underlying security is not held, except for the
    // all returns and annual returns calculations, where we use the largest interval that the position is
    // open in the original range.


    public ExtractorModifiedDietzReturn(SecurityAccountWrapper secAccountWrapper, SecurityReport securityReport, int startDateInt, int endDateInt,
                                        ReturnWindowType returnWindowType) {
        super(secAccountWrapper, securityReport, startDateInt, endDateInt, returnWindowType);
    }

    public ExtractorModifiedDietzReturn(ExtractorOrdinaryReturn extractorOrdinaryReturn){
        super(extractorOrdinaryReturn);
        getResult();
    }

    // Compute Modified Dietz return
    @Override
    public double computeReturn() {
        int intervalDays = 0;
        double unnormalizedWeightedCF = 0.0;
        double weightedCF = 0.0;
        int weightedDays = 0;
        long sumCF = 0;
        intervalDays = DateUtils.getDaysBetween(this.startDateInt, this.endDateInt);
        for (ReturnValueElement returnValueElement : collapseTotalReturnElements()) {
            weightedDays = DateUtils.getDaysBetween(returnValueElement.date, endDateInt);
            unnormalizedWeightedCF += weightedDays * returnValueElement.value;
            sumCF += returnValueElement.value;
        }

        if (intervalDays != 0) {
            weightedCF = unnormalizedWeightedCF / intervalDays;
            return ((double) ((endValue + incomeExpenseScalar) - startValue - sumCF))
                    / (startValue + weightedCF);
        } else {
            return SecurityReport.UndefinedReturn;
        }
    }
}