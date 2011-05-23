/*
 *  RepFromTo.java
 *  Copyright (C) 2010 Dale Furrow
 *  dkfurrow@google.com
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, a copy may be found at
 *  http://www.gnu.org/licenses/lgpl.html
 */
package com.moneydance.modules.features.invextension;


import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.CurrencyType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;
import com.moneydance.modules.features.invextension.BulkSecInfo.AGG_TYPE;

/** produces report for security defined by "from" date and "to" date
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/


public class RepFromTo {

    int fromDateInt;                /*start of report period */
    int toDateInt;                  /*end of report period */
    Account account;                /*reference account */
    String ticker;
    double startPos;                /*starting position */
    double endPos;                  /*ending position */
    double startPrice;              /*starting price */
    double endPrice;                /*ending price */
    double startValue;              /*starting value */
    double endValue;                /*ending value */
    double startCash;               /*starting "cash effect" (security effect on account cash) */
    double endCash;                 /*ending "cash effect" (security affect on account cash) */
    double initBalance;            /*initial balance of account */
    double buy;                     /*cumulative cash effect of buys (including commission) */
    double sell;                    /*cumulative cash effect of sells (including commission)  */
    double shortSell;               /*cumulative cash effect of shorts (including commission) */
    double coverShort;              /*cumulative cash effect of covers (including commission) */
    double income;                  /*cumulative income */
    double expense;                 /*cumulative expense */
    double longBasis;               /*ending average cost basis of long positions */
    double shortBasis;              /*ending average cost basis of short positions*/
    double realizedGain;            /*cumulative realized gains (against avg cost) */
    double unrealizedGain;          /*cumulative unrealized gains */
    double totalGain;            /*sum of realized and unrealized gains */
    double mdReturn;           /*period total return (Mod-Dietz method) */
    double annualPercentReturn;     /*period annualized return (Mod-Dietz method) */
    TreeMap<Integer, Double> arMap; /*date map of annual return data */
    TreeMap<Integer, Double> mdMap; /*date map of Mod-Dietz return data */
    TreeMap<Integer, Double> transMap;  /*date map of transfer data */
    
    /**
     * generic constructor, used to aggregate data from multiple securities
     * @param account reference account
     * @param fromDateInt "from" date
     * @param toDateInt "to" date
     */
    public RepFromTo(Account account, int fromDateInt, int toDateInt) { //constructor used for aggregation

        this.fromDateInt = fromDateInt;
        this.toDateInt = toDateInt;
        this.account = account;
        this.ticker = "~Null";
        this.startPos = 0.0;
        this.endPos = 0.0;
        this.startPrice = 0.0;
        this.endPrice = 0.0;
        this.startValue = 0.0;
        this.endValue = 0.0;
        this.startCash = 0;
        this.endCash = 0;
        this.initBalance = 0.0;
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
        this.arMap = new TreeMap<Integer, Double>();
        this.mdMap = new TreeMap<Integer, Double>();
        this.transMap = new TreeMap<Integer, Double>();
    }

    /**
     * constructor for individual security
     * @param currentInfo current account data
     * @param transSet transaction set to analyze
     * @param fromDateInt "from" date
     * @param toDateInt "to" date
     */

    public RepFromTo(BulkSecInfo currentInfo, SortedSet<TransValuesCum> transSet, int fromDateInt, int toDateInt) {
        this.account = transSet.first().transValues.secAccount;
        this.fromDateInt = fromDateInt;
        this.toDateInt = toDateInt;
        /*Currency type is null in case where account is investment account */
        CurrencyType thisCur = currentInfo.secCur.get(this.account);
        this.ticker = "~Null";
        if(thisCur != null) this.ticker =
                thisCur.getTickerSymbol().isEmpty() ?
                    "NoTicker" : thisCur.getTickerSymbol();
        this.startPrice = thisCur == null ? 1.0 : 1 / thisCur.getUserRateByDateInt(fromDateInt);
        this.endPrice = thisCur == null ? 1.0 : 1 / thisCur.getUserRateByDateInt(toDateInt);
        
        //Initialize variables
        this.startPos = 0;
        this.endPos = 0;
        this.startValue = 0;
        this.endValue = 0;
        this.startCash = 0;
        this.endCash = 0;
        if (transSet.first().transValues.accountRef.getAccountType() ==
                Account.ACCOUNT_TYPE_INVESTMENT) {this.initBalance =
                        ReportProd.longToDouble(transSet.first().
                        transValues.accountRef.getStartBalance()) / 100.0;
        } else {
            this.initBalance = 0.0;
        }
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
        this.arMap = new TreeMap<Integer, Double>();
        this.mdMap = new TreeMap<Integer, Double>();
        this.transMap = new TreeMap<Integer, Double>();

        /* intialize intermediate calculation variables */
        double startCumUnrealGain = 0;
        double endCumUnrealizedGain = 0;
        double startLongBasis = 0;
        double startShortBasis = 0;
        double toDateRate = thisCur == null ? 1.0 : thisCur.getUserRateByDateInt(toDateInt);
        double fromDateRate = thisCur == null ? 1.0 : thisCur.getUserRateByDateInt(fromDateInt);

        /*iterates through full transaction set*/
        for (Iterator<TransValuesCum> it = transSet.iterator(); it.hasNext();) {
            TransValuesCum transValuesCum = it.next();

            /*where transactions are before report dates*/
            if (transValuesCum.transValues.dateint <= fromDateInt) { 


                double splitAdjust = thisCur == null ? 1.0 :
                    thisCur.adjustRateForSplitsInt(transValuesCum.transValues.dateint,
                    fromDateRate, fromDateInt) / fromDateRate;
                this.startPos = transValuesCum.position * splitAdjust; //split adjusts last position from TransValuesCum
                this.startValue = this.startPrice * this.startPos;
                startLongBasis = transValuesCum.longBasis;
                startShortBasis = transValuesCum.shortBasis;

                /*initializes ending balance sheet values to start values
                (in case there are no transactions within report period)*/
                this.endPos = this.startPos;
                this.endValue = this.endPos * this.endPrice;
                this.longBasis = transValuesCum.longBasis;
                this.shortBasis = transValuesCum.shortBasis;
                /* increments end cash as well as start*/
                this.startCash = this.startCash + transValuesCum.transValues.cashEffect;
                this.endCash = this.endCash + transValuesCum.transValues.cashEffect;

            }
            /*where transaction period intersects report period*/
            if (transValuesCum.transValues.dateint > fromDateInt
                    && transValuesCum.transValues.dateint <= toDateInt) { 
                
                //cf is net cash effect of buy/sell/short/cover, incl commission
                double cf = -(transValuesCum.transValues.buy + transValuesCum.transValues.sell
                        + transValuesCum.transValues.shortSell + transValuesCum.transValues.coverShort
                        + transValuesCum.transValues.commision);

                //add values to date maps

                addValueToDateMap(this.arMap, transValuesCum.transValues.dateint,
                        transValuesCum.transValues.cashEffect
                        - transValuesCum.transValues.transfer);
                addValueToDateMap(this.mdMap, transValuesCum.transValues.dateint,
                        cf);
                addValueToDateMap(this.transMap, transValuesCum.transValues.dateint,
                        transValuesCum.transValues.transfer);

                /*add the cumulative Values (note buys are defined by change in
                 long basis, same with sells--commission is included)*/
                this.buy = this.buy + (transValuesCum.transValues.buy == 0 ? 0 :
                    -transValuesCum.transValues.buy - transValuesCum.transValues.commision);
                this.sell = this.sell + (transValuesCum.transValues.sell == 0 ? 0 :
                    -transValuesCum.transValues.sell - transValuesCum.transValues.commision);
                this.shortSell = this.shortSell + (transValuesCum.transValues.shortSell == 0 ? 0 :
                    -transValuesCum.transValues.shortSell - transValuesCum.transValues.commision);
                this.coverShort = this.coverShort + (transValuesCum.transValues.coverShort == 0 ? 0 :
                    -transValuesCum.transValues.coverShort - transValuesCum.transValues.commision);
                this.income = this.income + transValuesCum.transValues.income;
                this.expense = this.expense + transValuesCum.transValues.expense;
                this.realizedGain = this.realizedGain + transValuesCum.perRealizedGain;

                /*retrieves ending balance sheet variables*/
                this.endCash = this.endCash + transValuesCum.transValues.cashEffect;
                double splitAdjust = thisCur == null ? 1.0 :
                    thisCur.adjustRateForSplitsInt(transValuesCum.transValues.dateint,
                    toDateRate, toDateInt) / toDateRate;

                this.endPos = transValuesCum.position * splitAdjust;
                this.endValue = this.endPos * this.endPrice;
                this.longBasis = transValuesCum.longBasis;
                this.shortBasis = transValuesCum.shortBasis;

            } /*end--where transaction period intersects report period*/
        } /* end of input transaction set loop*/

        //calculate the total period unrealized gain

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

        /*get performance date--first Mod Dietz Returns */

        /* add the first value in return arrays (if startpos != 0)*/
        if (this.startPos != 0) {
            addValueToDateMap(this.arMap, fromDateInt, -this.startValue);
            addValueToDateMap(this.mdMap, fromDateInt, 0.0); //adds dummy value for mod-dietz
        }
        /* add the last value in return arrays (if endpos != 0)*/
        if (this.endPos != 0) {
            addValueToDateMap(this.arMap, toDateInt, this.endValue);
            addValueToDateMap(this.mdMap, toDateInt, 0.0); //adds dummy value for mod-dietz
        }

        this.mdReturn = getMDCalc(this.startValue, this.endValue,
                this.income, this.expense, this.mdMap);

        //then, get annualized returns

        this.annualPercentReturn = getAnnualReturn(this.arMap, this.mdReturn);

        // remove start and end values from ar date map for ease of aggregation
        if (this.startPos != 0) {
            addValueToDateMap(this.arMap, fromDateInt, +this.startValue);
        }
        // remove start and end values from date map for ease of aggregation
        if (this.endPos != 0) {
            addValueToDateMap(this.arMap, toDateInt, -this.endValue);
        }
    }

    /**
     * returns annualized returns (same as excel XIRR function)
     * @param retMap date map relating dateInts to cash flows
     * @param mdRet Mod-Dietz total return for inital guess
     * @return
     */
    public static double getAnnualReturn(TreeMap<Integer, Double> retMap, double mdRet) {
        /*assumes first value is startvalue, last is endvalue
        same with dates */

        int numPeriods = retMap == null ? 0 : retMap.keySet().size();
        double[] excelDates = new double[numPeriods];
        double[] annRetValuesArray = new double[numPeriods];
        int[] dateIntsArray = new int[numPeriods];

        /* put datemap info into primitive arrays */
        if (retMap != null && retMap.keySet().size() > 0 && retMap.values().size() > 0) {
            int i = 0;
            for (Iterator it = retMap.keySet().iterator(); it.hasNext();) {
                Integer dateInt = (Integer) it.next();
                Double value = retMap.get(dateInt);
                dateIntsArray[i] = dateInt;
                annRetValuesArray[i] = value;
                i++;
            }

        } else {
            return Double.NaN;
        }

        for (int i = 0; i < numPeriods; i++) {
            double d = DateUtils.getExcelDateValue(dateIntsArray[i]);
            excelDates[i] = d;
        }
        double totYrs = (excelDates[numPeriods - 1] - excelDates[0]) / 365;
        double guess = (1 + mdRet / totYrs); // modified dietz return divided by number of years
        //(have to add 1 because of returns algorithm)
        XIRRData thisData = new XIRRData(numPeriods, guess, annRetValuesArray, excelDates);
        double annualReturn = XIRR.xirr(thisData);

        return annualReturn; //annualReturn;
    }

    /**
     * Get Modified Dietz returns
     * @param startValue starting value (expressed as positive)
     * @param endValue ending value
     * @param income income (to be added to endValue)
     * @param expense expense (to be added to endValue)
     * @param mdMap datemap which relates dates to cash flows
     * @return Mod Dietz return
     */
    public static double getMDCalc(double startValue, double endValue,
            double income, double expense,
            TreeMap<Integer, Double> mdMap) {
        if (mdMap.keySet().size() > 0 && mdMap.values().size() > 0) {
            double mdValue = 0;
            double sumCF = 0;
            double weightCF = 0;

            Integer cd = DateUtils.getDaysBetween(mdMap.firstKey(), mdMap.lastKey());
            Double cdD = cd.doubleValue();

            for (Iterator it = mdMap.keySet().iterator(); it.hasNext();) {
                Integer thisDateInt = (Integer) it.next();
                Double cf = mdMap.get(thisDateInt);
                Integer dayBetw = DateUtils.getDaysBetween(mdMap.firstKey(), thisDateInt);
                Double dayBetD = dayBetw.doubleValue();
                double wSubI = (cdD - dayBetD) / cdD;
                weightCF = weightCF + (wSubI * cf);
                sumCF = sumCF + cf;
            }

            mdValue = ((endValue + income + expense) - startValue - sumCF) / (startValue + weightCF);
            return mdValue;
        } else {
            return Double.NaN;
        }


    }

    /**
     * Adds date/cashflow pair to existing date map
     * @param inMap existing date map
     * @param dateInt dateInto to add
     * @param addValue cashflow to add
     * @return new map
     */

    public static TreeMap<Integer, Double> addValueToDateMap(TreeMap<Integer, Double> inMap, Integer dateInt, Double addValue) {

        if (inMap != null) {
            if (inMap.get(dateInt) == null) {
                inMap.put(dateInt, addValue);
            } else {
                Double tempVal = inMap.get(dateInt);
                tempVal = tempVal + addValue;
                inMap.put(dateInt, tempVal);
            }
        }
        return inMap;
    }

    /*
     * header for report output
     */
    public static StringBuffer listTransValuesFTHeader(int fromDateInt, int toDateInt) {
        StringBuffer snapValues = new StringBuffer();
        snapValues.append("From/To Report,");
        snapValues.append("From,");
        snapValues.append(DateUtils.convertToShort(fromDateInt) + ",");
        snapValues.append("To,");
        snapValues.append(DateUtils.convertToShort(toDateInt));
        snapValues.append("\r\n");
        snapValues.append("PrntAcct,");
        snapValues.append("Security,");
        snapValues.append("Ticker,");
        snapValues.append("StartPos,");
        snapValues.append("EndPos,");
        snapValues.append("StartPrice,");
        snapValues.append("EndPrice,");
        snapValues.append("StartValue,");
        snapValues.append("EndValue,");
        snapValues.append("buy,");
        snapValues.append("sell,");
        snapValues.append("shortSell,");
        snapValues.append("coverShort,");
        snapValues.append("income,");
        snapValues.append("Expense,");
        snapValues.append("longBasis,");
        snapValues.append("shortBasis,");
        snapValues.append("realizedGain,");
        snapValues.append("unrealizedGain,");
        snapValues.append("periodReturn,");
        snapValues.append("percentReturn,");
        snapValues.append("annualPercentReturn");
        return snapValues;
    }

    /*
     * returns Tableheader for report
     */

    public static String[] getRepFromToHeader() {
        ArrayList<String> snapValues = new ArrayList<String>();

        snapValues.add("PrntAcct");
        snapValues.add("Security");
        snapValues.add("Ticker");
        snapValues.add("StartPos");
        snapValues.add("EndPos");
        snapValues.add("StartPrice");
        snapValues.add("EndPrice");
        snapValues.add("StartValue");
        snapValues.add("EndValue");
        snapValues.add("buy");
        snapValues.add("sell");
        snapValues.add("shortSell");
        snapValues.add("coverShort");
        snapValues.add("income");
        snapValues.add("Expense");
        snapValues.add("longBasis");
        snapValues.add("shortBasis");
        snapValues.add("realizedGain");
        snapValues.add("unrealizedGain");
        snapValues.add("periodReturn");
        snapValues.add("percentReturn");
        snapValues.add("annualPercentReturn");
        
       return snapValues.toArray(new String[snapValues.size()]);
    }

    /**
     * generates individual line report body
     * @param thisFT report line
     * @param AGG_TYPE level of aggregation (security, account, aggregate, cash, etc)
     * @return array of values
     */
    public Object[] getRepFromToObject(AGG_TYPE aggType) {
        ArrayList<Object> snapValues = new ArrayList<Object>();
          String tilde = "\u007e";
        switch (aggType) {
            case SEC: // individual security
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

   
}
