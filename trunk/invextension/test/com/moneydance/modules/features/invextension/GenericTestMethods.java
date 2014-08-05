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

import com.moneydance.apps.md.controller.io.FileUtils;
import com.moneydance.apps.md.model.*;

import java.io.File;
import java.util.*;

public class GenericTestMethods {

    public static final File mdTestFile = new File("./resources/testMD02.md");
    public static final boolean rptOutputSingle = true;
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final DateRange dateRange = new DateRange(fromDateInt, toDateInt, toDateInt);

    public static void main(String[] args) throws Exception {
        RootAccount root = FileUtils.readAccountsFromFile(mdTestFile, null);
        BulkSecInfo currentInfo = new BulkSecInfo(root, new GainsAverageCalc());

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
        verifyAggregateByCurrencyReport(currentInfo);
        System.out.println("DataFile Path: "
                + root.getDataFile().getParentFile().getAbsolutePath());
        Double sampleDateValue = DateUtils.getExcelDateValue(20100208);
        System.out.println("Sample Date Value" + sampleDateValue);
        GregorianCalendar gc = new GregorianCalendar(1900, 1, 1);
        gc.setTimeInMillis(0L);

        System.out.println("Long time: " + gc.getTime());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void verifyAggregateByCurrencyReport(BulkSecInfo currentInfo)
            throws Exception {
        int numFrozenColumns = 5; //irrelevant for purposes of test
        boolean closedPosHidden = true; //irrelevant for purposes of test
        AggregationController aggregationController = AggregationController.TICKER;
        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class, "Test Report",
                aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalFromToReport.MODEL_HEADER), dateRange);
        TotalFromToReport report = new TotalFromToReport(reportConfig);
        report.calcReport(currentInfo);

        HashSet<SecurityFromToReport> securityFromToReports = new HashSet<>();
        DateMap testDateMap = new DateMap();
        double testIncome = 0.0;
        double testExpense = 0.0;
        double testStartValue = 0.0;
        double testEndValue = 0.0;

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
        double reportMDReturn = 0.0;

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
        System.out.println("Report Leafs: " + reportLeafCount);

    }

    @SuppressWarnings("unused")
    public static void listTransactionCounts(BulkSecInfo currentInfo) {
        TreeSet<Account> allAccts = BulkSecInfo.getSelectedSubAccounts(
                currentInfo.getRoot(), Account.ACCOUNT_TYPE_INVESTMENT,
                Account.ACCOUNT_TYPE_SECURITY);
        TransactionSet transSet = currentInfo.getRoot().getTransactionSet();
        for (Account account : allAccts) {
            TxnSet txnSet = transSet.getTransactionsForAccount(account);
            System.out.println("Parent Acct: "
                    + account.getParentAccount().getAccountName()
                    + " Account: " + account.getAccountName() + " Size: "
                    + txnSet.getSize());

        }

    }

    @SuppressWarnings("unused")
    private static void listBaseCurrency(BulkSecInfo currentInfo) {
        CurrencyType baseCur = currentInfo.getRoot().getCurrencyType();
        System.out.println("Root Account Currency Name: " + baseCur.getName());
        HashSet<InvestmentAccountWrapper> investmentAccountWrappers = currentInfo.getInvestmentWrappers();

        for (InvestmentAccountWrapper investmentAccountWrapper : investmentAccountWrappers) {
            Account investmentAccount = investmentAccountWrapper.getInvestmentAccount();
            System.out.println(investmentAccount.getAccountName() + " has Currency: "
                    + investmentAccount.getCurrencyType().getName());
        }
    }

    @SuppressWarnings("unused")
    private static void listCreateDatesANDInitBals(BulkSecInfo currentInfo) {
        HashMap<Account, Double> initBals = new HashMap<>();
        HashMap<Account, Integer> createDates = new HashMap<>();
        HashSet<InvestmentAccountWrapper> theseInvs = currentInfo
                .getInvestmentWrappers();
        for (InvestmentAccountWrapper investmentAccountWrapper : theseInvs) {
            InvestmentAccount invAcct = (InvestmentAccount) investmentAccountWrapper
                    .getInvestmentAccount();
            initBals.put(invAcct, Long.valueOf(invAcct.getStartBalance())
                    .doubleValue());
            createDates.put(invAcct, invAcct.getCreationDateInt());
        }
        System.out
                .println("\nAccounts with Creation Dates and Initial Balances Follow");
        for (Account acct : createDates.keySet()) {
            System.out.println(acct.getAccountName() + ": Create Date -- "
                    + DateUtils.convertToShort(createDates.get(acct))
                    + " Init Bal -- " + initBals.get(acct));

        }

    }

    @SuppressWarnings("unused")
    public static void writeCurrenciesToFile(BulkSecInfo currentInfo) {
        ArrayList<String[]> secPricesReport = BulkSecInfo
                .ListAllCurrenciesInfo(currentInfo.getCurrencyWrappers());
        File secPricesReportFile = new File("E:\\Temp\\secPricesReport.csv");
        IOUtils.writeArrayListToCSV(BulkSecInfo.listCurrencySnapshotHeader(),
                secPricesReport, secPricesReportFile);
        System.out.println("Done Writing Currencies");
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

    @SuppressWarnings("unused")
    public static String getAcctInfo(Account acct) {

        return ("Account Name: " + acct.getAccountName() + " AcctNum: " + acct
                .getAccountNum());
    }

    @SuppressWarnings("unused")
    public static void listTransInfo(BulkSecInfo currentInfo) throws Exception {
        ArrayList<String[]> transActivityReport
                = currentInfo.listAllTransValues(currentInfo.getInvestmentWrappers());
        File transActivityReportFile = new File("E:\\Temp"
                + "\\transActivityReport1.csv");
        IOUtils.writeArrayListToCSV(TransactionValues.listTransValuesHeader(),
                transActivityReport, transActivityReportFile);
        System.out.println("Done Writing TransValuesMap");
    }

    public static int compareToSpecChar(LinkedList<Character> startChars,
                                        String o1, String o2) {

        char o11 = o1.charAt(0);
        char o21 = o2.charAt(0);
        if (startChars.contains(o11) && startChars.contains(o21)) {
            int indDiff = startChars.indexOf(o11) - startChars.indexOf(o21);
            if (indDiff == 0) {
                // either they start with the same char, in which case compare
                return o1.compareTo(o2);
            } else { // return difference in indices
                return indDiff;
            }
        } else if (startChars.contains(o11) && !startChars.contains(o21)) {
            return 1; // first String has special character
        } else if (!startChars.contains(o11) && startChars.contains(o21)) {
            return -1; // second string has special character
        } else {// neither first letter is special character
            return o1.compareTo(o2);

        }
    }

    @SuppressWarnings("unused")
    public static void testCompareSpec() {
        LinkedList<Character> startChars = new LinkedList<>();
        startChars.add("-".charAt(0));
        startChars.add("~".charAt(0));
        for (Character character : startChars) {
            System.out.println(character);
        }
        System.out.println(compareToSpecChar(startChars, "cat", "-Cat"));
    }

}
