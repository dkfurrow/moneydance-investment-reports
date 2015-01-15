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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by larus on 11/27/14.
 */
@SuppressWarnings("ALL")
public class ExtractorTotalReturn extends ExtractorBase<Double> {

    protected ArrayList<DateValuePair> nonZeroBuySellPairs;
    protected LinkedList<ArrayList<DateValuePair>> aggregatedBuySellPairs;
    protected ArrayList<DateValuePair> nonZeroIncExpPairs;
    protected LinkedList<ArrayList<DateValuePair>> aggregatedIncExpPairs;


    private long sumBuySell;
    private long sumIncExp;

    protected long startPosition = 0;
    protected long startValue = 0;
    protected long endPosition = 0;
    protected long endValue = 0;

    private boolean forceEndDate = false;

    public ExtractorTotalReturn(SecurityAccountWrapper secAccountWrapper, int startDateInt, int endDateInt,
                                boolean forceEndDate) {
        super(secAccountWrapper, startDateInt, endDateInt);
        this.forceEndDate = forceEndDate;
        nonZeroBuySellPairs = new ArrayList<>();
        aggregatedBuySellPairs = new LinkedList<>();
        aggregatedBuySellPairs.add(nonZeroBuySellPairs);

        nonZeroIncExpPairs = new ArrayList<>();
        aggregatedIncExpPairs = new LinkedList<>();
        aggregatedIncExpPairs.add(nonZeroIncExpPairs);

    }

    public boolean NextTransaction(TransactionValues transaction, int transactionDateInt) {
        if (!super.NextTransaction(transaction, transactionDateInt)) {
            return false;
        }

        if (startDateInt <= transactionDateInt && transactionDateInt <= endDateInt) {
            long incExpFlows = transaction.getIncome() + transaction.getExpense();
            long buySellFlows = transaction.getBuySellFlows();
            if(incExpFlows != 0) nonZeroIncExpPairs.add(new DateValuePair(transactionDateInt, incExpFlows));
            if(buySellFlows != 0) nonZeroBuySellPairs.add(new DateValuePair(transactionDateInt, buySellFlows));
            sumIncExp += incExpFlows;
            sumBuySell += buySellFlows;
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

        return computeMDReturn();
    }

    // Compiler warning (unchecked cast) because Java v7 type system is too weak to express this.
    public void AggregateFinancialResults(ExtractorBase<?> op) {
        ExtractorTotalReturn operand = (ExtractorTotalReturn) op;

        startDateInt = Math.min(startDateInt, operand.startDateInt);
        endDateInt = Math.max(endDateInt, operand.endDateInt);

        startPosition += operand.startPosition;
        startValue += operand.startValue;
        endPosition += operand.endPosition;
        endValue += operand.endValue;

        sumIncExp += operand.sumIncExp;
        sumBuySell += operand.sumBuySell;
        aggregatedIncExpPairs.add(operand.nonZeroIncExpPairs);
        aggregatedBuySellPairs.add(operand.nonZeroBuySellPairs);

    }

    public Double ComputeAggregatedFinancialResults() {
        return computeMDReturn();
    }

    // Compute Modified Dietz return
    private double computeMDReturn() {
        // Return is not defined over an interval in which the underlying security(s) are not held
        // with the exception of life-to-date
        if ((startPosition != 0 && endPosition != 0) || (forceEndDate && aggregatedBuySellPairs.size() > 0)) {
            long weightedCF = getWeightedBuySellCashFlows();
            if ((startValue + weightedCF) != 0) {
                return ((double) ((endValue + sumIncExp) - startValue - sumBuySell)) /
                        ((double) (startValue + weightedCF));
            }
        }

        return Double.NaN; // Default
    }

    private long getWeightedBuySellCashFlows(){

        ArrayList<DateValuePair> dateValuePairs = collapseAggregatedBuySellPairs();
        if(dateValuePairs.size() == 0) return 0;
        int endCalcPeriod = (forceEndDate && endPosition == 0) ?
                dateValuePairs.get(dateValuePairs.size() -1).date : endDateInt;
        long unnormalizedWeightedCF = 0;
        int intervalDays = DateUtils.getDaysBetween(startDateInt, endCalcPeriod);
        for (DateValuePair dateValuePair : dateValuePairs){
            int weightedDays = DateUtils.getDaysBetween(dateValuePair.date, endCalcPeriod);
            unnormalizedWeightedCF += weightedDays * dateValuePair.value;
        }
        return Math.round(unnormalizedWeightedCF / (double) intervalDays);
    }

    protected ArrayList<DateValuePair> collapseAggregatedBuySellPairs(){
        ArrayList<DateValuePair> buySellPairs = new ArrayList<>();
        for (ArrayList<DateValuePair> r : aggregatedBuySellPairs) {
            buySellPairs.addAll(r);
        }
        Collections.sort(buySellPairs);
        // Collapse returns on same date to single entry to speed computation
        int numReturns = buySellPairs.size();
        int i = 0;
        Iterator<DateValuePair> iterator = buySellPairs.iterator();
        if(iterator.hasNext()) iterator.next();
        while(iterator.hasNext()){
            DateValuePair dateValuePair = iterator.next();
            if(dateValuePair.compareTo(buySellPairs.get(i)) == 0){
                buySellPairs.get(i).value += dateValuePair.value;
                iterator.remove();
            } else {
                i++;
            }
        }

        return buySellPairs;
    }

    protected class DateValuePair implements Comparable<DateValuePair> {
        public final int date;
        public long value;

        public DateValuePair(int d, long v) {
            date = d;
            value = v;
        }

        public int compareTo(@NotNull DateValuePair operand) {
            return date - operand.date;
        }
    }
}
