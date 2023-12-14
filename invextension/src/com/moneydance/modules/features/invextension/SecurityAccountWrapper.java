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

import com.infinitekind.moneydance.model.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Adds functionality to
 */
public final class SecurityAccountWrapper implements Aggregator, Comparable<SecurityAccountWrapper> {
    private final Account securityAccount;
    private CurrencyWrapper currencyWrapper;

    private TxnSet txnSet;

    private InvestmentAccountWrapper invAcctWrapper;

    private GainsCalc gainsCalc;

    private AccountBook accountBook;

    private CurrencyWrapper cashCurrencyWrapper;
    private boolean isCash;
    private Tradeable tradeable;
    private SecurityTypeWrapper securityTypeWrapper;
    private SecuritySubTypeWrapper securitySubTypeWrapper;

    private String name;
    @Nullable
    private LinkedHashMap<String, TransactionValues> transValueMap;
    private LinkedHashMap<String, String> idMap;
    private DIV_FREQUENCY divFrequency = DIV_FREQUENCY.UNKNOWN;

    public SecurityAccountWrapper(@NotNull Account secAcct,
                                  CurrencyWrapper currencyWrapper, TxnSet txnSet,
                                  @NotNull InvestmentAccountWrapper invAcctWrapper,
                                  GainsCalc gainsCalc, AccountBook accountBook) throws Exception {
        this.securityAccount = secAcct;
        this.transValueMap = new LinkedHashMap<>();
        this.currencyWrapper = currencyWrapper;
        this.txnSet = txnSet;
        this.invAcctWrapper = invAcctWrapper;
        this.gainsCalc = gainsCalc;
        this.accountBook = accountBook;
        this.cashCurrencyWrapper = this.getInvAcctWrapper().getCashCurrencyWrapper();
        this.isCash = Objects.equals(this.currencyWrapper.curID, this.invAcctWrapper.getBulkSecInfo()
                .getCashCurrencyWrapper().getCurID());
        LogController.logMessage(Level.FINE, String.format("load Security Acct: %s | %s of currency type %s",
                this.securityAccount.getAccountName(), this.securityAccount.getUUID(),
                this.currencyWrapper.getCurrencyType().getUUID()));
        if(this.currencyWrapper == null){  // FIXME: remove after problem fixed
            LogController.logMessage(Level.WARNING, String.format("Security Acct: %s  in investment account %s has " +
                            "null currency type!",
                    this.securityAccount.getAccountName(), this.invAcctWrapper.getName()));
        }
        this.tradeable = new Tradeable(this.currencyWrapper, cashCurrencyWrapper);
        this.securityTypeWrapper = new SecurityTypeWrapper(this);
        this.securitySubTypeWrapper = new SecuritySubTypeWrapper(this);
        this.name = Objects.requireNonNull(secAcct.getAccountName()).trim();
        LogController.logMessage(Level.FINE, String.format("Generating Transaction Lines for Security Acct: %s",
                this.getName()));
        this.idMap = new LinkedHashMap<>();
        generateTransValues();
        CurrencyWrapper thisCurWrapper = invAcctWrapper.getBulkSecInfo().getCurrencyWrappers().get(secAcct
                .getCurrencyType().getParameter("id"));
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

    /**
     * gets TransactionValues for either single security account or
     * in the case of an investment cash account, the TransValues associated
     * with Investment Account cash
     */
    public void generateTransValues() throws Exception {
        this.transValueMap = new LinkedHashMap<>();
        ArrayList<ParentTxn> assocTrans = new ArrayList<>();
        Account thisAccount = isCash ? invAcctWrapper.getInvestmentAccount() : this.securityAccount;
        DividendFrequencyAnalyzer dividendFrequencyAnalyzer = new DividendFrequencyAnalyzer();
        for (AbstractTxn abstractTxn : txnSet) {
            if (BulkSecInfo.getAssociatedAccount(abstractTxn) == thisAccount) {
                ParentTxn parentTxn = abstractTxn instanceof ParentTxn ? (ParentTxn) abstractTxn : abstractTxn
                        .getParentTxn();
                if (!assocTrans.contains(parentTxn)) assocTrans.add(parentTxn);
            }

        }
        assocTrans.sort(BulkSecInfo.txnComp);
        for (ParentTxn parentTxn : assocTrans) {
            TransactionValues transValuesToAdd = new TransactionValues(parentTxn,
                    thisAccount, this, gainsCalc, this.transValueMap);
            dividendFrequencyAnalyzer.analyzeDividend(transValuesToAdd);
            assert this.transValueMap != null;
            this.transValueMap.put(transValuesToAdd.getTxnID(), transValuesToAdd);
            idMap.putAll(transValuesToAdd.getIdMap());
        }
        if (thisAccount.getAccountType() == Account.AccountType.INVESTMENT)
            LogController.logMessage(Level.FINE, String.format("Adding Cash Transactions for %s",
                    thisAccount.getAccountName()));
        assert this.transValueMap != null;
        LogController.logMessage(Level.FINE, String.format("For %s, Adding %d transaction lines",
                thisAccount.getAccountName(),this.transValueMap.size()));
//        setTransValuesMap(prevTransValuesMap);
    }

    public double getCurrencyRateByDateInt(int dateInt){
        return this.invAcctWrapper.getAccountCurrencyUserRateByDateInt(dateInt);
    }

    public long getPrice(int dateInt) {
        if (currencyWrapper.isCash) {
            return 100;
        } else {  // Price returned is the latest price if requested date is before first snapshot
            // Correct that by taking nearest snapshot (i.e. first)
            List<CurrencySnapshot> snapshots = currencyWrapper.getCurrencyType().getSnapshots();
            if (!snapshots.isEmpty()) {
                CurrencySnapshot firstSnapshot = snapshots.getFirst();
                if (dateInt < firstSnapshot.getDateInt()) {
                    return Math.round((1.0 / firstSnapshot.getRate() *
                            this.getCurrencyRateByDateInt(dateInt))* 100);
                } else {
                    return Math.round((1.0 / currencyWrapper.getCurrencyType().getRate(null, dateInt)
                            * this.getCurrencyRateByDateInt(dateInt))* 100);
                }
            } else {
                return Math.round((1.0 / currencyWrapper.getCurrencyType().getRate(null, dateInt) *
                        this.getCurrencyRateByDateInt(dateInt))  * 100);
            }

        }
    }

    @NotNull
    public ArrayList<String[]> listTransValuesInfo() {
        assert transValueMap != null;
        return transValueMap.values().stream().map(TransactionValues::listInfo)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public AccountBook getAccountBook(){
        return this.accountBook;
    }

    public CurrencyWrapper getCurrencyWrapper() {
        return this.currencyWrapper;
    }

    public CurrencyWrapper getCashCurrencyWrapper(){ return this.cashCurrencyWrapper;}

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
            return Objects.requireNonNull(securityAccount.getAccountName()).trim();
        } else {
            return this.name;
        }
    }

