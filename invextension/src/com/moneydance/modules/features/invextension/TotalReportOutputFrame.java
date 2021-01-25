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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serial;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;


/**
 * Handle external controls for TotalReportOutputPane
 */
class TotalReportOutputFrame extends JFrame implements ActionListener, ItemListener, PropertyChangeListener {
    public static final String SET_FROZEN_COLUMNS = "setFrozenColumns";
    public static final String REFRESH_PRICES = "refreshPrices";
    public static final String SORT_ROWS = "sortRows";
    public static final String COPY_CLIPBOARD = "copyClipboard";
    public static final String SWITCH_RETURN_TYPE = "switchReturnType";
    @Serial
    private static final long serialVersionUID = 2199471200123995601L;
    public static final int textFieldWidth = 305;
    public static final int textFieldHeight = 60;
    public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("H:mm:ss z");
    TotalReportOutputPane totalReportOutputPane;
    String frameText;
    ReportConfig reportConfig;
    JComboBox<Integer> freezeColsBox;
    JCheckBox hideClosedBox;
    JButton sortButton;
    JButton switchReturnTypeButton;
    JButton copyToClipboardButton;
    JComboBox<String> refreshPricesComboBox;
    String[] refreshPricesInterval = new String[]{"NA", "5", "10", "15", "60"};
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
        JPanel refreshPricesPanel = new JPanel();
        refreshPricesPanel.setLayout(new BoxLayout(refreshPricesPanel, BoxLayout.Y_AXIS));


        Integer[] freezeCols = new Integer[]{0, 1, 2, 3, 4, 5};
        JLabel freezeColsLabel = new JLabel("Set Frozen Columns  ");
        freezeColsBox = new JComboBox<>(freezeCols);
        freezeColsBox.setSelectedIndex(reportConfig.getNumFrozenColumns());
        sortButton = new JButton("Sort Table");
        hideClosedBox = new JCheckBox("Hide Positions with Zero Value", reportConfig.isClosedPosHidden());
        switchReturnTypeButton = new JButton();
        switchReturnTypes(true);
        copyToClipboardButton = new JButton("Copy Table to Clipboard");

        refreshPricesComboBox = new JComboBox<>(refreshPricesInterval);
        refreshPricesComboBox.setToolTipText("Run Yahoo Quotes Update Extension periodically, refresh");
        JLabel refreshPricesLabel = new JLabel("Refresh Prices Interval  ");
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

        reportStatusPane.setPreferredSize(new Dimension(textFieldWidth, textFieldHeight));
        reportStatusText.setLineWrap(false);

        refreshPricesComboBox.addActionListener(this);
        refreshPricesComboBox.setActionCommand(REFRESH_PRICES);
        refreshPricesPanel.add(refreshPricesLabel);
        refreshPricesPanel.add(refreshPricesComboBox);
        refreshPricesLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        refreshPricesComboBox.setAlignmentX(Component.LEFT_ALIGNMENT);


        freezeColsPanel.add(freezeColsLabel);
        freezeColsPanel.add(freezeColsBox);

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 2, 0, 2);
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
        controlPanel.add(refreshPricesPanel, c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 7;
        controlPanel.add(editInstructionLabel, c);

        c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(0, 0, 10, 0);
        this.add(controlPanel, c);
        c.gridx = 1;
        this.add(reportStatusPane, c);

        c.insets = new Insets(0, 0, 0, 0);
        c.gridx = 0;
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
            if(actionCommand.equals(REFRESH_PRICES)){
                startRefreshPrices();
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
    }

    public void startRefreshPrices() throws InterruptedException {

        MDData mdData = MDData.getInstance();
        int selectedIndex = refreshPricesComboBox.getSelectedIndex();
        if(selectedIndex == 0){
            updateStatus("Refresh Prices Stopped...");
            mdData.getObservableLastTransactionDate().removePropertyChangeListener(this);
            mdData.stopTransactionMonitorThread();
        } else {
            updateStatus("Refresh Prices Started...");
            reportLatestPriceTime(mdData.getLastPriceUpdateTime());
            if(!isLiveReport()) updateStatus("Warning: Report end date is not today!");
            long updateFrequencyMins = Long.parseLong(refreshPricesInterval[selectedIndex]);
            mdData.getObservableLastTransactionDate().addPropertyChangeListener(this);
            mdData.startTransactionMonitorThread(reportConfig, updateFrequencyMins);
        }
    }

    private boolean isLiveReport(){
        Class<? extends TotalReport> reportClass = reportConfig.getReportClass();
        if(reportClass.equals(TotalFromToReport.class)){
            return DateUtils.isToday(reportConfig.getDateRange().getToDateInt());
        } else {
            return DateUtils.isToday(reportConfig.getDateRange().getSnapDateInt());
        }
    }


    public void reportLatestPriceTime(Date date){
        String nowStr = TIME_FORMAT.format(new Date());
        updateStatus("As Of: " + nowStr + " Last Price: " + MDData.DATE_PATTERN_MEDIUM.format(date));
    }

    void updateStatus(String msg) {
        String output = reportStatusText.getText();
        output += (msg + "\n");
        reportStatusText.setText(output);
        reportStatusText.setCaretPosition(reportStatusText.getDocument().getLength());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("lastTransactionDate")){
            try {
                MDData mdData = MDData.getInstance();
                HashMap<String, Double> lastUserRateMap = new HashMap<>(mdData.getUserRateMap());
                mdData.reloadMDData(reportConfig);
                boolean reloadReport = mdData.hasNewUserRate(lastUserRateMap);
                if(reloadReport){
                    reportLatestPriceTime(mdData.getLastPriceUpdateTime());
                    totalReportOutputPane.getModel().refreshReport(mdData.getCurrentInfo());
                    totalReportOutputPane.sortRows();
                    totalReportOutputPane.repaint();
                    totalReportOutputPane.sortRows();
                }
            } catch (Exception e) {
                LogController.logException(e, "Error on Report Output Pane: ");
            }

        }

    }

    class OutputFrameWindowAdapter extends WindowAdapter {

        OutputFrameWindowAdapter() { super(); }
        @Override
        public void windowClosing(WindowEvent we) {
            try {
                TotalReportOutputFrame.this.setFrameInfo();
                MDData mdData = MDData.getInstance();
                mdData.stopTransactionMonitorThread();
            } catch (InterruptedException e) {
                LogController.logException(e, "Error on Total Report Window Close");
            }
        }
    }


}
