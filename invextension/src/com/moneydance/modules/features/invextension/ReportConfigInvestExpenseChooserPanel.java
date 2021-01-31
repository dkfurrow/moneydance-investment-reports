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

import java.io.Serial;
import java.util.*;

/**
 * Field chooser panel to identify which expenses are investment expenses.
 */
public class ReportConfigInvestExpenseChooserPanel extends ReportConfigChooserPanelBase<Account> {

    @Serial
    private static final long serialVersionUID = 5799086825356016359L;

    public ReportConfigInvestExpenseChooserPanel(ReportControlPanel reportControlPanel) {
        super(reportControlPanel, "<<-Remove Expense", "Add Expense->>", "Reset", "Expenses", "Actions", "Investment Expenses");
    }

    public void populateBothExpenseLists(ReportConfig reportConfig) {
        populateBothLists(reportConfig);
    }

    @Override
    void populateLeftList(ReportConfig reportConfig) {
        Account root = MDData.getInstance().getRoot();
        if (root != null) {
            TreeSet<Account> expenseAccounts = BulkSecInfo.getSelectedSubAccounts(root, Account.AccountType.EXPENSE);
            ArrayList<Account> sortedExpenseAccounts = new ArrayList<>(expenseAccounts);
            sortedExpenseAccounts.sort(new compareAccountNames());

            sortedExpenseAccounts.forEach(leftListModel::addElement);
        }
    }

    private class compareAccountNames implements Comparator<Account> {
        @Override
        public int compare(Account a1, Account a2) {
            return fullName(a1).compareTo(fullName(a2));
        }
    }

    String fullName(Account acct) {
        Account parent = acct.getParentAccount();
        String parentName = parent.getDepth() == 0 ? "" : parent.getAccountName().trim() + ":";
        return parentName + acct.getAccountName().trim();
    }

    @Override
    void populateRightList(ReportConfig reportConfig) {
        HashSet<String> investmentExpenseIds = reportConfig.getInvestmentExpenseIds(); // Account numbers

        for (String expenseAccountId : investmentExpenseIds) {
            int index = findPossibleExpenseByAccountId(expenseAccountId);
            if (index != -1) {
                moveFromLeftToRight(index);
            }
        }
    }

    private int findPossibleExpenseByAccountId(String accountId) {
        for (int i = 0; i < leftListModel.size(); i++) {
            Account acct = leftListModel.get(i);
            if (acct.getUUID().equals(accountId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    void updateReportConfig() {
        HashSet<String> investmentExpenseIds = new HashSet<>();

        for (int i = 0; i < rightListModel.size(); i++) {
            Account acct = rightListModel.get(i);
            investmentExpenseIds.add(acct.getUUID());
        }

        reportControlPanel.getReportConfig().setInvestmentExpenseIds(investmentExpenseIds);
    }
}









