package com.moneydance.modules.features.invextension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;

import com.moneydance.apps.md.model.Account;
import com.moneydance.modules.features.invextension.BulkSecInfo;
import com.moneydance.modules.features.invextension.BulkSecInfo.AGG_TYPE;
import com.moneydance.modules.features.invextension.DateUtils;
import com.moneydance.modules.features.invextension.RepFromTo;
import com.moneydance.modules.features.invextension.ReportProd;
import com.moneydance.modules.features.invextension.TransValuesCum;

/**
 * Generates dump of  intermediate values in 
 * "From/To" Report to allow for auditing
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class CheckRepFromTo {
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;

    public static void main(String[] args) throws Exception {
	BulkSecInfo currentInfo = BulkSecInfoTest.getBaseSecurityInfo();
	ArrayList<RepFromTo> ftReports = getFromToReports(currentInfo,
		fromDateInt, toDateInt);
	for (Iterator<RepFromTo> iterator = ftReports.iterator(); iterator
		.hasNext();) {
	    RepFromTo repFromTo = (RepFromTo) iterator.next();
	    printFromTo(repFromTo);
	}

    }

    public static ArrayList<RepFromTo> getFromToReports(
	    BulkSecInfo currentInfo, int fromDateInt, int toDateInt) {
	ArrayList<RepFromTo> ftData = new ArrayList<RepFromTo>();
	RepFromTo allInvFromTo = new RepFromTo(null, fromDateInt, toDateInt);
	RepFromTo allCashFromTo = new RepFromTo(null, fromDateInt, toDateInt);

	/* loop through investment accounts */
	for (Iterator<Account> it = currentInfo.invSec.keySet().iterator(); it
		.hasNext();) {
	    Account invAcct = (Account) it.next();
	    RepFromTo thisInvFromTo = new RepFromTo(invAcct, fromDateInt,
		    toDateInt);

	    /* loop through securities */
	    for (Iterator<Account> it1 = currentInfo.invSec.get(invAcct)
		    .iterator(); it1.hasNext();) {
		Account secAcct = (Account) it1.next();
		SortedSet<TransValuesCum> transSet = currentInfo.transValuesCumMap
			.get(secAcct);
		RepFromTo thisSecFromTo = new RepFromTo(currentInfo, transSet,
			fromDateInt, toDateInt);
		ftData.add(thisSecFromTo);
		thisInvFromTo = ReportProd.addFT(thisSecFromTo, thisInvFromTo);
	    } // end securities loop
	    // gets aggregated returns for securities in account
	    thisInvFromTo = ReportProd.getFTAggReturns(thisInvFromTo); 
	    thisInvFromTo.setAggType(AGG_TYPE.ACCT_SEC);
	    ftData.add(thisInvFromTo);
	    // add to aggregate securities
	    allInvFromTo = ReportProd.addFT(thisInvFromTo, allInvFromTo); 

	    SortedSet<TransValuesCum> parentSet = currentInfo.transValuesCumMap
		    .get(invAcct); // gets investment account transactions (bank
				   // txns)
	    RepFromTo thisCashFromTo = new RepFromTo(currentInfo, parentSet,
		    fromDateInt, toDateInt); // get report for investment
					     // account
	    thisCashFromTo.setAggType(AGG_TYPE.ACCT_CASH);
	    // add to investment accounts (bank txns)
	    allCashFromTo = ReportProd.addFT(thisCashFromTo, allCashFromTo); 
	    RepFromTo cashReport = ReportProd.getFTCashReturns(thisCashFromTo,
		    thisInvFromTo); // get returns for cash account
	    allCashFromTo.setAggType(AGG_TYPE.ALL_CASH);
	    cashReport.setAggType(AGG_TYPE.ACCT_CASH);
	    ftData.add(cashReport);

	    RepFromTo thisAggRetFromTo = ReportProd.getFTAggRetWCash(
		    thisCashFromTo, thisInvFromTo); // get aggregated returns
						    // with cash accounted for
	    thisAggRetFromTo.setAggType(AGG_TYPE.ACCT_SEC_PLUS_CASH);
	    ftData.add(thisAggRetFromTo);
	} // end investment account loop
	  // get returns for aggregated investment accounts
	allInvFromTo = ReportProd.getFTAggReturns(allInvFromTo); 
	// get aggregated returns from all securities
	allInvFromTo.setAggType(AGG_TYPE.ALL_SEC);
	ftData.add(allInvFromTo);

	RepFromTo allCashReport = ReportProd.getFTCashReturns(allCashFromTo,
		allInvFromTo); // get cash returns for all accounts
	allCashReport.setAggType(AGG_TYPE.ALL_CASH);
	ftData.add(allCashReport);

	RepFromTo allAggRetFromTo = ReportProd.getFTAggRetWCash(allCashFromTo,
		allInvFromTo); // get agg returns w/ cash for all accounts
	allAggRetFromTo.setAggType(AGG_TYPE.ALL_SEC_PLUS_CASH);
	ftData.add(allAggRetFromTo);

	return ftData;

    }

    public static void printFromTo(RepFromTo inFT) {
	String tab = "\u0009";
	System.out.println("Report: From/To" + "\n");
	String acctName = inFT.getAccount() != null ? inFT.getAccount()
		.getAccountName() : "ALL";
	String acctTicker = inFT.getAccount() != null ? inFT.getTicker()
		: "NoTicker";
	String acctAgg = inFT.getAggType() != null ? inFT.getAggType()
		.toString() : "NoAgg";

	System.out.println("Account: " + tab + acctName + tab + "Ticker:" + tab
		+ acctTicker + tab + "AggType: " + tab + acctAgg);
	System.out.println("From: " + tab + inFT.getFromDateInt() + tab
		+ "To: " + tab + inFT.getToDateInt());
	System.out.println("StartPos: " + tab + inFT.getStartPos() + tab
		+ "StartPrice: " + tab + inFT.getStartPrice() + tab
		+ "StartValue:" + tab + inFT.getStartValue());
	System.out.println("EndPos: " + tab + inFT.getEndPos() + tab
		+ "EndPrice: " + tab + inFT.getEndPrice() + tab + "EndValue:"
		+ tab + inFT.getEndValue());
	System.out.println("StartCash: " + tab + inFT.getStartCash() + tab
		+ "EndCash: " + tab + inFT.getEndCash() + tab + "InitBal:"
		+ tab + inFT.getInitBalance());
	System.out.println("Buy: " + tab + inFT.getBuy() + tab + "Sell: " + tab
		+ inFT.getSell() + tab + "Short:" + tab + inFT.getShortSell()
		+ tab + "CoverShort:" + tab + inFT.getCoverShort());
	System.out.println("Income: " + tab + inFT.getIncome() + tab
		+ "Expense: " + tab + inFT.getExpense());
	System.out.println("LongBasis: " + tab + inFT.getLongBasis() + tab
		+ "ShortBasis: " + tab + inFT.getShortBasis());
	System.out.println("RealizedGain: " + tab + inFT.getRealizedGain()
		+ tab + "UnrealizedGain: " + tab + inFT.getUnrealizedGain()
		+ "TotalGain:" + tab + inFT.getTotalGain());
	printPerfMaps(inFT.getArMap(), inFT.getMdMap(), inFT.getTransMap(),
		"Maps");
	System.out.println("\n" + "mdReturn: " + tab + inFT.getMdReturn() + tab
		+ "AnnualReturn: " + tab + inFT.getAnnualPercentReturn());
	System.out.println("\n");

    }

    public static void printPerfMaps(TreeMap<Integer, Double> arMap,
	    TreeMap<Integer, Double> mdMap, TreeMap<Integer, Double> transMap,
	    String msg) {
	String tab = "\u0009";
	String space = "";
	int maxSize = Math.max(arMap == null ? 0 : arMap.size(), Math.max(
		mdMap == null ? 0 : mdMap.size(), transMap == null ? 0
			: transMap.size()));
	System.out.println(msg + "\n");
	System.out.println("arMap" + tab + tab + "mdMap" + tab + tab
		+ "transMap");
	System.out.println("Date" + tab + "Value" + tab + "Date" + tab
		+ "Value" + tab + "Date" + tab + "Value" + tab);

	Integer[] arMapDates = arMap == null ? new Integer[0]
		: returnKeySetArray(arMap);
	Integer[] mdMapDates = mdMap == null ? new Integer[0]
		: returnKeySetArray(mdMap);
	Integer[] transMapDates = transMap == null ? new Integer[0]
		: returnKeySetArray(transMap);

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

    public static Integer[] returnKeySetArray(Map<Integer, Double> map) {
	Integer[] outArray = new Integer[map.size()];
	int i = 0;
	for (Map.Entry<Integer, Double> mapEntry : map.entrySet()) {
	    outArray[i] = mapEntry.getKey();
	    i++;
	}
	return outArray;
    }

    public static String returnMapEle(int dateInt, double val) {
	String tab = "\u0009";
	StringBuilder outLine = new StringBuilder();

	outLine.append(DateUtils.convertToShort(dateInt));
	outLine.append(tab);
	outLine.append(Double.toString(val));
	return outLine.toString();
    }

}
