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

import com.moneydance.apps.md.controller.io.FileUtils;
import com.moneydance.apps.md.model.RootAccount;
import com.moneydance.awt.AwtUtil;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;


/**
 * produces panel for reports
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
public class ReportControlPanel extends javax.swing.JPanel {
    private static final long serialVersionUID = -7581739722392109525L;
    private static final Dimension optionsBoxDimension = new Dimension(400, 20);
    private ReportControlFrame reportControlFrame;
    private RootAccount root;
    private Preferences prefs = Prefs.REPORT_PREFS;
    private BulkSecInfo currentInfo = null;
    // Report types

    private String[] costBasisOptionStrings = {
            "Use Average Cost Basis Always", "Use Lot Matching Where Available"};
    // GUI Fields
    private JComboBox<String> costBasisOptionsComboBox = new JComboBox<>(costBasisOptionStrings);
    private JButton setDefaultsButton = new javax.swing.JButton(
            "Reset All Fields To Default");
    private JLabel aggregationOptionsLabel = new JLabel("Report Aggregation Options");
    private JComboBox<AggregationController> aggregationOptionsComboBox =
            new JComboBox<>(new DefaultComboBoxModel<>(AggregationController.values()));
    private JLabel costBasisOptionsLabel = new JLabel("Cost Basis Reporting Options");
    private JButton dirChooserButton = new javax.swing.JButton("Set output folder");
    private JTextField directoryOutputField = new javax.swing.JTextField();
    private JLabel snapReportLabel = new JLabel("Snapshot Reports");
    private JComboBox<String> snapReportComboBox = new JComboBox<>();
    private JLabel fromToReportLabel = new JLabel("'From-To' Reports");
    private JComboBox<String> fromToReportComboBox = new JComboBox<>();
    private JCheckBox transActivityCheckbox = new javax.swing.JCheckBox("Transaction Activity");
    private JCheckBox secPricesCheckbox = new javax.swing.JCheckBox("Securities Prices");
    private JButton runReportsButton = new javax.swing.JButton("Run Reports");
    private JButton customizeReportsButton = new javax.swing.JButton("Customize Report Configuration");
    private JTextField reportStatusField = new javax.swing.JTextField();

    private DateRangePanel datePanel;
    private static File outputDirectory;
    private static boolean severeError = false;


    /**
     * Creates new form NewJPanel
     */
    public ReportControlPanel(ReportControlFrame reportControlFrame) throws IllegalAccessException, NoSuchFieldException, BackingStoreException {
        this.reportControlFrame = reportControlFrame;
        initComponents();
        if (reportControlFrame.getExtension() != null) {
            root = reportControlFrame.getExtension().getUnprotectedContext().getRootAccount();
            reportStatusField.setText("Choose reports to run");
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

    private void initComponents() throws IllegalAccessException, BackingStoreException, NoSuchFieldException {

        populateReportNames();

        transActivityCheckbox.setSelected(prefs.getBoolean(
                Prefs.RUN_ACTIVITY, false));
        secPricesCheckbox.setSelected(prefs.getBoolean(
                Prefs.RUN_SECURITIES_PRICES, false));


        aggregationOptionsComboBox.setSelectedIndex(prefs.getInt(
                Prefs.AGGREGATION_OPTIONS, 0));
        costBasisOptionsComboBox.setSelectedIndex(prefs.getInt(
                Prefs.COST_BASIS_USED, 0));
        aggregationOptionsComboBox.setPreferredSize(optionsBoxDimension);
        costBasisOptionsComboBox.setPreferredSize(optionsBoxDimension);
        directoryOutputField.setText(prefs.get(Prefs.EXPORT_DIR_PATH,
                getDefaultDirectoryPath()));
        outputDirectory = new File(directoryOutputField.getText());



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
                setDefaultsButtonActionPerformed();
            }

        });
        dirChooserButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dirChooserButtonActionPerformed();
            }
        });
        runReportsButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                new ReportExecutor().execute();
            }
        });
        customizeReportsButton.addActionListener(new CustomizeReportButtonListener(this.reportControlFrame));


        // Create and format sub-panels to load into main panel
        datePanel = new DateRangePanel(new DateRange());
        JPanel reportsToRunPanel = new JPanel();
        JPanel reportOptionsPanel = new JPanel();
        JPanel folderPanel = new JPanel();
        JPanel runPanel = new JPanel();
        JPanel buttonPanel = new JPanel();

        // Set all panel borders the same
        String[] titles = {"Reports to Run", "Report Options",
                "Download Location", "Report Status", "Run Reports or Customize"};
        JPanel[] panels = {reportsToRunPanel, reportOptionsPanel,
                folderPanel, runPanel, buttonPanel};

        for (int i = 0; i < panels.length; i++) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            panels[i].setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        }

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
        c.gridy++;
        reportsToRunPanel.add(fromToReportComboBox, c);
        c.gridy++;
        reportsToRunPanel.add(snapReportLabel, c);
        c.gridy++;
        reportsToRunPanel.add(snapReportComboBox, c);
        c.gridy++;
        reportsToRunPanel.add(transActivityCheckbox, c);
        c.gridy++;
        reportsToRunPanel.add(secPricesCheckbox, c);

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
        c.ipady = 6;
        folderPanel.add(directoryOutputField, c);

        // report options sub-panel
        reportOptionsPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.NONE;

        c.insets = new Insets(10, 0, 0, 0);
        reportOptionsPanel.add(aggregationOptionsLabel, c);
        c.gridx = 0;
        c.gridy++;

        reportOptionsPanel.add(aggregationOptionsComboBox, c);
        c.gridy++;
        reportOptionsPanel.add(costBasisOptionsLabel, c);
        c.gridy++;
        reportOptionsPanel.add(costBasisOptionsComboBox, c);
        c.gridy++;
        reportOptionsPanel.add(folderPanel, c);

        // run sub-panel (for program results)
        runPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.ipady = 12;
        runPanel.add(reportStatusField, c);

        //button panel
        buttonPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.weightx = 1;
        c.fill = GridBagConstraints.NONE;
        //labels
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;
        buttonPanel.add(runReportsButton, c);
        c.gridy++;
        buttonPanel.add(customizeReportsButton, c);
        c.gridy++;
        c.anchor = GridBagConstraints.WEST;
        buttonPanel.add(runPanel, c);

        // lay out main panel
        this.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(2, 2, 2, 2);
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;
        //set defaults button
        this.add(setDefaultsButton, c);
        c.gridwidth = 1;
        c.fill = GridBagConstraints.HORIZONTAL; //fill all components
        // add date sub-panel to left
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridy++;
        this.add(reportOptionsPanel, c);
        //report options to the right
        c.gridx++;
        this.add(datePanel, c);
        //report chooser lower left
        c.gridx = 0;
        c.gridy++;
        this.add(reportsToRunPanel, c);
        //button panel lower right
        c.gridx++;
        this.add(buttonPanel, c);
    }

    private void populateReportNames() throws BackingStoreException, NoSuchFieldException,
            IllegalAccessException {
        ArrayList<Class<? extends TotalReport>> reportClasses = new ArrayList<>();
        reportClasses.add(TotalFromToReport.class);
        reportClasses.add(TotalSnapshotReport.class);
        ArrayList<JComboBox<String>> comboBoxes = new ArrayList<>();
        comboBoxes.add(fromToReportComboBox);
        comboBoxes.add(snapReportComboBox);

        for (int i = 0; i < reportClasses.size(); i++) {
            ReportConfig.setDefaultConfigInPrefs(reportClasses.get(i));
            comboBoxes.get(i).addItem("NONE");
            comboBoxes.get(i).setPreferredSize(optionsBoxDimension);
            for (String reportName : ReportConfig.getReportNamesForClass(reportClasses.get(i))) {
                comboBoxes.get(i).addItem(reportName);
            }
        }
    }

    public void refreshReportNames() throws BackingStoreException, NoSuchFieldException,
            IllegalAccessException {
        ArrayList<Class<? extends TotalReport>> reportClasses = new ArrayList<>();
        reportClasses.add(TotalFromToReport.class);
        reportClasses.add(TotalSnapshotReport.class);
        ArrayList<JComboBox<String>> comboBoxes = new ArrayList<>();
        comboBoxes.add(fromToReportComboBox);
        comboBoxes.add(snapReportComboBox);
        for (JComboBox<String> comboBox : comboBoxes) {
            while (comboBox.getItemCount() > 0) {
                comboBox.removeItemAt(0);
            }
        }
        for (int i = 0; i < reportClasses.size(); i++) {
            comboBoxes.get(i).addItem("NONE");
            for (String reportName : ReportConfig.getReportNamesForClass(reportClasses.get(i))) {
                comboBoxes.get(i).addItem(reportName);
            }
            comboBoxes.get(i).setSelectedIndex(0);
        }
    }

    private String getDefaultDirectoryPath() {
        String defaultPath = root == null ? System.getProperty("user.home") :
                root.getDataFile().getParentFile().getAbsolutePath();
        File defaultPathFolder = new File(defaultPath);
        if (defaultPathFolder.canWrite()) {
            return defaultPath;
        } else {
            return System.getProperty("user.home");
        }
    }

    public static File getOutputFile(String fileName) {
        return new File(outputDirectory, fileName);
    }

    public static String getOutputDirectoryPath(){
        return outputDirectory.getAbsolutePath();
    }

    public DateRangePanel getDateRangePanel() {
        return datePanel;
    }

    private void openBrowserToDownloadFile() throws IOException {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(outputDirectory.toURI());
            }

        }

    }

    private void setDefaultsButtonActionPerformed() {
        setDefaultPreferences();
    }

    private void dirChooserButtonActionPerformed() {
        showFileChooser();
    }

    public void setDefaultPreferences() {

        transActivityCheckbox.setSelected(false);
        secPricesCheckbox.setSelected(false);

        aggregationOptionsComboBox.setSelectedIndex(0);
        costBasisOptionsComboBox.setSelectedIndex(0);

        directoryOutputField.setText(getDefaultDirectoryPath());
    }

    public static void setSevereError(boolean isSevereError){
        severeError = isSevereError;
    }

    public void setReportStatusFieldText(String message){
        reportStatusField.setText(message);
    }


    public void savePreferences() {

        prefs.putBoolean(Prefs.RUN_ACTIVITY,
                transActivityCheckbox.isSelected());
        prefs.putBoolean(Prefs.RUN_SECURITIES_PRICES,
                secPricesCheckbox.isSelected());

        prefs.putInt(Prefs.AGGREGATION_OPTIONS,
                aggregationOptionsComboBox.getSelectedIndex());
        prefs.putInt(Prefs.COST_BASIS_USED,
                costBasisOptionsComboBox.getSelectedIndex());

        prefs.put(Prefs.EXPORT_DIR_PATH, directoryOutputField.getText());

    }

    public String showErrorMessage(String message){
        return message + " See Log in " + directoryOutputField.getText()
                + " for details";
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

    public void loadMDFile(File mdFile) {
        new MDFileLoader(mdFile).execute();
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

    class CustomizeReportButtonListener implements ActionListener {
        ReportControlFrame reportControlFrame;

        CustomizeReportButtonListener(ReportControlFrame reportControlFrame) {
            this.reportControlFrame = reportControlFrame;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                ReportConfigControlFrame.setReportConfigControlFrame(reportControlFrame);
            } catch (IllegalAccessException | NoSuchFieldException | BackingStoreException e1) {
                String msg = " Exception on Report Customization: ";
                LogController.logException(e1, msg);
                setReportStatusFieldText(showErrorMessage(msg));
            }
        }
    }

    private class MDFileLoader extends SwingWorker<Void, String> {
        File mdFile;

        MDFileLoader(File mdFile) {
            this.mdFile = mdFile;
        }

        @Override
        protected Void doInBackground() throws Exception {
            try {
                publish("Loading " + mdFile.getParentFile().getName());
                root = FileUtils.readAccountsFromFile(mdFile, null);
                publish(mdFile.getParentFile().getName() + " Loaded! Choose Reports to run.");
            } catch (Exception e) {
                publish("Error! " + mdFile.getParentFile().getName() + " not loaded!");
                LogController.logException(e, "Error on Loading MD File: ");
            }
            return null;
        }

        @Override
        protected void process(java.util.List<String> msgs) {
            String output = "";
            for (String msg : msgs) {
                output += (msg + " ");
            }
            reportStatusField.setText(output);
        }

    }



    private class ReportExecutor extends SwingWorker<Void, String> {

        @Override
        protected Void doInBackground() throws Exception {
            if(severeError) {
                return null;
            }
            publish("Reports are running...");
            int costBasisType = costBasisOptionsComboBox.getSelectedIndex();
            GainsCalc implGainsCalc;
            if (costBasisType == 0) {
                implGainsCalc = new GainsAverageCalc();
            } else {
                implGainsCalc = new GainsLotMatchCalc();
            }
            //load BulkSecInfo...
            if (root != null) {
                try {
                    currentInfo = new BulkSecInfo(root, implGainsCalc);
                } catch (Exception e) {
                    LogController.logException(e, "Error on loading security information from datafile: ");
                    publish(showErrorMessage("Error--Could not load securities from data file!"));
                }
            }
            //Now Run Reports...
            if (!severeError && currentInfo != null) {
                try {
                    AggregationController aggregationController = (AggregationController) aggregationOptionsComboBox.getSelectedItem();


                    if (snapReportComboBox.getSelectedIndex() != 0) {
                        ReportConfig reportConfig = new ReportConfig(TotalSnapshotReport.class,
                                (String) snapReportComboBox.getSelectedItem());
                        if (datePanel.getDateRange().isValid()) reportConfig.setDateRange(datePanel.getDateRange());
                        // selected@
                        if (aggregationController != AggregationController.DEFAULT)
                            reportConfig.setAggregationController(aggregationController);
                        TotalReport report = new TotalSnapshotReport(reportConfig);
                        report.calcReport(currentInfo);
                        report.displayReport();
                    }

                    if (fromToReportComboBox.getSelectedIndex() != 0) {
                        ReportConfig reportConfig = new ReportConfig(TotalFromToReport.class,
                                (String) fromToReportComboBox.getSelectedItem());
                        if (datePanel.getDateRange().isValid()) reportConfig.setDateRange(datePanel.getDateRange());
                        // selected@
                        if (aggregationController != AggregationController.DEFAULT)
                            reportConfig.setAggregationController(aggregationController);
                        TotalReport report = new TotalFromToReport(reportConfig);
                        report.calcReport(currentInfo);
                        report.displayReport();
                    }

                    if (transActivityCheckbox.isSelected()) {
                        ArrayList<String[]> transActivityReport
                                = currentInfo.listAllTransValues(currentInfo.getInvestmentWrappers());
                        File transActivityReportFile = getOutputFile("transActivityReport.csv");
                        IOUtils.writeArrayListToCSV(TransactionValues.listTransValuesHeader(),
                                transActivityReport, transActivityReportFile);
                    }
                    if (secPricesCheckbox.isSelected()) {
                        ArrayList<String[]> secPricesReport
                                = BulkSecInfo.ListAllCurrenciesInfo(currentInfo.getCurrencyWrappers());
                        File secPricesReportFile = getOutputFile("secPricesReport.csv");
                        IOUtils.writeArrayListToCSV(BulkSecInfo.listCurrencySnapshotHeader(),
                                secPricesReport,
                                secPricesReportFile);
                    }

                    if (transActivityCheckbox.isSelected()
                            || secPricesCheckbox.isSelected()) {
                        openBrowserToDownloadFile();
                    }

                } catch (Exception e) {
                    LogController.logException(e, "Error on running reports: ");
                    publish(showErrorMessage("Error--Could not run reports!"));
                }
            } else {
                publish(showErrorMessage("Error--Reports not run! "));
            }
            if (!severeError) publish("Reports have been run!");
            return null;
        }

        @Override
        protected void process(java.util.List<String> msgs) {
            String output = "";
            for (String msg : msgs) {
                output += (msg + " ");
            }
            reportStatusField.setText(output);
        }

    }


}
