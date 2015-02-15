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

import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.RootAccount;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

/**
 * Field chooser panel to identify which expenses are investment expenses.
 */
public class ReportConfigInvestExpenseChooserPanel extends JPanel {

    private static final long serialVersionUID = -8990699863699414946L;
    private RootAccount root;
    private final ReportControlPanel reportControlPanel;

    // JLists
    private final DefaultListModel<Account> possibleExpensesListModel = new DefaultListModel<>();
    private final JList<Account> possibleExpensesList = new JList<>(possibleExpensesListModel);
    private final JScrollPane possibleExpensePanel = new JScrollPane(possibleExpensesList);

    private final DefaultListModel<Account> investmentExpensesListModel = new DefaultListModel<>();
    private final JList<Account> investmentExpensesList = new JList<>(investmentExpensesListModel);
    private final JScrollPane investmentExpensesPane = new JScrollPane(investmentExpensesList);


    public ReportConfigInvestExpenseChooserPanel(ReportControlPanel reportControlPanel) {
        this.reportControlPanel = reportControlPanel;

        // Buttons
        JButton removeButton = new JButton("<<-Remove Expense");
        JButton addButton = new JButton("Add Expense->>");
        JButton resetButton = new JButton("Reset");

        // subPanels
        JPanel possibleExpensesPanel = new JPanel();
        JPanel expenseControlPanel = new JPanel();
        JPanel investmentExpensesPanel = new JPanel();

        String[] titles = {"Expenses", "Actions", "Investment Expenses"};
        JPanel[] panels = {possibleExpensesPanel, expenseControlPanel, investmentExpensesPanel};
        for (int i = 0; i < panels.length; i++) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            panels[i].setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        }

        possibleExpensesPanel.add(possibleExpensePanel);
        investmentExpensesPanel.add(investmentExpensesPane);

        //button panel
        expenseControlPanel.setLayout(new GridLayout(3, 1));
        expenseControlPanel.add(removeButton);
        expenseControlPanel.add(addButton);
        expenseControlPanel.add(resetButton);

        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 0;
        this.add(possibleExpensesPanel, c);
        c.gridx++;
        c.anchor = GridBagConstraints.CENTER;
        this.add(expenseControlPanel, c);
        c.gridx++;
        c.anchor = GridBagConstraints.NORTH;
        this.add(investmentExpensesPanel, c);

        // selection model
        investmentExpensesList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        // listeners
        removeButton.addActionListener(new removeExpenseListener());
        addButton.addActionListener(new addExpenseListener());
        resetButton.addActionListener(new resetExpensesListener());

        // renders
        possibleExpensesList.setCellRenderer(new AccountCellRenderer());
        investmentExpensesList.setCellRenderer(new AccountCellRenderer());
    }

    public void populateBothExpenseLists(ReportConfig reportConfig) throws Exception {
        possibleExpensesListModel.removeAllElements();
        investmentExpensesListModel.removeAllElements();

        populatePossibleExpensesList();
        populateInvestmentExpensesList(reportConfig);

        Dimension dimension = reportControlPanel.getRelatedDimension(possibleExpensePanel);
        possibleExpensePanel.setPreferredSize(dimension);
        investmentExpensesPane.setPreferredSize(dimension);
    }

    private void populatePossibleExpensesList() throws Exception {
        if (root == null) {
            root = reportControlPanel.getRoot();
        }
        if (root != null) {
            TreeSet<Account> expenseAccounts = BulkSecInfo.getSelectedSubAccounts(root, Account.ACCOUNT_TYPE_EXPENSE);
            ArrayList<Account> sortedExpenseAccounts = new ArrayList<>();
            sortedExpenseAccounts.addAll(expenseAccounts);
            Collections.sort(sortedExpenseAccounts, new compareExpenseNames());

            for (Account acct : sortedExpenseAccounts) {
                possibleExpensesListModel.addElement(acct);
            }
        } else {
            throw new Exception("Cannot obtain expense list");
        }
    }

    private class compareExpenseNames implements Comparator<Account> {
        @Override
        public int compare(Account a1, Account a2) {
            return fullExpenseName(a1).compareTo(fullExpenseName(a2));
        }
    }

    private String fullExpenseName(Account acct) {
        Account parent = acct.getParentAccount();
        String parentName = parent.getDepth() == 0 ? "" : parent.getAccountName().trim() + ":";
        return parentName + acct.getAccountName().trim();
    }

    private void populateInvestmentExpensesList(ReportConfig reportConfig) {
        HashSet<Integer> investmentExpenses = reportConfig.getInvestmentExpenseNums(); // Account numbers

        for (int expenseAccountNumber : investmentExpenses) {
            int index = findPossibleExpenseByAccountNum(expenseAccountNumber);
            if (index != -1) {
                moveFromPossibleToInvestmentExpense(index);
            }
        }
    }

    private void moveFromPossibleToInvestmentExpense(int pos) {
        Account acct = possibleExpensesListModel.remove(pos);
        insertInAlphabeticalOrder(investmentExpensesListModel, acct);
    }

    private void moveFromInvestmentExpenseToPossible(int pos) {
        Account acct = investmentExpensesListModel.remove(pos);
        insertInAlphabeticalOrder(possibleExpensesListModel, acct);
    }

    private void insertInAlphabeticalOrder(DefaultListModel<Account> model, Account acct) {
        for (int i = 0; i < model.size(); i++) {
            if (fullExpenseName(acct).compareTo(fullExpenseName(model.get(i))) < 0) {
                model.insertElementAt(acct, i);
                return;
            }
        }
        model.addElement(acct); // At end
    }

    private int findPossibleExpenseByAccountNum(int accountNum) {
        for (int i = 0; i < possibleExpensesListModel.size(); i++) {
            Account acct = possibleExpensesListModel.get(i);
            if (acct.getAccountNum() == accountNum) {
                return i;
            }
        }
        return -1;
    }

    private void updateReportConfig() {
        HashSet<Integer> investmentExpenseNums = new HashSet<>();

        for (int i = 0; i < investmentExpensesListModel.size(); i++) {
            Account acct = investmentExpensesListModel.get(i);
            investmentExpenseNums.add(acct.getAccountNum());
        }

        reportControlPanel.getReportConfig().setInvestmentExpenseNums(investmentExpenseNums);
    }


    private class removeExpenseListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int[] indices = investmentExpensesList.getSelectedIndices();

            for (int i = indices.length - 1; 0 <= i; i--) {
                moveFromInvestmentExpenseToPossible(indices[i]);
            }

            if (reportControlPanel != null) {
                updateReportConfig();
            }
        }
    }

    private class addExpenseListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int[] indices = possibleExpensesList.getSelectedIndices();

            for (int i = indices.length - 1; 0 <= i; i--) {
                moveFromPossibleToInvestmentExpense(indices[i]);
            }

            if (reportControlPanel != null) {
                updateReportConfig();
            }
        }
    }

    private class resetExpensesListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            for (int i = investmentExpensesListModel.size() - 1; 0 <= i; i--) {
                moveFromInvestmentExpenseToPossible(i);
            }
            if (reportControlPanel != null) updateReportConfig();
        }
    }


    private class AccountCellRenderer extends JLabel implements ListCellRenderer<Account> {
        private static final long serialVersionUID = 7586072864239449518L;
        private final Color HIGHLIGHT_COLOR = new Color(0, 0, 128);

        public AccountCellRenderer() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList<? extends Account> list, Account acct,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            setText(fullExpenseName(acct));
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









