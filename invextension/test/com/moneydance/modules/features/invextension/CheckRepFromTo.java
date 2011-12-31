/* CheckRepFromTo.java
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
	ArrayList<RepFromTo> ftReports = ReportProd.getFromToReports(currentInfo,
		fromDateInt, toDateInt);
	for (Iterator<RepFromTo> iterator = ftReports.iterator(); iterator
		.hasNext();) {
	    RepFromTo repFromTo = (RepFromTo) iterator.next();
	    printFromTo(repFromTo);
	}

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
