/*
 * SecurityAccountWrapper.java
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

import com.moneydance.apps.md.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

/**
 * Adds functionality to
 */
public class SecurityAccountWrapper implements Aggregator, Comparable<SecurityAccountWrapper> {
    private SecurityAccount securityAccount;
    private CurrencyWrapper currencyWrapper;
    private Tradeable tradeable;
    private SecurityTypeWrapper securityTypeWrapper;
    private SecuritySubTypeWrapper securitySubTypeWrapper;
    private InvestmentAccountWrapper invAcctWrapper;
    private String name;
    @Nullable
    private ArrayList<TransactionValues> transValuesList;
    private DIV_FREQUENCY divFrequency = DIV_FREQUENCY.UNKNOWN;

    public SecurityAccountWrapper(@NotNull SecurityAccount secAcct,
                                  @NotNull InvestmentAccountWrapper invAcct) throws Exception {
        this.securityAccount = secAcct;
        this.invAcctWrapper = invAcct;
        this.transValuesList = new ArrayList<>();
        this.currencyWrapper = invAcct.getBulkSecInfo().getCurrencyWrappers().get(secAcct.getCurrencyType()
                .getID());
        this.tradeable = new Tradeable(this.currencyWrapper);
        this.securityTypeWrapper = new SecurityTypeWrapper(this);
        this.securitySubTypeWrapper = new SecuritySubTypeWrapper(this);
        this.name = secAcct.getAccountName().trim();
        generateTransValues();
        CurrencyWrapper thisCurWrapper = invAcct.getBulkSecInfo().getCurrencyWrappers().get(secAcct
                .getCurrencyType().getID());
        // add account to list of accounts in currencyWrapper
        thisCurWrapper.secAccts.add(this);
        // set CurrencyWrapper associated with this SecurityWrapper
        setCurrencyWrapper(thisCurWrapper);
    }

    /**
     * empty object for table output
     *
     * @param name aggregation name
     */
    public SecurityAccountWrapper(String name) {
        securityAccount = null;
        this.name = name;
    }

    @SuppressWarnings("unused")
    @NotNull
    private static Vector<Account> getSubAccount(@NotNull SecurityAccount secAcct2) {
        Vector<Account> accts = new Vector<>();
        while (secAcct2.getSubAccounts().hasMoreElements()) {
            accts.add((Account) secAcct2.getSubAccounts().nextElement());
        }
        return accts;

    }

    /**
     * gets TransactionValues for either single security account or
     * in the case of an investment cash account, the TransValues associated
     * with Investment Account cash
     */
    public void generateTransValues() throws Exception {
        ArrayList<TransactionValues> transValuesSet = new ArrayList<>();
        ArrayList<ParentTxn> assocTrans = new ArrayList<>();
        Account thisAccount = currencyWrapper.curID == invAcctWrapper.getBulkSecInfo().getCashCurrencyWrapper().getCurID() ? invAcctWrapper
                .getInvestmentAccount() : this.securityAccount;
        TxnSet txnSet = invAcctWrapper.getBulkSecInfo().getTransactionSet().getTransactionsForAccount(thisAccount);
        DividendFrequencyAnalyzer dividendFrequencyAnalyzer = new DividendFrequencyAnalyzer();
        for (AbstractTxn abstractTxn : txnSet) {
            if (BulkSecInfo.getAssociatedAccount(abstractTxn) == thisAccount) {
                ParentTxn parentTxn = abstractTxn instanceof ParentTxn ? (ParentTxn) abstractTxn : abstractTxn
                        .getParentTxn();
                if (!assocTrans.contains(parentTxn)) assocTrans.add(parentTxn);
            }

        }
        Collections.sort(assocTrans, BulkSecInfo.txnComp);
        for (ParentTxn parentTxn : assocTrans) {
            TransactionValues transValuesToAdd = new TransactionValues(parentTxn,
                    thisAccount, this, transValuesSet, this.getBulkSecInfo());
            dividendFrequencyAnalyzer.analyzeDividend(transValuesToAdd);
            transValuesSet.add(transValuesToAdd);
            if (thisAccount instanceof SecurityAccount)
                invAcctWrapper.getBulkSecInfo().getSecurityTransactionValues().put(transValuesToAdd.getTxnID(),
                        transValuesToAdd);

        }
        setTransValuesList(transValuesSet);
    }

