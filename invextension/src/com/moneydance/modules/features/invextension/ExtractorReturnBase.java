/*
 * ExtractorReturnBase.java
 * Copyright (c) 2015, Dale K. Furrow
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
import java.util.LinkedList;
import java.util.TreeSet;

/**
 * Created by larus on 3/1/15.
 * <p/>
 * Class that allows dynamic selection between Modified-Dietz and ordinary return calculations.
 */
@SuppressWarnings("ALL")
public class ExtractorReturnBase extends ExtractorBase<Double> {
    protected ReturnWindowType returnWindowType;
    protected TreeSet<ReturnValueElement> capitalValues;
    protected TransactionValues firstTransaction = null;
    protected long incomeExpenseScalar = 0;

    protected long startPosition = 0;
    protected long startValue = 0;
    protected long endPosition = 0;
    protected long endValue = 0;

    protected boolean resultCurrent = false;
    protected double result = 0;
    protected String description = "";

    protected double priceScale;

    public static final String tab = "\t";
    public static final String nl = "\n";


    public ExtractorReturnBase(SecurityAccountWrapper secAccountWrapper, SecurityReport securityReport, int startDateInt, int endDateInt,
                               ReturnWindowType returnWindowType) {
        super(secAccountWrapper, startDateInt, endDateInt);
        if (securityReport != null) {
            description = securityReport.getDescription();
            priceScale = securityReport.getPriceScale();
        }
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
            case STUB:
                this.startDateInt = startDateInt;
                this.endDateInt = endDateInt;
                break;
        }
    }

    public ExtractorReturnBase(ExtractorReturnBase extractorReturnBase){
        super(extractorReturnBase);
        this.returnWindowType = extractorReturnBase.returnWindowType;
        this.capitalValues = extractorReturnBase.capitalValues;
        this.firstTransaction = extractorReturnBase.firstTransaction;
        this.incomeExpenseScalar = extractorReturnBase.incomeExpenseScalar;
        this.startPosition = extractorReturnBase.startPosition;
        this.startValue = extractorReturnBase.startValue;
        this.endPosition = extractorReturnBase.endPosition;
        this.endValue = extractorReturnBase.endValue;
        this.resultCurrent = false;
        this.result = 0;
        this.description = extractorReturnBase.description;
        this.priceScale = extractorReturnBase.priceScale;
    }

    protected static ExtractorReturnBase factory(SecurityAccountWrapper secAccountWrapper, SecurityReport securityReport, int startDateInt,
                                                 int endDateInt, ReturnWindowType returnWindowType, boolean useOrdinary) {
        if (useOrdinary) {
            return new ExtractorOrdinaryReturn(secAccountWrapper, securityReport, startDateInt, endDateInt, returnWindowType);
        } else {
            return new ExtractorModifiedDietzReturn(secAccountWrapper, securityReport, startDateInt, endDateInt, returnWindowType);
        }
    }

    public long getIncomeExpenseScalar() {
        return incomeExpenseScalar;
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
            long cashFlow = transaction.getBuySellFlows();
            if (cashFlow != 0) capitalValues.add(new ReturnValueElement(transactionDateInt,
                    cashFlow, transaction.getTxnID()));
        }

        return true;
    }

    // Compiler warning (unchecked cast) because Java v7 type system is too weak to express this.
    public void aggregateResults(ExtractorBase<?> op) {
        ExtractorReturnBase operand = (ExtractorReturnBase) op;

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
        LinkedList<ReturnValueElement> returnValueElements = collapseTotalReturnElements();
        for(ReturnValueElement returnValueElement : returnValueElements){
            auditString.append(returnValueElement.toString()).append(nl);
        }
        return auditString.toString();
    }

    protected String displayLong(long value) {
        Double valueDouble = priceScale != 0.0 ? (Long) value / priceScale : Double.NaN;
        return valueDouble.toString();
    }

    public String getDisplayDetails(){
        boolean isIRR = this instanceof ExtractorIRR;
        String flowsType = isIRR ? "total flows (buy/sells + income)"
                : "capital flows (buy/sells)";
        String mapExplanation = " map follows - (if multiple transaction occur on a given date," + nl
                +"only the first tranaction id is listed)." + nl;
        String valueExplanation = isIRR ? "Purchases indicated as negative, Sales postive." + nl
                : "Purchases indicated as positive, Sales negative." + nl;
        return flowsType + mapExplanation + valueExplanation;
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
            }
            switch (returnWindowType) {
                case DEFAULT:
                    if (startValue == 0) return SecurityReport.UndefinedReturn;
                    if (endValue == 0 && !capitalValues.isEmpty()) {
                        this.endDateInt = capitalValues.last().date;
                    }
                    break;
                case STUB: //returns same as 'ANY' IFF DEFAULT returns undefined
                    if (startValue != 0) return SecurityReport.UndefinedReturn;
                    if (startValue == 0 && !capitalValues.isEmpty()) {
                        this.startDateInt = DateUtils.getPrevBusinessDay(capitalValues.first().date);
                    }
                    if (endValue == 0 && !capitalValues.isEmpty()) {
                        this.endDateInt = capitalValues.last().date;
                    }
                    break;
                case ANY:
                    if (startValue == 0 && !capitalValues.isEmpty()) {
                        this.startDateInt = DateUtils.getPrevBusinessDay(capitalValues.first().date);
                    }
                    if (endValue == 0 && !capitalValues.isEmpty()) {
                        this.endDateInt = capitalValues.last().date;
                    }
                    break;
                case ALL:
                    if (endValue == 0 && !capitalValues.isEmpty()) {
                        this.endDateInt = capitalValues.last().date;
                    }
                    break;
            }
            result = computeReturn();
            resultCurrent = true;
        }

        return result;
    }

    public TreeSet<ReturnValueElement> getCapitalValues() {
        return capitalValues;
    }

    public String getDescription() {
        return description;
    }

    public LinkedList<ReturnValueElement> collapseTotalReturnElements() {
        LinkedList<ReturnValueElement> collapsedList = new LinkedList<>();
        // Collapse returns on same date to single entry to speed computation
        for (ReturnValueElement returnValueElement : capitalValues) {
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

    public double computeReturn() {
        throw new UnsupportedOperationException();
    }


    public enum ReturnWindowType {
        DEFAULT("Requires NonZero Initial Value at Window Start"),
        ALL("Adjust Start Date to Day Before First Transaction"),
        ANY("Any open position between window start and window end"),
        STUB("Returns 'ANY' only if Zero Initial Value at Window Start");

        private final String description;

        ReturnWindowType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return this.description;
        }
    }

    public class ReturnValueElement implements Comparable<ReturnValueElement> {
        public final int date;
        public long value;
        public String txnId;

        public ReturnValueElement(int d, long v, String id) {
            date = d;
            value = v;
            txnId = id;
        }

        public int compareTo(@NotNull ReturnValueElement operand) {
            if (date != operand.date) {
                return date - operand.date;
            } else {
                return txnId.compareTo(operand.txnId);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReturnValueElement)) return false;

            ReturnValueElement that = (ReturnValueElement) o;

            if (date != that.date) return false;
            if (txnId.compareTo(that.txnId) != 0) return false;
            if (value != that.value) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = date;
            result = 31 * result + (int) (value ^ (value >>> 32));
            temp = txnId.hashCode();
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        public String toString(){
            return txnId + tab + DateUtils.convertToShort(date) + tab +
                    ExtractorReturnBase.this.displayLong(value);
        }

        public ReturnValueElement clone() {
            return new ReturnValueElement(this.date, this.value, this.txnId);
        }

    }
}
