/*
 * BulkSecInfoTest.java
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

import com.moneydance.apps.md.controller.io.FileUtils;
import com.moneydance.apps.md.model.RootAccount;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertFalse;

/**
 * Class which compares transaction report lines (i.e. allows for sorting of
 * transaction report lines, and compares generated report to stored report)
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class BulkSecInfoTest {
    // Stored Test Database
    public static final File mdTestFile = new File("./resources/testMD02.moneydance/root.mdinternal");
    // Stored csv file of transaction activity report (average cost)
    public static final File mdTestFileCSVAvgCost = new File("./resources/transActivityReportAvgCost.csv");
    // Stored csv file of transaction activity report (lot matching)
    public static final File mdTestFileCSVLotMatch = new File("./resources/transActivityReportLotMatch.csv");
    // The following two elements determine precision of testing decimal places for comparison
    public static final int numDigitsToCompare = 4;
    // Limits precision to minimum digits of file or report output. To generate a failed test,
    // set to false--generated report and stored report have different decimal place precisions.
    public static final boolean limitComparisonToMinDigits = true;

    /**
     * Gets BulkSecInfo from stored moneydance data file (avg cost basis)
     *
     * @return BulkSecInfo from stored file
     * @throws Exception
     */
    public static BulkSecInfo getBaseSecurityInfoAvgCost() throws Exception {
        RootAccount root = FileUtils.readAccountsFromFile(mdTestFile, null);
        ReportConfig reportConfig = ReportConfig.getStandardReportConfig(TotalFromToReport.class);
        reportConfig.setAllExpenseAccountsToInvestment(root);
        reportConfig.setAllIncomeAccountsToInvestment(root);
        return new BulkSecInfo(root, reportConfig);
    }

    /**
     * Gets BulkSecInfo from stored moneydance data file (lot matching basis)
     *
     * @return BulkSecInfo with appropriate gains treatment
     * @throws Exception
     */
    public static BulkSecInfo getBaseSecurityInfoLotMatch() throws Exception {
        RootAccount root = FileUtils.readAccountsFromFile(mdTestFile, null);
        ReportConfig reportConfig = ReportConfig.getStandardReportConfig(TotalFromToReport.class);
        reportConfig.setAllExpenseAccountsToInvestment(root);
        reportConfig.setAllIncomeAccountsToInvestment(root);
        reportConfig.setUseAverageCostBasis(false);
        return new BulkSecInfo(root, reportConfig);
    }

    /**
     * Returns number of digits in double, either in basic or exponential format
     *
     * @param d input double
     * @return number of digits in double
     */
    private static int getPrecisionDigits(double d) {
        String s = Double.toString(d).toUpperCase();
        if (!s.contains(".")) {
            return 0;
        } else {
            if (s.contains("E")) {
                return s.substring(s.indexOf(".") + 1, s.indexOf("E")).length();
            } else {
                return s.substring(s.indexOf(".") + 1).length();
            }
        }
    }

    /**
     * Compares two strings--if not numeric, returns string comparison.
     * If numeric, determines whether numbers are within specified
     * difference of each other.
     *
     * @param s1             first string to compare
     * @param s2             second string to compare
     * @param decPlaces      decimal places of precision to compare
     * @param limitPrecision whether the decimal places apply
     * @return pass/fail on comparison
     */
    public static boolean similarElements(String s1, String s2, int decPlaces,
                                          boolean limitPrecision) {
        try {
            double d1 = Double.parseDouble(s1);
            double d2 = Double.parseDouble(s2);

            // Original Invextension uses NaN as uncertain value. This code uses 0. Meaning is the same.
            // Also, the difference test below does not catch the case when both values are NaN.
            if (Double.isNaN(d1) && (d2 == 0.0 || Double.isNaN(d2))) {
                return true;
            } else if ((d1 == 0.0 || Double.isNaN(d1)) && Double.isNaN(d2)) {
                return true;
            }

            // Fewest number of digits after decimal place
            int lowDigits = Math.min(getPrecisionDigits(d1), getPrecisionDigits(d2));
            double threshold = Math.pow(10, limitPrecision ? -Math.min(decPlaces, lowDigits) : -decPlaces);
            return Math.abs(d1 - d2) < threshold;
        } catch (NumberFormatException e) {
            return s1.equals(s2);   // One or both string is not a number
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Compares two dates.
     * Assumes that both strings are properly formatted dates.
     * Assumes file in resources directory is saved per baseFormat below
     *
     * @param compDateStr compareison date string
     * @param baseDateStr base stored date to compare to
     * @return pass/fail on comparison test
     * @throws ParseException
     */
    public static boolean sameDateStrings(String compDateStr, String baseDateStr)
            throws ParseException {
        SimpleDateFormat localFormat = new SimpleDateFormat(DateRangePanel.DATE_PATTERN, Locale.getDefault());
        Date baseDate = localFormat.parse(baseDateStr);
        Date compDate = localFormat.parse(compDateStr);
        return baseDate.equals(compDate);
    }

    /**
     * Compares ArrayLists of transaction lines, returns true if error is found
     *
     * @param compRpt   comparison report generated from MD data
     * @param baseRpt   comparison report generated from stored csv file
     * @param decPlaces number of decimal places to compare
     * @return true if error found
     * @throws ParseException
     */
    private static boolean compareTransactions(ArrayList<TransLine> compRpt,
                                               ArrayList<TransLine> baseRpt, int decPlaces) throws ParseException {
        boolean errorFound = false;
        System.out.println("Comparing Transactions-- ");
        for (int i = 0; i < compRpt.size(); i++) {
            TransLine compLine = compRpt.get(i);
            TransLine baseLine = baseRpt.get(i);
            if (TransLine.compareTransLines(compLine, baseLine, decPlaces, limitComparisonToMinDigits)) {
                errorFound = true;
            }
        }
        String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
        System.out.println("Finished Compare of Transaction Data" + msg + "\n");
        return errorFound;
    }

    /**
     * Reads stored file, places data into sorted array of TransLine objects
     *
     * @param readFile file to be read
     * @return ArrayList of TransLine type
     */
    private static ArrayList<TransLine> readCSVIntoTransLine(File readFile) {
        ArrayList<String[]> inputStrAL = IOUtils.readCSVIntoArrayList(readFile);
        inputStrAL.remove(0); // remove header row
        ArrayList<TransLine> outputTransAL = new ArrayList<>();
        for (String[] inLine : inputStrAL) {
            TransLine outLine = new TransLine(inLine);
            outputTransAL.add(outLine);
        }
        Collections.sort(outputTransAL);
        return outputTransAL;
    }

    /**
     * Generates transaction report from stored md file, places data into
     * sorted array of TransLine objects
     *
     * @param currentInfo BulkSecInfo associated with stored MD file
     * @return ArrayList of type Transline from stored md data
     * @throws Exception
     */
    private static ArrayList<TransLine> readStringArrayIntoTransLine(BulkSecInfo currentInfo) throws Exception {
        ArrayList<String[]> transActivityReport
                = currentInfo.listAllTransValues(currentInfo.getInvestmentWrappers());
        ArrayList<TransLine> outputTransAL = new ArrayList<>();
        for (String[] row : transActivityReport) {
            TransLine line = new TransLine(row);
            outputTransAL.add(line);
        }
        Collections.sort(outputTransAL);
        return outputTransAL;
    }

    /**
     * Test Method which compares TransValuesCum generated from test database
     * with stored report. (Average Cost)
     */
    @Test
    public void testListTransValuesCumMapAvgCost() {
        boolean errorFound = false;
        ArrayList<TransLine> transBase;
        ArrayList<TransLine> transTest;
        try {
            BulkSecInfo currentInfo = getBaseSecurityInfoAvgCost();
            transBase = readCSVIntoTransLine(mdTestFileCSVAvgCost);
            transTest = readStringArrayIntoTransLine(currentInfo);
            if (compareTransactions(transTest, transBase, numDigitsToCompare)) {
                errorFound = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
        System.out.println("Finished TransValuesCumMap Test for Average Cost " + msg);
        assertFalse(errorFound);
    }

    /* Test Method which compares TransValuesCum generated from test database
    * with stored report. (Lot Matching)
    * @throws Exception
    */
    @Test
    public void testListTransValuesCumMapLotMatch() {
        boolean errorFound = false;
        ArrayList<TransLine> transBase;
        ArrayList<TransLine> transTest;
        try {
            BulkSecInfo currentInfo = getBaseSecurityInfoLotMatch();
            transBase = readCSVIntoTransLine(mdTestFileCSVLotMatch);
            transTest = readStringArrayIntoTransLine(currentInfo);
            if (compareTransactions(transTest, transBase, numDigitsToCompare)) {
                errorFound = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
        System.out.println("Finished TransValuesCumMap Test for Lot Match " + msg);
        assertFalse(errorFound);
    }

    /**
     * Class with only one element, String array of transaction report
     * elements.  Implements comparable based on transaction id
     * so that generated report and stored report can be sorted and compared.
     * <p/>
     * Version 1.0
     *
     * @author Dale Furrow
     */
    private static class TransLine implements Comparable<TransLine> {
        private String[] row;

        private TransLine(String[] inputArray) {
            this.row = inputArray;
        }

        /**
         * Compares two transaction lines, based on
         * decimal place threshold.
         *
         * @param compRpt        test report generated from MD Data
         * @param baseRpt        comparison report generated from stored csv file
         * @param decPlaces      number of decimal places to compare
         * @param limitPrecision whether the decimal places apply
         * @return pass/fail on comparison test
         * @throws ParseException
         */
        private static boolean compareTransLines(TransLine compRpt,
                                                 TransLine baseRpt, int decPlaces, boolean limitPrecision)
                throws ParseException {
            boolean errorFound = false;
            for (int i = 0; i < compRpt.row.length; i++) {
                String compStr = compRpt.getRow()[i];
                String baseStr = baseRpt.getRow()[i];
                if (i == 5) {
                    if (!sameDateStrings(compStr, baseStr)) {
                        printErrorMessage(compRpt, baseRpt, i);
                        errorFound = true;
                    }
                } else {
                    if (!similarElements(compStr, baseStr, decPlaces, limitPrecision)) {
                        printErrorMessage(compRpt, baseRpt, i);
                        errorFound = true;
                    }
                }
            }
            if (!errorFound) {
                String info = "Account: " + compRpt.getRow()[0]
                        + " Security: " + compRpt.getRow()[1]
                        + " Transaction: " + compRpt.getRow()[4];
                System.out.println("Tested and Passed: " + info);
            }
            return errorFound;
        }

        private static void printErrorMessage(TransLine compRpt, TransLine baseRpt, int i) {
            System.out.println("Error at " + i + " member of report line"
                    + "-- Acct: " + compRpt.row[0]
                    + " Security: " + compRpt.row[1]
                    + " Transaction: " + compRpt.row[4]
                    + " Test = " + compRpt.getRow()[i]
                    + " Should = " + baseRpt.row[i]);
        }

        @Override
        public int compareTo(@NotNull TransLine t) {
            Double cDoubleComp = Double.parseDouble(t.getRow()[4]);
            Double cDoubleThis = Double.parseDouble(this.getRow()[4]);
            return cDoubleThis.compareTo(cDoubleComp);
        }

        private String[] getRow() {
            return this.row;
        }
    }
}
