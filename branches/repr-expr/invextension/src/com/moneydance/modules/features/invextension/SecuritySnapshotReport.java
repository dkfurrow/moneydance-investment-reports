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

import java.util.ArrayList;
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

        int fromDateInt = 19700101; // Earliest possible date
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
        simpleMetric.put(SMStartPrice, new MetricEntry<Number>(0L, eStartPrice));
        simpleMetric.put(SMStartPosition, new MetricEntry<Number>(0L, eStartPosition));
        simpleMetric.put(SMStartValue, new MetricEntry<Number>(0L, eStartValue));

        simpleMetric.put(SMEndPrice, new MetricEntry<Number>(0L, eEndPrice));
        simpleMetric.put(SMEndPosition, new MetricEntry<Number>(0L, eEndPosition));
        simpleMetric.put(SMEndValue, new MetricEntry<Number>(0L, eEndValue));

        simpleMetric.put(SMAbsPriceChange, new MetricEntry<Number>(0L, null));
        simpleMetric.put(SMAbsValueChange, new MetricEntry<Number>(0L, null));
        simpleMetric.put(SMPctPriceChange, new MetricEntry<Number>(0.0, null));

        simpleMetric.put(SMLongBasis, new MetricEntry<Number>(0L, eLongBasis));
        simpleMetric.put(SMShortBasis, new MetricEntry<Number>(0L, eShortBasis));

        simpleMetric.put(SMIncome, new MetricEntry<Number>(0L, eIncome));
        simpleMetric.put(SMAnnualizedDividend, new MetricEntry<Number>(0L, null));
        simpleMetric.put(SMDividendYield, new MetricEntry<Number>(0.0, null));
        simpleMetric.put(SMYieldOnBasis, new MetricEntry<Number>(0.0, null));

        simpleMetric.put(SMRealizedGain, new MetricEntry<Number>(0L, null));
        simpleMetric.put(SMUnrealizedGain, new MetricEntry<Number>(0L, null));
        simpleMetric.put(SMTotalGain, new MetricEntry<Number>(0L, null));

        // These extractors return multiple values, which are exploded into values in the normal metrics
        ExtractorPriceChanges ePriceChange = new ExtractorPriceChanges(securityAccount, fromDateInt, snapDateInt);  // x 3
        ExtractorDividends eDividends = new ExtractorDividends(securityAccount, fromDateInt, snapDateInt);      // x 3
        ExtractorGains eGains = new ExtractorGains(securityAccount, fromDateInt, snapDateInt);              // x 3

        multipleMetrics.put(MMPriceChange, new MetricEntry<>(Arrays.asList((Number) 0L, 0L, 0.0), ePriceChange));
        multipleMetrics.put(MMDividends, new MetricEntry<>(Arrays.asList((Number) 0L, 0.0, 0.0), eDividends));
        multipleMetrics.put(MMGains, new MetricEntry<>(Arrays.asList((Number) 0L, 0L, 0L), eGains));

        // Extractors for return calculations. Cannot point to same as above, since they have state.
        ExtractorTotalReturn aggregatedDayReturn
                = new ExtractorTotalReturn(securityAccount, prevDayFromDateInt, snapDateInt, false);
        ExtractorTotalReturn aggregatedWeekReturn
                = new ExtractorTotalReturn(securityAccount, weekFromDateInt, snapDateInt, false);
        ExtractorTotalReturn aggregatedMonthReturn
                = new ExtractorTotalReturn(securityAccount, MonthFromDateInt, snapDateInt, false);
        ExtractorTotalReturn aggregated3MonthReturn
                = new ExtractorTotalReturn(securityAccount, threeMonthFromDateInt, snapDateInt, false);
        ExtractorTotalReturn aggregatedYTDReturn
                = new ExtractorTotalReturn(securityAccount, ytdFromDateInt, snapDateInt, false);
        ExtractorTotalReturn aggregatedYearReturn
                = new ExtractorTotalReturn(securityAccount, oneYearFromDateInt, snapDateInt, false);
        ExtractorTotalReturn aggregated3YearReturn
                = new ExtractorTotalReturn(securityAccount, threeYearFromDateInt, snapDateInt, false);

        int allDateInt = snapDateInt;
        if (securityAccount != null) {
            ArrayList<TransactionValues> transSet = securityAccount.getTransactionValues();
            if (!transSet.isEmpty()) {
                allDateInt = DateUtils.getPrevBusinessDay(transSet.get(0).getDateInt());
            }
        }
        ExtractorTotalReturn aggregatedAllReturn
                = new ExtractorTotalReturn(securityAccount, allDateInt, snapDateInt, true);
        ExtractorTotalReturn aggregatedAnnualReturn
                = new ExtractorAnnualReturn(securityAccount, allDateInt, snapDateInt);

        returnsMetric.put(RMDayReturn, new MetricEntry<>(0.0, aggregatedDayReturn));
        returnsMetric.put(RMWeekReturn, new MetricEntry<>(0.0, aggregatedWeekReturn));
        returnsMetric.put(RMMonthReturn, new MetricEntry<>(0.0, aggregatedMonthReturn));
        returnsMetric.put(RM3MonthReturn, new MetricEntry<>(0.0, aggregated3MonthReturn));
        returnsMetric.put(RMYTDReturn, new MetricEntry<>(0.0, aggregatedYTDReturn));
        returnsMetric.put(RMYearReturn, new MetricEntry<>(0.0, aggregatedYearReturn));
        returnsMetric.put(RM3YearReturn, new MetricEntry<>(0.0, aggregated3YearReturn));
        returnsMetric.put(RMAllReturn, new MetricEntry<>(0.0, aggregatedAllReturn));
        returnsMetric.put(RMAnnualReturn, new MetricEntry<>(0.0, aggregatedAnnualReturn));

        // Do the calculations by running the extractors over the transactions in this account.
        doCalculations(securityAccount);
        // Distribute the values from extractors that return multiple values
        explode(MMPriceChange, SMAbsPriceChange, SMAbsValueChange, SMPctPriceChange);
        explode(MMDividends, SMAnnualizedDividend, SMDividendYield, SMYieldOnBasis);
        explode(MMGains, SMRealizedGain, SMUnrealizedGain, SMTotalGain);
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
        simpleMetric.get(SMAbsPriceChange).value = 0L;
        simpleMetric.get(SMPctPriceChange).value = 0.0;
        addValue(SMAbsValueChange, operand);
        addValue(SMLongBasis, operand);
        addValue(SMShortBasis, operand);
        addValue(SMIncome, operand);
        addValue(SMRealizedGain, operand);
        addValue(SMUnrealizedGain, operand);
        addValue(SMTotalGain, operand);

        // Recompute dividend yields
        combineDividends(operand);
    }

    /**
     * combines dividend data depending on whether dividend data are valid
     *
     * @param operand security snapshot to be combined
     */
    private void combineDividends(SecurityReport operand) {
        if ((Long) simpleMetric.get(SMAnnualizedDividend).value == 0
                && (Long) operand.simpleMetric.get(SMAnnualizedDividend).value != 0) {
            //take operand values
            assignValue(SMAnnualizedDividend, operand);
            assignValue(SMDividendYield, operand);
            assignValue(SMYieldOnBasis, operand);
        } else if ((Long) simpleMetric.get(SMAnnualizedDividend).value != 0
                && (Long) operand.simpleMetric.get(SMAnnualizedDividend).value != 0) {
            // both valid, add
            addValue(SMAnnualizedDividend, operand);
            double annualizedDividend = simpleMetric.get(SMAnnualizedDividend).value.doubleValue();
            simpleMetric.get(SMDividendYield).value = annualizedDividend / (Long) simpleMetric.get(SMEndValue).value;
            simpleMetric.get(SMYieldOnBasis).value = annualizedDividend / (Long) simpleMetric.get(SMLongBasis).value;
        }
        // if both are zero, ignore and return
        // if operand is zero, then ignore and return
        // retain current values --return
    }

    @Override
    public void recordMetrics() {
        outputSimplePrice(SMEndPrice);
        outputSimplePosition(SMEndPosition);
        outputSimplePrice(SMEndValue);

        outputSimplePrice(SMAbsPriceChange);
        outputSimplePrice(SMAbsValueChange);
        outputSimpleValue(SMPctPriceChange);

        outputReturn(RMDayReturn);
        outputReturn(RMWeekReturn);
        outputReturn(RMMonthReturn);
        outputReturn(RM3MonthReturn);
        outputReturn(RMYTDReturn);
        outputReturn(RMYearReturn);
        outputReturn(RM3YearReturn);

        outputReturn(RMAllReturn);
        outputReturn(RMAnnualReturn);

        outputSimplePrice(SMLongBasis);
        outputSimplePrice(SMShortBasis);

        outputSimplePrice(SMIncome);
        outputSimplePrice(SMAnnualizedDividend);
        outputSimpleValue(SMDividendYield);
        outputSimpleValue(SMYieldOnBasis);

        outputSimplePrice(SMRealizedGain);
        outputSimplePrice(SMUnrealizedGain);
        outputSimplePrice(SMTotalGain);
    }
}


