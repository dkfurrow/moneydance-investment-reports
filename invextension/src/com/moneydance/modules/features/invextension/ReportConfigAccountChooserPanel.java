/*
 * ReportConfigAccountChooserPanel.java
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

import com.infinitekind.moneydance.model.Account;
import com.moneydance.apps.md.controller.io.FileUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.TreeSet;

/**
 * Field chooser panel to control the order and identity of fields to be included
 * in a given report
 */
public class ReportConfigAccountChooserPanel extends JPanel {

    private static final long serialVersionUID = -8990699863699414946L;
    private Account root;
    private ReportControlPanel reportControlPanel;

    //JLists
    private final DefaultListModel<Account> availableAccountsListModel = new DefaultListModel<>();
    private final JList<Account> availableAccountsList = new JList<>(availableAccountsListModel);
    private final JScrollPane availableAccountPane = new JScrollPane(availableAccountsList);
    private final DefaultListModel<Account> includedAccountsListModel = new DefaultListModel<>();
    private final JList<Account> includedAccountsList = new JList<>(includedAccountsListModel);
    private final JScrollPane includedAccountsPane = new JScrollPane(includedAccountsList);
    private final JCheckBox removeInactiveAccountsBox = new JCheckBox("<HTML>Remove Inactive Accounts" +
            "</HTML>", false);
    private final JCheckBox removeHideOnHomepageAccountsBox = new JCheckBox("<HTML>Remove accounts with<br>" +
            "'Hide on Home Page' set</HTML>", false);

    private ReportConfigAccountChooserPanel() {
        initComponents();
    }

    public ReportConfigAccountChooserPanel(ReportControlPanel reportControlPanel) {
        this.reportControlPanel = reportControlPanel;
        initComponents();

    }

    @SuppressWarnings("unused")
    public static void main(String[] args) throws Exception {
        Class<? extends TotalReport> reportClass = TotalSnapshotReport.class;
        ReportConfig reportConfig = ReportConfig.getStandardReportConfig(reportClass);
        ReportConfigAccountChooserPanel testPanel = new ReportConfigAccountChooserPanel();
        String testFileStr1 = "E:\\\\RECORDS\\moneydance\\\\Test\\\\20141014test.moneydance\\\\root.mdinternal";
        String testFileStr2 = "E:\\\\RECORDS\\moneydance\\\\Test\\\\TestSave.moneydance\\\\root.mdinternal";
        File mdFile = new File(testFileStr2);
        testPanel.root = FileUtils.readAccountsFromFile(mdFile, null);
        testPanel.populateBothAccountLists(reportConfig);
        ReportControlPanel.TestFrame frame = new ReportControlPanel.TestFrame(testPanel);
    }

    private void initComponents() {
        //buttons
        JButton removeButton = new JButton("<<-Remove Accounts");
        JButton addButton = new JButton("Add Accounts->>");
        JButton resetButton = new JButton("Reset");

        //subPanels
        JPanel availableAccountsPanel = new JPanel();
        JPanel accountControlPanel = new JPanel();
        JPanel accountsIncludedPanel = new JPanel();

        String[] titles = {"Available Accounts", "Actions", "Accounts in Report"};
        JPanel[] panels = {availableAccountsPanel, accountControlPanel, accountsIncludedPanel};
        for (int i = 0; i < panels.length; i++) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            panels[i].setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        }


        availableAccountsPanel.add(availableAccountPane);
        accountsIncludedPanel.add(includedAccountsPane);
        //button panel
        removeInactiveAccountsBox.setBorderPainted(true);
        removeHideOnHomepageAccountsBox.setBorderPainted(true);
        accountControlPanel.setLayout(new GridLayout(5, 1));
        accountControlPanel.add(removeButton);
        accountControlPanel.add(addButton);
        accountControlPanel.add(resetButton);
        accountControlPanel.add(removeInactiveAccountsBox);
        accountControlPanel.add(removeHideOnHomepageAccountsBox);


        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 0;
        this.add(availableAccountsPanel, c);
        c.gridx++;
        c.anchor = GridBagConstraints.CENTER;
        this.add(accountControlPanel, c);
        c.gridx++;
        c.anchor = GridBagConstraints.NORTH;
        this.add(accountsIncludedPanel, c);

