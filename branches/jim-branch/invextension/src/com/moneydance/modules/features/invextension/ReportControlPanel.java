/* ReportControlPanel.java
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

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Locale;
import java.util.prefs.Preferences;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;

import com.moneydance.apps.md.model.RootAccount;
import com.moneydance.awt.AwtUtil;
import com.moneydance.awt.JDateField;
import com.moneydance.util.CustomDateFormat;

import com.moneydance.modules.features.invextension.FormattedTable.ColSizeOption;


/** produces panel for reports
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public class ReportControlPanel extends javax.swing.JPanel { //implements ActionListener
    private static final long serialVersionUID = 1020488211446251526L;
    private RootAccount root;
    private Preferences prefs = Prefs.reportPrefs;
    private static final String datePattern = ((SimpleDateFormat) DateFormat
                                               .getDateInstance(DateFormat.SHORT, Locale.getDefault())).toPattern();
    private static final CustomDateFormat dateFormat = new CustomDateFormat(datePattern);

    private BulkSecInfo currentInfo;
    // Report types
    private String[] rptOptionStrings = {
        "By Investment Account, Then By Tradeable Securities/Account Cash",
        "By Ticker", "By Security Type, Then By Security SubType" };
    private String[] costBasisOptionStrings = {
        "Use Average Cost Basis Always", "Use Lot Matching Where Available" };
    // GUI Fields

    private JButton setDefaultsButton = new javax.swing.JButton("Reset All Fields To Default");

    private JLabel snapDateLabel = new javax.swing.JLabel("Report Snapshot Date");
    private JDateField snapDateField = new JDateField(dateFormat);
    private JLabel fromDateLabel = new javax.swing.JLabel("Report \"From\" Date");
    private JDateField fromDateField = new JDateField(dateFormat);
    private JLabel toDateLabel = new javax.swing.JLabel("Report \"To\" Date");
    private JDateField toDateField = new JDateField(dateFormat);

    private JLabel reportOptionsLabel = new JLabel("Report Aggregation Options");
    private JComboBox<String> reportOptionsComboBox = new JComboBox<String>(rptOptionStrings);
    private JComboBox<String> costBasisOptionsComboBox = new JComboBox<String>(costBasisOptionStrings);
    private JCheckBox aggregateSingleCheckBox =
        new JCheckBox("Show Aggregates for Composite Reports of One Security");



    private JButton dirChooserButton = new javax.swing.JButton("Set output folder");
    private JTextField directoryOutputField = new javax.swing.JTextField();
    private JCheckBox snapReportCheckbox = new javax.swing.JCheckBox("Shapshot Report");
    private JCheckBox fromToReportCheckbox = new javax.swing.JCheckBox("\"From/To\" Report");
    private JCheckBox transActivityCheckbox = new javax.swing.JCheckBox("Transaction Activity");
    private JCheckBox secPricesCheckbox = new javax.swing.JCheckBox("Securities Prices");
    private JButton runReportsButton = new javax.swing.JButton("Run Reports");
    private JTextField reportStatusField = new javax.swing.JTextField("Choose Reports to Run");

    private JFileChooser chooser;



    /** Creates new form NewJPanel */
    public ReportControlPanel(RootAccount root) {
        this.root = root;
        initComponents();
    }


    private void initComponents() {
        // Set Input Fields
        int todayDateInt = DateUtils.getLastCurrentDateInt();
        int yearAgoDateInt = DateUtils.addMonthsInt(todayDateInt, -12);

        snapDateField.setDateInt(todayDateInt);
        toDateField.setDateInt(todayDateInt);
        fromDateField.setDateInt(prefs.getInt(Prefs.fromReportDate,
                                              yearAgoDateInt));

        snapDateField.setHorizontalAlignment(JTextField.RIGHT); // alignment
        toDateField.setHorizontalAlignment(JTextField.RIGHT);
        fromDateField.setHorizontalAlignment(JTextField.RIGHT);

        snapReportCheckbox.setSelected(prefs.getBoolean(Prefs.runSnapshot, false));
        fromToReportCheckbox.setSelected(prefs.getBoolean(Prefs.runFromTo, false));
        transActivityCheckbox.setSelected(prefs.getBoolean(Prefs.runTransActivity, false));
        secPricesCheckbox.setSelected(prefs.getBoolean(Prefs.runSecuritiesPrices, false));

        reportOptionsComboBox.setSelectedIndex(prefs.getInt(Prefs.aggregationOptions, 0));
        costBasisOptionsComboBox.setSelectedIndex(prefs.getInt(Prefs.costBasisUsed, 0));
        aggregateSingleCheckBox.setSelected(prefs.getBoolean(Prefs.showSingletonAggregates, false));    
        
        directoryOutputField.setText(prefs.get(Prefs.exportPathPref, getDefaultDirectoryPath()));


        // Set text field width, button color
        Dimension textFields
            // Arbitrary max width for text fields
            = new Dimension(Math.max(directoryOutputField.getPreferredSize().width, 400), 14);
        directoryOutputField.setPreferredSize(textFields);
        reportStatusField.setPreferredSize(textFields);
        runReportsButton.setForeground(Color.red);

        // Add action listeners

        // button action listeners
        setDefaultsButton.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    setDefaultsButtonActionPerformed(evt);
                }

            });
        dirChooserButton.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    dirChooserButtonActionPerformed(evt);
                }});
        runReportsButton.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    runReportsButtonActionPerformed(evt);
                }});

        // Create and format sub-panels to load into main panel
        JPanel datePanel = new JPanel();
        JPanel reportsToRunPanel = new JPanel();
        JPanel reportOptionsPanel = new JPanel();
        JPanel folderPanel = new JPanel();
        JPanel runPanel = new JPanel();

        // Set all panel borders the same
        String[] titles = { "Report Dates", "Reports to Run", "Report Options",
                            "Download Location", "Report Status" };
        JPanel[] panels = { datePanel, reportsToRunPanel, reportOptionsPanel,
                            folderPanel, runPanel };

        for (int i = 0; i < panels.length; i++) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory .createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            panels[i].setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
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

        // Reports to Run sub-panel
        reportsToRunPanel.setLayout(new GridLayout(4, 1));
        reportsToRunPanel.add(snapReportCheckbox);
        reportsToRunPanel.add(fromToReportCheckbox);
        reportsToRunPanel.add(transActivityCheckbox);
        reportsToRunPanel.add(secPricesCheckbox);

        // report options sub-panel
        reportOptionsPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 0, 5);
        reportOptionsPanel.add(reportOptionsLabel, c);
        c.gridx = 0;
        c.gridy = 1;
        c.ipady = 12;
        reportOptionsPanel.add(reportOptionsComboBox, c);
        c.gridy = 2;
        reportOptionsPanel.add(costBasisOptionsComboBox, c);
        c.gridy = 3;
        reportOptionsPanel.add(aggregateSingleCheckBox, c);

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
        c.insets = new Insets(5, 5, 5, 5);

        //add setDefaultsButton
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;
        this.add(setDefaultsButton, c);

        //fill in side-by-side panels
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.EAST;
        c.fill = GridBagConstraints.BOTH; //fill all components

        c.gridwidth = 1;
        // add date sub-panel to left
        c.gridy = 1;
        this.add(datePanel, c);

        // add check box sub-panel to right
        c.gridx = 1;
        this.add(reportsToRunPanel, c);

        // all remaining components center, panel-width
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 2;
        c.gridx = 0;

        //add Report options sub-panel
        c.gridy = 2;
        this.add(reportOptionsPanel, c);

        // add folder sub-panel
        c.gridy = 3;
        this.add(folderPanel, c);

        // add Run Reports Button
        c.fill = GridBagConstraints.NONE; //want normal-sized button
        c.gridy = 4;
        this.add(runReportsButton, c);

        // add Run sub-panel (Program Results)
        c.fill = GridBagConstraints.BOTH; // back to fill all components
        c.gridy = 5;
        this.add(runPanel, c);

    }

    public static String getDateFormat(){
        return datePattern;
    }

    private String getDefaultDirectoryPath() {
        String defaultPath = root.getDataFile().getParentFile()
            .getAbsolutePath();
        File defaultPathFolder = new File(defaultPath);
        if (defaultPathFolder.canWrite()) {
            return defaultPath;
        } else {
            return System.getProperty("user.home");

        }

    }

    private void openBrowserToDownloadFile() throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                File sampleFolder = new File(directoryOutputField.getText());
                desktop.browse(sampleFolder.toURI());
            }

        }
        
    }


    /**set Date Fields
     * @param fromDateInt
     * @param toDateInt
     * @param snapDateInt
     */
    public void setDates(int fromDateInt, int toDateInt, int snapDateInt){
        fromDateField.setDateInt(fromDateInt);
        toDateField.setDateInt(toDateInt);
        snapDateField.setDateInt(snapDateInt);


    }

    private void setDefaultsButtonActionPerformed(ActionEvent evt) {
        setDefaultPreferences();
    }

    private void dirChooserButtonActionPerformed(java.awt.event.ActionEvent evt) {
        showFileChooser();
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void runReportsButtonActionPerformed(java.awt.event.ActionEvent evt) {
        reportStatusField.setText("Reports are running...");
        int costBasisType = costBasisOptionsComboBox.getSelectedIndex();
        GainsCalc implGainsCalc = null;
        if(costBasisType == 0){
            implGainsCalc = new GainsAverageCalc();
        } else {
            implGainsCalc = new GainsLotMatchCalc();
        }

        if(this.root != null) {
            try {
                this.currentInfo = new BulkSecInfo(this.root, implGainsCalc);
            } catch (Exception e) {
                File errorFile = new File(directoryOutputField.getText()
                                          + "\\IRBaseErrorlog.txt");
                StringBuffer erLOG = getStackTrace(e);
                IOUtils.writeResultsToFile(erLOG, errorFile);
            }
        } else {
            this.currentInfo = null;
        }

        try {

            int reportType = reportOptionsComboBox.getSelectedIndex();
            Class<?> firstAggClass = null;
            Class<?> secondAggClass = null;
            Boolean catHierarchy = false;
            Boolean rptOutputSingle = aggregateSingleCheckBox.isSelected() ? true
                : false;
            if (reportType == 2) {
                firstAggClass = SecurityTypeWrapper.class;
                secondAggClass = SecuritySubTypeWrapper.class;
                catHierarchy = true; //Sub Type is subset of SecurityType
            } else if (reportType == 1) {
                firstAggClass = CurrencyWrapper.class;
                secondAggClass = AllAggregate.class;
            } else {
                firstAggClass = InvestmentAccountWrapper.class;
                secondAggClass = Tradeable.class;
            }

            if (snapReportCheckbox.isSelected()) {
                int snapDateInt = snapDateField.getDateInt();
                TotalReport report = new TotalSnapshotReport(currentInfo,
                                                             firstAggClass,
                                                             secondAggClass,
                                                             catHierarchy,
                                                             rptOutputSingle,
                                                             snapDateInt);
                displayReport(report);
            }

            if (fromToReportCheckbox.isSelected()) {
                int fromDateInt = fromDateField.getDateInt();
                int toDateInt = toDateField.getDateInt();
                TotalReport report = new TotalFromToReport(currentInfo,
                                                           firstAggClass,
                                                           secondAggClass,
                                                           catHierarchy,
                                                           rptOutputSingle,
                                                           fromDateInt,
                                                           toDateInt);
                displayReport(report);
            }

            if (transActivityCheckbox.isSelected()) {
                ArrayList<String[]> transActivityReport
                    = currentInfo.listTransValuesSet(currentInfo.getInvestmentWrappers());
                File transActivityReportFile
                    = new File(directoryOutputField.getText() + "\\transActivityReport.csv");
                IOUtils.writeArrayListToCSV(TransactionValues.listTransValuesHeader(),
                                            transActivityReport,
                                            transActivityReportFile);
            }
            if (secPricesCheckbox.isSelected()) {
                ArrayList<String[]> secPricesReport
                    = BulkSecInfo.ListAllCurrenciesInfo(BulkSecInfo.getCurrencyWrappers());
                File secPricesReportFile
                    = new File(directoryOutputField.getText() + "\\secPricesReport.csv");
                IOUtils.writeArrayListToCSV(BulkSecInfo.listCurrencySnapshotHeader(),
                                            secPricesReport,
                                            secPricesReportFile);
            }

            if (transActivityCheckbox.isSelected()
                || secPricesCheckbox.isSelected()) {
                openBrowserToDownloadFile();
            }
        
        } catch (Exception e) {
            File errorFile = new File(directoryOutputField.getText() + "\\IRRerportErrors.txt");
            StringBuffer erLOG = getStackTrace(e);
            IOUtils.writeResultsToFile(erLOG, errorFile);
            reportStatusField.setText("Error! See Log in Output Folder");
        }
        reportStatusField.setText("Reports have been run!");
    }


    private void displayReport(TotalReport<?, ?> report) throws SecurityException,
        IllegalArgumentException, NoSuchFieldException,
        IllegalAccessException {
        ReportTableModel model = new ReportTableModel(report.getReportTable(),
                                                      report.getReportHeader());

        ReportOutputTable.CreateAndShowTable(model,
                                             report.getColumnFormats(),
                                             report.getClosedPosColumn(),
                                             report.getFrozenColumn(),
                                             report.getFirstSortColumn(),
                                             report.getSecondSortColumn(),
                                             ColSizeOption.MAXCONTCOLRESIZE,
                                             report.getReportTitle(),
                                             report.getRowBackgrounds());
    }


    public static void main(String[] args) {
        ReportControlPanel testPanel = new ReportControlPanel(null);
        @SuppressWarnings("unused")
            TestFrame frame = new TestFrame(testPanel);
    }


    public static class TestFrame extends JFrame {
        private static final long serialVersionUID = 2202318227772787528L;
        public TestFrame(final ReportControlPanel testPanel) {
            this.setTitle("Test Investment Reports Panel");
            testPanel.setOpaque(true);
            this.setContentPane(testPanel);
            this.addWindowListener(new WindowListener() {
                    @Override
                    public void windowOpened(WindowEvent e) {
                    }
                    @Override
                    public void windowIconified(WindowEvent e) {
                    }
                    @Override
                    public void windowDeiconified(WindowEvent e) {
                    }
                    @Override
                    public void windowDeactivated(WindowEvent e) {
                    }
                    @Override
                    public void windowClosing(WindowEvent e) {
                        testPanel.savePreferences();
                    }
                    @Override
                    public void windowClosed(WindowEvent e) {
                        e.getWindow().setVisible(false);
                        e.getWindow().dispose();
                        System.exit(0);
                    }
                    @Override
                    public void windowActivated(WindowEvent e) {
                    }
                });
            this.pack();
            AwtUtil.centerWindow(this);
            this.setVisible(true);
        }
    }

    public void setDefaultPreferences() {

        int todayDateInt = DateUtils.getLastCurrentDateInt();
        int yearAgoDateInt = DateUtils.addMonthsInt(todayDateInt, -12);

        snapDateField.setDateInt(todayDateInt);
        toDateField.setDateInt(todayDateInt);
        fromDateField.setDateInt(yearAgoDateInt);
        
        snapReportCheckbox.setSelected(false);
        fromToReportCheckbox.setSelected(false);
        transActivityCheckbox.setSelected(false);
        secPricesCheckbox.setSelected(false);

        reportOptionsComboBox.setSelectedIndex(0);
        costBasisOptionsComboBox.setSelectedIndex(0);
        aggregateSingleCheckBox.setSelected(false);
        
        directoryOutputField.setText(getDefaultDirectoryPath());
    }




    public void savePreferences() {

        prefs.putInt(Prefs.fromReportDate, fromDateField.getDateInt());

        prefs.putBoolean(Prefs.runSnapshot, snapReportCheckbox.isSelected());
        prefs.putBoolean(Prefs.runFromTo, fromToReportCheckbox.isSelected());
        prefs.putBoolean(Prefs.runTransActivity, transActivityCheckbox.isSelected());
        prefs.putBoolean(Prefs.runSecuritiesPrices, secPricesCheckbox.isSelected());

        prefs.putInt(Prefs.aggregationOptions, reportOptionsComboBox.getSelectedIndex());
        prefs.putInt(Prefs.costBasisUsed, costBasisOptionsComboBox.getSelectedIndex());
        prefs.putBoolean(Prefs.showSingletonAggregates, aggregateSingleCheckBox.isSelected());
        
        prefs.put(Prefs.exportPathPref, directoryOutputField.getText());

    }


    public void showFileChooser() {
        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(directoryOutputField.getText()));

        chooser.setDialogTitle("Choose Output Directory");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            System.out.println("getCurrentDirectory(): " + chooser.getSelectedFile().getAbsolutePath());
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


        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public String getColumnName(int col) {
            return columnNames[col];
        }

        public String[] getColumnNames() {
            return this.columnNames;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        // Allows table to be editable
        @Override
        public boolean isCellEditable(int row, int col) {
            // Note that the data/cell address is constant, no matter where the
            // cell appears onscreen.
            return col >= 2;
        }

        // allows table to be editable
        @Override
        public void setValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }
    }
}
