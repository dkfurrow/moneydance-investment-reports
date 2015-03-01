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
 *
 * Compute Modified-Dietz return calculation for an investment over a given time interval.
 */
@SuppressWarnings("ALL")
public class ExtractorModifiedDietzReturn extends ExtractorReturnBase {
    private boolean computingAllReturns;

    private TransactionValues firstTransaction = null;
    
    private long income;
    private long expenses;
    private long unnormalizedWeightedCF;
    private long sumCF;

    protected long startPosition = 0;
    protected long startValue = 0;
    protected long endPosition = 0;
    protected long endValue = 0;

    // Returns are not defined over an range in which the underlying security is not held, except for the
    // all returns and annual returns calculations, where we use the largest interval that the position is
    // open in the original range.
    private int intervalStartDateInt;
    private int intervaleEndDateInt;

    private boolean resultCurrent = false;
    private double result = 0;

    public ExtractorModifiedDietzReturn(SecurityAccountWrapper secAccountWrapper, int startDateInt, int endDateInt,
                                        boolean computingAllReturns) {
        super(secAccountWrapper, startDateInt, endDateInt, computingAllReturns);
        this.computingAllReturns = computingAllReturns;
        if (secAccountWrapper == null && computingAllReturns) {
            intervalStartDateInt = Integer.MAX_VALUE;
            intervaleEndDateInt = Integer.MIN_VALUE;
        } else {
            intervalStartDateInt = startDateInt;
            intervaleEndDateInt = endDateInt;
        }
    }

    public boolean processNextTransaction(TransactionValues transaction, int transactionDateInt) {
        if (!super.processNextTransaction(transaction, transactionDateInt)) {
            return false;
        }

        if (startDateInt < transactionDateInt && transactionDateInt <= endDateInt) {
            if (firstTransaction == null) {
                firstTransaction = transaction;
            }
            income += transaction.getIncome();
            expenses += transaction.getExpense();
            int weightedDays = DateUtils.getDaysBetween(transaction.getDateInt(), endDateInt);
            long cashFlow = transaction.getBuySellFlows();
            unnormalizedWeightedCF += weightedDays * cashFlow;
            sumCF += cashFlow;
        }

        return true;
    }

    public Double getResult() {
        if (!resultCurrent) {
            if (securityAccount != null) {
                // Not aggregate account
                startPosition = getStartPosition(securityAccount);
                long startPrice = securityAccount.getPrice(startDateInt);
                startValue = qXp(startPosition, startPrice);
                endPosition = getEndPosition(securityAccount);
                long endPrice = securityAccount.getPrice(endDateInt);
                endValue = qXp(endPosition, endPrice);

                if (computingAllReturns && startPosition == 0 && firstTransaction != null) {
                    intervalStartDateInt = firstTransaction.getDateInt();
                }
                if (computingAllReturns && endPosition == 0 && lastTransactionWithinDateRange != null) {
                    intervaleEndDateInt = lastTransactionWithinDateRange.getDateInt();
                }
            }

            result = computeMDReturn();
            resultCurrent = true;
        }

        return result;
    }

    // Compiler warning (unchecked cast) because Java v7 type system is too weak to express this.
    public void aggregateResults(ExtractorBase<?> op) {
        ExtractorModifiedDietzReturn operand = (ExtractorModifiedDietzReturn) op;

        if (operand.firstTransaction != null) {
            intervalStartDateInt = Math.min(intervalStartDateInt, operand.intervalStartDateInt);
        }
        if (operand.lastTransactionWithinDateRange != null) {
            intervaleEndDateInt = Math.max(intervaleEndDateInt, operand.intervaleEndDateInt);
        }

        startPosition += operand.startPosition;
        startValue += operand.startValue;
        endPosition += operand.endPosition;
        endValue += operand.endValue;

        income += operand.income;
        expenses += operand.expenses;
        unnormalizedWeightedCF += operand.unnormalizedWeightedCF;
        sumCF += operand.sumCF;

        resultCurrent = false;
    }

    // Compute Modified Dietz return
    private double computeMDReturn() {
        if (computingAllReturns || (startPosition != 0 && endPosition != 0)) {
            int intervalDays = DateUtils.getDaysBetween(intervalStartDateInt, intervaleEndDateInt);
            long weightedCF = Math.round(unnormalizedWeightedCF / (double) intervalDays);

            if ((startValue + weightedCF) != 0) {
                return ((double) ((endValue + income + expenses) - startValue - sumCF)) / (startValue + weightedCF);
            }
        }

        return SecurityReport.UndefinedReturn; // No flow in interval, so return is undefined.
    }
}
