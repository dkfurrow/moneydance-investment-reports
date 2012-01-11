/* ReportProdTest.java
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

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.SortedSet;

import org.junit.Before;
import org.junit.Test;

import com.moneydance.apps.md.model.Account;

/**
 * Generates 3 tests:
 * (1) Compares generated "From/To" report from stored MD file to base version
 * saved in CSV Form.
 * (2) Compares generated "Snapshot" report from stored MD file to base version
 * saved in CSV Form.
 * (3) Compares generated "SnapShot" report to iterated "From/To" report, to
 * ensure consistency between generated data between the two reports
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class ReportProdTest {
    /**
     * Class with only one element, String array of transaction report
     * elements.  Implements comparable based on investment account and security 
     * so that generated report and stored report can be sorted and compared.
     *
     * Version 1.0 
     * @author Dale Furrow
     *
     */
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

	/**Compares two report lines, based on 
	 * decimal place threshold.
	 * @param compRpt
	 * @param baseRpt
	 * @param decPlaces
	 * @param limitPrecision
	 * @return
	 */
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

	/* Compares based on concatenation of investment name
	 * and security name
	 * 
	 */
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
    // Report Dates for comparison
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    //Stored CSV Files
    public static final File ftBaseFile = new File("./resources/ft20100601.csv");
    public static final File snapBaseFile = new File(
	    "./resources/snap20100601.csv");
    

    /**returns minimum transaction date for all transactions in md file
     * @param currentInfo
     * @return
     */
    public static int getMinTransDate(BulkSecInfo currentInfo) {
	int minDateInt = Integer.MAX_VALUE;

	for (Iterator<Account> it = currentInfo.transValuesCumMap.keySet()
		.iterator(); it.hasNext();) {
	    Account account = (Account) it.next();
	    
	    SortedSet<TransValuesCum> tvcSet = currentInfo.transValuesCumMap
		    .get(account);
	    // TransValuesCum sorts by Date, so first element is earliest
	    minDateInt = Math.min(tvcSet.first().getTransValues().getDateint(),
		    minDateInt);
	}

	return minDateInt;
    }

    /**Reads object array into reportLine object
     * @param inObj
     * @return
     */
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

    /**Compares ArrayLists of report lines, returns true if error is found
     * @param compRpt
     * @param baseRpt
     * @param decPlaces
     * @return true if error found
     */
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

    /**Generates Return Date Map for 9 standardized return dates
     *  (same as code in snap report)
     * @param currentInfo
     * @return
     */
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

    /**Reads stored file, places data into sorted array of ReportLine objects
     * @param readFile
     * @return
     */
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

    /**Test column in snap report against column in "From/To" Report
     * to check for consistency
     * @param snapTest
     * @param ftTest
     * @param snapCol
     * @param ftCol
     * @return
     */
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

    /**Tests "From/To" Report generated from MD File 
     * against saved version in CSV File
     */
    @Test
    public void testGetFromToReport() {
	boolean errorFound = false;
	Object[][] ftObj = ReportProd.getFromToReportObjs(currentInfo, fromDateInt,
		toDateInt);
	ArrayList<ReportLine> ftTest = readObjArrayIntoRptLine(ftObj);
	ArrayList<ReportLine> ftBase = readCSVIntoRptLine(ftBaseFile);
	errorFound = compareRpts("From/To Report", ftTest, ftBase,
		BulkSecInfoTest.precCompare);

	String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
	System.out.println("Finished From/To Report Test " + msg);

	assertFalse(errorFound);
    }

    /**Tests "Snap" Report generated from MD File 
     * against saved version in CSV File
     */
    @Test
    public void testGetSnapReport() {
	boolean errorFound = false;
	Object[][] snapObj = ReportProd.getSnapReportObj(currentInfo, toDateInt);
	ArrayList<ReportLine> snapTest = readObjArrayIntoRptLine(snapObj);
	ArrayList<ReportLine> snapBase = readCSVIntoRptLine(snapBaseFile);
	errorFound = compareRpts("Snapshot Report", snapTest, snapBase,
		BulkSecInfoTest.precCompare);

	String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
	System.out.println("Finished Snap Report Test " + msg);

	assertFalse(errorFound);

    }


    /**Tests "Snap" Report generated from MD File against iterations of 
     * "From/To" Reports, to ensure that reports are consistent     * 
     */
    @Test
    public void testRepSnapAgainstFT() {

	boolean errorFound = false;
	LinkedHashMap<String, Integer> retDateMap = getRetDateMap(currentInfo);
	Object[][] snapObj = ReportProd.getSnapReportObj(currentInfo, toDateInt);
	ArrayList<ReportLine> snapTest = readObjArrayIntoRptLine(snapObj);

	// print out Return Dates for the various return categories for reference
	for (Iterator<String> iterator = retDateMap.keySet().iterator(); iterator
		.hasNext();) {
	    String retCat = (String) iterator.next();
	    int dateInt = retDateMap.get(retCat);
	    System.out.println("Period: " + retCat + " Date: "
		    + DateUtils.convertToShort(dateInt));

	}
	
	//Iterate through start dates in Returns Date Map, generate associated
	//"From/To" Report, compare appropriate column in "Snap" Report with like
	// column in "From/To" Report
	for (Iterator<String> iterator = retDateMap.keySet().iterator(); iterator
		.hasNext();) {
	    String retCat = (String) iterator.next();
	    int dateInt = retDateMap.get(retCat);
	    Object[][] ftObj = ReportProd.getFromToReportObjs(currentInfo, dateInt,
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
