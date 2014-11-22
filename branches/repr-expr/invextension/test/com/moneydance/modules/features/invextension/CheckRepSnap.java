/*
 * CheckRepSnap.java
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
import java.util.TreeMap;

/**
 * Generates dump of  intermediate values in
 * "Snap" Report to allow for auditing
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class CheckRepSnap {
    public static final boolean rptOutputSingle = false;
    public static final int numFrozenColumns = 5; //Irrelevant for testing purposes
    // private static final int getFromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    private static final DateRange dateRange = new DateRange(toDateInt, toDateInt, toDateInt);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {

        AggregationController aggregationController = AggregationController.INVACCT;
        boolean closedPosHidden = true;

        ReportConfig reportConfig = new ReportConfig(TotalSnapshotReport.class, "Test Report",
                true, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalSnapshotReport.MODEL_HEADER),
                ReportConfig.getDefaultExcludedAccounts(), dateRange);
        TotalSnapshotReport snapshotReport = new TotalSnapshotReport(reportConfig);
        snapshotReport.calcReport(BulkSecInfoTest.getBaseSecurityInfoAvgCost());
        ArrayList<ComponentReport> componentReports = snapshotReport
                .getReports();
        for (ComponentReport componentReport : componentReports) {
            printSnap(componentReport);
        }

    }

    public static void printSnap(ComponentReport componentReport)
            throws SecurityException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException {
        SecuritySnapshotReport snapLine;
        CompositeReport compositeReport = null;

        if (componentReport instanceof SecuritySnapshotReport) {
            snapLine = (SecuritySnapshotReport) componentReport;
        } else {
            compositeReport = (CompositeReport) componentReport;
            snapLine = (SecuritySnapshotReport) compositeReport.getAggregateReport();
        }

        String tab = "\u0009";
        System.out.println("\n" + "Report: Snap" + "\n");
        String acctName = compositeReport != null ? compositeReport.getName()
                : snapLine.getName();
        String acctTicker = compositeReport != null ? "NoTicker"
                : snapLine.getCurrencyWrapper().ticker;

        System.out.println("Account: " + tab + acctName + tab + "Ticker:" + tab
                + acctTicker);
        System.out.println("Snapshot Date: " + tab + snapLine.getSnapDateInt());
        printRetDateMap(snapLine.getReturnsStartDate(), "Return Dates");
        printInputMap(snapLine.getStartPoses(), "Start Positions");
        printInputMap(snapLine.getStartPrices(), "Start Prices");
        printInputMap(snapLine.getStartValues(), "Start Values");

        System.out.println("EndPos: " + tab + snapLine.getEndPos() + tab
                + "EndPrice: " + tab + snapLine.getLastPrice() + tab + "EndValue:"
                + tab + snapLine.getEndValue());

        printInputMap(snapLine.getIncomes(), "Income Amounts");
        printInputMap(snapLine.getExpenses(), "Expense Amounts");

        System.out.println("All Maps Follow: \n");
        printAllPerfMaps(snapLine.getMdMap(), snapLine.getArMap(), snapLine.getTransMap());

        System.out.println("Returns: \n");
        System.out.println("1-Day Ret: " + tab + snapLine.getTotRet1Day() + tab
                + "1-Wk Ret: " + tab + snapLine.getTotRetWk() + tab + "4-Wk Ret: "
                + tab + snapLine.getTotRet4Wk() + tab + "3-Mnth Ret: " + tab
                + snapLine.getTotRet3Mnth() + tab + "1-Yr Ret: " + tab
                + snapLine.getTotRetYear() + tab + "3-Yr Ret: " + tab
                + snapLine.getTotRet3year() + tab + "YTD Ret: " + tab
                + snapLine.getTotRetYTD() + tab + "All Ret: " + tab
                + snapLine.getTotRetAll() + tab + "All AnnRet: " + tab
                + snapLine.getAnnRetAll() + tab);
    }

    public static void printRetDateMap(CategoryMap<Integer> categoryMap,
                                       String msg) {
        StringBuilder outStr = new StringBuilder();
        String tab = "\u0009";
        outStr.append(msg).append("\n");
        String[] retCats = {"PREV", "1Wk", "4Wk", "3Mnth", "1Yr", "3Yr",
                "YTD", "All"};
        for (String retCat : retCats) {
            Integer value = categoryMap.get(retCat) == null ? 0 : categoryMap
                    .get(retCat);
            String dateStr = value == 0 ? "N/A" : DateUtils
                    .convertToShort(value);
            outStr.append(retCat).append(tab).append(dateStr).append(tab);
        }
        System.out.println(outStr.toString());
    }

    public static void printInputMap(CategoryMap<Long> categoryMap, String msg) {
        StringBuilder outStr = new StringBuilder();
        String tab = "\u0009";
        String[] retCats = {"PREV", "1Wk", "4Wk", "3Mnth", "1Yr", "3Yr",
                "YTD", "All"};
        outStr.append(msg).append("\n");
        for (String retCat : retCats) {
            Double value = categoryMap.get(retCat) == null ? Double.NaN
                    : categoryMap.get(retCat);
            outStr.append(retCat).append(tab).append(value.toString())
                    .append(tab);
        }
        System.out.println(outStr.toString());
    }

    public static void printAllPerfMaps(CategoryMap<DateMap> categoryMap,
                                        CategoryMap<DateMap> categoryMap2, CategoryMap<DateMap> categoryMap3) {
        String[] retCats = {"PREV", "1Wk", "4Wk", "3Mnth", "1Yr", "3Yr",
                "YTD", "All"};
        for (String retCat : retCats) {
            DateMap mdMap = (DateMap) (categoryMap.get(retCat) == null ? new TreeMap<Integer, Double>()
                    : categoryMap.get(retCat));
            DateMap arMap = (DateMap) (categoryMap2.get(retCat) == null ? new TreeMap<Integer, Double>()
                    : categoryMap2.get(retCat));
            DateMap transMap = (DateMap) (categoryMap3.get(retCat) == null ? new TreeMap<Integer, Double>()
                    : categoryMap3.get(retCat));
            CheckRepFromTo.printPerfMaps(arMap, mdMap, transMap, "\n"
                    + "Maps: " + retCat);
        }

    }

}
