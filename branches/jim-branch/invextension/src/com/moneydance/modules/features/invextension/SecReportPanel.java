/* SecReportPanel.java
 * Copyright 2011 Dale K. Furrow . All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.moneydance.modules.features.invextension;

import com.jgoodies.forms.factories.Borders.EmptyBorder;
import com.moneydance.apps.md.model.RootAccount;
import com.moneydance.awt.AwtUtil;
import com.moneydance.awt.JDateField;
import com.moneydance.util.CustomDateFormat;
import com.moneydance.modules.features.invextension.ReportTable.ColSizeOption;

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
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;


/** produces panel for reports
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public class SecReportPanel extends javax.swing.JPanel { //implements ActionListener
    private static final long serialVersionUID = 1020488211446251526L;
    private String iniFilePath
    = System.getProperty("user.home") + "\\.moneydance\\invextension.ini";

    //variable declarations
    //MD Data
    private RootAccount root;
    private BulkSecInfo currentInfo;

    //GUI Fields
    private CustomDateFormat df = new CustomDateFormat("M/d/yyyy");
    private JLabel snapDateLabel = new javax.swing.JLabel("Report Snapshot Date");
    private JDateField snapDateField = new JDateField(df);
    private JLabel fromDateLabel = new javax.swing.JLabel("Report \"From\" Date");
    private JDateField fromDateField = new JDateField(df);
    private JLabel toDateLabel = new javax.swing.JLabel("Report \"To\" Date");
    private JDateField toDateField = new JDateField(df);
    private JButton dirChooserButton = new javax.swing.JButton("Set output folder");
    private JTextField directoryOutputField = new javax.swing.JTextField(getPreviousDirectory());
    private JCheckBox snapReportCheckbox = new javax.swing.JCheckBox("Shapshot Report");
    private JCheckBox fromToReportCheckbox = new javax.swing.JCheckBox("\"From/To\" Report");
    private JCheckBox transActivityCheckbox = new javax.swing.JCheckBox("Transaction Activity");
    private JCheckBox secPricesCheckbox = new javax.swing.JCheckBox("Securities Prices");
    private JButton runReportsButton = new javax.swing.JButton("Run Reports");
    private JTextField reportStatusField = new javax.swing.JTextField("Choose Reports to Run");

    private JFileChooser chooser;


    /** Creates new form NewJPanel */
    public SecReportPanel(RootAccount root) {
        initComponents();
        this.root = root;
        if(root != null) {
            this.currentInfo = new BulkSecInfo(root);
        } else {
            this.currentInfo = null;
        }
    }


    private void initComponents() {
        // Defaults for date input fields
        int todayDateInt = DateUtils.getLastCurrentDateInt();
        int yearAgoDateInt = DateUtils.addMonthsInt(todayDateInt, -12);

        snapDateField.setDateInt(todayDateInt);
        toDateField.setDateInt(todayDateInt);
        fromDateField.setDateInt(yearAgoDateInt);

        snapDateField.setHorizontalAlignment(JTextField.RIGHT); //alignment
        toDateField.setHorizontalAlignment(JTextField.RIGHT);
        fromDateField.setHorizontalAlignment(JTextField.RIGHT);

        // Set text field width, button color
        Dimension textFields
            // Arbitrary max width for text fields
            = new Dimension(Math.max(directoryOutputField.getPreferredSize().width, 400), 14);
        directoryOutputField.setPreferredSize(textFields);
        reportStatusField.setPreferredSize(textFields);
        runReportsButton.setForeground(Color.red);

        // Add action listeners

        // button action listeners
        dirChooserButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    dirChooserButtonActionPerformed(evt);
                }});
        runReportsButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    runReportsButtonActionPerformed(evt);
                }});

        // Create and format sub-panels to load into main panel
        JPanel datePanel = new JPanel();
        JPanel checkBoxPanel = new JPanel();
        JPanel folderPanel = new JPanel();
        JPanel runPanel = new JPanel();

        // Set all panel borders the same
        String[] titles = { "Report Dates", "Reports to Run",
                            "Download Location", "Report Status" };
        JPanel[] panels = { datePanel, checkBoxPanel, folderPanel, runPanel };

        for (int i = 0; i < panels.length; i++) {
            JPanel panel = panels[i];
            String title = titles[i];
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory .createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            panel.setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        }

        // Layout sub-panels

        // date sub-panel
        datePanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 5, 0, 5); // left-right padding only
        c.weighty = 1;                     //justify everything vertically

        // labels
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1;          // fill space w/ labels
        c.gridx = 0;
        datePanel.add(snapDateLabel, c);
        c.gridy = 1;
        datePanel.add(fromDateLabel, c);
        c.gridy = 2;
        datePanel.add(toDateLabel, c);

        // date fields
        c.weightx = 0;          //normal-width date fields
        c.gridx = 1;
        c.gridy = 0;
        datePanel.add(snapDateField, c);
        c.gridy = 1;
        datePanel.add(fromDateField, c);
        c.gridy = 2;
        datePanel.add(toDateField, c);

        // check box sub-panel
        checkBoxPanel.setLayout(new GridLayout(4, 1));
        checkBoxPanel.add(snapReportCheckbox);
        checkBoxPanel.add(fromToReportCheckbox);
        checkBoxPanel.add(transActivityCheckbox);
        checkBoxPanel.add(secPricesCheckbox);

        // directory sub-panel
        folderPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 0, 5);
        folderPanel.add(dirChooserButton, c);
        c.gridx = 0;
        c.gridy = 1;
        c.ipady = 12;
        folderPanel.add(directoryOutputField, c);

        // run sub-panel (for program results)
        runPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.ipady = 12;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        runPanel.add(reportStatusField, c);

        // lay out main panel
        this.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; //fill all components to button

        // add date sub-panel
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(5, 5, 5, 5);
        this.add(datePanel, c);

        // add check box sub-panel
        c.gridx = 1;
        c.gridy = 0;
        this.add(checkBoxPanel, c);

        // all remaining components center, panel-width
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 2;

        // add folder sub-panel
        c.gridx = 0;
        c.gridy = 1;
        this.add(folderPanel, c);

        // add Run Reports Button
        c.fill = GridBagConstraints.NONE; //want normal-sized button
        c.gridy = 2;
        this.add(runReportsButton, c);

        // add Run sub-panel (Program Results)
        c.fill = GridBagConstraints.BOTH; // back to fill all components
        c.gridy = 3;
        this.add(runPanel, c);

    }


    private void dirChooserButtonActionPerformed(java.awt.event.ActionEvent evt) {
        showFileChooser();
    }


    private void runReportsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        reportStatusField.setText("Reports are running...");

        try {
            if (snapReportCheckbox.isSelected()) {
                int snapDateInt = snapDateField.getDateInt();
                FullSecurityReport report = new FullSnapshotReport(currentInfo, snapDateInt);
                displayReport(report);
            }

            if (fromToReportCheckbox.isSelected()) {
                int fromDateInt = fromDateField.getDateInt();
                int toDateInt = toDateField.getDateInt();
                FullSecurityReport report
                    = new FullFromToReport(currentInfo, fromDateInt, toDateInt);
                displayReport(report);
            }

            if (transActivityCheckbox.isSelected()) {
                ArrayList<String[]> transActivityReport
                    = currentInfo.listTransValuesCumMap(currentInfo.transValuesCumMap);
                File transActivityReportFile
                    = new File(directoryOutputField.getText() + "\\transActivityReport.csv");
                IOUtils.writeArrayListToCSV(TransValuesCum.listTransValuesCumHeader(),
                                            transActivityReport, transActivityReportFile);

            }

            if (secPricesCheckbox.isSelected()) {
                ArrayList<String[]> secPricesReport
                    = BulkSecInfo.ListAllCurrenciesInfo(currentInfo.allCurrTypes);
                File secPricesReportFile
                    = new File(directoryOutputField.getText() + "\\secPricesReport.csv");
                IOUtils.writeArrayListToCSV(BulkSecInfo.listCurrencySnapshotHeader(),
                                            secPricesReport,
                                            secPricesReportFile);
            }
        } catch (Exception e) {
            File errorFile = new File(directoryOutputField.getText() + "\\errlog.txt");
            StringBuffer erLOG = getStackTrace(e);
            IOUtils.writeResultsToFile(erLOG, errorFile);
        }

        IOUtils.writeIniFile(iniFilePath, directoryOutputField.getText());
        reportStatusField.setText("Reports have been run!");
    }


    private void displayReport(FullSecurityReport report) {
        ReportTableModel model
            = new ReportTableModel(report.getReportTable(), report.getReportHeader());

        ReportTable.CreateAndShowTable(model,
                                       report.getColumnTypes(),
                                       report.getClosedPosColumn(),
                                       ColSizeOption.MAXCONTCOLRESIZE,
                                       report.getFrozenColumn(),
                                       report.getReportTitle());
    }


    public static void main(String[] args) {
        JFrame testFrame = new JFrame("Test Investment Reports Panel");
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SecReportPanel testPanel = new SecReportPanel(null);
        testPanel.setOpaque(true);
        testFrame.setContentPane(testPanel);
        testFrame.pack();
        AwtUtil.centerWindow(testFrame);
        testFrame.setVisible(true);
    }


    public void showFileChooser() {
        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(directoryOutputField.getText()));

        chooser.setDialogTitle("Choose Output Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            System.out.println("getCurrentDirectory(): "
                               + chooser.getSelectedFile().getAbsolutePath());
            directoryOutputField.setText(chooser.getSelectedFile().getAbsolutePath());
        } else {
            System.out.println("No Selection ");
        }
    }


    public static StringBuffer getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return new StringBuffer(sw.toString());
    }


    public String getPreviousDirectory() {
        File iniFile = new File(iniFilePath);
        if(iniFile.exists()) {
            return IOUtils.readIniFile(iniFile);
        } else {
            return System.getProperty("user.home");
        }
    }


    /*
     * Class provides a generic TableModel which receives data from the
     * reporting methods above.
     */
    static class ReportTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 5838000411345317854L;
        public String[] columnNames;
        public Object[][] data;

        public ReportTableModel(Object[][] body, String[] columnNames) {
            super();

            assert(body != null);
            assert(columnNames != null);

            this.columnNames = columnNames;
            this.data = body;
        }


        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public String[] getColumnNames() {
            return this.columnNames;
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        // Allows table to be editable
        public boolean isCellEditable(int row, int col) {
            // Note that the data/cell address is constant, no matter where the
            // cell appears onscreen.
            return col >= 2;
        }

        // allows table to be editable
        public void setValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }
    }
}
