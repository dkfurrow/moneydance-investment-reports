/*
 * TotalReportOutputFrame.java
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
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.Observable;
import java.util.Observer;

/**
 * Handle external controls for TotalReportOutputPane
 */
class TotalReportOutputFrame extends JFrame implements ActionListener, ItemListener, Observer {
    public static final String SET_FROZEN_COLUMNS = "setFrozenColumns";
    public static final String SORT_ROWS = "sortRows";
    public static final String COPY_CLIPBOARD = "copyClipboard";
    public static final String SWITCH_RETURN_TYPE = "switchReturnType";
    private static final long serialVersionUID = 2199471200123995601L;
    public static final int textFieldWidth = 300;
    TotalReportOutputPane totalReportOutputPane;
    String frameText;
    ReportConfig reportConfig;
    JComboBox<Integer> freezeColsBox;
    JCheckBox hideClosedBox;
    JButton sortButton;
    JButton switchReturnTypeButton;
    JButton copyToClipboardButton;
    JCheckBox refreshPricesBox;
    Date lastPriceUpdateOnSelection;
    private JTextArea reportStatusText = new javax.swing.JTextArea();
    private JScrollPane reportStatusPane = new JScrollPane(reportStatusText);
    private boolean returnTypeSwitched = false;


    public TotalReportOutputFrame(TotalReportOutputPane totalReportOutputPane, String frameText) {
        this.totalReportOutputPane = totalReportOutputPane;
        this.frameText = frameText;
        this.reportConfig = totalReportOutputPane.getReportConfig();
        initComponents();
    }

