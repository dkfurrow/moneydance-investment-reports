/* AccountWrapper.java
 * Copyright 2012 ${author} . All rights reserved.
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
import java.util.SortedSet;

import com.moneydance.apps.md.model.Account;

/**
 * Interface to return parent of  SecurityAccountWrapper
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public interface AccountWrapper {

    public HashSet<SecurityAccountWrapper> getSecurityAccountWrappers();

    public AccountWrapper getCashAccount();

    public CurrencyWrapper getCurrencyWrapper() throws Exception;
    
    public SecurityTypeWrapper getSecurityTypeWrapper() throws Exception;
    
    public SecuritySubTypeWrapper getSecuritySubTypeWrapper() throws Exception;
    
    public Tradeable getTradeable() throws Exception;

    public String getName();

    /**
     * @return parent for accountwrapper
     */
    public Account getAccountReference();
    
    public SecurityAccountWrapper getSecurityAccountWrapper() throws Exception;

    public SortedSet<TransactionValues> getTransactionValues() throws Exception;

    public void setAllTransactionValues(SortedSet<TransactionValues> transactionValuesSet) throws Exception;
    
    public void setCurrencyWrapper(CurrencyWrapper currencyWrapper) throws Exception;
    
    void addTransactionValuesSet(SortedSet<TransactionValues> thisTransactionValuesSet)
	    throws Exception;

}
