/*
 * TestMethods.java
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

import java.util.LinkedList;

/**
 * Various test methods useful in development
 */
public class TestMethods {

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        try {
            Class reportClass = TotalSnapshotReport.class;
            LinkedList<Integer> viewHeaderTest = ReportConfig.getLinkedListFromString("0,1,3,5,6,10");
            AggregationController aggregationController = AggregationController.INVACCT;
            boolean outputSingle = false;
            String reportName = "TotalSnapshotReport1";
            DateRange dateRange = DateRange.getDefaultDateRange();
            int numFrozenColumns = 5;
            boolean closedPosHidden = true;


//            ReportConfig selectedReportConfig = new ReportConfig(reportClass,reportingName, aggregationMode, outputSingle,
//                    numFrozenColumns, closedPosHidden, viewHeaderTest, dateRange);
//            ReportConfig selectedReportConfig = ReportConfig.getDefaultReportConfig(reportClass);
//            System.out.println(selectedReportConfig);
//            selectedReportConfig.saveReportConfig();
//            ReportConfig newReportConfig = new ReportConfig(reportClass, reportingName);
//            System.out.println("Here is the saved report config");
//            System.out.println(newReportConfig.toString());
//            ReportConfig.printPrefNode(reportClass);
//            ReportConfig.clearAllReportConfigsForClass(reportClass);
//            ArrayList<ReportConfig> reportConfigs = ReportConfig.getReportConfigsForClass(reportClass);
//            System.out.println("report configs size: " + reportConfigs.size());
//            ReportConfig.printPrefNode(reportClass);
            ReportConfig.clearConfigNode();
            ReportConfig.printConfigNode();

//            for(ReportConfig thisReportConfig : reportConfigs){
//                System.out.println(thisReportConfig);
//                System.out.println("--------");
//
//            }
            System.out.println(reportClass.getPackage().getName());


        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

//        try {
//            Preferences prefsNode = Preferences.userRoot().node("/com/moneydance");
//            prefsNode.removeNode();
//            ReportConfig.printConfigNode();
//        } catch (BackingStoreException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }


    }


}
