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
    public static final Class<InvestmentAccountWrapper> invAggClass = InvestmentAccountWrapper.class;
    public static final Class<Tradeable> tradeableAggClass = Tradeable.class;
    public static final boolean catHierarchy = false;
    public static final boolean rptOutputSingle = false;

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void main(String[] args) throws Exception {
	BulkSecInfo currentInfo = BulkSecInfoTest.getBaseSecurityInfoAvgCost();
	
	TotalFromToReport fromToReport
        = new TotalFromToReport(currentInfo, invAggClass,
		tradeableAggClass, catHierarchy, rptOutputSingle, fromDateInt,
		toDateInt);
	ArrayList<ComponentReport> componentReports = fromToReport.getReports();
	for (Iterator<ComponentReport> iterator = componentReports.iterator(); iterator
		.hasNext();) {
	    ComponentReport componentReport = iterator.next();
	    printFromTo(componentReport);
	}

    }

    

    public static void printFromTo(ComponentReport componentReport) throws Exception {
	SecurityFromToReport reportLine = null;
	CompositeReport< ?, ?> compositeReport = null;
	
	if(componentReport instanceof SecurityFromToReport){
	    reportLine = (SecurityFromToReport) componentReport;
	} else {
	    compositeReport = (CompositeReport<?, ?>) componentReport;
	    reportLine = (SecurityFromToReport) compositeReport.aggregateReport;
	}
	
	
	String tab = "\u0009";
	System.out.println("Report: From/To" + "\n");
	
	
	
	String acctName = compositeReport != null ? compositeReport.getName() : reportLine.getName();
	String acctTicker = compositeReport != null ? "NoTicker" : reportLine.getCurrencyWrapper().ticker;

	System.out.println("Account: " + tab + acctName + tab + "Ticker:" + tab
		+ acctTicker);
	System.out.println("From: " + tab + reportLine.getFromDateInt() + tab
		+ "To: " + tab + reportLine.getToDateInt());
	System.out.println("StartPos: " + tab + reportLine.getStartPos() + tab
		+ "StartPrice: " + tab + reportLine.getStartPrice() + tab
		+ "StartValue:" + tab + reportLine.getStartValue());
	System.out.println("EndPos: " + tab + reportLine.getEndPos() + tab
		+ "EndPrice: " + tab + reportLine.getEndPrice() + tab + "EndValue:"
		+ tab + reportLine.getEndValue());
	System.out.println("Buy: " + tab + reportLine.getBuy() + tab + "Sell: " + tab
		+ reportLine.getSell() + tab + "Short:" + tab + reportLine.getShortSell()
		+ tab + "CoverShort:" + tab + reportLine.getCoverShort());
	System.out.println("Income: " + tab + reportLine.income + tab
		+ "Expense: " + tab + reportLine.getExpense());
	System.out.println("LongBasis: " + tab + reportLine.getLongBasis() + tab
		+ "ShortBasis: " + tab + reportLine.getShortBasis());
	System.out.println("RealizedGain: " + tab + reportLine.getRealizedGain()
		+ tab + "UnrealizedGain: " + tab + reportLine.getUnrealizedGain()
		+ tab + "TotalGain:" + tab + reportLine.getTotalGain());
	printPerfMaps(reportLine.getArMap(), reportLine.getMdMap(), reportLine.getTransMap(),
		"Maps");
	System.out.println("\n" + "mdReturn: " + tab + reportLine.getMdReturn() + tab
		+ "AnnualReturn: " + tab + reportLine.getAnnualPercentReturn());
	System.out.println("\n");

    }

    public static void printPerfMaps(DateMap arMap,
	    DateMap mdMap, DateMap transMap, String msg) {
	String tab = "\u0009";
	String space = "";
	int maxSize = Math.max(arMap == null ? 0 : arMap.getMap().size(), Math.max(
		mdMap == null ? 0 : mdMap.getMap().size(), transMap == null ? 0
			: transMap.getMap().size()));
	System.out.println(msg + "\n");
	System.out.println("arMap" + tab + tab + "mdMap" + tab + tab
		+ "transMap");
	System.out.println("Date" + tab + "Value" + tab + "Date" + tab
		+ "Value" + tab + "Date" + tab + "Value" + tab);

	Integer[] arMapDates = arMap == null ? new Integer[0]
		: returnKeySetArray(arMap.getMap());
	Integer[] mdMapDates = mdMap == null ? new Integer[0]
		: returnKeySetArray(mdMap.getMap());
	Integer[] transMapDates = transMap == null ? new Integer[0]
		: returnKeySetArray(transMap.getMap());

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
