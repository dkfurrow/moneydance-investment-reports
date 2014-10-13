/*
 * TotalSnapshotReport.java
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
 * Generates total output for "Snapshot" Report
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */

public class TotalSnapshotReport extends TotalReport {


    public static final LinkedList<String> MODEL_HEADER = new LinkedList<>(Arrays.asList("InvAcct", "Security", "SecType",
            "SecSubType", "Ticker", "Last\nPrice", "End\nPos", "End\nValue",
            "Abs\nPrcChg", "Abs\nValChg", "Pct\nPrcChg", "TR\n1Day", "TR\n1Wk",
            "TR\n1Mth", "TR\n3Mth", "TR\nYTD", "TR\n1Year", "TR\n3Year",
            "TR\nALL", "AnnRet\nAll", "Long\nBasis", "Short\nBasis", "Income",
            "Ann.\nDiv", "Div\nYield", "Yield On\nBasis", "Rlzd\nGain", "Unrlzd\nGain",
            "Total\nGain"));
    public static String reportTypeName = "Snapshot Report";
    private static ColType[] COL_TYPES = new ColType[]{ColType.OBJECT, ColType.OBJECT, ColType.OBJECT,
            ColType.OBJECT, ColType.OBJECT, ColType.DOUBLE2, ColType.DOUBLE3, ColType.DOUBLE2,
            ColType.DOUBLE2, ColType.DOUBLE2, ColType.PERCENT1, ColType.PERCENT1, ColType.PERCENT1,
            ColType.PERCENT1, ColType.PERCENT1, ColType.PERCENT1, ColType.PERCENT1, ColType.PERCENT1,
            ColType.PERCENT1, ColType.PERCENT1, ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2,
            ColType.DOUBLE2, ColType.PERCENT1, ColType.PERCENT1, ColType.DOUBLE2, ColType.DOUBLE2,
            ColType.DOUBLE2};

    public TotalSnapshotReport(ReportConfig reportConfig) throws Exception {
        super(reportConfig, COL_TYPES, MODEL_HEADER);
    }

    @Override
    public int getClosedPosColumn() {
        return getModelHeader().indexOf("End\nValue");
    }

    @Override
    public String getReportTitle() {
        StringBuilder output = new StringBuilder();
        output.append("Investment Performance Snapshot: ");
        output.append(DateUtils.convertToShort(getReportDate().getSnapDateInt()));
        output.append(" -- Aggregate By: ").append(aggregationController.getFirstAggregator().getReportingName());
        if (!(aggregationController.getSecondAggregator() instanceof AllAggregate)) {
            output.append(" Then By: ").append(aggregationController.getSecondAggregator().getReportingName());
        }

        return output.toString();
    }

    @Override
    public SecurityReport getLeafSecurityReport(
            SecurityAccountWrapper securityAccountWrapper, DateRange dateRange) {
        return new SecuritySnapshotReport(securityAccountWrapper, dateRange);
    }

}
