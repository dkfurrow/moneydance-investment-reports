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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created by larus on 11/27/14.
 */
@SuppressWarnings("ALL")
public class ExtractorAnnualReturn extends ExtractorTotalReturn {



    public ExtractorAnnualReturn(SecurityAccountWrapper securityAccount, int startDateInt, int endDateInt, boolean lifeToDate) {
        super(securityAccount, startDateInt, endDateInt, lifeToDate);
    }

//    public boolean NextTransaction(TransactionValues transaction, int transactionDateInt) {
//        return super.NextTransaction(transaction, transactionDateInt);
//    }

    public Double FinancialResults(SecurityAccountWrapper securityAccount) {
        double mdReturn = super.FinancialResults(securityAccount);

        return computeFinancialResults(mdReturn);
    }

    // Compiler warning (unchecked cast) because Java v7 type system is too weak to express this.
    public void AggregateFinancialResults(ExtractorBase<?> op) {
        ExtractorAnnualReturn operand = (ExtractorAnnualReturn) op;
        super.AggregateFinancialResults(operand);
    }

    public Double ComputeAggregatedFinancialResults() {
        double mdReturn = super.ComputeAggregatedFinancialResults();

        return mdReturn == Double.NaN ? Double.NaN : computeFinancialResults(mdReturn);
    }

    protected ArrayList<DateValuePair> collapseAggregatedTotalFlows(){
        ArrayList<DateValuePair> totalFlowPairs = new ArrayList<>();
        for (ArrayList<DateValuePair> r : aggregatedBuySellPairs) {
            totalFlowPairs.addAll(r);
        }
        // reverse sign for annual returns convention
        for(DateValuePair dateValuePair : totalFlowPairs){
            dateValuePair.value *= -1;
        }
        for (ArrayList<DateValuePair> r : aggregatedIncExpPairs) {
            totalFlowPairs.addAll(r);
        }
        Collections.sort(totalFlowPairs);
        // Collapse returns on same date to single entry to speed computation
        int numReturns = totalFlowPairs.size();
        int i = 0;
        Iterator<DateValuePair> iterator = totalFlowPairs.iterator();
        if(iterator.hasNext()) iterator.next();
        while(iterator.hasNext()){
            DateValuePair dateValuePair = iterator.next();
            if(dateValuePair.compareTo(totalFlowPairs.get(i)) == 0){
                totalFlowPairs.get(i).value += dateValuePair.value;
                iterator.remove();
            } else {
                i++;
            }
        }

        return totalFlowPairs;
    }

    private Double computeFinancialResults(double mdReturn) {
        ArrayList<DateValuePair> aggregatedTotalFlows = collapseAggregatedTotalFlows();

        int numPeriods = aggregatedTotalFlows.size();
        double[] returns = new double[numPeriods + 2];
        double[] excelDates = new double[numPeriods + 2];
        int next = 0;

        if (startPosition != 0 && startValue != 0) {
            excelDates[next] = DateUtils.getExcelDateValue(startDateInt);
            returns[next] = (double) -startValue;
            next++;
        }

        for (int i = 0; i < numPeriods; i++) {
            DateValuePair dv = aggregatedTotalFlows.get(i);
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
                // Need to supply guess to return algorithm, so use modified dietz
                // return divided by number of years (have to add 1 because of returns
                // algorithm). Must be greater than zero, so we'll start with 10%, unless MD is greater.
                double guess = Math.max((1 + mdReturn / totYrs), 0.01);

                XIRRData thisData = new XIRRData(next, guess, returns, excelDates);
                double xirr = XIRR.xirr(thisData);
                return xirr;
            }
        }

        return 0.0; // Default
    }
}