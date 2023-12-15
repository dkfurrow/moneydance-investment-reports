/*
 * SecurityFromToReport.java
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
 * Report detailing performance attributes based on a specific "from" and
 * "to" date
 * Version 1.0
 *
 * @author Dale Furrow
 */
public final class SecurityFromToReport extends SecurityReport {
    /**
     * Generic constructor, which produces either the SecurityReport associated
     * with a given SecurityAccountWrapper or a blank report
     *
     * @param securityAccount reference account
     * @param dateRange       date range
     */
    public SecurityFromToReport(ReportConfig reportConfig, SecurityAccountWrapper securityAccount,
                                CompositeReport compositeReport ,DateRange dateRange) {
        super(reportConfig, securityAccount, compositeReport ,dateRange);

        int fromDateInt = dateRange.getFromDateInt();
        int toDateInt = dateRange.getToDateInt();

        // Extractors for metrics
        ExtractorStartPrice eStartPrice = new ExtractorStartPrice(securityAccount, fromDateInt, toDateInt);
        ExtractorStartPosition eStartPosition = new ExtractorStartPosition(securityAccount, fromDateInt, toDateInt);
        ExtractorStartValue eStartValue = new ExtractorStartValue(securityAccount, fromDateInt, toDateInt);

        ExtractorEndPrice eEndPrice = new ExtractorEndPrice(securityAccount, fromDateInt, toDateInt);
        ExtractorEndPosition eEndPosition = new ExtractorEndPosition(securityAccount, fromDateInt, toDateInt);
        ExtractorEndValue eEndValue = new ExtractorEndValue(securityAccount, fromDateInt, toDateInt);
        ExtractorShortBasis eShortBasis = new ExtractorShortBasis(securityAccount, fromDateInt, toDateInt);
        ExtractorLongBasis eLongBasis = new ExtractorLongBasis(securityAccount, fromDateInt, toDateInt);

        ExtractorBuy eBuys = new ExtractorBuy(securityAccount, fromDateInt, toDateInt);
        ExtractorCoveredShort eCoverShorts = new ExtractorCoveredShort(securityAccount, fromDateInt, toDateInt);
        ExtractorExpense eExpense = new ExtractorExpense(securityAccount, fromDateInt, toDateInt);
        ExtractorIncome eIncome = new ExtractorIncome(securityAccount, fromDateInt, toDateInt);
        ExtractorSell eSells = new ExtractorSell(securityAccount, fromDateInt, toDateInt);
        ExtractorShortSell eShortSells = new ExtractorShortSell(securityAccount, fromDateInt, toDateInt);

        // Put them into a table under the appropriate names
        simpleMetric.put(SMStartPrice, new MetricEntry<>(0L, eStartPrice));
        simpleMetric.put(SMStartPosition, new MetricEntry<>(0L, eStartPosition));
        simpleMetric.put(SMStartValue, new MetricEntry<>(0L, eStartValue));

        simpleMetric.put(SMEndPrice, new MetricEntry<>(0L, eEndPrice));
        simpleMetric.put(SMEndPosition, new MetricEntry<>(0L, eEndPosition));
        simpleMetric.put(SMEndValue, new MetricEntry<>(0L, eEndValue));

        simpleMetric.put(SMLongBasis, new MetricEntry<>(0L, eLongBasis));
        simpleMetric.put(SMShortBasis, new MetricEntry<>(0L, eShortBasis));

        simpleMetric.put(SMBuy, new MetricEntry<>(0L, eBuys));
        simpleMetric.put(SMCoveredShort, new MetricEntry<>(0L, eCoverShorts));
        simpleMetric.put(SMExpense, new MetricEntry<>(0L, eExpense));
        simpleMetric.put(SMIncome, new MetricEntry<>(0L, eIncome));
        simpleMetric.put(SMSell, new MetricEntry<>(0L, eSells));
        simpleMetric.put(SMShortSell, new MetricEntry<>(0L, eShortSells));

        simpleMetric.put(SMRealizedGain, new MetricEntry<>(0L, null));
        simpleMetric.put(SMUnrealizedGain, new MetricEntry<>(0L, null));
        simpleMetric.put(SMTotalGain, new MetricEntry<>(0L, null));

        // These extractors return multiple values, which are exploded into values in the normal metrics
        ExtractorGains eGains = new ExtractorGainsFT(securityAccount, fromDateInt, toDateInt);              // x 3

        multipleMetrics.put(MMGains, new MetricEntry<>(Arrays.asList(0L, 0L, 0L), eGains));

        // Extractors for return calculations.
        ExtractorReturnBase aggregatedWindowTotalReturn = ExtractorReturnBase.factory(securityAccount, this,fromDateInt,
                toDateInt, ExtractorReturnBase.ReturnWindowType.DEFAULT, reportConfig.useOrdinaryReturn());
        ExtractorIRR aggregatedWindowAnnualReturn
                = new ExtractorIRR(securityAccount, this, fromDateInt, toDateInt, ExtractorReturnBase.ReturnWindowType.DEFAULT);
        ExtractorReturnBase aggregatedStubTotalReturn
                = ExtractorReturnBase.factory(securityAccount, this, fromDateInt,
                toDateInt, ExtractorReturnBase.ReturnWindowType.STUB, reportConfig.useOrdinaryReturn());
        ExtractorIRR aggregatedStubAnnualReturn
                = new ExtractorIRR(securityAccount, this, fromDateInt, toDateInt, ExtractorReturnBase.ReturnWindowType.STUB);

        returnsMetric.put(RMWindowTotalReturn, new MetricEntry<>(0.0, aggregatedWindowTotalReturn));
        returnsMetric.put(RMWindowAnnualReturn, new MetricEntry<>(0.0, aggregatedWindowAnnualReturn));
        returnsMetric.put(RMStubTotalReturn, new MetricEntry<>(0.0, aggregatedStubTotalReturn));
        returnsMetric.put(RMStubAnnualReturn, new MetricEntry<>(0.0, aggregatedStubAnnualReturn));


        // Do the calculations by running the extractors over the transactions in this account.
        doCalculations(securityAccount);
        // Distribute the values from extractors that return multiple values
        explode(MMGains, SMRealizedGain, SMUnrealizedGain, SMTotalGain);
    }

