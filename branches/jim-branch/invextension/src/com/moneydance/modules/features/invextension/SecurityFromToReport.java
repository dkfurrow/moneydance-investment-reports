/* SecurityFromToReport.java
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
 * Produces report for security defined by "from" date and "to" date.
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */

public class SecurityFromToReport extends SecurityReport {
    int fromDateInt;            // start of report period
    int toDateInt;              // end of report period
    Account account;            // reference account
    String ticker;

    double startPos;            // starting position
    double endPos;              // ending position
    double startPrice;          // starting price
    double endPrice;            // ending price
    double startValue;          // starting value
    double endValue;            // ending value

    double startCash;           // starting "cash effect" (security effect on account cash)
    double endCash;             // ending "cash effect" (security affect on account cash)
    double initBalance;         // initial balance of account

    double buy;                 // cumulative cash effect of buys (including commission)
    double sell;                // cumulative cash effect of sells (including commission)
    double shortSell;           // cumulative cash effect of shorts (including commission)
    double coverShort;          // cumulative cash effect of covers (including commission)

    double income;              // cumulative income
    double expense;             // cumulative expense

    double longBasis;           // ending average cost basis of long positions
    double shortBasis;          // ending average cost basis of short positions
    double realizedGain;        // cumulative realized gains (against avg cost)
    double unrealizedGain;      // cumulative unrealized gains
    double totalGain;           // sum of realized and unrealized gains

    double mdReturn;            // period total return (Mod-Dietz method)
    double annualPercentReturn; // period annualized return (Mod-Dietz method)
    AGG_TYPE aggType;           // aggregation type (e.g. security, investment, etc);

    DateMap arMap;              // date map of annual return data
    DateMap mdMap;              // date map of Mod-Dietz return data
    DateMap transMap;           // date map of transfer data

    /**
     * Generic constructor, produced empty report used to aggregate data from
     * multiple securities.
     *
     * @param account
     *            reference account
     *
     * @param fromDateInt
     *            "from" date
     *
     * @param toDateInt
     *            "to" date
     */
    public SecurityFromToReport(Account account, int fromDateInt, int toDateInt, AGG_TYPE aggType) {

        this.fromDateInt = fromDateInt;
        this.toDateInt = toDateInt;
        this.account = account;
        this.ticker = "~Null";
        if (account != null && account.getAccountType() == Account.ACCOUNT_TYPE_SECURITY) {
            CurrencyType thisCur = account.getCurrencyType();
            this.ticker = thisCur.getTickerSymbol().isEmpty()
                ? "NoTicker"
                : thisCur.getTickerSymbol();
        }

        this.startPos = 0.0;
        this.endPos = 0.0;
        this.startPrice = 0.0;
        this.endPrice = 0.0;
        this.startValue = 0.0;
        this.endValue = 0.0;

        this.startCash = 0;
        this.endCash = 0;
        if (account == null
            || account.getAccountType() == Account.ACCOUNT_TYPE_SECURITY) {
            this.initBalance = 0.0;
        } else {
            this.initBalance = new Long(account.getStartBalance()).doubleValue() / 100.0;
        }

        this.buy = 0.0;
        this.sell = 0.0;
        this.shortSell = 0.0;
        this.coverShort = 0.0;

        this.income = 0.0;
        this.expense = 0.0;

        this.longBasis = 0.0;
        this.shortBasis = 0.0;
        this.realizedGain = 0.0;
        this.unrealizedGain = 0.0;
        this.totalGain = 0.0;

        this.mdReturn = 0.0;
        this.annualPercentReturn = 0.0;

        this.arMap = new DateMap();
        this.mdMap = new DateMap();
        this.transMap = new DateMap();
        this.aggType = aggType;
    }