    public long getPrice(int dateInt) {
        if (currencyWrapper.isCash) {
            return 100;
        } else {
            return Math.round(1.0 / currencyWrapper.getCurrencyType().getUserRateByDateInt(dateInt) * 100);
        }
    }

    @NotNull
    public ArrayList<String[]> listTransValuesInfo() {
        ArrayList<String[]> outputList = new ArrayList<>();
        for (TransactionValues transactionValues : transValuesList) {
            outputList.add(transactionValues.listInfo());
        }
        return outputList;
    }

    private BulkSecInfo getBulkSecInfo() {
        return this.invAcctWrapper.getBulkSecInfo();
    }

    public CurrencyWrapper getCurrencyWrapper() {
        return this.currencyWrapper;
    }

    public void setCurrencyWrapper(CurrencyWrapper currWrapper) {
        this.currencyWrapper = currWrapper;
    }

    public SecurityTypeWrapper getSecurityTypeWrapper() {
        return this.securityTypeWrapper;
    }

    public SecuritySubTypeWrapper getSecuritySubTypeWrapper() {
        return this.securitySubTypeWrapper;
    }

    public Tradeable getTradeable() {
        return this.tradeable;
    }

    public boolean isTradeable() {
        return !(currencyWrapper == null || currencyWrapper.isCash());
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.moneydance.modules.features.invextension.IAccount#getAccountRef
     * ()
     */

    public String getName() {
        if (securityAccount != null) {
            return securityAccount.getAccountName().trim();
        } else {
            return this.name;
        }

    }

    public void setName(String name) {
        this.name = name;
    }

    @NotNull
    public SecurityAccountWrapper getSecurityAccountWrapper() {
        return this;
    }

    public DIV_FREQUENCY getDivFrequency() {
        return divFrequency;
    }

    public void setDivFrequency(DIV_FREQUENCY divFrequency) {
        this.divFrequency = divFrequency;
    }

    @Nullable
    public ArrayList<TransactionValues> getTransactionValues() {
        return this.transValuesList;
    }

    public void setAllTransactionValues(@Nullable ArrayList<TransactionValues> transValuesSet) {
        if (transValuesSet != null) {
            this.transValuesList = transValuesSet;
        }

    }

    @Override
    public int compareTo(@NotNull SecurityAccountWrapper o) {
        return BulkSecInfo.acctComp.compare(this.securityAccount, o.securityAccount);
    }

    public InvestmentAccountWrapper getInvAcctWrapper() {
        return invAcctWrapper;
    }

    public void setTransValuesList(@Nullable ArrayList<TransactionValues> transValuesList) {
        this.transValuesList = transValuesList;
    }

    @NotNull
    public ArrayList<TransactionValues> getFromToSubset(@NotNull ArrayList<Integer> fromToIndices) {
        if (!fromToIndices.isEmpty()) {
            return (new ArrayList<>(transValuesList.subList(fromToIndices.get(0),
                    fromToIndices.get(1) + 1)));//To index is exclusive per List interface
        } else {
            return new ArrayList<>();
        }
    }

    @NotNull
    public ArrayList<Integer> getFromToIndices(@NotNull DateRange dateRange) {
        ArrayList<Integer> outputList = new ArrayList<>();
        int fromElement = binarySearch(dateRange.getFromDateInt(), true);
        if (fromElement != -1) {
            int toElement = binarySearchRecursive(dateRange.getToDateInt(), fromElement,
                    this.getTransactionValues().size() - 1, false);
            if (toElement != -1) {
                outputList.add(fromElement);
                outputList.add(toElement);
            }
        }
        return outputList;
    }

    public int binarySearch(int searchDateInt, boolean getFromDate) {
        return binarySearchRecursive(searchDateInt, 0, this.getTransactionValues().size() - 1, getFromDate);
    }

    public int binarySearchRecursive(int searchDateInt, int left, int right, boolean getFromDate) {
        if (left > right || getTransactionValues().isEmpty()) return -1;
        int leftDateInt = getTransactionValues().get(left).getDateint();
        int rightDateInt = getTransactionValues().get(right).getDateint();
        if (getFromDate) {//check endpoints of list
            if (searchDateInt < leftDateInt) return left;
            if (searchDateInt >= rightDateInt) return -1;
        } else {
            if (searchDateInt >= rightDateInt) return right;
            if (searchDateInt < leftDateInt) return -1;
        }
        //"middle" defined so that 2 element-set returns correct side
        int middle = getFromDate ? Math.max((left + right) / 2, 1) : (left + right) / 2;
        int middleDateInt = getTransactionValues().get(middle).getDateint();
        if (getFromDate) { //get "from" element
            int leftOfMiddleDateInt = getTransactionValues().get(middle - 1).getDateint();
            if (searchDateInt < middleDateInt && searchDateInt >= leftOfMiddleDateInt) return middle;
            else if (searchDateInt < leftOfMiddleDateInt) return binarySearchRecursive(searchDateInt, left, middle - 1,
                    getFromDate); //search left side
            else return binarySearchRecursive(searchDateInt, middle + 1, right, getFromDate);
        } else { //get "to" element
            int rightOfMiddleDateInt = getTransactionValues().get(middle + 1).getDateint();
            if (searchDateInt >= middleDateInt && searchDateInt < rightOfMiddleDateInt) return middle;
            else if (searchDateInt >= rightOfMiddleDateInt)
                return binarySearchRecursive(searchDateInt, middle + 1, right,
                        getFromDate);//search right side
            else return binarySearchRecursive(searchDateInt, left, middle - 1, getFromDate);
        }
    }

    public SecurityType getSecurityType() {
        return this.securityAccount.getSecurityType();
    }

    public String getSecuritySubType() {
        return securityAccount.getSecuritySubType();
    }

    public SecurityAccount getSecurityAccount() {
        return securityAccount;
    }

    @Override
    public String getAggregateName() {
        return null;
    }

    @Override
    public String getAllTypesName() {
        return null;
    }

    @Override
    public String getColumnName() {
        return null;
    }

    @Override
    public String getReportingName() {
        return null;
    }


    public enum DIV_FREQUENCY {
        ANNUAL(),
        BIANNUAL(),
        QUARTERLY(),
        MONTHLY(),
        UNKNOWN();

        DIV_FREQUENCY() {
        }
    }

    /**
     * class determines dividend Frequency from
     * dividend history
     */
    class DividendFrequencyAnalyzer {
        public static final int MINIMUM_EX_DIV_DAYS = 21;
        public static final int DIV_FREQENCY_INCREMENT = 60; // approx. two-month period
        boolean dividendDetermined;
        int lastDividendDateInt;

        DividendFrequencyAnalyzer() {
            this.dividendDetermined = false;
            this.lastDividendDateInt = -1; //dummy date
        }

        void analyzeDividend(@NotNull TransactionValues transactionValues) {
            if (transactionValues.getIncome() > 0) {
                InvestTxnType transType = TxnUtil.getInvestTxnType(transactionValues.getParentTxn());
                switch (transType) {
                    case DIVIDEND:
                    case DIVIDENDXFR:
                    case DIVIDEND_REINVEST:
                    case BANK:
                        if (transactionValues.getIncome() != 0.0) {
                            int dividendDate = transactionValues.getDateint();
                            int daysBetweenDivs = lastDividendDateInt == -1 ? -1 : DateUtils
                                    .getDaysBetween(dividendDate, lastDividendDateInt);
                            if (daysBetweenDivs == -1) {//implies first dividend in history
                                setDivFrequency(DIV_FREQUENCY.ANNUAL);
                                lastDividendDateInt = dividendDate;
                            } else {//dividend frequency is annual unless we observe more frequent distributions
                                if (daysBetweenDivs <= MINIMUM_EX_DIV_DAYS) return; //ignore--probable correction of
                                // previous transaction
                                if (daysBetweenDivs > MINIMUM_EX_DIV_DAYS && daysBetweenDivs < DIV_FREQENCY_INCREMENT) {
                                    setDivFrequency(DIV_FREQUENCY.MONTHLY);
                                } else if (daysBetweenDivs >= DIV_FREQENCY_INCREMENT && daysBetweenDivs < DIV_FREQENCY_INCREMENT * 2) {
                                    setDivFrequency(DIV_FREQUENCY.QUARTERLY);
                                } else if (daysBetweenDivs >= DIV_FREQENCY_INCREMENT * 2 && daysBetweenDivs <
                                        DIV_FREQENCY_INCREMENT * 4) {
                                    setDivFrequency(DIV_FREQUENCY.BIANNUAL);
                                }
                                // else dividend frequency still assumed to be annual
                                lastDividendDateInt = dividendDate;
                                dividendDetermined = true;
                            }
                        }
                }
            }
        }
    }

}
