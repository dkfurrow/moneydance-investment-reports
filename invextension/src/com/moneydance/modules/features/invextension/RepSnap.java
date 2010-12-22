/*
 *  RepSnap.java
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
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeMap;

/** produces report for security defined by one date ("snapDate")
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public class RepSnap {

    int snapDateInt;
    Account account;
    CurrencyType cur;
    String ticker;

    double lastPrice;                           //ending price
    double endPos;                              //ending position
    double endValue;                            //ending value
    double endCash;                             //ending effect of security on account cash
    double startBalance;                        // initial investment account balance
    double avgCostBasis;                        //final average cost balance
    //one day values
    double absPriceChange;                      //absolute price change (from previous day to snapDate)
    double pctPriceChange;                      //percent price change (from previous day to snapDate)
    double absValueChange;                      //absolute value change (from previous day to snapDate)
    //total numbers
    double income;                              //total income (all dates)
    double totalGain;                           //total absolute gain (all dates)
    double totRetAll;                           //total Mod-Dietz return (all dates)
    double annRetAll;                           //annualized return (all dates)
    //returns
    double totRet1Day;                          //Mod-Dietz return (1 day)
    double totRetWk;                            //Mod-Dietz return (1 week)
    double totRet4Wk;                           //Mod-Dietz return (1 month)
    double totRet3Mnth;                         //Mod-Dietz return (3 month)
    double totRetYTD;                           //Mod-Dietz return (Year-to-Date)
    double totRetYear;                          //Mod-Dietz return (1 Year)
    double totRet3year;                         //Mod-Dietz return (1 Years)
    // intermediate values
    LinkedHashMap<String, Integer> retDateMap;              //maps return category to start dates
    LinkedHashMap<String, Double> startValues;              //maps return category to start values
    LinkedHashMap<String, Double> incomes;                  //maps return category to income
    LinkedHashMap<String, Double> expenses;                 //maps return category to expense
    LinkedHashMap<String, Double> mdReturns;                //maps return category to Mod-Dietz returns
    LinkedHashMap<String, Double> startCashs;               //maps return category to starting cash effect
    LinkedHashMap<String, TreeMap<Integer, Double>> mdMap;  //maps return category to Mod-Dietz date map
    LinkedHashMap<String, TreeMap<Integer, Double>> arMap;  //maps return category to Annualized Return Date Map
    LinkedHashMap<String, TreeMap<Integer, Double>> transMap;//maps return category to transfer date map

    /**
     * Generic constructor, used to aggregate values
     * @param account reference account
     * @param snapDateInt snap date
     */

    public RepSnap(Account account, int snapDateInt) {
        this.snapDateInt = snapDateInt;
        this.account = account;
        this.ticker = "~Null";
        this.lastPrice = 0.0;
        this.endPos = 0.0;
        this.endValue = 0.0;
        this.endCash = 0.0;
        this.startBalance = 0.0;
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
        this.retDateMap = new LinkedHashMap<String, Integer>();
        this.startValues = new LinkedHashMap<String, Double>();
        this.incomes = new LinkedHashMap<String, Double>();
        this.expenses = new LinkedHashMap<String, Double>();
        this.mdReturns = new LinkedHashMap<String, Double>();
        this.startCashs = new LinkedHashMap<String, Double>();

        this.mdMap = new LinkedHashMap<String, TreeMap<Integer, Double>>();
        this.arMap = new LinkedHashMap<String, TreeMap<Integer, Double>>();
        this.transMap = new LinkedHashMap<String, TreeMap<Integer, Double>>();
        


    }

    /**
     * constructor for individual security
     * @param currentInfo current account data
     * @param transSet transaction set to analyze
     * @param snapDateInt  report date date
     */

    public RepSnap(BulkSecInfo currentInfo, SortedSet<TransValuesCum> transSet, int snapDateInt) {

        this.account = transSet.first().transValues.secAccount;
        CurrencyType thisCur = currentInfo.secCur.get(this.account);
        this.cur = thisCur;
        this.ticker = "~Null";
        if(thisCur != null) this.ticker =
                thisCur.getTickerSymbol().isEmpty() ?
                    "NoTicker" : thisCur.getTickerSymbol();
        this.snapDateInt = snapDateInt;
        this.lastPrice = (1 / (thisCur == null ? 1 : thisCur.getUserRateByDateInt(snapDateInt)));
        if (transSet.first().transValues.accountRef.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT) {
            this.startBalance = SecReportProd.longToDouble(transSet.first().transValues.accountRef.getStartBalance()) / 100.0;
        } else {
            this.startBalance = 0.0;
        }

        //create dates for returns calculations

        int fromDateInt = DateUtils.getPrevBusinessDay(transSet.first().transValues.dateint); //ensures all dates for appropriate variables
        int prevFromDateInt = DateUtils.getPrevBusinessDay(snapDateInt);
        int wkFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addDaysInt(snapDateInt, -7));
        int mnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -1));
        int threeMnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -3));
        int oneYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -12));
        int threeYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -36));
        int ytdFromDateInt = DateUtils.getStartYear(snapDateInt);


        //put dates in return map
        this.retDateMap = new LinkedHashMap<String, Integer>();

        this.retDateMap.put("All", fromDateInt);
        this.retDateMap.put("PREV", prevFromDateInt);
        this.retDateMap.put("1Wk", wkFromDateInt);
        this.retDateMap.put("4Wk", mnthFromDateInt);
        this.retDateMap.put("3Mnth", threeMnthFromDateInt);
        this.retDateMap.put("1Yr", oneYearFromDateInt);
        this.retDateMap.put("3Yr", threeYearFromDateInt);
        this.retDateMap.put("YTD", ytdFromDateInt);

        //these values dependent only on snapDate

        double endCumUnrealizedGain = 0;
        double longBasis = 0;
        double shortBasis = 0;
        

        // these values calculated once, based on total transaction history

        double realizedGain = 0;
        double unrealizedGain = 0;
        double startCumUnrealGain = 0; // by definition, since this calculation covers all transactions
        double annualPercentReturn = 0;

        // initialize array lists needed for returns calculations
        LinkedHashMap<String, Double> startPos = new LinkedHashMap<String, Double>();
        this.startValues = new LinkedHashMap<String, Double>();
        LinkedHashMap<String, Double> startPrice = new LinkedHashMap<String, Double>();
        this.startCashs = new LinkedHashMap<String, Double>();
        this.incomes = new LinkedHashMap<String, Double>();
        this.expenses = new LinkedHashMap<String, Double>();
        this.mdReturns = new LinkedHashMap<String, Double>();

        this.arMap = new LinkedHashMap<String, TreeMap<Integer, Double>>();
        this.mdMap = new LinkedHashMap<String, TreeMap<Integer, Double>>();
        this.transMap = new LinkedHashMap<String, TreeMap<Integer, Double>>();
        

        // fill startPrice Array List
        
        for (Iterator<String> it = this.retDateMap.keySet().iterator(); it.hasNext();) {
            String retCat = (String) it.next();
            int thisDateInt = this.retDateMap.get(retCat);
            startPrice.put(retCat, 1 / (thisCur == null ? 1 : thisCur.getUserRateByDateInt(thisDateInt)));
        }
        //initialize ArrayLists values to zero (so we can use "set" method later
        
        for (Iterator<String> it1 = this.retDateMap.keySet().iterator(); it1.hasNext();) {
            String retCat = (String) it1.next();
            startPos.put(retCat, 0.0);
            this.startValues.put(retCat, 0.0);
            this.incomes.put(retCat, 0.0);
            this.expenses.put(retCat, 0.0);
            this.startCashs.put(retCat, 0.0);
            this.mdReturns.put(retCat, 0.0);

            this.arMap.put(retCat, new TreeMap<Integer, Double>());
            this.mdMap.put(retCat, new TreeMap<Integer, Double>());
            this.transMap.put(retCat, new TreeMap<Integer, Double>());
        }

        /* intialize other variables */
        this.endPos = 0.0;
        this.endValue = 0.0;
        this.endCash = 0.0;
        this.avgCostBasis = 0.0;
        //one day values
        this.absPriceChange = 0.0;
        this.pctPriceChange = 0.0;
        this.absValueChange = 0.0;
        //total numbers
        this.income = 0.0;
        this.totalGain = 0.0;
        this.totRetAll = 0.0;
        this.annRetAll = 0.0;
        //returns
        this.totRet1Day = 0.0;
        this.totRetWk = 0.0;
        this.totRet4Wk = 0.0;
        this.totRet3Mnth = 0.0;
        this.totRetYTD = 0.0;
        this.totRetYear = 0.0;
        this.totRet3year = 0.0;

        // iterate through transaction values list
        for (Iterator<TransValuesCum> it = transSet.iterator(); it.hasNext();) {
            TransValuesCum transValuesCum = it.next();
            //iterate through return dates
            for (Iterator<String> it1 = this.retDateMap.keySet().iterator(); it1.hasNext();) {
                String retCat = (String) it1.next();
                int thisFromDateInt = this.retDateMap.get(retCat);
                //where transactions are before report dates
                if (transValuesCum.transValues.dateint <= thisFromDateInt) {

                    double currentRate = (thisCur == null ? 1
                            : thisCur.getUserRateByDateInt(thisFromDateInt));
                    double splitAdjust = (thisCur == null ? 1
                            : thisCur.adjustRateForSplitsInt(transValuesCum.transValues.dateint,
                            currentRate, thisFromDateInt) / currentRate);
                    startPos.put(retCat, transValuesCum.position * splitAdjust); //split adjusts last position from TransValuesCum
                    this.startValues.put(retCat, startPrice.get(retCat) * startPos.get(retCat));
                    this.startCashs.put(retCat, this.startCashs.get(retCat)
                            + transValuesCum.transValues.cashEffect);

                    if ("All".equals(retCat)) {
                        //end cash increment, only needs to be done once ("All" loop for convenience)
                        this.endCash = this.endCash + transValuesCum.transValues.cashEffect;
                    }

                }

                //where transaction period intersects report period
                if (transValuesCum.transValues.dateint > thisFromDateInt
                        && transValuesCum.transValues.dateint <= snapDateInt) { 

                    // MDCalc variable--net effect of calculation is to return buys and sells, including commission
                    double cf = -(transValuesCum.transValues.buy
                            + transValuesCum.transValues.sell
                            + transValuesCum.transValues.shortSell 
                            + transValuesCum.transValues.coverShort
                            + transValuesCum.transValues.commision);

                    //add variables to arrays needed for returns calculation

                    RepFromTo.addValueToDateMap(this.arMap.get(retCat),
                            transValuesCum.transValues.dateint,
                            transValuesCum.transValues.cashEffect
                            - transValuesCum.transValues.transfer);
                    RepFromTo.addValueToDateMap(this.mdMap.get(retCat),
                            transValuesCum.transValues.dateint, cf);
                    RepFromTo.addValueToDateMap(this.transMap.get(retCat),
                            transValuesCum.transValues.dateint,
                            transValuesCum.transValues.transfer);

                    this.incomes.put(retCat, this.incomes.get(retCat)
                            + transValuesCum.transValues.income);
                    this.expenses.put(retCat, this.expenses.get(retCat)
                            + transValuesCum.transValues.expense);

                    if ("All".equals(retCat)) {//end cash increment--only needs to be done once
                        realizedGain = transValuesCum.perRealizedGain + realizedGain;
                        this.endCash = this.endCash + transValuesCum.transValues.cashEffect;
                    }

                    double currentRate = (thisCur == null ? 1 :
                        thisCur.getUserRateByDateInt(snapDateInt));
                    double splitAdjust = (thisCur == null ? 1 :
                        thisCur.adjustRateForSplitsInt(transValuesCum.transValues.dateint,
                            currentRate, snapDateInt) / currentRate);

                    this.endPos = transValuesCum.position * splitAdjust;
                    this.endValue = this.endPos * this.lastPrice;
                    longBasis = transValuesCum.longBasis;
                    shortBasis = transValuesCum.shortBasis;
                    this.avgCostBasis = longBasis + shortBasis;

                } //end--where transaction period intersects report period
            } //end of start date iterative loop
        } // end of input transaction set loop

        if (this.endPos > 0) {
            endCumUnrealizedGain = this.endValue - longBasis;
        } else if (this.endPos < 0) {
            endCumUnrealizedGain = this.endValue - shortBasis;
        }
        unrealizedGain = endCumUnrealizedGain - startCumUnrealGain; //startCumUnrealGain is zero by definition
        this.totalGain = realizedGain + unrealizedGain;

        //now go through arrays and get returns/calc values
        for (Iterator<String> it1 = this.retDateMap.keySet().iterator(); it1.hasNext();) {
            String retCat = (String) it1.next();
            int thisFromDateInt = this.retDateMap.get(retCat);
            // add the first value in return arrays (if startpos != 0)
            if (startPos.get(retCat) != 0) {
                RepFromTo.addValueToDateMap(
                        this.arMap.get(retCat),
                        thisFromDateInt, -this.startValues.get(retCat));
                RepFromTo.addValueToDateMap(
                        this.mdMap.get(retCat),
                        thisFromDateInt, 0.0); //Dummy Value for Mod-Dietz
            }
            // add the last value in return arrays (if endpos != 0)
            if (this.endPos != 0) {
                RepFromTo.addValueToDateMap(
                        this.arMap.get(retCat),
                        snapDateInt, this.endValue);
                RepFromTo.addValueToDateMap(
                        this.mdMap.get(retCat),
                        snapDateInt, 0.0);//Dummy Value for Mod-Dietz
            }

            // get MD returns on all start dates, only get annualized return on all dates

            this.mdReturns.put(retCat, RepFromTo.getMDCalc(this.startValues.get(retCat),
                    this.endValue, this.incomes.get(retCat), this.expenses.get(retCat),
                    this.mdMap.get(retCat)));

            if ("All".equals(retCat)) {
                annualPercentReturn = RepFromTo.getAnnualReturn(this.arMap.get(retCat),
                        this.mdReturns.get("All"));
                this.annRetAll = annualPercentReturn;
                this.income = this.incomes.get(retCat);
            }

            //remove start and end values from return date maps for ease of aggregation
            if (startPos.get(retCat) != 0) {
                RepFromTo.addValueToDateMap(this.arMap.get(retCat),
                        thisFromDateInt, +this.startValues.get(retCat));
            }
            //remove start and end values from return date maps for ease of aggregation
            if (this.endPos != 0) {
                RepFromTo.addValueToDateMap(this.arMap.get(retCat),
                        snapDateInt, -this.endValue);
            }
        } // end of start date iterative loop

        //Produce output

        if (this.retDateMap.get("PREV") == null) {
            this.absPriceChange = Double.NaN;
            this.absValueChange = Double.NaN;
            this.pctPriceChange = Double.NaN;
        } else {
            double prevPrice = (thisCur == null ? 1 :
                1 / thisCur.getUserRateByDateInt(this.retDateMap.get("PREV")));
            this.absPriceChange = this.lastPrice - prevPrice;
            this.absValueChange = this.endPos * this.absPriceChange;
            this.pctPriceChange = this.lastPrice / prevPrice - 1;
        }
        this.totRet1Day = this.mdReturns.get("PREV") == null ? Double.NaN : this.mdReturns.get("PREV");
        this.totRetAll = this.mdReturns.get("All") == null ? Double.NaN : this.mdReturns.get("All");
        this.totRetWk = this.mdReturns.get("1Wk") == null ? Double.NaN : this.mdReturns.get("1Wk");
        this.totRet4Wk = this.mdReturns.get("4Wk") == null ? Double.NaN : this.mdReturns.get("4Wk");
        this.totRet3Mnth = this.mdReturns.get("3Mnth") == null ? Double.NaN : this.mdReturns.get("3Mnth");
        this.totRetYear = this.mdReturns.get("1Yr") == null ? Double.NaN : this.mdReturns.get("1Yr");
        this.totRet3year = this.mdReturns.get("3Yr") == null ? Double.NaN : this.mdReturns.get("3Yr");
        this.totRetYTD = this.mdReturns.get("YTD") == null ? Double.NaN : this.mdReturns.get("YTD");
        
        
    }

     /*
     * header for report output
     */
    public static StringBuffer listTransValuesSnapHeader(int snapDateInt) {
        StringBuffer snapValues = new StringBuffer();
        snapValues.append("Snapshot Report,");
        snapValues.append("Report Date,");
        snapValues.append(DateUtils.convertToShort(snapDateInt));
        snapValues.append("\r\n");
        snapValues.append("PrntAcct" + ",");
        snapValues.append("Account" + ",");
        snapValues.append("Ticker" + ",");
        snapValues.append("LastPrice,");
        snapValues.append("EndPos,");
        snapValues.append("EndValue,");
        snapValues.append("AbsPrcChg,");
        snapValues.append("AbsValChg,");
        snapValues.append("PctPrcChg,");
        snapValues.append("TR1Day,");
        snapValues.append("TR1Wk,");
        snapValues.append("TR1Mth,");
        snapValues.append("TR3Mth,");
        snapValues.append("TR_YTD,");
        snapValues.append("TR1Year,");
        snapValues.append("TR3Year,");
        snapValues.append("TR_ALL,");
        snapValues.append("AnnRet_ALL,");
        snapValues.append("AvgCost,");
        snapValues.append("income,");
        snapValues.append("TotGain");
//        snapValues.append("\n");

        return snapValues;
    }

     /**
     * generates individual line report body
     * @param thisSnap report line
     * @param reportType level of aggregation (security, account, aggregate, cash, etc)
     * @return array of values
     */
    public static String[] loadTransValuesSnap(RepSnap thisSnap, int reportType) {
        ArrayList<String> snapValues = new ArrayList<String>();

        switch (reportType) {
            case 1: // individual security
                snapValues.add(thisSnap.account.getParentAccount().getAccountName());
                snapValues.add(thisSnap.account.getAccountName());
                break;
            case 2: //aggregated securities
                snapValues.add(thisSnap.account.getAccountName());
                snapValues.add("~AllSec");
                break;
            case 3: //cash balance
                snapValues.add(thisSnap.account.getAccountName());
                snapValues.add("~Cash");
                break;
            case 4: //aggregated securities + cash
                snapValues.add(thisSnap.account.getAccountName());
                snapValues.add("~AllSec+Cash");
                break;
            case 5: //all securities
                snapValues.add("~ALL");
                snapValues.add("~AllSec");
                break;
            case 6: //all cash
                snapValues.add("~ALL");
                snapValues.add("~Cash");
                break;
            case 7: //all securities +  cash
                snapValues.add("~ALL");
                snapValues.add("~AllSec+Cash");
                break;
        }
        snapValues.add(thisSnap.ticker);
        snapValues.add(Double.toString(thisSnap.lastPrice));
        snapValues.add(Double.toString(thisSnap.endPos));
        snapValues.add(Double.toString(thisSnap.endValue));
        snapValues.add(Double.toString(thisSnap.absPriceChange));
        snapValues.add(Double.toString(thisSnap.absValueChange));
        snapValues.add(Double.toString(thisSnap.pctPriceChange));
        snapValues.add(Double.toString(thisSnap.totRet1Day));
        snapValues.add(Double.toString(thisSnap.totRetWk));
        snapValues.add(Double.toString(thisSnap.totRet4Wk));
        snapValues.add(Double.toString(thisSnap.totRet3Mnth));
        snapValues.add(Double.toString(thisSnap.totRetYTD));
        snapValues.add(Double.toString(thisSnap.totRetYear));
        snapValues.add(Double.toString(thisSnap.totRet3year));
        snapValues.add(Double.toString(thisSnap.totRetAll));
        snapValues.add(Double.toString(thisSnap.annRetAll));
        snapValues.add(Double.toString(thisSnap.avgCostBasis));
        snapValues.add(Double.toString(thisSnap.income));
        snapValues.add(Double.toString(thisSnap.totalGain));
        return snapValues.toArray(new String[snapValues.size()]);
    }
}
