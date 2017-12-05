/*
 * ReportProdTest.java
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

import com.moneydance.modules.features.invextension.SecurityReport.MetricEntry;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import com.moneydance.modules.features.invextension.ReportProdTest.ReportLine;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertFalse;

/**
 * Generates 3 tests:
 * (1) Compares generated "From/To" report from stored MD file to base version
 * saved in CSV Form.
 * (2) Compares generated "Snapshot" report from stored MD file to base version
 * saved in CSV Form.
 * (3) Compares generated "SnapShot" report to iterated "From/To" report, to
 * ensure consistency between generated data between the two reportsPanel
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class ConsistencyTest {
    public static final boolean rptOutputSingle = false;
    // Stored CSV Files
    public static final File ftBaseFile = new File("./resources/ft20100601.csv");
    public static final File snapBaseFile = new File("./resources/snap20100601.csv");
    public static final int numFrozenColumns = 5; //Irrelevant for testing purposes
    public static final boolean closedPosHidden = true; //Irrelevant for testing purposes
    // Report Dates for comparison
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final DateRange dateRange = new DateRange(fromDateInt, toDateInt, toDateInt);
    private static BulkSecInfo currentInfo;



    @Before
    public void setUp() throws Exception {
        currentInfo = BulkSecInfoTest.getBaseSecurityInfoAvgCost();
    }

    /**
     * Generates Return Date Map for 9 standardized return dates
     * (same as code in snap report)
     *
     * @param currentInfo input BulkSecInfo
     * @return date map
     */
    private static LinkedHashMap<String, Integer> getRetDateMap(BulkSecInfo currentInfo) {
        LinkedHashMap<String, Integer> retDateMap = new LinkedHashMap<>();
        int firstDateInt = DateUtils.getPrevBusinessDay(currentInfo.getFirstDateInt());
        int fromDateInt = DateUtils.getPrevBusinessDay(firstDateInt);
        int prevFromDateInt = DateUtils.getPrevBusinessDay(toDateInt);
        int wkFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addDaysInt(toDateInt, -7));
        int mnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(toDateInt, -1));
        int threeMnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(toDateInt, -3));
        int oneYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(toDateInt, -12));
        int threeYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(toDateInt, -36));
        int ytdFromDateInt = DateUtils.getStartYear(toDateInt);

        retDateMap.put("PREV", prevFromDateInt);
        retDateMap.put("1Wk", wkFromDateInt);
        retDateMap.put("4Wk", mnthFromDateInt);
        retDateMap.put("3Mnth", threeMnthFromDateInt);
        retDateMap.put("1Yr", oneYearFromDateInt);
        retDateMap.put("3Yr", threeYearFromDateInt);
        retDateMap.put("YTD", ytdFromDateInt);
        retDateMap.put("All", fromDateInt);

        return retDateMap;
    }

    /**
     * Test column in snap report against column in "From/To" Report
     * to check for consistency
     *
     * @param snapValues ArrayList of ReportLine associated with snapshot report
     * @param ftValues   ArrayList of ReportLine associated with "From-To" report
     * @param snapCol  column from snapshot report
     * @param ftCol    column from "From-To" Report
     * @return pass/fail of test
     */
    private static boolean testRepSnapCol(ArrayList<ReportLine> snapValues,
                                          ArrayList<ReportLine> ftValues, int snapCol, int ftCol) {
        boolean errorFound = false;
        for (int i = 0; i < snapValues.size(); i++) {
            String snapValue = snapValues.get(i).getRow()[snapCol];
            String ftValue = ftValues.get(i).getRow()[ftCol];
            if (!BulkSecInfoTest.similarElements(snapValue, ftValue,
                    BulkSecInfoTest.numDigitsToCompare,
                    BulkSecInfoTest.limitComparisonToMinDigits)) {
                System.out.println("SnapReport: Row " + i
                        + " Column " + snapCol + "(FT: " + ftCol + ")"
                        + " ERROR! (" + snapValues.get(i).getRow()[0] + ": " + snapValues.get(i).getRow()[1] + ")"
                        + " Snap = " + snapValue
                        + " Should be FT = " + ftValue);
                errorFound = true;

            } else {
                System.out.println("SnapReport: Row " + i + " Column " + snapCol + " Passed");
            }
        }
        return errorFound;
    }



    /**
     * Tests "Snap" Report generated from MD File against iterations of
     * "From/To" Reports, to ensure that reportsPanel are consistent     *
     *
     * @throws Exception
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testRepSnapAgainstFT() throws Exception {
        boolean errorFound = false;
        LinkedHashMap<String, Integer> retDateMap = getRetDateMap(currentInfo);

        AggregationController aggregationController = AggregationController.INVACCT;
        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class, "Test Report",
                true, false, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalSnapshotReport.MODEL_HEADER),
                ReportConfig.getDefaultExcludedAccounts(), ReportConfig.getDefaultInvestmentExpenseAccounts(),
                ReportConfig.getDefaultInvestmentIncomeAccounts(), dateRange);
        reportConfig.setAllExpenseAccountsToInvestment(currentInfo.getRoot());
        reportConfig.setAllIncomeAccountsToInvestment(currentInfo.getRoot());
        TotalSnapshotReport snapshotReport = new TotalSnapshotReport(reportConfig, currentInfo);
        snapshotReport.calcReport();
        Object[][] snapValues = snapshotReport.getReportTable();
        ArrayList<ReportLine> snapTest = ReportProdTest.readObjArrayIntoRptLine(snapValues);
        DateRange thisDateRange = dateRange;

        // print out Return Dates for the various return categories for reference
        System.out.println("Reference Date: " + DateUtils.convertToShort(dateRange.getSnapDateInt()));
        for (String retCat : retDateMap.keySet()) {
            int dateInt = retDateMap.get(retCat);
            System.out.println("Period: " + retCat + " Date: " + DateUtils.convertToShort(dateInt));
        }

        //Iterate through start dates in Returns Date Map, generate associated
        //"From/To" Report, compare appropriate column in "Snap" Report with like
        // column in "From/To" Report
        for (String retCat : retDateMap.keySet()) {
            int dateInt = retDateMap.get(retCat);
            thisDateRange.setFromDateInt(dateInt);
            reportConfig = new ReportConfig(TotalFromToReport.class, "Test Report",
                    true, false, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                    ReportConfig.getDefaultViewHeader(TotalFromToReport.MODEL_HEADER),
                    ReportConfig.getDefaultExcludedAccounts(), ReportConfig.getDefaultInvestmentExpenseAccounts(),
                    ReportConfig.getDefaultInvestmentIncomeAccounts(),dateRange);
            TotalFromToReport fromToReport = new TotalFromToReport(reportConfig, currentInfo);
            fromToReport.calcReport();
            Object[][] ftValues = fromToReport.getReportTable();
            ArrayList<ReportLine> ftTest = ReportProdTest.readObjArrayIntoRptLine(ftValues);
            switch (retCat) {
                case "PREV":
                    if (testRepSnapCol(snapTest, ftTest, 5, 8))
                        errorFound = true; // Last Price
                    if (testRepSnapCol(snapTest, ftTest, 6, 6))
                        errorFound = true; // End Pos
                    if (testRepSnapCol(snapTest, ftTest, 7, 10))
                        errorFound = true; // End value
                    if (testRepSnapCol(snapTest, ftTest, 11, 22))
                        errorFound = true; // MD returns
                    break;
                case "1Wk":
                    if (testRepSnapCol(snapTest, ftTest, 12, 22)) // MD returns
                        errorFound = true;
                    break;
                case "4Wk":
                    if (testRepSnapCol(snapTest, ftTest, 13, 22))
                        errorFound = true; // MD returns
                    break;
                case "3Mnth":
                    if (testRepSnapCol(snapTest, ftTest, 14, 22))
                        errorFound = true; // MD returns
                    break;
                case "1Yr":
                    if (testRepSnapCol(snapTest, ftTest, 16, 22))
                        errorFound = true; // MD returns
                    break;
                case "3Yr":
                    if (testRepSnapCol(snapTest, ftTest, 17, 22))
                        errorFound = true; // MD returns
                    break;
                case "YTD":
                    if (testRepSnapCol(snapTest, ftTest, 15, 22))
                        errorFound = true; // MD returns
                    break;
                case "All":
                    // adjusted column to get "stub" columns from FT Report
                    if (testRepSnapCol(snapTest, ftTest, 18, 24))
                        errorFound = true; // MD returns
                    if (testRepSnapCol(snapTest, ftTest, 19, 25))
                        errorFound = true; // Ann returns
                    if (testRepSnapCol(snapTest, ftTest, 22, 15))
                        errorFound = true; // Income
                    if (testRepSnapCol(snapTest, ftTest, 26, 19))
                        errorFound = true; // Realized Gain
                    if (testRepSnapCol(snapTest, ftTest, 27, 20))
                        errorFound = true; // Unrealized Gain
                    break;
            }
        }

        String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
        System.out.println("Finished Consistency Test of RepSnap to RepFT " + msg);

        assertFalse(errorFound);
    }

}