    public String getFullName(){
        if (securityAccount != null) {
            return Objects.requireNonNull(securityAccount.getParentAccount()).getAccountName() + ":" +
                    Objects.requireNonNull(securityAccount.getAccountName()).trim();
        } else {
            return this.name;
        }
    }

    public String getInvestmentAccountName(){
        return invAcctWrapper.getInvestmentAccount().getAccountName();
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
    public LinkedHashMap<String, TransactionValues> getTransactionValues() {
        return this.transValueMap;
    }

    public void setAllTransactionValues(@Nullable LinkedHashMap<String, TransactionValues> transValueMap) {
        if (transValueMap != null) {
            this.transValueMap = transValueMap;
        }

    }
    
    @Override
    public int compareTo(@NotNull SecurityAccountWrapper o) {
        return BulkSecInfo.acctComp.compare(this.securityAccount, o.securityAccount);
    }

    public InvestmentAccountWrapper getInvAcctWrapper() {
        return invAcctWrapper;
    }

    public SecurityType getSecurityType() {
        return this.securityAccount.getSecurityType();
    }

    public String getSecuritySubType() {
        return securityAccount.getSecuritySubType();
    }

    public Account getSecurityAccount() {
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

    public TransactionValues getParentTransValuesFromSplitId(String splitId){
        if (idMap.containsKey(splitId)){
            String parentId = idMap.get(splitId);
            assert transValueMap != null;
            return transValueMap.getOrDefault(parentId, null);
        } else {
            return null;
        }
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
        public static final int DIV_FREQUENCY_INCREMENT = 60; // approx. two-month period
        boolean dividendDetermined;
        int lastDividendDateInt;

        DividendFrequencyAnalyzer() {
            this.dividendDetermined = false;
            this.lastDividendDateInt = -1; //dummy date
        }

        public void analyzeDividend(@NotNull TransactionValues transactionValues) {
            if (transactionValues.getIncome() > 0) {
                InvestTxnType transType = transactionValues.getParentTxn().getInvestTxnType();
                switch (transType) {
                    case DIVIDEND:
                    case DIVIDENDXFR:
                    case DIVIDEND_REINVEST:
                    case BANK:
                        if (transactionValues.getIncome() != 0.0) {
                            int dividendDate = transactionValues.getDateInt();
                            int daysBetweenDivs = lastDividendDateInt == -1 ? -1 : DateUtils
                                    .getDaysBetween(dividendDate, lastDividendDateInt);
                            if (daysBetweenDivs == -1) {//implies first dividend in history
                                setDivFrequency(DIV_FREQUENCY.ANNUAL);
                                lastDividendDateInt = dividendDate;
                            } else {//dividend frequency is annual unless we observe more frequent distributions
                                if (daysBetweenDivs <= MINIMUM_EX_DIV_DAYS) return; //ignore--probable correction of
                                // previous transaction
                                if (daysBetweenDivs < DIV_FREQUENCY_INCREMENT) {
                                    setDivFrequency(DIV_FREQUENCY.MONTHLY);
                                } else if (daysBetweenDivs < DIV_FREQUENCY_INCREMENT * 2) {
                                    setDivFrequency(DIV_FREQUENCY.QUARTERLY);
                                } else if (daysBetweenDivs < DIV_FREQUENCY_INCREMENT * 4) {
                                    setDivFrequency(DIV_FREQUENCY.BIANNUAL);
                                } else {
                                    // else dividend frequency still assumed to be annual
                                    setDivFrequency(DIV_FREQUENCY.ANNUAL);
                                }
                                lastDividendDateInt = dividendDate;
                                dividendDetermined = true;

                            }
                        }
                }
            }
        }
    }

}
