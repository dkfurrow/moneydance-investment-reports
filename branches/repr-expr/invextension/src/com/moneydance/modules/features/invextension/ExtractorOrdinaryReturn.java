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
 * Created by larus on 3/1/15.
 */
@SuppressWarnings("ALL")
public class ExtractorOrdinaryReturn extends ExtractorReturnBase {
    protected long startValue = 0;
    protected long endValue = 0;

    public ExtractorOrdinaryReturn(SecurityAccountWrapper secAccountWrapper, int startDateInt, int endDateInt,
                                   ReturnWindowType computingAllReturns) {
        super(secAccountWrapper, startDateInt, endDateInt, computingAllReturns);
    }

    public Double getResult() {
        if (securityAccount != null) {
            long startPosition = getStartPosition(securityAccount);
            long startPrice = securityAccount.getPrice(startDateInt);
            startValue = qXp(startPosition, startPrice);
            long endPosition = getEndPosition(securityAccount);
            long endPrice = securityAccount.getPrice(endDateInt);
            endValue = qXp(endPosition, endPrice);
        }

        if (startValue != 0) {
            double result = (double) (endValue - startValue) / (double) startValue;
            return result;
        } else {
            return SecurityReport.UndefinedReturn; // No starting position, so return is undefined.
        }
    }

    // Compiler warning (unchecked cast) because Java v7 type system is too weak to express this.
    public void aggregateResults(ExtractorBase<?> op) {
        ExtractorOrdinaryReturn operand = (ExtractorOrdinaryReturn) op;

        startValue += operand.startValue;
        endValue += operand.endValue;
    }
}
