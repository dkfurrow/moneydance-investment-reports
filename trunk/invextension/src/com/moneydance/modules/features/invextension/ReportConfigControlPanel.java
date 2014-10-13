/*
 * ReportConfigControlPanel.java
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

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.BackingStoreException;

import static com.moneydance.modules.features.invextension.ReportConfig.getReportTypeName;


/**
 * multi-panel which controls report configuration options
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
public class ReportConfigControlPanel extends JPanel {
    private static final long serialVersionUID = -1521332923539321021L;
    ReportControlFrame reportControlFrame;
    JButton saveConfigButton;
    ReportConfig selectedReportConfig;
    OtherConfigsPanel otherConfigsPanel;
    DateRangePanel datePanel;
    ReportConfigTreePanel reportTreePanel;
    ReportConfigFieldChooserPanel fieldChooserPanel;
    private JFrame reportConfigControlFrame; //implements ActionListener


    /**
     * Creates new form NewJPanel
     */
    public ReportConfigControlPanel(JFrame reportConfigControlFrame, ReportControlFrame reportControlFrame) throws IllegalAccessException, BackingStoreException,
            NoSuchFieldException {
        this.reportConfigControlFrame = reportConfigControlFrame;
        this.reportControlFrame = reportControlFrame;
        initComponents();
    }

    public static void main(String[] args) throws IllegalAccessException, BackingStoreException, NoSuchFieldException {
        ReportControlPanel.TestFrame frame = new ReportControlPanel.TestFrame();
        ReportConfigControlPanel testPanel = new ReportConfigControlPanel(frame, null);
        frame.addPanel(testPanel);
    }

    private void initComponents() throws IllegalAccessException, BackingStoreException, NoSuchFieldException {
        // Setup SaveConfigButton
        saveConfigButton = new JButton("Save Current Configuration");
        saveConfigButton.addActionListener(new SaveConfigActionListener());
        saveConfigButton.setForeground(Color.RED);
        Font defaultFault = saveConfigButton.getFont();
        Font newFont = new Font(defaultFault.getName(), Font.BOLD, 14);
        saveConfigButton.setFont(newFont);

        // Create and format sub-panels to load into main panel
        JPanel allReportOptionsPanel = new JPanel();
        otherConfigsPanel = new OtherConfigsPanel();
        datePanel = new DateRangePanel(new DateRange());
        reportTreePanel = new ReportConfigTreePanel(this);
        fieldChooserPanel = new ReportConfigFieldChooserPanel();

        // Set all panel borders the same
        String[] titles = {"All Report Options", "Date Options", "Choose Fields"};
        JPanel[] panels = {allReportOptionsPanel, datePanel, fieldChooserPanel};

        for (int i = 0; i < panels.length; i++) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            panels[i].setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        }
        GridBagConstraints c;
        //lay out all Report Options
        allReportOptionsPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.NORTH;
        allReportOptionsPanel.add(saveConfigButton, c);
        c.gridy++;
        c.anchor = GridBagConstraints.NORTHWEST;
        allReportOptionsPanel.add(otherConfigsPanel, c);
        c.gridy++;
        allReportOptionsPanel.add(datePanel, c);

        //create tabbed Pane
        JTabbedPane reportTabbedPane = new JTabbedPane();
        reportTabbedPane.addTab("Main Report Options", null, allReportOptionsPanel, "Choose main options");
        reportTabbedPane.addTab("Field Chooser", null, fieldChooserPanel, "Choose Display Fields");

        // lay out main panel
        this.setLayout(new GridBagLayout());
        c = new GridBagConstraints();

        //add reportTree
        c.anchor = GridBagConstraints.NORTH;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        this.add(reportTreePanel, c);

        c.gridx = 1;
        this.add(reportTabbedPane, c);
    }

    /**
     * Saves Report Config (subject to validation checks)
     * @throws IllegalAccessException
     * @throws BackingStoreException
     * @throws NoSuchFieldException
     */
    public void saveSelectedReportConfig() throws IllegalAccessException, BackingStoreException,
            NoSuchFieldException {
        if (selectedReportConfig != null) {
            DateRange dateRange = datePanel.getDateRange();
            this.selectedReportConfig.setDateRange(dateRange);
            otherConfigsPanel.updateReportConfig(this);
            fieldChooserPanel.updateReportConfig(this);
            boolean errorStandardSave = otherConfigsPanel.getReportName().trim().equals(Prefs.STANDARD_NAME.trim());
            boolean errorAggregationChoice = otherConfigsPanel.getAggregationController() == AggregationController.DEFAULT;
            boolean errorDateRange = !datePanel.getDateRange().isValid();
            if (errorStandardSave || errorAggregationChoice || errorDateRange) {
                String errorAggregationChoiceMsg = "Cannot choose this Aggregation Option ! Choose another. \n";
                String errorStandardSaveMsg = "Cannot save over Standard configuration. \nChoose another report name.";
                String errorDateRangeMsg = "Invalid Date Range. Choose a valid date range";
                String errorMessage = "";
                if (errorStandardSave) errorMessage += errorStandardSaveMsg;
                if (errorDateRange) errorMessage += errorDateRangeMsg;
                if (errorAggregationChoice) errorMessage += errorAggregationChoiceMsg;
                JOptionPane.showMessageDialog(reportConfigControlFrame, errorMessage);
            } else {
                selectedReportConfig.setStandardConfig(false);
                selectedReportConfig.saveReportConfig();
            }

            reportTreePanel.refreshTree();
        } else {
            JOptionPane.showMessageDialog(reportConfigControlFrame, "No Report Type--Must select report in tree diagram\n" +
                    "in order to save a report configuration!");
        }
    }

    public ReportConfig getSelectedReportConfig() {
        return this.selectedReportConfig;
    }

    public void setSelectedReportConfig(ReportConfig reportConfig) {
        this.selectedReportConfig = reportConfig;
    }

    public JFrame getReportConfigControlFrame() {
        return reportConfigControlFrame;
    }

    public void populateSubPanels() throws NoSuchFieldException, IllegalAccessException {
        otherConfigsPanel.populateOtherConfigsPanel();
        datePanel.populateDateRangePanel(selectedReportConfig.getDateRange());
        fieldChooserPanel.populateFieldChooser(selectedReportConfig);
    }

    public void resetReportPanelPrefs(){
        reportControlFrame.getReportControlPanel().setPreferences();
    }

    private class SaveConfigActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                saveSelectedReportConfig();
            } catch (IllegalAccessException | BackingStoreException | NoSuchFieldException e1) {
                LogController.logException(e1, "Error on Saving Report Config: ");
                ReportConfigControlFrame.setExceptionThrown(true);
                ReportConfigControlFrame.showErrorDialog(ReportConfigControlPanel.this);
            }
        }
    }

    /**
     * Controls Aggregation, number of frozen panels, etc
     */
    public class OtherConfigsPanel extends JPanel {

        private static final long serialVersionUID = 2476680585088267452L;
        private Integer[] numFrozenColumnsOptions = {0, 1, 2, 3, 4, 5};


        private JLabel reportTypeLabel = new JLabel("Report Type");
        private JTextField reportTypeText = new JTextField(30);

        private JLabel reportNameLabel = new JLabel("Report Name");
        private JTextField reportNameText = new JTextField(30);
        private JLabel aggregationOptionsLabel = new JLabel("Report Aggregation Options");
        private JComboBox<AggregationController> aggregationOptionsComboBox =
                new JComboBox<>(new DefaultComboBoxModel<>(AggregationController.values()));
        private AggregationController aggregationController;
        private JLabel aggregateSingleLabel = new JLabel("Show Aggregates for Single Row?");
        private JCheckBox aggregateSingleCheckBox = new JCheckBox("Show Aggregates for Single Row" +
                "Security", false);
        private JLabel numFrozenColumnsLabel = new JLabel("Display-Number of Frozen Columns");
        private JComboBox<Integer> numFrozenColumnsComboBox = new JComboBox<>(numFrozenColumnsOptions);
        private JLabel hideClosedPosLabel = new JLabel("Hide Closed Positions?");
        private JCheckBox hideClosedPosCheckBox = new JCheckBox("Hide Closed Positions", true);

        public OtherConfigsPanel() {
            //initialize sub-panels
            JPanel masterPanel = new JPanel();
            //format sub-panels
            TitledBorder titledBorder = BorderFactory.createTitledBorder("Basic Report Options");
            Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            masterPanel.setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
            //set reportType Text editable to false
            reportTypeText.setEditable(false);
            // Layout sub-panels
            GridBagConstraints c;
            masterPanel.setLayout(new GridBagLayout());
            c = new GridBagConstraints();
            c.insets = new Insets(0, 5, 0, 5); // left-right padding only
            c.weightx = 1;  //justify everything vertically
            //labels
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            c.gridy = 0;
            //add labels
            masterPanel.add(reportTypeLabel, c);
            c.gridy++;
            masterPanel.add(reportNameLabel, c);
            c.gridy++;
            masterPanel.add(aggregationOptionsLabel, c);
            c.gridy++;
            masterPanel.add(aggregateSingleLabel, c);
            c.gridy++;
            masterPanel.add(numFrozenColumnsLabel, c);
            c.gridy++;
            masterPanel.add(hideClosedPosLabel, c);
            //data
            c.gridx = 1;
            c.gridy = 0;
            //add data fields
            masterPanel.add(reportTypeText, c);
            c.gridy++;
            masterPanel.add(reportNameText, c);
            c.gridy++;
            masterPanel.add(aggregationOptionsComboBox, c);
            c.gridy++;
            masterPanel.add(aggregateSingleCheckBox, c);
            c.gridy++;
            masterPanel.add(numFrozenColumnsComboBox, c);
            c.gridy++;
            masterPanel.add(hideClosedPosCheckBox, c);
            this.add(masterPanel);
        }

        public void populateOtherConfigsPanel() throws NoSuchFieldException, IllegalAccessException {
            reportTypeText.setText(getReportTypeName(selectedReportConfig.getReportClass()));
            reportNameText.setText(selectedReportConfig.getReportName());
            aggregationOptionsComboBox.setSelectedItem(selectedReportConfig.getAggregationController());
            aggregateSingleCheckBox.setSelected(selectedReportConfig.isOutputSingle());
            numFrozenColumnsComboBox.setSelectedIndex(selectedReportConfig.getNumFrozenColumns());
            hideClosedPosCheckBox.setSelected(selectedReportConfig.isClosedPosHidden());

        }

        public void updateReportConfig(ReportConfigControlPanel reportConfigControlPanel) {
            ReportConfig reportConfig = reportConfigControlPanel.getSelectedReportConfig();
            reportConfig.setReportName(reportNameText.getText());
            aggregationController = (AggregationController) aggregationOptionsComboBox.getSelectedItem();
            reportConfig.setAggregationController(aggregationController);
            reportConfig.setOutputSingle(aggregateSingleCheckBox.isSelected());
            reportConfig.setNumFrozenColumns(numFrozenColumnsComboBox.getSelectedIndex());
            reportConfig.setClosedPosHidden(hideClosedPosCheckBox.isSelected());
        }


        public String getReportName() {
            return reportNameText.getText();
        }

        public AggregationController getAggregationController() {
            return aggregationController;
        }
    }


}
