/*
 * TotalFromToReport.java
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

import com.moneydance.modules.features.invextension.TotalReportOutputPane.ColType;

import java.util.Arrays;
import java.util.LinkedList;


/**
 * Generates total output for "From/To" Report
 * Version 1.0
 *
 * @author Dale Furrow
 */
public final class TotalFromToReport extends TotalReport {

    public static final LinkedList<String> MODEL_HEADER = new LinkedList<>(Arrays.asList("InvAcct", "Security", "SecType",
            "SecSubType", "Ticker", "StartPos", "EndPos", "Start\nPrice", "End\nPrice", "Start\nValue", "End\nValue", "Buy",
            "Sell", "Short\nSell", "Cover\nShort", "Income", "Expense", "Long\nBasis", "Short\nBasis", "Realized\nGain",
            "Unrealized\nGain", "Period\nReturn", "TotPct\nReturn", "AnnPct\nReturn", "Stub-TotPct\nReturn", "Stub-AnnPct\nReturn"));
    public static String reportTypeName = "'From-To' Report";
    private static final ColType[] COL_TYPES = new ColType[]{ColType.OBJECT,
            ColType.OBJECT, ColType.OBJECT, ColType.OBJECT, ColType.OBJECT,
            ColType.DOUBLE3, ColType.DOUBLE3, ColType.DOUBLE2, ColType.DOUBLE2,
            ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2,
            ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2,
            ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2,
            ColType.DOUBLE2, ColType.PERCENT1, ColType.PERCENT1, ColType.PERCENT1, ColType.PERCENT1};


    public TotalFromToReport(ReportConfig reportConfig, BulkSecInfo currentInfo) throws Exception {
        super(reportConfig, currentInfo, COL_TYPES, MODEL_HEADER);
    }

    @Override
    public int getClosedPosColumn() {
        return MODEL_HEADER.indexOf("End\nValue");
    }

    @Override
    public String getReportTitle() {
        StringBuilder output = new StringBuilder();
        output.append("Investment Performance--From: ");
        output.append(DateUtils.convertToShort(getReportDate().getFromDateInt())).append(" To: ");
        output.append(DateUtils.convertToShort(getReportDate().getToDateInt())).append(" - ");
        output.append("Aggregate By: ").append(aggregationController.getFirstAggregator().getReportingName());
        if (!(aggregationController.getSecondAggregator() instanceof AllAggregate)) {
            output.append(" Then By: ").append(aggregationController.getSecondAggregator().getReportingName());
        }
        return output.toString();

    }

    @Override
    public SecurityReport getLeafSecurityReport(SecurityAccountWrapper securityAccountWrapper, DateRange dateRange) {
        return new SecurityFromToReport(reportConfig, securityAccountWrapper, null, dateRange);
    }

    @Override
    public CompositeReport getAllCompositeReport(DateRange dateRange, AggregationController aggregationController) {
        CompositeReport compositeReport = new CompositeReport(aggregationController);
        SecurityFromToReport allAggregate = new SecurityFromToReport(reportConfig, null, compositeReport, dateRange);
        compositeReport.setAggregateReport(allAggregate);
        return  compositeReport;
    }

}
