/*
 * ExtractorGains.java
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

import java.util.Arrays;
import java.util.List;

/**
 * Created by larus on 11/28/14.
 */
public class ExtractorGainsFT extends ExtractorGains {
    public ExtractorGainsFT(SecurityAccountWrapper securityAccount, int startDateInt, int endDateInt) {
        super(securityAccount, startDateInt, endDateInt);
    }

    public List<Long> FinancialResults(SecurityAccountWrapper securityAccount) {  // RealizedGain, UnrealizedGain, TotalGain
        long startUnrealizedGain = 0;
        long endUnrealizedGain = 0;

        long startPosition = 0;
        if (lastTransactionBeforeStartDate != null) {
            startPosition = getStartPosition(securityAccount);
            long startPrice = securityAccount.getPrice(startDateInt);
            long startValue = qXp(startPosition, startPrice);

            if (startPosition > 0) {
                startUnrealizedGain = startValue - lastTransactionBeforeStartDate.getLongBasis();
            } else if (startPosition < 0) {
                startUnrealizedGain = startValue - lastTransactionBeforeStartDate.getShortBasis();
            }
        }

        if (lastTransactionWithinDateRange != null) {
            long endPosition = getEndPosition(securityAccount);
            long endValue = qXp(endPosition, securityAccount.getPrice(endDateInt));
            if (endPosition > 0) {
                endUnrealizedGain = endValue - lastTransactionWithinDateRange.getLongBasis();
            } else if (endPosition < 0) {
                endUnrealizedGain = endValue - lastTransactionWithinDateRange.getShortBasis();
            }
        } else {
            long endPosition = startPosition;
            long endValue = qXp(endPosition, securityAccount.getPrice(endDateInt));
            if (endPosition > 0) {
                endUnrealizedGain = endValue - lastTransactionBeforeStartDate.getLongBasis();
            } else if (endPosition < 0) {
                endUnrealizedGain = endValue - lastTransactionBeforeStartDate.getShortBasis();
            }
        }

        long unrealizedGain = endUnrealizedGain - startUnrealizedGain;
        long totalGain = realizedGain + unrealizedGain;

        return Arrays.asList(realizedGain, unrealizedGain, totalGain);
    }
}