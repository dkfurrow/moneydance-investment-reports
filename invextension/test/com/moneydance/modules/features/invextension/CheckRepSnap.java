package com.moneydance.modules.features.invextension;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.SortedSet;
import java.util.TreeMap;

import com.moneydance.apps.md.model.Account;
import com.moneydance.modules.features.invextension.BulkSecInfo;
import com.moneydance.modules.features.invextension.BulkSecInfo.AGG_TYPE;
import com.moneydance.modules.features.invextension.DateUtils;
import com.moneydance.modules.features.invextension.RepSnap;
import com.moneydance.modules.features.invextension.ReportProd;
import com.moneydance.modules.features.invextension.TransValuesCum;

/**
 * Generates dump of  intermediate values in 
 * "Snap" Report to allow for auditing
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class CheckRepSnap {
    // private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;

    public static void main(String[] args) throws Exception {
	BulkSecInfo currentInfo = BulkSecInfoTest.getBaseSecurityInfo();
	ArrayList<RepSnap> snapReports = getRepSnapReports(currentInfo,
		toDateInt);
	for (Iterator<RepSnap> iterator = snapReports.iterator(); iterator.hasNext();) {
	    RepSnap repSnap = (RepSnap) iterator.next();
	    printSnap(repSnap);
	}

    }

    public static ArrayList<RepSnap> getRepSnapReports(BulkSecInfo currentInfo,
	    int toDateInt) {
	ArrayList<RepSnap> snapData = new ArrayList<RepSnap>();
	RepSnap allInvSnap = new RepSnap(null, toDateInt);
	RepSnap allCashSnap = new RepSnap(null, toDateInt);

	/* loop through investment accounts */
	for (Iterator<Account> it = currentInfo.invSec.keySet().iterator(); it.hasNext();) {
	    Account invAcct = (Account) it.next();
	    RepSnap thisInvSnap = new RepSnap(invAcct, toDateInt);

	    /* loop through securities */
	    for (Iterator<Account> it1 = currentInfo.invSec.get(invAcct).iterator(); it1
		    .hasNext();) {
		Account secAcct = (Account) it1.next();
		SortedSet<TransValuesCum> transSet = currentInfo.transValuesCumMap
			.get(secAcct);
		RepSnap thisSecSnap = new RepSnap(currentInfo, transSet,
			toDateInt);
		snapData.add(thisSecSnap);
		thisInvSnap = ReportProd.addSnap(thisSecSnap, thisInvSnap);
	    } // end securities loop
	    
	    // gets aggregated returns for securities in account
	    thisInvSnap = ReportProd.getSnapAggReturns(thisInvSnap); 
	    thisInvSnap.setAggType(AGG_TYPE.ACCT_SEC);
	    snapData.add(thisInvSnap);
	    // add to aggregate securities
	    allInvSnap = ReportProd.addSnap(thisInvSnap, allInvSnap); 
	    // gets investment account transactions (bank txns)
	    SortedSet<TransValuesCum> parentSet = currentInfo.transValuesCumMap
		    .get(invAcct); 
	    
	    RepSnap thisCashSnap = new RepSnap(currentInfo, parentSet,
		    toDateInt); // get report for investment account
	    thisCashSnap.setAggType(AGG_TYPE.ACCT_CASH);

	    // add to investment accounts (bank txns)
	    allCashSnap = ReportProd.addSnap(thisCashSnap, allCashSnap); 
	    RepSnap cashReport = ReportProd.getSnapCashReturns(thisCashSnap,
		    thisInvSnap); // get returns for cash account

	    allCashSnap.setAggType(AGG_TYPE.ALL_CASH);
	    cashReport.setAggType(AGG_TYPE.ACCT_CASH);
	    snapData.add(cashReport);

	    RepSnap thisAggRetSnap = ReportProd.getSnapAggRetWCash(
		    thisCashSnap, thisInvSnap); // get aggregated returns with
						// cash accounted for
	    thisAggRetSnap.setAggType(AGG_TYPE.ACCT_SEC_PLUS_CASH);
	    snapData.add(thisAggRetSnap);
	} // end investment account loop
	  // get returns for aggregated investment accounts
	allInvSnap = ReportProd.getSnapAggReturns(allInvSnap);
	// get aggregated returns from all securities
	allInvSnap.setAggType(AGG_TYPE.ALL_SEC);
	snapData.add(allInvSnap);

	RepSnap allCashReport = ReportProd.getSnapCashReturns(allCashSnap,
		allInvSnap); // get cash returns for all accounts
	allCashReport.setAggType(AGG_TYPE.ALL_CASH);
	snapData.add(allCashReport);

	RepSnap allAggRetSnap = ReportProd.getSnapAggRetWCash(allCashSnap,
		allInvSnap); // get agg returns w/ cash for all accounts
	allAggRetSnap.setAggType(AGG_TYPE.ALL_SEC_PLUS_CASH);
	snapData.add(allAggRetSnap);

	return snapData;

    }

    public static void printSnap(RepSnap inSnap) {
	String tab = "\u0009";
	System.out.println("\n" + "Report: Snap" + "\n");
	String acctName = inSnap.getAccount() != null ? inSnap.getAccount()
		.getAccountName() : "ALL";
	String acctTicker = inSnap.getAccount() != null ? inSnap.getTicker()
		: "NoTicker";
	String acctAgg = inSnap.getAggType() != null ? inSnap.getAggType()
		.toString() : "NoAgg";

	System.out.println("Account: " + tab + acctName + tab + "Ticker:" + tab
		+ acctTicker + tab + "AggType: " + tab + acctAgg);
	System.out.println("Snapshot Date: " + tab + inSnap.getSnapDateInt());
	printRetDateMap(inSnap.getRetDateMap(), "Return Dates");
	printInputMap(inSnap.getStartPoses(), "Start Positions");
	printInputMap(inSnap.getStartPrices(), "Start Prices");
	printInputMap(inSnap.getStartValues(), "Start Values");

	System.out.println("EndPos: " + tab + inSnap.getEndPos() + tab
		+ "EndPrice: " + tab + inSnap.getLastPrice() + tab
		+ "EndValue:" + tab + inSnap.getEndValue());

	printInputMap(inSnap.getStartCashs(), "Starting Cash Positions");
	System.out.println("EndCash: " + tab + inSnap.getEndCash());

	printInputMap(inSnap.getIncomes(), "Income Amounts");
	printInputMap(inSnap.getExpenses(), "Expense Amounts");

	System.out.println("All Maps Follow: \n");
	printAllPerfMaps(inSnap.getMdMap(), inSnap.getArMap(),
		inSnap.getTransMap());

	System.out.println("Returns: \n");
	System.out.println("1-Day Ret: " + tab + inSnap.getTotRet1Day() + tab
		+ "1-Wk Ret: " + tab + inSnap.getTotRetWk() + tab
		+ "4-Wk Ret: " + tab + inSnap.getTotRet4Wk() + tab
		+ "3-Mnth Ret: " + tab + inSnap.getTotRet3Mnth() + tab
		+ "1-Yr Ret: " + tab + inSnap.getTotRetYear() + tab
		+ "3-Yr Ret: " + tab + inSnap.getTotRet3year() + tab
		+ "YTD Ret: " + tab + inSnap.getTotRetYTD() + tab + "All Ret: "
		+ tab + inSnap.getTotRetAll() + tab + "All AnnRet: " + tab
		+ inSnap.getAnnRetAll() + tab);
    }

    public static void printRetDateMap(LinkedHashMap<String, Integer> inMap,
	    String msg) {
	StringBuilder outStr = new StringBuilder();
	String tab = "\u0009";
	outStr.append(msg + "\n");
	String[] retCats = { "PREV", "1Wk", "4Wk", "3Mnth", "1Yr", "3Yr",
		"YTD", "All" };
	for (String retCat : retCats) {
	    Integer value = inMap.get(retCat) == null ? 0 : inMap.get(retCat);
	    String dateStr = value == 0 ? "N/A" : DateUtils
		    .convertToShort(value);
	    outStr.append(retCat).append(tab).append(dateStr).append(tab);
	}
	System.out.println(outStr.toString());
    }

    public static void printInputMap(LinkedHashMap<String, Double> inMap,
	    String msg) {
	StringBuilder outStr = new StringBuilder();
	String tab = "\u0009";
	String[] retCats = { "PREV", "1Wk", "4Wk", "3Mnth", "1Yr", "3Yr",
		"YTD", "All" };
	outStr.append(msg + "\n");
	for (String retCat : retCats) {
	    Double value = inMap.get(retCat) == null ? Double.NaN : inMap
		    .get(retCat);
	    outStr.append(retCat).append(tab).append(value.toString())
		    .append(tab);
	}
	System.out.println(outStr.toString());
    }

    public static void printAllPerfMaps(
	    LinkedHashMap<String, TreeMap<Integer, Double>> mdMaps,
	    LinkedHashMap<String, TreeMap<Integer, Double>> arMaps,
	    LinkedHashMap<String, TreeMap<Integer, Double>> transMaps) {
	String[] retCats = { "PREV", "1Wk", "4Wk", "3Mnth", "1Yr", "3Yr",
		"YTD", "All" };
	for (String retCat : retCats) {
	    TreeMap<Integer, Double> mdMap = (mdMaps.get(retCat) == null ? 
		    new TreeMap<Integer, Double>() : mdMaps.get(retCat));
	    TreeMap<Integer, Double> arMap = (arMaps.get(retCat) == null ? 
		    new TreeMap<Integer, Double>() : arMaps.get(retCat));
	    TreeMap<Integer, Double> transMap = (transMaps.get(retCat) == null ?
		    new TreeMap<Integer, Double>() : transMaps.get(retCat));
	    CheckRepFromTo.printPerfMaps(arMap, mdMap, transMap, "\n"
		    + "Maps: " + retCat);
	}

    }

}
