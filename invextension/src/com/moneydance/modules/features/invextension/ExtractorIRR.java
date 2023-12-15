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
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * Created by larus on 11/27/14.
 * <p/>
 * Compute the IRR for an investment.
 */
@SuppressWarnings("ALL")
public final class ExtractorIRR extends ExtractorModifiedDietzReturn {
    private TreeSet<ReturnValueElement> incomeValues;

    private boolean resultCurrent = false;
    private double result = 0;


    public ExtractorIRR(SecurityAccountWrapper securityAccount, SecurityReport securityReport,
                        int startDateInt, int endDateInt, ReturnWindowType windowType) {
        super(securityAccount, securityReport, startDateInt, endDateInt, windowType);
        incomeValues = new TreeSet<>();

    }

    @Override
    public boolean processNextTransaction(TransactionValues transaction, int transactionDateInt) {
        if (!super.processNextTransaction(transaction, transactionDateInt)) {
            return false;
        }

        if (startDateInt < transactionDateInt && transactionDateInt <= endDateInt) {
            long incomeFlows = transaction.getIncomeExpenseFlows();
            if (incomeFlows != 0) {
                incomeValues.add(new ReturnValueElement(transactionDateInt, incomeFlows,
                        transaction.getTxnID()));
            }
        }

        return true;
    }

    @Override
    public Double getResult() {
        if (!resultCurrent) {
            double mdReturn = super.getResult();
            if (mdReturn == SecurityReport.UndefinedReturn) {
                result = SecurityReport.UndefinedReturn;
            } else {
                result = computeFinancialResults(mdReturn);
            }
            resultCurrent = true;
        }
        return result;
    }

    // Compiler warning (unchecked cast) because Java v7 type system is too weak to express this.
    @Override
    public void aggregateResults(ExtractorBase<?> op) {
        ExtractorIRR operand = (ExtractorIRR) op;
        super.aggregateResults(operand);
        incomeValues.addAll(operand.incomeValues);
        resultCurrent = false;
    }

    private Double computeFinancialResults(double mdReturn) {
        LinkedList<ReturnValueElement> allTuples = collapseAnnualReturnElements();

        int outputArraySize = allTuples.size();
        if (startValue != 0) outputArraySize++;
        if (endValue != 0) outputArraySize++;
        double[] returns = new double[outputArraySize];
        double[] excelDates = new double[outputArraySize];
        int next = 0;

        if (startValue != 0) {
            excelDates[next] = DateUtils.getExcelDateValue(startDateInt);
            returns[next] = (double) -startValue;
            next++;
        }

        for (ReturnValueElement returnValueElement : allTuples) {
            excelDates[next] = DateUtils.getExcelDateValue(returnValueElement.date);
            returns[next] = (double) returnValueElement.value;
            next++;
        }

        if (endValue != 0) {
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

    @Override
    public String getAuditString(){
        StringBuilder auditString = new StringBuilder();
        auditString.append(description).append(nl);
        auditString.append("Result:").append(tab).append(result).append(nl);
        auditString.append("StartDate:").append(tab).append(DateUtils.convertToShort(startDateInt)).append(tab);
        auditString.append("EndDate:").append(tab).append(DateUtils.convertToShort(endDateInt)).append(tab).append(nl);
        auditString.append("StartValue:").append(tab).append(displayLong(startValue)).append(tab);
        auditString.append("EndValue:").append(tab).append(displayLong(endValue)).append(tab).append(nl);
        auditString.append("Income(less expenses)").append(tab).append(displayLong(incomeExpenseScalar)).append(nl);
        auditString.append(getDisplayDetails());
        auditString.append(nl).append("TxnId").append(tab).append("Date").append(tab).append("Value").append(nl);
        LinkedList<ReturnValueElement> returnValueElements = collapseAnnualReturnElements();
        for(ReturnValueElement returnValueElement : returnValueElements){
            auditString.append(returnValueElement.toString()).append(nl);
        }
        return auditString.toString();
    }

    private LinkedList<ReturnValueElement> collapseAnnualReturnElements() {
        ArrayList<ReturnValueElement> startList = new ArrayList<>();

        // reverse sign to comport with annual returns calc
        for (ReturnValueElement returnValueElement : capitalValues) {
            ReturnValueElement newReturnValueElement = returnValueElement.clone();
            newReturnValueElement.value *= -1;
            startList.add(newReturnValueElement);
        }
        for (ReturnValueElement returnValueElement : incomeValues) {
            ReturnValueElement newReturnValueElement = returnValueElement.clone();
            startList.add(newReturnValueElement);
        }

        Collections.sort(startList);
        LinkedList<ReturnValueElement> collapsedList = new LinkedList<>();
        // Collapse returns on same date to single entry to speed computation
        for (ReturnValueElement returnValueElement : startList) {
            if (collapsedList.isEmpty()) {
                collapsedList.add(returnValueElement.clone());
            } else {
                ReturnValueElement lastValue = collapsedList.peekLast();
                if (lastValue.date == returnValueElement.date) {
                    lastValue.value += returnValueElement.value;
                } else {
                    collapsedList.add(returnValueElement.clone());
                }
            }
        }
        return collapsedList;
    }
}