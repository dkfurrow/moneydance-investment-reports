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

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

import static org.junit.Assert.assertFalse;
import com.moneydance.modules.features.invextension.SecurityReport.MetricEntry;

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
public class ReportProdTest {
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

    /**
     * Reads object array into reportLine object
     *
     * @param inObj input array of objects
     * @return report line object
     */
    public static ArrayList<ReportLine> readObjArrayIntoRptLine(Object[][] inObj) {
        ArrayList<ReportLine> outputRptAL = new ArrayList<>();
        for (Object[] inLine : inObj) {
            ReportLine outLine = new ReportLine(inLine);
            outputRptAL.add(outLine);
        }
        Collections.sort(outputRptAL);
        return outputRptAL;
    }

    /**
     * Compares ArrayLists of report lines, returns true if error is found
     *
     * @param compRpt   comparison report generated from md data
     * @param baseRpt   base report from saved csv files
     * @param decPlaces number of decimal places to check for numbers
     * @return true if error found
     */
    private static boolean compareReports(String info,
                                          ArrayList<ReportLine> compRpt, ArrayList<ReportLine> baseRpt,
                                          int decPlaces) {
        boolean errorFound = false;
        System.out.println("Comparing Report-- " + info);
        for (int i = 0; i < compRpt.size(); i++) {
            ReportLine compLine = compRpt.get(i);
            ReportLine baseLine = baseRpt.get(i);
            if (ReportLine.compareRptLines(compLine, baseLine, decPlaces,
                    BulkSecInfoTest.limitComparisonToMinDigits)) {
                errorFound = true;
            }
        }
        String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
        System.out.println("Finished Compare of " + info + msg + "\n");
        return errorFound;
    }



    /**
     * Reads stored file, places data into sorted array of ReportLine objects
     *
     * @param readFile input file
     * @return ArrayList of ReportLine objects
     */
    private static ArrayList<ReportLine> readCSVIntoRptLine(File readFile) {
        ArrayList<String[]> inputStrAL = IOUtils.readCSVIntoArrayList(readFile);
        assert inputStrAL != null;
        inputStrAL.remove(0); // remove header row
        ArrayList<ReportLine> outputRptAL = new ArrayList<>();
        for (String[] inLine : inputStrAL) {
            ReportLine outLine = new ReportLine(inLine);
            outputRptAL.add(outLine);
        }
        Collections.sort(outputRptAL);
        return outputRptAL;
    }



    @Before
    public void setUp() throws Exception {
        currentInfo = BulkSecInfoTest.getBaseSecurityInfoAvgCost();
    }

