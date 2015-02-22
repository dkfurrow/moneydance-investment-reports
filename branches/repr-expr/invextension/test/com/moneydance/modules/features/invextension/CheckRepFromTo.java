/*
 * CheckRepFromTo.java
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
 * "From/To" Report to allow for auditing
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class CheckRepFromTo {
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final DateRange dateRange = new DateRange(fromDateInt, toDateInt, toDateInt);

    public static void main(String[] args) throws Exception {
        BulkSecInfo currentInfo = BulkSecInfoTest.getBaseSecurityInfoAvgCost();

        int numFrozenColumns = 5; //irrelevant for purposes of test
        boolean closedPosHidden = true;//irrelevant for purposes of test
        AggregationController aggregationController = AggregationController.INVACCT;
        boolean rptOutputSingle = false;
        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class, "Test Report",
                true, aggregationController, rptOutputSingle, numFrozenColumns, closedPosHidden,
                ReportConfig.getDefaultViewHeader(TotalFromToReport.MODEL_HEADER),
                ReportConfig.getDefaultExcludedAccounts(), ReportConfig.getDefaultInvestmentExpenseAccounts(),
                ReportConfig.getDefaultInvestmentIncomeAccounts(),dateRange);
        reportConfig.setAllExpenseAccountsToInvestment(currentInfo.getRoot());
        reportConfig.setAllIncomeAccountsToInvestment(currentInfo.getRoot());
        TotalFromToReport fromToReport = new TotalFromToReport(reportConfig);
        fromToReport.calcReport(currentInfo);
        ArrayList<ComponentReport> componentReports = fromToReport.getReports();
        for (ComponentReport componentReport : componentReports) {
            printFromTo(componentReport);
        }
    }

    public static void printFromTo(ComponentReport componentReport) throws Exception {
        SecurityFromToReport report;
        String acctName;
        String acctTicker;

        if (componentReport instanceof SecurityFromToReport) {
            report = (SecurityFromToReport) componentReport;
            acctName = report.getName();
            acctTicker = report.getCurrencyWrapper().ticker;
        } else {
            CompositeReport compositeReport = (CompositeReport) componentReport;
            report = (SecurityFromToReport) compositeReport.getAggregateReport();
            acctName = compositeReport.getName();
            acctTicker = "NoTicker";
        }

        String tab = "\u0009";
        System.out.println("Report: From/To" + "\n");

        System.out.println("Account: " + tab + acctName + tab + "Ticker:" + tab + acctTicker);
        DateRange dates = report.getDateRange();
        System.out.println("From: " + tab + dates.getFromDateInt() + tab
                + "To: " + tab + dates.getToDateInt());

        System.out.println("StartPos: " + tab + report.getSimpleMetric(SecurityReport.SMStartPosition) + tab
                + "StartPrice: " + tab + report.getSimpleMetric(SecurityReport.SMStartPrice) + tab
                + "StartValue:" + tab + report.getSimpleMetric(SecurityReport.SMStartValue));
        System.out.println("EndPos: " + tab + report.getSimpleMetric(SecurityReport.SMEndPosition) + tab
                + "EndPrice: " + tab + report.getSimpleMetric(SecurityReport.SMEndPrice) + tab
                + "EndValue:" + tab + report.getSimpleMetric(SecurityReport.SMEndValue));
        System.out.println("Buy: " + tab + report.getSimpleMetric(SecurityReport.SMBuy) + tab
                + "Sell: " + tab + report.getSimpleMetric(SecurityReport.SMSell) + tab
                + "Short:" + tab + report.getSimpleMetric(SecurityReport.SMShortSell) + tab
                + "CoverShort:" + tab + report.getSimpleMetric(SecurityReport.SMCoveredShort));
        System.out.println("Income: " + tab + report.getSimpleMetric(SecurityReport.SMIncome) + tab
                + "Expense: " + tab + report.getSimpleMetric(SecurityReport.SMExpense));
        System.out.println("LongBasis: " + tab + report.getSimpleMetric(SecurityReport.SMLongBasis) + tab
                + "ShortBasis: " + tab + report.getSimpleMetric(SecurityReport.SMShortBasis));
        System.out.println("RealizedGain: " + tab + report.getSimpleMetric(SecurityReport.SMRealizedGain) + tab
                + "UnrealizedGain: " + tab + report.getSimpleMetric(SecurityReport.SMUnrealizedGain) + tab
                + "TotalGain:" + tab + report.getSimpleMetric(SecurityReport.SMTotalGain));

        System.out.println("\n" + "mdReturn: " + tab + report.getReturnMetric(SecurityReport.RMAllReturn) + tab
                + "AnnualReturn: " + tab + report.getReturnMetric(SecurityReport.RMAnnualReturn));
        System.out.println("\n");
    }
}
