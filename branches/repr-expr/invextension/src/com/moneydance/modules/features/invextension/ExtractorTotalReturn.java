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

import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by larus on 11/27/14.
 */
public class ExtractorTotalReturn extends ExtractorBase<Double> {
    // Invextension computes MD returns from first->last transaction in date range if the start and end
    // positions are zero. Makes more sense to compute over entire date range, but this flag exists for
    // compatibility. (Also using the entire date range eliminates the need to save the transactions in
    // this extractor -- but not AnnualReturn -- since the intermediate results can be added together.)
    private final boolean computeMDReturnOverEntireDateRange = false;

    protected ArrayList<TransactionValues> selectedTransactions;
    protected double mdReturn = 0;
    protected long startPosition = 0;
    protected long startValue = 0;
    protected long endPosition = 0;
    protected long endValue = 0;

    public ExtractorTotalReturn(SecurityAccountWrapper secAccountWrapper, int startDateInt, int endDateInt) {
        super(secAccountWrapper, startDateInt, endDateInt);

        selectedTransactions = new ArrayList<>();
    }

    public boolean NextTransaction(TransactionValues transaction, SecurityAccountWrapper securityAccount) {
        if (!super.NextTransaction(transaction, securityAccount)) {
            return false;
        }

        int transactionDateInt = transaction.getDateInt();
        if (startDateInt < transactionDateInt && transactionDateInt <= endDateInt) {
            selectedTransactions.add(transaction);
        }

        return true;
    }

    public Double FinancialResults(SecurityAccountWrapper securityAccount) {
        startPosition = getStartPosition(securityAccount);
        long startPrice = securityAccount.getPrice(startDateInt);
        startValue = qXp(startPosition, startPrice);
        endPosition = getEndPosition(securityAccount);
        long endPrice = securityAccount.getPrice(endDateInt);
        endValue = qXp(endPosition, endPrice);

        mdReturn = computeMDReturn();
        return mdReturn;
    }

    // Compiler warning (unchecked cast) because Java v7 type system is too weak to express this.
    public Double CombineFinancialResults(ExtractorBase<?> op) {
        ExtractorTotalReturn operand = (ExtractorTotalReturn) op;

        selectedTransactions.addAll(operand.selectedTransactions);
        Collections.sort(selectedTransactions);    // Reorder transactions sum from different securities

        startPosition += operand.startPosition;
        startValue += operand.startValue;
        endPosition += operand.endPosition;
        endValue += operand.endValue;

        mdReturn = computeMDReturn();
        return mdReturn;
    }

    // Compute Modified Dietz return
    protected double computeMDReturn() {
        long income = 0;
        long expenses = 0;
        long weightedCF = 0;
        long sumCF = 0;

        if (selectedTransactions.size() > 0) {
            int firstDateInt = selectedTransactions.get(0).getDateInt();
            if (computeMDReturnOverEntireDateRange || startPosition != 0) {
                firstDateInt = startDateInt;
            }
            int lastDateInt = selectedTransactions.get(selectedTransactions.size() - 1).getDateInt();
            if (computeMDReturnOverEntireDateRange || endPosition != 0) {
                lastDateInt = endDateInt;
            }
            int intervalDays = DateUtils.getDaysBetween(firstDateInt, lastDateInt);

            for (TransactionValues transaction : selectedTransactions) {
                long cashFlow = -(transaction.getBuy() + transaction.getSell()
                        + transaction.getShortSell() + transaction.getCoverShort() + transaction.getCommission());
                income += transaction.getIncome();
                expenses += transaction.getExpense();
                int transactionDays = DateUtils.getDaysBetween(firstDateInt, transaction.getDateInt());
                double transactionFraction = ((double) (intervalDays - transactionDays)) / intervalDays;
                weightedCF += Math.round(transactionFraction * cashFlow);
                sumCF += cashFlow;
            }
        }

        if ((startValue + weightedCF) != 0) {
            return ((double) ((endValue + income + expenses) - startValue - sumCF)) / (startValue + weightedCF);
        }

        return 0.0; // Default
    }
}
