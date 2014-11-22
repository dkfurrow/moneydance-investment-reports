/*
 * CheckRepFromTo.java
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

import java.util.ArrayList;
import java.util.Map;

/**
 * Generates dump of  intermediate values in
 * "From/To" Report to allow for auditing
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class CheckRepFromTo {
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final DateRange dateRange = new DateRange(fromDateInt, toDateInt, toDateInt);


    public static void main(String[] args) throws Exception {
        BulkSecInfo currentInfo = BulkSecInfoTest.getBaseSecurityInfoAvgCost();

        int numFrozenColumns = 5; //irrelevant for purposes of test
        boolean closedPosHidden = true;//irrelevant for purposes of test
        AggregationController aggregationController = AggregationController.INVACCT;
        boolean rptOutputSingle = false;
        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class, "Test Report",
                true, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalFromToReport.MODEL_HEADER),
                ReportConfig.getDefaultExcludedAccounts(), dateRange);
        TotalFromToReport fromToReport
                = new TotalFromToReport(reportConfig);
        fromToReport.calcReport(currentInfo);
        ArrayList<ComponentReport> componentReports = fromToReport.getReports();
        for (ComponentReport componentReport : componentReports) {
            printFromTo(componentReport);
        }

    }


    public static void printFromTo(ComponentReport componentReport) throws Exception {
        SecurityFromToReport reportLine;
        CompositeReport compositeReport = null;

        if (componentReport instanceof SecurityFromToReport) {
            reportLine = (SecurityFromToReport) componentReport;
        } else {
            compositeReport = (CompositeReport) componentReport;
            reportLine = (SecurityFromToReport) compositeReport.getAggregateReport();
        }


        String tab = "\u0009";
        System.out.println("Report: From/To" + "\n");


        String acctName = compositeReport != null ? compositeReport.getName() : reportLine.getName();
        String acctTicker = compositeReport != null ? "NoTicker" : reportLine.getCurrencyWrapper().ticker;

        System.out.println("Account: " + tab + acctName + tab + "Ticker:" + tab
                + acctTicker);
        System.out.println("From: " + tab + reportLine.getFromDateInt() + tab
                + "To: " + tab + reportLine.getToDateInt());
        System.out.println("StartPos: " + tab + reportLine.getStartPos() + tab
                + "StartPrice: " + tab + reportLine.getStartPrice() + tab
                + "StartValue:" + tab + reportLine.getStartValue());
        System.out.println("EndPos: " + tab + reportLine.getEndPos() + tab
                + "EndPrice: " + tab + reportLine.getEndPrice() + tab + "EndValue:"
                + tab + reportLine.getEndValue());
        System.out.println("Buy: " + tab + reportLine.getBuy() + tab + "Sell: " + tab
                + reportLine.getSell() + tab + "Short:" + tab + reportLine.getShortSell()
                + tab + "CoverShort:" + tab + reportLine.getCoverShort());
        System.out.println("Income: " + tab + reportLine.income + tab
                + "Expense: " + tab + reportLine.getExpense());
        System.out.println("LongBasis: " + tab + reportLine.getLongBasis() + tab
                + "ShortBasis: " + tab + reportLine.getShortBasis());
        System.out.println("RealizedGain: " + tab + reportLine.getRealizedGain()
                + tab + "UnrealizedGain: " + tab + reportLine.getUnrealizedGain()
                + tab + "TotalGain:" + tab + reportLine.getTotalGain());
        printPerfMaps(reportLine.getArMap(), reportLine.getMdMap(), reportLine.getTransMap(),
                "Maps");
        System.out.println("\n" + "mdReturn: " + tab + reportLine.getMdReturn() + tab
                + "AnnualReturn: " + tab + reportLine.getAnnualPercentReturn());
        System.out.println("\n");

    }

    public static void printPerfMaps(DateMap arMap,
                                     DateMap mdMap, DateMap transMap, String msg) {
        String tab = "\u0009";
        String space = "";
        int maxSize = Math.max(arMap == null ? 0 : arMap.getMap().size(), Math.max(
                mdMap == null ? 0 : mdMap.getMap().size(), transMap == null ? 0
                        : transMap.getMap().size()));
        System.out.println(msg + "\n");
        System.out.println("arMap" + tab + tab + "mdMap" + tab + tab
                + "transMap");
        System.out.println("Date" + tab + "Value" + tab + "Date" + tab
                + "Value" + tab + "Date" + tab + "Value" + tab);

        Integer[] arMapDates = arMap == null ? new Integer[0]
                : returnKeySetArray(arMap.getMap());
        Integer[] mdMapDates = mdMap == null ? new Integer[0]
                : returnKeySetArray(mdMap.getMap());
        Integer[] transMapDates = transMap == null ? new Integer[0]
                : returnKeySetArray(transMap.getMap());

        for (int i = 0; i < maxSize; i++) {
            StringBuilder outLine = new StringBuilder();

            if (i < arMapDates.length) {
                outLine.append(
                        returnMapEle(arMapDates[i], arMap.get(arMapDates[i])))
                        .append(tab);
            } else {
                outLine.append(space).append(tab).append(space).append(tab);
            }

            if (i < mdMapDates.length) {
                outLine.append(
                        returnMapEle(mdMapDates[i], mdMap.get(mdMapDates[i])))
                        .append(tab);
            } else {
                outLine.append(space).append(tab).append(space).append(tab);
            }

            if (i < transMapDates.length) {
                outLine.append(returnMapEle(transMapDates[i],
                        transMap.get(transMapDates[i])));
            } else {
                outLine.append(space).append(tab).append(space).append(tab);
            }

            System.out.println(outLine);
        }
    }

    public static Integer[] returnKeySetArray(Map<Integer, Long> map) {
        Integer[] outArray = new Integer[map.size()];
        int i = 0;
        for (Map.Entry<Integer, Long> mapEntry : map.entrySet()) {
            outArray[i] = mapEntry.getKey();
            i++;
        }
        return outArray;
    }

    public static String returnMapEle(int dateInt, double val) {
        String tab = "\u0009";

        return DateUtils.convertToShort(dateInt) + tab + Double.toString(val);
    }

}
