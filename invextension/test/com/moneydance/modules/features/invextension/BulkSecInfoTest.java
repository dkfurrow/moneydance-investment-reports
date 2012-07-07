/* BulkSecInfoTest.java
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import com.moneydance.apps.md.controller.io.FileUtils;
import com.moneydance.apps.md.model.RootAccount;

/**
 * Class which compares transaction report lines (i.e. allows for sorting of
 * transaction report lines, and compares generated report to stored report)
 * 
 * Version 1.0
 * 
 * @author Dale Furrow
 * 
 */
public class BulkSecInfoTest {
  //Stored Test Database
    public static final File mdTestFile = new File("./resources/testMD02.md");
    //stored csv file of transaction activity report (average cost)
    public static final File transBaseFileAvgCost = new File(
	    "./resources/transActivityReportAvgCost.csv");
  //stored csv file of transaction activity report (lot matching)
    public static final File transBaseFileLotMatch = new File(
	    "./resources/transActivityReportLotMatch.csv");
    // the following two elements determine precision of testing
    // decimal places for comparison
    public static final int precCompare = 10;
    // limits precision to minimum digits of file or report output
    // to generate a failed test, set to false--generated report and stored
    // report have different decimal place precisions
    public static final boolean limitPrecision = true; 
    /**
     * Class with only one element, String array of transaction report
     * elements.  Implements comparable based on transaction id 
     * so that generated report and stored report can be sorted and compared.
     *
     * Version 1.0 
     * @author Dale Furrow
     *
     */
    private static class TransLine implements Comparable<TransLine> {
	String[] row;

	private TransLine(String[] inputArray) {
	    this.row = inputArray;
	}

	/**Compares two transaction lines, based on 
	 * decimal place threshold.
	 * @param compRpt
	 * @param baseRpt
	 * @param decPlaces
	 * @param limitPrecision
	 * @return
	 */
	private static boolean compareTransLines(TransLine compRpt,
		TransLine baseRpt, int decPlaces, boolean limitPrecision) {
	    boolean errorFound = false;
	    for (int i = 0; i < compRpt.row.length; i++) {
		String compStr = compRpt.getRow()[i];
		String baseStr = baseRpt.getRow()[i];
		if (!similarElements(compStr, baseStr, decPlaces,
			limitPrecision)) {
		    printErrorMessage(compRpt, baseRpt, i);
		    errorFound = true;
		}
	    }
	    if (!errorFound) {
		String info = "Account: " + compRpt.getRow()[0] + " Security: "
			+ compRpt.getRow()[1] + " Transaction: "
			+ compRpt.getRow()[2];
		System.out.println("Tested and Passed: " + info);
	    }
	    return errorFound;

	}

	private static void printErrorMessage(TransLine compRpt,
		TransLine baseRpt, int i) {
	    System.out.println("Error at " + i + " member of report line"
		    + "-- Acct: " + compRpt.row[0] + " Security: "
		    + compRpt.row[1] + " Transaction: " + compRpt.row[2]
		    + " Test = " + compRpt.getRow()[i] + " Should = "
		    + baseRpt.row[i]);
	}

	@Override
	public int compareTo(TransLine t) {
	    Double cDoubleComp = Double.parseDouble(t.getRow()[2]);
	    Double cDoubleThis = Double.parseDouble(this.getRow()[2]);
	    return cDoubleThis.compareTo(cDoubleComp);

	}

	private String[] getRow() {
	    return this.row;
	}

    }

    /**
     * gets BulkSecInfo from stored moneydance data file (avg cost basis)
     * @return
     * @throws Exception
     */
    public static BulkSecInfo getBaseSecurityInfoAvgCost() throws Exception {
	RootAccount root = FileUtils.readAccountsFromFile(mdTestFile, null);
	BulkSecInfo currentInfo = new BulkSecInfo(root, new GainsAverageCalc());
	return currentInfo;
    }
    
    /** gets BulkSecInfo from stored moneydance data file (Lot Matching basis)
    * @return
    * @throws Exception
    */
   public static BulkSecInfo getBaseSecurityInfoLotMatch() throws Exception {
	RootAccount root = FileUtils.readAccountsFromFile(mdTestFile, null);
	BulkSecInfo currentInfo = new BulkSecInfo(root, new GainsLotMatchCalc());
	return currentInfo;
   }

