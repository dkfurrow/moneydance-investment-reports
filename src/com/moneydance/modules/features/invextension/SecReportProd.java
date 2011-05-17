/*
 *  SecReportPanel.java
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
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;



import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/** produces FromTo and Snap reports
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public final class SecReportProd {

    private SecReportProd(){
    }

    /**
     * produces FromTo Report for securities, accounts, and aggregate of accounts
     * @param currentInfo current transaction info
     * @param fromDateInt from date
     * @param toDateInt to date
     * @return ArrayList of String Arrays for all securities
     */
    public static ArrayList<String[]> getFromToReport(BulkSecInfo currentInfo, int fromDateInt, int toDateInt) {
        ArrayList<String[]> reportArrayList = new ArrayList<String[]>();
        RepFromTo allInvFromTo = new RepFromTo(null, fromDateInt, toDateInt);
        RepFromTo allCashFromTo = new RepFromTo(null, fromDateInt, toDateInt);

        /*loop through investment accounts */
        for (Iterator it = currentInfo.invSec.keySet().iterator(); it.hasNext();) {
            Account invAcct = (Account) it.next();
            RepFromTo thisInvFromTo = new RepFromTo(invAcct, fromDateInt, toDateInt);
            /* loop through securities */
            for (Iterator it1 = currentInfo.invSec.get(invAcct).iterator(); it1.hasNext();) {
                Account secAcct = (Account) it1.next();
                SortedSet<TransValuesCum> transSet = currentInfo.transValuesCumMap.get(secAcct);
                RepFromTo thisSecFromTo = new RepFromTo(currentInfo, transSet, fromDateInt, toDateInt);
                reportArrayList.add(RepFromTo.loadTransValuesFromTo(thisSecFromTo, 1));
                thisInvFromTo = addFT(thisSecFromTo, thisInvFromTo);
            } // end securities loop
            thisInvFromTo = getFTAggReturns(thisInvFromTo); //get aggregated returns for securities in account
            reportArrayList.add(RepFromTo.loadTransValuesFromTo(thisInvFromTo, 2));
            allInvFromTo = addFT(thisInvFromTo, allInvFromTo); // add to aggregate securities.

            SortedSet<TransValuesCum> parentSet =
                    currentInfo.transValuesCumMap.get(invAcct); //gets investment account transactions (bank txns)
            RepFromTo thisCashFromTo =
                    new RepFromTo(currentInfo, parentSet, fromDateInt, toDateInt); //get report for investment account
            allCashFromTo =
                    addFT(thisCashFromTo, allCashFromTo); //add to investment accounts (bank txns)
            RepFromTo cashReport =
                    getFTCashReturns(thisCashFromTo, thisInvFromTo); //get returns for cash account
            reportArrayList.add(RepFromTo.loadTransValuesFromTo(cashReport, 3));

            RepFromTo thisAggRetFromTo =
                    getFTAggRetWCash(thisCashFromTo, thisInvFromTo); //get  aggregated returns with cash accounted for
            reportArrayList.add(RepFromTo.loadTransValuesFromTo(thisAggRetFromTo, 4));
        } //end investment account loop
        //get returns for aggregated investment accounts
        allInvFromTo =
                getFTAggReturns(allInvFromTo); //get aggregated returns from all securities
        reportArrayList.add(RepFromTo.loadTransValuesFromTo(allInvFromTo, 5));

        RepFromTo allCashReport =
                getFTCashReturns(allCashFromTo, allInvFromTo); //get cash returns for all accounts
        reportArrayList.add(RepFromTo.loadTransValuesFromTo(allCashReport, 6));

        RepFromTo allAggRetFromTo =
                getFTAggRetWCash(allCashFromTo, allInvFromTo); //get  agg returns w/ cash for all accounts
        reportArrayList.add(RepFromTo.loadTransValuesFromTo(allAggRetFromTo, 7));

        Collections.sort(reportArrayList, PrntAcct_Order);
        return reportArrayList;
    }

    /**
     * produces FromTo Report for securities, accounts, and aggregate of accounts
     * @param currentInfo current transaction info
     * @param snapDateInt report date
     * @return ArrayList of String Arrays for all securities
     */
    public static ArrayList<String[]> getSnapReport(BulkSecInfo currentInfo, int snapDateInt) {
        ArrayList<String[]> reportArrayList = new ArrayList<String[]>();
        RepSnap allInvSnap = new RepSnap(null, snapDateInt);
        RepSnap allCashSnap = new RepSnap(null, snapDateInt);
        /*loop through investment accounts */
        for (Iterator it = currentInfo.invSec.keySet().iterator(); it.hasNext();) {
            Account invAcct = (Account) it.next();
            RepSnap thisInvSnap = new RepSnap(invAcct, snapDateInt);
            /* loop through securities */
            for (Iterator it1 = currentInfo.invSec.get(invAcct).iterator(); it1.hasNext();) {
                Account secAcct = (Account) it1.next();
                SortedSet<TransValuesCum> transSet = currentInfo.transValuesCumMap.get(secAcct);
                RepSnap thisSecSnap = new RepSnap(currentInfo, transSet, snapDateInt);
                reportArrayList.add(RepSnap.loadTransValuesSnap(thisSecSnap, 1));
                thisInvSnap = addSnap(thisSecSnap, thisInvSnap);
            }// end securities loop
            RepSnap thisInvRepSnap =
                    getSnapAggReturns(thisInvSnap); //get aggregated returns for securities
            reportArrayList.add(RepSnap.loadTransValuesSnap(thisInvRepSnap, 2));
            allInvSnap = addSnap(thisInvSnap, allInvSnap); // add to aggregate securities.

            SortedSet<TransValuesCum> parentSet =
                    currentInfo.transValuesCumMap.get(invAcct);
            RepSnap thisCashSnap = new RepSnap(currentInfo, parentSet, snapDateInt);
            allCashSnap =
                    addSnap(thisCashSnap, allCashSnap); //add to cash accounts
            RepSnap cashReport =
                    getSnapCashReturns(thisCashSnap, thisInvSnap); //get returns for cash account
            reportArrayList.add(RepSnap.loadTransValuesSnap(cashReport, 3));

            RepSnap thisAggRetSnap =
                    getSnapAggRetWCash(thisCashSnap, thisInvSnap); //get  aggregated returns with cash accounted for
            reportArrayList.add(RepSnap.loadTransValuesSnap(thisAggRetSnap, 4));
        }//end investment account loop

        //get returns for aggregated investment accounts
        allInvSnap = getSnapAggReturns(allInvSnap); //get aggregated returns from all securities
        reportArrayList.add(RepSnap.loadTransValuesSnap(allInvSnap, 5));

        RepSnap allCashReport = getSnapCashReturns(allCashSnap, allInvSnap); //get cash returns for all accounts
        reportArrayList.add(RepSnap.loadTransValuesSnap(allCashReport, 6));

        RepSnap allAggRetSnap = getSnapAggRetWCash(allCashSnap, allInvSnap); //get  agg returns w/ cash for all accounts
        reportArrayList.add(RepSnap.loadTransValuesSnap(allAggRetSnap, 7));

        Collections.sort(reportArrayList, PrntAcct_Order);
        return reportArrayList;

    }
    public static final Comparator<String[]> PrntAcct_Order =
            new Comparator<String[]>() {

                public int compare(String[] o1, String[] o2) {
                    int parentCmp = o1[0].compareTo(o2[0]);
                    return (parentCmp == 0 ? o1[1].compareTo(o2[1]) : parentCmp);
                }
            };

    /*
     * Next three methods are used to combine date maps for aggregate calculations  +

    /**
     * combines datemaps, either adding or subtracting cash flows
     * @param map1 input map
     * @param map2 input map
     * @param combType either "add" or "subtract"
     * @return output map
     */
    public static TreeMap<Integer, Double> combineDateMaps(TreeMap<Integer, Double> map1, TreeMap<Integer, Double> map2, String combType) {
        TreeMap<Integer, Double> outMap = new TreeMap<Integer, Double>(map1);

        if (map2 != null) {
            for (Iterator it = map2.keySet().iterator(); it.hasNext();) {
                Integer dateint2 = (Integer) it.next();
                Double value2 = map2.get(dateint2);

                if (outMap.get(dateint2) == null) {
                    if ("add".equals(combType)) {
                        outMap.put(dateint2,
                                value2);
                    }
                    if ("subtract".equals(combType)) {
                        outMap.put(dateint2,
                                -value2);
                    }
                } else {
                    if ("add".equals(combType)) {
                        outMap.put(dateint2,
                                map1.get(dateint2) + value2);
                    }
                    if ("subtract".equals(combType)) {
                        outMap.put(dateint2,
                                map1.get(dateint2) - value2);
                    }
                }
            }
        }
        return outMap;
    }

    /*
     * combines intermediate values for start value, end value, income, expense
     * for aggregate mod-dietz returns calculations.
     */
    public static LinkedHashMap<String, Double> addDoubleMap(
            LinkedHashMap<String, Double> map1,
            LinkedHashMap<String, Double> map2) {
        LinkedHashMap<String, Double> outMap =
                new LinkedHashMap<String, Double>(map1);
        if (map2 != null) {
            for (Iterator it = map2.keySet().iterator(); it.hasNext();) {
                String retCat2 = (String) it.next();
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

    /**
     * combines map of datemaps for Snap Reports, either adding or subtracting cash flows
     * @param map1 input map
     * @param map2 input map
     * @param retType either "add" or "subtract"
     * @return output map
     */
    public static LinkedHashMap<String, TreeMap<Integer, Double>> combineDateMapMap(LinkedHashMap<String, TreeMap<Integer, Double>> map1,
            LinkedHashMap<String, TreeMap<Integer, Double>> map2,
            String retType) {
        LinkedHashMap<String, TreeMap<Integer, Double>> outMap =
                new LinkedHashMap<String, TreeMap<Integer, Double>>(map1);
        if (map2 != null) {
            for (Iterator it = map2.keySet().iterator(); it.hasNext();) {
                String retCat2 = (String) it.next();
                TreeMap<Integer, Double> treeMap2 = map2.get(retCat2);
                if (map1.get(retCat2) == null) {
                    outMap.put(retCat2, treeMap2);
                } else {
                    TreeMap<Integer, Double> treeMap1 = map1.get(retCat2);
                    TreeMap<Integer, Double> tempMap =
                            new TreeMap<Integer, Double>(combineDateMaps(treeMap1, treeMap2, retType));
                    outMap.put(retCat2, tempMap);
                }
            }
        }
        return outMap;
    }

    private static LinkedHashMap<String, Integer> combineCatMap(LinkedHashMap<String, Integer> map1, LinkedHashMap<String, Integer> map2) {
        LinkedHashMap<String, Integer> outMap = new LinkedHashMap<String, Integer>(map1);
        for (Iterator it = map2.keySet().iterator(); it.hasNext();) {
            String cat2 = (String) it.next();
            Integer date2 = map2.get(cat2);
            if (map1.get(cat2) == null) {
                outMap.put(cat2, date2);
            } else {
                outMap.put(cat2, Math.min(map1.get(cat2), date2));
            }
        }
        return outMap;
    }

    public static double longToDouble(long inLong) {
        Long newInLong = new Long(inLong);
        return Double.valueOf(newInLong.toString());
    }

    /**
     * adds RepFromTo objects, in order to aggregate data at
     * account level or at aggregate level
     * @param thisSecFromTo RepFromTo associated with Security
     * @param thisInvFromTo RepFromTo associated with Investment Account
     * @return output RepFromTo
     */
    private static RepFromTo addFT(RepFromTo thisSecFromTo, RepFromTo thisInvFromTo) {
        RepFromTo outObj = thisInvFromTo;
        outObj.fromDateInt = thisInvFromTo.fromDateInt;
        outObj.toDateInt = thisInvFromTo.toDateInt;
        outObj.account = thisInvFromTo.account;
        outObj.startPos = 0.0;
        outObj.endPos = 0.0;
        outObj.startPrice = 0.0;
        outObj.endPrice = 0.0;
        outObj.startValue = thisInvFromTo.startValue + thisSecFromTo.startValue;
        outObj.endValue = thisInvFromTo.endValue + thisSecFromTo.endValue;
        outObj.buy = thisInvFromTo.buy + thisSecFromTo.buy;
        outObj.sell = thisInvFromTo.sell + thisSecFromTo.sell;
        outObj.shortSell = thisInvFromTo.shortSell + thisSecFromTo.shortSell;
        outObj.coverShort = thisInvFromTo.coverShort + thisSecFromTo.coverShort;
        outObj.income = thisInvFromTo.income + thisSecFromTo.income;
        outObj.expense = thisInvFromTo.expense + thisSecFromTo.expense;
        outObj.longBasis = thisInvFromTo.longBasis + thisSecFromTo.longBasis;
        outObj.shortBasis = thisInvFromTo.shortBasis + thisSecFromTo.shortBasis;
        outObj.realizedGain = thisInvFromTo.realizedGain + thisSecFromTo.realizedGain;
        outObj.unrealizedGain = thisInvFromTo.unrealizedGain + thisSecFromTo.unrealizedGain;
        outObj.periodReturn = thisInvFromTo.periodReturn + thisSecFromTo.periodReturn;
        outObj.startCash = thisInvFromTo.startCash + thisSecFromTo.startCash;
        outObj.endCash = thisInvFromTo.endCash + thisSecFromTo.endCash;

        if (thisSecFromTo.account == null) { //need to handle both cases of aggregation at investment account and across investment accounts
            outObj.startBalance = thisSecFromTo.startBalance;
        } else if (thisSecFromTo.account.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT) {
            outObj.startBalance = thisInvFromTo.startBalance + thisSecFromTo.startBalance;
        }

        outObj.arMap = combineDateMaps(thisInvFromTo.arMap, thisSecFromTo.arMap, "add");
        outObj.mdMap = combineDateMaps(thisInvFromTo.mdMap, thisSecFromTo.mdMap, "add");
        outObj.transMap = combineDateMaps(thisInvFromTo.transMap, thisSecFromTo.transMap, "add");
        outObj.percentReturn = 0.0;
        outObj.annualPercentReturn = 0.0;

        return outObj;
    }

    /**
     * adds RepSnap objects, in order to aggregate data at
     * account level or at aggregate level
     * @param thisSecFromTo RepSnap associated with Security
     * @param thisInvFromTo RepSnap associated with Investment Account
     * @return output RepSnap
     */
    private static RepSnap addSnap(RepSnap thisSecSnap, RepSnap thisInvSnap) {
        RepSnap outObj = thisInvSnap;

        outObj.lastPrice = 0.0;
        outObj.endPos = 0.0;
        outObj.endValue = thisSecSnap.endValue + thisInvSnap.endValue;
        outObj.endCash = thisSecSnap.endCash + thisInvSnap.endCash;
        if (thisSecSnap.account == null) { //need to handle both cases of aggregation at investment account and across investment accounts
            outObj.startBalance = thisSecSnap.startBalance;
        } else if (thisSecSnap.account.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT) {
            outObj.startBalance = thisInvSnap.startBalance + thisSecSnap.startBalance;
        }
        outObj.avgCostBasis = thisSecSnap.avgCostBasis + thisInvSnap.avgCostBasis;
        outObj.absPriceChange = 0.0;
        outObj.pctPriceChange = 0.0;
        outObj.absValueChange = thisSecSnap.absValueChange + thisInvSnap.absValueChange;
        outObj.income = thisSecSnap.income + thisInvSnap.income;
        outObj.totalGain = thisSecSnap.totalGain + thisInvSnap.totalGain;
        outObj.totRetAll = 0.0;
        outObj.annRetAll = 0.0;
        outObj.totRet1Day = 0.0;
        outObj.totRetWk = 0.0;
        outObj.totRet4Wk = 0.0;
        outObj.totRet3Mnth = 0.0;
        outObj.totRetYTD = 0.0;
        outObj.totRetYear = 0.0;
        outObj.totRet3year = 0.0;

        outObj.retDateMap = combineCatMap(thisSecSnap.retDateMap, thisInvSnap.retDateMap);
        outObj.startValues = addDoubleMap(thisInvSnap.startValues, thisSecSnap.startValues);
        outObj.incomes = addDoubleMap(thisInvSnap.incomes, thisSecSnap.incomes);
        outObj.expenses = addDoubleMap(thisInvSnap.expenses, thisSecSnap.expenses);
        outObj.mdReturns = thisInvSnap.mdReturns;
        outObj.startCashs = addDoubleMap(thisInvSnap.startCashs, thisSecSnap.startCashs);

        outObj.mdMap = combineDateMapMap(thisInvSnap.mdMap, thisSecSnap.mdMap, "add");
        outObj.arMap = combineDateMapMap(thisInvSnap.arMap, thisSecSnap.arMap, "add");
        outObj.transMap = combineDateMapMap(thisInvSnap.transMap, thisSecSnap.transMap, "add");

        return outObj;
    }

    /**
     * gets returns as Investment Account level for aggregated securities
     * @param thisInvFromTo aggregated securities RepFromTo
     * @return RepFromTo with correct return information
     */
    private static RepFromTo getFTAggReturns(RepFromTo thisInvFromTo) {
        RepFromTo outObj = thisInvFromTo;
        //get Mod-Dietz Returns
        double mdReturn = RepFromTo.getMDCalc(outObj.startValue,
                outObj.endValue, outObj.income, outObj.expense,
                outObj.mdMap);
        outObj.percentReturn = mdReturn;

        //add start and end values to return date maps
        if (thisInvFromTo.startValue != 0) {
            RepFromTo.addValueToDateMap(thisInvFromTo.arMap,
                    thisInvFromTo.fromDateInt, -thisInvFromTo.startValue);
        }
        if (thisInvFromTo.endValue != 0) {
            RepFromTo.addValueToDateMap(thisInvFromTo.arMap,
                    thisInvFromTo.toDateInt, thisInvFromTo.endValue);
        }

        //get annualized returns
        outObj.annualPercentReturn = RepFromTo.getAnnualReturn(thisInvFromTo.arMap, mdReturn);

        //remove start and end values from return date maps (to avoid conflicts in aggregation)
        if (thisInvFromTo.startValue != 0) {
            RepFromTo.addValueToDateMap(thisInvFromTo.arMap,
                    thisInvFromTo.fromDateInt, thisInvFromTo.startValue);
        }
        if (thisInvFromTo.endValue != 0) {
            RepFromTo.addValueToDateMap(thisInvFromTo.arMap,
                    thisInvFromTo.toDateInt, -thisInvFromTo.endValue);
        }

        return outObj;

    }

    /**
     * gets returns as Investment Account level for aggregated securities
     * @param thisInvFromTo aggregated securities RepSnap
     * @return RepSnap with correct return information
     */
    private static RepSnap getSnapAggReturns(RepSnap thisInvSnap) {
        RepSnap outObj = thisInvSnap;
//

        for (Iterator<String> it1 = thisInvSnap.retDateMap.keySet().iterator(); it1.hasNext();) {
            String retCat = (String) it1.next();
            // get MD returns on all start dates, only get annualized return for "All" dates

            outObj.mdReturns.put(retCat, RepFromTo.getMDCalc(thisInvSnap.startValues.get(retCat),
                    thisInvSnap.endValue, thisInvSnap.incomes.get(retCat), thisInvSnap.expenses.get(retCat),
                    thisInvSnap.mdMap.get(retCat)));

            if ("All".equals(retCat)) {
                //add start and end values to return date maps
                if (thisInvSnap.startValues.get(retCat) != 0.0) {
                    RepFromTo.addValueToDateMap(thisInvSnap.arMap.get(retCat),
                            thisInvSnap.retDateMap.get(retCat), -thisInvSnap.startValues.get(retCat));
                }
                if (thisInvSnap.endValue != 0.0) {
                    RepFromTo.addValueToDateMap(thisInvSnap.arMap.get(retCat),
                            thisInvSnap.snapDateInt, thisInvSnap.endValue);
                }
                //get return
                outObj.annRetAll = RepFromTo.getAnnualReturn(thisInvSnap.arMap.get(retCat), outObj.mdReturns.get("All"));
                outObj.income = thisInvSnap.incomes.get(retCat);

                //remove start and end values from return date maps (to avoid conflicts in aggregation)
                if (thisInvSnap.startValues.get(retCat) != 0.0) {
                    RepFromTo.addValueToDateMap(thisInvSnap.arMap.get(retCat),
                            thisInvSnap.retDateMap.get(retCat), thisInvSnap.startValues.get(retCat));
                }
                if (thisInvSnap.endValue != 0.0) {
                    RepFromTo.addValueToDateMap(thisInvSnap.arMap.get(retCat),
                            thisInvSnap.snapDateInt, -thisInvSnap.endValue);
                }

            }
        }
        outObj.totRet1Day = thisInvSnap.retDateMap.get("PREV") == null ? Double.NaN : outObj.mdReturns.get("PREV");
        outObj.totRetAll = thisInvSnap.retDateMap.get("All") == null ? Double.NaN : outObj.mdReturns.get("All");
        outObj.totRetWk = thisInvSnap.retDateMap.get("1Wk") == null ? Double.NaN : outObj.mdReturns.get("1Wk");
        outObj.totRet4Wk = thisInvSnap.retDateMap.get("4Wk") == null ? Double.NaN : outObj.mdReturns.get("4Wk");
        outObj.totRet3Mnth = thisInvSnap.retDateMap.get("3Mnth") == null ? Double.NaN : outObj.mdReturns.get("3Mnth");
        outObj.totRetYear = thisInvSnap.retDateMap.get("1Yr") == null ? Double.NaN : outObj.mdReturns.get("1Yr");
        outObj.totRet3year = thisInvSnap.retDateMap.get("3Yr") == null ? Double.NaN : outObj.mdReturns.get("3Yr");
        outObj.totRetYTD = thisInvSnap.retDateMap.get("YTD") == null ? Double.NaN : outObj.mdReturns.get("YTD");

        return outObj;

    }

    /**
     * gets RepFromTo for cash associated with an Investment Account
     * @param thisCashFromTo RepFromTo associated with Account "bank" transactions
     * @param thisInvFromTo RepFromTo associated with Securities
     * @return RepFrontTo representing income/returns for cash portion of Investment Account
     */
    private static RepFromTo getFTCashReturns(RepFromTo thisCashFromTo, RepFromTo thisInvFromTo) {
        RepFromTo cashValue = new RepFromTo(thisInvFromTo.account, thisInvFromTo.fromDateInt, thisInvFromTo.toDateInt);
        TreeMap<Integer, Double> comboTransMDMap = combineDateMaps(thisInvFromTo.transMap, thisCashFromTo.transMap, "add");

        double startBal = thisCashFromTo.startBalance;
        cashValue.startValue = cleanedValue(thisInvFromTo.startCash + thisCashFromTo.startCash + startBal);
        cashValue.endValue = cleanedValue(thisInvFromTo.endCash + thisCashFromTo.endCash + startBal);
        cashValue.startPos = cashValue.startValue;
        cashValue.endPos = cashValue.endValue;
        cashValue.income = thisCashFromTo.income;
        cashValue.expense = thisCashFromTo.expense;



        /*add transfer map to map of buys/sells/income/expense */
        comboTransMDMap = combineDateMaps(comboTransMDMap, thisInvFromTo.arMap, "add");
        /* cash RetMap adds in investment-account-level interest/expenses */
        TreeMap<Integer, Double> cashRetMap = combineDateMaps(thisCashFromTo.arMap, comboTransMDMap, "subtract");
        /* this handles case where fromDateInt < first transaction,
         since start value will not equal zero if there's an account starting balance*/
        int adjFromDateInt = cashValue.fromDateInt;
        int minDateInt = comboTransMDMap.isEmpty() ? 0 :
            DateUtils.getPrevBusinessDay(comboTransMDMap.firstKey());
        if(cashValue.startValue == startBal && cashValue.fromDateInt <= minDateInt)
            adjFromDateInt = Math.max(cashValue.fromDateInt, minDateInt );
        // add dummy values to Mod-Dietz date maps, start and end to return maps
        if(Math.abs(cashValue.startValue) > 0.0001){
            RepFromTo.addValueToDateMap(comboTransMDMap, adjFromDateInt, 0.0);
            RepFromTo.addValueToDateMap(cashRetMap, adjFromDateInt, -cashValue.startValue);
        }
        if(Math.abs(cashValue.endValue) > 0.0001){
            RepFromTo.addValueToDateMap(comboTransMDMap, thisInvFromTo.toDateInt, 0.0);
            RepFromTo.addValueToDateMap(cashRetMap, thisInvFromTo.toDateInt, cashValue.endValue);//add start and end values w/ cash balances for ret Calc
        }
        //calculate returns
        cashValue.percentReturn = RepFromTo.getMDCalc(cashValue.startValue,
                cashValue.endValue, cashValue.income, cashValue.expense, comboTransMDMap);
        cashValue.annualPercentReturn = RepFromTo.getAnnualReturn(cashRetMap, cashValue.percentReturn);
         return cashValue;
    }

    /**
     * gets RepSnap for cash associated with an Investment Account
     * @param thisCashSnap RepSnap associated with Account "bank" transactions
     * @param thisInvSnap RepSnapo associated with Securities
     * @return RepSnap representing income/returns for cash portion of Investment Account
     */
    private static RepSnap getSnapCashReturns(RepSnap thisCashSnap, RepSnap thisInvSnap) {
        RepSnap cashValue = new RepSnap(thisInvSnap.account, thisInvSnap.snapDateInt);

        LinkedHashMap<String, TreeMap<Integer, Double>> comboTransMDMap =
                combineDateMapMap(thisInvSnap.transMap, thisCashSnap.transMap, "add");
        
        LinkedHashMap<String, Integer> adjRetDateMap = 
                new LinkedHashMap<String, Integer>(thisInvSnap.retDateMap);


        double startBal = thisCashSnap.startBalance;

        for (Iterator it = thisInvSnap.retDateMap.keySet().iterator(); it.hasNext();) {
            String retCat = (String) it.next();
            cashValue.startValues.put(retCat, cleanedValue(thisInvSnap.startCashs.get(retCat)
                    + thisCashSnap.startCashs.get(retCat) + startBal));
            cashValue.incomes.put(retCat, thisCashSnap.incomes.get(retCat));
            cashValue.expenses.put(retCat, thisCashSnap.expenses.get(retCat));
            /* this handles case where fromDateInt < first transaction,
            since start value will not equal zero if there's an account starting balance*/
            int minDateInt = comboTransMDMap.get(retCat).isEmpty()? 0 :
                DateUtils.getPrevBusinessDay(
                comboTransMDMap.get(retCat).firstKey());
            if(cashValue.startValues.get(retCat) == startBal &&
                    thisInvSnap.retDateMap.get(retCat) <= minDateInt)
                adjRetDateMap.put(retCat, Math.max(
                        thisInvSnap.retDateMap.get(retCat), minDateInt));
        }

        cashValue.endValue = cleanedValue(thisInvSnap.endCash + thisCashSnap.endCash + startBal);
        cashValue.endPos = cashValue.endValue;
        cashValue.income = thisCashSnap.income; //note, we do not display expenses in this object
        //but they are tracked for returns calculations

        LinkedHashMap<String, TreeMap<Integer, Double>> adjAnnRetValues =
                new LinkedHashMap<String, TreeMap<Integer, Double>>(thisInvSnap.arMap);
        for (Iterator it = thisInvSnap.retDateMap.keySet().iterator(); it.hasNext();) {
            String retCat = (String) it.next();
            Integer thisFromDateInt = adjRetDateMap.get(retCat);
            // add dummy values to date maps (if start and end values exist)
            if (Math.abs(cashValue.startValues.get(retCat)) > 0.0001) RepFromTo.addValueToDateMap(
                    adjAnnRetValues.get(retCat), thisFromDateInt, 0.0);
            if (Math.abs(cashValue.endValue) > 0.0001) RepFromTo.addValueToDateMap(
                    adjAnnRetValues.get(retCat), cashValue.snapDateInt, 0.0);
        }

        /*add transfer map to map of buys/sells/income/expense */
        comboTransMDMap = combineDateMapMap(comboTransMDMap, adjAnnRetValues, "add");
        /*calculate period returns and annual return for all dates */
        for (Iterator it = thisInvSnap.retDateMap.keySet().iterator(); it.hasNext();) {
            String retCat = (String) it.next();
            double thisPercentReturn = RepFromTo.getMDCalc(cashValue.startValues.get(retCat),
                    cashValue.endValue, cashValue.incomes.get(retCat),
                    cashValue.expenses.get(retCat), comboTransMDMap.get(retCat));
            cashValue.mdReturns.put(retCat, thisPercentReturn);
//            if(retCat.equals("All") && thisInvSnap.account
//                    .getAccountName().equals("USAATaxExemptFund")){
//                testWriteTotRet("USAA-Cash-All", cashValue.startValues.get(retCat),
//                        cashValue.endValue, 0.0, 0.0, comboTransMDMap.get(retCat));
//
//            }

            if ("All".equals(retCat)) {
                /* cash RetMap adds in investment-account-level interest/expenses */
                TreeMap<Integer, Double> cashRetMap = combineDateMaps(
                        thisCashSnap.arMap.get(retCat),
                        comboTransMDMap.get(retCat), "subtract");
                // add start and end values to date map
                Integer thisFromDateInt = adjRetDateMap.get(retCat);
                if(Math.abs(cashValue.startValues.get(retCat)) > 0.0001)
                    RepFromTo.addValueToDateMap(cashRetMap,
                            thisFromDateInt, -cashValue.startValues.get(retCat));
                if(Math.abs(cashValue.endValue) > 0.0001)
                    RepFromTo.addValueToDateMap(cashRetMap,
                        thisInvSnap.snapDateInt, cashValue.endValue);
                cashValue.annRetAll = RepFromTo.getAnnualReturn(cashRetMap,
                        cashValue.mdReturns.get(retCat));
                cashValue.income = thisCashSnap.incomes.get(retCat);
            }
        }
        cashValue.totRet1Day = thisInvSnap.retDateMap.get("PREV") == null ? Double.NaN : cashValue.mdReturns.get("PREV");
        cashValue.totRetAll = thisInvSnap.retDateMap.get("All") == null ? Double.NaN : cashValue.mdReturns.get("All");
        cashValue.totRetWk = thisInvSnap.retDateMap.get("1Wk") == null ? Double.NaN : cashValue.mdReturns.get("1Wk");
        cashValue.totRet4Wk = thisInvSnap.retDateMap.get("4Wk") == null ? Double.NaN : cashValue.mdReturns.get("4Wk");
        cashValue.totRet3Mnth = thisInvSnap.retDateMap.get("3Mnth") == null ? Double.NaN : cashValue.mdReturns.get("3Mnth");
        cashValue.totRetYear = thisInvSnap.retDateMap.get("1Yr") == null ? Double.NaN : cashValue.mdReturns.get("1Yr");
        cashValue.totRet3year = thisInvSnap.retDateMap.get("3Yr") == null ? Double.NaN : cashValue.mdReturns.get("3Yr");
        cashValue.totRetYTD = thisInvSnap.retDateMap.get("YTD") == null ? Double.NaN : cashValue.mdReturns.get("YTD");

        return cashValue;
    }

    /**
     * gets RepFromTo for Investment Account with Associated Cash accounted for as a Security.
     * @param thisCashFromTo RepFromTo associated with Account "bank" transactions
     * @param thisInvFromTo RepFromTo associated with Securities
     * @return RepFrontTo representing income/returns for Investment Account,
     * Cash and Securities included
     */
    private static RepFromTo getFTAggRetWCash(RepFromTo thisCashFromTo, RepFromTo thisInvFromTo) {
        RepFromTo outObj = new RepFromTo(thisInvFromTo.account, thisInvFromTo.fromDateInt, thisInvFromTo.toDateInt);

        //copy over aggregate values from aggregated securities
        outObj.buy = thisInvFromTo.buy;
        outObj.sell = thisInvFromTo.sell;
        outObj.shortSell = thisInvFromTo.shortSell;
        outObj.coverShort = thisInvFromTo.coverShort;
        outObj.longBasis = thisInvFromTo.longBasis;
        outObj.shortBasis = thisInvFromTo.shortBasis;
        outObj.realizedGain = thisInvFromTo.realizedGain;
        outObj.unrealizedGain = thisInvFromTo.unrealizedGain;
        outObj.periodReturn = thisInvFromTo.periodReturn;
        //add balance sheet and income statement values where applicable
        outObj.startValue = thisInvFromTo.startValue + thisCashFromTo.startValue;
        outObj.endValue = thisInvFromTo.endValue + thisCashFromTo.endValue;
        outObj.income = thisInvFromTo.income + thisCashFromTo.income;
        outObj.expense = thisInvFromTo.expense + thisCashFromTo.expense;
        outObj.startCash = thisInvFromTo.startCash + thisCashFromTo.startCash;
        outObj.endCash = thisInvFromTo.endCash + thisCashFromTo.endCash;

        //combine transfer date map
        outObj.transMap = combineDateMaps(thisInvFromTo.transMap, thisCashFromTo.transMap, "add");

        //get correct start and end balances w/ cash accounted for
        double startBal = thisCashFromTo.startBalance;
        outObj.startValue = cleanedValue(outObj.startValue + outObj.startCash + startBal);
        outObj.endValue = cleanedValue(outObj.endValue + outObj.endCash + startBal);

        //from account returns perspective, only transfers matter, so they
        //become the "buys" and "sells" for MD returns calculations and annual returns calcs

        //get MD returns
        TreeMap<Integer, Double> mdMap = outObj.transMap;
        /* reverse transfer map for returns calc purposes */
        TreeMap<Integer, Double> retMap = combineDateMaps(new TreeMap<Integer, Double>(),
                outObj.transMap, "subtract");
        /* this handles case where fromDateInt < first transaction,
        since start value will not equal zero if there's an account starting balance*/
        int adjFromDateInt = outObj.fromDateInt;
        int minDateInt = mdMap.isEmpty() ? 0 :
                    DateUtils.getPrevBusinessDay(mdMap.firstKey());
        if(outObj.startValue == startBal && outObj.fromDateInt <= minDateInt)
            adjFromDateInt = Math.max(outObj.fromDateInt, minDateInt );
        // add dummy values to Mod-Dietz date maps, start and end to return maps
        if(Math.abs(outObj.startValue) > 0.0001){
            RepFromTo.addValueToDateMap(mdMap, adjFromDateInt, 0.0);
            RepFromTo.addValueToDateMap(retMap, adjFromDateInt, -outObj.startValue);
        }
        if(Math.abs(outObj.endValue) > 0.0001){
            RepFromTo.addValueToDateMap(mdMap, outObj.toDateInt, 0.0);
            RepFromTo.addValueToDateMap(retMap, thisInvFromTo.toDateInt, outObj.endValue);
        }
        /*calc returns (note--no income/expenses since only transfers are considered
        i.e. endValue includes income/expenses*/
        double allMDReturn = RepFromTo.getMDCalc(outObj.startValue, outObj.endValue, 0.0, 0.0,
                mdMap);
        outObj.percentReturn = allMDReturn;
        //get annualized returns
        outObj.annualPercentReturn = RepFromTo.getAnnualReturn(retMap, allMDReturn);

        return outObj;
    }

    /**
     * gets RepSnap for Investment Account with Associated Cash accounted for as a Security.
     * @param thisCashSnap RepSnap associated with Account "bank" transactions
     * @param thisInvSnap RepSnap associated with Securities
     * @return RepFrontTo representing income/returns for Investment Account,
     * Cash and Securities included
     */
    private static RepSnap getSnapAggRetWCash(RepSnap thisCashSnap, RepSnap thisInvSnap) {
        RepSnap outObj = new RepSnap(thisInvSnap.account, thisInvSnap.snapDateInt);

        LinkedHashMap<String, Integer> adjRetDateMap =
                new LinkedHashMap<String, Integer>(thisInvSnap.retDateMap);

        double startBal = thisCashSnap.startBalance;

        //copy over aggregate values from aggregated securities
        outObj.totalGain = thisInvSnap.totalGain;
        outObj.income = thisInvSnap.income + thisCashSnap.income;
        outObj.absValueChange = thisInvSnap.absValueChange;
        //ending balance sheet values

        //combine transfer date maps
        outObj.transMap = combineDateMapMap(thisInvSnap.transMap, thisCashSnap.transMap, "add");

        //get correct start and end balances w/ cash accounted for
        for (Iterator it = thisInvSnap.retDateMap.keySet().iterator(); it.hasNext();) {
            String retCat = (String) it.next();
            outObj.startCashs.put(retCat, thisInvSnap.startCashs.get(retCat)
                    + thisCashSnap.startCashs.get(retCat));
            outObj.startValues.put(retCat, cleanedValue(outObj.startCashs.get(retCat)
                    + thisInvSnap.startValues.get(retCat) + 
                    thisCashSnap.startValues.get(retCat) + startBal));
            /* this handles case where fromDateInt < first transaction,
            since start value will not equal zero if there's an account starting balance*/
            int minDateInt = outObj.transMap.get(retCat).isEmpty() ? 0 :
                        DateUtils.getPrevBusinessDay(
                        outObj.transMap.get(retCat).firstKey());
            if(outObj.startValues.get(retCat) == startBal &&
                    thisInvSnap.retDateMap.get(retCat) <= minDateInt)
                adjRetDateMap.put(retCat, Math.max(
                        thisInvSnap.retDateMap.get(retCat), minDateInt));
        }
        outObj.endValue = cleanedValue(thisInvSnap.endValue + thisCashSnap.endValue
                + thisInvSnap.endCash + thisCashSnap.endCash + startBal);
        outObj.endCash = thisInvSnap.endCash + thisCashSnap.endCash + startBal;
        //get returns
        for (Iterator it = thisInvSnap.retDateMap.keySet().iterator(); it.hasNext();) {
            String retCat = (String) it.next();
            Integer thisFromDateInt = adjRetDateMap.get(retCat);
            TreeMap<Integer, Double> mdMap = outObj.transMap.get(retCat);
            // add dummy values to Mod-Dietz date maps
            if(Math.abs(outObj.startValues.get(retCat)) > 0.0001)
                RepFromTo.addValueToDateMap(mdMap, thisFromDateInt, 0.0);
            if(Math.abs(outObj.endValue) > 0.0001)
                RepFromTo.addValueToDateMap(mdMap, outObj.snapDateInt, 0.0);
            /*calc returns (note--no income/expenses since only transfers are considered
            i.e. endValue includes income/expenses*/
            double thisMDReturn = RepFromTo.getMDCalc(
                    outObj.startValues.get(retCat), outObj.endValue, 0.0, 0.0, mdMap);
            outObj.mdReturns.put(retCat, thisMDReturn);
            // get annualized returns
            if ("All".equals(retCat)) {
                TreeMap<Integer, Double> retMap = combineDateMaps(new TreeMap<Integer, Double>(),
                        outObj.transMap.get(retCat), "subtract");
                //add start and end values
                if(Math.abs(outObj.startValues.get(retCat)) > 0.0001)
                    RepFromTo.addValueToDateMap(retMap, thisFromDateInt,
                            -outObj.startValues.get(retCat));
                if(Math.abs(outObj.endValue) > 0.0001)
                    RepFromTo.addValueToDateMap(retMap,
                            thisInvSnap.snapDateInt, outObj.endValue);
                //calculate returns
                outObj.annRetAll = RepFromTo.getAnnualReturn(retMap,
                        outObj.mdReturns.get(retCat));
            }
        }
        outObj.totRet1Day = thisInvSnap.retDateMap.get("PREV") == null ? Double.NaN : outObj.mdReturns.get("PREV");
        outObj.totRetAll = thisInvSnap.retDateMap.get("All") == null ? Double.NaN : outObj.mdReturns.get("All");
        outObj.totRetWk = thisInvSnap.retDateMap.get("1Wk") == null ? Double.NaN : outObj.mdReturns.get("1Wk");
        outObj.totRet4Wk = thisInvSnap.retDateMap.get("4Wk") == null ? Double.NaN : outObj.mdReturns.get("4Wk");
        outObj.totRet3Mnth = thisInvSnap.retDateMap.get("3Mnth") == null ? Double.NaN : outObj.mdReturns.get("3Mnth");
        outObj.totRetYear = thisInvSnap.retDateMap.get("1Yr") == null ? Double.NaN : outObj.mdReturns.get("1Yr");
        outObj.totRet3year = thisInvSnap.retDateMap.get("3Yr") == null ? Double.NaN : outObj.mdReturns.get("3Yr");
        outObj.totRetYTD = thisInvSnap.retDateMap.get("YTD") == null ? Double.NaN : outObj.mdReturns.get("YTD");

        return outObj;
    }


    /**
     * Rounds values near zero to exactly zero
     * @param input input value
     * @return input or zero
     */
    public static double cleanedValue(double input){
        double thresh = 0.0001;
        if((input > 0 && input < thresh) || (input < 0 && input > -thresh) ){
            return 0;
        } else {
            return input;
        }
    }

    //test method to output return primitives for use during debugging
     private static void testWriteTotRet(String accountName, Double startValue,
    double endValue, Double income, Double expense, TreeMap<Integer, Double> mdMap) {
    File writeFile = new File("E:\\Temp\\" + accountName + ".csv");
    PrintWriter outputStream = null;
    try {
    outputStream = new PrintWriter(new FileWriter(writeFile));
    outputStream.println("dump of return values");
    outputStream.println("startValue," + startValue);
    outputStream.println("endValue," + endValue);
    outputStream.println("income," + income);
    outputStream.println("expense," + expense);
    for (Iterator it = mdMap.keySet().iterator(); it.hasNext();) {
    Integer dateInt = (Integer) it.next();
    Double value = mdMap.get(dateInt);
    outputStream.println(dateInt + "," + value);
    }

    } catch (Exception e) {
    Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, e);
    } finally {
    outputStream.close();
    }
    }


}

 

    //unused methods follow
   // <editor-fold defaultstate="collapsed" desc="comment">
/*public static Double testSum(TreeMap<Integer, Double> inMap) {
    Double sum = 0.0;

    for (Iterator it = inMap.values().iterator(); it.hasNext();) {
    Double thisVal = (Double) it.next();
    sum = sum + thisVal;
    }
    return sum;
    }

   

    private static void testWriteCash(String accountName, double startBal,
    Double invStartCash, Double cashStartCash, Double invStartValue,
    Double cashStartValue) {
    File writeFile = new File("E:\\Temp\\" + accountName + ".csv");
    PrintWriter outputStream = null;
    try {
    outputStream = new PrintWriter(new FileWriter(writeFile));
    outputStream.println("End File,");
    outputStream.println("startBal," + startBal);
    outputStream.println("invEndCash," + invStartCash);
    outputStream.println("cashEndCash," + cashStartCash);
    outputStream.println("invEndValue," + invStartValue);
    outputStream.println("cashEndValue," + cashStartValue);


    } catch (Exception e) {
    Logger.getLogger(IOMethods.class.getName()).log(Level.SEVERE, null, e);
    } finally {
    outputStream.close();
    }
    }*/// </editor-fold>



