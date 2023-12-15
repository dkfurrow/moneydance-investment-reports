/*
 * GenericTestMethods.java
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

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountBook;
import com.moneydance.apps.md.controller.io.FileUtils;

import java.io.File;
import java.util.GregorianCalendar;

public class GenericTestMethods {
    public static final File mdTestFile = new File("./resources/testMD02.md");
    public static final boolean rptOutputSingle = true;
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final DateRange dateRange = new DateRange(fromDateInt, toDateInt, toDateInt);

    public static void main(String[] args) throws Exception {
//        Account root = FileUtils.readAccountsFromFile(mdTestFolder, null);
//        BulkSecInfo currentInfo = new BulkSecInfo(root, ReportConfig.getStandardReportConfig(TotalFromToReport.class));

//	listBaseCurrency(currentInfo);
//	listAssocSecTnsMap(currentInfo);
//	listInvSecmap(currentInfo);
//	listCreateDatesANDInitBals(currentInfo);
//	System.out.println("First Date: "+ currentInfo.firstDateInt);
//	System.out.println("Next AcctNum: "+ currentInfo.nextAcctNum);
//	writeCurrenciesToFile(currentInfo);
//	listTransInfo(currentInfo);
//	listTransactionCounts(currentInfo);
//	testCompareSpec();
//	listTransWithTransUtil(currentInfo);
//        verifyAggregateByCurrencyReport(currentInfo);
//        System.out.println("DataFile Path: " + root.getDataFile().getParentFile().getAbsolutePath());
//        long sampleDateValue = DateUtils.getExcelDateValue(20100208);
//        System.out.println("Sample Date Value" + sampleDateValue);
//        GregorianCalendar gc = new GregorianCalendar(1900, 1, 1);
//        gc.setTimeInMillis(0L);

//        System.out.println("Long time: " + gc.getTime());
//        Account account = new Account(AccountBook.nullAccountBook());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void verifyAggregateByCurrencyReport(BulkSecInfo currentInfo)
            throws Exception {
/*        int numFrozenColumns = 5; //irrelevant for purposes of test
        boolean closedPosHidden = true; //irrelevant for purposes of test
        AggregationController aggregationController = AggregationController.TICKER;
        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class, "Test Report",
                true, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalFromToReport.MODEL_HEADER),
                ReportConfig.getDefaultExcludedAccounts(), dateRange);
        TotalFromToReport report = new TotalFromToReport(reportConfig);
        report.calcReport(currentInfo);

        HashSet<SecurityFromToReport> securityFromToReports = new HashSet<>();
        DateMap testDateMap = new DateMap();
        long testIncome = 0;
        long testExpense = 0;
        long testStartValue = 0;
        long testEndValue = 0;

        int reportLeafCount = 0;
        int testLeafCount = 0;

        for (SecurityReport securityReport : report.getSecurityReports()) {
            SecurityFromToReport securityFromToReport = (SecurityFromToReport) securityReport;
            securityFromToReports.add(securityFromToReport);
            testDateMap = testDateMap
                    .combine(securityFromToReport.getMdMap(), "add");
            testIncome += securityFromToReport.income;
            testExpense += securityFromToReport.getExpense();
            testStartValue += securityFromToReport.getStartValue();
            testEndValue += securityFromToReport.getEndValue();
        }

        double testMDReturn = securityFromToReports
                .iterator()
                .next()
                .computeMDReturn(testStartValue, testEndValue, testIncome,
                        testExpense, testDateMap);
        double reportMDReturn = 0;

        System.out.println("Test Return: " + testMDReturn);

        for (CompositeReport compositeReport : report.getCompositeReports()) {
            SecurityFromToReport aggregateReport = (SecurityFromToReport)
                    compositeReport.getAggregateReport();
            if (compositeReport.getFirstAggregator() == null
                    && compositeReport.getSecondAggregator() == null) {
                reportMDReturn = aggregateReport.getMdReturn();
                reportLeafCount = compositeReport.getSecurityReports().size();
            } else {
                testLeafCount += compositeReport.getSecurityReports().size();
            }
        }

        System.out.println("Report Return: " + reportMDReturn);
        System.out
                .println(testMDReturn == reportMDReturn ? "Passed" : "Failed");
        System.out.println("Test Leafs: " + testLeafCount);
        System.out.println("Report Leafs: " + reportLeafCount);*/
    }


//    private static void listInvSecmap(BulkSecInfo currentInfo) throws Exception {
//	HashSet<InvestmentAccountWrapper> theseInvs = currentInfo.invs;
//	TreeMap<Account, SortedSet<TransactionValues>> transValuesMap = currentInfo.transValuesMap;
//	for (Iterator iterator = theseInvs.iterator(); iterator.hasNext();) {
//	    InvestmentAccountWrapper parentAccountWrapper = (InvestmentAccountWrapper) iterator
//		    .next();
//	    int creationDateInt = parentAccountWrapper.invAcct
//		    .getCreationDateInt();
//	    System.out.println("\n" + "["
//		    + getAcctInfo(parentAccountWrapper.invAcct)
//		    + "Creation Date: " + creationDateInt + "]"
//		    + " Child Accounts:");
//	    HashSet<IAccount> subAcctWrappers = parentAccountWrapper
//		    .getAllAccountWrappers();
//
//	    for (Iterator iterator2 = subAcctWrappers.iterator(); iterator2
//		    .hasNext();) {
//		IAccount secAcctWrapper = (IAccount) iterator2
//			.next();
//		String numTrans = secAcctWrapper.getTransValues().size()
//			+ " Transactions ";
//		System.out.println("--"
//			+ getAcctInfo(secAcctWrapper.getSecurityAccount())
//			+ numTrans);
//
//	    }
//
//	}
//
//    }
}
