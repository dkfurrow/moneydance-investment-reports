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
import java.util.TreeSet;

/**
 * Created by larus on 11/27/14.
 *
 * Compute Modified-Dietz return calculation for an investment over a given time interval.
 */
@SuppressWarnings("ALL")
public class ExtractorModifiedDietzReturn extends ExtractorReturnBase {
    private ReturnWindowType returnWindowType;
    protected TreeSet<ReturnValueTuple> capitalValues;

    private TransactionValues firstTransaction = null;

    private long incomeExpenseScalar = 0;


    protected long startPosition = 0;
    protected long startValue = 0;
    protected long endPosition = 0;
    protected long endValue = 0;

    // Returns are not defined over an range in which the underlying security is not held, except for the
    // all returns and annual returns calculations, where we use the largest interval that the position is
    // open in the original range.


    private boolean resultCurrent = false;
    private double result = 0;

    public ExtractorModifiedDietzReturn(SecurityAccountWrapper secAccountWrapper, int startDateInt, int endDateInt,
                                        ReturnWindowType returnWindowType) {
        super(secAccountWrapper, startDateInt, endDateInt, returnWindowType);
        this.returnWindowType = returnWindowType;
        capitalValues = new TreeSet<>();
        switch (returnWindowType) {
            case ALL:
                if (secAccountWrapper == null) {
                    this.startDateInt = Integer.MAX_VALUE;
                    this.endDateInt = Integer.MIN_VALUE;
                } else {
                    ArrayList<TransactionValues> transSet = secAccountWrapper.getTransactionValues();
                    if (!transSet.isEmpty()) {
                        this.startDateInt = DateUtils.getPrevBusinessDay(transSet.get(0).getDateInt());
                        this.endDateInt = endDateInt;
                    } else {
                        break;
                    }
                }
                break;
            case ANY:
            case DEFAULT:
                this.startDateInt = startDateInt;
                this.endDateInt = endDateInt;
                break;
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
            incomeExpenseScalar += transaction.getIncomeExpenseFlows();
//            int weightedDays = DateUtils.getDaysBetween(transaction.getDateInt(), endDateInt);
            long cashFlow = transaction.getBuySellFlows();
            if(cashFlow != 0) capitalValues.add(new ReturnValueTuple(transactionDateInt,
                    cashFlow, transaction.getTxnID()));
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
                switch (returnWindowType){
                    case DEFAULT:
                        if(startValue == 0) return SecurityReport.UndefinedReturn;
                        if(endValue == 0 && lastTransactionWithinDateRange != null){
                            this.endDateInt = lastTransactionWithinDateRange.getDateInt();
                        }
                        break;
                    case ANY:
                        if(startValue == 0 && firstTransaction != null) {
                            this.startDateInt = firstTransaction.getDateInt();
                        }
                        if(endValue == 0 && lastTransactionWithinDateRange != null){
                            this.endDateInt = lastTransactionWithinDateRange.getDateInt();
                        }
                        break;
                    case ALL:
                        if(endValue == 0 && lastTransactionWithinDateRange != null){
                            this.endDateInt = lastTransactionWithinDateRange.getDateInt();
                        }
                        break;
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
            this.startDateInt = Math.min(this.startDateInt, operand.startDateInt);
        }
        if (operand.lastTransactionWithinDateRange != null) {
            this.endDateInt = Math.max(this.endDateInt, operand.endDateInt);
        }

        startPosition += operand.startPosition;
        startValue += operand.startValue;
        endPosition += operand.endPosition;
        endValue += operand.endValue;

        incomeExpenseScalar += operand.incomeExpenseScalar;
        capitalValues.addAll(operand.capitalValues);
        resultCurrent = false;
    }

    // Compute Modified Dietz return
    private double computeMDReturn() {
        int intervalDays = 0;
        double unnormalizedWeightedCF = 0.0;
        double weightedCF = 0.0;
        int weightedDays = 0;
        long sumCF = 0;



        intervalDays = DateUtils.getDaysBetween(this.startDateInt, this.endDateInt);
        for(ReturnValueTuple returnValueTuple : collapseTotalReturnTuples()){
            weightedDays = DateUtils.getDaysBetween(returnValueTuple.date, endDateInt);
            unnormalizedWeightedCF += weightedDays * returnValueTuple.value;
            sumCF += returnValueTuple.value;
        }

        if(intervalDays != 0){
            weightedCF = unnormalizedWeightedCF / intervalDays;
            return ((double) ((endValue + incomeExpenseScalar) - startValue - sumCF))
                    / (startValue + weightedCF);
        } else {
            return SecurityReport.UndefinedReturn;
        }
    }

    private ArrayList<ReturnValueTuple> collapseTotalReturnTuples(){
        ArrayList<ReturnValueTuple> collapsedList = new ArrayList<>(capitalValues);
        // Collapse returns on same date to single entry to speed computation
        int numTuples = collapsedList.size();

        for(int i = 0; i < numTuples; i++){
            int j = i + 1;
            while(j < numTuples && collapsedList.get(i).date == collapsedList.get(j).date){
                collapsedList.get(i).value += collapsedList.get(j).value;
                collapsedList.remove(j);
                numTuples --;
            }
        }
        return collapsedList;
    }
}
