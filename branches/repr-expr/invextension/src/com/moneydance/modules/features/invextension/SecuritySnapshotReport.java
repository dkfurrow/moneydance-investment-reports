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


    private long lastPrice = 0;           //ending price
    private long endPos = 0;              //ending position
    private long endValue = 0;            //ending value

    private long longBasis = 0;           //final long basis
    private long shortBasis = 0;          //final short basis

    //one day values
    private long absPriceChange = 0;      //absolute price change (from previous day to snapDate)
    private double pctPriceChange = 0;    //percent price change (from previous day to snapDate)
    private long absValueChange = 0;      //absolute value change (from previous day to snapDate)

    //total numbers
    private long income = 0;              //total income (all dates)
    private long unrealizedGain = 0;      // unrealized gain
    private long realizedGain = 0;        //realized gain
    private long totalGain = 0;           //total absolute gain (all dates)
    private double totRetAll = 0;         //total Mod-Dietz return (all dates)
    private double annRetAll = 0;         //annualized return (all dates)

    //Dividend Yields
    private long annualizedDividend = 0;
    private double dividendYield = 0;
    private double yieldOnBasis = 0;


    //returns
    private double totRet1Day = 0;          //Mod-Dietz return (1 day)
    private double totRetWk = 0;            //Mod-Dietz return (1 week)
    private double totRet4Wk = 0;           //Mod-Dietz return (1 month)
    private double totRet3Mnth = 0;         //Mod-Dietz return (3 month)
    private double totRetYTD = 0;           //Mod-Dietz return (Year-to-Date)
    private double totRetYear = 0;          //Mod-Dietz return (1 Year)
    private double totRet3year = 0;         //Mod-Dietz return (1 Years)

    // intermediate values
    private CategoryMap<Integer> returnsStartDate;      //maps return category to start dates
    private CategoryMap<Long> startValues;              //maps return category to start values
    private CategoryMap<Long> startPoses;               //maps return category to start positions
    private CategoryMap<Long> startPrices;              //maps return category to start positions
    private CategoryMap<Long> incomes;                  //maps return category to income
    private CategoryMap<Long> expenses;                 //maps return category to expense
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
        int wkFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addDaysInt(snapDateInt, -7));
        int mnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -1));
        int threeMnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -3));
        int oneYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -12));
        int threeYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -36));
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
            startPoses.put(retCat, 0L);
            this.startValues.put(retCat, 0L);
            this.incomes.put(retCat, 0L);
            this.expenses.put(retCat, 0L);
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

            fromDateInt = transSet.isEmpty() ? snapDateInt
                    : DateUtils.getPrevBusinessDay(transSet.get(0).getDateint());

            // put dates in return map
            this.returnsStartDate.put("All", fromDateInt);

            // these values dependent only on snapDate

            double annualPercentReturn;

            // fill startPrice Array List
            for (String retCat : this.returnsStartDate.keySet()) {
                int thisDateInt = this.returnsStartDate.get(retCat);
                startPrices.put(retCat, secAccountWrapper.getPrice(thisDateInt));
            }

            // iterate through transaction values list
            for (TransactionValues transactionValues : transSet) {
                long totalFlows = transactionValues.getBuy() + transactionValues.getSell()
                        + transactionValues.getShortSell() + transactionValues.getCoverShort()
                        + transactionValues.getCommission() + transactionValues.getIncome()
                        + transactionValues.getExpense();

                // iterate through return dates
                for (String retCat : this.returnsStartDate.keySet()) {
                    int thisFromDateInt = this.returnsStartDate.get(retCat);
                    int transValuesDate = transactionValues.getDateint();

                    // where transactions are before report dates
                    if (transValuesDate <= thisFromDateInt) {
                        long adjustedPos = getSplitAdjustedPosition(transactionValues.getPosition(),
                                transValuesDate, thisFromDateInt);
                        startPoses.put(retCat, adjustedPos); // split adjusts last position
                        // from
                        // TransValuesCum
                        this.startValues.put(retCat, startPrices.get(retCat) * startPoses.get(retCat)/10000);
                    }

                    // where transaction period intersects report period
                    if (transValuesDate > thisFromDateInt && transValuesDate <= snapDateInt) {
                        // MDCalc variable--net effect of calculation is to
                        // return buys and sells, including commission
                        long cf = -(transactionValues.getBuy() + transactionValues.getSell()
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
                            this.endValue = this.endPos * this.lastPrice / 10000;
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
                    this.mdMap.get(retCat).add(thisFromDateInt, 0L);
                }
                // add the last value in return arrays (if endpos != 0)
                if (this.endPos != 0) {
                    this.arMap.get(retCat).add(snapDateInt, this.endValue);
                    // dummy values for Mod-dietz
                    this.mdMap.get(retCat).add(snapDateInt, 0L);
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
            } // end of start date iteration

            // Produce output, get returns

            if (this.returnsStartDate.get("PREV") == null) {
                this.absPriceChange = 0;
                this.absValueChange = 0;
                this.pctPriceChange = 0;
            } else {
                long prevPrice = secAccountWrapper.getPrice(this.returnsStartDate.get("PREV"));

                this.absPriceChange = this.lastPrice - prevPrice;
                this.absValueChange = this.endPos * this.absPriceChange / 10000;
                if (prevPrice != 0) {
                    this.pctPriceChange = ((double)this.lastPrice) / prevPrice - 1.0;
                } else {
                    this.pctPriceChange = 0;
                }
            }
            this.totRet1Day = this.mdReturns.get("PREV") == null ? 0
                    : this.mdReturns.get("PREV");
            this.totRetAll = this.mdReturns.get("All") == null ? 0
                    : this.mdReturns.get("All");
            this.totRetWk = this.mdReturns.get("1Wk") == null ? 0
                    : this.mdReturns.get("1Wk");
            this.totRet4Wk = this.mdReturns.get("4Wk") == null ? 0
                    : this.mdReturns.get("4Wk");
            this.totRet3Mnth = this.mdReturns.get("3Mnth") == null ? 0
                    : this.mdReturns.get("3Mnth");
            this.totRetYear = this.mdReturns.get("1Yr") == null ? 0
                    : this.mdReturns.get("1Yr");
            this.totRet3year = this.mdReturns.get("3Yr") == null ? 0
                    : this.mdReturns.get("3Yr");
            this.totRetYTD = this.mdReturns.get("YTD") == null ? 0
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
                    arMap.get(retCat).add(returnsStartDate.get(retCat), -startValues.get(retCat));
                }
                if (endValue != 0) {
                    arMap.get(retCat).add(snapDateInt, endValue);
                }
                // get return
                annRetAll = computeAnnualReturn(arMap.get(retCat), mdReturns.get("All"));
                income = incomes.get(retCat);

            }
        }
        totRet1Day = returnsStartDate.get("PREV") == null ? 0 : mdReturns.get("PREV");
        totRetAll = returnsStartDate.get("All") == null ? 0 : mdReturns.get("All");
        totRetWk = returnsStartDate.get("1Wk") == null ? 0 : mdReturns.get("1Wk");
        totRet4Wk = returnsStartDate.get("4Wk") == null ? 0 : mdReturns.get("4Wk");
        totRet3Mnth = returnsStartDate.get("3Mnth") == null ? 0 : mdReturns.get("3Mnth");
        totRetYear = returnsStartDate.get("1Yr") == null ? 0 : mdReturns.get("1Yr");
        totRet3year = returnsStartDate.get("3Yr") == null ? 0 : mdReturns.get("3Yr");
        totRetYTD = returnsStartDate.get("YTD") == null ? 0 : mdReturns.get("YTD");
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
            this.endPos = 0;
            this.lastPrice = 0;
        }

        this.endValue += operand.endValue;
        this.longBasis += operand.longBasis;
        this.shortBasis += operand.shortBasis;
        this.absPriceChange = 0;
        this.pctPriceChange = 0;
        this.absValueChange += operand.absValueChange;
        this.income += operand.income;
        this.unrealizedGain += operand.unrealizedGain;
        this.realizedGain += operand.realizedGain;
        this.totalGain += operand.totalGain;
        this.totRetAll = 0;
        this.annRetAll = 0;
        combineDividendData(operand);
        this.totRet1Day = 0;
        this.totRetWk = 0;
        this.totRet4Wk = 0;
        this.totRet3Mnth = 0;
        this.totRetYTD = 0;
        this.totRetYear = 0;
        this.totRet3year = 0;

        this.returnsStartDate = combineReturns(this.returnsStartDate,
                operand.returnsStartDate);
        this.startValues = addLongMap(this.startValues, operand.startValues);
        this.incomes = addLongMap(this.incomes, operand.incomes);
        this.expenses = addLongMap(this.expenses, operand.expenses);

        this.mdMap = combineDateMapMap(this.mdMap, operand.mdMap, "add");
        this.arMap = combineDateMapMap(this.arMap, operand.arMap, "add");
        this.transMap = combineDateMapMap(this.transMap, operand.transMap, "add");
    }

    /**
     * combines dividend data depending on whether dividend data are valid
     *
     * @param operand security snapshot to be combined
     */
    private void combineDividendData(SecuritySnapshotReport operand) {
        if (this.annualizedDividend == 0 & operand.annualizedDividend != 0) {
            //take operand values
            this.annualizedDividend = operand.annualizedDividend;
            this.dividendYield = operand.dividendYield;
            this.yieldOnBasis = operand.yieldOnBasis;

        } else if (this.annualizedDividend != 0 & operand.annualizedDividend != 0) {
            // both valid, add
            this.annualizedDividend += operand.annualizedDividend;
            this.dividendYield = ((double)annualizedDividend) / endValue;
            this.yieldOnBasis = ((double)annualizedDividend) / longBasis;
        }
        // if both are zero, ignore and return
        // if operand is zero, then ignore and return
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
        outputLine.add(this.lastPrice/100.0); // FIXME
        outputLine.add(this.endPos/10000.0);
        outputLine.add(this.endValue/100.0);
        outputLine.add(this.absPriceChange/100.0);
        outputLine.add(this.absValueChange/100.0);
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
        outputLine.add(this.longBasis/100.0);
        outputLine.add(this.shortBasis/100.0);
        outputLine.add(this.income/100.0);
        outputLine.add(this.annualizedDividend / 100.0);
        outputLine.add(this.dividendYield);
        outputLine.add(this.yieldOnBasis);
        outputLine.add(this.realizedGain/100.0);
        outputLine.add(this.unrealizedGain/100.0);
        outputLine.add(this.totalGain/100.0);
    }

    /*
     * Combine returns category maps.
     */
    private CategoryMap<Integer> combineReturns(CategoryMap<Integer> map1, CategoryMap<Integer> map2) {
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
    private CategoryMap<Long> addLongMap(CategoryMap<Long> map1, CategoryMap<Long> map2) {
        CategoryMap<Long> outMap = new CategoryMap<>(map1);

        if (map2 != null) {
            for (String retCat2 : map2.keySet()) {
                Long value2 = map2.get(retCat2);
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

    public long getLastPrice() {
        return lastPrice;
    }

    public long getEndPos() {
        return endPos;
    }

    public long getEndValue() {
        return endValue;
    }

    public long getIncome() {
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

    public CategoryMap<Long> getStartValues() {
        return startValues;
    }

    public CategoryMap<Long> getStartPoses() {
        return startPoses;
    }

    public CategoryMap<Long> getStartPrices() {
        return startPrices;
    }

    public CategoryMap<Long> getIncomes() {
        return incomes;
    }

    public CategoryMap<Long> getExpenses() {
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
                    if (daysFromLastDivTransaction
                            > SecurityAccountWrapper.DividendFrequencyAnalyzer.MINIMUM_EX_DIV_DAYS)
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
            if (daysFromLastBasisTransaction
                    <= SecurityAccountWrapper.DividendFrequencyAnalyzer.MINIMUM_EX_DIV_DAYS) {
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
        public long getAnnualizedDividend() {
            long totalDividends = 0;
            long annualizingFactor = 0;
            for (TransactionValues transactionValues : dividendTransactions) {
                totalDividends += transactionValues.getIncome();
            }
            SecurityAccountWrapper.DIV_FREQUENCY div_frequency = getSecurityAccountWrapper().getDivFrequency();
            switch (div_frequency) {
                case ANNUAL:
                    annualizingFactor = 1;
                    break;
                case BIANNUAL:
                    annualizingFactor = 2;
                    break;
                case QUARTERLY:
                    annualizingFactor = 4;
                    break;
                case MONTHLY:
                    annualizingFactor = 12;
                    break;
                default:
                    break;
            }
            return totalDividends > 0 ? totalDividends * annualizingFactor : 0;
        }

        public void updateYieldInformation() {
            if (basisTransactions.size() > 0) {
                //reference transaction is last transaction older than MINIMUM_EX_DIV_DAYS
                // allows for situations where dividends are immediately reinvested
                TransactionValues basisReferenceTransaction = basisTransactions.getFirst();
                long splitAdjustReferencePos = getSplitAdjustedPosition(basisReferenceTransaction.getPosition(),
                        basisReferenceTransaction.getDateint(), snapDateInt);
                long annualizedDivTotal = getAnnualizedDividend();
                double annualizedDivPerShare = (splitAdjustReferencePos > 0 && endPos > 0) ?
                        ((double)annualizedDivTotal) / splitAdjustReferencePos * 10000 : 0;
                annualizedDividend = Math.round(annualizedDivPerShare * endPos) / 10000;
                dividendYield = (lastPrice != 0 && annualizedDivPerShare != 0) ?
                        ((double)annualizedDivPerShare) / lastPrice : 0.0;
                yieldOnBasis = (longBasis > 0 && annualizedDivPerShare != 0) ?
                        ((double)annualizedDividend) / longBasis : 0.0;
            }
        }
    }
}


