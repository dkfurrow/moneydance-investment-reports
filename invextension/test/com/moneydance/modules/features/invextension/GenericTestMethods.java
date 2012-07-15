/* GenericTestMethods.java
 * Copyright 2011 Dale K. Furrow . All rights reserved.
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR ANY 
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.moneydance.modules.features.invextension;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.moneydance.apps.md.controller.io.FileUtils;
import com.moneydance.apps.md.model.AbstractTxn;
import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.CurrencyType;
import com.moneydance.apps.md.model.InvestmentAccount;
import com.moneydance.apps.md.model.ParentTxn;
import com.moneydance.apps.md.model.RootAccount;
import com.moneydance.apps.md.model.SplitTxn;
import com.moneydance.apps.md.model.TransactionSet;
import com.moneydance.apps.md.model.TxnSet;
import com.moneydance.apps.md.model.TxnUtil;

public class GenericTestMethods {
    
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final File mdTestFile = new File("./resources/testMD02.md");
    public static final File testFile = new File("E:\\Temp\\testFile.csv");
    public static final Class<InvestmentAccountWrapper> invAggClass = InvestmentAccountWrapper.class;
    public static final Class<Tradeable> tradeableAggClass = Tradeable.class;
    public static final Class<CurrencyWrapper> currencyAggClass = CurrencyWrapper.class;
    public static final Class<AllAggregate> allAggClass = AllAggregate.class;
    public static final Class<SecurityTypeWrapper> secTypeAggClass = SecurityTypeWrapper.class;
    public static final Class<SecuritySubTypeWrapper> secSubTypeAggClass = SecuritySubTypeWrapper.class;
    public static final boolean catHierarchy = false;
    public static final boolean rptOutputSingle = true;

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
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static void verifyAggregateByCurrencyReport(BulkSecInfo currentInfo)
	    throws Exception {
	TotalFromToReport report = new TotalFromToReport(currentInfo,
		currencyAggClass, allAggClass, false, true, fromDateInt,
		toDateInt);

	HashSet<SecurityFromToReport> securityFromToReports = new HashSet<SecurityFromToReport>();
	DateMap testDateMap = new DateMap();
	double testIncome = 0.0;
	double testExpense = 0.0;
	double testStartValue = 0.0;
	double testEndValue = 0.0;

	int reportLeafCount = 0;
	int testLeafCount = 0;

	for (Iterator iterator = report.getSecurityReports().iterator(); iterator
		.hasNext();) {
	    SecurityReport securityReport = (SecurityReport) iterator.next();
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

	for (Iterator iterator = report.getCompositeReports().iterator(); iterator
		.hasNext();) {
	    CompositeReport compositeReport = (CompositeReport) iterator.next();
	    SecurityFromToReport aggregateReport = (SecurityFromToReport)
		    compositeReport.aggregateReport;
	    if (compositeReport.firstAggregateVal == null
		    && compositeReport.secondAggregateVal == null) {
		reportMDReturn = aggregateReport.getMdReturn();
		reportLeafCount = compositeReport.securityReports.size();
	    } else {
		testLeafCount += compositeReport.securityReports.size();
	    }
	}

	System.out.println("Report Return: " + reportMDReturn);
	System.out
		.println(testMDReturn == reportMDReturn ? "Passed" : "Failed");
	System.out.println("Test Leafs: " + testLeafCount);
	System.out.println("Report Leafs: " + reportLeafCount);

    }

    public static void listTransactionCounts(BulkSecInfo currentInfo) {
	TreeSet<Account> allAccts = BulkSecInfo.getSelectedSubAccounts(
		currentInfo.getRoot(), Account.ACCOUNT_TYPE_INVESTMENT,
		Account.ACCOUNT_TYPE_SECURITY);
	TransactionSet transSet = currentInfo.getRoot().getTransactionSet();
	for (Iterator<Account> iterator = allAccts.iterator(); iterator
		.hasNext();) {
	    Account account = (Account) iterator.next();
	    TxnSet txnSet = transSet.getTransactionsForAccount(account);
	    System.out.println("Parent Acct: "
		    + account.getParentAccount().getAccountName()
		    + " Account: " + account.getAccountName() + " Size: "
		    + txnSet.getSize());

	}

    }

    private static void listBaseCurrency(BulkSecInfo currentInfo) {
	CurrencyType baseCur = currentInfo.getRoot().getCurrencyType();
	System.out.println("Root Account Currency Name: " + baseCur.getName());
	HashSet<Account> accts = currentInfo.getSecAccts();
	for (Iterator iterator = accts.iterator(); iterator.hasNext();) {
	    Account account = (Account) iterator.next();
	    if (account.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT)
		System.out.println(account.getAccountName() + " has Currency: "
			+ account.getCurrencyType().getName());
	    
	}
	
    }   

    private static void listCreateDatesANDInitBals(BulkSecInfo currentInfo) {
	HashMap<Account, Double> initBals = new HashMap<Account, Double>();
	HashMap<Account, Integer> createDates = new HashMap<Account, Integer>();
	HashSet<InvestmentAccountWrapper> theseInvs = currentInfo
		.getInvestmentWrappers();
	for (Iterator iterator = theseInvs.iterator(); iterator.hasNext();) {
	    InvestmentAccountWrapper investmentAccountWrapper = (InvestmentAccountWrapper) iterator
		    .next();
	    InvestmentAccount invAcct = (InvestmentAccount) investmentAccountWrapper
		    .getInvestmentAccount();
	    initBals.put(invAcct, Long.valueOf(invAcct.getStartBalance())
		    .doubleValue());
	    createDates.put(invAcct, invAcct.getCreationDateInt());
	}
	System.out
		.println("\nAccounts with Creation Dates and Initial Balances Follow");
	for (Iterator iterator = createDates.keySet().iterator(); iterator
		.hasNext();) {
	    Account acct = (Account) iterator.next();
	    System.out.println(acct.getAccountName() + ": Create Date -- "
		    + DateUtils.convertToShort(createDates.get(acct))
		    + " Init Bal -- " + initBals.get(acct));

	}

    }
    
    public static void writeCurrenciesToFile(BulkSecInfo currentInfo) {
	ArrayList<String[]> secPricesReport = BulkSecInfo
		.ListAllCurrenciesInfo(BulkSecInfo.getCurrencyWrappers());
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

    public static String getAcctInfo(Account acct) {
	
	return ("Account Name: " + acct.getAccountName() + " AcctNum: " + acct
		.getAccountNum() ) ;
    }
    
    public static void listTransInfo(BulkSecInfo currentInfo) throws Exception {
	ArrayList<String[]> transActivityReport
        = currentInfo.listTransValuesSet(currentInfo.getInvestmentWrappers());
	File transActivityReportFile = new File("E:\\Temp"
		+ "\\transActivityReport1.csv");
	IOUtils.writeArrayListToCSV(TransactionValues.listTransValuesHeader(),
		transActivityReport, transActivityReportFile);
	System.out.println("Done Writing TransValuesMap");
    }
    
    public static void listTransWithTransUtil(BulkSecInfo currentInfo)
	    throws Exception {
	TreeSet<TransactionValues> allTransValues = new TreeSet<TransactionValues>();
	ArrayList<String[]> transValuesData = new ArrayList<String[]>();
	for (Iterator iterator = currentInfo.getInvestmentWrappers().iterator(); iterator
		.hasNext();) {
	    InvestmentAccountWrapper inv = (InvestmentAccountWrapper) iterator
		    .next();
	    allTransValues.addAll(inv.getTransactionValues());
	}

	for (Iterator iterator = allTransValues.iterator(); iterator.hasNext();) {
	    TransactionValues transactionValues = (TransactionValues) iterator.next();
	    ArrayList<String> dataStrArray = new ArrayList<String>();
	    SplitTxn secSplit = TxnUtil.getSecurityPart(transactionValues.getParentTxn());
	    SplitTxn commisSplit = TxnUtil
		    .getCommissionPart(transactionValues.getParentTxn());
	    SplitTxn expenseSplit = TxnUtil
		    .getExpensePart(transactionValues.getParentTxn());
	    SplitTxn xferSplit = TxnUtil
		    .getXfrPart(transactionValues.getParentTxn());
	    
	    Double secDouble = secSplit == null ? Double.NaN : (Long
		    .valueOf(secSplit.getAmount()).doubleValue()) / 100.0;
	    Double commisDouble = commisSplit == null ? Double.NaN : (Long
		    .valueOf(commisSplit.getAmount()).doubleValue()) / 100.0;
	    Double expenseDouble = expenseSplit == null ? Double.NaN : (Long
		    .valueOf(expenseSplit.getAmount()).doubleValue()) / 100.0;
	    Double xferDouble = xferSplit == null ? Double.NaN : (Long
		    .valueOf(xferSplit.getAmount()).doubleValue()) / 100.0;
	    String[] oldLineArray = TransactionValues
		    .loadArrayTransValues(transactionValues);
	    for (String string : oldLineArray) {
		dataStrArray.add(string);
	    }
	    dataStrArray.add(Double.toString(secDouble));
	    dataStrArray.add(Double.toString(commisDouble));
	    dataStrArray.add(Double.toString(expenseDouble));
	    dataStrArray.add(Double.toString(xferDouble));
	    transValuesData.add(dataStrArray.toArray(new String[dataStrArray
		    .size()]));
	}

	StringBuffer newHeader = TransactionValues.listTransValuesHeader();
	newHeader.append(",");
	newHeader.append("SecurityPart,");
	newHeader.append("CommisPart,");
	newHeader.append("expensePart,");
	newHeader.append("xferPart,");

	File transActivityReportFile = new File("E:\\Temp"
		+ "\\transActivityReport2.csv");
	IOUtils.writeArrayListToCSV(newHeader, transValuesData,
		transActivityReportFile);
	System.out.println("Done Writing New Transaction Data");
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

    public static void testCompareSpec(){
	LinkedList<Character> startChars = new LinkedList<Character>();
	startChars.add("-".charAt(0));
	startChars.add("~".charAt(0));
	for (Character character : startChars) {
	    System.out.println(character);
	}
	System.out.println(compareToSpecChar(startChars, "cat", "-Cat"));
    }

}