    /**
     * Tests "From/To" Report generated from MD File
     * against saved version in CSV File
     *
     * @throws Exception
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetFromToReport() throws Exception {
        boolean errorFound;
        AggregationController aggregationController = AggregationController.INVACCT;
        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class, "Test Report",
                true, false, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalFromToReport.MODEL_HEADER),
                ReportConfig.getDefaultExcludedAccounts(), ReportConfig.getDefaultInvestmentExpenseAccounts(),
                ReportConfig.getDefaultInvestmentIncomeAccounts(),  dateRange);
        reportConfig.setAllExpenseAccountsToInvestment(currentInfo.getRoot());
        reportConfig.setAllIncomeAccountsToInvestment(currentInfo.getRoot());
        TotalFromToReport fromToReport = new TotalFromToReport(reportConfig, currentInfo);
        fromToReport.calcReport();
        Object[][] ftObj = fromToReport.getReportTable();
        ArrayList<ReportLine> ftTest = readObjArrayIntoRptLine(ftObj);
        ArrayList<ReportLine> ftBase = readCSVIntoRptLine(ftBaseFile);
        errorFound = compareReports("From/To Report", ftTest, ftBase, BulkSecInfoTest.numDigitsToCompare);

        String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
        System.out.println("Finished From/To Report Test " + msg);

        ArrayList<ComponentReport> componentReports = fromToReport.getReports();
        for (ComponentReport componentReport : componentReports) {
            CheckRepFromTo.printFromTo(componentReport);
        }

        assertFalse(errorFound);
    }

    /**
     * Tests "Snap" Report generated from MD File
     * against saved version in CSV File
     *
     * @throws Exception
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testGetSnapReport() throws Exception {
        boolean errorFound;
        AggregationController aggregationController = AggregationController.INVACCT;
        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class, "Test Report",
                true, false, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalSnapshotReport.MODEL_HEADER),
                ReportConfig.getDefaultExcludedAccounts(), ReportConfig.getDefaultInvestmentExpenseAccounts(),
                ReportConfig.getDefaultInvestmentIncomeAccounts(), dateRange);
        reportConfig.setAllExpenseAccountsToInvestment(currentInfo.getRoot());
        TotalSnapshotReport snapshotReport = new TotalSnapshotReport(reportConfig, currentInfo);
        snapshotReport.calcReport();
        Object[][] snapObj = snapshotReport.getReportTable();
        ArrayList<ReportLine> snapTest = readObjArrayIntoRptLine(snapObj);
        ArrayList<ReportLine> snapBase = readCSVIntoRptLine(snapBaseFile);
        errorFound = compareReports("Snapshot Report", snapTest, snapBase, BulkSecInfoTest.numDigitsToCompare);

        String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
        System.out.println("Finished Snap Report Test " + msg);

        assertFalse(errorFound);
    }



    // This test is not useful anymore, as the check just replicates the same computation.
/*  @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
   public void testAggregateByCurrencyReport() throws Exception {
        boolean errorFound = false;
        System.out.println("Starting Test of Aggregated Currency From/To Report");
        AggregationController aggregationController = AggregationController.TICKER;
        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class, "Test Report",
                true, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalFromToReport.MODEL_HEADER),
                ReportConfig.getDefaultExcludedAccounts(), dateRange);
        TotalFromToReport report = new TotalFromToReport(reportConfig);
        report.calcReport(currentInfo);

        HashSet<SecurityFromToReport> securityFromToReports = new HashSet<>();
        long testIncome = 0;
        long testExpense = 0;
        long testStartValue = 0;
        long testEndValue = 0;

        int reportLeafCount = 0;
        int testLeafCount = 0;

        double testMDReturn = 0.0;
        for (SecurityReport securityReport : report.getSecurityReports()) {
            SecurityFromToReport ftReport = (SecurityFromToReport) securityReport;
            securityFromToReports.add(ftReport);
            testIncome += (Long)ftReport.getSimpleMetric(SecurityReport.SMIncome);
            testExpense += (Long)ftReport.getSimpleMetric(SecurityReport.SMExpense);
            testStartValue += (Long)ftReport.getSimpleMetric(SecurityReport.SMStartValue);
            testEndValue += (Long)ftReport.getSimpleMetric(SecurityReport.SMEndValue);
            testMDReturn = ftReport.getReturnMetric(SecurityReport.RMAllReturn);
        }

//        double testMDReturn = securityFromToReports
//                .iterator()
//                .next()
//                .computeMDReturn(testStartValue, testEndValue, testIncome, testExpense, testDateMap);

        double reportMDReturn = 0;
        for (CompositeReport compositeReport : report.getCompositeReports()) {
            SecurityFromToReport aggregateReport = (SecurityFromToReport)
                    compositeReport.getAggregateReport();
            if (compositeReport.getFirstAggregator() == null
                    && compositeReport.getSecondAggregator() == null) {
                reportMDReturn = aggregateReport.getReturnMetric(SecurityReport.RMAllReturn);
                reportLeafCount = compositeReport.getSecurityReports().size();
            } else if (compositeReport.getCompositeType() == CompositeReport.COMPOSITE_TYPE.FIRST) {
                testLeafCount += compositeReport.getSecurityReports().size();
            }
        }

        if (testMDReturn == reportMDReturn) {
            System.out.println("Test Return: " + testMDReturn);
            System.out.println("Report Return: " + reportMDReturn);
            System.out.println("Manually Computed MD Return for Aggregated Currency Report Matches Total");
        } else {
            errorFound = true;
            System.out.println("Test Return: " + testMDReturn);
            System.out.println("Report Return: " + reportMDReturn);
            System.out.println("Manually Computed MD Return does not match total for Aggregated Currency Report");
        }

        if (testLeafCount == reportLeafCount) {
            System.out.println("Test Leafs: " + testLeafCount);
            System.out.println("Report Leafs: " + reportLeafCount);
            System.out.println("Leaf Security Report Count+ Matches Sum of Currency Composites");
        } else {
            errorFound = true;
            System.out.println("Test Leafs: " + testLeafCount);
            System.out.println("Report Leafs: " + reportLeafCount);
            System.out.println("Leaf Security Report Count+ Does Not Match Sum of Currency Composites!");
        }

        assertFalse(errorFound);
        System.out.println("Finished with Test of Aggregated Currency From/To Report");
    }*/


/*  @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    public void testAggregateBySecurityTypeReport() throws Exception {
        boolean errorFound = false;
        System.out.println("Starting Test of Aggregated SecurityType From/To Report");
        AggregationController aggregationController = AggregationController.SECTYPE;
        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class, "Test Report",
                true, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalFromToReport.MODEL_HEADER),
                ReportConfig.getDefaultExcludedAccounts(), dateRange);
        TotalFromToReport report = new TotalFromToReport(reportConfig);
        report.calcReport(currentInfo);

        DateMap testDateMap = new DateMap();
        long testIncome = 0;
        long testExpense = 0;
        long testStartValue = 0;
        long testEndValue = 0;

        int reportLeafCount = 0;
        int testLeafCount = 0;
        double reportMDReturn = 0;
        double testMDReturn;

        for (CompositeReport compositeReport : report.getCompositeReports()) {
            SecurityFromToReport aggregateReport = (SecurityFromToReport)
                    compositeReport.getAggregateReport();
            if (compositeReport.getFirstAggregator() == null
                    && compositeReport.getSecondAggregator() == null) {
                reportMDReturn = aggregateReport.getMdReturn();
                reportLeafCount = compositeReport.getSecurityReports().size();
            } else if (compositeReport.getCompositeType() == CompositeReport.COMPOSITE_TYPE.FIRST) {
                testLeafCount += compositeReport.getSecurityReports().size();
                testDateMap = testDateMap.combine(aggregateReport.getMdMap(), "add");
                testIncome += aggregateReport.income;
                testExpense += aggregateReport.getExpense();
                testStartValue += aggregateReport.getStartValue();
                testEndValue += aggregateReport.getEndValue();
            }
        }

        SecurityFromToReport arbitraryFromToReport =
                (SecurityFromToReport) report.getSecurityReports().iterator().next();
        testMDReturn = arbitraryFromToReport.computeMDReturn(testStartValue,
                testEndValue, testIncome, testExpense, testDateMap);

        if (Math.abs(testMDReturn - reportMDReturn) < Math.pow(1, -BulkSecInfoTest.numDigitsToCompare)) {
            System.out.println("Test Return: " + testMDReturn);
            System.out.println("Report Return: " + reportMDReturn);
            System.out.println("Manually Computed MD Return " +
                    "for Aggregated Security Report Matches Total");
        } else {
            errorFound = true;
            System.out.println("Test Return: " + testMDReturn);
            System.out.println("Report Return: " + reportMDReturn);
            System.out.println("Manually Computed MD Return " +
                    "does not match total for Aggregated Security Report");
        }

        if (testLeafCount == reportLeafCount) {
            System.out.println("Test Leafs: " + testLeafCount);
            System.out.println("Report Leafs: " + reportLeafCount);
            System.out.println("Leaf Security Report Count" +
                    " Matches Sum of Currency Composites");
        } else {
            errorFound = true;
            System.out.println("Test Leafs: " + testLeafCount);
            System.out.println("Report Leafs: " + reportLeafCount);
            System.out.println("Leaf Security Report Count" +
                    " Does Not Match Sum of Currency Composites!");
        }
        assertFalse(errorFound);
        System.out.println("Finished with Aggregated SecurityType From/To Report");
    }*/