        //selection model
        includedAccountsList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        //listeners
        removeButton.addActionListener(new removeAccountsListener());
        addButton.addActionListener(new addAccountsListener());
        resetButton.addActionListener(new resetListener());
        removeInactiveAccountsBox.addActionListener(new removeInactiveAccountsListener());
        removeHideOnHomepageAccountsBox.addActionListener(new removeHideOnHomePageAccountListener());

        // renders
        availableAccountsList.setCellRenderer(new AccountCellRenderer());
        includedAccountsList.setCellRenderer(new AccountCellRenderer());

    }

    private void initializeHideInactiveAccountsButton() {
        boolean hideInactiveAccountsRemoved = true;
        HashSet<Account> hideInactiveAccounts = new HashSet<>();
        TreeSet<Account> investmentAccountSet
                = BulkSecInfo.getSelectedSubAccounts(root, Account.AccountType.INVESTMENT);

        for (Account investmentAccount : investmentAccountSet) {
            if (investmentAccount.getAccountIsInactive()) hideInactiveAccounts.add(investmentAccount);
        }
        for (int i = 0; i < includedAccountsListModel.size(); i++) {
            Account account = includedAccountsListModel.getElementAt(i);
            if (hideInactiveAccounts.contains(account)) {
                hideInactiveAccountsRemoved = false;
                break;
            }
        }
        removeInactiveAccountsBox.setSelected(hideInactiveAccountsRemoved);
    }

    private void initializeHideOnHomePageButton() {
        boolean hideOnHomePageAccountsRemoved = true;
        HashSet<Account> hideOnHomePageAccounts = new HashSet<>();
        TreeSet<Account> investmentAccountSet
                = BulkSecInfo.getSelectedSubAccounts(root, Account.AccountType.INVESTMENT);

        for (Account investmentAccount : investmentAccountSet) {
            if (investmentAccount.getHideOnHomePage()) hideOnHomePageAccounts.add(investmentAccount);
        }
        for (int i = 0; i < includedAccountsListModel.size(); i++) {
            Account account = includedAccountsListModel.getElementAt(i);
            if (hideOnHomePageAccounts.contains(account)) {
                hideOnHomePageAccountsRemoved = false;
                break;
            }
        }
        removeHideOnHomepageAccountsBox.setSelected(hideOnHomePageAccountsRemoved);
    }

    private void removeInactiveAccounts() {
        for (int i = includedAccountsListModel.size() - 1; 0 <= i; i--) {
            Account account = includedAccountsListModel.getElementAt(i);
            if (account.getAccountIsInactive()) {
                moveFromIncludedToAvailable(i);
            }
        }
        if (reportControlPanel != null) updateReportConfig();
    }

    private void removeHideOnHomePageAccounts() {
        for (int i = includedAccountsListModel.size() - 1; 0 <= i; i--) {
            Account account = includedAccountsListModel.getElementAt(i);
            if (account.getHideOnHomePage()) {
                moveFromIncludedToAvailable(i);
            }
        }
        if (reportControlPanel != null) updateReportConfig();
    }

    public void populateBothAccountLists(ReportConfig reportConfig) throws Exception {
        availableAccountsListModel.removeAllElements();
        includedAccountsListModel.removeAllElements();

        populateAvailableAccountsList();
        populateIncludedAccountsList(reportConfig);

        Dimension dimension = reportControlPanel.getRelatedDimension(availableAccountPane);
        availableAccountPane.setPreferredSize(dimension);
        includedAccountsPane.setPreferredSize(dimension);

        initializeHideInactiveAccountsButton();
        initializeHideOnHomePageButton();
    }

    private void populateAvailableAccountsList() throws Exception {
        if (root == null) root = reportControlPanel.getRoot();
        if (root != null) {
            TreeSet<Account> investmentAccountSet
                    = BulkSecInfo.getSelectedSubAccounts(root, Account.AccountType.INVESTMENT);

            for (Account investmentAccount : investmentAccountSet) {
                availableAccountsListModel.addElement(investmentAccount);
            }
        } else {
            throw new Exception("Cannot obtain account list");
        }
    }

    private void populateIncludedAccountsList(ReportConfig reportConfig) {
        HashSet<Integer> excludedAccountsSet = reportConfig.getExcludedAccountNums();

        for (int i = availableAccountsListModel.size() - 1; 0 <= i; i--) {
            Account availableAccount = availableAccountsListModel.getElementAt(i);
            if (!excludedAccountsSet.contains(availableAccount.getAccountNum())) {
                moveFromAvailableToIncluded(i);
            }
        }
    }

    private void moveFromAvailableToIncluded(int pos) {
        Account acct = availableAccountsListModel.remove(pos);
        insertInAlphabeticalOrder(includedAccountsListModel, acct);
    }

    private void moveFromIncludedToAvailable(int pos) {
        Account acct = includedAccountsListModel.remove(pos);
        insertInAlphabeticalOrder(availableAccountsListModel, acct);
    }

    private void insertInAlphabeticalOrder(DefaultListModel<Account> model, Account acct) {
        for (int i = 0; i < model.size(); i++) {
            if (acct.getAccountName().compareTo(model.get(i).getAccountName()) < 0) {
                model.insertElementAt(acct, i);
                return;
            }
        }
        model.addElement(acct); // At end
    }

    private void updateReportConfig() {
        HashSet<Integer> excludedAccountNums = new HashSet<>();
        HashSet<Account> excludedAccounts = getExcludedAccountSet();

        for (Account account : excludedAccounts) {
            excludedAccountNums.add(account.getAccountNum());
        }

        reportControlPanel.getReportConfig().setExcludedAccountNums(excludedAccountNums);
    }

    private LinkedHashSet<Account> getExcludedAccountSet() {
        LinkedHashSet<Account> includedAccountSet = new LinkedHashSet<>();

        for (int i = 0; i < includedAccountsListModel.size(); i++) {
            includedAccountSet.add(includedAccountsListModel.get(i));
        }
        LinkedHashSet<Account> totalAccountSet = new LinkedHashSet<>();
        for (int i = 0; i < availableAccountsListModel.size(); i++) {
            totalAccountSet.add(availableAccountsListModel.get(i));
        }

        totalAccountSet.removeAll(includedAccountSet);
        return totalAccountSet;
    }

    private class removeAccountsListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int[] indices = includedAccountsList.getSelectedIndices();
            removeAccountRange(indices);
        }
    }

    private void removeAccountRange(int[] indices) {
        for (int i = indices.length - 1; 0 <= i; i--) {
            moveFromIncludedToAvailable(indices[i]);
        }
        int sizeRemaining = includedAccountsListModel.size();

        if (sizeRemaining == 0) { //Nobody's left, disable firing.
            refillIncludedAccounts();
            JOptionPane.showMessageDialog(this, "Must leave at least one account!");
        }

        if (reportControlPanel != null) {
            updateReportConfig();
        }
    }


    private class addAccountsListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int[] indices = availableAccountsList.getSelectedIndices();

            for (int i = indices.length - 1; 0 <= i; i--) {
                moveFromAvailableToIncluded(indices[i]);
            }

            if (reportControlPanel != null) {
                updateReportConfig();
            }
        }
    }

    private class resetListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            refillIncludedAccounts();
        }
    }

    private void refillInactiveAccounts() {
        for (int i = availableAccountsListModel.size() - 1; 0 <= i; i--) {
            moveFromAvailableToIncluded(i);
        }
        removeInactiveAccountsBox.setSelected(false);
        if (reportControlPanel != null) updateReportConfig();
    }

    private void refillIncludedAccounts() {
        for (int i = availableAccountsListModel.size() - 1; 0 <= i; i--) {
            moveFromAvailableToIncluded(i);
        }
        removeHideOnHomepageAccountsBox.setSelected(false);
        if (reportControlPanel != null) updateReportConfig();
    }

    private class removeInactiveAccountsListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == removeInactiveAccountsBox) {
                if (removeInactiveAccountsBox.isSelected()) {
                    removeInactiveAccounts();
                } else {
                    refillInactiveAccounts();
                }
            }
        }
    }

    private class removeHideOnHomePageAccountListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == removeHideOnHomepageAccountsBox) {
                if (removeHideOnHomepageAccountsBox.isSelected()) {
                    removeHideOnHomePageAccounts();
                } else {
                    refillIncludedAccounts();
                }
            }
        }
    }

    class AccountCellRenderer extends JLabel implements ListCellRenderer<Account> {
        private static final long serialVersionUID = 7586072864239449518L;
        private final Color HIGHLIGHT_COLOR = new Color(0, 0, 128);

        public AccountCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList<? extends Account> list, Account value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            String displayText = value.getAccountName().trim() + " (id: " + value.getAccountNum() + ")";
            setText(displayText);
            if (isSelected) {
                setBackground(HIGHLIGHT_COLOR);
                setForeground(Color.white);
            } else {
                setBackground(Color.white);
                setForeground(Color.black);
            }
            return this;
        }
    }


}









