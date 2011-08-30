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

public class BulkSecInfoTest {
    private static class TransLine implements Comparable<TransLine> {
	String[] row;

	private TransLine(String[] inputArray) {
	    this.row = inputArray;
	}

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
	    Integer cIntComp = Integer.parseInt(t.getRow()[2]);
	    Integer cIntThis = Integer.parseInt(this.getRow()[2]);
	    return cIntThis.compareTo(cIntComp);

	}

	private String[] getRow() {
	    return this.row;
	}

    }

    public static final File mdTestFile = new File("./resources/testMD01.md");
    public static final File transBaseFile = new File(
	    "./resources/transActivityReport.csv");
    // the following two elements determine precision of testing
    public static final int precCompare = 10;

    public static final boolean limitPrecision = true; /*
						        * limits precision to
						        * minimum digits of file
						        * or report output
						        */

    public static BulkSecInfo getBaseSecurityInfo() throws Exception {
	RootAccount root = FileUtils.readAccountsFromFile(mdTestFile, null);
	BulkSecInfo currentInfo = new BulkSecInfo(root);
	return currentInfo;
    }

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

    public static boolean isNumeric(String inString) {
	Pattern pattern = Pattern.compile("^\\-?[0-9]{1,3}(\\,[0-9]{3})*"
		+ "(\\.[0-9]+)?$|^[0-9]+(\\.[0-9]+)?$");
	Pattern pattern1 = Pattern
		.compile("(^N/A$)|(^[-]?(\\d+)(\\.\\d{0,3})?$)|(^[-]?"
			+ "(\\d{1,3},(\\d{3},)*\\d{3}(\\.\\d{1,3})?|\\d{1,3}(\\.\\d{1,3})?)$)");
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

    public static boolean similarElements(String s1, String s2, int decPlaces,
	    boolean limitPrecision) {
	if (isNumeric(s1) && isNumeric(s2)) {
	    double d1 = Double.parseDouble(s1);
	    double d2 = Double.parseDouble(s2);
	    int lowDigits = Math.min(getPrecisionDigits(d1),
		    getPrecisionDigits(d2));
	    double threshold = Math.pow(10,
		    limitPrecision ? -Math.min(decPlaces, lowDigits)
			    : -decPlaces);
	    return Math.abs(d1 - d2) < threshold;

	} else { // string elements
	    return s1.equals(s2);
	}
    }

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

    private static ArrayList<TransLine> readStringArrayIntoTransLine(
	    BulkSecInfo currentInfo) {
	ArrayList<String[]> transActivityReport = currentInfo
		.listTransValuesCumMap(currentInfo.transValuesCumMap);
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

    private static boolean testTransactions(BulkSecInfo currentInfo) {
	boolean errorFound = false;
	ArrayList<TransLine> transBase = readCSVIntoTransLine(transBaseFile);
	ArrayList<TransLine> transTest = readStringArrayIntoTransLine(currentInfo);
	errorFound = compareTransactions(transTest, transBase, precCompare);
	return errorFound;
    }

    @Test
    public void testListTransValuesCumMap() throws Exception {
	boolean errorFound = false;
	BulkSecInfo currentInfo = getBaseSecurityInfo();
	if (testTransactions(currentInfo))
	    errorFound = true;
	String msg = errorFound ? " -- Errors Found!" : " -- No Errors Found";
	System.out.println("Finished TransValuesCumMap Test " + msg);
	assertFalse(errorFound);
    }

}
