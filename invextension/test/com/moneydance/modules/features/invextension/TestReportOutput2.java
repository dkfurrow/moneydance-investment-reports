/*
 * TestReportOutput2.java
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

import com.infinitekind.moneydance.model.Account;
import com.infinitekind.moneydance.model.AccountBook;
import com.moneydance.apps.md.controller.AccountBookWrapper;
import com.moneydance.apps.md.controller.io.AccountBookUtil;
import com.moneydance.apps.md.controller.io.FileUtils;


import java.awt.*;
import java.io.File;
import java.util.ArrayList;


/**
 * Struct to hold AccountBook, RootAccount
 */


@SuppressWarnings("unused")
public class TestReportOutput2 {
    public static final String mdTestFolderStr = "./resources/testMD02.moneydance";
    public static final File mdTestFolder = new File(mdTestFolderStr);
    // Stored Test Database
    public static final int numFrozenColumns = 5; //Irrelevant for testing purposes
    public static final boolean closedPosHidden = true; //Irrelevant for testing purposes
    public static final String reportName = "TestName"; //Irrelevant for testing purposes
    private static String testFileStr = "./invextension/resources/testMD02.moneydance";
    //
    public static final DateRange testDateRange = new DateRange(20090601, 20100601, 20100601);
    private static String outputDirectory = "D://Temp";

    private static File getOutputFile(String fileName) {
        return new File(TestReportOutput2.outputDirectory, fileName);
    }

    public static TestReportOutput2.MDFileInfo loadRootAccountFromFolder() throws Exception {

        System.out.println("Test Folder Exists? " + mdTestFolder.isDirectory());
        System.out.println("Loading Wrapper...");
        AccountBookWrapper wrapper = AccountBookWrapper.wrapperForFolder(mdTestFolder);

        // must add this section or get null pointer error
        ArrayList<File> folderFiles = new ArrayList<>();
        folderFiles.add(mdTestFolder);
        AccountBookUtil.INTERNAL_FOLDER_CONTAINERS = folderFiles;

        System.out.println("Doing Initial Load of AccountBook...");
        wrapper.loadDataModel(null);
        AccountBook accountBook = wrapper.getBook();
        int accountCount = accountBook.getRootAccount().getSubAccounts().size();
        long transactionCount = accountBook.getTransactionSet().getTransactionCount();
        System.out.println("AccountBook Initialized...Number of Accounts: " + accountCount + ", with "
                + transactionCount + " transactions");
        return new TestReportOutput2.MDFileInfo(accountBook, accountBook.getRootAccount());
    }

    public static class MDFileInfo{
        AccountBook accountBook;
        Account rootAccount;


        public MDFileInfo(AccountBook accountBook, Account rootAccount) {
            this.accountBook = accountBook;
            this.rootAccount = rootAccount;
        }

        public AccountBook getAccountBook() {
            return accountBook;
        }

        public Account getRootAccount() {
            return rootAccount;
        }
    }


    //
    public static void main(String[] args) throws Exception {
        TestReportOutput2.MDFileInfo mdFileInfo = loadRootAccountFromFolder();
        Account root = mdFileInfo.getRootAccount();
        ReportConfig reportConfig = ReportConfig.getTestReportConfig(root, false,
                AggregationController.INVACCT);
        BulkSecInfo currentInfo = new BulkSecInfo(mdFileInfo.getAccountBook(), reportConfig);
        ArrayList<String[]> transActivityReport =  currentInfo.listAllTransValues();


        File transActivityReportFile = getOutputFile("transActivityReport.csv");
        IOUtils.writeArrayListToCSV(TransactionValues.listTransValuesHeader(),
                transActivityReport, transActivityReportFile);
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
            desktop.browse(new File(TestReportOutput2.outputDirectory).toURI());
        }

//        mdData.loadMDFile(mdTestFolder, null);
//        mdData.initializeMDDataHeadless(false);
//        Account root = FileUtils.readAccountsFromFile(mdTestFolder, null);

//        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class, "StandardFTTest");
//        ReportConfig reportConfig = ReportConfig.getStandardReportConfig(TotalFromToReport.class);
//        reportConfig.setDateRange(testDateRange);
//        reportConfig.setAllExpenseAccountsToInvestment(root);
//        reportConfig.setAllIncomeAccountsToInvestment(root);
//        System.out.println(reportConfig.toString());
//        TotalReport report = new TotalFromToReport(reportConfig);
//        BulkSecInfo currentInfo = new BulkSecInfo(root, reportConfig);
//        report.calcReport(currentInfo);
//        report.displayReport();



    }


//    private static void printReport(Object[][] report)
//            throws NoSuchFieldException, IllegalAccessException {
//
//        StringBuffer outBuffer = writeObjectToStringBuffer(report);
//        IOUtils.writeResultsToFile(outBuffer, testFile);
//        System.out.println("Finished!");
//    }


    public static StringBuffer writeObjectToStringBuffer(Object[][] object) {
        StringBuffer outBuffer = new StringBuffer();
        for (Object[] objects : object) {
            for (int j = 0; j < objects.length; j++) {
                Object element = objects[j] == null ? "*NULL*" : objects[j];
                if (j == objects.length - 1) {
                    outBuffer.append(element.toString()).append("\r\n");
                } else {
                    outBuffer.append(element.toString()).append(",");
                }
            }
        }
        return outBuffer;

    }


}
