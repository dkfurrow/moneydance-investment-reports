/*
 * ExtractorAnnualReturn.java
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
 */
public class ExtractorAnnualReturn extends ExtractorTotalReturn {
    // Annual return:

    public ExtractorAnnualReturn(SecurityAccountWrapper securityAccount, int startDateInt, int endDateInt) {
        super(securityAccount, startDateInt, endDateInt);
    }

    public boolean NextTransaction(TransactionValues transaction, SecurityAccountWrapper securityAccount) {
        return super.NextTransaction(transaction, securityAccount);
    }

    public Double FinancialResults(SecurityAccountWrapper securityAccount) {
        super.FinancialResults(securityAccount);

        return computeFinancialResults();
    }

    // Compiler warning (unchecked cast) because Java v7 type system is too weak to express this.
    public Double CombineFinancialResults(ExtractorBase<?> op) {
        ExtractorTotalReturn operand = (ExtractorTotalReturn) op;
        super.CombineFinancialResults(operand);

        return computeFinancialResults();
    }

    private Double computeFinancialResults() {
        int numPeriods = selectedTransactions.size();
        double[] returns = new double[numPeriods + 2];
        double[] excelDates = new double[numPeriods + 2];
        int next = 0;

        if (startPosition != 0 && startValue != 0) {
            excelDates[next] = DateUtils.getExcelDateValue(startDateInt);
            returns[next] = (double) -startValue;
            next++;
        }
        for (TransactionValues transaction : selectedTransactions) {
            double totalFlows = (double) (transaction.getBuy() + transaction.getSell()
                    + transaction.getShortSell() + transaction.getCoverShort()
                    + transaction.getCommission() + transaction.getIncome()
                    + transaction.getExpense());
            if (totalFlows != 0.0) {
                double flowDate = DateUtils.getExcelDateValue(transaction.getDateInt());
                excelDates[next] = flowDate;
                returns[next] = totalFlows;
                next++;
            }
        }
        if (endPosition != 0 && endValue != 0) {
            excelDates[next] = DateUtils.getExcelDateValue(endDateInt);
            returns[next] = (double) endValue;
            next++;
        }

        if (next != 0) {
            double totYrs = (excelDates[next - 1] - excelDates[0]) / 365;
            if (totYrs != 0) {
                // Need to supply guess to return algorithm, so use modified dietz
                // return divided by number of years (have to add 1 because of returns
                // algorithm). Must be greater than zero, so we'll start with 10%, unless MD is greater.
                double guess = Math.max((1 + mdReturn / totYrs), 0.01);

                XIRRData thisData = new XIRRData(next, guess, returns, excelDates);
                double ret = XIRR.xirr(thisData);
                return ret;
            }
        }

        return 0.0; // Default
    }
}