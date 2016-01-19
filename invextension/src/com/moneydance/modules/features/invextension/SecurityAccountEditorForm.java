/*
 * SecurityAccountEditorForm.java
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

import com.infinitekind.moneydance.model.*;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashSet;

/**
 * Edits Security Account Information, initialized by double-click
 * on report output
 */
public class SecurityAccountEditorForm extends JFrame implements ActionListener {
    private static final long serialVersionUID = 4955951732443697372L;
    private static final String SECURITY_TYPE_CHANGED = "securityTypeChanged";
    private static final String ADD_SECURITY_SUBTYPE = "addSecuritySubType";
    private static final String UPDATE_SECURITY = "updateSecurity";
    JTextField securityNameTextField = new JTextField();
    JTextField tickerTextField = new JTextField();
    JComboBox<SecurityType> securityTypeComboBox = new JComboBox<>
            (new DefaultComboBoxModel<>(SecurityType.values()));
    JComboBox<String> securitySubTypeComboBox = new JComboBox<>();
    JButton addSecuritySubTypeButton = new BasicArrowButton(BasicArrowButton.EAST, Color.BLACK,
            Color.lightGray, Color.WHITE, Color.WHITE);
    JButton updateSecurityButton = new JButton("Update Security");
    private SecurityAccountWrapper securityAccountWrapper;
    private TotalReportOutputPane.FormattedTable table;


