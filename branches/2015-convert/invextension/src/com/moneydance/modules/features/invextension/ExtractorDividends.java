/*
 * ExtractorDividends.java
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
 * Created by larus on 11/28/14.
 */



import com.infinitekind.moneydance.model.InvestTxnType;
import com.infinitekind.moneydance.model.TxnUtil;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;


public class ExtractorDividends extends ExtractorBase<List<Number>> {
    private TransactionValues firstTransactionBasis;
    private TransactionValues lastTransactionBasis;

    Stack<TransactionValues> dividendTransactions = new Stack<>(); //recent distributions

    public ExtractorDividends(SecurityAccountWrapper securityAccount, int startDateInt, int endDateInt) {
        super(securityAccount, startDateInt, endDateInt);

        firstTransactionBasis = null;
        lastTransactionBasis = null;
    }

    public boolean processNextTransaction(TransactionValues transaction, int transactionDateInt) {
        if (!super.processNextTransaction(transaction, transactionDateInt)) {
            return false;
        }

        if (startDateInt < transactionDateInt && transactionDateInt <= endDateInt) {
            InvestTxnType transType = TxnUtil.getInvestTxnType(transaction.getParentTxn());
            updateBasisTransactions(transaction);
            if (isDividendType(transType) && transaction.getIncome() != 0) {
                updateDividendTransactions(transaction);
            }
        }
        return true;
    }

    public List<Number> getResult() {    // AnnualizedDividend, DividendYield, YieldOnBasis
        if (firstTransactionBasis != null && lastTransactionWithinDateRange != null) {
            // reference transaction is last transaction older than MINIMUM_EX_DIV_DAYS
            // allows for situations where dividends are immediately reinvested
            TransactionValues basisReferenceTransaction = firstTransactionBasis;
            long splitAdjustReferencePos = getSplitAdjustedPosition(securityAccount,
                    basisReferenceTransaction.getPosition(),
                    basisReferenceTransaction.getDateInt(), endDateInt);
            long annualizedDivTotal = getAnnualizedDividend(securityAccount);
            long endPosition = getEndPosition(securityAccount);
            long lastPrice = securityAccount.getPrice(endDateInt);
            long longBasis = lastTransactionWithinDateRange.getLongBasis();

            if (splitAdjustReferencePos > 0 && endPosition > 0) {
                double positionRatio = (double) endPosition / (double) splitAdjustReferencePos;
                long annualizedDividend = Math.round(positionRatio * annualizedDivTotal);
                long annualizedDivPerShare = pDq(annualizedDivTotal, splitAdjustReferencePos);
                double dividendYield = (double) annualizedDivPerShare / lastPrice;
                double yieldOnBasis = (longBasis > 0) ? annualizedDividend / longBasis : 0.0;
                return Arrays.asList((Number) annualizedDividend, dividendYield, yieldOnBasis);
            }
        }

        return Arrays.asList((Number) 0L, 0.0, 0.0);    // Default
    }


    //
    // Internal
    //
    private void updateBasisTransactions(TransactionValues transaction) {
        if (firstTransactionBasis == null) {
            firstTransactionBasis = transaction;
            lastTransactionBasis = transaction;
        }
        int daysBetween = DateUtils.getDaysBetween(transaction.getDateInt(), lastTransactionBasis.getDateInt());
        if (daysBetween <= SecurityAccountWrapper.DividendFrequencyAnalyzer.MINIMUM_EX_DIV_DAYS) {
            //possible correct to previous, or multiple distributions, update last transaction value only
            lastTransactionBasis = transaction;
        } else {
            // new transaction: swap last and first
            firstTransactionBasis = lastTransactionBasis;
            lastTransactionBasis = transaction;
        }
    }

    private boolean isDividendType(InvestTxnType transType) {
        switch (transType) {
            case DIVIDEND:
            case DIVIDEND_REINVEST:
            case DIVIDENDXFR:
            case BANK:
                return true;

            default:
                return false;
        }
    }

    /**
     * add current dividend, clear stack of "old" dividend transactions
     *
     * @param transaction dividend transaction
     */
    private void updateDividendTransactions(TransactionValues transaction) {
        int currentDateInt = transaction.getDateInt();
        for (Iterator<TransactionValues> iterator = dividendTransactions.iterator(); iterator.hasNext(); ) {
            int daysFromLastDivTransaction = DateUtils.getDaysBetween(currentDateInt, iterator.next().getDateInt());
            //remove transaction if older than MINIMUM_EX_DIV_DAYS
            if (daysFromLastDivTransaction > SecurityAccountWrapper.DividendFrequencyAnalyzer.MINIMUM_EX_DIV_DAYS)
                iterator.remove();
        }
        dividendTransactions.push(transaction);
    }

    /**
     * Calculate annualized dividend based on dividend frequency from SecurityAccountWrapper
     *
     * @return annualized dividend in units of currency
     */
    private long getAnnualizedDividend(SecurityAccountWrapper securityAccount) {
        long totalDividends = 0;
        long annualizingFactor = 0;
        for (TransactionValues transactionValues : dividendTransactions) {
            totalDividends += transactionValues.getIncome();
        }
        SecurityAccountWrapper.DIV_FREQUENCY div_frequency = securityAccount.getDivFrequency();
        switch (div_frequency) {
            case ANNUAL:
                annualizingFactor = 1;
                break;
            case BIANNUAL:
                annualizingFactor = 2;
                break;
            case QUARTERLY:
                annualizingFactor = 4;
                break;
            case MONTHLY:
                annualizingFactor = 12;
                break;
            default:
                break;
        }
        return totalDividends > 0 ? totalDividends * annualizingFactor : 0;
    }
}