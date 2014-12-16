/*
 * SecuritySnapshotReport.java
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

import com.moneydance.modules.features.invextension.CompositeReport.COMPOSITE_TYPE;

import java.util.Arrays;

/**
 * Report detailing performance attributes based on a specific snapshot
 * date
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class SecuritySnapshotReport extends SecurityReport {
    /**
     * Generic constructor, which produces either the SecurityReport associated
     * with a given SecurityAccountWrapper or a blank report
     *
     * @param securityAccount Security Account Wrapper
     * @param dateRange       input date range
     */
    public SecuritySnapshotReport(SecurityAccountWrapper securityAccount, DateRange dateRange) {
        super(securityAccount, dateRange);

        int fromDateInt = dateRange.getFromDateInt();
        int snapDateInt = dateRange.getSnapDateInt();

        // Dates for return calculations
        int prevDayFromDateInt = DateUtils.getPrevBusinessDay(snapDateInt);
        int weekFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addDaysInt(snapDateInt, -7));
        int MonthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -1));
        int threeMonthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -3));
        int ytdFromDateInt = DateUtils.getStartYear(snapDateInt);
        int oneYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -12));
        int threeYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils.addMonthsInt(snapDateInt, -36));

        // Extractors for metrics
        ExtractorStartPrice eStartPrice = new ExtractorStartPrice(securityAccount, fromDateInt, snapDateInt);
        ExtractorStartPosition eStartPosition = new ExtractorStartPosition(securityAccount, fromDateInt, snapDateInt);
        ExtractorStartValue eStartValue = new ExtractorStartValue(securityAccount, fromDateInt, snapDateInt);

        ExtractorEndPrice eEndPrice = new ExtractorEndPrice(securityAccount, fromDateInt, snapDateInt);
        ExtractorEndPosition eEndPosition = new ExtractorEndPosition(securityAccount, fromDateInt, snapDateInt);
        ExtractorEndValue eEndValue = new ExtractorEndValue(securityAccount, fromDateInt, snapDateInt);

        ExtractorLongBasis eLongBasis = new ExtractorLongBasis(securityAccount, fromDateInt, snapDateInt);
        ExtractorShortBasis eShortBasis = new ExtractorShortBasis(securityAccount, fromDateInt, snapDateInt);
        ExtractorIncome eIncome = new ExtractorIncome(securityAccount, fromDateInt, snapDateInt);

        // Put them into a table under the appropriate names
        simpleMetric.put("StartPrice", new pair<Number>(0L, eStartPrice));
        simpleMetric.put("StartPosition", new pair<Number>(0L, eStartPosition));
        simpleMetric.put("StartValue", new pair<Number>(0L, eStartValue));

        simpleMetric.put("EndPrice", new pair<Number>(0L, eEndPrice));
        simpleMetric.put("EndPosition", new pair<Number>(0L, eEndPosition));
        simpleMetric.put("EndValue", new pair<Number>(0L, eEndValue));

        simpleMetric.put("AbsPriceChange", new pair<Number>(0L, null));
        simpleMetric.put("AbsValueChange", new pair<Number>(0L, null));
        simpleMetric.put("PctPriceChange", new pair<Number>(0.0, null));

        simpleMetric.put("LongBasis", new pair<Number>(0L, eLongBasis));
        simpleMetric.put("ShortBasis", new pair<Number>(0L, eShortBasis));

        simpleMetric.put("Income", new pair<Number>(0L, eIncome));
        simpleMetric.put("AnnualizedDividend", new pair<Number>(0L, null));
        simpleMetric.put("DividendYield", new pair<Number>(0.0, null));
        simpleMetric.put("YieldOnBasis", new pair<Number>(0.0, null));

        simpleMetric.put("RealizedGain", new pair<Number>(0L, null));
        simpleMetric.put("UnrealizedGain", new pair<Number>(0L, null));
        simpleMetric.put("TotalGain", new pair<Number>(0L, null));

        // These extractors return multiple values, which are exploded into values in the normal metrics
        ExtractorPriceChanges ePriceChange = new ExtractorPriceChanges(securityAccount, fromDateInt, snapDateInt);  // x 3
        ExtractorDividends eDividends = new ExtractorDividends(securityAccount, fromDateInt, snapDateInt);      // x 3
        ExtractorGains eGains = new ExtractorGains(securityAccount, fromDateInt, snapDateInt);              // x 3

        multipleMetrics.put("_PriceChange", new pair<>(Arrays.asList((Number) 0L, 0L, 0.0), ePriceChange));
        multipleMetrics.put("_Dividends", new pair<>(Arrays.asList((Number) 0L, 0.0, 0.0), eDividends));
        multipleMetrics.put("_Gains", new pair<>(Arrays.asList((Number) 0L, 0L, 0L), eGains));

        // Extractors for return calculations. Cannot point to same as above, since they have state.
        ExtractorTotalReturn aggregatedDayReturn = new ExtractorTotalReturn(securityAccount, prevDayFromDateInt, snapDateInt);
        ExtractorTotalReturn aggregatedWeekReturn = new ExtractorTotalReturn(securityAccount, weekFromDateInt, snapDateInt);
        ExtractorTotalReturn aggregatedMonthReturn = new ExtractorTotalReturn(securityAccount, MonthFromDateInt, snapDateInt);
        ExtractorTotalReturn aggregated3MonthReturn = new ExtractorTotalReturn(securityAccount, threeMonthFromDateInt, snapDateInt);
        ExtractorTotalReturn aggregatedYTDReturn = new ExtractorTotalReturn(securityAccount, ytdFromDateInt, snapDateInt);
        ExtractorTotalReturn aggregatedYearReturn = new ExtractorTotalReturn(securityAccount, oneYearFromDateInt, snapDateInt);
        ExtractorTotalReturn aggregated3YearReturn = new ExtractorTotalReturn(securityAccount, threeYearFromDateInt, snapDateInt);
        ExtractorTotalReturn aggregatedAllReturn = new ExtractorTotalReturn(securityAccount, fromDateInt, snapDateInt);
        ExtractorTotalReturn aggregatedAnnualReturn = new ExtractorAnnualReturn(securityAccount, fromDateInt, snapDateInt);

        returnsMetric.put("DayReturn", new pair<>(0.0, aggregatedDayReturn));
        returnsMetric.put("WeekReturn", new pair<>(0.0, aggregatedWeekReturn));
        returnsMetric.put("MonthReturn", new pair<>(0.0, aggregatedMonthReturn));
        returnsMetric.put("3MonthReturn", new pair<>(0.0, aggregated3MonthReturn));
        returnsMetric.put("YTDReturn", new pair<>(0.0, aggregatedYTDReturn));
        returnsMetric.put("YearReturn", new pair<>(0.0, aggregatedYearReturn));
        returnsMetric.put("3YearReturn", new pair<>(0.0, aggregated3YearReturn));
        returnsMetric.put("AllReturn", new pair<>(0.0, aggregatedAllReturn));
        returnsMetric.put("AnnualReturn", new pair<>(0.0, aggregatedAnnualReturn));

        // Do the calculations by running the extractors over the transactions in this account.
        doCalculations(securityAccount);
        // Distribute the values from extractors that return multiple values
        explode("_PriceChange", "AbsPriceChange", "AbsValueChange", "PctPriceChange");
        explode("_Dividends", "AnnualizedDividend", "DividendYield", "YieldOnBasis");
        explode("_Gains", "RealizedGain", "UnrealizedGain", "TotalGain");
    }

    @Override
    public CompositeReport getCompositeReport(AggregationController aggregationController, COMPOSITE_TYPE compType) {
        return new CompositeReport(this, aggregationController, compType);
    }

    @Override
    public SecurityReport getAggregateSecurityReport() {
        SecuritySnapshotReport aggregate = new SecuritySnapshotReport(null, getDateRange());
        return initializeAggregateSecurityReport(aggregate);
    }

    @Override
    public void addTo(SecurityReport operand) {
        super.addTo(operand);

        // Combine basic metrics
        simpleMetric.get("AbsPriceChange").value = 0L;
        simpleMetric.get("PctPriceChange").value = 0.0;
        addValue("AbsValueChange", operand, "AbsValueChange");
        addValue("LongBasis", operand, "LongBasis");
        addValue("ShortBasis", operand, "ShortBasis");
        addValue("Income", operand, "Income");
        addValue("RealizedGain", operand, "RealizedGain");
        addValue("UnrealizedGain", operand, "UnrealizedGain");
        addValue("TotalGain", operand, "TotalGain");

        // Recompute dividend yields
        combineDividends(operand);
    }

    /**
     * combines dividend data depending on whether dividend data are valid
     *
     * @param operand security snapshot to be combined
     */
    private void combineDividends(SecurityReport operand) {
        if (simpleMetric.get("AnnualizedDividend").value == 0
                && operand.simpleMetric.get("AnnualizedDividend").value != 0) {
            //take operand values
            assignValue("AnnualizedDividend", operand, "AnnualizedDividend");
            assignValue("DividendYield", operand, "DividendYield");
            assignValue("YieldOnBasis", operand, "YieldOnBasis");
        } else if (simpleMetric.get("AnnualizedDividend").value != 0
                && operand.simpleMetric.get("AnnualizedDividend").value != 0) {
            // both valid, add
            addValue("AnnualizedDividend", operand, "AnnualizedDividend");
            double annualizedDividend = simpleMetric.get("AnnualizedDividend").value.doubleValue();
            simpleMetric.get("DividendYield").value = annualizedDividend / (Long) simpleMetric.get("EndValue").value;
            simpleMetric.get("YieldOnBasis").value = annualizedDividend / (Long) simpleMetric.get("LongBasis").value;
        }
        // if both are zero, ignore and return
        // if operand is zero, then ignore and return
        // retain current values --return
    }

    @Override
    public void recordMetrics() {
        outputSimplePrice("EndPrice");
        outputSimplePosition("EndPosition");
        outputSimplePrice("EndValue");

        outputSimplePrice("AbsPriceChange");
        outputSimplePrice("AbsValueChange");
        outputSimpleValue("PctPriceChange");

        outputReturn("DayReturn");
        outputReturn("WeekReturn");
        outputReturn("MonthReturn");
        outputReturn("3MonthReturn");
        outputReturn("YTDReturn");
        outputReturn("YearReturn");
        outputReturn("3YearReturn");

        outputReturn("AllReturn");
        outputReturn("AnnualReturn");

        outputSimplePrice("LongBasis");
        outputSimplePrice("ShortBasis");

        outputSimplePrice("Income");
        outputSimplePrice("AnnualizedDividend");
        outputSimpleValue("DividendYield");
        outputSimpleValue("YieldOnBasis");

        outputSimplePrice("RealizedGain");
        outputSimplePrice("UnrealizedGain");
        outputSimplePrice("TotalGain");
    }
}


