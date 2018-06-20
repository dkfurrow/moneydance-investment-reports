/*
 * InvestmentAccountWrapper.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

/**
 * Wrapper for Moneydance Class Investment Account, adds increased functionality
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */

public class InvestmentAccountWrapper implements Aggregator {
    // name of aggregation method
    static String reportingName = "Investment Account";
    // column name for sorting
    static String columnName = "InvAcct";
    //associated BulkSecInfo
    BulkSecInfo currentInfo;
    // associated Investment Account
    private Account investmentAccount;
    // Account Number
    private int acctNum;
    // associated CashAccount
    private SecurityAccountWrapper cashWrapper;
    // Security Account Wrappers
    private ArrayList<SecurityAccountWrapper> securityAccountWrappers;
    private String name;

    public InvestmentAccountWrapper(Account invAcct, BulkSecInfo currentInfo,
                                    ReportConfig reportConfig) throws Exception {
        this.currentInfo = currentInfo;
        this.investmentAccount = invAcct;
        this.acctNum = this.investmentAccount.getAccountNum();
        this.securityAccountWrappers = new ArrayList<>();
        this.name = investmentAccount.getAccountName().trim();
        //get Security Sub Accounts
        TreeSet<Account> subSecAccts = BulkSecInfo.getSelectedSubAccounts(invAcct,
                Account.AccountType.SECURITY);
        //Loop through Security Sub Accounts
        for (Account subSecAcct : subSecAccts) {
            //Load Security Account into Wrapper Class
            SecurityAccountWrapper secAcctWrapper = new SecurityAccountWrapper(subSecAcct, this, reportConfig);
            // add Security Account to Investment Account
            this.securityAccountWrappers.add(secAcctWrapper);
        }
        createCashWrapper(reportConfig);  //creates basic cash wrapper
        this.securityAccountWrappers.add(cashWrapper);   //add cash wrapper to total securityAccountWrappers
        createCashTransactions(); //populates cash wrapper with synthetic cash transactions
    }

    public InvestmentAccountWrapper() {
    }

    public InvestmentAccountWrapper(String name) {
        this.name = name;
    }


    /**
     * Populate Synthetic Cash Transactions for a given Investment Account
     *
     * @throws Exception
     */
    public void createCashTransactions() throws Exception {
        ArrayList<TransactionValues> tempTransValues = new ArrayList<>();
        // add to tempTransValues all Security and Account-Level Cash
        // transactions for this InvestmentAccountWrapper
        tempTransValues.addAll(this.getTransactionValues());
        ArrayList<TransactionValues> cashTransactions = new ArrayList<>();

        // add initial balance as a transValues object (use day before first
        // transaction date if available, creation date if not
        int firstDateInt = tempTransValues.isEmpty()
                ? DateUtils.getPrevBusinessDay(this.investmentAccount.getCreationDateInt())
                : DateUtils.getPrevBusinessDay(tempTransValues.get(0).getDateInt());
        TransactionValues initialTransactionValues = new TransactionValues(this, firstDateInt);
        cashTransactions.add(initialTransactionValues);

        // now there is guaranteed to be one transaction, so prevTransValues always exists
        for (TransactionValues transactionValues : tempTransValues) {
            TransactionValues prevCashTransValues = cashTransactions.get(cashTransactions.size() - 1);
            // add synthetic cash transaction to overall cashTransactions set
            TransactionValues newTransactionValues = new TransactionValues(transactionValues, prevCashTransValues, this);
            cashTransactions.add(newTransactionValues);
            //Collections.sort(cashTransactions, TransactionValues.transComp);
        }
        this.cashWrapper.setAllTransactionValues(cashTransactions);
    }

    /**
     * creates CashWrapper as a money market mutual fund
     *
     * @throws Exception
     */
    private void createCashWrapper(ReportConfig reportConfig) throws Exception {
        Account cashAccount = new Account(null);
        cashAccount.setAccountName("CASH");
        cashAccount.setComment("New Security to hold cash transactions");
        cashAccount.setSecurityType(SecurityType.MUTUAL);
        cashAccount.setSecuritySubType("Money Market");
        cashAccount.setCurrencyType(currentInfo.getCashCurrencyWrapper().getCurrencyType());
        cashAccount.setParentAccount(this.investmentAccount);
        this.cashWrapper = new SecurityAccountWrapper(cashAccount, this, reportConfig);
        currentInfo.getCashCurrencyWrapper().secAccts.add(this.cashWrapper);
        cashWrapper.generateTransValues();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + acctNum;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        InvestmentAccountWrapper other = (InvestmentAccountWrapper) obj;
        return acctNum == other.acctNum;
    }

    public BulkSecInfo getBulkSecInfo() {
        return this.currentInfo;
    }

    public AccountBook getAccountBook(){
        return this.currentInfo.getAccountBook();
    }


    public ArrayList<SecurityAccountWrapper> getSecurityAccountWrappers() {
        return this.securityAccountWrappers;
    }


    public SecurityAccountWrapper getCashAccountWrapper() {
        return this.cashWrapper;
    }


    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns sorted transaction value lines for this investment account
     *
     * @return sorted transaction values list
     * @throws Exception
     */
    public ArrayList<TransactionValues> getTransactionValues() throws Exception {
        ArrayList<TransactionValues> outputTransactionValues = new ArrayList<>();
        for (SecurityAccountWrapper securityAccountWrapper : securityAccountWrappers) {
            ArrayList<TransactionValues> accountTransactionValues = securityAccountWrapper.getTransactionValues();
            if (accountTransactionValues != null) {
                for (TransactionValues transactionValues : accountTransactionValues) {
                    boolean success = outputTransactionValues.add(transactionValues);
                    if (!success)
                        throw new Exception("Error: Failed on "
                                + this.investmentAccount.getAccountName()
                                + "getTransValues");
                }
            }
        }
        Collections.sort(outputTransactionValues, TransactionValues.transComp);
        return outputTransactionValues;
    }

    public ArrayList<String[]> listTransValuesInfo() {
        ArrayList<String[]> outputList = new ArrayList<>();
        for (SecurityAccountWrapper securityAccountWrapper : securityAccountWrappers) {
            outputList.addAll(securityAccountWrapper.listTransValuesInfo());
        }
        return outputList;
    }

    public CurrencyType getBaseCurrency(){
        return currentInfo.getRoot().getCurrencyType();
    }

    public CurrencyType getAccountCurrency(){
        return this.investmentAccount.getCurrencyType();
    }

    public double getAccountCurrencyUserRateByDateInt(int dateInt){
        return  this.getAccountCurrency().getUserRateByDateInt(dateInt);
    }


    @Override
    public String getAggregateName() {
        return this.getName() + " ";
    }

    @Override
    public String getAllTypesName() {
        return "Accounts-ALL";
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public String getReportingName() {
        return reportingName;
    }

    public Account getInvestmentAccount() {
        return this.investmentAccount;
    }

}
