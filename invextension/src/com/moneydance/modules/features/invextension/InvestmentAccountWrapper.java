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
 * Wrapper for Investment Account with associated properties
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class InvestmentAccountWrapper extends AggregatingType implements AccountWrapper   {
    //associated Investment Account
    public InvestmentAccount invAcct;
    // Account Number
    public int acctNum;
    //associated Parent Account
    public Account parentAccount;
    //associated CashAccount
    public SecurityAccountWrapper cashAcct;
    //Security Account Wrappers
    public HashSet<SecurityAccountWrapper> secAccts;
    // default name
    static String defaultName = "~All-Accts";
    // default column to sort on
    static Integer defaultColumn = 0;
    // name of aggregation method
    static String outputName = "Investment Account";

    
    public InvestmentAccountWrapper(InvestmentAccount invAcct,
	    BulkSecInfo currentInfo) throws Exception {
	this.invAcct = invAcct;
	this.acctNum = this.invAcct.getAccountNum();
	this.parentAccount = this.getParentAccountRef();
	this.secAccts = new HashSet<SecurityAccountWrapper>();
	this.cashAcct = getCashWrapper(currentInfo, this);
	this.secAccts.add(cashAcct);
    }

    /* (non-Javadoc)
     * @see com.moneydance.modules.features.invextension.AccountWrapper#getAccountRef()
     */
    @Override
    public Account getParentAccountRef() {
	// TODO Auto-generated method stub
	return invAcct.getRootAccount();
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

    /* Returns all TransValues for AccountWrapper
     * @see com.moneydance.modules.features.invextension.AccountWrapper#getTransValues()
     */
    @Override
    public SortedSet<TransValues> getTransValues() throws Exception {
	SortedSet<TransValues> theseTransValues = new TreeSet<TransValues>();
	for (Iterator<SecurityAccountWrapper> iterator = secAccts.iterator(); iterator
		.hasNext();) {
	    AccountWrapper acctWrapper = (AccountWrapper) iterator.next();

	    SortedSet<TransValues> accountTransValues = acctWrapper
		    .getTransValues();
	    if (accountTransValues != null) {
		for (Iterator<TransValues> iterator2 = accountTransValues
			.iterator(); iterator2.hasNext();) {
		    TransValues transValues = (TransValues) iterator2.next();
		    boolean success = theseTransValues.add(transValues);
		    if (!success)
			throw new Exception("Error: Failed on "
				+ this.invAcct.getAccountName()
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
    public static SecurityAccountWrapper getCashWrapper(
	    BulkSecInfo currentInfo, InvestmentAccountWrapper invAcctWrapper) throws Exception {
	SecurityAccount cashAccount = new SecurityAccount("~Cash",
		BulkSecInfo.nextAcctNum, BulkSecInfo.cashCurType, null, null,
		invAcctWrapper.invAcct);
	cashAccount.setComment("New Security to hold cash transactions");
	cashAccount.setSecurityType(SecurityType.MUTUAL);
	cashAccount.setSecuritySubType("Money Market");
	SecurityAccountWrapper cashAcctWrapper = new SecurityAccountWrapper(
		cashAccount, invAcctWrapper);
	cashAcctWrapper.secAcct = cashAccount;
	cashAcctWrapper.currWrapper = BulkSecInfo.curs
		.get(BulkSecInfo.cashCurType.getID());
	cashAcctWrapper.invAcctWrapper = invAcctWrapper;
	cashAcctWrapper.transValuesSet = new TreeSet<TransValues>();
	BulkSecInfo.nextAcctNum++;
	CurrencyWrapper cashCurrWrapper = BulkSecInfo.curs
		.get(BulkSecInfo.cashCurType.getID());
	cashCurrWrapper.secAccts.add(cashAcctWrapper);
	return cashAcctWrapper;
    }

    /**Populate Synthetic Cash Transactions for a given Investment Account
     * @param currentInfo
     * @throws Exception
     */
    public void createCashTransactions(BulkSecInfo currentInfo, GainsCalc gainsCalc)
	    throws Exception {
	SortedSet<TransValues> tempTransValues = new TreeSet<TransValues>();
	//add to tempTransValues all Security and Account-Level Cash transactions
	//for this InvestmentAccountWrapper
	tempTransValues.addAll(this.getTransValues());
	tempTransValues.addAll(currentInfo.getTransValuesForSingleAcct(this.invAcct));
	SortedSet<TransValues> cashTransactions = new TreeSet<TransValues>();
	//add initial balance as a transValues object (use day before first
	// transaction date if available, creation date if not
	int firstDateInt = tempTransValues.isEmpty() ? DateUtils
		.getPrevBusinessDay(this.invAcct.getCreationDateInt())
		: DateUtils.getPrevBusinessDay(tempTransValues.first().dateint);
	cashTransactions.add(new TransValues(this, firstDateInt));
	//now there is guaranteed to be one transaction, so prevTransValues
	//always exists
	for (Iterator<TransValues> iterator = tempTransValues.iterator(); iterator
		.hasNext();) {
	    TransValues transValues = (TransValues) iterator.next();
	    TransValues prevTransValues = cashTransactions.last();
	    //add synthetic cash transaction to overall cashTransactions set
	    cashTransactions.add(new TransValues(transValues, prevTransValues,
		    this));
	}
	this.cashAcct.addTransValuesSet(cashTransactions);
    }

    @Override
    public AccountWrapper getCashAccount() {
	return this.cashAcct;
    }

    @Override
    public SecurityAccountWrapper getSecurityAccountWrapper() throws Exception {
	throw new Exception("Illegal call to getSecurityAccount");
	
    }

    @Override
    public void setCurrWrapper(CurrencyWrapper currWrapper) throws Exception {
	throw new Exception("Illegal call to setCurrWrapper");
    }

    @Override
    public void setAllTransValues(SortedSet<TransValues> transValuesSet)
	    throws Exception {
	throw new Exception("Illegal call to setAllTransValues");

    }

    @Override
    public String getName() {
	return this.invAcct.getAccountName();
    }

    @Override
    public void addTransValuesSet(SortedSet<TransValues> thisTransValuesSet)
	    throws Exception {
	throw new Exception("Illegal call to addTransValuesSet");
    }

    @Override
    public HashSet<SecurityAccountWrapper> getSecurityAccountWrappers() {
	return this.secAccts;
    }

    @Override
    public CurrencyWrapper getCurrWrapper() throws Exception {
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
    String getFirstAggregateName() {
   	return this.invAcct.getAccountName();
       }

       @Override
    String getSecondAggregateName() {
	   return this.invAcct.getAccountName() + "~";
       }

       @Override
    String getAllAggregateName() {
	   return this.invAcct.getAccountName() + "*";
       }
       
       @Override
    String getDefaultName(){
	   return "~All-Accounts";
       }

}
