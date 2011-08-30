package com.moneydance.modules.features.invextension;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.SortedSet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.moneydance.apps.md.model.Account;

public class ReportProdTest {
    public static class ReportLine implements Comparable<ReportLine> {
	private String[] row;

	private ReportLine(Object[] inputObj) {
	    String[] convArray = new String[inputObj.length];
	    for (int i = 0; i < inputObj.length; i++) {
		Object obj = inputObj[i];
		if (obj instanceof String) {
		    convArray[i] = (String) obj;
		} else if (obj instanceof Number) {
		    convArray[i] = obj.toString();
		} else {
		    convArray[i] = null;
		}
	    }
	    this.row = convArray;
	}

	private ReportLine(String[] inputArray) {
	    this.row = inputArray;
	}

	private static boolean compareRptLines(ReportLine compRpt,
		ReportLine baseRpt, int decPlaces, boolean limitPrecision) {
	    boolean errorFound = false;
	    for (int i = 0; i < compRpt.getRow().length; i++) {
		String compStr = compRpt.getRow()[i];
		String baseStr = baseRpt.getRow()[i];
		if (!BulkSecInfoTest.similarElements(compStr, baseStr,
			decPlaces, limitPrecision)) {
		    printErrorMessage(compRpt, baseRpt, i);
		    errorFound = true;
		}
	    }
	    if (!errorFound) {
		String info = "Account: " + compRpt.getRow()[0] + " Security: "
			+ compRpt.getRow()[1];
		System.out.println("Tested and Passed: " + info);
	    }
	    return errorFound;
	}

	private static void printErrorMessage(ReportLine compRpt,
		ReportLine baseRpt, int i) {
	    System.out.println("Error at " + i + " member of report line"
		    + "-- Acct: " + compRpt.getRow()[0] + " Security: "
		    + compRpt.getRow()[1] + " Test = " + compRpt.getRow()[i]
		    + " Should = " + baseRpt.getRow()[i]);
	}

	@Override
	public int compareTo(ReportLine r) {
	    String cStrComp = r.getRow()[0] + r.getRow()[1];
	    String cStrThis = this.getRow()[0] + this.getRow()[1];
	    return cStrThis.compareTo(cStrComp);

	}

	public String[] getRow() {
	    return this.row;
	}
    }

    private static BulkSecInfo currentInfo;
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final File ftBaseFile = new File("./resources/ft20100601.csv");
    public static final File snapBaseFile = new File(
	    "./resources/snap20100601.csv");

    public static int getMinTransDate(BulkSecInfo currentInfo) {
	int minDateInt = Integer.MAX_VALUE;

	for (Iterator<Account> it = currentInfo.transValuesCumMap.keySet()
		.iterator(); it.hasNext();) {
	    Account account = (Account) it.next();
	    SortedSet<TransValuesCum> tvcSet = currentInfo.transValuesCumMap
		    .get(account);
	    for (Iterator<TransValuesCum> it1 = tvcSet.iterator(); it1
		    .hasNext();) {
		TransValuesCum transValuesCum = (TransValuesCum) it1.next();
		minDateInt = Math.min(minDateInt, transValuesCum
			.getTransValues().getDateint());
	    }
	}

	return minDateInt;
    }

    public static ArrayList<ReportLine> readObjArrayIntoRptLine(Object[][] inObj) {
	ArrayList<ReportLine> outputRptAL = new ArrayList<ReportLine>();
	for (int i = 0; i < inObj.length; i++) {
	    Object[] inLine = inObj[i];
	    ReportLine outLine = new ReportLine(inLine);
	    outputRptAL.add(outLine);
	}
	Collections.sort(outputRptAL);
	return outputRptAL;
    }

    private static boolean compareRpts(String info,
	    ArrayList<ReportLine> compRpt, ArrayList<ReportLine> baseRpt,
	    int decPlaces) {
	boolean errorFound = false;
	System.out.println("Comparing Report-- " + info);
	for (int i = 0; i < compRpt.size(); i++) {
	    ReportLine compLine = compRpt.get(i);
	    ReportLine baseLine = baseRpt.get(i);
	    if (ReportLine.compareRptLines(compLine, baseLine, decPlaces,
		    BulkSecInfoTest.limitPrecision))
		errorFound = true;
	}
	String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
	System.out.println("Finished Compare of " + info + msg + "\n");
	return errorFound;
    }

