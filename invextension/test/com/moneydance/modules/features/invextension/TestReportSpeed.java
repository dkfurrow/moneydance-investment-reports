/*
 * TestReportSpeed.java
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

import javax.swing.*;
import java.io.File;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("unused")
public class TestReportSpeed extends JFrame {

    public static final DateRange testDateRange = new DateRange(20090601, 20100601, 20100601);
    public static final File testFile = new File("E:\\Temp\\testFile.csv");
    private static final long serialVersionUID = -2315625753772573103L;
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    /**
     *
     */
    //
    //
    private static String testFileStr = "./resources/testMD02.moneydance/root.mdinternal";
    private static String testFileStr1 = "E:\\\\RECORDS\\moneydance\\\\Test\\\\20141014test-2015.moneydance";
    private static String testFileStr2 = "E:\\\\RECORDS\\moneydance\\\\Test\\\\TestSave.moneydance\\\\root.mdinternal";
    private static final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final DecimalFormat decFormat = new DecimalFormat("#.000");
    private static LinkedHashMap<String, Date> recordTimes = new LinkedHashMap<>();
    private static final String startTime = "startTime";
    public static final File mdTestFolder = new File(testFileStr1);
    private static final String tab = "\t";

    public static void main(String[] args) throws Exception {
        addRecordTime(startTime);
        BulkSecInfoTest.MDFileInfo mdFileInfo = loadRootAccountFromFolder();
        Account root = mdFileInfo.getRootAccount();
        addRecordTime("fileLoaded");
        ReportConfig reportConfig = ReportConfig.getStandardReportConfig(TotalSnapshotReport.class);
//        reportConfig.setUseAverageCostBasis(false); //Was test--revised GainsLotMatchCalc to limit unnecessary calls
        BulkSecInfo currentInfo =  new BulkSecInfo(mdFileInfo.getAccountBook(), reportConfig);
        addRecordTime("bulkInfoLoaded");
        reportConfig.setDateRange(testDateRange);
        TotalReport report = new TotalSnapshotReport(reportConfig, currentInfo);
        report.calcReport();
        addRecordTime("reportCalculated");
        report.displayReport();
        addRecordTime("endProcess");
        describeBulkSecInfo(currentInfo);
        displayResults();
    }

    /**
     * initializes AccountBook from folder
     * (note--must use java 1.8--otherwise get
     * ava.security.NoSuchAlgorithmException: PBKDF2WithHmacSHA512 SecretKeyFactory not available)
     * @throws Exception
     */
    public static BulkSecInfoTest.MDFileInfo loadRootAccountFromFolder() throws Exception {


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
        return new BulkSecInfoTest.MDFileInfo(accountBook, accountBook.getRootAccount());
    }




    public static void addRecordTime(String mileStone){
        recordTimes.put(mileStone, new Date());
    }

    public static void displayResults(){
        Date startDT = null;
        for(String mileStone: recordTimes.keySet()){
            Date thisCurrentDT = recordTimes.get(mileStone);
            if(mileStone.equals(startTime)) startDT = thisCurrentDT;
            String formattedDTStr = sdf.format(thisCurrentDT.getTime());
            String secondsFromStartStr = !mileStone.equals(startTime) ? getTimeBetween(startDT, thisCurrentDT) : "";
            System.out.println(mileStone + tab + formattedDTStr + tab +  secondsFromStartStr);
        }
        long diff = recordTimes.get("endProcess").getTime() - recordTimes.get("fileLoaded").getTime();
        DecimalFormat df = new DecimalFormat("##.##");

        System.out.println("From File Load to End: " + df.format((double) diff / 1000.0)   + " seconds");
    }

    public static String getTimeBetween(Date startDT, Date currentDT){

        long diffInMils = currentDT.getTime() - startDT.getTime();
        return decFormat.format(new Double(diffInMils) / 1000);
    }

    public static void describeBulkSecInfo(BulkSecInfo bulkSecInfo) throws Exception {
        int totalSecurities = 0;
        int totalTransactions = 0;
        int investmentCount = 0;
        for (InvestmentAccountWrapper investmentAccountWrapper : bulkSecInfo.getInvestmentWrappers()){
            int securityQuantity = investmentAccountWrapper.getSecurityAccountWrappers().size();
            int transactionQuantity  = investmentAccountWrapper.getTransactionValues().size();
            String investmentAcctCounterName = "Investment Acct: " + (investmentCount + 1);
            System.out.println( investmentAcctCounterName+ tab + "Security Count: " + tab +
                    securityQuantity + tab + "Transaction Count: " + tab + transactionQuantity);
            totalSecurities += securityQuantity;
            totalTransactions += transactionQuantity;
            investmentCount ++;
        }
        System.out.println( "Total Investments: "+ investmentCount + tab +
                "Total Securities: " + tab + totalSecurities + tab +
                "Total Transactions: " + tab + totalTransactions);
    }


}
