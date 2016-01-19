/*
 * ReportControlPanel.java
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

import com.moneydance.awt.AwtUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.prefs.BackingStoreException;


/**
 * produces panel for reports
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
public class ReportControlPanel extends javax.swing.JPanel implements ActionListener, PropertyChangeListener,
        ItemListener {
    private static final long serialVersionUID = -7581739722392109525L;
    public static final Dimension OPTIONS_BOX_DIMENSION = new Dimension(400, 20);
    public static final int textFieldWidth = 400;
    private static final String SHOW_HELP_FILE = "showHelpFile";
    private static final String RESET_REPORT_OPTIONS = "resetReportOptions";
    private static final String REMOVE_SELECTED_REPORT = "removeSelectedReport";
    private static final String REMOVE_ALL_REPORTS = "removeAllReports";
    private static final String SAVE_CUSTOM_REPORT = "saveCustomReport";
    private static final String RUN_REPORTS = "runReports";
    private static final String SET_COST_BASIS = "setCostBasis";
    private static final String SET_AGGREGATOR = "setAggregator";
    private static final String SET_OUTPUT_SINGLE = "setOutputSingle";
    private static final String SET_FROZEN_COLUMNS = "setFrozenColumns";
    private static final String HIDE_CLOSED_POSITIONS = "hideClosedPositions";
    private static final String USE_ORDINARY_RETURN = "useOrdinaryReturn";

    private static File outputDirectory;
    private static Level logLevel = Level.INFO;
    private ReportControlFrame reportControlFrame;

    private static MDData mdData = MDData.getInstance();

    private JLabel snapReportLabel = new JLabel("Snapshot Reports");
    private JComboBox<String> snapReportComboBox = new JComboBox<>();
    private JLabel fromToReportLabel = new JLabel("'From-To' Reports");
    private JComboBox<String> fromToReportComboBox = new JComboBox<>();
    private JCheckBox transActivityCheckbox = new javax.swing.JCheckBox("Transactions");
    private JCheckBox secPricesCheckbox = new javax.swing.JCheckBox("Securities Prices");
    private JButton runReportsButton = new javax.swing.JButton("Run Reports");
    private JButton saveCustomReportsButton = new javax.swing.JButton("Save Custom Report");
    private JButton removeCustomReportButton = new JButton("Remove Custom Report");
    private JButton showHelpFileButton = new JButton("Help");
    private JButton removeAllCustomReportsButton = new JButton("Remove All Custom Reports and Reset");


    private JTextArea reportStatusText = new javax.swing.JTextArea();
    private JScrollPane reportStatusPane = new JScrollPane(reportStatusText);
    private ReportOptionsPanel reportOptionsPanel = new ReportOptionsPanel();
    private DateRangePanel dateRangePanel;
    private ReportConfigFieldChooserPanel fieldChooserPanel;
    private ReportConfigAccountChooserPanel accountChooserPanel;
    private ReportConfigInvestIncomeChooserPanel investmentIncomeChooserPanel;
    private ReportConfigInvestExpenseChooserPanel investmentExpenseChooserPanel;
    private FolderPanel folderPanel = new FolderPanel();
    private ReportConfig reportConfig;



    /**
     * Creates new form NewJPanel
     */
    public ReportControlPanel(ReportControlFrame reportControlFrame) throws Exception {
        this.reportControlFrame = reportControlFrame;
        if (reportControlFrame.isRunInApplication()) mdData.initializeMDDataInApplication(true);
        initComponents();
        if(reportControlFrame.isRunInApplication()){
            java.util.List<String> msgs = mdData.getTransactionStatus();
            msgs.add("Choose Reports to Run");
            updateStatus(msgs);
        }
    }

    @SuppressWarnings("unused")
    public static StringBuffer getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return new StringBuffer(sw.toString());
    }

    private static File getOutputFile(String fileName) {
        return new File(outputDirectory, fileName);
    }

    public static String getOutputDirectoryPath() {
        return outputDirectory.getAbsolutePath();
    }




    public static void setLogLevel(Level logLevel) {
        ReportControlPanel.logLevel = logLevel;
    }


    private static void setPanelTitle(JPanel panel, String title) {
        TitledBorder titledBorder = BorderFactory.createTitledBorder(title);
        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        titledBorder.setTitleColor(new Color(100, 100, 100));
        panel.setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
    }

    private void initComponents() throws Exception {

        populateReportNames();



        // Set text field width, button color
        reportStatusPane.setPreferredSize(new Dimension(textFieldWidth, 54));
        reportStatusText.setWrapStyleWord(true);
        reportStatusText.setLineWrap(true);
        runReportsButton.setForeground(Color.red);
        showHelpFileButton.setForeground(Color.blue);

        // Add action listeners
        runReportsButton.setActionCommand(RUN_REPORTS);
        runReportsButton.addActionListener(this);
        showHelpFileButton.setActionCommand(SHOW_HELP_FILE);
        showHelpFileButton.addActionListener(this);
        saveCustomReportsButton.setActionCommand(SAVE_CUSTOM_REPORT);
        saveCustomReportsButton.addActionListener(this);
        removeCustomReportButton.setActionCommand(REMOVE_SELECTED_REPORT);
        removeCustomReportButton.addActionListener(this);
        removeAllCustomReportsButton.setActionCommand(REMOVE_ALL_REPORTS);
        removeAllCustomReportsButton.addActionListener(this);



        // Combo Box Action Listeners

        fromToReportComboBox.addItemListener(this);
        snapReportComboBox.addItemListener(this);


        // Create and format sub-panels to load into main panel
        dateRangePanel = new DateRangePanel(new DateRange());
        dateRangePanel.addChangeListener(this);

        JPanel reportsToRunPanel = new JPanel();
        JPanel downloadsPanel = new JPanel();
        JPanel runPanel = new JPanel();
        JPanel buttonPanel = new JPanel();

        JPanel mainReportPanel = new JPanel();
        fieldChooserPanel = new ReportConfigFieldChooserPanel(this);
        accountChooserPanel = new ReportConfigAccountChooserPanel(this);
        investmentIncomeChooserPanel = new ReportConfigInvestIncomeChooserPanel(this);
        investmentExpenseChooserPanel = new ReportConfigInvestExpenseChooserPanel(this);

        JTabbedPane reportTabbedPane = new JTabbedPane();
        reportTabbedPane.addTab("Report Options", null, mainReportPanel, "Main Options");
        reportTabbedPane.addTab("Report Fields", null, fieldChooserPanel, "Choose Fields to Display");
        reportTabbedPane.addTab("Accounts", null, accountChooserPanel, "Choose Accounts to Run");
        reportTabbedPane.addTab("Investment Income", null, investmentIncomeChooserPanel, "Identify Investment Income");
        reportTabbedPane.addTab("Investment Expenses", null, investmentExpenseChooserPanel, "Identify Investment Expenses");


        // Set all panel borders the same
        setPanelTitle(reportsToRunPanel, "Reports to Run (Choose One)");
        setPanelTitle(downloadsPanel, "Export Transaction/Price Data");
        setPanelTitle(runPanel, "Generate Report");
        setPanelTitle(buttonPanel, "Custom Reports");

        // Layout sub-panels
        GridBagConstraints c;

        // Reports to Run sub-panel
        reportsToRunPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.weightx = 1;  //justify everything vertically
        //labels
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        reportsToRunPanel.add(fromToReportLabel, c);
        c.gridx = 1;
        reportsToRunPanel.add(fromToReportComboBox, c);
        c.gridx = 0;
        c.gridy++;
        reportsToRunPanel.add(snapReportLabel, c);
        c.gridx = 1;
        reportsToRunPanel.add(snapReportComboBox, c);
        // downloads panel
        downloadsPanel.setLayout(new GridBagLayout());
        c.gridx = 0;
        c.gridy = 0;
        downloadsPanel.add(transActivityCheckbox, c);
        c.gridx++;
        downloadsPanel.add(secPricesCheckbox, c);



        // run sub-panel (for program results)
        runPanel.setLayout(new GridBagLayout());
        JPanel runButtonPanel = new JPanel(new GridBagLayout());
        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5,5,5,5);
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;
        runButtonPanel.add(runReportsButton, c);
        c.gridx ++;
        runButtonPanel.add(showHelpFileButton, c);
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0;
        c.gridx = 0;
        c.gridy = 0;
        runPanel.add(runButtonPanel, c);
        c.gridy++;
        runPanel.add(reportStatusPane, c);

        //button panel
        buttonPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;
        buttonPanel.add(saveCustomReportsButton, c);
        c.gridx++;
        buttonPanel.add(removeCustomReportButton, c);
        c.weightx = 1;
        c.gridwidth = 2;
        c.gridx = 0;
        c.gridy++;
        buttonPanel.add(removeAllCustomReportsButton, c);


        // lay out main panel
        mainReportPanel.setLayout(new GridBagLayout());

        // lay out left/right panels
        JComponent[] leftPanelComponents = {reportOptionsPanel, folderPanel, reportsToRunPanel, downloadsPanel};
        JComponent[] rightPanelComponents = {dateRangePanel, buttonPanel};

        JPanel leftPanel = addVerticalComponents(leftPanelComponents);
        JPanel rightPanel = addVerticalComponents(rightPanelComponents);
        //add left and right panels
        c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.gridy = 0;
        c.gridx= 0;
        mainReportPanel.add(leftPanel, c);
        c.gridx++;
        mainReportPanel.add(rightPanel, c);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.BOTH;
        mainReportPanel.add(runPanel, c);
        // finally, add tabbed pain to 'this'
        this.add(reportTabbedPane);
        //set preferences
        setReportConfigInGUI();
        if(mdData.getRoot() != null) {
            setAccountAndFolderSubPanels();
        }


    }

    public void setAccountAndFolderSubPanels() throws Exception {
        accountChooserPanel.populateBothAccountLists(reportConfig);
        investmentIncomeChooserPanel.populateBothIncomeLists(reportConfig);
        investmentExpenseChooserPanel.populateBothExpenseLists(reportConfig);
        folderPanel.setOutputDirectory();
    }

    private JPanel addVerticalComponents(JComponent[] panelComponents) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        //set defaults button
        c.insets = new Insets(2, 2, 2, 2);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        for (JComponent component : panelComponents){
            panel.add(component, c);
            c.gridy++;
        }
        return panel;
    }

    private void populateReportNames() throws Exception {
        ArrayList<Class<? extends TotalReport>> reportClasses = new ArrayList<>();
        reportClasses.add(TotalFromToReport.class);
        reportClasses.add(TotalSnapshotReport.class);
        ArrayList<JComboBox<String>> comboBoxes = new ArrayList<>();
        comboBoxes.add(fromToReportComboBox);
        comboBoxes.add(snapReportComboBox);

        for (int i = 0; i < reportClasses.size(); i++) {
            ReportConfig.setStandardConfigInPrefs(reportClasses.get(i));
            comboBoxes.get(i).addItem("NONE");
            comboBoxes.get(i).setPreferredSize(OPTIONS_BOX_DIMENSION);
            for (String reportName : ReportConfig.getReportNamesForClass(reportClasses.get(i))) {
                comboBoxes.get(i).addItem(reportName);
            }
        }
        getLastReportRun();
    }

    private void removeSelectedReport() throws Exception {

        if (!reportConfig.getReportName().equals(Prefs.STANDARD_NAME)) {
            reportConfig.clearReportConfigFromPrefs();
            JComboBox<String> selectedComboBox = reportConfig.getReportClass() == TotalFromToReport.class ?
                    fromToReportComboBox : snapReportComboBox;
            selectedComboBox.removeItem(reportConfig.getReportName());

        } else {
            JOptionPane.showMessageDialog(this, "Cannot Remove Standard Configuration");
        }
    }

    private void removeAllReportsAndReset() throws Exception {
        Prefs.clearAllPrefs();
        fromToReportComboBox.removeItemListener(this);
        snapReportComboBox.removeItemListener(this);
        fromToReportComboBox.removeAllItems();
        snapReportComboBox.removeAllItems();

        populateReportNames();

        fromToReportComboBox.addItemListener(this);
        snapReportComboBox.addItemListener(this);
        loadNewReportConfig(snapReportComboBox);

    }




    private void saveLastReportRun() throws NoSuchFieldException, IllegalAccessException {
        Class<? extends TotalReport> reportClass = null;
        String reportName = null;
        if(snapReportComboBox.getSelectedIndex() > 0) {
            reportClass = TotalSnapshotReport.class;
            reportName = (String) snapReportComboBox.getSelectedItem();
        }
        if(fromToReportComboBox.getSelectedIndex() > 0) {
            reportClass = TotalFromToReport.class;
            reportName = (String) fromToReportComboBox.getSelectedItem();
        }
        if(reportClass != null){
            Prefs.REPORT_PREFS.put(Prefs.LAST_REPORT_TYPE_RUN, ReportConfig.getReportTypeName(reportClass));
            Prefs.REPORT_PREFS.put(Prefs.LAST_REPORT_NAME_RUN, reportName);
        }
    }

    private void getLastReportRun() throws Exception {

        String lastReportTypeName = Prefs.REPORT_PREFS.get(Prefs.LAST_REPORT_TYPE_RUN,
                ReportConfig.getReportTypeName(TotalSnapshotReport.class));
        String lastReportClassSimpleName = TotalReport.getClassSimpleNameFromReportTypeName(lastReportTypeName);
        String lastReportName = Prefs.REPORT_PREFS.get(Prefs.LAST_REPORT_NAME_RUN,
                Prefs.STANDARD_NAME);
        boolean reportConfigExists = Prefs.REPORT_CONFIG_PREFS.node(lastReportTypeName).nodeExists(lastReportName);
        String validReportName = reportConfigExists ? lastReportName : Prefs.STANDARD_NAME;
        reportConfig = new ReportConfig(lastReportClassSimpleName, validReportName);
        JComboBox<String> comboBoxToSelect = lastReportClassSimpleName.equals(TotalFromToReport.class.getSimpleName())
                ? fromToReportComboBox : snapReportComboBox;
        // set combo boxes appropriately
        comboBoxToSelect.setSelectedItem(validReportName);
        (comboBoxToSelect == fromToReportComboBox ? snapReportComboBox : fromToReportComboBox).setSelectedIndex(0);
        // set downloads
        setDownloadPreferences();
    }

    public ReportControlFrame getReportControlFrame() {
        return reportControlFrame;
    }

    public DateRangePanel getDateRangePanel() {
        return dateRangePanel;
    }

    private void openBrowserToDownloadFile() throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(outputDirectory.toURI());
            }

        }

    }

    public void setReportStatusFieldText(String message) {
        reportStatusText.setText(message);
    }


    public void savePreferences() {
        Prefs.REPORT_PREFS.putBoolean(Prefs.RUN_ACTIVITY,
                transActivityCheckbox.isSelected());
        Prefs.REPORT_PREFS.putBoolean(Prefs.RUN_SECURITIES_PRICES,
                secPricesCheckbox.isSelected());
        folderPanel.savePreferences();
        reportConfig.saveReportConfig();
    }

    public void setDownloadPreferences(){
        transActivityCheckbox.setSelected(Prefs.REPORT_PREFS.getBoolean(Prefs.RUN_ACTIVITY, false));
        secPricesCheckbox.setSelected(Prefs.REPORT_PREFS.getBoolean(Prefs.RUN_SECURITIES_PRICES, false));
    }



    public void setReportConfigInGUI() throws Exception {

        reportOptionsPanel.setReportConfigInOptionsPanel();
        dateRangePanel.populateDateRangePanel(reportConfig.getDateRange());
        fieldChooserPanel.populateFieldChooser(reportConfig);
        if (mdData.getRoot() != null) {
            accountChooserPanel.populateBothAccountLists(reportConfig);
            investmentIncomeChooserPanel.populateBothIncomeLists(reportConfig);
            investmentExpenseChooserPanel.populateBothExpenseLists(reportConfig);
        }
    }

    public String showErrorMessage(String message) {
        return message + " See Log in " + folderPanel.getDirectoryOutputField()
                + " for details";
    }

    void updateStatus(java.util.List<String> msgs) {
        String output = reportStatusText.getText();
        for (String msg : msgs) {
            String newLine = msgs.indexOf(msg) < msgs.size() - 1 ? "\n" : "";
            output += (msg + newLine);
        }
        reportStatusText.setText(output);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        switch(actionCommand){
            case REMOVE_SELECTED_REPORT:
                try {
                    removeSelectedReport();
                } catch (Exception e1) {
                    String msg = "Error on removing selected report";
                    LogController.logException(e1, msg);
                    showErrorMessage(msg);
                }
                break;
            case REMOVE_ALL_REPORTS:
                try {
                    removeAllReportsAndReset();
                } catch (Exception e1){
                    String msg = "Error on removing all reports";
                    LogController.logException(e1, msg);
                    showErrorMessage(msg);
                }
                break;
            case SAVE_CUSTOM_REPORT:
                try {
                    if(reportConfig.getDateRange().isValid()){
                        ReportSaveControlFrame.setReportSaveControlFrame(reportControlFrame);
                    } else {
                        JOptionPane.showMessageDialog(ReportControlPanel.this, "Invalid Date Range! Cannot Save.");
                    }
                } catch (IllegalAccessException | NoSuchFieldException | BackingStoreException e1) {
                    String msg = " Exception on Report Save: ";
                    LogController.logException(e1, msg);
                    setReportStatusFieldText(showErrorMessage(msg));
                }
                break;
            case RUN_REPORTS:
                String incExpWarning = reportConfig.showIncExpWarning();
                int continueRun = JOptionPane.YES_OPTION;
                if(incExpWarning.length() > 0)
                    continueRun = JOptionPane.showConfirmDialog(ReportControlPanel.this, incExpWarning,
                            "Warning: Income/Expenses", JOptionPane.YES_NO_OPTION);
                if(continueRun == JOptionPane.YES_OPTION){
                    new ReportExecutor().execute();
                }
                break;
            case RESET_REPORT_OPTIONS:
                reportOptionsPanel.resetFields();
                break;
            case SET_AGGREGATOR:
                AggregationController aggregationController = (AggregationController) reportOptionsPanel
                        .aggregationOptionsComboBox.getSelectedItem();
                reportConfig.setAggregationController(aggregationController);
                break;
            case SET_COST_BASIS:
                boolean useAverageCost = reportOptionsPanel.costBasisOptionsComboBox.getSelectedIndex() == 0;
                reportConfig.setUseAverageCostBasis(useAverageCost);
                break;
            case SET_OUTPUT_SINGLE:
                reportConfig.setOutputSingle(reportOptionsPanel.aggregateSingleCheckBox.isSelected());
                break;
            case SET_FROZEN_COLUMNS:
                reportConfig.setNumFrozenColumns(reportOptionsPanel.numFrozenColumnsComboBox.getSelectedIndex());
                break;
            case HIDE_CLOSED_POSITIONS:
                reportConfig.setClosedPosHidden(reportOptionsPanel.hideClosedPosCheckBox.isSelected());
                break;
            case USE_ORDINARY_RETURN:
                reportConfig.setUseOrdinaryReturn(reportOptionsPanel.useOrdinaryReturnCheckBox.isSelected());
                break;
            case SHOW_HELP_FILE:
                HelpFileDisplay.showHelpFile(this.getLocationOnScreen());
                break;
            default:
                break;
        }
    }

    private void loadNewReportConfig(JComboBox<String> selectedComboBox) {

        JComboBox<String> otherComboBox = selectedComboBox.equals(fromToReportComboBox)
                ? snapReportComboBox : fromToReportComboBox;
        Class<? extends TotalReport> reportClass = selectedComboBox.equals(fromToReportComboBox)
                ? TotalFromToReport.class : TotalSnapshotReport.class;
        String reportName = (String) selectedComboBox.getSelectedItem();

        if(otherComboBox.getSelectedIndex() != 0) otherComboBox.setSelectedIndex(0);
        if(selectedComboBox.getSelectedIndex() != 0){
            try {
                reportConfig = new ReportConfig(reportClass, reportName);
                setReportConfigInGUI();
            } catch (Exception e) {
                String msg = "Error on loading new report config";
                LogController.logException(e, msg);
                showErrorMessage(msg);
            }
        } else { //user sets to NONE,  set report to standard config of other type
            reportClass = otherComboBox.equals(fromToReportComboBox) ? TotalFromToReport.class : TotalSnapshotReport.class;
            try {
                otherComboBox.setSelectedIndex(1);
                reportConfig = ReportConfig.getStandardReportConfig(reportClass);
                setReportConfigInGUI();
            } catch (Exception e) {
                String msg = "Error on loading new report config";
                LogController.logException(e, msg);
                showErrorMessage(msg);
            }
        }
        selectedComboBox.setSelectedItem(reportName);

    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if(evt.getPropertyName().equals(DateRangePanel.DATE_RANGE_CHANGED)
                && evt.getNewValue() instanceof DateRange){
            reportConfig.setDateRange((DateRange) evt.getNewValue());
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        int state = e.getStateChange();
        if(state == ItemEvent.SELECTED){
            if(e.getSource().equals(fromToReportComboBox)){
                loadNewReportConfig(fromToReportComboBox);
            } else {
                loadNewReportConfig(snapReportComboBox);
            }
        }
    }

    public ReportConfig getReportConfig() {
        return reportConfig;
    }

    public Dimension getRelatedDimension(JScrollPane scrollPane){
        return new Dimension(scrollPane.getPreferredSize().width,
                this.getPreferredSize().height - 150);
    }

    public void setNewReportName(String reportName) {
        Class<? extends TotalReport> reportClass = reportConfig.getReportClass();
        JComboBox<String> comboBox = reportClass == TotalFromToReport.class
                ? fromToReportComboBox : snapReportComboBox;
        boolean nameAlreadyUsed = false;
        for(int i = 0; i < comboBox.getItemCount(); i++){
            String name = comboBox.getItemAt(i);
            if(name.equals(reportName)){
                nameAlreadyUsed = true;
                break;
            }
        }
        if(!nameAlreadyUsed){
            comboBox.addItem(reportName);
            comboBox.setSelectedItem(reportName);
        }
    }

    public static class TestFrame extends JFrame {

        private static final long serialVersionUID = 2202318227772787528L;

        public TestFrame() {
        }


        public TestFrame(final JPanel testPanel) {
            addPanel(testPanel);
        }

        public void addPanel(JPanel testPanel) {
            this.setTitle("Test Investment Reports Panel");
            testPanel.setOpaque(true);
            this.setContentPane(testPanel);
            this.addWindowListener(new TestWindowListener());
            this.pack();
            AwtUtil.centerWindow(this);
            this.setVisible(true);
        }

        class TestWindowListener implements WindowListener {
            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }

            @Override
            public void windowClosed(WindowEvent e) {
                e.getWindow().setVisible(false);
                e.getWindow().dispose();
                System.exit(0);
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        }
    }


    public class FolderPanel extends JPanel{
        private static final long serialVersionUID = 3037092760394483468L;
        private JButton dirChooserButton = new javax.swing.JButton("Set output folder");
        private JTextField directoryOutputField = new javax.swing.JTextField();

        public FolderPanel(){
            // set Action Listener
            dirChooserButton.addActionListener(e -> showFileChooser());
            setPanelTitle(this, "Download Location");
            directoryOutputField.setPreferredSize(new Dimension(textFieldWidth, 24));
            this.setLayout(new GridBagLayout());
            GridBagConstraints c;
            c = new GridBagConstraints();
            c.insets = new Insets(2, 5, 2, 5);
            c.anchor = GridBagConstraints.WEST;
            this.add(dirChooserButton, c);
            c.gridy =1;
            this.add(directoryOutputField, c);
        }


        public void showFileChooser() {
            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File(directoryOutputField.getText()));

            chooser.setDialogTitle("Choose Output Directory");
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setAcceptAllFileFilterUsed(false);
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                System.out.println("getCurrentDirectory(): "
                        + chooser.getSelectedFile().getAbsolutePath());
                directoryOutputField.setText(chooser.getSelectedFile().getAbsolutePath());
                outputDirectory = new File(directoryOutputField.getText());
            } else {
                System.out.println("No Selection ");
            }
        }

        public void setOutputDirectory() {
            String outputPath = Prefs.REPORT_PREFS.get(Prefs.EXPORT_DIR_PATH, "");
            directoryOutputField.setText(outputPath);
            if (outputPath.length() == 0) {
                outputPath = mdData.getRoot() == null ? System.getProperty("user.home") :
                        mdData.getAccountBook().getRootFolder().getAbsolutePath();
                File outputPathFolder = new File(outputPath);
                if (outputPathFolder.canWrite()) {
                    directoryOutputField.setText(outputPath);
                } else {
                    directoryOutputField.setText(System.getProperty("user.home"));
                }
            }
            outputDirectory = new File(directoryOutputField.getText());
        }

        public void savePreferences() {
            Prefs.REPORT_PREFS.put(Prefs.EXPORT_DIR_PATH, directoryOutputField.getText());
        }

        public String getDirectoryOutputField(){
            return directoryOutputField.getText();
        }

    }


    /**
     * Controls Aggregation, number of frozen panels, etc
     */
    class ReportOptionsPanel extends JPanel {

        public static final long serialVersionUID = 2476680585088267452L;
        public Integer[] numFrozenColumnsOptions = {0, 1, 2, 3, 4, 5};

        private JButton resetReportOptions = new javax.swing.JButton(
                "Reset To Default");

        public JLabel aggregationOptionsLabel = new JLabel("Aggregate by");
        public JComboBox<AggregationController> aggregationOptionsComboBox =
                new JComboBox<>(new DefaultComboBoxModel<>(AggregationController.values()));
        public JLabel costBasisOptionsLabel = new JLabel("Cost Basis");
        public String[] costBasisOptionStrings = {
                "Average Cost Basis Always", "Lot Matching Where Available"};
        // GUI Fields
        public JComboBox<String> costBasisOptionsComboBox = new JComboBox<>(costBasisOptionStrings);

        public JCheckBox aggregateSingleCheckBox = new JCheckBox("Show Aggregates for Single " +
                "Security", false);
        public JLabel numFrozenColumnsLabel = new JLabel("Number of Frozen Display Columns");
        public JComboBox<Integer> numFrozenColumnsComboBox = new JComboBox<>(numFrozenColumnsOptions);
        public JCheckBox hideClosedPosCheckBox = new JCheckBox("Hide Positions with Zero Value", true);
        public JCheckBox useOrdinaryReturnCheckBox = new JCheckBox("Use Ordinary Return Calculation", false);



        public ReportOptionsPanel() {
            //set action commands
            resetReportOptions.setActionCommand(RESET_REPORT_OPTIONS);
            aggregationOptionsComboBox.setActionCommand(SET_AGGREGATOR);
            costBasisOptionsComboBox.setActionCommand(SET_COST_BASIS);
            aggregateSingleCheckBox.setActionCommand(SET_OUTPUT_SINGLE);
            numFrozenColumnsComboBox.setActionCommand(SET_FROZEN_COLUMNS);
            hideClosedPosCheckBox.setActionCommand(HIDE_CLOSED_POSITIONS);
            useOrdinaryReturnCheckBox.setActionCommand(USE_ORDINARY_RETURN);
            // add action listeners
            resetReportOptions.addActionListener(ReportControlPanel.this);
            aggregationOptionsComboBox.addActionListener(ReportControlPanel.this);
            costBasisOptionsComboBox.addActionListener(ReportControlPanel.this);
            aggregateSingleCheckBox.addActionListener(ReportControlPanel.this);
            numFrozenColumnsComboBox.addActionListener(ReportControlPanel.this);
            hideClosedPosCheckBox.addActionListener(ReportControlPanel.this);
            useOrdinaryReturnCheckBox.addActionListener(ReportControlPanel.this);


            String ordinaryReturnsCBToolTip = "<html> If checked, uses non-time-weighted ('Ordinary') returns" +
                    "<br>" + "otherwise uses Modified-Dietz returns </html>";
            useOrdinaryReturnCheckBox.setToolTipText(ordinaryReturnsCBToolTip);

            //initialize sub-panels
            JPanel topPanel = new JPanel();
            // Layout sub-panels
            GridBagConstraints c;
            topPanel.setLayout(new GridBagLayout());
            c = new GridBagConstraints();
            c.insets = new Insets(0, 5, 0, 5);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            c.gridy = 0;

            c.gridy++;
            topPanel.add(aggregationOptionsLabel, c);
            c.gridx = 1;
            topPanel.add(aggregationOptionsComboBox, c);
            c.gridx = 1;
            c.gridy++;
            topPanel.add(aggregateSingleCheckBox, c);
            c.gridx = 0;
            c.gridy++;
            topPanel.add(costBasisOptionsLabel, c);
            c.gridx = 1;
            topPanel.add(costBasisOptionsComboBox, c);
            c.gridx = 1;
            c.gridy++;
            topPanel.add(hideClosedPosCheckBox, c);
            c.gridx = 1;
            c.gridy++;
            topPanel.add(useOrdinaryReturnCheckBox, c);
            c.gridx = 0;
            c.gridy++;
            topPanel.add(numFrozenColumnsLabel, c);
            //data
            c.gridx = 1;
            topPanel.add(numFrozenColumnsComboBox, c);

            setPanelTitle(this, "Report Options");
            this.setLayout(new GridBagLayout());
            c = new GridBagConstraints();
            c.insets = new Insets(2, 5, 2, 5);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            c.gridy = 0;

            this.add(topPanel, c);
            c.gridy++;

            // add reset button
            this.add(resetReportOptions, c);
        }



        public void resetFields() {
            aggregationOptionsComboBox.setSelectedIndex(0);
            costBasisOptionsComboBox.setSelectedIndex(0);
            aggregateSingleCheckBox.setSelected(false);
            numFrozenColumnsComboBox.setSelectedItem(5);
            hideClosedPosCheckBox.setSelected(true);
            useOrdinaryReturnCheckBox.setSelected(false);
        }

        public void setReportConfigInOptionsPanel() {
            aggregationOptionsComboBox.setSelectedItem(reportConfig.getAggregationController());
            costBasisOptionsComboBox.setSelectedIndex(reportConfig.useAverageCostBasis() ? 0 : 1);
            aggregateSingleCheckBox.setSelected(reportConfig.isOutputSingle());
            numFrozenColumnsComboBox.setSelectedItem(reportConfig.getNumFrozenColumns());
            hideClosedPosCheckBox.setSelected(reportConfig.isClosedPosHidden());
            useOrdinaryReturnCheckBox.setSelected(reportConfig.useOrdinaryReturn());
        }
    }


    private class ReportExecutor extends SwingWorker<Void, String> {

        @Override
        protected Void doInBackground() throws Exception {
            if (logLevel.intValue() == Level.SEVERE.intValue()) {
                publish(showErrorMessage("Cannot run reports!"));
                return null;
            }
            saveLastReportRun();
            publish(reportConfig.getDescription() + " is running...\n");
            if(transActivityCheckbox.isSelected()) publish("writing transaction data to file\n");
            if(secPricesCheckbox.isSelected()) publish("writing security price data to file\n");
            //load BulkSecInfo...
            if (mdData.getRoot() != null) {
                try {
                    mdData.generateCurrentInfo(reportConfig);
                } catch (Exception e) {
                    LogController.logException(e, "Error on loading security information from datafile: ");
                    publish(showErrorMessage("Error--Could not load securities from data file!"));
                }
            }
            //Now Run Reports...
            if (logLevel != Level.SEVERE && mdData.getCurrentInfo() != null) {
                try {
                    if (snapReportComboBox.getSelectedIndex() != 0) {
                        TotalReport report = new TotalSnapshotReport(reportConfig, MDData
                                .getInstance().getCurrentInfo());
                        report.calcReport();
                        report.displayReport();
                    }
                    if (fromToReportComboBox.getSelectedIndex() != 0) {
                        TotalReport report = new TotalFromToReport(reportConfig, MDData.
                                getInstance().getCurrentInfo());
                        report.calcReport();
                        report.displayReport();
                    }
                    if (transActivityCheckbox.isSelected()) {
                        ArrayList<String[]> transActivityReport
                                = MDData.getInstance().getCurrentInfo().listAllTransValues();
                        File transActivityReportFile = getOutputFile("transActivityReport.csv");
                        IOUtils.writeArrayListToCSV(TransactionValues.listTransValuesHeader(),
                                transActivityReport, transActivityReportFile);
                    }
                    if (secPricesCheckbox.isSelected()) {
                        ArrayList<String[]> secPricesReport
                                = MDData.getInstance().getCurrentInfo().ListAllCurrenciesInfo();
                        File secPricesReportFile = getOutputFile("secPricesReport.csv");
                        IOUtils.writeArrayListToCSV(BulkSecInfo.listCurrencySnapshotHeader(),
                                secPricesReport, secPricesReportFile);
                    }

                    if (transActivityCheckbox.isSelected() || secPricesCheckbox.isSelected()) {
                        openBrowserToDownloadFile();
                    }

                } catch (Exception e) {
                    LogController.logException(e, "Error on running reports: ");
                    publish(showErrorMessage("Error--Could not run reports!"));
                }
            } else {
                publish(showErrorMessage("Error--Reports not run! "));
            }
            if (logLevel.intValue() < Level.WARNING.intValue()) {
                publish("Reports have been run!");
            } else if ((logLevel.intValue() == Level.WARNING.intValue())) {
                publish(showErrorMessage("Reports run with WARNINGS!  " +
                        "Transaction data may not have validated. "));
                ReportControlPanel.this.getReportControlFrame().toFront();
                ReportControlPanel.this.getReportControlFrame().repaint();
            }
            return null;
        }

        @Override
        protected void process(java.util.List<String> msgs) {
            String output = "";
            for (String msg : msgs) {
                output += (msg + " ");
            }
            reportStatusText.setText(output);
        }

    }


}