    /**
     * Class with only one element, String array of transaction report
     * elements.  Implements comparable based on investment account and security
     * so that generated report and stored report can be sorted and compared.
     * <p/>
     * Version 1.0
     *
     * @author Dale Furrow
     */
    public static class ReportLine implements Comparable<ReportLine> {
        private String[] row;

        private ReportLine(Object[] inputObj) {
            String[] convArray = new String[inputObj.length];
            for (int i = 0; i < inputObj.length; i++) {
                Object obj = inputObj[i];
                if (obj instanceof MetricEntry) {
                    MetricEntry metricEntry = (MetricEntry) obj;
                    Double value = metricEntry.getDisplayValue();
                    if(value.equals(SecurityReport.UndefinedReturn)){
                        convArray[i] = "";
                    } else {
                        convArray[i] = value.toString();
                    }

                } else if (isObjectAggregator(obj)) {
                    convArray[i] = getNameFromObject(obj);
                } else {
                    convArray[i] = null;
                }
            }
            this.row = convArray;
        }

        private ReportLine(String[] inputArray) {
            this.row = inputArray;
        }

        /**
         * Compares two report lines, based on
         * decimal place threshold.
         *
         * @param compRpt        report to be compared
         * @param baseRpt        base report
         * @param decPlaces      precision used for comparison
         * @param limitPrecision boolean (whether to limit precision)
         * @return pass/fail of test
         */
        private static boolean compareRptLines(ReportLine compRpt,
                                               ReportLine baseRpt, int decPlaces, boolean limitPrecision) {
            boolean errorFound = false;
            for (int i = 0; i < compRpt.getRow().length; i++) {
                String compStr = compRpt.getRow()[i];
                String baseStr = baseRpt.getRow()[i];
                if (!BulkSecInfoTest.similarElements(compStr, baseStr, decPlaces, limitPrecision)) {
                    printErrorMessage(compRpt, baseRpt, i);
                    errorFound = true;
                }
            }
            if (!errorFound) {
                String info = "Account: " + compRpt.getRow()[0] + " Security: " + compRpt.getRow()[1];
                System.out.println("Tested and Passed: " + info);
            }
            return errorFound;
        }

