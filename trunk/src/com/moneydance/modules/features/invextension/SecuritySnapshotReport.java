/*
 * SecuritySnapshotReport.java
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

import com.moneydance.apps.md.model.InvestTxnType;
import com.moneydance.apps.md.model.TxnUtil;
import com.moneydance.modules.features.invextension.CompositeReport.COMPOSITE_TYPE;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Report detailing performance attributes based on a specific snapshot
 * date
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class SecuritySnapshotReport extends SecurityReport {

    private int snapDateInt;


    private double lastPrice;           //ending price
    private double endPos;              //ending position
    private double endValue;            //ending value

    private double longBasis;           //final long basis
    private double shortBasis;          //final short basis

    //one day values
    private double absPriceChange;      //absolute price change (from previous day to snapDate)
    private double pctPriceChange;      //percent price change (from previous day to snapDate)
    private double absValueChange;      //absolute value change (from previous day to snapDate)

    //total numbers
    private double income;              //total income (all dates)
    private double unrealizedGain;      // unrealized gain
    private double realizedGain;        //realized gain
    private double totalGain;           //total absolute gain (all dates)
    private double totRetAll;           //total Mod-Dietz return (all dates)
    private double annRetAll;           //annualized return (all dates)

    //Dividend Yields
    private double annualizedDividend = 0.0;
    private double dividendYield = 0.0;
    private double yieldOnBasis = 0.0;


    //returns
    private double totRet1Day;          //Mod-Dietz return (1 day)
    private double totRetWk;            //Mod-Dietz return (1 week)
    private double totRet4Wk;           //Mod-Dietz return (1 month)
    private double totRet3Mnth;         //Mod-Dietz return (3 month)
    private double totRetYTD;           //Mod-Dietz return (Year-to-Date)
    private double totRetYear;          //Mod-Dietz return (1 Year)
    private double totRet3year;         //Mod-Dietz return (1 Years)

    // intermediate values
    private CategoryMap<Integer> returnsStartDate;      //maps return category to start dates
    private CategoryMap<Double> startValues;            //maps return category to start values
    private CategoryMap<Double> startPoses;             //maps return category to start positions
    private CategoryMap<Double> startPrices;            //maps return category to start positions
    private CategoryMap<Double> incomes;                //maps return category to income
    private CategoryMap<Double> expenses;               //maps return category to expense
    private CategoryMap<Double> mdReturns;              //maps return category to Mod-Dietz returns


    private CategoryMap<DateMap> mdMap;                 //maps return category to Mod-Dietz date map
    private CategoryMap<DateMap> arMap;                 //maps return category to Annualized Return Date Map
    private CategoryMap<DateMap> transMap;              //maps return category to transfer date map


    /**
     * Generic constructor, which produces either the SecurityReport associated
     * with a given SecurityAccountWrapper or a blank report
     *
     * @param secAccountWrapper Security Account Wrapper
     * @param dateRange         input date range
     */
    public SecuritySnapshotReport(SecurityAccountWrapper secAccountWrapper,
                                  DateRange dateRange) {

        super(secAccountWrapper, dateRange);

        this.snapDateInt = dateRange.getSnapDateInt();

        this.lastPrice = 0.0;
        this.endPos = 0.0;
        this.endValue = 0.0;

        this.longBasis = 0.0;
        this.shortBasis = 0.0;

        this.absPriceChange = 0.0;
        this.pctPriceChange = 0.0;
        this.absValueChange = 0.0;

        this.income = 0.0;
        this.unrealizedGain = 0.0;
        this.realizedGain = 0.0;
        this.totalGain = 0.0;
        this.totRetAll = 0.0;
        this.annRetAll = 0.0;

        this.annualizedDividend = 0.0;
        this.dividendYield = 0.0;
        this.yieldOnBasis = 0.0;

        this.totRet1Day = 0.0;
        this.totRetWk = 0.0;
        this.totRet4Wk = 0.0;
        this.totRet3Mnth = 0.0;
        this.totRetYTD = 0.0;
        this.totRetYear = 0.0;
        this.totRet3year = 0.0;

        this.returnsStartDate = new CategoryMap<>();
        this.startValues = new CategoryMap<>();
        this.startPoses = new CategoryMap<>();
        this.startPrices = new CategoryMap<>();
        this.incomes = new CategoryMap<>();
        this.expenses = new CategoryMap<>();
        this.mdReturns = new CategoryMap<>();

        this.mdMap = new CategoryMap<>();
        this.arMap = new CategoryMap<>();
        this.transMap = new CategoryMap<>();

        AnnualDividendCalculator annualDividendCalculator = new AnnualDividendCalculator();


        // Calculate return dates (use snapDate for "ALL" as it is latest
        // possible
        int fromDateInt = snapDateInt;
        int prevFromDateInt = DateUtils.getPrevBusinessDay(snapDateInt);
        int wkFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
                .addDaysInt(snapDateInt, -7));
        int mnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
                .addMonthsInt(snapDateInt, -1));
        int threeMnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
                .addMonthsInt(snapDateInt, -3));
        int oneYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
                .addMonthsInt(snapDateInt, -12));
        int threeYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
                .addMonthsInt(snapDateInt, -36));
        int ytdFromDateInt = DateUtils.getStartYear(snapDateInt);

        // put dates in return map
        this.returnsStartDate = new CategoryMap<>();

        this.returnsStartDate.put("All", fromDateInt);
        this.returnsStartDate.put("PREV", prevFromDateInt);
        this.returnsStartDate.put("1Wk", wkFromDateInt);
        this.returnsStartDate.put("4Wk", mnthFromDateInt);
        this.returnsStartDate.put("3Mnth", threeMnthFromDateInt);
        this.returnsStartDate.put("1Yr", oneYearFromDateInt);
        this.returnsStartDate.put("3Yr", threeYearFromDateInt);
        this.returnsStartDate.put("YTD", ytdFromDateInt);

        // initialize ArrayLists values to zero
        for (String retCat : this.returnsStartDate.keySet()) {
            startPoses.put(retCat, 0.0);
            this.startValues.put(retCat, 0.0);
            this.incomes.put(retCat, 0.0);
            this.expenses.put(retCat, 0.0);
            this.mdReturns.put(retCat, 0.0);

            this.arMap.put(retCat, new DateMap());
            this.mdMap.put(retCat, new DateMap());
            this.transMap.put(retCat, new DateMap());
        }

        if (secAccountWrapper != null) {

            this.lastPrice = secAccountWrapper.getPrice(snapDateInt);

            // create dates for returns calculations
            // ensures all dates for appropriate variables
            ArrayList<TransactionValues> transSet = secAccountWrapper.getTransactionValues();

            fromDateInt = transSet.isEmpty() ? snapDateInt : DateUtils
                    .getPrevBusinessDay(transSet.get(0).getDateint());

            // put dates in return map
            this.returnsStartDate.put("All", fromDateInt);

            // these values dependent only on snapDate

            longBasis = 0.0;
            shortBasis = 0.0;
            double annualPercentReturn;

            // fill startPrice Array List
            for (String retCat : this.returnsStartDate.keySet()) {
                int thisDateInt = this.returnsStartDate.get(retCat);
                startPrices.put(retCat, secAccountWrapper.getPrice(thisDateInt));
            }

            // iterate through transaction values list
            for (TransactionValues transactionValues : transSet) {
                double totalFlows = transactionValues.getBuy() + transactionValues.getSell()
                        + transactionValues.getShortSell() + transactionValues.getCoverShort()
                        + transactionValues.getCommission() + transactionValues.getIncome()
                        + transactionValues.getExpense();

                // iterate through return dates
                for (String retCat : this.returnsStartDate.keySet()) {
                    int thisFromDateInt = this.returnsStartDate.get(retCat);
                    int transValuesDate = transactionValues.getDateint();

                    // where transactions are before report dates
                    if (transValuesDate <= thisFromDateInt) {
                        double adjustedPos = getSplitAdjustedPosition(transactionValues.getPosition(),
                                transValuesDate, thisFromDateInt);
                        startPoses.put(retCat, adjustedPos); // split adjusts last position
                        // from
                        // TransValuesCum
                        this.startValues.put(retCat, startPrices.get(retCat)
                                * startPoses.get(retCat));

                    }

                    // where transaction period intersects report period
                    if (transValuesDate > thisFromDateInt
                            && transValuesDate <= snapDateInt) {

                        // MDCalc variable--net effect of calculation is to
                        // return buys and sells, including commission
                        double cf = -(transactionValues.getBuy() + transactionValues.getSell()
                                + transactionValues.getShortSell()
                                + transactionValues.getCoverShort() + transactionValues.getCommission());

                        // add variables to arrays needed for returns
                        // calculation

                        this.arMap.get(retCat).add(transValuesDate, totalFlows);
                        this.mdMap.get(retCat).add(transValuesDate, cf);
                        this.transMap.get(retCat).add(transValuesDate, transactionValues.getTransfer());

                        this.incomes.put(retCat, this.incomes.get(retCat) + transactionValues.getIncome());
                        this.expenses.put(retCat, this.expenses.get(retCat) + transactionValues.getExpense());

                        if ("All".equals(retCat)) {//For values which are common to all returns transactions
                            //or are unused in returns calculations
                            realizedGain += transactionValues.getPerRealizedGain();
                            this.endPos = getSplitAdjustedPosition(transactionValues.getPosition(),
                                    transValuesDate, snapDateInt);
                            this.endValue = this.endPos * this.lastPrice;
                            longBasis = transactionValues.getLongBasis();
                            shortBasis = transactionValues.getShortBasis();
                            annualDividendCalculator.analyzeTransaction(transactionValues);
                        }

                    } // end--where transaction period intersects report period
                } // end of start date iterative loop
            } // end of input transaction set loop

            if (this.endPos > 0) {
                unrealizedGain = this.endValue - longBasis;
            } else if (this.endPos < 0) {
                unrealizedGain = this.endValue - shortBasis;
            }


            this.totalGain = realizedGain + unrealizedGain;

            // now go through arrays and get returns/calc values
            for (String retCat : this.returnsStartDate.keySet()) {
                int thisFromDateInt = this.returnsStartDate.get(retCat);
                // add the first value in return arrays (if startpos != 0)
                if (startPoses.get(retCat) != 0) {
                    this.arMap.get(retCat).add(thisFromDateInt,
                            -this.startValues.get(retCat));
                    // dummy values for Mod-dietz
                    this.mdMap.get(retCat).add(thisFromDateInt, 0.0);
                }
                // add the last value in return arrays (if endpos != 0)
                if (this.endPos != 0) {
                    this.arMap.get(retCat).add(snapDateInt, this.endValue);
                    // dummy values for Mod-dietz
                    this.mdMap.get(retCat).add(snapDateInt, 0.0);
                }

                // get MD returns on all start dates, only get annualized return
                // on all dates

                this.mdReturns.put(retCat, computeMDReturn(this.startValues.get(retCat),
                        this.endValue, this.incomes.get(retCat), this.expenses.get(retCat),
                        this.mdMap.get(retCat)));
                //get annualized returns only for total period
                if ("All".equals(retCat)) {
                    annualPercentReturn = computeAnnualReturn(this.arMap.get(retCat),
                            this.mdReturns.get("All"));
                    this.annRetAll = annualPercentReturn;
                    this.income = this.incomes.get(retCat);
                }

                // remove start and end values from return date maps for ease of
                // aggregation
                if (startPoses.get(retCat) != 0) {
                    this.arMap.get(retCat).add(thisFromDateInt,
                            +this.startValues.get(retCat));
                }
                // remove start and end values from return date maps for ease of
                // aggregation
                if (this.endPos != 0) {
                    this.arMap.get(retCat).add(snapDateInt, -this.endValue);
                }
            } // end of start date iterateration

            // Produce output, get returns

            if (this.returnsStartDate.get("PREV") == null) {
                this.absPriceChange = Double.NaN;
                this.absValueChange = Double.NaN;
                this.pctPriceChange = Double.NaN;
            } else {
                double prevPrice = secAccountWrapper.getPrice(this.returnsStartDate.get("PREV"));
                this.absPriceChange = this.lastPrice - prevPrice;
                this.absValueChange = this.endPos * this.absPriceChange;
                this.pctPriceChange = this.lastPrice / prevPrice - 1.0;
            }
            this.totRet1Day = this.mdReturns.get("PREV") == null ? Double.NaN
                    : this.mdReturns.get("PREV");
            this.totRetAll = this.mdReturns.get("All") == null ? Double.NaN
                    : this.mdReturns.get("All");
            this.totRetWk = this.mdReturns.get("1Wk") == null ? Double.NaN
                    : this.mdReturns.get("1Wk");
            this.totRet4Wk = this.mdReturns.get("4Wk") == null ? Double.NaN
                    : this.mdReturns.get("4Wk");
            this.totRet3Mnth = this.mdReturns.get("3Mnth") == null ? Double.NaN
                    : this.mdReturns.get("3Mnth");
            this.totRetYear = this.mdReturns.get("1Yr") == null ? Double.NaN
                    : this.mdReturns.get("1Yr");
            this.totRet3year = this.mdReturns.get("3Yr") == null ? Double.NaN
                    : this.mdReturns.get("3Yr");
            this.totRetYTD = this.mdReturns.get("YTD") == null ? Double.NaN
                    : this.mdReturns.get("YTD");

            annualDividendCalculator.updateYieldInformation();
        }

    }

    @Override
    public CompositeReport getCompositeReport(AggregationController aggregationController,
                                              COMPOSITE_TYPE compType) {
        return new CompositeReport(this, aggregationController, compType);
    }

    @Override
    public void recomputeAggregateReturns() {
        for (String retCat : returnsStartDate.keySet()) {
            // get MD returns on all start dates, only get annualized return for
            // "All" dates
            mdReturns.put(retCat, computeMDReturn(startValues.get(retCat), endValue,
                    incomes.get(retCat), expenses.get(retCat), mdMap.get(retCat)));

            if ("All".equals(retCat)) {
                // add start and end values to return date maps
                if (startValues.get(retCat) != 0.0) {
                    arMap.get(retCat).add(returnsStartDate.get(retCat),
                            -startValues.get(retCat));
                }
                if (endValue != 0.0) {
                    arMap.get(retCat).add(snapDateInt, endValue);
                }
                // get return
                annRetAll = computeAnnualReturn(arMap.get(retCat),
                        mdReturns.get("All"));
                income = incomes.get(retCat);

            }
        }
        totRet1Day = returnsStartDate.get("PREV") == null ? Double.NaN
                : mdReturns.get("PREV");
        totRetAll = returnsStartDate.get("All") == null ? Double.NaN
                : mdReturns.get("All");
        totRetWk = returnsStartDate.get("1Wk") == null ? Double.NaN : mdReturns
                .get("1Wk");
        totRet4Wk = returnsStartDate.get("4Wk") == null ? Double.NaN
                : mdReturns.get("4Wk");
        totRet3Mnth = returnsStartDate.get("3Mnth") == null ? Double.NaN
                : mdReturns.get("3Mnth");
        totRetYear = returnsStartDate.get("1Yr") == null ? Double.NaN
                : mdReturns.get("1Yr");
        totRet3year = returnsStartDate.get("3Yr") == null ? Double.NaN
                : mdReturns.get("3Yr");
        totRetYTD = returnsStartDate.get("YTD") == null ? Double.NaN
                : mdReturns.get("YTD");

    }

    @Override
    public SecurityReport getAggregateSecurityReport() {
        SecuritySnapshotReport thisAggregate = new SecuritySnapshotReport(null,
                getDateRange());
        // add report body values (except Returns)
        thisAggregate.lastPrice = this.lastPrice;
        thisAggregate.endPos = this.endPos;
        thisAggregate.endValue = this.endValue;

        thisAggregate.longBasis = this.longBasis;
        thisAggregate.shortBasis = this.shortBasis;

        // one day values
        thisAggregate.absPriceChange = this.absPriceChange;
        thisAggregate.pctPriceChange = this.pctPriceChange;
        thisAggregate.absValueChange = this.absValueChange;

        // total numbers
        thisAggregate.income = this.income;
        thisAggregate.unrealizedGain = this.unrealizedGain;
        thisAggregate.realizedGain = this.realizedGain;
        thisAggregate.totalGain = this.totalGain;
        thisAggregate.annualizedDividend = this.annualizedDividend;
        thisAggregate.dividendYield = this.dividendYield;
        thisAggregate.yieldOnBasis = this.yieldOnBasis;

        // intermediate values
        thisAggregate.returnsStartDate = this.returnsStartDate;
        thisAggregate.startValues = this.startValues;
        thisAggregate.startPoses = this.startPoses;
        thisAggregate.startPrices = this.startPrices;
        thisAggregate.incomes = this.incomes;
        thisAggregate.expenses = this.expenses;
        thisAggregate.mdReturns = this.mdReturns;

        thisAggregate.mdMap = this.mdMap;
        thisAggregate.arMap = this.arMap;
        thisAggregate.transMap = this.transMap;

        // make aggregating classes the same except secAccountWrapper
        thisAggregate.setInvestmentAccountWrapper(this.getInvestmentAccountWrapper());
        thisAggregate.setSecurityAccountWrapper(null);
        thisAggregate.setSecurityTypeWrapper(this.getSecurityTypeWrapper());
        thisAggregate.setSecuritySubTypeWrapper(this
                .getSecuritySubTypeWrapper());
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
    public void addTo(SecurityReport securityReport) {
        SecuritySnapshotReport operand = (SecuritySnapshotReport) securityReport;
        if (this.getSecurityAccountWrapper() != null)
            throw new UnsupportedOperationException(
                    "Illegal call to addTo method for SecurityReport");

        if (this.getCurrencyWrapper() != null && operand.getCurrencyWrapper() != null
                && this.getCurrencyWrapper().equals(operand.getCurrencyWrapper())) {

            this.endPos += operand.endPos;
            this.lastPrice = operand.lastPrice;

        } else {
            this.endPos = 0.0;
            this.lastPrice = 0.0;
        }

        this.endValue += operand.endValue;
        this.longBasis += operand.longBasis;
        this.shortBasis += operand.shortBasis;
        this.absPriceChange = 0.0;
        this.pctPriceChange = 0.0;
        this.absValueChange += operand.absValueChange;
        this.income += operand.income;
        this.unrealizedGain += operand.unrealizedGain;
        this.realizedGain += operand.realizedGain;
        this.totalGain += operand.totalGain;
        this.totRetAll = 0.0;
        this.annRetAll = 0.0;
        combineDividendData(operand);
        this.totRet1Day = 0.0;
        this.totRetWk = 0.0;
        this.totRet4Wk = 0.0;
        this.totRet3Mnth = 0.0;
        this.totRetYTD = 0.0;
        this.totRetYear = 0.0;
        this.totRet3year = 0.0;

        this.returnsStartDate = combineReturns(this.returnsStartDate,
                operand.returnsStartDate);
        this.startValues = addDoubleMap(this.startValues, operand.startValues);
        this.incomes = addDoubleMap(this.incomes, operand.incomes);
        this.expenses = addDoubleMap(this.expenses, operand.expenses);

        this.mdMap = combineDateMapMap(this.mdMap, operand.mdMap, "add");
        this.arMap = combineDateMapMap(this.arMap, operand.arMap, "add");
        this.transMap = combineDateMapMap(this.transMap, operand.transMap,
                "add");
    }

    /**
     * combines dividend data depending on whether dividend data are valid
     *
     * @param operand security snapshot to be combined
     */
    private void combineDividendData(SecuritySnapshotReport operand) {
        if (Double.isNaN(this.annualizedDividend) & !Double.isNaN(operand.annualizedDividend)) {
            //take operand values
            this.annualizedDividend = operand.annualizedDividend;
            this.dividendYield = operand.dividendYield;
            this.yieldOnBasis = operand.yieldOnBasis;

        } else if (!Double.isNaN(this.annualizedDividend) & !Double.isNaN(operand.annualizedDividend)) {
            // both valid, add
            this.annualizedDividend += operand.annualizedDividend;
            this.dividendYield = annualizedDividend / endValue;
            this.yieldOnBasis = annualizedDividend / longBasis;
        }
        // if both are NaN, ignore and return
        // if operand is Nan, then ignore and return
        // retain current values --return
    }

    @Override
    public Object[] toTableRow() throws SecurityException,
            IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException {

        addLineBody();
        return super.getOutputLine().toArray();
    }

    @Override
    public void addLineBody() {
        ArrayList<Object> outputLine = super.getOutputLine();
        outputLine.add(this.lastPrice);
        outputLine.add(this.endPos);
        outputLine.add(this.endValue);
        outputLine.add(this.absPriceChange);
        outputLine.add(this.absValueChange);
        outputLine.add(this.pctPriceChange);
        outputLine.add(this.totRet1Day);
        outputLine.add(this.totRetWk);
        outputLine.add(this.totRet4Wk);
        outputLine.add(this.totRet3Mnth);
        outputLine.add(this.totRetYTD);
        outputLine.add(this.totRetYear);
        outputLine.add(this.totRet3year);
        outputLine.add(this.totRetAll);
        outputLine.add(this.annRetAll);
        outputLine.add(this.longBasis);
        outputLine.add(this.shortBasis);
        outputLine.add(this.income);
        outputLine.add(this.annualizedDividend);
        outputLine.add(this.dividendYield);
        outputLine.add(this.yieldOnBasis);
        outputLine.add(this.realizedGain);
        outputLine.add(this.unrealizedGain);
        outputLine.add(this.totalGain);
    }

    /*
     * Combine returns category maps.
     */
    private CategoryMap<Integer> combineReturns(CategoryMap<Integer> map1,
                                                CategoryMap<Integer> map2) {
        CategoryMap<Integer> outMap = new CategoryMap<>(map1);

        for (String map2Key : map2.keySet()) {
            Integer map2Value = map2.get(map2Key);
            if (map1.get(map2Key) == null) {
                outMap.put(map2Key, map2Value);
            } else {
                outMap.put(map2Key, Math.min(map1.get(map2Key), map2Value));
            }
        }

        return outMap;
    }

    /*
     * Combines intermediate values for start value, end value, income, expense
     * for aggregate mod-dietz returns calculations.
     */
    private CategoryMap<Double> addDoubleMap(CategoryMap<Double> map1,
                                             CategoryMap<Double> map2) {
        CategoryMap<Double> outMap = new CategoryMap<>(map1);

        if (map2 != null) {
            for (String retCat2 : map2.keySet()) {
                Double value2 = map2.get(retCat2);
                if (map1.get(retCat2) == null) {
                    outMap.put(retCat2, value2);
                } else {
                    outMap.put(retCat2, map1.get(retCat2) + value2);
                }
            }
        }

        return outMap;
    }

    /*
     * Combines map of datemaps for Snap Reports, either adding or subtracting
     * cash flows.
     * @param map1 input map
     * @param map2 input map
     * @param combType either "add" or "subtract"
     * @return output map
     */
    private CategoryMap<DateMap> combineDateMapMap(CategoryMap<DateMap> map1,
                                                   CategoryMap<DateMap> map2, String combType) {
        CategoryMap<DateMap> outMap = new CategoryMap<>(map1);

        if (map2 != null) {
            for (String retCat2 : map2.keySet()) {
                DateMap treeMap2 = map2.get(retCat2);
                if (map1.get(retCat2) == null) {
                    outMap.put(retCat2, treeMap2);
                } else {
                    DateMap treeMap1 = map1.get(retCat2);
                    DateMap tempMap = new DateMap(treeMap1.combine(treeMap2,
                            combType));
                    outMap.put(retCat2, tempMap);
                }
            }
        }

        return outMap;
    }

    public int getSnapDateInt() {
        return snapDateInt;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public double getEndPos() {
        return endPos;
    }

    public double getEndValue() {
        return endValue;
    }

    public double getIncome() {
        return income;
    }

    public double getTotRetAll() {
        return totRetAll;
    }

    public double getAnnRetAll() {
        return annRetAll;
    }

    public double getTotRet1Day() {
        return totRet1Day;
    }

    public double getTotRetWk() {
        return totRetWk;
    }

    public double getTotRet4Wk() {
        return totRet4Wk;
    }

    public double getTotRet3Mnth() {
        return totRet3Mnth;
    }

    public double getTotRetYTD() {
        return totRetYTD;
    }

    public double getTotRetYear() {
        return totRetYear;
    }

    public double getTotRet3year() {
        return totRet3year;
    }

    public CategoryMap<Integer> getReturnsStartDate() {
        return returnsStartDate;
    }

    public CategoryMap<Double> getStartValues() {
        return startValues;
    }

    public CategoryMap<Double> getStartPoses() {
        return startPoses;
    }

    public CategoryMap<Double> getStartPrices() {
        return startPrices;
    }

    public CategoryMap<Double> getIncomes() {
        return incomes;
    }

    public CategoryMap<Double> getExpenses() {
        return expenses;
    }

    public CategoryMap<DateMap> getMdMap() {
        return mdMap;
    }

    public CategoryMap<DateMap> getArMap() {
        return arMap;
    }

    public CategoryMap<DateMap> getTransMap() {
        return transMap;
    }

    /**
     * class which calculates dividend values based on
     * dividend history, and the dividend frequency from the SecurityAccountWrapper
     */
    class AnnualDividendCalculator {

        Deque<TransactionValues> basisTransactions = new LinkedBlockingDeque<>(2); //current transaction and most recent
        Stack<TransactionValues> dividendTransactions = new Stack<>(); //recent distributions
        HashSet<InvestTxnType> dividendTypes = new HashSet<>(Arrays.asList(InvestTxnType.DIVIDEND,
                InvestTxnType.DIVIDEND_REINVEST, InvestTxnType.DIVIDENDXFR, InvestTxnType.BANK));


        AnnualDividendCalculator() {
        }

        void analyzeTransaction(@NotNull TransactionValues transactionValues) {
            InvestTxnType transType = TxnUtil.getInvestTxnType(transactionValues.getParentTxn());
            boolean isDividend = dividendTypes.contains(transType) && transactionValues.getIncome() != 0;
            updateBasisTransactions(transactionValues);
            if (isDividend) updateDividendTransactions(transactionValues);
        }

        /**
         * add current dividend, clear stack of "old" dividend transactions
         *
         * @param transactionValues dividend transaction
         */
        void updateDividendTransactions(TransactionValues transactionValues) {
            int currentDateInt = transactionValues.getDateint();
            if (dividendTransactions.size() > 0) {
                for (Iterator<TransactionValues> iterator = dividendTransactions.iterator(); iterator.hasNext(); ) {
                    int daysFromLastDivTransaction = DateUtils.getDaysBetween(currentDateInt,
                            iterator.next().getDateint());
                    //remove transaction if older than MINIMUM_EX_DIV_DAYS
                    if (daysFromLastDivTransaction > SecurityAccountWrapper.DividendFrequencyAnalyzer.MINIMUM_EX_DIV_DAYS)
                        iterator.remove();
                }
            }
            dividendTransactions.push(transactionValues);
        }


        void updateBasisTransactions(TransactionValues transactionValues) {
            if (basisTransactions.size() == 0) {
                basisTransactions.addFirst(transactionValues);
                basisTransactions.addLast(transactionValues);
            }
            TransactionValues firstTransactonValues = basisTransactions.removeFirst();
            TransactionValues lastTransactonValues = basisTransactions.removeLast();
            int daysFromLastBasisTransaction = DateUtils.getDaysBetween(transactionValues.getDateint(),
                    lastTransactonValues.getDateint());
            if (daysFromLastBasisTransaction <= SecurityAccountWrapper.DividendFrequencyAnalyzer.MINIMUM_EX_DIV_DAYS) {
                //possible correct to previous, or multiple distributions, update last transaction value only
                lastTransactonValues = transactionValues;
            } else {
                // new transaction, swap last and first
                firstTransactonValues = lastTransactonValues;
                lastTransactonValues = transactionValues;
            }
            basisTransactions.addFirst(firstTransactonValues);
            basisTransactions.addLast(lastTransactonValues);
        }

        /**
         * calcs annualized dividend based on dividend frequency from SecurityAccountWrapper
         *
         * @return annualized dividend in units of currency
         */
        public double getAnnualizedDividend() {
            double totalDividends = 0.0;
            double annualizingFactor = 0.0;
            for (TransactionValues transactionValues : dividendTransactions) {
                totalDividends += transactionValues.getIncome();
            }
            SecurityAccountWrapper.DIV_FREQUENCY div_frequency = getSecurityAccountWrapper().getDivFrequency();
            switch (div_frequency) {
                case ANNUAL:
                    annualizingFactor = 1.0;
                    break;
                case BIANNUAL:
                    annualizingFactor = 2.0;
                    break;
                case QUARTERLY:
                    annualizingFactor = 4.0;
                    break;
                case MONTHLY:
                    annualizingFactor = 12.0;
                    break;
                default:
                    break;
            }
            return totalDividends > 0 ? totalDividends * annualizingFactor : 0.0;
        }

        public void updateYieldInformation() {
            if (basisTransactions.size() > 0) {
                //reference transaction is last transaction older than MINIMUM_EX_DIV_DAYS
                // allows for situations where dividends are immediately reinvested
                TransactionValues basisReferenceTransaction = basisTransactions.getFirst();
                double splitAdjustReferencePos = getSplitAdjustedPosition(basisReferenceTransaction.getPosition(),
                        basisReferenceTransaction.getDateint(), snapDateInt);
                double annualizedDivTotal = getAnnualizedDividend();
                double annualizedDivPerShare = (splitAdjustReferencePos > 0.0 && endPos > 0.0) ?
                        annualizedDivTotal / splitAdjustReferencePos : Double.NaN;
                boolean isValidDivPerShare = !Double.isNaN(annualizedDivPerShare);
                annualizedDividend = isValidDivPerShare ? annualizedDivPerShare * endPos : Double.NaN;
                dividendYield = (lastPrice != 0.0 && isValidDivPerShare) ?
                        annualizedDivPerShare / lastPrice : Double.NaN;
                yieldOnBasis = (longBasis > 0.0 && isValidDivPerShare) ?
                        (annualizedDivPerShare * endPos) / longBasis : Double.NaN;
            }
        }


    }
}


