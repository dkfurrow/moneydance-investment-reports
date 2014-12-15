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

import java.util.ArrayList;
import java.util.Arrays;


/**
 * Report detailing performance attributes based on a specific "from" and
 * "to" date
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class SecurityFromToReport extends SecurityReport {
    /**
     * Generic constructor, which produces either the SecurityReport associated
     * with a given SecurityAccountWrapper or a blank report
     *
     * @param securityAccount reference account
     * @param dateRange       date range
     */
    public SecurityFromToReport(SecurityAccountWrapper securityAccount, DateRange dateRange) {
        super(securityAccount, dateRange);

        int fromDateInt = dateRange.getFromDateInt();
        int toDateInt = dateRange.getToDateInt();

        // Extractors for metrics
        ExtractorStartPrice eStartPrice = new ExtractorStartPrice(securityAccount, fromDateInt, toDateInt);
        ExtractorStartPosition eStartPosition = new ExtractorStartPosition(securityAccount, fromDateInt, toDateInt);
        ExtractorStartValue eStartValue = new ExtractorStartValue(securityAccount, fromDateInt, toDateInt);
        ExtractorStartLongBasis eLongBasis = new ExtractorStartLongBasis(securityAccount, fromDateInt, toDateInt);
        ExtractorStartShortBasis eShortBasis = new ExtractorStartShortBasis(securityAccount, fromDateInt, toDateInt);

        ExtractorEndPrice eEndPrice = new ExtractorEndPrice(securityAccount, fromDateInt, toDateInt);
        ExtractorEndPosition eEndPosition = new ExtractorEndPosition(securityAccount, fromDateInt, toDateInt);
        ExtractorEndValue eEndValue = new ExtractorEndValue(securityAccount, fromDateInt, toDateInt);
        ExtractorLongBasis eStartLongBasis = new ExtractorLongBasis(securityAccount, fromDateInt, toDateInt);
        ExtractorShortBasis eStartShortBasis = new ExtractorShortBasis(securityAccount, fromDateInt, toDateInt);

        ExtractorBuy eBuys = new ExtractorBuy(securityAccount, fromDateInt, toDateInt);
        ExtractorCoveredShort eCoverShorts = new ExtractorCoveredShort(securityAccount, fromDateInt, toDateInt);
        ExtractorExpense eExpense = new ExtractorExpense(securityAccount, fromDateInt, toDateInt);
        ExtractorIncome eIncome = new ExtractorIncome(securityAccount, fromDateInt, toDateInt);
        ExtractorSell eSells = new ExtractorSell(securityAccount, fromDateInt, toDateInt);
        ExtractorShortSell eShortSells = new ExtractorShortSell(securityAccount, fromDateInt, toDateInt);

        // Put them into a table under the appropriate names
        simpleMetric.put("StartPrice", new pair<Number>(0L, eStartPrice));
        simpleMetric.put("StartPosition", new pair<Number>(0L, eStartPosition));
        simpleMetric.put("StartValue", new pair<Number>(0L, eStartValue));
        simpleMetric.put("StartLongBasis", new pair<Number>(0L, eStartLongBasis));
        simpleMetric.put("StartShortBasis", new pair<Number>(0L, eStartShortBasis));

        simpleMetric.put("EndPrice", new pair<Number>(0L, eEndPrice));
        simpleMetric.put("EndPosition", new pair<Number>(0L, eEndPosition));
        simpleMetric.put("EndValue", new pair<Number>(0L, eEndValue));
        simpleMetric.put("LongBasis", new pair<Number>(0L, eLongBasis));
        simpleMetric.put("ShortBasis", new pair<Number>(0L, eShortBasis));

        simpleMetric.put("Buy", new pair<Number>(0L, eBuys));
        simpleMetric.put("CoveredShort", new pair<Number>(0L, eCoverShorts));
        simpleMetric.put("Expense", new pair<Number>(0L, eExpense));
        simpleMetric.put("Income", new pair<Number>(0L, eIncome));
        simpleMetric.put("Sell", new pair<Number>(0L, eSells));
        simpleMetric.put("ShortSell", new pair<Number>(0L, eShortSells));

        simpleMetric.put("RealizedGain", new pair<Number>(0L, null));
        simpleMetric.put("UnrealizedGain", new pair<Number>(0L, null));
        simpleMetric.put("TotalGain", new pair<Number>(0L, null));

        // These extractors return multiple values, which are exploded into values in the normal metrics
        ExtractorGains eGains = new ExtractorGains(securityAccount, fromDateInt, toDateInt);              // x 3

        multipleMetrics.put("_Gains", new pair<>(Arrays.asList((Number) 0L, 0L, 0L), eGains));

        // Extractors for return calculations. Cannot point to same as above, since they have state.
        ExtractorTotalReturn aggregatedAllReturn = new ExtractorTotalReturn(securityAccount, fromDateInt, toDateInt);
        ExtractorAnnualReturn aggregatedAnnualReturn = new ExtractorAnnualReturn(securityAccount, fromDateInt, toDateInt);

        returnsMetric.put("AllReturn", new pair<>(0.0, aggregatedAllReturn));
        returnsMetric.put("AnnualReturn", new pair<>(0.0, aggregatedAnnualReturn));

        // Do the calculations by running the extractors over the transactions in this account.
        doCalculations(securityAccount);
        // Distribute the values from extractors that return multiple values
        explode("_Gains", "RealizedGain", "UnrealizedGain", "TotalGain");
    }

    @Override
    public CompositeReport getCompositeReport(AggregationController aggregationController, COMPOSITE_TYPE compType) {
        return new CompositeReport(this, aggregationController, compType);
    }

    @Override
    public SecurityReport getAggregateSecurityReport() {
        SecurityFromToReport aggregate = new SecurityFromToReport(null, getDateRange());
        return initializeAggregateSecurityReport(aggregate);
    }

    @Override
    public void addTo(SecurityReport operand) {
        super.addTo(operand);

        addValue("Buy", operand, "Buy");
        addValue("Sell", operand, "Sell");
        addValue("ShortSell", operand, "ShortSell");
        addValue("CoveredShort", operand, "CoveredShort");
        addValue("Income", operand, "Income");
        addValue("Expense", operand, "Expense");
        addValue("LongBasis", operand, "LongBasis");
        addValue("ShortBasis", operand, "ShortBasis");
        addValue("RealizedGain", operand, "RealizedGain");
        addValue("UnrealizedGain", operand, "UnrealizedGain");
        addValue("TotalGain", operand, "TotalGain");
    }

    @Override
    public Object[] toTableRow() throws SecurityException, IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException {
        addLineBody();
        return super.getOutputLine().toArray();
    }

    @Override
    public void addLineBody() {
        ArrayList<Object> outputLine = super.getOutputLine();
        outputLine.add((Long) simpleMetric.get("StartPosition").value / 10000.0);
        outputLine.add((Long) simpleMetric.get("EndPosition").value / 10000.0);
        outputLine.add((Long) simpleMetric.get("StartPrice").value / 100.0);
        outputLine.add((Long) simpleMetric.get("EndPrice").value / 100.0);
        outputLine.add((Long) simpleMetric.get("StartValue").value / 100.0);
        outputLine.add((Long) simpleMetric.get("EndValue").value / 100.0);

        outputLine.add((Long) simpleMetric.get("Buy").value / 100.0);
        outputLine.add((Long) simpleMetric.get("Sell").value / 100.0);
        outputLine.add((Long) simpleMetric.get("ShortSell").value / 100.0);
        outputLine.add((Long) simpleMetric.get("CoveredShort").value / 100.0);
        outputLine.add((Long) simpleMetric.get("Income").value / 100.0);
        outputLine.add((Long) simpleMetric.get("Expense").value / 100.0);

        outputLine.add((Long) simpleMetric.get("LongBasis").value / 100.0);
        outputLine.add((Long) simpleMetric.get("ShortBasis").value / 100.0);

        outputLine.add((Long) simpleMetric.get("RealizedGain").value / 100.0);
        outputLine.add((Long) simpleMetric.get("UnrealizedGain").value / 100.0);
        outputLine.add((Long) simpleMetric.get("TotalGain").value / 100.0);

        outputLine.add(returnsMetric.get("AllReturn").value);
        outputLine.add(returnsMetric.get("AnnualReturn").value);
    }
}