        private static void printErrorMessage(ReportLine compRpt,
                                              ReportLine baseRpt, int i) {
            System.out.println("Error at " + i + " member of report line"
                    + "-- Acct: " + compRpt.getRow()[0]
                    + " Security: " + compRpt.getRow()[1]
                    + " Test = " + compRpt.getRow()[i]
                    + " Should = " + baseRpt.getRow()[i]);
        }

        public boolean isObjectAggregator(Object o) {
            return o instanceof Aggregator;
        }

        public String getNameFromObject(Object o) {
            String outputName = "";
            if (o instanceof InvestmentAccountWrapper) {
                outputName = ((InvestmentAccountWrapper) o).getName();
            } else if (o instanceof SecurityAccountWrapper) {
                outputName = ((SecurityAccountWrapper) o).getName();
            } else if (o instanceof SecurityTypeWrapper) {
                outputName = ((SecurityTypeWrapper) o).getName();
            } else if (o instanceof SecuritySubTypeWrapper) {
                outputName = ((SecuritySubTypeWrapper) o).getName();
            } else if (o instanceof CurrencyWrapper) {
                outputName = ((CurrencyWrapper) o).getName();
            } else {
                try {
                    throw new Exception("invalid attempt to get name from object");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return outputName;
        }

        /* Compares based on concatenation of investment name
         * and security name
         *
         */
        @Override
        public int compareTo(@NotNull ReportLine r) {
            String cStrComp = r.getRow()[0] + r.getRow()[1];
            String cStrThis = this.getRow()[0] + this.getRow()[1];
            return cStrThis.compareTo(cStrComp);
        }

        public String[] getRow() {
            return this.row;
        }

        public String toString(){
            StringBuilder sb = new StringBuilder();
            for(String s : this.getRow()){
                sb.append(s).append(':');
            }
            return sb.toString();
        }
    }
}
