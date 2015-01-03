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
public class ExtractorGains extends ExtractorBase<List<Long>> {
    protected long realizedGain;

    public ExtractorGains(SecurityAccountWrapper securityAccount, int startDateInt, int endDateInt) {
        super(securityAccount, startDateInt, endDateInt);

        realizedGain = 0;
    }

    public boolean NextTransaction(TransactionValues transaction, int transactionDateInt) {
        if (!super.NextTransaction(transaction, transactionDateInt)) {
            return false;
        }

        if (startDateInt < transactionDateInt && transactionDateInt <= endDateInt) {
            realizedGain += transaction.getPerRealizedGain();
        }

        return true;
    }

    public List<Long> FinancialResults(SecurityAccountWrapper securityAccount) {  // RealizedGain, UnrealizedGain, TotalGain
        if (lastTransactionWithinDateRange != null) {
            long unrealizedGain = 0;
            long endPosition = getEndPosition(securityAccount);
            long lastPrice = securityAccount.getPrice(endDateInt);
            long endValue = qXp(endPosition, lastPrice);

            if (endPosition > 0) {
                unrealizedGain = endValue - lastTransactionWithinDateRange.getLongBasis();
            } else if (endPosition < 0) {
                unrealizedGain = endValue - lastTransactionWithinDateRange.getShortBasis();
            }

            long totalGain = realizedGain + unrealizedGain;

            return Arrays.asList(realizedGain, unrealizedGain, totalGain);
        }

        return Arrays.asList(0L, 0L, 0L);  // Default
    }
}