    /**
     * Constructor for individual security.
     *
     * @param account
     *            reference account
     *
     * @param transSet
     *            transaction set to analyze
     *
     * @param currency
     *            currency for security
     *
     * @param fromDateInt
     *            "from" date
     *
     * @param toDateInt
     *            "to" date
     */
    public SecurityFromToReport(Account account,
                                SortedSet<TransValuesCum> transSet,
                                CurrencyType currency,
                                int fromDateInt,
                                int toDateInt,
                                AGG_TYPE aggType) {
        this(account, fromDateInt, toDateInt, aggType);    // Initialize object

        if (transSet != null) {
            // Currency is null when account is investment account
            if (currency != null) {
                this.ticker = currency.getTickerSymbol().isEmpty()
                    ? "NoTicker"
                    : currency.getTickerSymbol();
                this.startPrice = 1.0 / currency.getUserRateByDateInt(fromDateInt);
                this.endPrice = 1.0 / currency.getUserRateByDateInt(toDateInt);
            } else {
                this.startPrice = 1.0;
                this.endPrice = 1.0;
            }

            if (account.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT) {
                this.initBalance
                    = new Long(transSet.first().transValues.accountRef.getStartBalance()).doubleValue() / 100.0;
            } else {
                this.initBalance = 0.0;
            }

            this.aggType = aggType;

            // intialize intermediate calculation variables
            double startCumUnrealGain = 0;
            double endCumUnrealizedGain = 0;
            double startLongBasis = 0;
            double startShortBasis = 0;
            double toDateRate = currency == null
                ? 1.0
                : currency.getUserRateByDateInt(toDateInt);
            double fromDateRate = currency == null
                ? 1.0
                : currency.getUserRateByDateInt(fromDateInt);

            // iterates through full transaction set
            for (Iterator<TransValuesCum> it = transSet.iterator(); it.hasNext();) {
                TransValuesCum transValuesCum = it.next();

                // Where transactions are before report dates
                if (transValuesCum.transValues.dateint <= fromDateInt) {
                    double splitAdjust = currency == null
                        ? 1.0
                        : currency.adjustRateForSplitsInt(transValuesCum.transValues.dateint,
                                                          fromDateRate,
                                                          fromDateInt) / fromDateRate;
                    // split adjusts last position from TransValuesCum
                    this.startPos = transValuesCum.position * splitAdjust;
                    this.startValue = this.startPrice * this.startPos;
                    startLongBasis = transValuesCum.longBasis;
                    startShortBasis = transValuesCum.shortBasis;

                    // Initializes ending balance sheet values to start values (in
                    // case there are no transactions within report period).
                    this.endPos = this.startPos;
                    this.endValue = this.endPos * this.endPrice;
                    this.longBasis = transValuesCum.longBasis;
                    this.shortBasis = transValuesCum.shortBasis;
                    // increments end cash as well as start
                    this.startCash += transValuesCum.transValues.cashEffect;
                    this.endCash += transValuesCum.transValues.cashEffect;

                }

                // Where transaction period intersects report period
                if (transValuesCum.transValues.dateint > fromDateInt
                    && transValuesCum.transValues.dateint <= toDateInt) {
                    // cf is net cash effect of buy/sell/short/cover, incl
                    // commission
                    double cf = -(transValuesCum.transValues.buy
                                  + transValuesCum.transValues.sell
                                  + transValuesCum.transValues.shortSell
                                  + transValuesCum.transValues.coverShort +
                                  transValuesCum.transValues.commision);

                    // add values to date maps
                    this.arMap.add(transValuesCum.transValues.dateint,
                                   transValuesCum.transValues.cashEffect - transValuesCum.transValues.transfer);
                    this.mdMap.add(transValuesCum.transValues.dateint, cf);
                    this.transMap.add(transValuesCum.transValues.dateint, transValuesCum.transValues.transfer);

                    // Add the cumulative Values (note buys are defined by change in
                    // long basis, same with sells--commission is included).
                    this.buy += transValuesCum.transValues.buy == 0.0
                        ? 0.0
                        : -transValuesCum.transValues.buy - transValuesCum.transValues.commision;
                    this.sell += transValuesCum.transValues.sell == 0.0
                        ? 0.0
                        : -transValuesCum.transValues.sell - transValuesCum.transValues.commision;
                    this.shortSell += transValuesCum.transValues.shortSell == 0.0
                        ? 0.0
                        : -transValuesCum.transValues.shortSell - transValuesCum.transValues.commision;
                    this.coverShort += transValuesCum.transValues.coverShort == 0.0
                        ? 0.0
                        : -transValuesCum.transValues.coverShort - transValuesCum.transValues.commision;
                    this.income += transValuesCum.transValues.income;
                    this.expense = transValuesCum.transValues.expense;
                    this.realizedGain = transValuesCum.perRealizedGain;

                    // retrieves ending balance sheet variables
                    this.endCash += transValuesCum.transValues.cashEffect;
                    double splitAdjust = currency == null
                        ? 1.0
                        : currency.adjustRateForSplitsInt(transValuesCum.transValues.dateint,
                                                         toDateRate,
                                                         toDateInt) / toDateRate;

                    this.endPos = transValuesCum.position * splitAdjust;
                    this.endValue = this.endPos * this.endPrice;
                    this.longBasis = transValuesCum.longBasis;
                    this.shortBasis = transValuesCum.shortBasis;

                } // end--where transaction period intersects report period
            } // end of input transaction set loop

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

            // Get performance date--first Mod Dietz Returns
            //

            // Add the first value in return arrays (if startpos != 0)
            if (this.startPos != 0) {
                this.arMap.add(fromDateInt, -this.startValue);
                this.mdMap.add(fromDateInt, 0.0); // adds dummy value for mod-dietz
            }

            // add the last value in return arrays (if endpos != 0)
            if (this.endPos != 0) {
                this.arMap.add(toDateInt, this.endValue);
                this.mdMap.add(toDateInt, 0.0); // adds dummy value for mod-dietz
            }

            this.mdReturn = computeMDReturn(this.startValue, this.endValue, this.income,
                                      this.expense, this.mdMap);

            // Then, get annualized returns
            //
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


    /**
     * Generates individual line report body.
     *
     * @param thisFT
     *            report line
     *
     * @return array of values
     */
    public Object[] toTableRow() {
        ArrayList<Object> snapValues = new ArrayList<Object>();
        String tilde = "\u007e";
        switch (aggType) {
        case SEC: // individual security
            snapValues.add(this.account.getParentAccount().getAccountName());
            snapValues.add(this.account.getAccountName());
            break;
        case ACCT_SEC: // aggregated securities
            snapValues.add(this.account.getAccountName());
            snapValues.add(tilde + "AllSec");
            break;
        case ACCT_CASH: // cash balance
            snapValues.add(this.account.getAccountName());
            snapValues.add(tilde + "Cash");
            break;
        case ACCT_SEC_PLUS_CASH: // aggregated securities + cash
            snapValues.add(this.account.getAccountName());
            snapValues.add(tilde + "AllSec+Cash");
            break;
        case ALL_SEC: // all securities
            snapValues.add(tilde + "ALL");
            snapValues.add(tilde + "AllSec");
            break;
        case ALL_CASH: // all cash
            snapValues.add(tilde + "ALL");
            snapValues.add(tilde + "Cash");
            break;
        case ALL_SEC_PLUS_CASH: // all securities + cash
            snapValues.add(tilde + "ALL");
            snapValues.add(tilde + "AllSec+Cash");
            break;
        }
        snapValues.add(this.ticker);
        snapValues.add(this.startPos);
        snapValues.add(this.endPos);
        snapValues.add(this.startPrice);
        snapValues.add(this.endPrice);
        snapValues.add(this.startValue);
        snapValues.add(this.endValue);
        snapValues.add(this.buy);
        snapValues.add(this.sell);
        snapValues.add(this.shortSell);
        snapValues.add(this.coverShort);
        snapValues.add(this.income);
        snapValues.add(this.expense);
        snapValues.add(this.longBasis);
        snapValues.add(this.shortBasis);
        snapValues.add(this.realizedGain);
        snapValues.add(this.unrealizedGain);
        snapValues.add(this.totalGain);
        snapValues.add(this.mdReturn);
        snapValues.add(this.annualPercentReturn);
        return snapValues.toArray();
    }


    /**
     * Add two SecurityFromToReport objects, in order to aggregate data at account level or at
     * aggregate level.
     *
     * @param operand
     *            SecurityFromToReport associated with Security
     *
     * @return output SecurityFromToReport
     */
    public SecurityFromToReport addTo(SecurityReport opd) {
        SecurityFromToReport operand = (SecurityFromToReport)opd;

        this.fromDateInt = this.fromDateInt;
        this.toDateInt = this.toDateInt;
        this.account = this.account;
        this.startPos = 0.0;
        this.endPos = 0.0;
        this.startPrice = 0.0;
        this.endPrice = 0.0;
        this.startValue += operand.startValue;
        this.endValue +=  operand.endValue;
        this.buy += operand.buy;
        this.sell += operand.sell;
        this.shortSell += operand.shortSell;
        this.coverShort += operand.coverShort;
        this.income += operand.income;
        this.expense += operand.expense;
        this.longBasis += operand.longBasis;
        this.shortBasis += operand.shortBasis;
        this.realizedGain += operand.realizedGain;
        this.unrealizedGain += operand.unrealizedGain;
        this.totalGain += operand.totalGain;
        this.startCash += operand.startCash;
        this.endCash += operand.endCash;

        // need to handle both cases of aggregation (1) at investment account
        // and (2) across multiple investment account
        if (operand.account == null) {
            this.initBalance = operand.initBalance;
        } else if (operand.account.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT) {
            this.initBalance += operand.initBalance;
        }

        this.arMap = this.arMap.combine(operand.arMap, "add");
        this.mdMap = this.mdMap.combine(operand.mdMap, "add");
        this.transMap = this.transMap.combine(operand.transMap, "add");
        this.mdReturn = 0.0;
        this.annualPercentReturn = 0.0;

        return this;
    }


    /**
     * Update returns in Investment Account level for aggregated securities.
     *
     * @param thisInvFromTo
     *            aggregated securities SecurityFromToReport
     *
     */
    public void recomputeAggregateReturns() {
        // get Mod-Dietz Returns
        double mdReturnVal = computeMDReturn(startValue, endValue, income, expense, mdMap);

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

        // remove start and end values from return date maps (to avoid conflicts
        // in aggregation)
        if (startValue != 0) {
            arMap.add(fromDateInt, startValue);
        }
        if (endValue != 0) {
            arMap.add(toDateInt, -endValue);
        }
    }


    /**
     * Computes from-to report for cash associated with this investment account.
     *
     * @param cashFromTo
     *            SecurityFromToReport associated with Account "bank" transactions
     *
     * @return RepFrontTo representing income/returns for cash portion of
     *         Investment Account
     */
    public SecurityFromToReport computeCashReturns(SecurityReport cash, AGG_TYPE aggType) {
        SecurityFromToReport cashFromTo = (SecurityFromToReport)cash;

        // cashValue has start, end cash positions, income and expenses
        SecurityFromToReport cashValue = new SecurityFromToReport(this.account,
                                                                  this.fromDateInt,
                                                                  this.toDateInt,
                                                                  aggType);

        // comboTransMDMap has purchases/sales of cash (i.e. reverse of security
        // transactions) start by adding transfers in and out of securities and
        // investment accounts
        DateMap comboTransMDMap = this.transMap.combine(cashFromTo.transMap, "add");

        // generate starting and ending cash balances, non-security related
        // account transactions
        double initBal = cashFromTo.initBalance;
        cashValue.startValue = cleanedValue(this.startCash + cashFromTo.startCash + initBal);
        cashValue.endValue = cleanedValue(this.endCash + cashFromTo.endCash + initBal);
        cashValue.startPos = cashValue.startValue;
        cashValue.endPos = cashValue.endValue;
        cashValue.income = cashFromTo.income;
        cashValue.expense = cashFromTo.expense;

        // now add transfer map to map of buys/sells/income/expense
        // (effectively, purchase/sales of cash caused by security activity
        comboTransMDMap = comboTransMDMap.combine(this.arMap, "add");

        // cashRetMap effectively reverses sign of previous map (so cash
        // buys/sells with correct sign for returns calc), and adds
        // account-level income/expense transactions from arMap (e.g. account
        // interest)
        DateMap cashRetMap = cashFromTo.arMap.combine(comboTransMDMap, "subtract");

        // this handles case where fromDateInt < first transaction, AND initBal
        // != 0 (i.e. startValue = initBal. In that case, start date needs to be
        // adjusted to day prior to first transaction date
        int adjFromDateInt = cashValue.fromDateInt;
        int minDateInt = comboTransMDMap.isEmpty()
            ? 0
            : DateUtils.getPrevBusinessDay(comboTransMDMap.firstKey());
        if (cashValue.startValue == initBal
            && cashValue.fromDateInt <= minDateInt) {
            adjFromDateInt = Math.max(cashValue.fromDateInt, minDateInt);
        }
        // add dummy (zero) values to Mod-Dietz date maps, start and end to
        // return maps
        if (Math.abs(cashValue.startValue) > 0.0001) {
            comboTransMDMap.add(adjFromDateInt, 0.0);
            cashRetMap.add(adjFromDateInt, -cashValue.startValue);
        }
        if (Math.abs(cashValue.endValue) > 0.0001) {
            comboTransMDMap.add(this.toDateInt, 0.0);
            cashRetMap.add(this.toDateInt, cashValue.endValue);// add start and end values w/ cash
                                                               // balances for ret Calc
        }
        // calculate returns
        cashValue.mdReturn = computeMDReturn(cashValue.startValue,
                                             cashValue.endValue, cashValue.income, cashValue.expense,
                                             comboTransMDMap);
        cashValue.annualPercentReturn = computeAnnualReturn(cashRetMap, cashValue.mdReturn);

        // add maps for auditing purposes
        cashValue.mdMap = comboTransMDMap;
        cashValue.arMap = cashRetMap;

        return cashValue;
    }


    /**
     * Computes FromTo report for this investment account, with associated cash
     * accounted for as a security.
     *
     * @param thisCashFromTo SecurityFromToReport associated with Account "bank" transactions
     *
     * @return RepFrontTo representing income/returns for Investment Account,
     *         Cash and Securities included
     */
    public SecurityFromToReport computeAggregateReturnWCash(SecurityReport cash,
                                                            AGG_TYPE aggType) {
        SecurityFromToReport invValue = new SecurityFromToReport(this.account,
                                                                 this.fromDateInt,
                                                                 this.toDateInt,
                                                                 aggType);

        SecurityFromToReport thisCashFromTo = (SecurityFromToReport)cash;

        // copy over aggregate values from aggregated securities
        invValue.buy = this.buy;
        invValue.sell = this.sell;
        invValue.shortSell = this.shortSell;
        invValue.coverShort = this.coverShort;
        invValue.longBasis = this.longBasis;
        invValue.shortBasis = this.shortBasis;
        invValue.realizedGain = this.realizedGain;
        invValue.unrealizedGain = this.unrealizedGain;
        invValue.totalGain = this.totalGain;

        // add balance sheet and income statement values where applicable
        invValue.startValue = this.startValue + thisCashFromTo.startValue;
        invValue.endValue = this.endValue + thisCashFromTo.endValue;
        invValue.income = this.income + thisCashFromTo.income;
        invValue.expense = this.expense + thisCashFromTo.expense;
        invValue.startCash = this.startCash + thisCashFromTo.startCash;
        invValue.endCash = this.endCash + thisCashFromTo.endCash;

        // combine transfer date map
        invValue.transMap = this.transMap.combine(thisCashFromTo.transMap, "add");

        // get correct start and end balances w/ cash accounted for
        double initBal = thisCashFromTo.initBalance;
        invValue.startValue = cleanedValue(invValue.startValue + invValue.startCash + initBal);
        invValue.endValue = cleanedValue(invValue.endValue + invValue.endCash + initBal);

        // from account returns perspective, only transfers matter, so they
        // become the "buys" and "sells" for MD returns calculations and annual
        // returns calcs

        // get MD returns
        DateMap mdMap = invValue.transMap;
        /* reverse transfer map for returns calc purposes */
        DateMap retMap = new DateMap().combine(invValue.transMap, "subtract");

        // this handles case where fromDateInt < first transaction, AND initBal
        // != 0 (i.e. startValue = initBal). In that case, start date needs to
        // be adjusted to day prior to first transaction date
        int adjFromDateInt = invValue.fromDateInt;
        int minDateInt = mdMap.isEmpty()
            ? 0
            : DateUtils.getPrevBusinessDay(mdMap.firstKey());

        if (invValue.startValue == initBal && invValue.fromDateInt <= minDateInt) {
            adjFromDateInt = Math.max(invValue.fromDateInt, minDateInt);
        }
        // add dummy values to Mod-Dietz date maps, start and end to return maps
        if (Math.abs(invValue.startValue) > 0.0001) {
            mdMap.add(adjFromDateInt, 0.0);
            retMap.add(adjFromDateInt, -invValue.startValue);
        }
        if (Math.abs(invValue.endValue) > 0.0001) {
            mdMap.add(invValue.toDateInt, 0.0);
            retMap.add(this.toDateInt, invValue.endValue);
        }

        // calc returns (note--no income/expenses since only transfers are
        // considered i.e. endValue includes income/expenses
        double allMDReturn = computeMDReturn(invValue.startValue, invValue.endValue, 0.0, 0.0, mdMap);
        invValue.mdReturn = allMDReturn;

        // get annualized returns
        invValue.annualPercentReturn = computeAnnualReturn(retMap, allMDReturn);

        // add maps for auditing purposes
        invValue.mdMap = mdMap;
        invValue.arMap = retMap;

        return invValue;
    }
}