    @Override
    public CompositeReport getCompositeReport(AggregationController aggregationController, COMPOSITE_TYPE compType) {
        return new CompositeReport(this, aggregationController, compType);
    }

    @Override
    public SecurityReport getAggregateSecurityReport(CompositeReport compositeReport) {
        SecurityFromToReport aggregate = new SecurityFromToReport(reportConfig, null, compositeReport, getDateRange());
        return initializeAggregateSecurityReport(aggregate);
    }

    @Override
    public void addTo(SecurityReport operand) {
        super.addTo(operand);

        addValue(SMBuy, operand);
        addValue(SMSell, operand);
        addValue(SMShortSell, operand);
        addValue(SMCoveredShort, operand);
        addValue(SMIncome, operand);
        addValue(SMExpense, operand);
        addValue(SMLongBasis, operand);
        addValue(SMShortBasis, operand);
        addValue(SMRealizedGain, operand);
        addValue(SMUnrealizedGain, operand);
        addValue(SMTotalGain, operand);
    }

    @Override
    public void recordMetrics() {
        outputSimplePosition(SMStartPosition);
        outputSimplePosition(SMEndPosition);
        outputSimplePrice(SMStartPrice);
        outputSimplePrice(SMEndPrice);
        outputSimplePrice(SMStartValue);
        outputSimplePrice(SMEndValue);

        outputSimplePrice(SMBuy);
        outputSimplePrice(SMSell);
        outputSimplePrice(SMShortSell);
        outputSimplePrice(SMCoveredShort);
        outputSimplePrice(SMIncome);
        outputSimplePrice(SMExpense);

        outputSimplePrice(SMLongBasis);
        outputSimplePrice(SMShortBasis);

        outputSimplePrice(SMRealizedGain);
        outputSimplePrice(SMUnrealizedGain);
        outputSimplePrice(SMTotalGain);

        outputReturn(RMWindowTotalReturn);
        outputReturn(RMWindowAnnualReturn);
        outputReturn(RMStubTotalReturn);
        outputReturn(RMStubAnnualReturn);

    }
}
