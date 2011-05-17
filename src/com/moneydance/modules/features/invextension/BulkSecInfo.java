/*
 *  BulkSecInfo.java
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
import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.TransactionSet;
import com.moneydance.apps.md.model.AbstractTxn;
import com.moneydance.apps.md.model.CurrencyType;
import com.moneydance.apps.md.model.ParentTxn;
import com.moneydance.apps.md.model.RootAccount;
import com.moneydance.apps.md.model.SplitTxn;


import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;


/**
 * Retrieves maps which show relationships among accounts, between accounts and transactions
 * Generates basic transaction data and balance sheet data for further analysis
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
public class BulkSecInfo {

    public Main extension;                                          /*conveys account data here for processing */
    public RootAccount root;                                        /*root account */
    public HashSet<Account> secAccts;                               /*list of relevant accounts (Investment and Security) */
    public HashSet<AbstractTxn> SecTns;                             /*list of security and investment transaction (Parent and Split)*/
    public HashMap<Account, HashSet<AbstractTxn>> assocSecTnsMap;   /*Map of account to parent transactions */
    public HashMap<Account, CurrencyType> secCur;                   /*Map of securities to currencies (one-to-one) */
    public HashMap<CurrencyType, HashSet<Account>> curSec;          /*Map of currencies to securities (one-to-many) */
    public HashSet<CurrencyType> allCurrTypes;                      /* Hash set of currencies */
    public HashMap<Account, HashSet<Account>> invSec;               /*Map of Investment Accounts to Security Accounts */
    public HashMap<Account, SortedSet<TransValues>> transValuesMap; /*Map of Accounts to basic transaction data */
    public HashMap<Account, SortedSet<TransValuesCum>> transValuesCumMap;/*Map of Accounts to cumulative transaction data */

   



    public BulkSecInfo(Main extension, RootAccount root) {
        this.extension = extension;
        this.root = root;
        this.secAccts = getSelectedSubAccounts(root, Account.ACCOUNT_TYPE_INVESTMENT, Account.ACCOUNT_TYPE_SECURITY);
        this.SecTns = getTransactionsFromAccounts(root, secAccts);
        this.assocSecTnsMap = getMapAssocSecTns(SecTns);
        this.secCur = getAccountCurrencyMap(root, secAccts);
        this.curSec = getCurrencyAccountMap(secCur);
        this.allCurrTypes = getAllCurTypes();
        this.invSec = getMapInvSec(assocSecTnsMap);
        this.transValuesMap = getTransValuesMap(assocSecTnsMap);
        this.transValuesCumMap = getTransValuesCumMap(transValuesMap);

    }

    /**
     * loads selected accounts into HashSet
     * @param parentAcct parent account for query (i.e. "retreive all below")
     * @param acctTypes integer designation of account types (varArg)
     * @return HashSet of Accounts
     */

    public static HashSet<Account> getSelectedSubAccounts(Account parentAcct, int... acctTypes) {
        int sz = parentAcct.getSubAccountCount();
        ArrayList<Integer> acctTypesList = new ArrayList<Integer>();
        HashSet<Account> SubAccts = new HashSet<Account>();
        if (acctTypes.length > 0) {
            for (int i = 0; i < acctTypes.length; i++) {
                acctTypesList.add(acctTypes[i]);
            }
        }
        for (int i = 0; i < sz; i++) {
            Account acct = parentAcct.getSubAccount(i);
            if (acctTypesList.contains(acct.getAccountType()) || acctTypes.length == 0) {
                SubAccts.add(acct);
            }
            SubAccts.addAll(getSelectedSubAccounts(acct, acctTypes)); //recursively add accounts
        }
        return SubAccts; //includes accounts with no transactions!
    }

    /**
     * loads transactions from a set of accounts into hash set
     * @param root root account
     * @param Accts hash set of accounts
     * @return Hash set of transactions (Parent and Split)
     */
    public static HashSet<AbstractTxn> getTransactionsFromAccounts(RootAccount root, HashSet<Account> Accts) {

        TransactionSet txnSet = root.getTransactionSet();
        Enumeration txnEnum = txnSet.getAllTransactions();
        HashSet<AbstractTxn> txns = new HashSet<AbstractTxn>();

        while (txnEnum.hasMoreElements()) {
            AbstractTxn txnBase = (AbstractTxn) txnEnum.nextElement();
            if (Accts.contains(txnBase.getAccount())) {
                txns.add(txnBase);
            }
        }
        return txns;
    }

     /**
      * creates map of account to associated hash set of <b>Parent</b> transactions
      * under the rule that, if a security account is part of the parent transaction,
      * the security account is <b>associated</b> with the parent transaction.
      * Investment accounts are, then, only associated with transactions which have
      * no security accounts in the Parent or Split
      * @param txns list of transactions
      * @return HashMap of Accounts with their associated transactions
      */
     public static HashMap<Account, HashSet<AbstractTxn>> getMapAssocSecTns(HashSet<AbstractTxn> txns) {
        HashMap<Account, HashSet<AbstractTxn>> acctMap = new HashMap<Account, HashSet<AbstractTxn>>();

        for (Iterator<AbstractTxn> it = txns.iterator(); it.hasNext();) {
            AbstractTxn thisTxn = it.next();
            HashSet<AbstractTxn> assocTrans = new HashSet<AbstractTxn>();
            Account assocAcct = thisTxn.getAccount();

            if (thisTxn instanceof ParentTxn) {
                assocTrans.add(thisTxn);
                ParentTxn parent = (ParentTxn) thisTxn;
                /*tests for presence of Security Accounts in transaction */
                for (int i = 0; i < parent.getSplitCount(); i++) {
                    if (parent.getSplit(i).getAccount().getAccountType()
                            == Account.ACCOUNT_TYPE_SECURITY) {
                        assocAcct =
                                parent.getSplit(i).getAccount();
                    }
                }
            } else { /* transaction is split */
                SplitTxn split = (SplitTxn) thisTxn;
                ParentTxn assocParent = split.getParentTxn();
                assocTrans.add(assocParent);
                for (int i = 0; i < assocParent.getSplitCount(); i++) {
                    /*tests for presence of Security Accounts in transaction */
                    if (assocParent.getSplit(i).getAccount().getAccountType()
                            == Account.ACCOUNT_TYPE_SECURITY) {
                        assocAcct =
                                assocParent.getSplit(i).getAccount();
                    }
                }
            }
            // add relationship to map
            if (acctMap.get(assocAcct) == null) { //first time this account has been seen
                acctMap.put(assocAcct, assocTrans);
            } else { //this account has been seen before
                acctMap.get(assocAcct).addAll(assocTrans);
            }
        }
        return acctMap;
    }

     /**
      * generates map of Securities with associated Currencies
      * note that Securities might have no associated transactions
      * @param root root account
      * @param SubAccts account list
      * @return map of security-currency relationships
      */
    public static  HashMap<Account, CurrencyType> getAccountCurrencyMap(RootAccount root, HashSet<Account> SubAccts) {
        HashMap<Account, CurrencyType> AcctCur = new HashMap<Account, CurrencyType>();
        for (Iterator<Account> it = SubAccts.iterator(); it.hasNext();) {
            Account account = it.next();
            if(account.getAccountType() == Account.ACCOUNT_TYPE_SECURITY)
                AcctCur.put(account, account.getCurrencyType());
        }
        return AcctCur;
    }

    /**
      * generates map of Currencies with associated Securities
      * note that Securities might have no associated transactions
     * @param AcctCur map of Security Accounts to associated Currencies
     * @return map of currency-security relationships
      */
    public static HashMap<CurrencyType, HashSet<Account>> getCurrencyAccountMap(HashMap<Account, CurrencyType> AcctCur) {
        HashMap<CurrencyType, HashSet<Account>> CurrAcct =
                new HashMap<CurrencyType, HashSet<Account>>();
        for (Iterator<Account> it = AcctCur.keySet().iterator(); it.hasNext();) {
            Account account = (Account) it.next();
            CurrencyType thisCur = AcctCur.get(account);
            HashSet<Account> acctSet = new HashSet<Account>();
            // add relationship to map
            if (CurrAcct.get(thisCur) == null) { //first time this currency has been seen
                acctSet.add(account);
                CurrAcct.put(thisCur, acctSet);
            } else { //this account has been seen before
                CurrAcct.get(thisCur).add(account);
            }
        }
        return CurrAcct;
    }

    /**
     * generates HashSet of all Currencies
     * @return "no-duplicates" list of currencies from SecCur
     */
     private HashSet<CurrencyType> getAllCurTypes() {
        HashSet<CurrencyType> CurNoDupes = new HashSet<CurrencyType>();
        for (Iterator it = secCur.values().iterator(); it.hasNext();) {
            CurrencyType cur = (CurrencyType) it.next();
            CurNoDupes.add(cur);
        }
        return CurNoDupes;
    }

    /**
     * retrieves basic transaction values from list of parent transactions
     * @param assocSecTnsMap map of accounts to associated parent transactions
     * @return HashMap of Accounts and associated basic transaction data
     */
    private HashMap<Account, SortedSet<TransValues>> getTransValuesMap(
            HashMap<Account, HashSet<AbstractTxn>> assocSecTnsMap) {

        HashMap<Account, SortedSet<TransValues>> ParentInfoMap =
                new HashMap<Account, SortedSet<TransValues>>();
        //add ParentTxns to map
        for (Iterator<Account> it = assocSecTnsMap.keySet().iterator(); it.hasNext();) {
            Account account = (Account) it.next();
            HashSet<AbstractTxn> tns = new HashSet<AbstractTxn>(assocSecTnsMap.get(account));
            SortedSet<TransValues> transValues = new TreeSet<TransValues>();
            for (Iterator<AbstractTxn> it1 = tns.iterator(); it1.hasNext();) {
                AbstractTxn abstractTxn = it1.next();

                if (abstractTxn instanceof ParentTxn) {
                    ParentTxn pTxn = (ParentTxn) abstractTxn;
                    transValues.add(new TransValues(pTxn, account));
                }
            }
            ParentInfoMap.put(account, transValues);
        }
        return ParentInfoMap;
    }

     /**
     * retrieves cumulative transaction values from list of parent transactions
     * @param assocSecTnsMap map of accounts to associated parent transactions
     * @return HashMap of Accounts and associated cumulative transaction data
     */
    private HashMap<Account, SortedSet<TransValuesCum>> getTransValuesCumMap
            (HashMap<Account,SortedSet<TransValues>> transValuesMap) {
        HashMap<Account, SortedSet<TransValuesCum>> thisTransValuesCumMap =
                new HashMap<Account, SortedSet<TransValuesCum>>();
        for (Iterator it = transValuesMap.keySet().iterator(); it.hasNext();) {
            Account thisAccount = (Account) it.next();
            SortedSet<TransValues> transValues = new TreeSet<TransValues>(transValuesMap.get(thisAccount));
            SortedSet<TransValuesCum> transValuesCum = TransValuesCum.getTransValuesCum(transValues, this);
            thisTransValuesCumMap.put(thisAccount, transValuesCum);
        }
        return thisTransValuesCumMap;
    }
    
    /**
     * generates map of investment accounts and associated security accounts
     * @param assocSecTnsMap
     * @return map of investment accounts to security sub accounts
     */
     private HashMap<Account, HashSet<Account>> getMapInvSec(HashMap<Account, HashSet<AbstractTxn>> assocSecTnsMap) {
        HashMap<Account, HashSet<Account>> thisInvSec = new HashMap<Account, HashSet<Account>>();
        for (Iterator<Account> it = assocSecTnsMap.keySet().iterator(); it.hasNext();) {
            Account account = (Account) it.next();
            HashSet<Account> secs = new HashSet<Account>();
            if (account.getAccountType() == Account.ACCOUNT_TYPE_SECURITY) {
                if (thisInvSec.get(account.getParentAccount()) == null) {
                    secs.add(account);
                    thisInvSec.put(account.getParentAccount(), secs);
                } else {
                    thisInvSec.get(account.getParentAccount()).add(account);
                }
            }
        }
        return thisInvSec;
    }
    
     /**
      * generates array list of string arrays for cumulative transaction info
      * to facilitate output ot file
      * @param transValuesCumMap HashSet of Accounts to cumulative transaction info
      * @return ArrayList of String Arrays for output
      */
     public ArrayList<String[]> listTransValuesCumMap(HashMap<Account, SortedSet<TransValuesCum>> transValuesCumMap) {
        ArrayList<String[]> txnInfo = new ArrayList<String[]>();

        for (Iterator it = transValuesCumMap.keySet().iterator(); it.hasNext();) {
            Account thisAccount = (Account) it.next();
            TreeSet<TransValuesCum> cpvs = new TreeSet<TransValuesCum>(transValuesCumMap.get(thisAccount));
            for (Iterator it1 = cpvs.iterator(); it1.hasNext();) {
                TransValuesCum cpv = (TransValuesCum) it1.next();
                txnInfo.add(TransValuesCum.loadArrayTransValuesCum(cpv));
            }
        }
        Collections.sort(txnInfo, PrntAcct_Order);
        return txnInfo;
    }

    public static final Comparator<String[]> PrntAcct_Order =
                                 new Comparator<String[]>() {
        public int compare(String[] o1, String[] o2) {
            int parentCmp = o1[0].compareTo(o2[0]);
            return (parentCmp == 0 ? o1[1].compareTo(o2[1]) : parentCmp );
        }

    };

    /**
     * loads currency data into ArrayList of String Arrays
     * @param allCurTypes HashSet of currencies
     * @return ArrayList of String Arrays
     */
    public static  ArrayList<String[]> ListAllCurrenciesInfo(HashSet<CurrencyType> allCurTypes) {
        ArrayList<String[]> currInfo = new ArrayList<String[]>();

        for (Iterator it = allCurTypes.iterator(); it.hasNext();) {
            CurrencyType cur = (CurrencyType) it.next();
            for (int i = 0; i < cur.getSnapshotCount(); i++) {

                currInfo.add(loadCurrencySnapshotArray(cur, i));
            }
        }
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
        currInfo.add(Double.toString(1/closeRate));
        currInfo.add(Double.toString(1/cur.adjustRateForSplitsInt(dateint, closeRate, todayDate)));
        return currInfo.toArray(new String[currInfo.size()]);
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

}

