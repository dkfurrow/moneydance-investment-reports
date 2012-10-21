/* ComponentReport.java
 * Copyright 2012 Dale K. Furrow . All rights reserved.
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

import java.util.Iterator;


/**
 * Base class for security reports and composites.
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/

public abstract class ComponentReport {
    
    /**adds SecurityReport to other SecurityReport or CompositeReport
     * @param securityReport
     */
    public abstract void addTo(SecurityReport securityReport);

    /**
     * Returns annualized returns (same as excel XIRR function).
     * @param retMap date map relating dateInts to cash flows
     * @param mdRet Mod-Dietz total return for inital guess
     * @return
     */
    public double computeAnnualReturn(DateMap retMap, double mdRet) {
	// Assumes first value is startvalue, last is endvalue same with dates.

	int numPeriods = retMap == null ? 0 : retMap.keySet().size();
	double[] excelDates = new double[numPeriods];
	double[] annRetValuesArray = new double[numPeriods];
	int[] dateIntsArray = new int[numPeriods];

	// Put datemap info into primitive arrays
	if (retMap != null && retMap.keySet().size() > 0
		&& retMap.values().size() > 0) {
	    int i = 0;
	    for (Iterator<Integer> it = retMap.keySet().iterator(); it
		    .hasNext();) {
		Integer dateInt = it.next();
		Double value = retMap.get(dateInt);

		dateIntsArray[i] = dateInt;
		annRetValuesArray[i] = value;
		i++;
	    }
	} else {
	    return Double.NaN;
	}

	for (int i = 0; i < numPeriods; i++) {
	    excelDates[i] = DateUtils.getExcelDateValue(dateIntsArray[i]);
	}
	double totYrs = (excelDates[numPeriods - 1] - excelDates[0]) / 365;

	// Need to supply guess to return algorithm, so use modified dietz
	// return divided by number of years (have to add 1 because of returns
	// algorithm). Must be greater than zero
	double guess = Math.max((1 + mdRet / totYrs), 0.01);

	XIRRData thisData = new XIRRData(numPeriods, guess, annRetValuesArray,
		excelDates);
	double annualReturn = XIRR.xirr(thisData);

	return annualReturn;
    }


    /**
     * Compute Modified Dietz returns
     * @param startValue starting value (expressed as positive)
     * @param endValue ending value
     * @param income income (to be added to endValue)
     * @param expense  expense (to be added to endValue)
     * @param mdMap datemap which relates dates to cash flows
     * @return Mod Dietz return
     */
    public double computeMDReturn(double startValue, double endValue,
	    double income, double expense, DateMap mdMap) {
	if (mdMap.keySet().size() > 0 && mdMap.values().size() > 0) {
	    double mdValue = 0;
	    double sumCF = 0;
	    double weightCF = 0;

	    Integer cd = DateUtils.getDaysBetween(mdMap.firstKey(),
		    mdMap.lastKey());
	    Double cdD = cd.doubleValue();

	    for (Iterator<Integer> it = mdMap.keySet().iterator(); it.hasNext();) {
		Integer thisDateInt = it.next();
		Double cf = mdMap.get(thisDateInt);
		Integer dayBetw = DateUtils.getDaysBetween(mdMap.firstKey(),
			thisDateInt);
		Double dayBetD = dayBetw.doubleValue();
		double wSubI = (cdD - dayBetD) / cdD;
		weightCF = weightCF + (wSubI * cf);
		sumCF = sumCF + cf;
	    }

	    mdValue = ((endValue + income + expense) - startValue - sumCF)
		    / (startValue + weightCF);
	    return mdValue;
	} else {
	    return Double.NaN;
	}
    }

    /** Recomputes Aggregate Returns after CompositeReport is complete
     * 
     */
    public abstract void recomputeAggregateReturns();
    
    /**
     * @return produces report line for output from ComponentReport
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public  Object[] toTableRow() throws SecurityException, IllegalArgumentException, 
    NoSuchFieldException, IllegalAccessException {
	return null;
    }
}