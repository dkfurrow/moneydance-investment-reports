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


/**
 * Report detailing performance attributes based on a specific "from" and
 * "to" date
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class SecurityFromToReport extends SecurityReport {
    public long income;                 // cumulative income
    private int fromDateInt;            // start of report period
    private int toDateInt;              // end of report period
    private long startPos;              // starting position
    private long endPos;                // ending position
    private long startPrice;            // starting price
    private long endPrice;              // ending price
    private long startValue;            // starting value
    private long endValue;              // ending value
    private long buy;                   // cumulative cash effect of buys (including commission)
    private long sell;                  // cumulative cash effect of sells (including commission)
    private long shortSell;             // cumulative cash effect of shorts (including commission)
    private long coverShort;            // cumulative cash effect of covers (including commission)
    private long expense;               // cumulative expense
    private long longBasis;             // ending average cost basis of long positions
    private long shortBasis;            // ending average cost basis of short positions
    private long realizedGain;          // cumulative realized gains
    private long unrealizedGain;        // cumulative unrealized gains
    private long totalGain;             // sum of realized and unrealized gains
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

        this.startPos = 0;
        this.endPos = 0;
        this.startPrice = 0;
        this.endPrice = 0;
        this.startValue = 0;
        this.endValue = 0;

        this.buy = 0;
        this.sell = 0;
        this.shortSell = 0;
        this.coverShort = 0;

        this.income = 0;
        this.expense = 0;

        this.longBasis = 0;
        this.shortBasis = 0;
        this.realizedGain = 0;
        this.unrealizedGain = 0;
        this.totalGain = 0;

        this.mdReturn = 0;
        this.annualPercentReturn = 0;

        this.arMap = new DateMap();
        this.mdMap = new DateMap();
        this.transMap = new DateMap();

        if (secAccountWrapper != null) {

            this.startPrice = secAccountWrapper.getPrice(fromDateInt);
            this.endPrice = secAccountWrapper.getPrice(toDateInt);

            // intialize intermediate calculation variables
            long startCumUnrealGain = 0;
            long endCumUnrealizedGain = 0;
            long startLongBasis = 0;
            long startShortBasis = 0;

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
                this.startValue = this.startPrice * this.startPos;
                startLongBasis = priorTransactionValues.getLongBasis();
                startShortBasis = priorTransactionValues.getShortBasis();

                // Initializes ending balance sheet values to start values (in
                // case there are no transactions within report period).
                this.endPos = this.startPos;
                this.endValue = this.endPos * this.endPrice;
                this.longBasis = priorTransactionValues.getLongBasis();
                this.shortBasis = priorTransactionValues.getShortBasis();

            }

            // Where transaction period intersects report period
            for (TransactionValues transactionValues : subTransactionSet) {
                // buySellFlows is net cash effect of buy/sell/short/cover, incl commission
                // totalFlows are all cash flows (including income/expense)
                // add values to date maps
                long totalFlows = transactionValues.getTotalFlows();
                long buySellFlows = transactionValues.getBuySellFlows();

                this.arMap.add(transactionValues.getDateint(),
                        totalFlows);
                this.mdMap.add(transactionValues.getDateint(), buySellFlows);
                this.transMap.add(transactionValues.getDateint(), transactionValues.getTransfer());

                // Add the cumulative Values (note buys are defined by change in
                // long basis, same with sells--commission is included).
                this.buy += transactionValues.getBuy() == 0
                        ? 0
                        : -transactionValues.getBuy() - transactionValues.getCommission();
                this.sell += transactionValues.getSell() == 0
                        ? 0
                        : -transactionValues.getSell() - transactionValues.getCommission();
                this.shortSell += transactionValues.getShortSell() == 0
                        ? 0
                        : -transactionValues.getShortSell() - transactionValues.getCommission();
                this.coverShort += transactionValues.getCoverShort() == 0
                        ? 0
                        : -transactionValues.getCoverShort() - transactionValues.getCommission();
                this.income += transactionValues.getIncome();
                this.expense += transactionValues.getExpense();
                this.realizedGain += transactionValues.getPerRealizedGain();
                this.endPos = getSplitAdjustedPosition(transactionValues.getPosition(),
                        transactionValues.getDateint(), toDateInt);
                this.endValue = this.endPos * this.endPrice;
                this.longBasis = transactionValues.getLongBasis();
                this.shortBasis = transactionValues.getShortBasis();

            } // end--where transaction period intersects report period


            // Calculate the total period unrealized gain
            if (this.startPos > 0) {
                startCumUnrealGain = this.startValue - startLongBasis;
            } else if (this.startPos < 0) {
                startCumUnrealGain = this.startValue - startShortBasis;
            }

            if (this.endPos > 0) {
                endCumUnrealizedGain = this.endValue - this.longBasis;
            } else if (this.endPos < 0) {
                endCumUnrealizedGain = this.endValue - this.shortBasis;
            }
            this.unrealizedGain = endCumUnrealizedGain - startCumUnrealGain;
            this.totalGain = this.realizedGain + this.unrealizedGain;

            // Get performance data--first Mod Dietz Returns

            // Add the first value in return arrays (if startpos != 0)
            if (this.startPos != 0) {
                this.arMap.add(fromDateInt, -this.startValue);
                this.mdMap.add(fromDateInt, 0L); // adds dummy value for mod-dietz
            }
            // add the last value in return arrays (if endpos != 0)
            if (this.endPos != 0) {
                this.arMap.add(toDateInt, this.endValue);
                this.mdMap.add(toDateInt, 0L); // adds dummy value for mod-dietz
            }

            this.mdReturn = computeMDReturn(this.startValue, this.endValue, this.income,
                    this.expense, this.mdMap);
            // Now get annualized returns
            this.annualPercentReturn = computeAnnualReturn(this.arMap, this.mdReturn);

            // Remove start and end values from ar date map to enable aggregation
            if (this.startPos != 0) {
                this.arMap.add(fromDateInt, +this.startValue);
            }
            // Remove start and end values from date map for ease of aggregation
            if (this.endPos != 0) {
                this.arMap.add(toDateInt, -this.endValue);
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
                && this.getCurrencyWrapper()
                .equals(securityFromToReport.getCurrencyWrapper())) {
            this.startPos += securityFromToReport.startPos;
            this.endPos += securityFromToReport.endPos;
            this.startPrice = securityFromToReport.startPrice;
            this.endPrice = securityFromToReport.endPrice;

        } else {
            this.startPos = 0;
            this.endPos = 0;
            this.startPrice = 0;
            this.endPrice = 0;
        }

        //populate other values from this SecurityReport
        this.startValue += securityFromToReport.startValue;
        this.endValue += securityFromToReport.endValue;
        this.buy += securityFromToReport.buy;
        this.sell += securityFromToReport.sell;
        this.shortSell += securityFromToReport.shortSell;
        this.coverShort += securityFromToReport.coverShort;
        this.income += securityFromToReport.income;
        this.expense += securityFromToReport.expense;
        this.longBasis += securityFromToReport.longBasis;
        this.shortBasis += securityFromToReport.shortBasis;
        this.realizedGain += securityFromToReport.realizedGain;
        this.unrealizedGain += securityFromToReport.unrealizedGain;
        this.totalGain += securityFromToReport.totalGain;

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
        double mdReturnVal = computeMDReturn(startValue, endValue, income,
                expense, mdMap);

        // SecurityFromToReport.mdReturn = thisReturn;
        mdReturn = mdReturnVal;

        // add start and end values to return date maps
        if (startValue != 0) {
            arMap.add(fromDateInt, -startValue);
        }
        if (endValue != 0) {
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

    public long getStartPos() {
        return startPos;
    }

    public long getEndPos() {
        return endPos;
    }

    public long getStartPrice() {
        return startPrice;
    }

    public long getEndPrice() {
        return endPrice;
    }

    public long getStartValue() {
        return startValue;
    }

    public long getEndValue() {
        return endValue;
    }

    public long getBuy() {
        return buy;
    }

    public long getSell() {
        return sell;
    }

    public long getShortSell() {
        return shortSell;
    }

    public long getCoverShort() {
        return coverShort;
    }

    public long getIncome() {
        return income;
    }

    public long getExpense() {
        return expense;
    }

    public long getLongBasis() {
        return longBasis;
    }

    public long getShortBasis() {
        return shortBasis;
    }

    public long getRealizedGain() {
        return realizedGain;
    }

    public long getUnrealizedGain() {
        return unrealizedGain;
    }

    public long getTotalGain() {
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
