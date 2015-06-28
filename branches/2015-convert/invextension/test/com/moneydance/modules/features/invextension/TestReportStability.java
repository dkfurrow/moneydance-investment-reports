/*
 * TestReportStability2.java
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

import com.moneydance.apps.md.controller.io.FileUtils;
import com.moneydance.apps.md.model.RootAccount;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import javax.swing.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import static org.junit.Assert.assertFalse;


@SuppressWarnings("unused")
public class TestReportStability extends JFrame {

    public static final DateRange testDateRange = new DateRange(20090601, 20100601, 20100601);
    public static final File testFile = new File("E:\\Temp\\testFile.csv");
    public static final int numberIterations = 50;
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final DecimalFormat decFormat = new DecimalFormat("#.000");
    private static final String startTime = "startTime";
    private static final String tab = "\t";
    public static Object[][][] reportsPanel;
    public static Object[][][] outputPanel;
    public static ReportConfig reportConfig;
    public static Class<? extends TotalReport> reportClass = TotalSnapshotReport.class;
    public static RootAccount root;
    public static BulkSecInfo currentInfo;
    public static LinkedList<String> modelHeader;
    public static Double threshold = 0.001;
    public static int reportLineLength;
    public static int reportLineQuantity;
    private static String testFileStr = "./resources/testMD02.moneydance/root.mdinternal";
    public static final File mdTestFile = new File(testFileStr);

    @Test
    public void testReportStability() {
        boolean errorFound;
        try {
            initializeTest();
            for (int k = 0; k < numberIterations; k++) {
                runReport(k);
            }
            populateOutputPanel();
            errorFound = evaluateOutputPanel();
            assertFalse(errorFound);
            System.out.println("Done");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void initializeTest() throws Exception {
        //initialize common elements
        root = FileUtils.readAccountsFromFile(mdTestFile, null);
        reportConfig = ReportConfig.getStandardReportConfig(reportClass);
        reportConfig.setAllExpenseAccountsToInvestment(root);
        reportConfig.setAllIncomeAccountsToInvestment(root);
        currentInfo = new BulkSecInfo(root, reportConfig);
        //initialize comparison object
        TotalReport report = (TotalReport) reportClass.getDeclaredConstructors()[0].newInstance(reportConfig);
        modelHeader = report.getModelHeader();
        report.calcReport(currentInfo);
        Object[][] reportObject = report.getReportTable();
        reportLineQuantity = reportObject.length;
        reportLineLength = reportObject[0].length;
        reportsPanel = new Object[reportLineQuantity][reportLineLength][numberIterations];
        outputPanel = new Object[reportLineQuantity][reportLineLength][2];
    }

    public static void runReport(int iterationCount) throws Exception {
        TotalReport report = (TotalReport) reportClass.getDeclaredConstructors()[0].newInstance(reportConfig);
        report.calcReport(currentInfo);
        Object[][] initialReportObject = report.getReportTable();
        Object[][] sortedReportObject = sortReportObject(initialReportObject);
        for (int i = 0; i < reportLineQuantity; i++) {
            for (int j = 0; j < reportLineLength; j++) {
                reportsPanel[i][j][iterationCount] = sortedReportObject[i][j];
            }
        }
    }

    private static Object[][] sortReportObject(Object[][] initialReportObject) {
        int lineLength = initialReportObject[0].length;
        ArrayList<ReportLine> reportLineList = new ArrayList<>();
        Object[][] sortedReportLines = new Object[initialReportObject.length][lineLength];
        for (Object[] reportLine : initialReportObject) {
            reportLineList.add(new ReportLine(reportLine));
        }
        Collections.sort(reportLineList);
        for (int i = 0; i < reportLineList.size(); i++) {
            Object[] reportline = reportLineList.get(i).line;
            System.arraycopy(reportline, 0, sortedReportLines[i], 0, reportline.length);
        }
        return sortedReportLines;

    }

    public static void populateOutputPanel() {
        for (int i = 0; i < reportLineQuantity; i++) {
            for (int j = 0; j < reportLineLength; j++) {
                Object firstObject = reportsPanel[i][j][0];
                if (!(firstObject instanceof Number)) {
                    outputPanel[i][j][0] = firstObject;
                    outputPanel[i][j][1] = firstObject;
                } else {
                    for (int k = 0; k < numberIterations; k++) {
                        Object object = reportsPanel[i][j][k];
                        Double value = 0.0;
                        if (object instanceof Double) {
                            value = (Double) reportsPanel[i][j][k];
                        }
                        if (k == 0) {
                            outputPanel[i][j][0] = value;
                            outputPanel[i][j][1] = value;
                        } else {
                            outputPanel[i][j][0] = Math.max((Double) outputPanel[i][j][0], value);
                            outputPanel[i][j][1] = Math.min((Double) outputPanel[i][j][1], value);
                        }
                    }
                }
            }
        }
    }

    public static boolean evaluateOutputPanel() {
        boolean errorFound = false;
        InvestmentAccountWrapper investmentAccountWrapper = null;
        SecurityAccountWrapper securityAccountWrapper = null;
        String outputStr;
        for (int i = 0; i < reportLineQuantity; i++) {
            if (outputPanel[i][0][0] != null && outputPanel[i][0][0] instanceof InvestmentAccountWrapper) {
                investmentAccountWrapper = (InvestmentAccountWrapper) outputPanel[i][0][0];
            }
            if (outputPanel[i][1][0] != null && outputPanel[i][1][0] instanceof SecurityAccountWrapper) {
                securityAccountWrapper = (SecurityAccountWrapper) outputPanel[i][1][0];
            }
            outputStr = (investmentAccountWrapper == null ? "Null" : investmentAccountWrapper.getName()) + " : "
                    + (securityAccountWrapper == null ? "Null" : securityAccountWrapper.getName());
            for (int j = 0; j < reportLineLength; j++) {
                if (outputPanel[i][j][0] instanceof Double) {
                    Double hiLoDiff = (Double) outputPanel[i][j][0] - (Double) outputPanel[i][j][1];
                    if (hiLoDiff > threshold) {
                        errorFound = true;
                        String columnHeader = modelHeader.get(j);
                        System.out.println("Row: " + outputStr + " Col: " + columnHeader + " : " + "Error: " +
                                " High Value: " + outputPanel[i][j][0] + " Low Value: " + outputPanel[i][j][1]);
                    }
                }

            }
        }
        return errorFound;
    }



    static class ReportLine implements Comparable<ReportLine> {
        Object[] line;

        ReportLine(Object[] line) {
            this.line = line;
        }


        @Override
        public int compareTo(@NotNull ReportLine operand) {
            InvestmentAccountWrapper thisInvestmentAccountWrapper = null;
            SecurityAccountWrapper thisSecurityAccountWrapper = null;
            InvestmentAccountWrapper operandInvestmentAccountWrapper = null;
            SecurityAccountWrapper operandSecurityAccountWrapper = null;
            if (this.line[0] != null && this.line[0] instanceof InvestmentAccountWrapper) {
                thisInvestmentAccountWrapper = (InvestmentAccountWrapper) this.line[0];
            }
            if (this.line[1] != null && this.line[1] instanceof SecurityAccountWrapper) {
                thisSecurityAccountWrapper = (SecurityAccountWrapper) this.line[1];
            }
            if (operand.line[0] != null && operand.line[0] instanceof InvestmentAccountWrapper) {
                operandInvestmentAccountWrapper = (InvestmentAccountWrapper) operand.line[0];
            }
            if (operand.line[1] != null && operand.line[1] instanceof SecurityAccountWrapper) {
                operandSecurityAccountWrapper = (SecurityAccountWrapper) operand.line[1];
            }
            assert thisInvestmentAccountWrapper != null;
            assert operandInvestmentAccountWrapper != null;
            if (!(thisInvestmentAccountWrapper.getName().equals(operandInvestmentAccountWrapper.getName()))) {
                return thisInvestmentAccountWrapper.getName().compareTo(operandInvestmentAccountWrapper.getName());
            } else {
                assert thisSecurityAccountWrapper != null;
                assert operandSecurityAccountWrapper != null;
                return thisSecurityAccountWrapper.getName().compareTo(operandSecurityAccountWrapper.getName());
            }
        }
    }

}
