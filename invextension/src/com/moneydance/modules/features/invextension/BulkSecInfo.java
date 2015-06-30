/*
 * BulkSecInfo.java
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

import java.util.*;

/**
 * Retrieves maps which show relationships among accounts, + between accounts
 * and transactions Generates basic transaction data and balance sheet data for
 * further analysis
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
public class BulkSecInfo {

    /**
     * Comparator to generate ordering of accounts based on type,
     * name and number
     */
    static Comparator<Account> acctComp = new Comparator<Account>() {
        @Override
        public int compare(Account a1, Account a2) {
            Integer t1 = a1.getAccountType().code();
            Integer t2 = a2.getAccountType().code();
            String name1 = a1.getAccountName();
            String name2 = a1.getAccountName();
            Integer num1 = a1.getAccountNum();
            Integer num2 = a2.getAccountNum();

            if (t1.compareTo(t2) != 0) {// different Account Types
                // Investment: 3000, Security 4000
                return t1.compareTo(t2);// sort by Account Type
            } else { // same account type
                if (name1.compareTo(name2) != 0) {// different names
                    return name1.compareTo(name2);
                } else {// same names and account types
                    return num1.compareTo(num2);
                }
            }
        }
    };
    /**
     * Comparator sorts transaction by date, account number, a custom
     * ordering based on transaction type, finally by transaction ID
     */
    static Comparator<ParentTxn> txnComp = new Comparator<ParentTxn>() {
        @Override
        public int compare(ParentTxn t1, ParentTxn t2) {

            Integer d1 = t1.getDateInt();
            Integer d2 = t2.getDateInt();
            String id1 = t1.getParameter("id");
            String id2 = t2.getParameter("id");
            Integer assocAcctNum1 = getAssociatedAccount(t1).getAccountNum();
            Integer assocAcctNum2 = getAssociatedAccount(t2).getAccountNum();
            Integer transTypeSort1 = getTxnSortOrder(t1);
            Integer transTypeSort2 = getTxnSortOrder(t2);

            if (d1.compareTo(d2) != 0) {// different dates
                return d1.compareTo(d2); // return date order
            } else { // same date
                // if Associated Accounts are different, sort Acct Nums
                if (assocAcctNum1.compareTo(assocAcctNum2) != 0) {
                    return assocAcctNum1.compareTo(assocAcctNum2);
                } else {
                    // if transaction types are different, sort on custom order
                    if (transTypeSort1.compareTo(transTypeSort2) != 0) {
                        return transTypeSort1.compareTo(transTypeSort2);
                    } else { // sort on transIDs
                        return id1.compareTo(id2);
                    } // end transIDs order
                }// end custom order
            } // end date order
        } // end compare method
    }; // end inner class

    /* conveys account data here for processing */
    public Main extension;
    /* Cash Currency Type for uninvested cash */
    private CurrencyWrapper cashCurrencyWrapper;
    /* first transaction date (to price cash currency)*/
    private int firstDateInt;
    /* HashSet of CurrencyWrappers */
    private HashMap<String, CurrencyWrapper> currencyWrappers;
    /* all transactions in root */
    private TransactionSet transactionSet;
    /*TreeMap of Transvalues for leaf-level security Accounts */
    private HashMap<String, TransactionValues> securityTransactionValues;
    /* root account */
    private Account root;
    /* Account Book */
    private AccountBook accountBook;
    /* GainsCalc Type */
    private GainsCalc gainsCalc;
    /* ReportConfig from panel or from test code */
    private static ReportConfig reportConfig;
    /* HashSet of InvestmentAccount Wrappers */
    private HashSet<InvestmentAccountWrapper> investmentWrappers;

    public BulkSecInfo(AccountBook accountBook, ReportConfig reportConfig) throws Exception {
        this.accountBook = accountBook;
        this.root = accountBook.getRootAccount();
        BulkSecInfo.reportConfig = reportConfig;
        this.gainsCalc = reportConfig.useAverageCostBasis() ? new GainsAverageCalc() : new GainsLotMatchCalc();
        transactionSet = accountBook.getTransactionSet();
        securityTransactionValues = new HashMap<>();
        firstDateInt = getFirstDateInt(transactionSet);
        cashCurrencyWrapper = defineCashCurrency();
        currencyWrappers = getCurrencyWrappersFromRoot();
        investmentWrappers = getInvestmentAccountInfo(reportConfig);
    }

    public int getFirstDateInt(TransactionSet transactionSet){
        int dateInt = Integer.MAX_VALUE;
        for(AbstractTxn txn : transactionSet){
            dateInt = Math.min(txn.getDateInt(), dateInt);
        }
        return dateInt;
    }

    public static ReportConfig getReportConfig() { return BulkSecInfo.reportConfig;}

    /**
     * loads selected accounts into HashSet
     *
     * @param parentAcct parentTxn account for query (i.e. "retrieve all below")
     * @param acctTypes  integer designation of account types (varArg)
     * @return HashSet of Accounts
     */

    public static TreeSet<Account> getSelectedSubAccounts(Account parentAcct,
                                                          Account.AccountType... acctTypes) {
        int sz = parentAcct.getSubAccountCount();
        ArrayList<Account.AccountType> acctTypesList = new ArrayList<>();
        TreeSet<Account> SubAccts = new TreeSet<>(acctComp);
        if (acctTypes.length > 0) {
            for (Account.AccountType acctType : acctTypes) {
                acctTypesList.add(acctType);
            }
        }
        for (int i = 0; i < sz; i++) {
            Account acct = parentAcct.getSubAccount(i);
            if (acctTypesList.contains(acct.getAccountType())
                    || acctTypes.length == 0) {
                SubAccts.add(acct);
            } // recursively add accounts
            SubAccts.addAll(getSelectedSubAccounts(acct, acctTypes));
        }
        return SubAccts; // note: includes accounts with no transactions!
    }

    /**
     * returns integer for customer sort order based on transaction type
     * (ensures buys come before sells on day trades, for example)
     *
     * @param parentTxn Parent Transaction
     * @return Integer which represents sort order
     */
    public static Integer getTxnSortOrder(ParentTxn parentTxn) {
        InvestTxnType transType = TxnUtil.getInvestTxnType(parentTxn);
        Integer txnOrder = 0;
        switch (transType) {
            case BUY:
                txnOrder = 0;
                break;
            case BUY_XFER:
                txnOrder = 1;
                break;
            case DIVIDEND_REINVEST:
                txnOrder = 2;
                break;
            case SELL:
                txnOrder = 3;
                break;
            case SELL_XFER:
                txnOrder = 4;
                break;
            case SHORT:
                txnOrder = 5;
                break;
            case COVER:
                txnOrder = 6;
                break;
            case MISCINC:
                txnOrder = 7;
                break;
            case MISCEXP:
                txnOrder = 8;
                break;
            case DIVIDEND:
                txnOrder = 9;
                break;
            case DIVIDENDXFR:
                txnOrder = 10;
                break;
            case BANK:
                txnOrder = 11;
                break;
        }
        return txnOrder;
    }

    /**
     * loads currency data into ArrayList of String Arrays
     *
     * @return ArrayList of String Arrays
     */
    public static ArrayList<String[]> ListAllCurrenciesInfo(
            HashMap<String, CurrencyWrapper> theseCurs) {
        ArrayList<String[]> currInfo = new ArrayList<>();

        for (CurrencyWrapper curWrapper : theseCurs.values()) {
            List<CurrencySnapshot> snapshots = curWrapper.currencyType.getSnapshots();
            for (int i = 0; i < snapshots.size(); i++) {
                currInfo.add(loadCurrencySnapshotArray(curWrapper.currencyType, snapshots.get(i)));
            }
        }
        return currInfo;
    }

    /**
     * loads data from individual currency snapshot
     *
     * @param cur CurrencyType
     * @param snapshot   index of currency snapshot
     * @return String array of currency and price info
     */
    public static String[] loadCurrencySnapshotArray(CurrencyType cur, CurrencySnapshot snapshot) {
        ArrayList<String> currencyInfo = new ArrayList<>();
        currencyInfo.add(Integer.toString(cur.getID())); // TODO: Change output of currency snapshots
        currencyInfo.add(cur.getName());
        if (cur.getTickerSymbol().isEmpty()) {
            currencyInfo.add("NoTicker");
        } else {
            currencyInfo.add(cur.getTickerSymbol());
        }
        int todayDate = DateUtils.getLastCurrentDateInt();
        int dateInt = snapshot.getDateInt();
        double closeRate = snapshot.getUserRate();
        currencyInfo.add(DateUtils.convertToShort(dateInt));
        currencyInfo.add(Double.toString(1 / closeRate));
        currencyInfo.add(Double.toString(1 / cur.adjustRateForSplitsInt(dateInt,
                closeRate, todayDate)));
        return currencyInfo.toArray(new String[currencyInfo.size()]);
    }

    public static StringBuffer listCurrencySnapshotHeader() {
        StringBuffer currInfo = new StringBuffer();
        currInfo.append("id " + ",");
        currInfo.append("Name " + ",");
        currInfo.append("Ticker " + ",");
        currInfo.append("Date " + ",");
        currInfo.append("PricebyDate " + ",");
        currInfo.append("PriceByDate(Adjust)");
        return currInfo;
    }

    /**
     * Generates account associated with transaction (i.e. if a security is in
     * the transaction hierarchy, return that account, otherwise, return the
     * transaction account
     *
     * @param thisTxn transaction of interest
     * @return account associated with this transaction
     */
    public static Account getAssociatedAccount(AbstractTxn thisTxn) {
        Account associatedAccount = thisTxn.getAccount();
        ParentTxn parent;
        if (thisTxn instanceof ParentTxn) {
            parent = (ParentTxn) thisTxn;

        } else { // this is a split transaction
            SplitTxn split = (SplitTxn) thisTxn;
            parent = split.getParentTxn();
        }
        for (int i = 0; i < parent.getSplitCount(); i++) {
            if (parent.getSplit(i).getAccount().getAccountType()
                    == Account.AccountType.SECURITY)
                associatedAccount = parent.getSplit(i).getAccount();
        }
        return associatedAccount;
    }

    public TransactionSet getTransactionSet() {
        return transactionSet;
    }

    public HashMap<String, TransactionValues> getSecurityTransactionValues() {
        return securityTransactionValues;
    }

    public CurrencyWrapper getCashCurrencyWrapper() {
        return cashCurrencyWrapper;
    }

    public HashMap<String, CurrencyWrapper> getCurrencyWrappers() {
        return currencyWrappers;
    }

    public int getFirstDateInt() {
        return firstDateInt;
    }

    public Account getRoot() {
        return root;
    }

    public AccountBook getAccountBook() {return accountBook;}

    public GainsCalc getGainsCalc() {
        return gainsCalc;
    }

    public HashSet<InvestmentAccountWrapper> getInvestmentWrappers() {
        return investmentWrappers;
    }

    /**
     * lists all TransactionValues in InvestmentAccountWrappers
     *
     * @param investmentAccountWrappers Hash set of investment account wrappers
     * @return ArrayList of String Arrays with values
     * @throws Exception
     */
    public ArrayList<String[]> listAllTransValues(
            HashSet<InvestmentAccountWrapper> investmentAccountWrappers) throws Exception {
        ArrayList<String[]> transactionsInfo = new ArrayList<>();

        for (InvestmentAccountWrapper investmentAccountWrapper : investmentAccountWrappers) {
            transactionsInfo.addAll(investmentAccountWrapper.listTransValuesInfo());
        }
        return transactionsInfo;
    }

    /**
     * Defines a 'cash' currency whose value is always 1
     *
     * @return synthetic cash security, value 1
     */
    private CurrencyWrapper defineCashCurrency() {
        CurrencyTable currencyTable = accountBook.getCurrencies();
        int dateInt = DateUtils.convertToDateInt(new Date());
        CurrencyType cashCurrencyType = new CurrencyType(currencyTable);
        cashCurrencyType.setCurrencyType(CurrencyType.Type.SECURITY);
        cashCurrencyType.setName("CASH");
        cashCurrencyType.setTickerSymbol("CASH");
        cashCurrencyType.setRawRate(1.0, false);
        cashCurrencyType.setDecimalPlaces(4);
        // "asof_dt" is parameter to set date (if needed)
        cashCurrencyType.addSnapshotInt(firstDateInt, 1.0);
        CurrencyWrapper cashCurrencyWrapper = new CurrencyWrapper(cashCurrencyType, this);
        cashCurrencyWrapper.setCash();
        return cashCurrencyWrapper;
    }

    /**
     * creates map of currency ids and associated currency wrappers
     *
     * @return map of currency ids to associated currency wrappers
     */
    private HashMap<String, CurrencyWrapper> getCurrencyWrappersFromRoot() {
        List<CurrencyType> currencies = accountBook.getCurrencies().getAllCurrencies();
        HashMap<String, CurrencyWrapper> wrapperHashMap = new HashMap<>();
        for (CurrencyType currency : currencies) {
            if (currency.getCurrencyType() == CurrencyType.Type.SECURITY) {
                String thisID = currency.getParameter("id");
                wrapperHashMap.put(thisID, new CurrencyWrapper(currency, this));
            }
        }
        // make sure new Currency is added!
        wrapperHashMap.put(cashCurrencyWrapper.getCurID(),
                cashCurrencyWrapper);

        return wrapperHashMap;
    }

    /**
     * @param reportConfig Report Config from display
     * @return Completed InvestmentAccountWrappers with interpreted transaction
     * information
     * @throws Exception
     */
    private HashSet<InvestmentAccountWrapper> getInvestmentAccountInfo(ReportConfig reportConfig) throws Exception {
        TreeSet<Account> allSubAccounts = getSelectedSubAccounts(root, Account.AccountType.INVESTMENT);
        HashSet<Integer> excludedAccountNums = reportConfig.getExcludedAccountNums();
        TreeSet<Account> selectedSubAccounts = new TreeSet<>(acctComp);
        for (Account account : allSubAccounts){
            if(!excludedAccountNums.contains(account.getAccountNum())) {
                selectedSubAccounts.add(account);
            }
        }
        HashSet<InvestmentAccountWrapper> invAcctWrappers = new HashSet<>();
        for (Account selectedSubAccount : selectedSubAccounts) {
            //Load investment account into Wrapper Class
            InvestmentAccountWrapper invAcctWrapper =
                    new InvestmentAccountWrapper(selectedSubAccount, this, reportConfig);
            invAcctWrappers.add(invAcctWrapper);
        } // end Investment Accounts Loop
        return invAcctWrappers;
    }

}

