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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;

/** produces panel for reports
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public class SecReportPanel extends javax.swing.JPanel { //implements ActionListener

    //variable declarations
    //MD Data
    Main extension;
    RootAccount root;
    BulkSecInfo currentInfo;
    //GUI Fields
    JLabel snapDateLabel = new javax.swing.JLabel("Report Snapshot Date");
    JDateFieldAlt snapDateField = new JDateFieldAlt();
    JLabel fromDateLabel = new javax.swing.JLabel("Report \"From\" Date");
    JDateFieldAlt fromDateField = new JDateFieldAlt();
    JLabel toDateLabel = new javax.swing.JLabel("Report \"To\" Date");
    JDateFieldAlt toDateField = new JDateFieldAlt();
    JButton dirChooserButton = new javax.swing.JButton("Set output folder");
    JTextField directoryOutputField = new javax.swing.JTextField(getLastFileUsed());
    JCheckBox snapReportCheckbox = new javax.swing.JCheckBox("Shapshot Report");
    JCheckBox fromToReportCheckbox = new javax.swing.JCheckBox("\"From/To\" Report");
    JCheckBox transActivityCheckbox = new javax.swing.JCheckBox("Transaction Activity");
    JCheckBox secPricesCheckbox = new javax.swing.JCheckBox("Securities Prices");
    JButton runReportsButton = new javax.swing.JButton("Run Reports");
    JTextField reportStatusField = new javax.swing.JTextField("Choose Reports to Run");

    JFileChooser chooser;

    boolean snapReportRun;
    boolean fromToReportRun;
    boolean transActivityReportRun;
    boolean secPricesReportRun;

    /** Creates new form NewJPanel */
    public SecReportPanel(Main extension, RootAccount root) {
        initComponents();
        this.extension = extension;
        this.root = root;
        this.currentInfo = new BulkSecInfo(extension, root);
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {

        //Set up  date input fields to defaults
        Date todayDate = DateUtils.convertToDate(DateUtils
                .getLastCurrentDateInt());
        
        Date yearAgoDate = DateUtils
                .convertToDate(DateUtils.addMonthsInt(DateUtils
                .convertToDateInt(todayDate), -12));

        snapDateField.setDate(todayDate);
        toDateField.setDate(todayDate);
        fromDateField.setDate(yearAgoDate);
        
        Dimension textFields = new Dimension(400, 14);
        directoryOutputField.setPreferredSize(textFields);
        reportStatusField.setPreferredSize(textFields);
        runReportsButton.setForeground(Color.red);
        //add action listeners

        dirChooserButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dirChooserButtonActionPerformed(evt);
            }
        });
        runReportsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runReportsButtonActionPerformed(evt);
            }
        });
        snapReportCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                snapReportCheckboxActionPerformed(evt);
            }
        });
        fromToReportCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fromToReportCheckboxActionPerformed(evt);
            }
        });
        transActivityCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                transActivityCheckboxActionPerformed(evt);
            }
        });
        secPricesCheckbox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                secPricesCheckboxActionPerformed(evt);
            }
        });



        //create and format blocks to load into main panel
        JPanel datePanel = new JPanel();
        JPanel checkBoxPanel = new JPanel();
        JPanel folderPanel = new JPanel();
        JPanel runPanel = new JPanel();

        //borders
        datePanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Report Dates"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        checkBoxPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Reports to Run"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        folderPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Download Location"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        runPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Report Status"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        //change all title colors
        ArrayList<JPanel> panels = new ArrayList<JPanel>(
                Arrays.asList(datePanel, checkBoxPanel, folderPanel, runPanel));
        for (Iterator<JPanel> it = panels.iterator(); it.hasNext();) {
            JPanel jPanel = it.next();
            CompoundBorder thisBorder = (CompoundBorder) jPanel.getBorder();
            TitledBorder outsideBorder = (TitledBorder) thisBorder.getOutsideBorder();
            outsideBorder.setTitleColor(new Color(100, 100, 100));
        }
        //layout panels
        //date Panel
        datePanel.setLayout(new GridLayout(3, 2));
        datePanel.add(snapDateLabel);
        datePanel.add(snapDateField);
        datePanel.add(fromDateLabel);
        datePanel.add(fromDateField);
        datePanel.add(toDateLabel);
        datePanel.add(toDateField);
        //Check Box Panel
        checkBoxPanel.setLayout(new GridLayout(4, 1));
        checkBoxPanel.add(snapReportCheckbox);
        checkBoxPanel.add(fromToReportCheckbox);
        checkBoxPanel.add(transActivityCheckbox);
        checkBoxPanel.add(secPricesCheckbox);
        // Folder Panel
        folderPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 0, 5);
        folderPanel.add(dirChooserButton, c);
        c.gridx = 0;
        c.gridy = 1;
        c.ipady = 12;
        folderPanel.add(directoryOutputField, c);

        // run panel (for program results)
        runPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.ipady = 12;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        runPanel.add(reportStatusField, c);

        //lay out main panel
        this.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        //add date panel
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(5, 5, 0, 5);
        this.add(datePanel, c);
        // add Check Box Panel
        c.gridx = 1;
        c.gridy = 0;
        this.add(checkBoxPanel, c);
        // add Folder Panel
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        this.add(folderPanel, c);
        // add Run Reports Button
        c.gridy = 2;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 5, 5, 5);
        c.gridwidth = 2;
        this.add(runReportsButton, c);
        // add Run Panel (Program Results)
        c.gridy = 3;
        c.anchor = GridBagConstraints.WEST;
        c.ipady = 12;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 5, 5, 5);
        this.add(runPanel, c);

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
            if (snapReportRun) {
                SecReportProd.getSnapReport(currentInfo, snapDateInt);
            }
            if (fromToReportRun) {
                SecReportProd.getFromToReport(currentInfo, fromDateInt, toDateInt);
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
