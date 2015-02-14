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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Created by larus on 11/27/14.
 */
@SuppressWarnings("ALL")
public class ExtractorAnnualReturn extends ExtractorTotalReturn {
    private class dateValuePair implements Comparable<dateValuePair> {
        public final int date;
        public long value;

        public dateValuePair(int d, long v) {
            date = d;
            value = v;
        }

        public int compareTo(@NotNull dateValuePair operand) {
            return date - operand.date;
        }
    }

    private ArrayList<dateValuePair> nonZeroReturns;
    private LinkedList<ArrayList<dateValuePair>> aggregatedReturns;
    private int aggregatedReturnsSize;

    public ExtractorAnnualReturn(SecurityAccountWrapper securityAccount, int startDateInt, int endDateInt) {
        super(securityAccount, startDateInt, endDateInt, true);

        nonZeroReturns = new ArrayList<>();
        aggregatedReturns = new LinkedList<>();
        aggregatedReturnsSize = 0;
        aggregatedReturns.add(nonZeroReturns);
    }

    public boolean NextTransaction(TransactionValues transaction, int transactionDateInt) {
        if (!super.NextTransaction(transaction, transactionDateInt)) {
            return false;
        }

        if (startDateInt < transactionDateInt && transactionDateInt <= endDateInt) {
            long totalFlows = transaction.getTotalFlows();
            if (totalFlows != 0) {
                nonZeroReturns.add(new dateValuePair(transactionDateInt, totalFlows));
            }
        }

        return true;
    }

    public Double FinancialResults(SecurityAccountWrapper securityAccount) {
        double mdReturn = super.FinancialResults(securityAccount);

        return computeFinancialResults(mdReturn);
    }

    // Compiler warning (unchecked cast) because Java v7 type system is too weak to express this.
    public void AggregateFinancialResults(ExtractorBase<?> op) {
        ExtractorAnnualReturn operand = (ExtractorAnnualReturn) op;
        super.AggregateFinancialResults(operand);

        aggregatedReturns.add(operand.nonZeroReturns);
        aggregatedReturnsSize += operand.nonZeroReturns.size();
    }

    public Double ComputeAggregatedFinancialResults() {
        double mdReturn = super.ComputeAggregatedFinancialResults();

        return computeFinancialResults(mdReturn);
    }

    private Double computeFinancialResults(double mdReturn) {
        ArrayList<dateValuePair> allReturns = new ArrayList<>(aggregatedReturnsSize);
        for (ArrayList<dateValuePair> r : aggregatedReturns) {
            allReturns.addAll(r);
        }
        Collections.sort(allReturns);
        // Collapse returns on same date to single entry to speed computation
        int numReturns = allReturns.size();
        int i = 0;
        int j = 1;
        for (; i < numReturns && j < numReturns; i++, j++) {
            assert i < j;
            for (; j < numReturns && allReturns.get(i) == allReturns.get(j); j++) {
                allReturns.get(i).value += allReturns.get(j).value;
            }
            assert (j >= numReturns) || allReturns.get(i) != allReturns.get(j);
            if (j < numReturns) {
                allReturns.set(i + 1, allReturns.get(j));
            }
        }

        int numPeriods = (numReturns == 0) ? 0 : i + 1;
        double[] returns = new double[numPeriods + 2];
        double[] excelDates = new double[numPeriods + 2];
        int next = 0;

        if (startPosition != 0 && startValue != 0) {
            excelDates[next] = DateUtils.getExcelDateValue(startDateInt);
            returns[next] = (double) -startValue;
            next++;
        }

        for (i = 0; i < numPeriods; i++) {
            dateValuePair dv = allReturns.get(i);
            excelDates[next] = DateUtils.getExcelDateValue(dv.date);
            returns[next] = (double) dv.value;
            next++;
        }

        if (endPosition != 0 && endValue != 0) {
            excelDates[next] = DateUtils.getExcelDateValue(endDateInt);
            returns[next] = (double) endValue;
            next++;
        }

        if (next != 0) {
            double totYrs = (excelDates[next - 1] - excelDates[0]) / 365;
            if (totYrs != 0) {
                double guess = 0.10;
                // Need to supply guess to return algorithm, so use modified dietz return divided by number of years.
                // (Must add 1 because of returns algorithm).
                // Return must be greater than zero, so we'll start with 10%, unless MD is greater.
                // Also use 10% if MD return is undefined.
                if (mdReturn != SecurityReport.UndefinedReturn) {
                    guess = Math.max((1 + mdReturn / totYrs), 0.01);
                }

                XIRRData thisData = new XIRRData(next, guess, returns, excelDates);
                double xirr = XIRR.xirr(thisData);
                return xirr;
            }
        }

        return SecurityReport.UndefinedReturn; // No flow in interval, so return is undefined.
    }
}