//Unused Methods Follow:
// <editor-fold defaultstate="collapsed" desc="comment">
    /*
   



    public StringBuffer listTxnInfo(AbstractTxn abstractTxn) { //unused

        StringBuffer txnInfo = new StringBuffer();
        if (abstractTxn instanceof ParentTxn) {
            ParentTxn parent = (ParentTxn) abstractTxn;
            txnInfo.append("TxType; ParentTxn" + ",");
            txnInfo.append(" Txn " + "id; " + parent.getTxnId() + ",");
            txnInfo.append(" AcctNum; " + parent.getAccount().getAccountNum() + ",");
            txnInfo.append(" AcctName; " + parent.getAccount().getAccountName() + ",");
            txnInfo.append(" TransType; " + parent.getTransferType() + ",");
            txnInfo.append("InvstTxnType; " + TxnUtil.getInvstTxnType(parent) + ",");
            txnInfo.append(" Date; " + parent.getDateInt() + ",");
            txnInfo.append(" Value; " + parent.getValue() + ",");
            txnInfo.append("ParentID;" + parent.getParentTxn().getTxnId() + ",");
            txnInfo.append("ParentTxAcctName;" + parent.getParentTxn().getAccount().getAccountName() + ",");
            txnInfo.append("Amount; NoAmt" + ",");
            txnInfo.append("Rate; NoRate" + ",");

        }
        if (abstractTxn instanceof SplitTxn) {
            SplitTxn split = (SplitTxn) abstractTxn;
            txnInfo.append("TxType; SplitTxn" + ",");
            txnInfo.append(" Txn " + "id; " + split.getTxnId() + ",");
            txnInfo.append(" AcctNum; " + split.getAccount().getAccountNum() + ",");
            txnInfo.append(" AcctName; " + split.getAccount().getAccountName() + ",");
            txnInfo.append(" TransType; " + split.getTransferType() + ",");
            txnInfo.append("InvstTxnType; " + TxnUtil.getInvstTxnType(split.getParentTxn()) + ",");
            txnInfo.append(" Date; " + split.getDateInt() + ",");
            txnInfo.append(" Value; " + split.getValue() + ",");
            txnInfo.append("ParentID;" + split.getParentTxn().getTxnId() + ",");
            txnInfo.append("ParentTxAcctName;" + split.getParentTxn().getAccount().getAccountName() + ",");
            txnInfo.append(" Amount; " + split.getAmount() + ",");
            txnInfo.append(" Rate; " + split.getRate() + ",");

        }
        return txnInfo;
    }

    public StringBuffer listSubAccounts(HashSet<Account> SubAccts) { // unused
        StringBuffer secList = new StringBuffer();

        for (Iterator<Account> it = SubAccts.iterator(); it.hasNext();) {
            Account account = it.next();
            secList.append("Name; " + account.getAccountName() + ", ");
            secList.append("Number; " + account.getAccountNum() + ", ");
            secList.append("AcctType; " + account.getAccountType() + ", ");
            secList.append("ParentAcctName; " + account.getParentAccount().getAccountName() + ", ");
            secList.append("ParentAcctNum; " + account.getParentAccount().getAccountNum() + ", ");
            secList.append("FullName; " + account.getFullAccountName() + ", ");
            secList.append("RecBalance; " + account.getRecursiveBalance() + ", ");
            if (account.getCurrencyType() != null) {
                secList.append("CurrID; " + account.getCurrencyType().getID() + ", ");
            } else {
                secList.append("CurrID; " + "No Currency" + ", ");
            }
            if (account.getCurrencyType().getTickerSymbol() != null) {
                secList.append("Ticker; " + account.getCurrencyType().getTickerSymbol() + ", ");
            } else {
                secList.append("Ticker; " + "No Ticker" + ", ");
            }
            secList.append("\n");
        }
        return secList;
    }

    public StringBuffer listAcctMapInfo(HashMap<Account, HashSet<AbstractTxn>> acctMap) {// unused
        StringBuffer mapInfo = new StringBuffer();


        for (Iterator it = acctMap.keySet().iterator(); it.hasNext();) {
            Account secAcct = (Account) it.next();
            HashSet<AbstractTxn> absTxns = acctMap.get(secAcct);
            mapInfo.append(listTxnsInfo(absTxns, secAcct));
        }
        return mapInfo;
    }

    public StringBuffer listTxnsInfo(HashSet<AbstractTxn> inAbsTxns, Account thisAccount) { // unused
        StringBuffer txnInfo = new StringBuffer();

        for (Iterator<AbstractTxn> it = inAbsTxns.iterator(); it.hasNext();) {
            AbstractTxn abstractTxn = it.next();
            txnInfo.append("LinkedAccount; " + thisAccount.getAccountName()
                    + "(Num " + thisAccount.getAccountNum() + "),");
            txnInfo.append(listTxnInfo(abstractTxn));
            txnInfo.append("\n");
        }
        return txnInfo;
    }

    public StringBuffer listTxnsInfo(HashSet<AbstractTxn> inAbsTxns) { //unused
        StringBuffer txnInfo = new StringBuffer();

        for (Iterator<AbstractTxn> it = inAbsTxns.iterator(); it.hasNext();) {
            AbstractTxn abstractTxn = it.next();
            txnInfo.append("LinkedAccount; " + abstractTxn.getAccount().getAccountName()
                    + "(Num " + abstractTxn.getAccount().getAccountNum() + "),");
            txnInfo.append(listTxnInfo(abstractTxn));
            txnInfo.append("\n");
        }
        return txnInfo;
    }

    public static StringBuffer listParentInfoMap(HashMap<Account, SortedSet<TransValues>> ParentInfoMap) { //unused
        StringBuffer txnInfo = new StringBuffer();
        for (Iterator it = ParentInfoMap.keySet().iterator(); it.hasNext();) {
            Account thisAccount = (Account) it.next();
            TreeSet<TransValues> pvs = new TreeSet<TransValues>(ParentInfoMap.get(thisAccount));
            for (Iterator it1 = pvs.iterator(); it1.hasNext();) {
                TransValues pv = (TransValues) it1.next();
                txnInfo.append("LinkedAccount; " + thisAccount.getAccountName()
                        + "(Num " + thisAccount.getAccountNum() + "),");
                txnInfo.append(TransValues.listTransValues(pv));
                txnInfo.append("\n");
            }
        }
        return txnInfo;
    }

     public static StringBuffer writeCurrencyArrayList(ArrayList<String[]> inpArrayList) {
        StringBuffer addInfo = new StringBuffer();
        for (Iterator<String[]> it = inpArrayList.iterator(); it.hasNext();) {
            String[] strings = it.next();
            addInfo.append("Symbol; " + strings[0] + "," + "Date; " + strings[1]
                    + "Price; " + strings[2] + "\n");
        }
        return addInfo;
    }

    public static StringBuffer readFileIntoCurrSnap(BulkSecInfo currentInfo, File dataFile) {
        //expects to see--CurrID, dateint, price from csv file -- adjust to show currency id
        StringBuffer addInfo = new StringBuffer();
        ArrayList<String[]> inputCurData = new ArrayList<String[]>();
        inputCurData = IOMethods.readCSVIntoArrayList(dataFile);
        addInfo.append("Read File of Size; " + inputCurData.size() + "\n");

        //build hash map of ticker to CurrencyType
        HashMap<String, CurrencyType> symbolMap = new HashMap<String, CurrencyType>();
        for (Iterator<CurrencyType> it = currentInfo.allCurrTypes.iterator(); it.hasNext();) {
            CurrencyType thisCur = it.next();
            String id = Integer.toString(thisCur.getID());
            symbolMap.put(id, thisCur); //was getTickerSymbol
        }
        //get current currency info (Hash map of CurrencyType to price map)
        HashMap<CurrencyType, HashMap<Integer, Double>> allRates =
                new HashMap<CurrencyType, HashMap<Integer, Double>>();
        allRates = getCurrInfo(currentInfo.allCurrTypes);

        //removes all current snapshots
        for (Iterator<CurrencyType> it = allRates.keySet().iterator(); it.hasNext();) {
            CurrencyType thisCurrency = (CurrencyType) it.next();
            for (int i = 0; i < thisCurrency.getSnapshotCount(); i++) {
                int tempID = thisCurrency.getID();
                int tempDateInt = thisCurrency.getSnapshot(i).getDateInt();
                addInfo.append("Removed ID; " + tempID + ",");
                addInfo.append("Removed Date; " + tempDateInt + "\n");
                thisCurrency.removeSnapshot(i);
            }
        }

        for (Iterator<String[]> it = inputCurData.iterator(); it.hasNext();) {
            String[] strings = it.next();
            String symbol = new String();
            int dateInt = 0;
            double inputPrice = 0;
            symbol = strings[0];
            dateInt = Integer.valueOf(strings[1]);
            inputPrice = Double.valueOf(strings[2]);
            double inputUserRate = 1 / inputPrice;

            if (symbolMap.containsKey(symbol)) { //CurrencyType is part of current info
                CurrencyType thisCur = symbolMap.get(symbol);
                if (!allRates.get(thisCur).containsKey(dateInt)) {//i.e. if snapshot already exists
                    symbolMap.get(symbol).addSnapshotInt(dateInt, inputUserRate);
                    addInfo.append("Symbol; " + symbol + "," + "Date; " + dateInt + ","
                            + "UserRate; " + inputUserRate + "," + "String Length; "
                            + strings.length + "\n");
                } else {
                    addInfo.append("symbol; " + symbol
                            + ", Already has price for date;" + dateInt + "\n");
                }
            } else {
                addInfo.append("No Match for symbol; " + symbol + "\n");
            }
        }
        return addInfo;
    }

    public static HashMap<CurrencyType, HashMap<Integer, Double>> getCurrInfo(HashSet<CurrencyType> allCurTypes) {

        HashMap<CurrencyType, HashMap<Integer, Double>> allCurrRate = new HashMap<CurrencyType, HashMap<Integer, Double>>();


        for (Iterator<CurrencyType> it = allCurTypes.iterator(); it.hasNext();) {
            CurrencyType currencyType = it.next();
            HashMap<Integer, Double> thisCurrRate = new HashMap<Integer, Double>();
            for (int i = 0; i < currencyType.getSnapshotCount(); i++) {
                thisCurrRate.put(currencyType.getSnapshot(i).getDateInt(), currencyType.getSnapshot(i).getUserRate());
            }
            allCurrRate.put(currencyType, thisCurrRate);
        }
        return allCurrRate;
    }

    public StringBuffer listUserRates(HashMap<CurrencyType, HashMap<Integer, Double>> currRates) {
        StringBuffer outRates = new StringBuffer();
        for (Iterator<CurrencyType> it = currRates.keySet().iterator(); it.hasNext();) {
            CurrencyType thisCurrencyType = it.next();
            HashMap<Integer, Double> thisCurrRates = new HashMap<Integer, Double>();
            thisCurrRates = currRates.get(thisCurrencyType);
            for (Iterator<Integer> it1 = thisCurrRates.keySet().iterator(); it1.hasNext();) {
                Integer thisDate = it1.next();
                outRates.append("id; " + thisCurrencyType.getID() + ",");
                outRates.append("Name; " + thisCurrencyType.getName() + ",");
                if (!(thisCurrencyType.getTickerSymbol().isEmpty())) {
                    outRates.append("Ticker; " + thisCurrencyType.getTickerSymbol() + ",");
                } else {
                    outRates.append("Ticker; " + "NoTicker" + ",");
                }
                outRates.append("Date; " + thisDate + ",");
                outRates.append("UserRate; " + thisCurrRates.get(thisDate) + "\n");
            }
        }
        return outRates;
    }

    */
    // </editor-fold>