    /** returns number of digits in double, either in basic or exponential
     * format
     * @param d input double
     * @return number of digits in double
     */
    public static int getPrecisionDigits(double d) {
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

    /** Determines if string is numeric based on regex test
     * @param inString
     * @return true if numeric
     */
    public static boolean isNumeric(String inString) {
//	Pattern pattern = Pattern.compile("^\\-?[0-9]{1,3}(\\,[0-9]{3})*"
//		+ "(\\.[0-9]+)?$|^[0-9]+(\\.[0-9]+)?$");
//	Pattern pattern1 = Pattern
//		.compile("(^N/A$)|(^[-]?(\\d+)(\\.\\d{0,3})?$)|(^[-]?"
//			+ "(\\d{1,3},(\\d{3},)*\\d{3}(\\.\\d{1,3})?|\\d{1,3}(\\.\\d{1,3})?)$)");
	Pattern pattern2 = Pattern.compile("^(?:(?:[+\\-]?\\$?)|(?:\\$?[+\\-]"
		+ "?))?(?:(?:\\d{1,3}(?:(?:,\\d{3})|(?:\\d))*"
		+ "(?:\\.(?:\\d*|\\d+[eE][+\\-]\\d+))?)|"
		+ "(?:\\.\\d+(?:[eE][+\\-]\\d+)?))$");
	// pattern1 includes "N/A"
	// pattern2 includes exponents
	// , Pattern.CASE_INSENSITIVE); // can modify to add CASE_INSENSITVE
	// method
	Matcher matcher = pattern2.matcher(inString);
	return matcher.find();
    }

    /**Compares two strings--if not numeric, returns string comparison.
     * If numeric, determines whether numbers are within specified 
     * difference of each other.
     * @param s1
     * @param s2
     * @param decPlaces
     * @param limitPrecision
     * @return
     */
    public static boolean similarElements(String s1, String s2, int decPlaces,
	    boolean limitPrecision) {
	if (isNumeric(s1) && isNumeric(s2)) { //numeric elements
	    double d1 = Double.parseDouble(s1);
	    double d2 = Double.parseDouble(s2);
	    int lowDigits = Math.min(getPrecisionDigits(d1),
		    getPrecisionDigits(d2)); //gets least number of digits after
	    //decimal place
	    double threshold = Math.pow(10,
		    limitPrecision ? -Math.min(decPlaces, lowDigits)
			    : -decPlaces);
	    return Math.abs(d1 - d2) < threshold;

	} else { // string elements
	    return s1.equals(s2);
	}
    }

    /**Compares ArrayLists of transaction lines, returns true if error is found
     * @param compRpt
     * @param baseRpt
     * @param decPlaces
     * @return true if error found
     */
    private static boolean compareTransactions(ArrayList<TransLine> compRpt,
	    ArrayList<TransLine> baseRpt, int decPlaces) {
	boolean errorFound = false;
	System.out.println("Comparing Transactions-- ");
	for (int i = 0; i < compRpt.size(); i++) {
	    TransLine compLine = compRpt.get(i);
	    TransLine baseLine = baseRpt.get(i);
	    if (TransLine.compareTransLines(compLine, baseLine, decPlaces,
		    limitPrecision))
		errorFound = true;
	}
	String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
	System.out.println("Finished Compare of Transaction Data" + msg + "\n");
	return errorFound;
    }

    /**Reads stored file, places data into sorted array of TransLine objects
     * @param readFile
     * @return
     */
    private static ArrayList<TransLine> readCSVIntoTransLine(File readFile) {
	ArrayList<String[]> inputStrAL = IOUtils.readCSVIntoArrayList(readFile);
	inputStrAL.remove(0); // remove header row
	ArrayList<TransLine> outputTransAL = new ArrayList<TransLine>();
	for (Iterator<String[]> iterator = inputStrAL.iterator(); iterator
		.hasNext();) {
	    String[] inLine = iterator.next();
	    TransLine outLine = new TransLine(inLine);
	    outputTransAL.add(outLine);
	}
	Collections.sort(outputTransAL);
	return outputTransAL;
    }

    /**Generates transaction report from stored md file, places data into 
     * sorted array of TransLine objects
     * @param currentInfo
     * @return
     * @throws Exception 
     */
    private static ArrayList<TransLine> readStringArrayIntoTransLine(
	    BulkSecInfo currentInfo) throws Exception {
	ArrayList<String[]> transActivityReport = currentInfo
		.listTransValuesSet(currentInfo.getInvestmentWrappers());
	ArrayList<TransLine> outputTransAL = new ArrayList<TransLine>();
	for (Iterator<String[]> iterator = transActivityReport.iterator(); iterator
		.hasNext();) {
	    String[] row = iterator.next();
	    TransLine line = new TransLine(row);
	    outputTransAL.add(line);
	}
	Collections.sort(outputTransAL);
	return outputTransAL;
    }


    /**
     * Test Method which compares TransValuesCum generated from test database
     * with stored report. (Average Cost)
     * @throws Exception
     */
    @Test
    public void testListTransValuesCumMapAvgCost()  {
	boolean errorFound = false;
	ArrayList<TransLine> transBase = null;
	ArrayList<TransLine> transTest = null;
	try {
	    BulkSecInfo currentInfo = getBaseSecurityInfoAvgCost();
	    transBase = readCSVIntoTransLine(transBaseFileAvgCost);
	    transTest = readStringArrayIntoTransLine(currentInfo);
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	if (compareTransactions(transTest, transBase, precCompare))
	    errorFound = true;
	String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
	System.out.println("Finished TransValuesCumMap Test for Average Cost " + msg);
	assertFalse(errorFound);
    }
    
    /* Test Method which compares TransValuesCum generated from test database
    * with stored report. (Lot Matching)
    * @throws Exception
    */
   @Test
   public void testListTransValuesCumMapLotMatch()  {
	boolean errorFound = false;
	ArrayList<TransLine> transBase = null;
	ArrayList<TransLine> transTest = null;
	try {
	    BulkSecInfo currentInfo = getBaseSecurityInfoLotMatch();
	    transBase = readCSVIntoTransLine(transBaseFileLotMatch);
	    transTest = readStringArrayIntoTransLine(currentInfo);
	} catch (Exception e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	if (compareTransactions(transTest, transBase, precCompare))
	    errorFound = true;
	String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
	System.out.println("Finished TransValuesCumMap Test for Lot Match " + msg);
	assertFalse(errorFound);
   }

}
