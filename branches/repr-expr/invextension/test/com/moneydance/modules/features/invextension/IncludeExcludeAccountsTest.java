/*
 * IncludeExcludeAccountsTest.java
 * Copyright (c) 2015, Dale K. Furrow
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
import org.junit.Test;

import java.io.File;
import java.util.TreeMap;

import static org.junit.Assert.assertFalse;


@SuppressWarnings("unused")
/**
 * Tests to ensure that, if whether income/expense accounts are included or excluded,
 * cash balances are the same.
 */
public class IncludeExcludeAccountsTest {
    public static final File mdTestFile = new File("./resources/testMD02.md");
    public static final DateRange testDateRange = new DateRange(20090601, 20100601, 20100601);
    private static final String tab = "\t";
    private static Object[][] baseReportObject;

    @Test
    public void testIncludedExcludedAccounts() throws Exception {
        RootAccount root = FileUtils.readAccountsFromFile(mdTestFile, null);
        ReportConfig reportConfig = ReportConfig.getStandardReportConfig(TotalFromToReport.class);
        reportConfig.setDateRange(testDateRange);
        reportConfig.setAllExpenseAccountsToInvestment(root);
        reportConfig.setAllIncomeAccountsToInvestment(root);
        TotalReport report = new TotalFromToReport(reportConfig);
        BulkSecInfo currentInfo = new BulkSecInfo(root, reportConfig);
        report.calcReport(currentInfo);
        IncludeExcludeAccountsTest.baseReportObject = report.getReportTable();

        reportConfig = ReportConfig.getStandardReportConfig(TotalFromToReport.class);
        reportConfig.setDateRange(testDateRange);
        report = new TotalFromToReport(reportConfig);
        currentInfo = new BulkSecInfo(root, reportConfig);
        report.calcReport(currentInfo);
        compareCashAccounts(report.getReportTable());
    }

    private void compareCashAccounts(Object[][] inputObject) {
        System.out.println("Starting Comparison of Cash Balances versus Income Treatments...");
        boolean errorFound = false;
        TreeMap<String, CashBalances> baseBalances = new TreeMap<>();
        TreeMap<String, CashBalances> testBalances = new TreeMap<>();
        // load base and test balance maps
        for (Object[] objs : baseReportObject) {
            SecurityAccountWrapper securityAccountWrapper = (SecurityAccountWrapper) objs[1];
            assert objs[9] instanceof Double;
            assert objs[10] instanceof Double;
            if (securityAccountWrapper.getName().trim().equals("CASH")) {
                baseBalances.put(securityAccountWrapper.getFullName(),
                        new CashBalances((Double) objs[9], (Double) objs[10]));
            }
        }
        for (Object[] objs : inputObject) {
            SecurityAccountWrapper securityAccountWrapper = (SecurityAccountWrapper) objs[1];
            assert objs[9] instanceof Double;
            assert objs[10] instanceof Double;
            if (securityAccountWrapper.getName().trim().equals("CASH")) {
                testBalances.put(securityAccountWrapper.getFullName(),
                        new CashBalances((Double) objs[9], (Double) objs[10]));
            }
        }
        // compare maps
        for (String fullName : baseBalances.keySet()) {
            CashBalances thisBaseBalances = baseBalances.get(fullName);
            CashBalances thisTestBalances = testBalances.get(fullName);
            if (thisBaseBalances.equals(thisTestBalances)) {
                System.out.println("Compare Balances for " + fullName + " ... Ties");
            } else {
                errorFound = true;
                String errorMsg = "Error:" + tab + fullName
                        + tab + "base start bal:" + tab + thisBaseBalances.startBalance
                        + tab + "test start bal:" + tab + thisTestBalances.startBalance
                        + tab + "base end bal:" + tab + thisBaseBalances.endBalance
                        + tab + "test end bal:" + tab + thisTestBalances.endBalance;
                System.out.println(errorMsg);
            }
        }
        String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
        System.out.println("Finished Comparison of Cash Balances versus Income Treatments " + msg);
        assertFalse(errorFound);

    }

    private static final class CashBalances {
        public static final double compareThreshold = 0.001;
        public double startBalance;
        public double endBalance;


        public CashBalances(double startBalance, double endBalance) {
            this.startBalance = startBalance;
            this.endBalance = endBalance;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CashBalances)) return false;

            CashBalances that = (CashBalances) o;

            return Math.abs(that.endBalance - endBalance) <= compareThreshold
                    && Math.abs(that.startBalance - startBalance) <= compareThreshold;

        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            temp = Double.doubleToLongBits(startBalance);
            result = (int) (temp ^ (temp >>> 32));
            temp = Double.doubleToLongBits(endBalance);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }
    }

}
