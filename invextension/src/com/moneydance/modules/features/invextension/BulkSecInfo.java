/* BulkSecInfo.java
 * Copyright 2011 Dale K. Furrow . All rights reserved.
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, 
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, 
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY <COPYRIGHT HOLDER> ''AS IS'' AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL <COPYRIGHT HOLDER> OR CONTRIBUTORS BE LIABLE FOR ANY 
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND 
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF 
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.moneydance.modules.features.invextension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.moneydance.apps.md.model.AbstractTxn;
import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.CurrencyTable;
import com.moneydance.apps.md.model.CurrencyType;
import com.moneydance.apps.md.model.InvestTxnType;
import com.moneydance.apps.md.model.InvestmentAccount;
import com.moneydance.apps.md.model.ParentTxn;
import com.moneydance.apps.md.model.RootAccount;
import com.moneydance.apps.md.model.SecurityAccount;
import com.moneydance.apps.md.model.SplitTxn;
import com.moneydance.apps.md.model.TransactionSet;
import com.moneydance.apps.md.model.TxnSet;
import com.moneydance.apps.md.model.TxnUtil;

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
    
    /* conveys account data here for processing */
    public Main extension;
    /* root account */
    private RootAccount root;
    /* GainsCalc Type */
    private GainsCalc gainsCalc;
    /* list of relevant accounts (Investment and Security) */
    private HashSet<Account> secAccts;
    /* Cash Currency Type for uninvested cash */
    private static CurrencyType cashCurrencyWrapper;
    /* static reference to nextAcctNumber (to number account implementations of
     * cash currency */
    private static int nextAcctNumber;
    /*static reference to nextTxnNumber (to uniquely identify initial balance
     *  cash transactions)*/
    private static long nextTxnNumber;
    /* first transaction date (to price cash currency)*/
    private static int firstDateInt;
    /* HashSet of CurrencyWrappers */
    private static HashMap<Integer, CurrencyWrapper> currencyWrappers;
    /* all transactions in root */
    private static TransactionSet transactionSet;
    /*TreeMap of Transvalues for leaf-level security Accounts */
    private TreeMap<Double, TransactionValues> securityTransactionValues;
    /* HashSet of InvestmentAccount Wrappers */
    private HashSet<InvestmentAccountWrapper> investmentWrappers;
    
    
    
    
    public BulkSecInfo(RootAccount root, GainsCalc gainsCalc) throws Exception {
	this.root = root;
	this.gainsCalc = gainsCalc;
	nextAcctNumber = this.root.getHighestAccountNum() + 1;
	transactionSet = this.root.getTransactionSet();
	securityTransactionValues = new TreeMap<Double, TransactionValues>();
	BulkSecInfo.firstDateInt = transactionSet.getDateBounds().getStartDateInt();
	BulkSecInfo.nextTxnNumber = transactionSet.getAllTxns().getLastTxn().getTxnId() + 1L;
	cashCurrencyWrapper = defineCashCurrency();
	currencyWrappers = getCurrencyWrappersFromRoot();
	investmentWrappers = getInvestmentAccountInfo(getSelectedSubAccounts(root,
		Account.ACCOUNT_TYPE_INVESTMENT));
    }
    
    public enum AGG_TYPE {
	SEC, // Individual Security
	ACCT_SEC, // Securities Aggregated at Account Level
	ACCT_SEC_PLUS_CASH, // Previous plus Account Cash
	ACCT_CASH, // Account Cash Only
	ALL_SEC, // Securities in All Accounts
	ALL_CASH, // All Cash in Accounts
	ALL_SEC_PLUS_CASH // All Cash, All Securities
    }
    
    /**Comparator lists by parent account, then account for report purposes*/
    public static final Comparator<String[]> PrntAcct_Order = 
	    new Comparator<String[]>() {
	@Override
	public int compare(String[] o1, String[] o2) {
	    int parentCmp = o1[0].compareTo(o2[0]);
	    return (parentCmp == 0 ? o1[1].compareTo(o2[1]) : parentCmp);
	}
    };

    /**Comparator to generate ordering of accounts based on type,
     * name and number
     */
    static Comparator<Account> acctComp = new Comparator<Account>() {
	@Override
	public int compare(Account a1, Account a2) {
	    Integer t1 = a1.getAccountType();
	    Integer t2 = a2.getAccountType();
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

    /**Comparator sorts transaction by date, account number, a custom
     * ordering based on transaction type, finally by transaction ID
     */
    static Comparator<ParentTxn> txnComp = new Comparator<ParentTxn>() {
	@Override
	public int compare(ParentTxn t1, ParentTxn t2) {

	    Integer d1 = t1.getDateInt();
	    Integer d2 = t2.getDateInt();
	    Long id1 = t1.getTxnId();
	    Long id2 = t2.getTxnId();
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

    public static int getNextAcctNumber() {
        return nextAcctNumber;
    }

    public static void setNextAcctNumber(int nextAcctNumber) {
        BulkSecInfo.nextAcctNumber = nextAcctNumber;
    }

    public static long getNextTxnNumber() {
        return nextTxnNumber;
    }

    public static void setNextTxnNumber(long nextTxnNumber) {
        BulkSecInfo.nextTxnNumber = nextTxnNumber;
    }

    public RootAccount getRoot() {
        return root;
    }

    public GainsCalc getGainsCalc() {
        return gainsCalc;
    }

    public HashSet<InvestmentAccountWrapper> getInvestmentWrappers() {
        return investmentWrappers;
    }   
    public static TransactionSet getTransactionSet() {
        return transactionSet;
    }

    public TreeMap<Double, TransactionValues> getSecurityTransactionValues() {
        return securityTransactionValues;
    }
    

    public static CurrencyType getCashCurrencyWrapper() {
        return cashCurrencyWrapper;
    }

    public HashSet<Account> getSecAccts() {
        return secAccts;
    }
    public static HashMap<Integer, CurrencyWrapper> getCurrencyWrappers() {
        return currencyWrappers;
    }
    
    public static int getFirstDateInt(){
	return firstDateInt;
    }
    

    /**
     * loads selected accounts into HashSet
     * @param parentAcct
     *            parentTxn account for query (i.e. "retrieve all below")
     * @param acctTypes
     *            integer designation of account types (varArg)
     * @return HashSet of Accounts
     */

    public static TreeSet<Account> getSelectedSubAccounts(Account parentAcct,
	    int... acctTypes) {
	int sz = parentAcct.getSubAccountCount();
	ArrayList<Integer> acctTypesList = new ArrayList<Integer>();
	TreeSet<Account> SubAccts = new TreeSet<Account>(acctComp);
	if (acctTypes.length > 0) {
	    for (int i = 0; i < acctTypes.length; i++) {
		acctTypesList.add(acctTypes[i]);
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

    /**returns integer for customer sort order based on transaction type
     * (ensures buys come before sells on day trades, for example)
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
     * @param allCurTypes HashSet of currencies
     * @return ArrayList of String Arrays
     */
    public static ArrayList<String[]> ListAllCurrenciesInfo(
	    HashMap<Integer, CurrencyWrapper> theseCurs) {
	ArrayList<String[]> currInfo = new ArrayList<String[]>();

	for (Iterator<CurrencyWrapper> it = theseCurs.values().iterator(); it
		.hasNext();) {
	    CurrencyWrapper curWrapper =  it.next();
	    for (int i = 0; i < curWrapper.curType.getSnapshotCount(); i++) {
		currInfo.add(loadCurrencySnapshotArray(curWrapper.curType, i));
	    }
	}
	return currInfo;
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
     * loads data from individual currency snapshot
     * @param cur CurrencyType
     * @param i index of currency snapshot
     * @return String array of currency and price info
     */
    public static String[] loadCurrencySnapshotArray(CurrencyType cur, int i) {
	ArrayList<String> currInfo = new ArrayList<String>();
	currInfo.add(Integer.toString(cur.getID()));
	currInfo.add(cur.getName());
	if (cur.getTickerSymbol().isEmpty()) {
	    currInfo.add("NoTicker");
	} else {
	    currInfo.add(cur.getTickerSymbol());
	}
	int todayDate = DateUtils.getLastCurrentDateInt();
	int dateint = cur.getSnapshot(i).getDateInt();
	double closeRate = cur.getSnapshot(i).getUserRate();
	currInfo.add(DateUtils.convertToShort(dateint));
	currInfo.add(Double.toString(1 / closeRate));
	currInfo.add(Double.toString(1 / cur.adjustRateForSplitsInt(dateint,
		closeRate, todayDate)));
	return currInfo.toArray(new String[currInfo.size()]);
    }

    /**
     * Generates account associated with transaction (i.e. if a security is in
     * the transaction hierarchy, return that account, otherwise, return the
     * transaction account
     * 
     * @param thisTxn
     * @return
     */
    private static Account getAssociatedAccount(AbstractTxn thisTxn) {
	Account assocAcct = thisTxn.getAccount();
	ParentTxn parent = null;
	if (thisTxn instanceof ParentTxn) {
	    parent = (ParentTxn) thisTxn;

	} else { // this is a split transaction
	    SplitTxn split = (SplitTxn) thisTxn;
	    parent = split.getParentTxn();
	}
	for (int i = 0; i < parent.getSplitCount(); i++) {
	    if (parent.getSplit(i).getAccount().getAccountType() 
		    == Account.ACCOUNT_TYPE_SECURITY)
		assocAcct = parent.getSplit(i).getAccount();
	}
	return assocAcct;
    }
    
   

    /**lists all TransactionValues in InvestmentAccountWrappers
     * @param invAcctWrappers
     * @return
     * @throws Exception
     */
    public ArrayList<String[]> listTransValuesSet(
	    HashSet<InvestmentAccountWrapper> invAcctWrappers) throws Exception {
	ArrayList<String[]> txnInfo = new ArrayList<String[]>();

	for (Iterator<InvestmentAccountWrapper> it = invAcctWrappers.iterator(); it
		.hasNext();) {
	    InvestmentAccountWrapper thisInvAccount =  it.next();
	    TreeSet<TransactionValues> accountLines = new TreeSet<TransactionValues>(
		    thisInvAccount.getTransactionValues());
	    for (Iterator<TransactionValues> it1 = accountLines.iterator(); it1
		    .hasNext();) {
		TransactionValues reportLine = it1.next();
		txnInfo.add(TransactionValues.loadArrayTransValues(reportLine));
	    }
	}
	Collections.sort(txnInfo, PrntAcct_Order);
	return txnInfo;
    }

    private CurrencyType defineCashCurrency() {
	CurrencyTable curTable = root.getCurrencyTable();
	int nextId = curTable.getNextID();
	int dateInt = DateUtils.convertToDateInt(new Date());
	CurrencyType cashCurrency = new CurrencyType
		(nextId, "", "^CASH", 1.0, 1, "", "", "^CASH",
		dateInt, CurrencyType.CURRTYPE_SECURITY, curTable);
	cashCurrency.addSnapshotInt(BulkSecInfo.firstDateInt, 1.0);
	return cashCurrency;
    }

    /** creates map of currency ids and associated currency wrappers
     * @return
     */
    private HashMap<Integer, CurrencyWrapper> getCurrencyWrappersFromRoot() {
	CurrencyType[] currencies = root.getCurrencyTable().getAllCurrencies();
	HashMap<Integer, CurrencyWrapper> wrapperHashMap = 
		new HashMap<Integer, CurrencyWrapper>();

	for (CurrencyType currency : currencies) {
	    if (currency.getCurrencyType() == CurrencyType.CURRTYPE_SECURITY) {
		Integer thisID = currency.getID();
		wrapperHashMap.put(thisID, new CurrencyWrapper(currency));
	    }
	}
	// make sure new Currency is added!
	wrapperHashMap.put(BulkSecInfo.cashCurrencyWrapper.getID(),
		new CurrencyWrapper(BulkSecInfo.cashCurrencyWrapper));

	return wrapperHashMap;
    }

    /**
     * @param selectedSubAccounts Investment Accounts in file
     * @return Completed InvestmentAccountWrappers with interpreted transaction
     * information
     * @throws Exception
     */
    private HashSet<InvestmentAccountWrapper> getInvestmentAccountInfo(
	    TreeSet<Account> selectedSubAccounts) throws Exception {
	HashSet<InvestmentAccountWrapper> invAcctWrappers = 
		new HashSet<InvestmentAccountWrapper>();
//	this.secAccountWrappers = new HashSet<SecurityAccountWrapper>();

	for (Iterator<Account> iterator = selectedSubAccounts.iterator(); iterator
		.hasNext();) {
	    InvestmentAccount invAcct = (InvestmentAccount) iterator.next();
	    //Load investment account into Wrapper Class
	    InvestmentAccountWrapper invAcctWrapper = new InvestmentAccountWrapper(
		    invAcct, this);
	    //get Security Sub Accounts
	    TreeSet<Account> subSecAccts = getSelectedSubAccounts(invAcct,
		    Account.ACCOUNT_TYPE_SECURITY);
	    //Loop through Security Sub Accounts
	    for (Iterator<Account> iterator2 = subSecAccts.iterator(); iterator2
		    .hasNext();) {
		SecurityAccount secAcct = (SecurityAccount) iterator2.next();
		//Load Security Account into Wrapper Class
		SecurityAccountWrapper secAcctWrapper = new SecurityAccountWrapper(
			secAcct, invAcctWrapper);
		
		CurrencyWrapper thisCurWrapper = currencyWrappers.get(secAcct
			.getCurrencyType().getID());
		// add account to list of accounts in currWrapper
		thisCurWrapper.secAccts
			.add(secAcctWrapper);
		// set CurrencyWrapper associated with this SecurityWrapper
		secAcctWrapper.setCurrencyWrapper(thisCurWrapper);
		// add Security Account to Investment Account
		invAcctWrapper.getSecurityAccountWrappers().add(secAcctWrapper);
		// add security transvalues to security account
		secAcctWrapper.addTransactionValuesSet(getTransValuesForSingleAcct(
			secAcct));
	    }//end Security Sub Accounts Loop
	    // add cash transactions to synthetic cash account under this 
	    //Investment Account
	    invAcctWrapper.createCashTransactions(this, gainsCalc);
	    // add invAcctWrapper to all accounts, AccountWrappers to 
	    //Security Account Wrappers	    
	    invAcctWrappers.add(invAcctWrapper);
	} // end Investment Accounts Loop
	return invAcctWrappers;
    }
    
    /**gets TransactionValues for either single security account or single investment
     * account (i.e. investment-account-level transvalues
     * @param thisAccount
     * @param gainsCalc
     * @return
     */
    public SortedSet<TransactionValues> getTransValuesForSingleAcct(Account thisAccount) {
	SortedSet<TransactionValues> transValuesSet = new TreeSet<TransactionValues>();
	TreeSet<ParentTxn> assocTrans = new TreeSet<ParentTxn>(txnComp);
	TxnSet txnSet = BulkSecInfo.transactionSet
		.getTransactionsForAccount(thisAccount);
	for (Iterator<AbstractTxn> iterator = txnSet.iterator(); iterator
		.hasNext();) {
	    AbstractTxn abstractTxn =  iterator.next();
	    if (getAssociatedAccount(abstractTxn) == thisAccount) {
		assocTrans.add(abstractTxn instanceof ParentTxn ? 
			(ParentTxn) abstractTxn	: abstractTxn.getParentTxn());
	    }

	}
	for (Iterator<ParentTxn> iterator = assocTrans.iterator(); iterator
		.hasNext();) {
	    ParentTxn parentTxn = iterator.next();
	    TransactionValues transValuesToAdd = new TransactionValues(parentTxn,
		    thisAccount, transValuesSet, this);
	    transValuesSet.add(transValuesToAdd);
	    if (thisAccount instanceof SecurityAccount)
		securityTransactionValues.put(transValuesToAdd.txnID,
			transValuesToAdd);

	}

	return transValuesSet;
    }
}

