/*
 *  SecReportPanel.java
 *  Copyright (C) 2010 Dale Furrow
 *  dkfurrow@google.com
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, a copy may be found at
 *  http://www.gnu.org/licenses/lgpl.html
 */

package com.moneydance.modules.features.invextension;

import com.moneydance.apps.md.model.RootAccount;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import javax.swing.JFileChooser;

/** produces panel for reports
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public class SecReportPanel extends javax.swing.JPanel { //implements ActionListener

    JFileChooser chooser;
    Main extension;
    RootAccount root;
    boolean snapReportRun;
    boolean fromToReportRun;
    boolean transActivityReportRun;
    boolean secPricesReportRun;

    /** Creates new form NewJPanel */
    public SecReportPanel(Main extension, RootAccount root) {
        initComponents();
        this.extension = extension;
        this.root = root;
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        Date todayDate = DateUtils.convertToDate(DateUtils
                .getLastCurrentDateInt());
        
        Date yearAgoDate = DateUtils
                .convertToDate(DateUtils.addMonthsInt(DateUtils
                .convertToDateInt(todayDate), -12));

        

        snapDateLabel = new javax.swing.JLabel();
        snapDateField = new com.moneydance.modules.features.invextension.JDateFieldAlt(todayDate);
        fromDateLabel = new javax.swing.JLabel();
        fromDateField = new com.moneydance.modules.features.invextension.JDateFieldAlt(yearAgoDate);
        toDateLabel = new javax.swing.JLabel();
        toDateField = new com.moneydance.modules.features.invextension.JDateFieldAlt(todayDate);
        dirChooserButton = new javax.swing.JButton();
        runReportsButton = new javax.swing.JButton();
        directoryOutputField = new javax.swing.JTextField();
        snapReportCheckbox = new javax.swing.JCheckBox();
        fromToReportCheckbox = new javax.swing.JCheckBox();
        transActivityCheckbox = new javax.swing.JCheckBox();
        secPricesCheckbox = new javax.swing.JCheckBox();
        reportStatusField = new javax.swing.JTextField();
        snapDateLabel.setText("Report Snapshot Date");
        fromDateLabel.setText("Report \"From\" Date");
        toDateLabel.setText("Report \"To\" Date");

        dirChooserButton.setText("set output folder");
        dirChooserButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dirChooserButtonActionPerformed(evt);
            }
        });

        runReportsButton.setText("Run Reports");
        runReportsButton.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runReportsButtonActionPerformed(evt);
            }
        });
        
        directoryOutputField.setText(getLastFileUsed());
        reportStatusField.setText("Choose Reports to Run");


        snapReportCheckbox.setText("Shapshot Report");
        snapReportCheckbox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                snapReportCheckboxActionPerformed(evt);
            }
        });

        fromToReportCheckbox.setText("\"From/To\" Report");
        fromToReportCheckbox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fromToReportCheckboxActionPerformed(evt);
            }
        });

        transActivityCheckbox.setText("Transaction Activity");
        transActivityCheckbox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transActivityCheckboxActionPerformed(evt);
            }
        });

        secPricesCheckbox.setText("Securities Prices");
        secPricesCheckbox.addActionListener(new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secPricesCheckboxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addComponent(directoryOutputField, javax.swing.GroupLayout.Alignment.LEADING,
                javax.swing.GroupLayout.DEFAULT_SIZE, 446, Short.MAX_VALUE)
                .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(snapDateLabel).addComponent(fromDateLabel).addComponent(toDateLabel))
                .addGap(29, 29, 29).addGroup(layout.createParallelGroup(
                javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(toDateField)
                .addComponent(snapDateField).addComponent(fromDateField,
                javax.swing.GroupLayout.DEFAULT_SIZE, 133, Short.MAX_VALUE))).
                addComponent(dirChooserButton)).addPreferredGap(javax.swing.LayoutStyle
                .ComponentPlacement.RELATED, 56, Short.MAX_VALUE).addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false).addComponent(snapReportCheckbox).addComponent(fromToReportCheckbox).addComponent(transActivityCheckbox).addComponent(secPricesCheckbox))))).addGroup(layout.createSequentialGroup().addGap(200, 200, 200) //174
                .addComponent(runReportsButton)).addGroup(layout.createSequentialGroup().
                addContainerGap().addComponent(reportStatusField, javax.swing.GroupLayout.DEFAULT_SIZE, 446,
                Short.MAX_VALUE))).addContainerGap()));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addComponent(snapReportCheckbox)
                .addGap(18, 18, 18).addComponent(fromToReportCheckbox).addGap(18, 18, 18)
                .addComponent(transActivityCheckbox).addGap(18, 18, 18)
                .addComponent(secPricesCheckbox)).addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup().addGap(52, 52, 52) //38, 38, 38
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(fromDateLabel).addComponent(fromDateField,
                javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(snapDateLabel).addComponent(snapDateField,
                javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                javax.swing.GroupLayout.PREFERRED_SIZE))).addGap(24, 24, 24)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(toDateField, javax.swing.GroupLayout.PREFERRED_SIZE,
                javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(toDateLabel)).addGap(20, 20, 20).addComponent(dirChooserButton)))
                .addGap(29, 29, 29).addComponent(directoryOutputField,
                javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                javax.swing.GroupLayout.PREFERRED_SIZE).addGap(32, 32, 32)
                .addComponent(runReportsButton).addPreferredGap(javax.swing.LayoutStyle
                .ComponentPlacement.UNRELATED).addComponent(reportStatusField,
                javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                javax.swing.GroupLayout.PREFERRED_SIZE).addContainerGap(55, Short.MAX_VALUE)));
    } // end initcomponents

    private void dirChooserButtonActionPerformed(java.awt.event.ActionEvent evt) {
        showFileChooser();
    }

    private void runReportsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        reportStatusField.setText(null);
        reportStatusField.setText("Reports are running...");
        int fromDateInt =DateUtils.convertToDateInt(fromDateField.getDate());
        int toDateInt = DateUtils.convertToDateInt(toDateField.getDate());
        int snapDateInt = DateUtils.convertToDateInt(snapDateField.getDate());

        try {
            BulkSecInfo currentInfo = new BulkSecInfo(this.extension, this.root);

            if (snapReportRun) {
                ArrayList<String[]> snapReport = SecReportProd.getSnapReport(currentInfo, snapDateInt);
                File snapReportFile = new File(directoryOutputField.getText() + "\\SnapReport.csv");
                IOUtils.writeArrayListToCSV(RepSnap.listTransValuesSnapHeader(snapDateInt),
                        snapReport, snapReportFile);
                
            }
            if (fromToReportRun) {
//                ArrayList<String[]> fromToReport = SecReportProd.getFromToReport(currentInfo, fromDateInt, toDateInt);
                ArrayList<String[]> fromToReport = SecReportProd.getFromToReport(currentInfo, fromDateInt, toDateInt);
                File fromToReportFile = new File(directoryOutputField.getText() + "\\FromToReport.csv");
                IOUtils.writeArrayListToCSV(RepFromTo.listTransValuesFTHeader(fromDateInt,toDateInt), fromToReport, fromToReportFile);
            }

            if (transActivityReportRun) {
                ArrayList<String[]> transActivityReport = currentInfo.listTransValuesCumMap(currentInfo.transValuesCumMap);
                File transActivityReportFile = new File(directoryOutputField.getText() + "\\transActivityReport.csv");
                IOUtils.writeArrayListToCSV(TransValuesCum.listTransValuesCumHeader(),
                        transActivityReport, transActivityReportFile);
                
            }

            if (secPricesReportRun) {
                ArrayList<String[]> secPricesReport = BulkSecInfo.ListAllCurrenciesInfo(currentInfo.allCurrTypes);
                File secPricesReportFile = new File(directoryOutputField.getText() + "\\secPricesReport.csv");
                IOUtils.writeArrayListToCSV(BulkSecInfo.listCurrencySnapshotHeader(),
                        secPricesReport, secPricesReportFile);
            }
        } catch (Exception e) {

            File errorFile = new File(directoryOutputField.getText() + "\\errlog.txt");
            StringBuffer erLOG = getStackTrace(e);
            IOUtils.writeResultsToFile(erLOG, errorFile);
        }

        reportStatusField.setText(null);
        IOUtils.writeIniFile(directoryOutputField.getText());
        reportStatusField.setText("Reports have been run!");

    }

    private void snapReportCheckboxActionPerformed(java.awt.event.ActionEvent evt) {
        this.snapReportRun = false;
        if (snapReportCheckbox.isSelected()) {
            this.snapReportRun = true;
        }
    }

    private void fromToReportCheckboxActionPerformed(java.awt.event.ActionEvent evt) {
        this.fromToReportRun = false;
        if (fromToReportCheckbox.isSelected()) {
            this.fromToReportRun = true;
        }
    }

    private void transActivityCheckboxActionPerformed(java.awt.event.ActionEvent evt) {
        this.transActivityReportRun = false;
        if (transActivityCheckbox.isSelected()) {
            this.transActivityReportRun = true;
        }
    }

    private void secPricesCheckboxActionPerformed(java.awt.event.ActionEvent evt) {
        this.secPricesReportRun = false;
        if (secPricesCheckbox.isSelected()) {
            this.secPricesReportRun = true;
        }
    }

    public void showFileChooser() {
        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));

        chooser.setDialogTitle("Choose Output Directory");
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            System.out.println("getCurrentDirectory(): "
                    + chooser.getCurrentDirectory().getAbsolutePath());
            directoryOutputField.setText(null);
            directoryOutputField.setText(chooser.getCurrentDirectory().getAbsolutePath());

        } else {
            System.out.println("No Selection ");
        }
    }
    // Variables declaration - do not modify
    private javax.swing.JButton dirChooserButton;
    private javax.swing.JTextField directoryOutputField;
    private JDateFieldAlt fromDateField;
    private javax.swing.JLabel fromDateLabel;
    private javax.swing.JCheckBox fromToReportCheckbox;
    private javax.swing.JTextField reportStatusField;
    private javax.swing.JButton runReportsButton;
    private javax.swing.JCheckBox secPricesCheckbox;
    private JDateFieldAlt snapDateField;
    private javax.swing.JLabel snapDateLabel;
    private javax.swing.JCheckBox snapReportCheckbox;
    private JDateFieldAlt toDateField;
    private javax.swing.JLabel toDateLabel;
    private javax.swing.JCheckBox transActivityCheckbox;
    // End of variables declaration

     public static StringBuffer getStackTrace(Exception e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return new StringBuffer(sw.toString());
    }

     public String getLastFileUsed(){
         String filePath = new String();
         String defaultDir = new File(".").getAbsolutePath();
         File iniFile = new File(defaultDir + "invextension.ini");
         if(iniFile.exists()){
             filePath = IOUtils.readIniFile(iniFile);
             return filePath;
         } else{
             return defaultDir;
         }

     }

}
