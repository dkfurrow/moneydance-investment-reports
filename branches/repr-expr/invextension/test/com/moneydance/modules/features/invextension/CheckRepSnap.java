/*
 * CheckRepSnap.java
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

import java.util.ArrayList;

/**
 * Generates dump of  intermediate values in
 * "Snap" Report to allow for auditing
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class CheckRepSnap {
    public static final boolean rptOutputSingle = false;
    public static final int numFrozenColumns = 5; //Irrelevant for testing purposes
    // private static final int getFromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    private static final DateRange dateRange = new DateRange(toDateInt, toDateInt, toDateInt);

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void main(String[] args) throws Exception {
        AggregationController aggregationController = AggregationController.INVACCT;
        boolean closedPosHidden = true;

        ReportConfig reportConfig = new ReportConfig(TotalSnapshotReport.class, "Test Report",
                true, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalSnapshotReport.MODEL_HEADER),
                ReportConfig.getDefaultExcludedAccounts(), ReportConfig.getInvestmentExpenseAccounts(), dateRange);
        TotalSnapshotReport snapshotReport = new TotalSnapshotReport(reportConfig);
        snapshotReport.calcReport(BulkSecInfoTest.getBaseSecurityInfoAvgCost());
        ArrayList<ComponentReport> componentReports = snapshotReport
                .getReports();
        for (ComponentReport componentReport : componentReports) {
            printSnap(componentReport);
        }
    }

    public static void printSnap(ComponentReport componentReport)
            throws SecurityException, IllegalArgumentException, NoSuchFieldException, IllegalAccessException {
        SecuritySnapshotReport report;
        String acctName;
        String acctTicker;
        
        if (componentReport instanceof SecuritySnapshotReport) {
            report = (SecuritySnapshotReport) componentReport;
            acctName = report.getName();
            acctTicker = report.getCurrencyWrapper().ticker;
        } else {
            CompositeReport compositeReport = (CompositeReport) componentReport;
            report = (SecuritySnapshotReport) compositeReport.getAggregateReport();
            acctName = compositeReport.getName();
            acctTicker = "NoTicker";
        }

        String tab = "\u0009";
        System.out.println("\n" + "Report: Snap" + "\n");

        System.out.println("Account: " + tab + acctName + tab + "Ticker:" + tab + acctTicker);
        DateRange dates = report.getDateRange();
        System.out.println("Snapshot Date: " + tab + dates.getSnapDateInt());

        System.out.println("Date: " + tab + report.getReturnMetricStartDateInt(SecurityReport.RMDayReturn) + tab
                + "Value: " + tab + report.getReturnMetric(SecurityReport.RMDayReturn));
        System.out.println("Date: " + tab + report.getReturnMetricStartDateInt(SecurityReport.RMWeekReturn) + tab
                + "Value: " + tab + report.getReturnMetric(SecurityReport.RMWeekReturn));
        System.out.println("Date: " + tab + report.getReturnMetricStartDateInt(SecurityReport.RMMonthReturn) + tab
                + "Value: " + tab + report.getReturnMetric(SecurityReport.RMMonthReturn));
        System.out.println("Date: " + tab + report.getReturnMetricStartDateInt(SecurityReport.RM3MonthReturn) + tab
                + "Value: " + tab + report.getReturnMetric(SecurityReport.RM3MonthReturn));
        System.out.println("Date: " + tab + report.getReturnMetricStartDateInt(SecurityReport.RMYTDReturn) + tab
                + "Value: " + tab + report.getReturnMetric(SecurityReport.RMYTDReturn));
        System.out.println("Date: " + tab + report.getReturnMetricStartDateInt(SecurityReport.RMYearReturn) + tab
                + "Value: " + tab + report.getReturnMetric(SecurityReport.RMYearReturn));
        System.out.println("Date: " + tab + report.getReturnMetricStartDateInt(SecurityReport.RM3YearReturn) + tab
                + "Value: " + tab + report.getReturnMetric(SecurityReport.RM3YearReturn));

        System.out.println("EndPos: " + tab + report.getSimpleMetric(SecurityReport.SMEndPosition) + tab
                + "EndPrice: " + tab + report.getSimpleMetric(SecurityReport.SMEndPrice) + tab
                + "EndValue:" + tab + report.getSimpleMetric(SecurityReport.SMEndValue));

        System.out.println("Income: " + tab + report.getSimpleMetric(SecurityReport.SMIncome) + tab
                + "Expense: " + tab + report.getSimpleMetric(SecurityReport.SMExpense));
    }
}
