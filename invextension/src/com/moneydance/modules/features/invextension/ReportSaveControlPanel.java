/*
 * ReportSaveControlPanel.java
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


/**
 * multi-panel which controls report configuration options
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
public class ReportSaveControlPanel extends JPanel implements ActionListener {
    private static final long serialVersionUID = -1521332923539321021L;
    private static final String SAVE_CONFIG = "saveConfig";
    ReportControlFrame reportControlFrame;
    private JFrame reportSaveControlFrame; //implements ActionListener

    private JLabel reportTypeLabel = new JLabel("Report Type");
    private JTextField reportTypeText = new JTextField(30);
    private JLabel reportNameLabel = new JLabel("Report Name");
    private JTextField reportNameText = new JTextField(30);
    JButton saveConfigButton;


    /**
     * Creates new form NewJPanel
     */
    public ReportSaveControlPanel(JFrame reportSaveControlFrame, ReportControlFrame reportControlFrame)
            throws IllegalAccessException, BackingStoreException,
            NoSuchFieldException {
        this.reportSaveControlFrame = reportSaveControlFrame;
        this.reportControlFrame = reportControlFrame;
        initComponents();
    }

    public static void main(String[] args) throws IllegalAccessException, BackingStoreException, NoSuchFieldException {
        ReportControlPanel.TestFrame frame = new ReportControlPanel.TestFrame();
        ReportSaveControlPanel testPanel = new ReportSaveControlPanel(frame, null);
        frame.addPanel(testPanel);
    }

    private void initComponents() throws IllegalAccessException, BackingStoreException, NoSuchFieldException {


        // Create and format sub-panels to load into main panel


        saveConfigButton = new JButton("Save Current Configuration");
        saveConfigButton.setActionCommand(SAVE_CONFIG);
        saveConfigButton.addActionListener(this);
        saveConfigButton.setForeground(Color.RED);
        Font defaultFault = saveConfigButton.getFont();
        Font newFont = new Font(defaultFault.getName(), Font.BOLD, 14);
        saveConfigButton.setFont(newFont);
        //initialize sub-panels

        //format sub-panels
        TitledBorder titledBorder = BorderFactory.createTitledBorder("Report Save Options");
        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        titledBorder.setTitleColor(new Color(100, 100, 100));
        this.setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        //set reportType Text editable to false, initialize with reporttype name
        ReportConfig reportConfig = reportControlFrame.getReportControlPanel().getReportConfig();
        reportTypeText.setText(reportConfig.getReportTypeName());
        reportTypeText.setEditable(false);
        // if report is not standard, start with existing name
        if(!reportConfig.getReportName().equals(Prefs.STANDARD_NAME))
            reportNameText.setText(reportConfig.getReportName());
        // Layout sub-panels
        GridBagConstraints c;
        this.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(0, 5, 0, 5); // left-right padding only
        c.weightx = 1;  //justify everything vertically
        //labels
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        //add labels
        this.add(reportTypeLabel, c);
        c.gridy++;
        this.add(reportNameLabel, c);
        //data
        c.gridx = 1;
        c.gridy = 0;
        //add data fields
        this.add(reportTypeText, c);
        c.gridy++;
        this.add(reportNameText, c);
        //add button
        c.gridwidth = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.insets = new Insets(10, 5, 10, 5); // left-right padding only
        c.gridy++;
        this.add(saveConfigButton, c);


    }

    /**
     * Saves Report Config (subject to validation checks)
     * @throws IllegalAccessException
     * @throws java.util.prefs.BackingStoreException
     * @throws NoSuchFieldException
     */
    private void saveSelectedReportConfig() throws IllegalAccessException, BackingStoreException,
            NoSuchFieldException {
        ReportConfig reportConfig = reportControlFrame.getReportControlPanel().getReportConfig();
        if (reportConfig != null) {

            String reportNameInput = reportNameText.getText().trim();
            boolean errorStandardSave = reportNameInput.equals(Prefs.STANDARD_NAME.trim());
            boolean errorNoName = reportNameInput.length() == 0;


            if (errorStandardSave || errorNoName) {
                String errorStandardSaveMsg = "Cannot save over Standard configuration. \nChoose another report name.";
                String errorNoNameMsg = "Please choose an appropriate name for your customized report";
                String errorMessage = "";
                if(errorStandardSave) errorMessage += errorStandardSaveMsg;
                if(errorNoName) errorMessage += errorNoNameMsg;
                JOptionPane.showMessageDialog(reportSaveControlFrame, errorMessage);
            } else {
                reportConfig.setStandardConfig(false);
                reportConfig.setReportName(reportNameInput);
                reportConfig.saveReportConfig();
                reportControlFrame.getReportControlPanel().setNewReportName(reportNameInput);
            }

        } else {
            JOptionPane.showMessageDialog(reportSaveControlFrame, "No Report Type--Must select report in tree diagram\n" +
                    "in order to save a report configuration!");
        }
    }



    @Override
    public void actionPerformed(ActionEvent e) {
        if(e.getActionCommand().equals(SAVE_CONFIG)){
            try {
                saveSelectedReportConfig();
                reportSaveControlFrame.dispose();
            } catch (IllegalAccessException | BackingStoreException | NoSuchFieldException e1) {
                LogController.logException(e1, "Error on Saving Report Config: ");
                ReportSaveControlFrame.showErrorDialog(ReportSaveControlPanel.this);
            }
        }
    }

}
