/* GenericTestMethods.java
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.SortedSet;

import com.moneydance.apps.md.model.Account;

public class GenericTestMethods {

    public static void main(String[] args) throws Exception {
	BulkSecInfo currentInfo = BulkSecInfoTest.getBaseSecurityInfo();
	listInvSecmap(currentInfo);
	listCreateDatesANDInitBals(currentInfo);
    }

    private static void listCreateDatesANDInitBals(BulkSecInfo currentInfo) {
	HashMap<Account, Double> initBals = currentInfo.invInitBal;
	HashMap<Account, Integer> createDates = currentInfo.invCreateDate;
	System.out
		.println("\nAccounts with Creation Dates and Initial Balances Follow");
	for (Iterator iterator = createDates.keySet().iterator(); iterator.hasNext();) {
	    Account acct = (Account) iterator.next();
	    System.out.println(acct.getAccountName() + ": Create Date -- "
		    + DateUtils.convertToShort(createDates.get(acct))
		    + " Init Bal -- " + initBals.get(acct));
	    
	}
	
    }

    private static void listInvSecmap(BulkSecInfo currentInfo) {
	HashMap<Account, HashSet<Account>> invSec = currentInfo.invSec;
	HashMap<Account, SortedSet<TransValuesCum>> transValuesCumMap = currentInfo.transValuesCumMap;
	for (Iterator iterator = invSec.keySet().iterator(); iterator.hasNext();) {
	    Account parentAccount = (Account) iterator.next();
	    int creationDateInt = parentAccount.getCreationDateInt();
	    System.out.println("\n" + "[" + getAcctInfo(parentAccount) + 
		    "Creation Date: " +  creationDateInt +  "]"
		    + " Child Accounts:");
	    HashSet<Account> accts = invSec.get(parentAccount);
	    if (accts == null) {
		System.out.println("--No Child Accounts");
	    } else {
		for (Iterator iterator2 = accts.iterator(); iterator2.hasNext();) {
		    Account secAcct = (Account) iterator2.next();
		    String numTrans = transValuesCumMap.get(secAcct) == null ? " No Transactions"
			    : " " + transValuesCumMap.get(secAcct).size()
				    + " Transactions ";
		    System.out.println("--" + getAcctInfo(secAcct) + numTrans);

		}
	    }

	}

    }

    public static String getAcctInfo(Account acct) {
	
	return ("Account Name: " + acct.getAccountName() + " AcctNum: " + acct
		.getAccountNum() ) ;
    }

}
