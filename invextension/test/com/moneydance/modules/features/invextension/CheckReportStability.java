/*
 * TestReportStability.java
 * Copyright (c) 2015, Dale K. Furrow
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

import com.infinitekind.moneydance.model.Account;

import javax.swing.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;


@SuppressWarnings("unused")
public class CheckReportStability extends JFrame {

    public static final DateRange testDateRange = new DateRange(20090601, 20100601, 20100601);
    public static final File testFile = new File("E:\\Temp\\testFile.csv");
    private static final long serialVersionUID = -2315625753772573103L;
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final DecimalFormat decFormat = new DecimalFormat("#.000");
    private static final String startTime = "startTime";
    private static final String tab = "\t";
    /**
     *
     */
    //
    //
    private static String testFileStr = "./resources/testMD02.moneydance/root.mdinternal";
    public static final File mdTestFile = new File(testFileStr);
    private static String testFileStr1 = "E:\\\\RECORDS\\moneydance\\\\Test\\\\20141014test.moneydance\\\\root.mdinternal";
    private static String testFileStr2 = "E:\\\\RECORDS\\moneydance\\\\Test\\\\TestSave.moneydance\\\\root.mdinternal";
    private static LinkedHashMap<String, Date> recordTimes = new LinkedHashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("Security" + tab + "TR All" + tab + "Ann Ret All");
        for(int i = 0; i < 49; i++){
            runReportFromFile();
        }
    }

    private static void runReportFromFile() throws Exception {
        BulkSecInfoTest.MDFileInfo mdFileInfo = BulkSecInfoTest.loadRootAccountFromFolder();
        Account root = mdFileInfo.getRootAccount();

        Class testClass = TotalSnapshotReport.class;
        ReportConfig reportConfig = ReportConfig.getStandardReportConfig(testClass);
        reportConfig.setDateRange(testDateRange);
        reportConfig.setAllExpenseAccountsToInvestment(root);
        reportConfig.setAllIncomeAccountsToInvestment(root);
        BulkSecInfo currentInfo = new BulkSecInfo(mdFileInfo.getAccountBook(), reportConfig);
//        System.out.println(reportConfig.toString());
        TotalReport report = new TotalSnapshotReport(reportConfig, currentInfo);
        report.calcReport();
        Object[][] reportObject = report.getReportTable();
        printSelectedData(reportObject);
    }


    public static void printSelectedData(Object[][] inputObject) {

        StringBuffer sb = new StringBuffer();

        for (Object[] objs : inputObject) {
            InvestmentAccountWrapper investmentAccountWrapper = (InvestmentAccountWrapper) objs[0];
            SecurityAccountWrapper securityAccountWrapper = (SecurityAccountWrapper) objs[1];
            if (investmentAccountWrapper.getName().trim().equals("Accounts-ALL") &&
                    securityAccountWrapper.getName().trim().equals("All CASH")) {
                System.out.println("Final: " + investmentAccountWrapper.getName() + ":" + securityAccountWrapper.getName() + tab + objs[24].toString()
                        + tab + objs[25].toString());
            }
        }
    }

}
