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
