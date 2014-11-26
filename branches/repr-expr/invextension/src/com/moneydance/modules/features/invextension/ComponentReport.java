/*
 * ComponentReport.java
 * Copyright (c) 2014, Dale K. Furrow
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.moneydance.modules.features.invextension;

import java.math.BigDecimal;

/**
 * Base class for security reports and composites.
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */

public abstract class ComponentReport {

    /**
     * adds SecurityReport to other SecurityReport or CompositeReport
     *
     * @param securityReport security report to be added
     */
    public abstract void addTo(SecurityReport securityReport);

    /**
     * Returns annualized returns (same as excel XIRR function).
     *
     * @param retMap date map relating dateInts to cash flows
     * @param mdRet  Mod-Dietz total return for inital guess
     * @return Annual return
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
            for (Integer dateInt : retMap.keySet()) {
                BigDecimal value = retMap.get(dateInt);
                dateIntsArray[i] = dateInt;
                annRetValuesArray[i] = value.intValue();
                i++;
            }
        } else {
            return 0.0;
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

        return XIRR.xirr(thisData);
    }


    /**
     * Compute Modified Dietz returns
     *
     * @param startValue starting value (expressed as positive)
     * @param endValue   ending value
     * @param income     income (to be added to endValue)
     * @param expense    expense (to be added to endValue)
     * @param mdMap      datemap which relates dates to cash flows
     * @return Mod Dietz return
     */
    public double computeMDReturn(long startValue, long endValue,
                                  long income, long expense, DateMap mdMap) {
        if (mdMap.keySet().size() > 0 && mdMap.values().size() > 0) {
            Double mdValue;
            long sumCF = 0;
            long weightCF = 0;

            int cd = DateUtils.getDaysBetween(mdMap.firstKey(), mdMap.lastKey());

            for (Integer thisDateInt : mdMap.keySet()) {
                Long cf = mdMap.get(thisDateInt).longValue();
                int dayBetw = DateUtils.getDaysBetween(mdMap.firstKey(), thisDateInt);
                double wSubI = ((double)cd - (double)dayBetw) / (double)cd;
                weightCF += Math.round(wSubI * cf);
                sumCF += cf;
            }

            mdValue = (double)((endValue + income + expense) - startValue - sumCF)
                    / (double)(startValue + weightCF);
            return mdValue;
        } else {
            return 0.0;
        }
    }

    /**
     * Recomputes Aggregate Returns after CompositeReport is complete
     */
    public abstract void recomputeAggregateReturns();

    /**
     * @return produces report line for output from ComponentReport
     * @throws SecurityException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public abstract Object[] toTableRow() throws SecurityException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException;
}