    private static LinkedHashMap<String, Integer> getRetDateMap(
	    BulkSecInfo currentInfo) {

	LinkedHashMap<String, Integer> retDateMap = new LinkedHashMap<String, Integer>();
	int firstDateInt = getMinTransDate(currentInfo);
	int fromDateInt = DateUtils.getPrevBusinessDay(firstDateInt);
	int prevFromDateInt = DateUtils.getPrevBusinessDay(toDateInt);
	int wkFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
		.addDaysInt(toDateInt, -7));
	int mnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
		.addMonthsInt(toDateInt, -1));
	int threeMnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
		.addMonthsInt(toDateInt, -3));
	int oneYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
		.addMonthsInt(toDateInt, -12));
	int threeYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
		.addMonthsInt(toDateInt, -36));
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

    private static ArrayList<ReportLine> readCSVIntoRptLine(File readFile) {
	ArrayList<String[]> inputStrAL = IOUtils.readCSVIntoArrayList(readFile);
	inputStrAL.remove(0); // remove header row
	ArrayList<ReportLine> outputRptAL = new ArrayList<ReportLine>();
	for (Iterator<String[]> iterator = inputStrAL.iterator(); iterator
		.hasNext();) {
	    String[] inLine = iterator.next();
	    ReportLine outLine = new ReportLine(inLine);
	    outputRptAL.add(outLine);
	}
	Collections.sort(outputRptAL);
	return outputRptAL;
    }

    private static boolean testRepSnapCol(ArrayList<ReportLine> snapTest,
	    ArrayList<ReportLine> ftTest, int snapCol, int ftCol) {
	boolean errorFound = false;
	for (int i = 0; i < snapTest.size(); i++) {
	    String snapLineEle = snapTest.get(i).getRow()[snapCol];
	    String ftLineEle = ftTest.get(i).getRow()[ftCol];
	    if (!BulkSecInfoTest
		    .similarElements(snapLineEle, ftLineEle,
			    BulkSecInfoTest.precCompare,
			    BulkSecInfoTest.limitPrecision)) {
		System.out.println("SnapReport: Row " + i + " Column "
			+ snapCol + " ERROR!" + "Element = " + snapLineEle
			+ "Should = " + ftLineEle);
		errorFound = true;

	    } else {
		System.out.println("SnapReport: Row " + i + " Column "
			+ snapCol + " Passed");
	    }
	}
	return errorFound;

    }

    @Before
    public void setUp() throws Exception {
	currentInfo = BulkSecInfoTest.getBaseSecurityInfo();
    }

    @Test
    public void testGetFromToReport() {
	boolean errorFound = false;
	Object[][] ftObj = ReportProd.getFromToReport(currentInfo, fromDateInt,
		toDateInt);
	ArrayList<ReportLine> ftTest = readObjArrayIntoRptLine(ftObj);
	ArrayList<ReportLine> ftBase = readCSVIntoRptLine(ftBaseFile);
	errorFound = compareRpts("From/To Report", ftTest, ftBase,
		BulkSecInfoTest.precCompare);

	String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
	System.out.println("Finished From/To Report Test " + msg);

	assertFalse(errorFound);
    }

    @Test
    public void testGetSnapReport() {
	boolean errorFound = false;
	Object[][] snapObj = ReportProd.getSnapReport(currentInfo, toDateInt);
	ArrayList<ReportLine> snapTest = readObjArrayIntoRptLine(snapObj);
	ArrayList<ReportLine> snapBase = readCSVIntoRptLine(snapBaseFile);
	errorFound = compareRpts("Snapshot Report", snapTest, snapBase,
		BulkSecInfoTest.precCompare);

	String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
	System.out.println("Finished Snap Report Test " + msg);

	assertFalse(errorFound);

    }

    @Test
    public void testRepSnapAgainstFT() {

	boolean errorFound = false;
	LinkedHashMap<String, Integer> retDateMap = getRetDateMap(currentInfo);
	Object[][] snapObj = ReportProd.getSnapReport(currentInfo, toDateInt);
	ArrayList<ReportLine> snapTest = readObjArrayIntoRptLine(snapObj);

	for (Iterator iterator = retDateMap.keySet().iterator(); iterator
		.hasNext();) {
	    String retCat = (String) iterator.next();
	    int dateInt = retDateMap.get(retCat);
	    System.out.println("Period: " + retCat + " Date: "
		    + DateUtils.convertToShort(dateInt));

	}

	for (Iterator iterator = retDateMap.keySet().iterator(); iterator
		.hasNext();) {
	    String retCat = (String) iterator.next();
	    int dateInt = retDateMap.get(retCat);
	    Object[][] ftObj = ReportProd.getFromToReport(currentInfo, dateInt,
		    toDateInt);
	    ArrayList<ReportLine> ftTest = readObjArrayIntoRptLine(ftObj);
	    if (retCat.equals("PREV")) {
		if (testRepSnapCol(snapTest, ftTest, 3, 6))
		    errorFound = true; // Last Price
		if (testRepSnapCol(snapTest, ftTest, 4, 4))
		    errorFound = true; // End Pos
		if (testRepSnapCol(snapTest, ftTest, 5, 8))
		    ; // End value
		if (testRepSnapCol(snapTest, ftTest, 9, 20))
		    ; // MD returns

	    } else if (retCat.equals("1Wk")) {
		if (testRepSnapCol(snapTest, ftTest, 10, 20))
		    ; // MD returns

	    } else if (retCat.equals("4Wk")) {
		if (testRepSnapCol(snapTest, ftTest, 11, 20))
		    errorFound = true; // MD returns

	    } else if (retCat.equals("3Mnth")) {
		if (testRepSnapCol(snapTest, ftTest, 12, 20))
		    errorFound = true; // MD returns

	    } else if (retCat.equals("1Yr")) {
		if (testRepSnapCol(snapTest, ftTest, 14, 20))
		    errorFound = true; // MD returns

	    } else if (retCat.equals("3Yr")) {
		if (testRepSnapCol(snapTest, ftTest, 15, 20))
		    errorFound = true; // MD returns

	    } else if (retCat.equals("YTD")) {
		if (testRepSnapCol(snapTest, ftTest, 13, 20))
		    errorFound = true; // MD returns

	    } else if (retCat.equals("All")) {
		if (testRepSnapCol(snapTest, ftTest, 16, 20))
		    errorFound = true; // MD returns
		if (testRepSnapCol(snapTest, ftTest, 17, 21))
		    errorFound = true; // Ann returns
		if (testRepSnapCol(snapTest, ftTest, 19, 13))
		    errorFound = true; // Income
		if (testRepSnapCol(snapTest, ftTest, 20, 19))
		    errorFound = true; // Total Gain

	    }

	}

	String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
	System.out.println("Finished Consistency Test of RepSnap to RepFT "
		+ msg);

	assertFalse(errorFound);

    }

}
