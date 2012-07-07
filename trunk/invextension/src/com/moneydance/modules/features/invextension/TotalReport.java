/* TotalReport.java
 * Copyright 2012 Dale Furrow . All rights reserved.
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.moneydance.modules.features.invextension.CompositeReport.COMPOSITE_TYPE;
import com.moneydance.modules.features.invextension.ReportOutputTable.ColType;


/**
 * Base class for collection of security reports and composite reports
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/

public abstract class TotalReport<T extends Aggregator, U extends Aggregator> {
    public HashSet<SecurityReport> securityReports;
    public HashSet<CompositeReport<T,U>> compositeReports;
    public DateRange dateRange;
    public Class<T> firstAggregateClass;
    public Class<U> secondAggregateClass;
    public int firstSortColumn;
    public int secondSortColumn;
    
    // indicates if second aggregate is a subset of the first aggregate
    Boolean isHierarchy; 
    // indicates a composite report with only one security report will print 
    Boolean outputSingle;

    /**fetches all security reports and composites
     * @return
     */
    public ArrayList<ComponentReport> getReports() {
        ArrayList<ComponentReport> componentReports = new ArrayList<ComponentReport>();
        componentReports.addAll(securityReports);
        componentReports.addAll(compositeReports);
        return componentReports;
    }


    public DateRange getReportDate() {
        return dateRange;
    }


    public TotalReport(BulkSecInfo currentInfo, Class<T> firstAggClass,
	    Class<U> secondAggClass, Boolean isHierarchy, Boolean outputSingle,
	    DateRange dateRange) throws Exception {

	securityReports = new HashSet<SecurityReport>();
	compositeReports = new HashSet<CompositeReport<T, U>>();
	this.dateRange = dateRange;
	this.firstAggregateClass = firstAggClass;
	this.secondAggregateClass = secondAggClass;
	this.isHierarchy = isHierarchy;
	this.outputSingle = outputSingle;
	this.firstSortColumn = getDefaultColumn(firstAggClass);
	this.secondSortColumn = getDefaultColumn(secondAggClass);
	
	//produce all leaf-level Security Reports
	for (InvestmentAccountWrapper invWrapper : currentInfo.getInvestmentWrappers()) {
	    for (SecurityAccountWrapper secWrapper : invWrapper.getSecurityAccountWrappers()) {
		SecurityReport thisReport = getLeafSecurityReport(secWrapper,
			dateRange);
		securityReports.add(thisReport);
	    }
	}
	
	// generate "All Securities" composite, add to composite reports
	CompositeReport<T, U> allRept = getAllCompositeReport(dateRange,
		firstAggClass, secondAggClass);
	compositeReports.add(allRept);
	
	//generate composites and add Security Reports to them
	for (SecurityReport securityReport : securityReports) {
	    for (CompositeReport<T, U> compositeReport : compositeReports) {
		compositeReport.addTo(securityReport);
	    }
	    // generate composite based on first aggregate
	    compositeReports.add(securityReport.getCompositeReport(
		    firstAggClass, secondAggClass, COMPOSITE_TYPE.FIRST));
	    // if second AggClass isn't AllAggregate, need 1 or 2 more
	    // aggregates
	    if (securityReport.getAggregate(secondAggClass) != AllAggregate
		    .getInstance()) {
		compositeReports.add(securityReport.getCompositeReport(
			firstAggClass, secondAggClass, COMPOSITE_TYPE.BOTH));
		// if second aggregate a subset of first, don't need
		// second aggregate alone (line above suffices)
		if (!isHierarchy)
		    compositeReports.add(securityReport.getCompositeReport(
			    firstAggClass, secondAggClass,
			    COMPOSITE_TYPE.SECOND));
	    }
	}
	// recompute returns
	for (CompositeReport<T, U> compositeReport : compositeReports) {
	    compositeReport.recomputeAggregateReturns();
	}
    }

    /**Generates array of report line objects
     * @return
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public Object[][] getReportTable() throws SecurityException,
	    IllegalArgumentException, NoSuchFieldException,
	    IllegalAccessException {
	if (securityReports.isEmpty()) {
	    return new Object[0][0];
	} else {
	    HashSet<ComponentReport> allReports = new HashSet<ComponentReport>();
	    allReports.addAll(securityReports);
	    if (outputSingle) {
		allReports.addAll(compositeReports);
	    } else {
		for (CompositeReport<T, U> compositeReport : compositeReports) {
		    if (compositeReport.securityReports.size() > 1)
			allReports.add(compositeReport);
		}
	    }
	    int i = 0;
	    int cols = 0;
	    Object[][] table = null;
	    for (Iterator<ComponentReport> iterator = allReports.iterator(); iterator
		    .hasNext();) {
		ComponentReport componentReport = iterator.next();
		if (i == 0) {
		    cols = componentReport.toTableRow().length;
		    table = new Object[allReports.size()][cols];
		}
		Object[] row = componentReport.toTableRow();
		for (int j = 0; j < cols; j++) {
		    table[i][j] = row[j];
		}
		i++;
	    }
	    return table;
	} // end else
    }
    
    /** gets Default column for aggregating class (i.e. which to sort on)
     * @param aggClass
     * @return
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static Integer getDefaultColumn(
	    Class<? extends Aggregator> aggClass)
	    throws SecurityException, NoSuchFieldException,
	    IllegalArgumentException, IllegalAccessException {
	Field defaultNameField = aggClass.getDeclaredField("defaultColumn");
	return (Integer) defaultNameField.get(Integer.class);
    }
    
    /** gets Default column for aggregating class (i.e. which to sort on)
     * @param aggClass
     * @return
     * @throws SecurityException
     * @throws NoSuchFieldException
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public static String getOutputName(
	    Class<? extends Aggregator> aggClass)
	    throws SecurityException, NoSuchFieldException,
	    IllegalArgumentException, IllegalAccessException {
	Field defaultNameField = aggClass.getDeclaredField("outputName");
	return (String) defaultNameField.get(String.class);
    }
    
    /**generates appropriate leaf-level Security Report
     * @param securityAccountWrapper
     * @param thisDateRange
     * @return
     */
    public abstract SecurityReport getLeafSecurityReport(
	    SecurityAccountWrapper securityAccountWrapper, DateRange thisDateRange);
    
    /**Generates "All-Securities" Composite Report
     * @param thisDateRange
     * @param firstAggClass
     * @param secondAggClass
     * @return
     */
    public CompositeReport<T, U> getAllCompositeReport(DateRange thisDateRange,
	    Class<T> firstAggClass, Class<U> secondAggClass) {
	CompositeReport<T, U> allComposite = new CompositeReport<T, U>(
		thisDateRange, firstAggClass, secondAggClass);
	allComposite.aggregateReport = getLeafSecurityReport(null, thisDateRange);
	return allComposite;
    }

    /**Designates which column controls the "closed-position" report GUI function
     * @return
     */
    public abstract int getClosedPosColumn();

    /**Determines type of each column for GUI output.
     * @return
     */
    public abstract ColType[] getColumnTypes();

    /**generates inital index of frozen column for GUI.
     * @return
     */
    public abstract int getFrozenColumn();

    /**Generates GUI Header
     * @return
     */
    public abstract String[] getReportHeader();

    /**Generates GUI title
     * @return
     * @throws IllegalAccessException 
     * @throws NoSuchFieldException 
     * @throws IllegalArgumentException 
     * @throws SecurityException 
     */
    public abstract String getReportTitle() throws SecurityException,
	    IllegalArgumentException, NoSuchFieldException,
	    IllegalAccessException;
}