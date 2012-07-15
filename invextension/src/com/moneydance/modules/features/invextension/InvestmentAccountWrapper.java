/* InvestmentAccountWrapper.java
 * Copyright 2012 Dale Furrow . All rights reserved.
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

import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.InvestmentAccount;
import com.moneydance.apps.md.model.SecurityAccount;
import com.moneydance.apps.md.model.SecurityType;

/**
 * Wrapper for Moneydance Class Investment Account, adds increased 
 * functionality
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class InvestmentAccountWrapper extends Aggregator implements IAccount   {
    //associated Investment Account
    private InvestmentAccount investmentAccount;
    // Account Number
    private int acctNum;
    //associated Parent Account
    private Account parentAccount;
    //associated CashAccount
    private SecurityAccountWrapper cashWrapper;
    //Security Account Wrappers
    private HashSet<SecurityAccountWrapper> securityAccountWrappers;
    // default name
    static String defaultName = "~All-Accts";
    // default column to sort on
    static Integer defaultColumn = 0;
    // name of aggregation method
    static String outputName = "Investment Account";

    
    public InvestmentAccountWrapper(InvestmentAccount invAcct,
	    BulkSecInfo currentInfo) throws Exception {
	this.investmentAccount = invAcct;
	this.acctNum = this.investmentAccount.getAccountNum();
	this.parentAccount = this.getParentAccountReference();
	this.securityAccountWrappers = new HashSet<SecurityAccountWrapper>();
	this.cashWrapper = createCashWrapper(currentInfo, this);
	this.securityAccountWrappers.add(cashWrapper);
    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.IAccount#getAccountRef()
     */
    @Override
    public Account getParentAccountReference() {
	// TODO Auto-generated method stub
	return investmentAccount.getRootAccount();
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
	if (acctNum != other.acctNum)
	    return false;
	return true;
    }

    /* Returns all TransactionValues for IAccount
     * @see com.moneydance.modules.features.invextension.IAccount#getTransValues()
     */
    @Override
    public SortedSet<TransactionValues> getTransactionValues() throws Exception {
	SortedSet<TransactionValues> theseTransValues = new TreeSet<TransactionValues>();
	for (Iterator<SecurityAccountWrapper> iterator = securityAccountWrappers.iterator(); iterator
		.hasNext();) {
	    IAccount acctWrapper = (IAccount) iterator.next();

	    SortedSet<TransactionValues> accountTransValues = acctWrapper
		    .getTransactionValues();
	    if (accountTransValues != null) {
		for (Iterator<TransactionValues> iterator2 = accountTransValues
			.iterator(); iterator2.hasNext();) {
		    TransactionValues transactionValues =  iterator2.next();
		    boolean success = theseTransValues.add(transactionValues);
		    if (!success)
			throw new Exception("Error: Failed on "
				+ this.investmentAccount.getAccountName()
				+ "getTransValues");
		}

	    }
	}
	return theseTransValues;
    }
    
    /**returns next cashWrapper
     * @param currentInfo
     * @param invAcctWrapper
     * @return
     * @throws Exception 
     */
    public static SecurityAccountWrapper createCashWrapper(
	    BulkSecInfo currentInfo, InvestmentAccountWrapper invAcctWrapper) throws Exception {
	SecurityAccount cashAccount = new SecurityAccount("~Cash",
		BulkSecInfo.getNextAcctNumber(), BulkSecInfo.getCashCurrencyWrapper(), null, null,
		invAcctWrapper.investmentAccount);
	cashAccount.setComment("New Security to hold cash transactions");
	cashAccount.setSecurityType(SecurityType.MUTUAL);
	cashAccount.setSecuritySubType("Money Market");
	SecurityAccountWrapper cashAcctWrapper = new SecurityAccountWrapper(
		cashAccount, invAcctWrapper);
	cashAcctWrapper.setSecurityAccount(cashAccount);
	cashAcctWrapper.setCurrencyWrapper(BulkSecInfo.getCurrencyWrappers()
		.get(BulkSecInfo.getCashCurrencyWrapper().getID()));
	cashAcctWrapper.setInvAcctWrapper(invAcctWrapper);
	cashAcctWrapper.setTransValuesSet(new TreeSet<TransactionValues>());
	BulkSecInfo.setNextAcctNumber(BulkSecInfo.getNextAcctNumber() + 1);
	CurrencyWrapper cashCurrWrapper = BulkSecInfo.getCurrencyWrappers()
		.get(BulkSecInfo.getCashCurrencyWrapper().getID());
	cashCurrWrapper.secAccts.add(cashAcctWrapper);
	return cashAcctWrapper;
    }

    /**Populate Synthetic Cash Transactions for a given Investment Account
     * @param currentInfo
     * @throws Exception
     */
    public void createCashTransactions(BulkSecInfo currentInfo, GainsCalc gainsCalc)
	    throws Exception {
	SortedSet<TransactionValues> tempTransValues = new TreeSet<TransactionValues>();
	//add to tempTransValues all Security and Account-Level Cash transactions
	//for this InvestmentAccountWrapper
	tempTransValues.addAll(this.getTransactionValues());
	tempTransValues.addAll(currentInfo.getTransValuesForSingleAcct(this.investmentAccount));
	SortedSet<TransactionValues> cashTransactions = new TreeSet<TransactionValues>();
	//add initial balance as a transValues object (use day before first
	// transaction date if available, creation date if not
	int firstDateInt = tempTransValues.isEmpty() ? DateUtils
		.getPrevBusinessDay(this.investmentAccount.getCreationDateInt())
		: DateUtils.getPrevBusinessDay(tempTransValues.first().getDateint());
	cashTransactions.add(new TransactionValues(this, firstDateInt));
	//now there is guaranteed to be one transaction, so prevTransValues
	//always exists
	for (Iterator<TransactionValues> iterator = tempTransValues.iterator(); iterator
		.hasNext();) {
	    TransactionValues transactionValues = iterator.next();
	    TransactionValues prevTransValues = cashTransactions.last();
	    //add synthetic cash transaction to overall cashTransactions set
	    cashTransactions.add(new TransactionValues(transactionValues, prevTransValues,
		    this));
	}
	this.cashWrapper.addTransactionValuesSet(cashTransactions);
    }

    @Override
    public IAccount getCashAccountWrapper() {
	return this.cashWrapper;
    }

    @Override
    public SecurityAccountWrapper getSecurityAccountWrapper() throws Exception {
	throw new Exception("Illegal call to getSecurityAccount");
	
    }

    @Override
    public void setCurrencyWrapper(CurrencyWrapper currWrapper) throws Exception {
	throw new Exception("Illegal call to setCurrWrapper");
    }

    @Override
    public void setAllTransactionValues(SortedSet<TransactionValues> transValuesSet)
	    throws Exception {
	throw new Exception("Illegal call to setAllTransValues");

    }

    @Override
    public String getName() {
	return this.investmentAccount.getAccountName();
    }

    @Override
    public void addTransactionValuesSet(SortedSet<TransactionValues> thisTransValuesSet)
	    throws Exception {
	throw new Exception("Illegal call to addTransValuesSet");
    }

    @Override
    public HashSet<SecurityAccountWrapper> getSecurityAccountWrappers() {
	return this.securityAccountWrappers;
    }

    @Override
    public CurrencyWrapper getCurrencyWrapper() throws Exception {
	return null;
    }

    @Override
    public SecurityTypeWrapper getSecurityTypeWrapper() throws Exception {
	throw new Exception("Illegal call to getSecurityTypeWrapper");
    }

    @Override
    public SecuritySubTypeWrapper getSecuritySubTypeWrapper() throws Exception {
	throw new Exception("Illegal call to getSecuritySubTypeWrapper");
    }

    @Override
    public Tradeable getTradeable() throws Exception {
	throw new Exception("Illegal call to getTradeable");
    }
    
    @Override
    String getFirstAggregateOutput() {
   	return this.investmentAccount.getAccountName();
       }

       @Override
    String getSecondAggregateOutput() {
	   return this.investmentAccount.getAccountName() + "~";
       }

       @Override
    String getAllAggregateOutput() {
	   return this.investmentAccount.getAccountName() + "*";
       }
       
       @Override
    String getDefaultOutput(){
	   return "~All-Accounts";
       }

    public Account getInvestmentAccount() {
	return this.investmentAccount;
    }

}
