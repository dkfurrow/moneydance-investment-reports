/*
 * SecurityFromToReport.java
 * Copyright (c) 2014, Dale K. Furrow
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

import com.moneydance.modules.features.invextension.CompositeReport.COMPOSITE_TYPE;

import java.util.ArrayList;
import java.math.BigDecimal;


/**
 * Report detailing performance attributes based on a specific "from" and
 * "to" date
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class SecurityFromToReport extends SecurityReport {
    public BigDecimal income;                 // cumulative income
    private int fromDateInt;            // start of report period
    private int toDateInt;              // end of report period
    private BigDecimal startPos;              // starting position
    private BigDecimal endPos;                // ending position
    private BigDecimal startPrice;            // starting price
    private BigDecimal endPrice;              // ending price
    private BigDecimal startValue;            // starting value
    private BigDecimal endValue;              // ending value
    private BigDecimal buy;                   // cumulative cash effect of buys (including commission)
    private BigDecimal sell;                  // cumulative cash effect of sells (including commission)
    private BigDecimal shortSell;             // cumulative cash effect of shorts (including commission)
    private BigDecimal coverShort;            // cumulative cash effect of covers (including commission)
    private BigDecimal expense;               // cumulative expense
    private BigDecimal longBasis;             // ending average cost basis of long positions
    private BigDecimal shortBasis;            // ending average cost basis of short positions
    private BigDecimal realizedGain;          // cumulative realized gains
    private BigDecimal unrealizedGain;        // cumulative unrealized gains
    private BigDecimal totalGain;             // sum of realized and unrealized gains
    private double mdReturn;            // period total return (Mod-Dietz method)
    private double annualPercentReturn; // period annualized return (Mod-Dietz method)
    private DateMap arMap;              // date map of annual return data
    private DateMap mdMap;              // date map of Mod-Dietz return data
    private DateMap transMap;           // date map of transfer data


    /**
     * Generic constructor, which produces either the SecurityReport associated
     * with a given SecurityAccountWrapper or a blank report
     *
     * @param secAccountWrapper reference account
     * @param dateRange         date range
     */
    public SecurityFromToReport(SecurityAccountWrapper secAccountWrapper,
                                DateRange dateRange) {

        super(secAccountWrapper, dateRange);

        this.fromDateInt = dateRange.getFromDateInt();
        this.toDateInt = dateRange.getToDateInt();

        this.startPos = BigDecimal.ZERO;
        this.endPos = BigDecimal.ZERO;
        this.startPrice = BigDecimal.ZERO;
        this.endPrice = BigDecimal.ZERO;
        this.startValue=  BigDecimal.ZERO;
        this.endValue = BigDecimal.ZERO;

        this.buy = BigDecimal.ZERO;
        this.sell = BigDecimal.ZERO;
        this.shortSell = BigDecimal.ZERO;
        this.coverShort = BigDecimal.ZERO;

        this.income = BigDecimal.ZERO;
        this.expense = BigDecimal.ZERO;

        this.longBasis = BigDecimal.ZERO;
        this.shortBasis = BigDecimal.ZERO;
        this.realizedGain = BigDecimal.ZERO;
        this.unrealizedGain = BigDecimal.ZERO;
        this.totalGain = BigDecimal.ZERO;

        this.mdReturn = 0.0;
        this.annualPercentReturn = 0.0;

        this.arMap = new DateMap();
        this.mdMap = new DateMap();
        this.transMap = new DateMap();

        if (secAccountWrapper != null) {

            this.startPrice = secAccountWrapper.getPrice(fromDateInt);
            this.endPrice = secAccountWrapper.getPrice(toDateInt);

            // intialize intermediate calculation variables
            BigDecimal startCumUnrealGain = BigDecimal.ZERO;
            BigDecimal endCumUnrealizedGain = BigDecimal.ZERO;
            BigDecimal startLongBasis = BigDecimal.ZERO;
            BigDecimal startShortBasis = BigDecimal.ZERO;

            ArrayList<TransactionValues> fullTransactionSet = secAccountWrapper.getTransactionValues();
            ArrayList<Integer> fromToIndices = secAccountWrapper.getFromToIndices(dateRange);
            ArrayList<TransactionValues> subTransactionSet = secAccountWrapper.getFromToSubset(fromToIndices);

            boolean hasInitialPosition = !fullTransactionSet.isEmpty() &&
                    !(!fromToIndices.isEmpty() && fromToIndices.get(0) == 0);

            if (hasInitialPosition) {
                //security has initial position for this Date Range...so get position
                TransactionValues priorTransactionValues = fromToIndices.isEmpty() ? fullTransactionSet.get
                        (fullTransactionSet.size() - 1) : fullTransactionSet.get(fromToIndices.get(0) - 1);
                // split adjusts last position from TransValuesCum
                this.startPos = getSplitAdjustedPosition(priorTransactionValues.getPosition(),
                        priorTransactionValues.getDateint(), fromDateInt);
                this.startValue = this.startPrice.multiply(this.startPos);
                startLongBasis = priorTransactionValues.getLongBasis();
                startShortBasis = priorTransactionValues.getShortBasis();

                // Initializes ending balance sheet values to start values (in
                // case there are no transactions within report period).
                this.endPos = this.startPos;
                this.endValue = this.endPos.multiply(this.endPrice)
                        .setScale(moneyScale, BigDecimal.ROUND_HALF_EVEN);
                this.longBasis = priorTransactionValues.getLongBasis();
                this.shortBasis = priorTransactionValues.getShortBasis();

            }

            // Where transaction period intersects report period
            for (TransactionValues transactionValues : subTransactionSet) {
                // buySellFlows is net cash effect of buy/sell/short/cover, incl commission
                // totalFlows are all cash flows (including income/expense)
                // add values to date maps
                BigDecimal totalFlows = transactionValues.getTotalFlows();
                BigDecimal buySellFlows = transactionValues.getBuySellFlows();

                this.arMap.add(transactionValues.getDateint(),
                        totalFlows);
                this.mdMap.add(transactionValues.getDateint(), buySellFlows);
                this.transMap.add(transactionValues.getDateint(), transactionValues.getTransfer());

                // Add the cumulative Values (note buys are defined by change in
                // long basis, same with sells--commission is included).
                this.buy = this.buy.add(
                        transactionValues.getBuy().compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : transactionValues.getBuy().negate().subtract(transactionValues.getCommission()));
                this.sell = this.sell.add(
                        transactionValues.getSell().compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : transactionValues.getSell().negate().subtract(transactionValues.getCommission()));
                this.shortSell = this.shortSell.add(
                        transactionValues.getShortSell().compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : transactionValues.getShortSell().negate().subtract(transactionValues.getCommission()));
                this.coverShort = this.coverShort.add(
                        transactionValues.getCoverShort().compareTo(BigDecimal.ZERO) == 0
                        ? BigDecimal.ZERO
                        : transactionValues.getCoverShort().negate().subtract(transactionValues.getCommission()));
                this.income = this.income.add(transactionValues.getIncome());
                this.expense = this.expense.add(transactionValues.getExpense());
                this.realizedGain = this.realizedGain.add(transactionValues.getPerRealizedGain());
                this.endPos = getSplitAdjustedPosition(transactionValues.getPosition(),
                        transactionValues.getDateint(), toDateInt);
                this.endValue = this.endPos.multiply(this.endPrice)
                        .setScale(moneyScale, BigDecimal.ROUND_HALF_EVEN);
                this.longBasis = transactionValues.getLongBasis();
                this.shortBasis = transactionValues.getShortBasis();

            } // end--where transaction period intersects report period


            // Calculate the total period unrealized gain
            if (this.startPos.compareTo(BigDecimal.ZERO) > 0) {
                startCumUnrealGain = this.startValue.subtract(startLongBasis);
            } else if (this.startPos.compareTo(BigDecimal.ZERO) < 0) {
                startCumUnrealGain = this.startValue.subtract(startShortBasis);
            }

            if (this.endPos.compareTo(BigDecimal.ZERO) > 0) {
                endCumUnrealizedGain = this.endValue.subtract(this.longBasis);
            } else if (this.endPos.compareTo(BigDecimal.ZERO) < 0) {
                endCumUnrealizedGain = this.endValue.subtract(this.shortBasis);
            }
            this.unrealizedGain = endCumUnrealizedGain.subtract(startCumUnrealGain);
            this.totalGain = this.realizedGain.add(this.unrealizedGain);

            // Get performance data--first Mod Dietz Returns

            // Add the first value in return arrays (if startpos != 0)
            if (this.startPos.compareTo(BigDecimal.ZERO) != 0) {
                this.arMap.add(fromDateInt, this.startValue.negate());
                this.mdMap.add(fromDateInt, BigDecimal.ZERO); // adds dummy value for mod-dietz
            }
            // add the last value in return arrays (if endpos != 0)
            if (this.endPos.compareTo(BigDecimal.ZERO) != 0) {
                this.arMap.add(toDateInt, this.endValue);
                this.mdMap.add(toDateInt, BigDecimal.ZERO); // adds dummy value for mod-dietz
            }

            this.mdReturn = computeMDReturn(this.startValue.longValue(), this.endValue.longValue(), this.income.longValue(),
                    this.expense.longValue(), this.mdMap);
            // Now get annualized returns
            this.annualPercentReturn = computeAnnualReturn(this.arMap, this.mdReturn);

            // Remove start and end values from ar date map to enable aggregation
            if (this.startPos.compareTo(BigDecimal.ZERO) != 0) {
                this.arMap.add(fromDateInt, this.startValue);
            }
            // Remove start and end values from date map for ease of aggregation
            if (this.endPos.compareTo(BigDecimal.ZERO) != 0) {
                this.arMap.add(toDateInt, this.endValue.negate());
            }
        }
    }

    @Override
    public void addTo(SecurityReport securityReport) {
        SecurityFromToReport securityFromToReport = (SecurityFromToReport) securityReport;

        if (this.getSecurityAccountWrapper() != null)
            throw new UnsupportedOperationException(
                    "Illegal call to addTo method for SecurityReport");
        //if CurrencyWrappers are the same then prices and positions can be
        //added--if not, set prices and positions to zero
        if (this.getCurrencyWrapper() != null
                && securityFromToReport.getCurrencyWrapper() != null
                && this.getCurrencyWrapper().equals(securityFromToReport.getCurrencyWrapper())) {
            this.startPos = this.startPos.add(securityFromToReport.startPos);
            this.endPos = this.endPos.add(securityFromToReport.endPos);
            this.startPrice = securityFromToReport.startPrice;
            this.endPrice = securityFromToReport.endPrice;

        } else {
            this.startPos = BigDecimal.ZERO;
            this.endPos = BigDecimal.ZERO;
            this.startPrice = BigDecimal.ZERO;
            this.endPrice = BigDecimal.ZERO;
        }

        //populate other values from this SecurityReport
        this.startValue = this.startValue.add(securityFromToReport.startValue);
        this.endValue = this.endValue.add(securityFromToReport.endValue);
        this.buy = this.buy.add(securityFromToReport.buy);
        this.sell = this.sell.add(securityFromToReport.sell);
        this.shortSell = this.shortSell.add(securityFromToReport.shortSell);
        this.coverShort = this.coverShort.add(securityFromToReport.coverShort);
        this.income = this.income.add(securityFromToReport.income);
        this.expense = this.expense.add(securityFromToReport.expense);
        this.longBasis = this.longBasis.add(securityFromToReport.longBasis);
        this.shortBasis = this.shortBasis.add(securityFromToReport.shortBasis);
        this.realizedGain = this.realizedGain.add(securityFromToReport.realizedGain);
        this.unrealizedGain = this.unrealizedGain.add(securityFromToReport.unrealizedGain);
        this.totalGain = this.totalGain.add(securityFromToReport.totalGain);

        this.arMap = this.arMap.combine(securityFromToReport.arMap, "add");
        this.mdMap = this.mdMap.combine(securityFromToReport.mdMap, "add");
        this.transMap = this.transMap.combine(securityFromToReport.transMap, "add");
        //set returns to zero
        this.mdReturn = 0;
        this.annualPercentReturn = 0;
    }

    @Override
    public void recomputeAggregateReturns() {
        // get Mod-Dietz Returns
        double mdReturnVal = computeMDReturn(startValue.longValue(), endValue.longValue(), income.longValue(),
                expense.longValue(), mdMap);

        // SecurityFromToReport.mdReturn = thisReturn;
        mdReturn = mdReturnVal;

        // add start and end values to return date maps
        if (startValue.compareTo(BigDecimal.ZERO) != 0) {
            arMap.add(fromDateInt, startValue.negate());
        }
        if (endValue.compareTo(BigDecimal.ZERO) != 0) {
            arMap.add(toDateInt, endValue);
        }
        // get annualized returns
        annualPercentReturn = computeAnnualReturn(arMap, mdReturnVal);
    }

    @Override
    public Object[] toTableRow() throws SecurityException,
            IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException {
        addLineBody();
        return super.getOutputLine().toArray();
    }

    @Override
    public CompositeReport getCompositeReport(AggregationController aggregationController,
                                              COMPOSITE_TYPE compType) {
        return new CompositeReport(this, aggregationController, compType);
    }

    @Override
    public SecurityReport getAggregateSecurityReport() {
        SecurityFromToReport thisAggregate = new SecurityFromToReport(null, getDateRange());
        //add report body values (except Returns)
        thisAggregate.startPos = this.startPos;
        thisAggregate.endPos = this.endPos;
        thisAggregate.startPrice = this.startPrice;
        thisAggregate.endPrice = this.endPrice;
        thisAggregate.startValue = this.startValue;
        thisAggregate.endValue = this.endValue;
        thisAggregate.buy = this.buy;
        thisAggregate.sell = this.sell;
        thisAggregate.shortSell = this.shortSell;
        thisAggregate.coverShort = this.coverShort;
        thisAggregate.income = this.income;
        thisAggregate.expense = this.expense;
        thisAggregate.longBasis = this.longBasis;
        thisAggregate.shortBasis = this.shortBasis;
        thisAggregate.realizedGain = this.realizedGain;
        thisAggregate.unrealizedGain = this.unrealizedGain;
        thisAggregate.totalGain = this.totalGain;

        thisAggregate.mdMap = this.mdMap;
        thisAggregate.arMap = this.arMap;
        thisAggregate.transMap = this.transMap;

        //make aggregating classes the same except secAccountWrapper
        thisAggregate.setInvestmentAccountWrapper(this.getInvestmentAccountWrapper());
        thisAggregate.setSecurityAccountWrapper(null);
        thisAggregate.setSecurityTypeWrapper(this.getSecurityTypeWrapper());
        thisAggregate.setSecuritySubTypeWrapper(this.getSecuritySubTypeWrapper());
        thisAggregate.setTradeable(this.getTradeable());
        thisAggregate.setCurrencyWrapper(this.getCurrencyWrapper());

        return thisAggregate;
    }

    @Override
    public String getName() {
        if (this.getSecurityAccountWrapper() == null) {
            return "Null SecAccountWrapper";
        } else {
            return this.getInvestmentAccountWrapper().getName() + ": "
                    + this.getSecurityAccountWrapper().getName();
        }
    }

    @Override
    public void addLineBody() {
        ArrayList<Object> outputLine = super.getOutputLine();
        outputLine.add(this.startPos);
        outputLine.add(this.endPos);
        outputLine.add(this.startPrice);
        outputLine.add(this.endPrice);
        outputLine.add(this.startValue);
        outputLine.add(this.endValue);
        outputLine.add(this.buy);
        outputLine.add(this.sell);
        outputLine.add(this.shortSell);
        outputLine.add(this.coverShort);
        outputLine.add(this.income);
        outputLine.add(this.expense);
        outputLine.add(this.longBasis);
        outputLine.add(this.shortBasis);
        outputLine.add(this.realizedGain);
        outputLine.add(this.unrealizedGain);
        outputLine.add(this.totalGain);
        outputLine.add(this.mdReturn);
        outputLine.add(this.annualPercentReturn);

    }

    public int getFromDateInt() {
        return fromDateInt;
    }

    public int getToDateInt() {
        return toDateInt;
    }

    public BigDecimal getStartPos() {
        return startPos;
    }

    public BigDecimal getEndPos() {
        return endPos;
    }

    public BigDecimal getStartPrice() {
        return startPrice;
    }

    public BigDecimal getEndPrice() {
        return endPrice;
    }

    public BigDecimal getStartValue() {
        return startValue;
    }

    public BigDecimal getEndValue() {
        return endValue;
    }

    public BigDecimal getBuy() {
        return buy;
    }

    public BigDecimal getSell() {
        return sell;
    }

    public BigDecimal getShortSell() {
        return shortSell;
    }

    public BigDecimal getCoverShort() {
        return coverShort;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public BigDecimal getLongBasis() {
        return longBasis;
    }

    public BigDecimal getShortBasis() {
        return shortBasis;
    }

    public BigDecimal getRealizedGain() {
        return realizedGain;
    }

    public BigDecimal getUnrealizedGain() {
        return unrealizedGain;
    }

    public BigDecimal getTotalGain() {
        return totalGain;
    }

    public double getMdReturn() {
        return mdReturn;
    }

    public double getAnnualPercentReturn() {
        return annualPercentReturn;
    }

    public DateMap getArMap() {
        return arMap;
    }

    public DateMap getMdMap() {
        return mdMap;
    }

    public DateMap getTransMap() {
        return transMap;
    }

}