    public SecurityAccountEditorForm(SecurityAccountWrapper securityAccountWrapper, TotalReportOutputPane.FormattedTable table) {
        this.table = table;
        this.securityAccountWrapper = securityAccountWrapper;
        initComponents();
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */
    public static void createAndShowSecurityEditorForm(SecurityAccountWrapper securityAccountWrapper,
                                                       TotalReportOutputPane.FormattedTable table) {
        //Create and set up the window.
        //Create and set up the content pane.
        JFrame securityAccountEditorForm = new SecurityAccountEditorForm(securityAccountWrapper, table);
        securityAccountEditorForm.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        //Display the window.
        securityAccountEditorForm.pack();
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        Point point = pointerInfo.getLocation();
        securityAccountEditorForm.setLocation(point);
        securityAccountEditorForm.setVisible(true);
    }

    private void initComponents() {
        this.setTitle("Security Account Editor");
        JPanel mainPanel = new JPanel();
        JPanel buttonPanel = new JPanel();
        mainPanel.setBorder(new LineBorder(Color.BLACK));
        buttonPanel.setBorder(new LineBorder(Color.RED));

        JLabel securityNameTextLabel = new JLabel("Security Name");
        JLabel tickerTextLabel = new JLabel("Ticker Symbol");
        JLabel securityTypeTextLabel = new JLabel("Security Type");
        JLabel securitySubTypeTextLabel = new JLabel("Security Sub Type");

        //populate
        addSecuritySubTypeButton.setToolTipText("Add Security Subtype");
        securityNameTextField.setText(securityAccountWrapper.getName());
        tickerTextField.setText(securityAccountWrapper.getCurrencyWrapper().getName());
        securityTypeComboBox.setSelectedItem(securityAccountWrapper.getSecurityType());
        populateSecuritySubTypes(true);

        securityTypeComboBox.addActionListener(this);
        securityTypeComboBox.setActionCommand(SECURITY_TYPE_CHANGED);

        addSecuritySubTypeButton.addActionListener(this);
        addSecuritySubTypeButton.setActionCommand(ADD_SECURITY_SUBTYPE);

        updateSecurityButton.addActionListener(this);
        updateSecurityButton.setActionCommand(UPDATE_SECURITY);


        GridBagConstraints c = new GridBagConstraints();
        mainPanel.setLayout(new GridBagLayout());
        c.ipady = 2;
        c.ipadx = 2;
        c.insets = new Insets(1, 5, 1, 5); // left-right padding only
        c.anchor = GridBagConstraints.WEST;
        //labels
        c.gridx = 0;
        c.gridy = 0;
        mainPanel.add(securityNameTextLabel, c);
        c.gridy++;
        mainPanel.add(tickerTextLabel, c);
        c.gridy++;
        mainPanel.add(securityTypeTextLabel, c);
        c.gridy++;
        mainPanel.add(securitySubTypeTextLabel, c);
        // contents
        c.anchor = GridBagConstraints.EAST;
        c.gridx = 1;
        c.gridy = 0;
        mainPanel.add(securityNameTextField, c);
        c.gridy++;
        mainPanel.add(tickerTextField, c);
        c.gridy++;
        mainPanel.add(securityTypeComboBox, c);
        c.gridy++;
        mainPanel.add(securitySubTypeComboBox, c);
        c.gridx = 2;
        mainPanel.add(addSecuritySubTypeButton, c);

        buttonPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.CENTER;
        buttonPanel.add(updateSecurityButton, c);


        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.add(mainPanel);
        container.add(buttonPanel);
        this.add(container);

    }

    /**
     * populates Security Sub Types based on Security Type
     *
     * @param initial whether population is initial or subsequent
     */
    private void populateSecuritySubTypes(boolean initial) {
        SecurityType securityType = initial ? securityAccountWrapper.getSecurityType() :
                (SecurityType) securityTypeComboBox.getSelectedItem();
        LinkedHashSet<String> securitySubTypes = SecuritySubTypeWrapper.getSecuritySubtypeMap()
                .get(securityType);
        securitySubTypeComboBox.removeAllItems();
        for (String securitySubType : securitySubTypes) {
            securitySubTypeComboBox.addItem(securitySubType);
        }
        if (initial) securitySubTypeComboBox.setSelectedItem(securityAccountWrapper.getSecuritySubType());
    }

    /*
    Adds new security sub type based on user input
     */
    private void addSecuritySubType() {
        SecurityType securityType = (SecurityType) securityTypeComboBox.getSelectedItem();
        String msg = "Add security sub type for: " + securityType.name();
        String newSecuritySubType = (String) JOptionPane.showInputDialog(this, msg, "Add Security Sub Type",
                JOptionPane.PLAIN_MESSAGE, null, null, "");
        SecuritySubTypeWrapper.getSecuritySubtypeMap().get(securityType).add(newSecuritySubType);
        populateSecuritySubTypes(false);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals(SECURITY_TYPE_CHANGED)) {
            populateSecuritySubTypes(false);
        }
        if (actionCommand.equals(ADD_SECURITY_SUBTYPE)) {
            addSecuritySubType();
        }
        if (actionCommand.equals(UPDATE_SECURITY)) {
            updateSecurityWrapper();
        }

    }

    /**
     * updates both table and rootaccount for changed security information
     */
    private void updateSecurityWrapper() {
        String newSecurityName = securityNameTextField.getText().trim();
        String newTicker = tickerTextField.getText().trim();
        SecurityType newSecurityType = (SecurityType) securityTypeComboBox.getSelectedItem();
        String newSecuritySubType = (String) securitySubTypeComboBox.getSelectedItem();
        Account securityAccount = securityAccountWrapper.getSecurityAccount();
        CurrencyType currencyType = securityAccountWrapper.getCurrencyWrapper().getCurrencyType();
        currencyType.setName(newSecurityName);
        currencyType.setTickerSymbol(newTicker);
        securityAccount.setSecurityType(newSecurityType);
        securityAccount.setSecuritySubType(newSecuritySubType);
        syncAccountBook(securityAccount, securityAccountWrapper.getAccountBook());
        syncAccountBook(currencyType, securityAccountWrapper.getAccountBook());
        table.getReportTableModel().fireTableDataChanged();
        this.dispose();
    }

    /**
     * Syncs account book after making a change
     * @param syncableItem Syncable Item, e.g. Transaction, Account
     */
    public static void syncAccountBook(MoneydanceSyncableItem syncableItem, AccountBook accountBook){
        syncableItem.syncItem();
        accountBook.save();
    }


}
