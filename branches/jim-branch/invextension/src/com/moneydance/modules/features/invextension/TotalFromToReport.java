/* TotalFromToReport.java
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

import java.util.ArrayList;

import com.moneydance.modules.features.invextension.FormattedTable.ColFormat;


/**
 * Generates total output for "From/To" Report
 * Version 1.0
 * @author Dale Furrow
 * @param <T>
 * @param <U>
 */
public class TotalFromToReport<T extends Aggregator, U extends Aggregator> extends TotalReport<T, U> {
    private int reportFromDateInt;
    private int reportToDateInt;


    public TotalFromToReport(BulkSecInfo currentInfo,
                             Class<T> firstAggClass,
                             Class<U> secondAggClass,
                             Boolean isHierarchy,
                             Boolean outputSingle,
                             int fromDateInt,
                             int toDateInt) throws Exception {
        super(currentInfo,
              firstAggClass,
              secondAggClass,
              isHierarchy,
              outputSingle,
              new DateRange(fromDateInt, toDateInt));
        assert (fromDateInt != 0 && toDateInt != 0);
        reportFromDateInt = fromDateInt;
        reportToDateInt = toDateInt;
    }

    @Override
        public int getClosedPosColumn() {
        return 10;
    }

    private static ColFormat[] colFormats = new ColFormat[] {ColFormat.STRING,
                                                             ColFormat.STRING,
                                                             ColFormat.STRING,
                                                             ColFormat.STRING,
                                                             ColFormat.STRING,
                                                             ColFormat.DOUBLE3,
                                                             ColFormat.DOUBLE3,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.DOUBLE2,
                                                             ColFormat.PERCENT1,
                                                             ColFormat.PERCENT1};

    @Override
    public ColFormat[] getColumnFormats() {
        return colFormats;
    }

    @Override
        public int getFrozenColumn() {
        return 5;
    }

    @Override
        public String[] getReportHeader() {
        ArrayList<String> snapValues = new ArrayList<String>();

        snapValues.add("InvAcct");
        snapValues.add("Security");
        snapValues.add("SecType");
        snapValues.add("SecSubType");
        snapValues.add("Ticker");
        snapValues.add("StartPos");
        snapValues.add("EndPos");
        snapValues.add("StartPrice");
        snapValues.add("EndPrice");
        snapValues.add("StartValue");
        snapValues.add("EndValue");
        snapValues.add("buy");
        snapValues.add("sell");
        snapValues.add("shortSell");
        snapValues.add("coverShort");
        snapValues.add("income");
        snapValues.add("Expense");
        snapValues.add("longBasis");
        snapValues.add("shortBasis");
        snapValues.add("realizedGain");
        snapValues.add("unrealizedGain");
        snapValues.add("periodReturn");
        snapValues.add("percentReturn");
        snapValues.add("annualPercentReturn");

        return snapValues.toArray(new String[snapValues.size()]);
    }

    @Override
        public String getReportTitle() throws SecurityException,
        IllegalArgumentException, NoSuchFieldException,
        IllegalAccessException {
        StringBuilder output = new StringBuilder();
        output.append("Investment Performance--From: ");
        output.append(DateUtils.convertToShort(reportFromDateInt) + " To: ");
        output.append(DateUtils.convertToShort(reportToDateInt) + " - ");
        output.append("Aggregate By: " + TotalReport.getOutputName(getFirstAggregateClass()));
        if (!getSecondAggregateClass().equals(AllAggregate.class)) {
            output.append(" Then By: " + TotalReport.getOutputName(getSecondAggregateClass()));
        }
        return output.toString();

    }

    @Override
        public SecurityReport getLeafSecurityReport(SecurityAccountWrapper securityAccountWrapper,
                                                    DateRange dateRange) {
        return new SecurityFromToReport(securityAccountWrapper, dateRange);
    }

}
