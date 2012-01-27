/* SecuritySnapshotReport.java
 * Copyright 2011 Dale K. Furrow . All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.moneydance.modules.features.invextension;


import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.CurrencyType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedSet;

import com.moneydance.modules.features.invextension.BulkSecInfo.AGG_TYPE;


/**
 * Produces report for security defined by one date ("snapDate").
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/

public class SecuritySnapshotReport extends SecurityReport {
    private int snapDateInt;
    private Account account;
    private CurrencyType cur;
    private String ticker;
    private AGG_TYPE aggType;

    private double lastPrice;           //ending price
    private double endPos;              //ending position
    private double endValue;            //ending value
    private double endCash;             //ending effect of security on account cash
    private double initBalance;         // initial investment account balance
    private double avgCostBasis;                //final average cost balance

    //one day values
    private double absPriceChange;      //absolute price change (from previous day to snapDate)
    private double pctPriceChange;      //percent price change (from previous day to snapDate)
    private double absValueChange;      //absolute value change (from previous day to snapDate)

    //total numbers
    private double income;              //total income (all dates)
    private double totalGain;           //total absolute gain (all dates)
    private double totRetAll;           //total Mod-Dietz return (all dates)
    private double annRetAll;           //annualized return (all dates)

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
    private CategoryMap<Double> startCashs;             //maps return category to starting cash effect

    private CategoryMap<DateMap> mdMap;                 //maps return category to Mod-Dietz date map
    private CategoryMap<DateMap> arMap;                 //maps return category to Annualized Return Date Map
    private CategoryMap<DateMap> transMap;              //maps return category to transfer date map


    /**
     * Generic constructor, used to aggregate values.
     *
     * @param account reference account
     *
     * @param snapDateInt snap date
     */
    public SecuritySnapshotReport(Account account, int snapDateInt,  AGG_TYPE aggType) {
        this.snapDateInt = snapDateInt;
        this.account = account;
        this.cur = null;
        this.ticker = "~Null";
        if (account != null
            && account.getAccountType() == Account.ACCOUNT_TYPE_SECURITY) {
            CurrencyType thisCur = account.getCurrencyType();
            this.ticker = thisCur.getTickerSymbol().isEmpty()
                ? "NoTicker"
                : thisCur.getTickerSymbol();
        }
        this.aggType = aggType;

        this.lastPrice = 0.0;
        this.endPos = 0.0;
        this.endValue = 0.0;
        this.endCash = 0.0;
        if (account == null
            || account.getAccountType() == Account.ACCOUNT_TYPE_SECURITY) {
            this.initBalance = 0.0;
        } else {
            this.initBalance = new Long(account.getStartBalance()).doubleValue() / 100.0;
        }
        this.avgCostBasis = 0.0;

        this.absPriceChange = 0.0;
        this.pctPriceChange = 0.0;
        this.absValueChange = 0.0;

        this.income = 0.0;
        this.totalGain = 0.0;
        this.totRetAll = 0.0;
        this.annRetAll = 0.0;

        this.totRet1Day = 0.0;
        this.totRetWk = 0.0;
        this.totRet4Wk = 0.0;
        this.totRet3Mnth = 0.0;
        this.totRetYTD = 0.0;
        this.totRetYear = 0.0;
        this.totRet3year = 0.0;

        this.returnsStartDate = new CategoryMap<Integer>();
        this.startValues = new CategoryMap<Double>();
        this.startPoses = new CategoryMap<Double>();
        this.startPrices = new CategoryMap<Double>();
        this.incomes = new CategoryMap<Double>();
        this.expenses = new CategoryMap<Double>();
        this.mdReturns = new CategoryMap<Double>();
        this.startCashs = new CategoryMap<Double>();

        this.mdMap = new CategoryMap<DateMap>();
        this.arMap = new CategoryMap<DateMap>();
        this.transMap = new CategoryMap<DateMap>();

        // Calculate return dates (use snapDate for "ALL" as it is latest
        // possible
        int fromDateInt = snapDateInt;
        int prevFromDateInt = DateUtils.getPrevBusinessDay(snapDateInt);
        int wkFromDateInt
            = DateUtils.getLatestBusinessDay(DateUtils.addDaysInt(snapDateInt, -7));
        int mnthFromDateInt
            = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -1));
        int threeMnthFromDateInt
            = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -3));
        int oneYearFromDateInt
            = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -12));
        int threeYearFromDateInt
            = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -36));
        int ytdFromDateInt = DateUtils.getStartYear(snapDateInt);

        // put dates in return map
        this.returnsStartDate = new CategoryMap<Integer>();

        this.returnsStartDate.put("All", fromDateInt);
        this.returnsStartDate.put("PREV", prevFromDateInt);
        this.returnsStartDate.put("1Wk", wkFromDateInt);
        this.returnsStartDate.put("4Wk", mnthFromDateInt);
        this.returnsStartDate.put("3Mnth", threeMnthFromDateInt);
        this.returnsStartDate.put("1Yr", oneYearFromDateInt);
        this.returnsStartDate.put("3Yr", threeYearFromDateInt);
        this.returnsStartDate.put("YTD", ytdFromDateInt);

        // initialize ArrayLists values to zero
        for (Iterator<String> it1 = this.returnsStartDate.keySet().iterator(); it1.hasNext();) {
            String retCat = it1.next();
            startPoses.put(retCat, 0.0);
            this.startValues.put(retCat, 0.0);
            this.incomes.put(retCat, 0.0);
            this.expenses.put(retCat, 0.0);
            this.startCashs.put(retCat, 0.0);
            this.mdReturns.put(retCat, 0.0);

            this.arMap.put(retCat, new DateMap());
            this.mdMap.put(retCat, new DateMap());
            this.transMap.put(retCat, new DateMap());
        }
    }


    /**
     * Constructor for individual security.
     *
     * @param account
     *            reference account
     *
     * @param transSet
     *          transaction set to analyze
     *
     * @param currency
     *            currency for security
     *
     * @param snapDateInt
     *          report date date
     */
    public SecuritySnapshotReport(Account account,
                                  SortedSet<TransValuesCum> transSet,
                                  CurrencyType currency,
                                  int snapDateInt,
                                  AGG_TYPE aggType) {
        this(account, snapDateInt, aggType);    // Initialize object

        if (transSet != null) {
            this.cur = currency;
            if (currency != null) {
                this.ticker = currency.getTickerSymbol().isEmpty() 
                    ? "NoTicker"
                    : currency.getTickerSymbol();
                this.lastPrice = 1.0 /  currency.getUserRateByDateInt(snapDateInt);
            } else {
                this.lastPrice = 1.0;
            }

            if (account.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT) {
                this.initBalance
                    = new Long(transSet.first().transValues.accountRef.getStartBalance()).doubleValue() / 100.0;
            } else {
                this.initBalance = 0.0;
            }
            this.aggType = aggType;

            // create dates for returns calculations
            // ensures all dates for appropriate variables
            int fromDateInt = DateUtils.getPrevBusinessDay(transSet.first().transValues.dateint);

            // put dates in return map
            this.returnsStartDate.put("All", fromDateInt);

            // these values dependent only on snapDate
            double endCumUnrealizedGain = 0.0;
            double longBasis = 0.0;
            double shortBasis = 0.0;

            // these values calculated once, based on total transaction history
            double realizedGain = 0.0;
            double unrealizedGain = 0.0;
            double startCumUnrealGain = 0.0;    // by definition, since this calculation
            // covers all transactions
            double annualPercentReturn = 0.0;

            // fill startPrice Array List
            for (Iterator<String> it = this.returnsStartDate.keySet().iterator(); it.hasNext();) {
                String retCat = it.next();
                int thisDateInt = this.returnsStartDate.get(retCat);
                startPrices.put(retCat,
                                1.0 / (currency == null
                                       ? 1.0
                                       : currency.getUserRateByDateInt(thisDateInt)));
            }

            // iterate through transaction values list
            for (Iterator<TransValuesCum> it = transSet.iterator(); it.hasNext();) {
                TransValuesCum transValuesCum = it.next();

                // iterate through return dates
                for (Iterator<String> it1 = this.returnsStartDate.keySet().iterator(); it1.hasNext();) {
                    String retCat = it1.next();
                    int thisFromDateInt = this.returnsStartDate.get(retCat);

                    // where transactions are before report dates
                    if (transValuesCum.transValues.dateint <= thisFromDateInt) {
                        double currentRate = currency == null
                            ? 1.0
                            : currency.getUserRateByDateInt(thisFromDateInt);
                        double splitAdjust = currency == null
                            ? 1.0
                            : currency.adjustRateForSplitsInt(transValuesCum.transValues.dateint,
                                                              currentRate,
                                                              thisFromDateInt) / currentRate;
                        startPoses.put(retCat,
                                       transValuesCum.position * splitAdjust); // split adjusts last position from
                                                                               // TransValuesCum
                        this.startValues.put(retCat, startPrices.get(retCat) * startPoses.get(retCat));
                        this.startCashs.put(retCat,
                                            this.startCashs.get(retCat) + transValuesCum.transValues.cashEffect);

                        if ("All".equals(retCat)) {
                            // end cash increment, only needs to be done once ("All"
                            // loop for convenience)
                            this.endCash = this.endCash + transValuesCum.transValues.cashEffect;
                        }
                    }

                    // where transaction period intersects report period
                    if (transValuesCum.transValues.dateint > thisFromDateInt
                        && transValuesCum.transValues.dateint <= snapDateInt) {

                        // MDCalc variable--net effect of calculation is to return
                        // buys and sells, including commission
                        double cf = -(transValuesCum.transValues.buy
                                      + transValuesCum.transValues.sell
                                      + transValuesCum.transValues.shortSell
                                      + transValuesCum.transValues.coverShort +
                                      transValuesCum.transValues.commision);

                        // add variables to arrays needed for returns calculation

                        this.arMap.get(retCat).add(transValuesCum.transValues.dateint,
                                                   transValuesCum.transValues.cashEffect
                                                   - transValuesCum.transValues.transfer);
                        this.mdMap.get(retCat).add(transValuesCum.transValues.dateint, cf);
                        this.transMap.get(retCat).add(transValuesCum.transValues.dateint,
                                                      transValuesCum.transValues.transfer);

                        this.incomes.put(retCat,
                                         this.incomes.get(retCat)
                                         + transValuesCum.transValues.income);
                        this.expenses.put(retCat,
                                          this.expenses.get(retCat)
                                          + transValuesCum.transValues.expense);

                        if ("All".equals(retCat)) {// end cash increment--only needs to be done once
                            realizedGain = transValuesCum.perRealizedGain + realizedGain;
                            this.endCash = this.endCash + transValuesCum.transValues.cashEffect;
                        }

                        double currentRate = currency == null
                            ? 1.0
                            : currency.getUserRateByDateInt(snapDateInt);
                        double splitAdjust = currency == null
                            ? 1.0
                            : currency.adjustRateForSplitsInt(transValuesCum.transValues.dateint,
                                                              currentRate,
                                                              snapDateInt) / currentRate;

                        this.endPos = transValuesCum.position * splitAdjust;
                        this.endValue = this.endPos * this.lastPrice;
                        longBasis = transValuesCum.longBasis;
                        shortBasis = transValuesCum.shortBasis;
                        this.avgCostBasis = longBasis + shortBasis;
                    } // end--where transaction period intersects report period
                } // end of start date iterative loop
            } // end of input transaction set loop

            if (this.endPos > 0) {
                endCumUnrealizedGain = this.endValue - longBasis;
            } else if (this.endPos < 0) {
                endCumUnrealizedGain = this.endValue - shortBasis;
            }
            // startCumUnrealGain is zero by definition
            unrealizedGain = endCumUnrealizedGain - startCumUnrealGain;
            this.totalGain = realizedGain + unrealizedGain;

            // now go through arrays and get returns/calc values
            for (Iterator<String> it1 = this.returnsStartDate.keySet().iterator(); it1.hasNext();) {
                String retCat = it1.next();
                int thisFromDateInt = this.returnsStartDate.get(retCat);
                // add the first value in return arrays (if startpos != 0)
                if (startPoses.get(retCat) != 0) {
                    this.arMap.get(retCat).add(thisFromDateInt, -this.startValues.get(retCat));
                    this.mdMap.get(retCat).add(thisFromDateInt, 0.0);    // Dummy Value for Mod-Dietz
                }
                // add the last value in return arrays (if endpos != 0)
                if (this.endPos != 0) {
                    this.arMap.get(retCat).add(snapDateInt, this.endValue);
                    this.mdMap.get(retCat).add(snapDateInt, 0.0);    // Dummy Value for Mod-Dietz
                }

                // get MD returns on all start dates, only get annualized return on
                // all dates

                this.mdReturns.put(retCat,
                                   computeMDReturn(this.startValues.get(retCat),
                                                   this.endValue,
                                                   this.incomes.get(retCat),
                                                   this.expenses.get(retCat),
                                                   this.mdMap.get(retCat)));

                if ("All".equals(retCat)) {
                    annualPercentReturn = computeAnnualReturn(this.arMap.get(retCat),
                                                              this.mdReturns.get("All"));
                    this.annRetAll = annualPercentReturn;
                    this.income = this.incomes.get(retCat);
                }

                // remove start and end values from return date maps for ease of
                // aggregation
                if (startPoses.get(retCat) != 0) {
                    this.arMap.get(retCat).add(thisFromDateInt, +this.startValues.get(retCat));
                }
                // remove start and end values from return date maps for ease of
                // aggregation
                if (this.endPos != 0) {
                    this.arMap.get(retCat).add(snapDateInt, -this.endValue);
                }
            } // end of start date iterative loop

            // Produce output

            if (this.returnsStartDate.get("PREV") == null) {
                this.absPriceChange = Double.NaN;
                this.absValueChange = Double.NaN;
                this.pctPriceChange = Double.NaN;
            } else {
                double prevPrice = currency == null
                    ? 1.0
                    : 1.0 / currency.getUserRateByDateInt(this.returnsStartDate.get("PREV"));
                this.absPriceChange = this.lastPrice - prevPrice;
                this.absValueChange = this.endPos * this.absPriceChange;
                this.pctPriceChange = this.lastPrice / prevPrice - 1.0;
            }
            this.totRet1Day
                = this.mdReturns.get("PREV") == null ? Double.NaN : this.mdReturns.get("PREV");
            this.totRetAll
                = this.mdReturns.get("All") == null ? Double.NaN : this.mdReturns.get("All");
            this.totRetWk
                = this.mdReturns.get("1Wk") == null ? Double.NaN : this.mdReturns.get("1Wk");
            this.totRet4Wk
                = this.mdReturns.get("4Wk") == null ? Double.NaN : this.mdReturns.get("4Wk");
            this.totRet3Mnth
                = this.mdReturns.get("3Mnth") == null ? Double.NaN : this.mdReturns.get("3Mnth");
            this.totRetYear
                = this.mdReturns.get("1Yr") == null ? Double.NaN : this.mdReturns.get("1Yr");
            this.totRet3year
                = this.mdReturns.get("3Yr") == null ? Double.NaN : this.mdReturns.get("3Yr");
            this.totRetYTD
                = this.mdReturns.get("YTD") == null ? Double.NaN : this.mdReturns.get("YTD");
        }
    }


    /**
     * Generates individual line report body.
     *
     * @return array of values
     */
    public Object[] toTableRow() {
        ArrayList<Object> snapValues = new ArrayList<Object>();
        String tilde = "\u007e";

        switch (aggType) {
        case SEC : // individual security
            snapValues.add(this.account.getParentAccount().getAccountName());
            snapValues.add(this.account.getAccountName());
            break;
        case ACCT_SEC: //aggregated securities
            snapValues.add(this.account.getAccountName());
            snapValues.add(tilde + "AllSec");
            break;
        case ACCT_CASH: //cash balance
            snapValues.add(this.account.getAccountName());
            snapValues.add(tilde + "Cash");
            break;
        case ACCT_SEC_PLUS_CASH: //aggregated securities + cash
            snapValues.add(this.account.getAccountName());
            snapValues.add(tilde + "AllSec+Cash");
            break;
        case ALL_SEC: //all securities
            snapValues.add(tilde + "ALL");
            snapValues.add(tilde + "AllSec");
            break;
        case ALL_CASH: //all cash
            snapValues.add(tilde + "ALL");
            snapValues.add(tilde + "Cash");
            break;
        case ALL_SEC_PLUS_CASH: //all securities +  cash
            snapValues.add(tilde + "ALL");
            snapValues.add(tilde + "AllSec+Cash");
            break;
        }
        snapValues.add(this.ticker);
        snapValues.add(this.lastPrice);
        snapValues.add(this.endPos);
        snapValues.add(this.endValue);
        snapValues.add(this.absPriceChange);
        snapValues.add(this.absValueChange);
        snapValues.add(this.pctPriceChange);
        snapValues.add(this.totRet1Day);
        snapValues.add(this.totRetWk);
        snapValues.add(this.totRet4Wk);
        snapValues.add(this.totRet3Mnth);
        snapValues.add(this.totRetYTD);
        snapValues.add(this.totRetYear);
        snapValues.add(this.totRet3year);
        snapValues.add(this.totRetAll);
        snapValues.add(this.annRetAll);
        snapValues.add(this.avgCostBasis);
        snapValues.add(this.income);
        snapValues.add(this.totalGain);
        return snapValues.toArray();
    }


    /**
     * Adds SecuritySnapshotReport objects, in order to aggregate data at
     * account level or at aggregate level.
     *
     * @param addend
     *            SecuritySnapshotReport added into this object
     *            (addend is typically a security and the accumulator an investment account)
     *
     * @return the sum of the two SecuritySnapshotReports
     */
    public SecuritySnapshotReport addTo(SecurityReport opd) {
        SecuritySnapshotReport operand = (SecuritySnapshotReport)opd;

        this.lastPrice = 0.0;
        this.endPos = 0.0;
        this.endValue += operand.endValue;
        this.endCash += operand.endCash;

        // need to handle both cases of aggregation (1) at investment account
        // and (2) across multiple investment account
        if (operand.account == null) {
            this.initBalance = operand.initBalance;
        } else if (operand.account.getAccountType() ==
                   Account.ACCOUNT_TYPE_INVESTMENT) {
            this.initBalance += operand.initBalance;
        }
        this.avgCostBasis += operand.avgCostBasis;
        this.absPriceChange = 0.0;
        this.pctPriceChange = 0.0;
        this.absValueChange += operand.absValueChange;
        this.income += operand.income;
        this.totalGain += operand.totalGain;
        this.totRetAll = 0.0;
        this.annRetAll = 0.0;
        this.totRet1Day = 0.0;
        this.totRetWk = 0.0;
        this.totRet4Wk = 0.0;
        this.totRet3Mnth = 0.0;
        this.totRetYTD = 0.0;
        this.totRetYear = 0.0;
        this.totRet3year = 0.0;

        this.returnsStartDate = combineReturns(this.returnsStartDate, operand.returnsStartDate);
        this.startValues = addDoubleMap(this.startValues, operand.startValues);
        this.incomes = addDoubleMap(this.incomes, operand.incomes);
        this.expenses = addDoubleMap(this.expenses, operand.expenses);
        this.mdReturns = this.mdReturns;
        this.startCashs = addDoubleMap(this.startCashs, operand.startCashs);

        this.mdMap = combineDateMapMap(this.mdMap, operand.mdMap, "add");
        this.arMap = combineDateMapMap(this.arMap, operand.arMap, "add");
        this.transMap = combineDateMapMap(this.transMap, operand.transMap, "add");

        return this;
    }


    /**
     * Update returns of aggregated securities in this Investment Account snapshot.
     */
    public void recomputeAggregateReturns() {
        for (Iterator<String> it1 = returnsStartDate.keySet().iterator(); it1.hasNext();) {
            String retCat = it1.next();

            // get MD returns on all start dates, only get annualized return for
            // "All" dates
            mdReturns.put(retCat,
                          computeMDReturn(startValues.get(retCat),
                                          endValue,
                                          incomes.get(retCat),
                                          expenses.get(retCat),
                                          mdMap.get(retCat)));

            if ("All".equals(retCat)) {
                // add start and end values to return date maps
                if (startValues.get(retCat) != 0.0) {
                    arMap.get(retCat).add(returnsStartDate.get(retCat), -startValues.get(retCat));
                }
                if (endValue != 0.0) {
                    arMap.get(retCat).add(snapDateInt, endValue);
                }
                // get return
                annRetAll = computeAnnualReturn(arMap.get(retCat), mdReturns.get("All"));
                income = incomes.get(retCat);

                // remove start and end values from return date maps (to avoid
                // conflicts in aggregation)
                if (startValues.get(retCat) != 0.0) {
                    arMap.get(retCat).add(returnsStartDate.get(retCat), startValues.get(retCat));
                }
                if (endValue != 0.0) {
                    arMap.get(retCat).add(snapDateInt, -endValue);
                }

            }
        }
        totRet1Day = returnsStartDate.get("PREV") == null ? Double.NaN : mdReturns.get("PREV");
        totRetAll = returnsStartDate.get("All") == null ? Double.NaN : mdReturns.get("All");
        totRetWk = returnsStartDate.get("1Wk") == null ? Double.NaN : mdReturns.get("1Wk");
        totRet4Wk = returnsStartDate.get("4Wk") == null ? Double.NaN : mdReturns.get("4Wk");
        totRet3Mnth = returnsStartDate.get("3Mnth") == null ? Double.NaN : mdReturns.get("3Mnth");
        totRetYear = returnsStartDate.get("1Yr") == null ? Double.NaN : mdReturns.get("1Yr");
        totRet3year = returnsStartDate.get("3Yr") == null ? Double.NaN : mdReturns.get("3Yr");
        totRetYTD = returnsStartDate.get("YTD") == null ? Double.NaN : mdReturns.get("YTD");
    }


    /**
     * Computes snapshot for cash associated with this investment account snapshot.
     *
     * @param cashSnapshot
     *            Account's "bank" transactions snapshot.
     *
     * @return snapshot containing income/returns for cash portion of investment account.
     */
    public SecuritySnapshotReport computeCashReturns(SecurityReport cash, AGG_TYPE aggType) {
        SecuritySnapshotReport cashSnapshot = (SecuritySnapshotReport)cash;

        // cashReturn has start, end cash positions, income and expenses
        SecuritySnapshotReport cashReturn = new SecuritySnapshotReport(this.account,
                                                                       this.snapDateInt,
                                                                       aggType);

        // comboTransMDMap has purchases/sales of cash (i.e. reverse of security transactions)
        // start by adding transfers in and out of securities and investment accounts
        CategoryMap<DateMap> comboTransMDMap =
            combineDateMapMap(this.transMap, cashSnapshot.transMap, "add");

        // may need to adjust start dates, so create map for that
        CategoryMap<Integer> adjRetDateMap
            = combineReturns(cashSnapshot.returnsStartDate, this.returnsStartDate);

        double initBal = cashSnapshot.initBalance;

        // generate starting and ending cash balances, non-security related account transactions
        for (Iterator<String> it = this.returnsStartDate.keySet().iterator(); it.hasNext();) {
            String retCat = it.next();
            cashReturn.startValues.put(retCat,
                                       cleanedValue(this.startCashs.get(retCat)
                                                    + cashSnapshot.startCashs.get(retCat) + initBal));
            cashReturn.incomes.put(retCat, cashSnapshot.incomes.get(retCat));
            cashReturn.expenses.put(retCat, cashSnapshot.expenses.get(retCat));

            /*
             * this handles case where fromDateInt < first transaction, AND
             * initBal != 0 (i.e. startValue = initBal. In that case, start date
             * needs to be adjusted to day prior to first transaction date
             */
            int minDateInt = comboTransMDMap.get(retCat).isEmpty()
                ? 0
                : DateUtils.getPrevBusinessDay(comboTransMDMap.get(retCat).firstKey());
            if (cashReturn.startValues.get(retCat) == initBal
                && this.returnsStartDate.get(retCat) <= minDateInt) {
                adjRetDateMap.put(retCat, Math.max(this.returnsStartDate.get(retCat), minDateInt));
            }
        }

        cashReturn.endValue = cleanedValue(this.endCash + cashSnapshot.endCash + initBal);
        cashReturn.endPos = cashReturn.endValue;
        cashReturn.income = cashSnapshot.income; // note, we do not display
        // expenses in this object
        // but they are tracked for returns calculations

        CategoryMap<DateMap> adjAnnRetValues = new CategoryMap<DateMap>(this.arMap);
        for (Iterator<String> it = this.returnsStartDate.keySet().iterator(); it.hasNext();) {
            String retCat = it.next();
            Integer thisFromDateInt = adjRetDateMap.get(retCat);
            // add dummy values to date maps (if start and end values exist)
            if (Math.abs(cashReturn.startValues.get(retCat)) > 0.0001) {
                adjAnnRetValues.get(retCat).add(thisFromDateInt, 0.0);}
            if (Math.abs(cashReturn.endValue) > 0.0001) {
                adjAnnRetValues.get(retCat).add(cashReturn.snapDateInt, 0.0);}
        }

        /*
         * now add transfer map to map of buys/sells/income/expense
         * (effectively, purchase/sales of cash caused by security activity
         */
        comboTransMDMap = combineDateMapMap(comboTransMDMap, adjAnnRetValues, "add");
        // calculate period returns and annual return for all dates
        for (Iterator<String> it = this.returnsStartDate.keySet().iterator(); it.hasNext();) {
            String retCat = it.next();
            double thisPercentReturn = computeMDReturn(cashReturn.startValues.get(retCat),
                                                       cashReturn.endValue,
                                                       cashReturn.incomes.get(retCat),
                                                       cashReturn.expenses.get(retCat),
                                                       comboTransMDMap.get(retCat));
            cashReturn.mdReturns.put(retCat, thisPercentReturn);
            // add map values for auditing
            cashReturn.mdMap.put(retCat, comboTransMDMap.get(retCat));

            if ("All".equals(retCat)) {
                /*
                 * cashRetMap effectively reverses sign of previous map (so cash
                 * buys/sells with correct sign for returns calc), and adds to
                 * that account-level income/expense transactions from arMap
                 * (e.g. account interest)
                 */
                DateMap cashRetMap
                    = cashSnapshot.arMap.get(retCat).combine(comboTransMDMap.get(retCat),
                                                             "subtract");
                // add start and end values to date map
                Integer thisFromDateInt = adjRetDateMap.get(retCat);
                if (Math.abs(cashReturn.startValues.get(retCat)) > 0.0001) {
                    cashRetMap.add(thisFromDateInt, -cashReturn.startValues.get(retCat));}
                if (Math.abs(cashReturn.endValue) > 0.0001) {
                    cashRetMap.add(this.snapDateInt, cashReturn.endValue);}
                cashReturn.annRetAll = computeAnnualReturn(cashRetMap,
                                                           cashReturn.mdReturns.get(retCat));
                cashReturn.income = cashSnapshot.incomes.get(retCat);
                cashReturn.arMap.put("All", cashRetMap);
            }
        }
        cashReturn.totRet1Day = this.returnsStartDate.get("PREV") == null
            ? Double.NaN
            : cashReturn.mdReturns.get("PREV");
        cashReturn.totRetAll = this.returnsStartDate.get("All") == null
            ? Double.NaN
            : cashReturn.mdReturns.get("All");
        cashReturn.totRetWk = this.returnsStartDate.get("1Wk") == null
            ? Double.NaN
            : cashReturn.mdReturns.get("1Wk");
        cashReturn.totRet4Wk = this.returnsStartDate.get("4Wk") == null
            ? Double.NaN
            : cashReturn.mdReturns.get("4Wk");
        cashReturn.totRet3Mnth = this.returnsStartDate.get("3Mnth") == null
            ? Double.NaN
            : cashReturn.mdReturns.get("3Mnth");
        cashReturn.totRetYear = this.returnsStartDate.get("1Yr") == null
            ? Double.NaN
            : cashReturn.mdReturns.get("1Yr");
        cashReturn.totRet3year = this.returnsStartDate.get("3Yr") == null
            ? Double.NaN
            : cashReturn.mdReturns.get("3Yr");
        cashReturn.totRetYTD = this.returnsStartDate.get("YTD") == null
            ? Double.NaN
            : cashReturn.mdReturns.get("YTD");

        return cashReturn;
    }


    /**
     * Computes snapshot for this investment account with associated cash
     * accounted for as a security.
     *
     * @param cashSnapshot
     *        SecuritySnapshotReport associated with Account "bank" transactions.
     *
     * @return snapshot representing income/returns for investment account, cash
     *         and securities included.
     */
    public SecuritySnapshotReport computeAggregateReturnWCash(SecurityReport cash,
                                                              AGG_TYPE aggType) {
        SecuritySnapshotReport cashSnapshot = (SecuritySnapshotReport)cash;

        SecuritySnapshotReport invValue = new SecuritySnapshotReport(this.account,
                                                                     this.snapDateInt,
                                                                     aggType);

        CategoryMap<Integer> adjRetDateMap
            = combineReturns(cashSnapshot.returnsStartDate, this.returnsStartDate);

        double initBal = cashSnapshot.initBalance;

        // copy over aggregate values from aggregated securities
        invValue.totalGain = this.totalGain;
        invValue.income = this.income + cashSnapshot.income;
        invValue.absValueChange = this.absValueChange;
        // ending balance sheet values

        // combine transfer date maps
        invValue.transMap = combineDateMapMap(this.transMap, cashSnapshot.transMap, "add");

        // get correct start and end balances w/ cash accounted for
        for (Iterator<String> it = this.returnsStartDate.keySet().iterator(); it.hasNext();) {
            String retCat = it.next();
            invValue.startCashs.put(retCat, this.startCashs.get(retCat)
                                    + cashSnapshot.startCashs.get(retCat));
            invValue.startValues.put(retCat,
                                     cleanedValue(invValue.startCashs.get(retCat)
                                                  + this.startValues.get(retCat)
                                                  + cashSnapshot.startValues.get(retCat)
                                                  + initBal));
            /*
             * this handles case where fromDateInt < first transaction, AND
             * initBal != 0 (i.e. startValue = initBal. In that case, start date
             * needs to be adjusted to day prior to first transaction date
             */
            int minDateInt = invValue.transMap.get(retCat).isEmpty()
                ? 0
                : DateUtils.getPrevBusinessDay(invValue.transMap.get(retCat).firstKey());
            if (invValue.startValues.get(retCat) == initBal
                && this.returnsStartDate.get(retCat) <= minDateInt) {
                adjRetDateMap.put(retCat, Math.max(this.returnsStartDate.get(retCat), minDateInt));
            }
        }
        invValue.endValue = cleanedValue(this.endValue
                                         + cashSnapshot.endValue + this.endCash
                                         + cashSnapshot.endCash + initBal);
        invValue.endCash = this.endCash + cashSnapshot.endCash + initBal;
        // get returns
        for (Iterator<String> it = this.returnsStartDate.keySet().iterator(); it.hasNext();) {
            String retCat = it.next();
            Integer thisFromDateInt = adjRetDateMap.get(retCat);
            DateMap mdMap = invValue.transMap.get(retCat);
            // add dummy values to Mod-Dietz date maps
            if (Math.abs(invValue.startValues.get(retCat)) > 0.0001) {
                mdMap.add(thisFromDateInt, 0.0);}
            if (Math.abs(invValue.endValue) > 0.0001) {
                mdMap.add(invValue.snapDateInt, 0.0);}
            /*
             * calc returns (note--no income/expenses since only transfers are
             * considered i.e. endValue includes income/expenses
             */
            double thisMDReturn = computeMDReturn(invValue.startValues.get(retCat),
                                                  invValue.endValue,
                                                  0.0,
                                                  0.0,
                                                  mdMap);
            invValue.mdReturns.put(retCat, thisMDReturn);
            // add map for auditing
            invValue.mdMap.put(retCat, mdMap);
            // get annualized returns
            if ("All".equals(retCat)) {
                DateMap retMap = new DateMap().combine(invValue.transMap.get(retCat), "subtract");
                // add start and end values
                if (Math.abs(invValue.startValues.get(retCat)) > 0.0001)  {
                    retMap.add(thisFromDateInt, -invValue.startValues.get(retCat));}
                if (Math.abs(invValue.endValue) > 0.0001) {
                    retMap.add(this.snapDateInt, invValue.endValue);}
                // calculate returns
                invValue.annRetAll = computeAnnualReturn(retMap, invValue.mdReturns.get(retCat));
                // add map for auditing
                invValue.arMap.put("All", retMap);
            }
        }
        invValue.totRet1Day = this.returnsStartDate.get("PREV") == null
            ? Double.NaN
            : invValue.mdReturns.get("PREV");
        invValue.totRetAll = this.returnsStartDate.get("All") == null
            ? Double.NaN
            : invValue.mdReturns.get("All");
        invValue.totRetWk = this.returnsStartDate.get("1Wk") == null
            ? Double.NaN
            : invValue.mdReturns.get("1Wk");
        invValue.totRet4Wk = this.returnsStartDate.get("4Wk") == null
            ? Double.NaN
            : invValue.mdReturns.get("4Wk");
        invValue.totRet3Mnth = this.returnsStartDate.get("3Mnth") == null
            ? Double.NaN
            : invValue.mdReturns.get("3Mnth");
        invValue.totRetYear = this.returnsStartDate.get("1Yr") == null
            ? Double.NaN
            : invValue.mdReturns.get("1Yr");
        invValue.totRet3year = this.returnsStartDate.get("3Yr") == null
            ? Double.NaN
            : invValue.mdReturns.get("3Yr");
        invValue.totRetYTD = this.returnsStartDate.get("YTD") == null
            ? Double.NaN
            : invValue.mdReturns.get("YTD");

        return invValue;
    }


    /*
     * Combines intermediate values for start value, end value, income, expense
     * for aggregate mod-dietz returns calculations.
     */
    private CategoryMap<Double> addDoubleMap(CategoryMap<Double> map1,
                                             CategoryMap<Double> map2) {
        CategoryMap<Double> outMap = new CategoryMap<Double>(map1);

        if (map2 != null) {
            for (Iterator<String> it = map2.keySet().iterator(); it.hasNext();) {
                String retCat2 = it.next();
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
     * Combine returns category maps.
     *
     */
    private CategoryMap<Integer> combineReturns(CategoryMap<Integer> map1,
                                                CategoryMap<Integer> map2) {
        CategoryMap<Integer> outMap = new CategoryMap<Integer>(map1);

        for (Iterator<String> it = map2.keySet().iterator(); it.hasNext();) {
            String map2Key = it.next();
            Integer map2Value = map2.get(map2Key);
            if (map1.get(map2Key) == null) {
                outMap.put(map2Key, map2Value);
            } else {
                outMap.put(map2Key, Math.min(map1.get(map2Key), map2Value));
            }
        }

        return outMap;
    }


    /**
     * Combines map of datemaps for Snap Reports, either adding or subtracting
     * cash flows.
     *
     * @param map1
     *            input map
     *
     * @param map2
     *            input map
     *
     * @param combType
     *            either "add" or "subtract"
     *
     * @return output map
     */
    private CategoryMap<DateMap> combineDateMapMap(CategoryMap<DateMap> map1,
                                                   CategoryMap<DateMap> map2,
                                                   String combType) {
        CategoryMap<DateMap> outMap = new CategoryMap<DateMap>(map1);

        if (map2 != null) {
            for (Iterator<String> it = map2.keySet().iterator(); it.hasNext();) {
                String retCat2 = it.next();
                DateMap treeMap2 = map2.get(retCat2);
                if (map1.get(retCat2) == null) {
                    outMap.put(retCat2, treeMap2);
                } else {
                    DateMap treeMap1 = map1.get(retCat2);
                    DateMap tempMap = new DateMap(treeMap1.combine(treeMap2, combType));
                    outMap.put(retCat2, tempMap);
                }
            }
        }

        return outMap;
    }


    public int getSnapDateInt() {
        return snapDateInt;
    }


    public Account getAccount() {
        return account;
    }


    public CurrencyType getCur() {
        return cur;
    }


    public String getTicker() {
        return ticker;
    }


    public AGG_TYPE getAggType() {
        return aggType;
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


    public double getEndCash() {
        return endCash;
    }


    public double getInitBalance() {
        return initBalance;
    }


    public double getAvgCostBasis() {
        return avgCostBasis;
    }


    public double getAbsPriceChange() {
        return absPriceChange;
    }


    public double getPctPriceChange() {
        return pctPriceChange;
    }


    public double getAbsValueChange() {
        return absValueChange;
    }


    public double getIncome() {
        return income;
    }


    public double getTotalGain() {
        return totalGain;
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


    public CategoryMap<Double> getMdReturns() {
        return mdReturns;
    }


    public CategoryMap<Double> getStartCashs() {
        return startCashs;
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
}
