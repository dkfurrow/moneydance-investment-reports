/*
 *  SecurityAccountWrapper.java version 1.0 Feb 10, 2012
 *  Copyright (C) 2010 Dale Furrow
 *  dkfurrow@google.com
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, a copy may be found at
 *  http://www.gnu.org/licenses/lgpl.html
 */
package com.moneydance.modules.features.invextension;

import java.util.HashSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.SecurityAccount;

/**
 * Place description here
 * 
 * Version 1.0
 * 
 * @author Dale Furrow
 * 
 */
public class SecurityAccountWrapper extends SecurityAccount implements
	AccountWrapper, Comparable<SecurityAccountWrapper> {
    private SecurityAccount securityAccount;
    private int acctNum;
    private Account parentAccount;
    private CurrencyWrapper currWrapper;
    private Tradeable tradeable;
    private SecurityTypeWrapper securityTypeWrapper;
    private SecuritySubTypeWrapper securitySubTypeWrapper;
    private InvestmentAccountWrapper invAcctWrapper;
    private SortedSet<TransactionValues> transValuesSet;

    public SecurityAccountWrapper(SecurityAccount secAcct,
	    InvestmentAccountWrapper invAcct) throws Exception {
	super(secAcct.getAccountName(), secAcct.getAccountNum(), secAcct
		.getCurrencyType(), secAcct.cloneParameters(),
		getSubAccount(secAcct), secAcct.getParentAccount());
	this.securityAccount = secAcct;
	this.acctNum = this.securityAccount.getAccountNum();
	this.parentAccount = secAcct.getParentAccount();
	this.invAcctWrapper = invAcct;
	this.transValuesSet = new TreeSet<TransactionValues>();
	this.currWrapper = BulkSecInfo.getCurrencyWrappers().get(secAcct.getCurrencyType()
		.getID());
	this.tradeable = new Tradeable(this.currWrapper);
	this.securityTypeWrapper = new SecurityTypeWrapper(this);
	this.securitySubTypeWrapper = new SecuritySubTypeWrapper(this);
    }

    public void setSecurityAccount(SecurityAccount securityAccount) {
        this.securityAccount = securityAccount;
    }

    public void setCurrWrapper(CurrencyWrapper currWrapper) {
        this.currWrapper = currWrapper;
    }

    public void setTransValuesSet(SortedSet<TransactionValues> transValuesSet) {
        this.transValuesSet = transValuesSet;
    }

    private static Vector<Account> getSubAccount(SecurityAccount secAcct2) {
	Vector<Account> accts = new Vector<Account>();
	while (secAcct2.getSubAccounts().hasMoreElements()) {
	    accts.add((Account) secAcct2.getSubAccounts().nextElement());
	}
	return accts;

    }

    @Override
    public void setCurrencyWrapper(CurrencyWrapper currWrapper) {
	this.currWrapper = currWrapper;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.moneydance.modules.features.invextension.AccountWrapper#getAccountRef
     * ()
     */
    @Override
    public Account getParentAccountReference() {
	return this.parentAccount;
    }

    @Override
    public SortedSet<TransactionValues> getTransactionValues() {
	return this.transValuesSet;
    }

    @Override
    public AccountWrapper getCashAccountWrapper() {
	return this.invAcctWrapper.getCashAccountWrapper();
    }

    @Override
    public SecurityAccountWrapper getSecurityAccountWrapper() {
	return this;
    }

    @Override
    public int compareTo(SecurityAccountWrapper o) {
	return BulkSecInfo.acctComp.compare(this.securityAccount, o.securityAccount);
    }
    
    @Override
    public void setAllTransactionValues(SortedSet<TransactionValues> transValuesSet) {
	if (transValuesSet != null) {
	    this.transValuesSet = transValuesSet;
	}

    }

    @Override
    public void addTransactionValuesSet(SortedSet<TransactionValues> thisTransValuesSet)
	    throws Exception {
	if (thisTransValuesSet != null && !thisTransValuesSet.isEmpty()) {
	    boolean success = this.transValuesSet.addAll(thisTransValuesSet);
	    if (!success) {
		System.out.println("Name: " + invAcctWrapper.getInvestmentAccount().getAccountName());
		throw new Exception("addTransValuesSet failed for Account: "
			+ this.securityAccount.getAccountName());
	    }
	}

    }

    @Override
    public String getName() {
	return this.getAccountName();
    }
    
    public String getFullName() {
	return this.getAccountName() + ": "
		+ this.securityAccount.getParentAccount().getAccountName();
    }

    @Override
    public HashSet<SecurityAccountWrapper> getSecurityAccountWrappers() {
	HashSet<SecurityAccountWrapper> outSet = new HashSet<SecurityAccountWrapper>();
	outSet.add(this);
	return outSet;
    }

    @Override
    public CurrencyWrapper getCurrencyWrapper()  {
	return this.currWrapper;
    }
    
    @Override
    public SecurityTypeWrapper getSecurityTypeWrapper() {
	return this.securityTypeWrapper;
    }

    @Override
    public SecuritySubTypeWrapper getSecuritySubTypeWrapper() {
	return this.securitySubTypeWrapper;
    }

    @Override
    public Tradeable getTradeable() {
	return this.tradeable;
    }

    public InvestmentAccountWrapper getInvAcctWrapper() {
        return invAcctWrapper;
    }

    public void setInvAcctWrapper(InvestmentAccountWrapper invAcctWrapper) {
        this.invAcctWrapper = invAcctWrapper;
    }

    public SecurityAccount getSecAcct() {
        return securityAccount;
    }

    public int getAcctNum() {
        return acctNum;
    }

    

    public CurrencyWrapper getCurrWrapper() {
        return currWrapper;
    }

    public SortedSet<TransactionValues> getTransValuesSet() {
        return transValuesSet;
    }

}