    private void initComponents() {
        this.setTitle(frameText);
        this.getContentPane().setLayout(new GridBagLayout());
        final JPanel controlPanel = new JPanel(new GridBagLayout());
        JPanel freezeColsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,
                0, 0));

        Integer[] freezeCols = new Integer[]{0, 1, 2, 3, 4, 5};
        JLabel freezeColsLabel = new JLabel("Set Frozen Columns  ");
        freezeColsBox = new JComboBox<>(freezeCols);
        freezeColsBox.setSelectedIndex(reportConfig.getNumFrozenColumns());
        sortButton = new JButton("Sort Table");
        hideClosedBox = new JCheckBox("Hide Positions with Zero Value", reportConfig.isClosedPosHidden());
        switchReturnTypeButton = new JButton();
        switchReturnTypes(true);
        copyToClipboardButton = new JButton("Copy Table to Clipboard");
        refreshPricesBox = new JCheckBox("Refresh Security Prices");
        refreshPricesBox.setToolTipText("Run Yahoo Quotes Update Extension Every 5 minutes, refresh");
        JLabel editInstructionLabel = new JLabel("Double-Click Security to Edit Properties, " +
                "Returns for calculation backup");
        editInstructionLabel.setFont(editInstructionLabel.getFont().deriveFont(Font.ITALIC, 10));

        Border blackline = BorderFactory.createLineBorder(Color.BLACK);

        hideClosedBox.setBorder(blackline);
        freezeColsPanel.setBorder(blackline);
        controlPanel.setBorder(blackline);

        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.addWindowListener(new OutputFrameWindowAdapter());

        freezeColsBox.addActionListener(this);
        freezeColsBox.setActionCommand(SET_FROZEN_COLUMNS);

        sortButton.addActionListener(this);
        sortButton.setActionCommand(SORT_ROWS);

        switchReturnTypeButton.addActionListener(this);
        switchReturnTypeButton.setActionCommand(SWITCH_RETURN_TYPE);

        copyToClipboardButton.addActionListener(this);
        copyToClipboardButton.setActionCommand(COPY_CLIPBOARD);

        hideClosedBox.addItemListener(this);

        reportStatusPane.setPreferredSize(new Dimension(textFieldWidth, 40));
        reportStatusText.setWrapStyleWord(true);
        reportStatusText.setLineWrap(true);

        refreshPricesBox.addItemListener(this);

        freezeColsPanel.add(freezeColsLabel);
        freezeColsPanel.add(freezeColsBox);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 5, 2, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        controlPanel.add(freezeColsPanel, c);
        c.gridx++;
        controlPanel.add(sortButton, c);
        c.gridx++;
        controlPanel.add(hideClosedBox, c);
        c.gridx++;
        controlPanel.add(switchReturnTypeButton, c);
        c.gridx++;
        controlPanel.add(copyToClipboardButton, c);
        c.gridx++;
        controlPanel.add(refreshPricesBox, c);
        c.gridx++;
        controlPanel.add(reportStatusPane, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 4;
        controlPanel.add(editInstructionLabel, c);

        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(0, 0, 10, 0);

        this.add(controlPanel, c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;

        this.getContentPane().add(totalReportOutputPane, c);
        this.setLocation(reportConfig.getFrameInfo().getPoint());
        this.pack(); //added to accommodate field chooser
        this.setSize(reportConfig.getFrameInfo().getDimension());

    }

    private void switchReturnTypes(boolean isInitialSet){
        if(!isInitialSet) {
            totalReportOutputPane.switchReturnType();
            returnTypeSwitched = !returnTypeSwitched;
        }
        boolean useOrdinary = reportConfig.useOrdinaryReturn();
        String buttonText;
        if(!returnTypeSwitched){
            buttonText = useOrdinary ? "Switch to Time-Weighted Total Return" : "Switch to Ordinary Return";
        } else {
            buttonText = useOrdinary ? "Switch to Ordinary Return" : "Switch to Time-Weighted Total Return";
        }
        switchReturnTypeButton.setText(buttonText);
    }

    public void showFrame() {
        this.setVisible(false);
        this.totalReportOutputPane.sortRows();
        totalReportOutputPane.setFrozenColumns(reportConfig.getNumFrozenColumns()); //behavior doesn't work if placed before setVisible
        this.setVisible(true);
    }

    public void setFrameInfo() {
        reportConfig.setFrameInfoToPrefs(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String actionCommand = e.getActionCommand();
            if (actionCommand.equals(SET_FROZEN_COLUMNS)) {
                totalReportOutputPane.setFrozenColumns(freezeColsBox.getSelectedIndex());
            }
            if (actionCommand.equals(SORT_ROWS)) {
                totalReportOutputPane.sortRows(new Point(this.getLocationOnScreen()));
            }
            if (actionCommand.equals(SWITCH_RETURN_TYPE)){
                switchReturnTypes(false);
            }
            if (actionCommand.equals(COPY_CLIPBOARD)) {
                totalReportOutputPane.copyTableToClipboard();
            }

        } catch (Exception e1) {
            LogController.logException(e1, "Error on Report Output Pane: ");
            JOptionPane.showMessageDialog(this, "Error! See " +
                            ReportControlPanel.getOutputDirectoryPath() + " for details", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getSource().equals(hideClosedBox)) {
            totalReportOutputPane.closedPosHidden = (e.getStateChange() == ItemEvent.SELECTED);
            totalReportOutputPane.sortRows();
            totalReportOutputPane.setFrozenColumns(0); // repaint doesn't work properly w/o this line
            totalReportOutputPane.setFrozenColumns(freezeColsBox.getSelectedIndex());
        }
        if (e.getSource().equals(refreshPricesBox)){
            MDData mdData = MDData.getInstance();
            if(e.getStateChange() == ItemEvent.SELECTED){
                mdData.getLastTransactionDate().addObserver(this);
                mdData.startTransactionMonitorThread(reportConfig);
                lastPriceUpdateOnSelection = mdData.getLastPriceUpdatedDate();
                String updateText = "Latest Price: " + MDData.DATE_PATTERN_LONG.format(lastPriceUpdateOnSelection);
                updateStatus(updateText);
            } else {
                mdData.getLastTransactionDate().deleteObserver(this);
                mdData.stopTransactionMonitorThread();
            }
        }
    }

    void updateStatus(String msg) {
        String output = reportStatusText.getText();
        output += (msg + "\n");
        reportStatusText.setText(output);
    }

    @Override
    public void update(Observable o, Object arg) {
        try {
            if(o instanceof MDData.ObservableLastTransactionDate){
                String msg = "";
                MDData.ObservableLastTransactionDate lastTransactionDate = (MDData.ObservableLastTransactionDate) o;
                Date previousLastDate = lastTransactionDate.getPreviousLastTransactionDate();
                Date currentLastDate = lastTransactionDate.getLastTransactionDate();
                MDData mdData = MDData.getInstance();
                mdData.reloadMDData(reportConfig);
                Date newLastPriceDate = mdData.getLastPriceUpdatedDate();
                boolean reloadReport = newLastPriceDate.after(lastPriceUpdateOnSelection); //TODO: Review Timing of subsequent updates
                updateStatus("Reload Report: " + reloadReport); //TODO: Remove after debug
                if(reloadReport){
                    msg = "New Price Date " + MDData.DATE_PATTERN_LONG.format(newLastPriceDate);
                    totalReportOutputPane.getModel().refreshReport(mdData.getCurrentInfo());
                    totalReportOutputPane.sortRows();
                    totalReportOutputPane.repaint();
                    totalReportOutputPane.sortRows();

                }
                updateStatus(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    class OutputFrameWindowAdapter extends WindowAdapter {

        OutputFrameWindowAdapter() { super(); }
        @Override
        public void windowClosing(WindowEvent we) {
            TotalReportOutputFrame.this.setFrameInfo();
        }
    }


}
