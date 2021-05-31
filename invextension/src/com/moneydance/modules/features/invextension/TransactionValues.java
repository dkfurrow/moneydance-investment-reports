/*
 * TransactionValues.java
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

import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Level;
import com.moneydance.modules.features.invextension.BulkSecInfo.ComparablePair;

/**
 * produces basic transaction data
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
public class TransactionValues implements Comparable<TransactionValues> {
    
    // cumulative total gain after completion of transaction
    private long cumTotalGain;
    private final ParentTxn parentTxn; // parentTxn account
    // reference account (to determine correct sign for transfers)
    private final Account referenceAccount;
    private final SecurityAccountWrapper securityAccountWrapper;
    private final Integer dateInt; // transaction date
    private final String txnID; // transaction ID

    
    static Comparator<TransactionValues> transComp = (t1, t2) -> {
        ComparablePair<? extends Comparable<?>>[] comparablePairs = new ComparablePair<?>[4];

        comparablePairs[0] = new ComparablePair<>(t1.dateInt, t2.dateInt);
        comparablePairs[1] = new ComparablePair<>(t1.referenceAccount.getUUID(),
                t2.referenceAccount.getUUID());
        comparablePairs[2] = new ComparablePair<>(t1.getTxnSortOrder(), t2.getTxnSortOrder());
        comparablePairs[3] = new ComparablePair<>(t1.txnID, t2.txnID);

        return BulkSecInfo.compareAll(comparablePairs);
    }; // end inner class

    private final String desc; // transaction description
    private long buy = 0; // buy amount
    private long sell = 0; // sell amount
    private long shortSell = 0; // short sell amount
    private long coverShort = 0; // cover short amount
    private long commission = 0; // commission amount
    private long income = 0; // income amount
    private long expense = 0; // expense amount
    private long transfer = 0; // transfer amount
    private long secQuantity = 0; // security quantity
    // net position after completion of transaction
    private long position = 0;
    // market price on close of transaction day
    private long mktPrice = 0;
    // net average cost long basis after completion of transaction
    private long longBasis = 0;
    // net average cost short basis after completion of transaction
    private long shortBasis = 0;
    // net open value after completion of transaction
    private long openValue = 0;
    // net cumulative unrealized gains after completion of transaction
    private long cumUnrealizedGain = 0;
    // period unrealized gain (one transaction to next) after completion of
    // transaction
    private long perUnrealizedGain = 0;
    // period realized gain (one transaction to next) after completion of
    // transaction
    private long perRealizedGain = 0;
    // period income and expense gain (one transaction to next) after completion
    // of transaction
    private long perIncomeExpense = 0;
    // period total gain (one transaction to next) after completion of
    // transaction
    private long perTotalGain = 0;

    /**
     * Constructor to create a cash transaction for an Investment Account initial balance
     *
     * @param invAcctWrapper Investment Account
     */
    public TransactionValues(InvestmentAccountWrapper invAcctWrapper, int firstDateInt)
            throws Exception {
        // copy base values from Security Transaction
        String memo = "Inserted for Initial Balance: "
                + invAcctWrapper.getInvestmentAccount().getAccountName();
        this.securityAccountWrapper = invAcctWrapper.getCashAccountWrapper();
        this.parentTxn = new ParentTxn(null);
        parentTxn.setDateInt(firstDateInt);
        parentTxn.setTaxDateInt(firstDateInt);
        parentTxn.setDateEntered(0L);
        parentTxn.setDescription(memo);
        parentTxn.setMemo(memo);
        parentTxn.setStatus(AbstractTxn.STATUS_UNRECONCILED);
        String txnProxyUUID = invAcctWrapper.getInvestmentAccount().getParameter("id");
        parentTxn.setParameter("id", txnProxyUUID);
        this.txnID = txnProxyUUID;


        this.referenceAccount = invAcctWrapper.getCashAccountWrapper().getSecurityAccount();
        this.dateInt = firstDateInt;

        this.desc = this.parentTxn.getDescription();
        this.mktPrice = 100;


        long initBal = invAcctWrapper.getInvestmentAccount().getStartBalance();
        if (initBal > 0) {
            this.buy = -initBal;
            this.longBasis = initBal;
        }
        if (initBal < 0) {
            this.shortSell = -initBal;
            this.shortBasis = -initBal;
        }
        this.secQuantity = (-this.buy - this.coverShort - this.sell - this.shortSell) * 100;

        this.position = this.secQuantity;
        // OpenValue
        this.openValue = this.position * this.mktPrice / 10000;
        // mkt price is always 1, so no realized/unrealized gains

        // other fields derive
        this.perIncomeExpense = this.income + this.expense;
        this.perTotalGain = this.perUnrealizedGain + this.perRealizedGain + this.perIncomeExpense;
    }

    /**
     * Constructor which creates an appropriate TransactionValues object from a
     * primary security-level or Investment Account-level transaction
     *
     * @param thisParentTxn    Parent Transaction
     * @param referenceAccount Investment Account associated with Security or bank transaction
     */
    public TransactionValues(ParentTxn thisParentTxn, Account referenceAccount,
                             SecurityAccountWrapper securityAccountWrapper,
                             ArrayList<TransactionValues> prevTransLines,
                             BulkSecInfo currentInfo) throws Exception {

        //initialize values
        this.parentTxn = thisParentTxn;
        this.referenceAccount = referenceAccount;
        this.securityAccountWrapper = securityAccountWrapper;
        this.dateInt = thisParentTxn.getDateInt();
        this.txnID = thisParentTxn.getParameter("id");
        this.desc = thisParentTxn.getDescription();




        try {
            //iterate through splits
            for (int i = 0; i < parentTxn.getSplitCount(); i++) {
                //gets values for each split.  Account Reference is parentTxn
                //account in the case of a security, investment account
                //(i.e. itself) in the case of an investment account.
                SplitValues thisSplit = new SplitValues(parentTxn.getSplit(i),
                        referenceAccount.getAccountType() == Account.AccountType.INVESTMENT
                                ? referenceAccount : referenceAccount.getParentAccount());

                this.buy = this.buy + thisSplit.splitBuy;
                this.sell = this.sell + thisSplit.splitSell;
                this.shortSell = this.shortSell + thisSplit.splitShortSell;
                this.coverShort = this.coverShort + thisSplit.splitCoverShort;
                this.commission = this.commission + thisSplit.splitCommission;
                this.income = this.income + thisSplit.splitIncome;
                this.expense = this.expense + thisSplit.splitExpense;
                this.transfer = this.transfer + thisSplit.splitTransfer;
                this.secQuantity = this.secQuantity + thisSplit.splitSecQuantity;
            }

            //fill in rest of transValues
            TransactionValues prevTransLine = prevTransLines.isEmpty() ? null :
                    prevTransLines.get(prevTransLines.size() - 1);
            CurrencyType cur = this.referenceAccount.getCurrencyType();
            int currentDateInt = this.parentTxn.getDateInt();
            double currentRate = cur == null ? 1.0 : cur.getRate(null, currentDateInt);
            int prevDateInt = prevTransLine == null ? Integer.MIN_VALUE
                    : prevTransLine.parentTxn.getDateInt();
            double splitAdjust = (cur == null ? 1.0
                    : cur.adjustRateForSplitsInt(prevDateInt, currentRate, currentDateInt) / currentRate);
            long adjPrevPos = 0;
            long adjPrevMktPrc = 0;
            if (prevTransLine != null) {
                adjPrevPos = Math.round(prevTransLine.position * splitAdjust);
                adjPrevMktPrc = Math.round(prevTransLine.mktPrice / splitAdjust);
            }
            // mktPrice (Set to 1 if cur is null: Implies (Cash) Investment Account
            this.mktPrice = (cur == null ? 100
                    : Math.round(1 / cur.getRate(null, currentDateInt) * 100));

            // position
            if (prevTransLine == null) { // first transaction (buy || shortSell)
                // if first transaction improper, throw an exception
                InvestTxnType transactionType = parentTxn.getInvestTxnType();
                boolean validStartTransaction = (transactionType == InvestTxnType.BUY
                        || transactionType == InvestTxnType.BUY_XFER
                        || transactionType == InvestTxnType.SHORT);
                if (!validStartTransaction && securityAccountWrapper.isTradeable()) {
                    throwInitialTransactionException(transactionType);
                }
                this.position = this.secQuantity;
            } else { // subsequent transaction
                if(securityAccountWrapper.isTradeable()) {
                    testSubsequentTransaction(parentTxn.getInvestTxnType(), secQuantity, adjPrevPos);
                }
                this.position = this.secQuantity + adjPrevPos;
            }

            //get long and short basis
            GainsCalc gainsCalc = currentInfo.getGainsCalc();
            gainsCalc.initializeGainsCalc(currentInfo, this, prevTransLines);
            this.longBasis = gainsCalc.getLongBasis();
            this.shortBasis = gainsCalc.getShortBasis();

            // OpenValue
            this.openValue = this.position * this.mktPrice / 10000;

            // cumulative unrealized gains
            this.cumUnrealizedGain = 0;
            if (this.position > 0) {
                this.cumUnrealizedGain = this.openValue - this.longBasis;
            } else if (this.position < 0) {
                this.cumUnrealizedGain = this.openValue - this.shortBasis;
            }

            // period unrealized gains
            this.perUnrealizedGain = 0;
            if (this.position != 0) {
                if (this.secQuantity == 0) {
                    // income/expense transaction, period gain is
                    // change in cum unreal gains
                    this.perUnrealizedGain = this.cumUnrealizedGain
                            - (prevTransLine == null ? 0
                            : prevTransLine.cumUnrealizedGain);
                } else {// buy, sell, short, or cover transaction
                    // first case, add to long or add to short
                    // change in cumulative gains accounts for trans quantity
                    if (this.secQuantity * this.position > 0) {
                        this.perUnrealizedGain = this.cumUnrealizedGain
                                - (prevTransLine == null ? 0
                                : prevTransLine.cumUnrealizedGain);
                    } else { // reduce long or short
                        // unrealized gains equal 0 on position-closing
                        // transaction
                        this.perUnrealizedGain = this.position
                                * (this.mktPrice - adjPrevMktPrc) / 10000;
                    }
                }
            }

            // Period Realized gains
            if (this.sell > 0) { // sale transaction
                if (prevTransLine != null) {
                    this.perRealizedGain = (this.sell + this.commission + this.expense)
                            + (this.longBasis - prevTransLine.longBasis);
                } else {
                    throw new Exception(securityAccountWrapper.getName() + " : SELL/SELLXFER cannot be first transaction: ");
                }
            } else if (this.coverShort < 0) { // cover transaction
                if (prevTransLine != null) {
                    this.perRealizedGain = (this.coverShort + this.commission + this.expense)
                            + (this.shortBasis - prevTransLine.shortBasis);
                } else {
                    throw new Exception(securityAccountWrapper.getName() + " : COVER cannot be first transaction: ");
                }
            } else {
                // implies for closed pos, cumUnrealized-cumRealized =
                // commission (on last trade)
                this.perRealizedGain = 0;
            }

            // period income/expense
            this.perIncomeExpense = this.income + this.expense;

            // period total gain
            this.perTotalGain = this.perUnrealizedGain + this.perRealizedGain;

            // cumulative total gain
            this.cumTotalGain = prevTransLine == null ? this.perTotalGain :
                    this.perTotalGain + prevTransLine.cumTotalGain;
        } catch (Exception e) {
            String dateString = " Date: " + DateUtils.convertToShort(dateInt);
            String errorString = "Error in transaction values calculation, cash account: "
                    + securityAccountWrapper.getInvAcctWrapper().getName() +
                    " Security: " + securityAccountWrapper.getName() + dateString + " " + e.toString();
            LogController.getInstance();
            LogController.logMessage(Level.WARNING, errorString);
        }
    }

    /**
     * Generic Constructor for TransactionValues which are inserted into
     * synthetically created cash account
     *
     * @param transactionValues TransactionValues from Investment or Security Account
     * @param prevTransValues   previous TransactionValues (to get position)
     * @param invAcctWrapper    Investment Account
     */
    public TransactionValues(TransactionValues transactionValues, TransactionValues prevTransValues,
                             InvestmentAccountWrapper invAcctWrapper) throws Exception {
        // copy base values from Security Transaction
        this.parentTxn = transactionValues.parentTxn;
        this.referenceAccount = invAcctWrapper.getCashAccountWrapper().getSecurityAccount();
        this.securityAccountWrapper = prevTransValues.getSecurityAccountWrapper();
        this.dateInt = transactionValues.dateInt;
        // adding 0.1 to related transValues id to ensure unique cash id
        this.txnID = transactionValues.parentTxn.getInvestTxnType() ==
                InvestTxnType.BANK ? transactionValues.parentTxn.getParameter("id") :
                transactionValues.parentTxn.getParameter("id") + "_1";
        this.desc = "INSERTED: " + parentTxn.getDescription();
        this.mktPrice = 100;

        long thisTransfer = transactionValues.transfer;
        long acctEntry = -transactionValues.buy - transactionValues.coverShort
                - transactionValues.sell - transactionValues.shortSell - transactionValues.income
                - transactionValues.expense - transactionValues.commission;
        long prevPos = prevTransValues.position;
        long prevVal = prevPos / 100;
        
        InvestTxnType txnType = this.parentTxn.getInvestTxnType();

        switch (txnType) {
            case BANK: // transfer in/out, account-level income or expense
                if (thisTransfer > 0) {// transfer in
                    if (prevPos < 0) {
                        this.coverShort = Math.max(-thisTransfer, prevVal);
                        this.buy = Math.min(-thisTransfer - prevVal, 0);
                    } else {
                        this.buy = -thisTransfer;
                    }
                } else if (thisTransfer < 0) {// transfer out
                    if (prevPos > 0) {
                        this.sell = Math.min(-thisTransfer, prevVal);
                        this.shortSell = Math.max(-thisTransfer - prevVal, 0);
                    } else {
                        this.shortSell = -thisTransfer;
                    }
                } else { // income or expense
                    if (acctEntry <= 0) {// Account level Income
                        // like dividend/reinvest)
                        if (prevPos < 0) {
                            this.coverShort = Math.max(acctEntry, prevVal);
                            this.buy = Math.min(acctEntry - prevVal, 0);
                        } else {
                            this.buy = acctEntry;
                        }
                        this.income = -acctEntry;
                    } else {// Account level expense
                        // like capital call (debit expense credit security)
                        if (prevPos > 0) {
                            this.sell = Math.min(acctEntry, prevVal);
                            this.shortSell = Math.max(acctEntry - prevVal, 0);
                        } else {
                            this.shortSell = acctEntry;
                        }
                        this.expense = -acctEntry;

                    }
                }
                break;
            case BUY:
            case COVER:
            case MISCEXP: // include transfer to cover case of purchase against non-investment income
                if (prevPos > 0) {
                    this.sell = Math.min(acctEntry - thisTransfer, prevVal);
                    this.shortSell = Math.max(acctEntry - thisTransfer - prevVal, 0);
                } else {
                    this.shortSell = acctEntry - thisTransfer;
                }
                break;
            case SELL:
            case SHORT:
            case MISCINC:
            case DIVIDEND: // include transfer to cover case of sell to non-investment expense
                if (prevPos < 0) {
                    this.coverShort = Math.max(acctEntry - thisTransfer, prevVal);
                    this.buy = Math.min(acctEntry - thisTransfer - prevVal, 0);
                } else {
                    this.buy = acctEntry - thisTransfer;
                }
                break;
            case BUY_XFER:
            case SELL_XFER:
            case DIVIDENDXFR:
            case DIVIDEND_REINVEST:
                // All cash quantities stay zero (combined transactions have no
                // net effect on cash
                break;

        }

        this.secQuantity = (-this.buy - this.coverShort - this.sell - this.shortSell) * 100;

        this.position = this.secQuantity + prevPos;
        this.longBasis = Math.max(this.position, 0) / 100;
        this.shortBasis = Math.min(this.position, 0) / 100;
        // OpenValue
        this.openValue = this.position * this.mktPrice / 10000;
        //mkt price is always 1, so no realized/unrealized gains

        //other fields derive
        this.transfer = thisTransfer;
        this.perIncomeExpense = this.income + this.expense;
        this.perTotalGain = this.perUnrealizedGain + this.perRealizedGain + this.perIncomeExpense;
        this.cumTotalGain = this.perTotalGain + prevTransValues.cumTotalGain;
    }

    /*
     * lists header for transaction report
     */
    public static StringBuffer listTransValuesHeader() {
        StringBuffer txnInfo = new StringBuffer();
        txnInfo.append("ParentAcct " + ",");
        txnInfo.append("Security " + ",");
        txnInfo.append("Ticker " + ",");
        txnInfo.append("DivFrequency " + ",");
        txnInfo.append("TxnId " + ",");
        txnInfo.append("Date" + ",");
        txnInfo.append("TxnType" + ",");
        txnInfo.append("Desc" + ",");
        txnInfo.append("Buy" + ",");
        txnInfo.append("Sell" + ",");
        txnInfo.append("Short" + ",");
        txnInfo.append("Cover" + ",");
        txnInfo.append("Commission" + ",");
        txnInfo.append("Income" + ",");
        txnInfo.append("Expense" + ",");
        txnInfo.append("transfer" + ",");
        txnInfo.append("secQuantity" + ",");
        txnInfo.append("MktPrice" + ",");
        txnInfo.append("Position" + ",");
        txnInfo.append("LongBasis" + ",");
        txnInfo.append("ShortBasis" + ",");
        txnInfo.append("OpenValue" + ",");
        txnInfo.append("CumUnrealGain" + ",");
        txnInfo.append("PerUnrealGain" + ",");
        txnInfo.append("PerRealGain" + ",");
        txnInfo.append("PerInc_Exp" + ",");
        txnInfo.append("PerTotalGain" + ",");
        txnInfo.append("CumTotalGain");
        return txnInfo;
    }


    static class InitialTransactionException extends Exception {
        @Serial
        private static final long serialVersionUID = 7055848337870560480L;
        public InitialTransactionException(String message){
            super(message);
        }
    }

    private void throwInitialTransactionException(InvestTxnType transactionType) throws InitialTransactionException {
        String errorString = "Error in investment account: " + securityAccountWrapper.getInvAcctWrapper().getName() +
                " Security: " + securityAccountWrapper.getName() + " must start with either a Buy, BuyXfer or Short, " +
                "but instead starts with a " + transactionType.name();
        throw new InitialTransactionException(errorString);
    }

    private void testSubsequentTransaction(InvestTxnType investTxnType, long secQuantity, long adjPrevPos) {
        String warningStr = "";
        String dateString = " Date: " + DateUtils.convertToShort(dateInt);
        long thisPosition = this.secQuantity + adjPrevPos;
        if (adjPrevPos > 0) {
            if (thisPosition < 0) warningStr = "Error in investment account: " + securityAccountWrapper.getInvAcctWrapper().getName() +
                    " Security: " + securityAccountWrapper.getName() + dateString + " takes position from long to short in one transaction, " +
                    "Please create two transactions--a SELL or SELLXFER to flatten the position, and a separate SHORT " +
                    "transaction to create the final short position";
        } else if (adjPrevPos < 0) {
            if (thisPosition > 0) warningStr = "Error in investment account: " + securityAccountWrapper.getInvAcctWrapper().getName() +
                    " Security: " + securityAccountWrapper.getName() + dateString + " takes position from short to long in one transaction, " +
                    "Please create two transactions--a COVER to flatten the position, and a separate BUY or BUYXFER " +
                    "transaction to create the final long position";

        } else {
            if (secQuantity > 0) {
                boolean validTradeType = (investTxnType == InvestTxnType.BUY
                        || investTxnType == InvestTxnType.BUY_XFER || investTxnType == InvestTxnType.DIVIDEND_REINVEST);
                if (!validTradeType)
                    warningStr = "Error in investment account: " + securityAccountWrapper.getInvAcctWrapper().getName() +
                            " Security: " + securityAccountWrapper.getName() + dateString +
                            " takes position from flat to long, " +
                            "so must be a BUY or BUYXFER, " + "but instead is a " + investTxnType.name();
                if (validTradeType && investTxnType == InvestTxnType.DIVIDEND_REINVEST){
                    String noteStr =
                            "Check investment account: " + securityAccountWrapper.getInvAcctWrapper().getName() +
                            " Security: " + securityAccountWrapper.getName() + dateString +
                                    " takes position from flat to long, " +
                            "so should be a BUY or BUYXFER, " + "but instead is a " + investTxnType.name() + " " +
                                    "so check ex-dividend dates.";
                    LogController.logMessage(Level.INFO, noteStr);
                }
            } else if (secQuantity < 0) {
                boolean validTradeType = (investTxnType == InvestTxnType.SHORT);
                if (!validTradeType)
                    warningStr = "Error in investment account: " + securityAccountWrapper.getInvAcctWrapper().getName() +
                            " Security: " + securityAccountWrapper.getName() + dateString + " takes position from flat to short, " +
                            "so must be a SHORT, " + "but instead is a " + investTxnType.name();

            }
        }
        if (warningStr.length() > 0) LogController.logMessage(Level.WARNING, warningStr);
    }

    @Override
    public int compareTo(@NotNull TransactionValues transactionValues) {
        return TransactionValues.transComp.compare(this, transactionValues);
    }

    public SecurityAccountWrapper getSecurityAccountWrapper() {
        return securityAccountWrapper;
    }

    @SuppressWarnings("unused")
    public Account getAccountRef() {
        return referenceAccount;
    }

    @SuppressWarnings("unused")
    public long getCumTotalGain() {
        return cumTotalGain;
    }

    @SuppressWarnings("unused")
    public long getCumUnrealizedGain() {
        return cumUnrealizedGain;
    }

    public Integer getDateInt() {
        return dateInt;
    }

    @SuppressWarnings("unused")
    public String getDesc() {
        return desc;
    }

    public long getLongBasis() {
        return longBasis;
    }

    @SuppressWarnings("unused")
    public long getMktPrice() {
        return mktPrice;
    }

    @SuppressWarnings("unused")
    public long getOpenValue() {
        return openValue;
    }

    //getters and setters (supports use of compiled JAR)
    @SuppressWarnings("unused")
    public ParentTxn getParent() {
        return parentTxn;
    }

    @SuppressWarnings("unused")
    public ParentTxn getParentTxn() {
        return parentTxn;
    }

    @SuppressWarnings("unused")
    public long getPerIncomeExpense() {
        return perIncomeExpense;
    }

    public long getPerRealizedGain() {
        return perRealizedGain;
    }

    @SuppressWarnings("unused")
    public long getPerTotalGain() {
        return perTotalGain;
    }

    @SuppressWarnings("unused")
    public long getPerUnrealizedGain() {
        return perUnrealizedGain;
    }

    @SuppressWarnings("unused")
    public long getPosition() {
        return position;
    }

    public long getSecQuantity() {
        return secQuantity;
    }

    public long getShortBasis() {
        return shortBasis;
    }

    public String getTxnID() {
        return txnID;
    }

    public Long getTransfer() { return transfer;}

    public Account getReferenceAccount() {
        return referenceAccount;
    }

    public String[] listInfo() {
        ArrayList<String> txnInfo = new ArrayList<>();
        InvestTxnType transType = parentTxn.getInvestTxnType();
        txnInfo.add(referenceAccount.getParentAccount().getAccountName());
        txnInfo.add(referenceAccount.getAccountName());
        txnInfo.add(securityAccountWrapper.getCurrencyWrapper().getName() == null ? "NoTicker" : securityAccountWrapper.getCurrencyWrapper().getName());
        txnInfo.add(securityAccountWrapper.getDivFrequency().toString());
        txnInfo.add(txnID);
        txnInfo.add(DateUtils.convertToShort(dateInt));
        txnInfo.add(transType.toString());
        txnInfo.add(desc);
        txnInfo.add(Double.toString(buy / 100.0));
        txnInfo.add(Double.toString(sell / 100.0));
        txnInfo.add(Double.toString(shortSell / 100.0));
        txnInfo.add(Double.toString(coverShort / 100.0));
        txnInfo.add(Double.toString(commission / 100.0));
        txnInfo.add(Double.toString(income / 100.0));
        txnInfo.add(Double.toString(expense / 100.0));
        txnInfo.add(Double.toString(transfer / 100.0));
        txnInfo.add(Double.toString(secQuantity / 10000.0));
        txnInfo.add(Double.toString(mktPrice / 100.0));
        txnInfo.add(Double.toString(position / 10000.0));
        txnInfo.add(Double.toString(longBasis / 100.0));
        txnInfo.add(Double.toString(shortBasis / 100.0));
        txnInfo.add(Double.toString(openValue / 100.0));
        txnInfo.add(Double.toString(cumUnrealizedGain / 100.0));
        txnInfo.add(Double.toString(perUnrealizedGain / 100.0));
        txnInfo.add(Double.toString(perRealizedGain / 100.0));
        txnInfo.add(Double.toString(perIncomeExpense / 100.0));
        txnInfo.add(Double.toString(perTotalGain / 100.0));
        txnInfo.add(Double.toString(cumTotalGain / 100.0));
        return txnInfo.toArray(new String[0]);
    }

    /**
     * Custom sort order to put buys before sells, shorts before covers
     *
     * @return custom transaction order based on method
     */
    public Integer getTxnSortOrder() {
        InvestTxnType transType = this.parentTxn != null ?
                parentTxn.getInvestTxnType() : InvestTxnType.BANK;
        return BulkSecInfo.getTxnSortOrder(transType);
    }


    public long getIncomeExpenseFlows() {
        return getIncome() + getExpense();
    }



    public long getBuy() {
        return buy;
    }

    public long getCommission() {
        return commission;
    }

    public long getCoverShort() {
        return coverShort;
    }

    public long getExpense() {
        return expense;
    }

    public long getIncome() {
        return income;
    }

    public long getSell() {
        return sell;
    }

    public long getShortSell() {
        return shortSell;
    }

    /**
     * buySellFlows is net cash effect of buy/sell/short/cover, incl commission
     * note sign convention for modified-dietz calc...buys are > 0
     *
     * @return cash effect of buy/sell/short/cover transaction
     */
    public long getBuySellFlows() {
        return -(getBuy() + getSell() + getShortSell() + getCoverShort() + getCommission());
    }

    /**
     * determines buy/sell/income, etc values for split based upon
     * parentTxn transaction type.  variable names are same as TransactionValues
     */
    private class SplitValues {

        SplitTxn split;
        long splitBuy = 0;
        long splitSell = 0;
        long splitShortSell = 0;
        long splitCoverShort = 0;
        long splitCommission = 0;
        long splitIncome = 0;
        long splitExpense = 0;
        long splitTransfer = 0;
        long splitSecQuantity = 0;

        public SplitValues(SplitTxn thisSplit, Account accountRef) {
            this.split = thisSplit;
            InvestTxnType txnType = thisSplit.getParentTxn().getInvestTxnType();
            Account.AccountType acctType = thisSplit.getAccount().getAccountType();
            Account.AccountType parentAcctType = thisSplit.getParentTxn().getAccount().getAccountType();
            long amountLong = -thisSplit.getAmount(); //added minus sign for 2015
            long valueLong = thisSplit.getValue();


	    /*
         * goes through each transaction type, assigns values for each
	     * variable based on indicated transaction type and account type
	     */
            switch (txnType) {
                case BUY:// consists of buy, commission, and (potentially) transfer
                case BUY_XFER: // no net cash effect (transfer offsets buy)
                    switch (acctType) {
                        case SECURITY:
                            this.splitBuy = amountLong;
                            this.splitSecQuantity = valueLong;
                            break;
                        case EXPENSE:
                            this.splitCommission = amountLong;
                            break;
                        case INCOME:
                            if (isInvestmentIncome(thisSplit)) {
                                this.splitIncome = amountLong;
                            } else {
                                this.splitTransfer = split.getAccount() == accountRef
                                        ? -amountLong : amountLong;
                            }
                            break;
                        default:
                            this.splitTransfer = split.getAccount() == accountRef
                                    ? -amountLong : amountLong;
                            break;
                    }
                    break;
                case SELL:// consists of sell, commission, and (potentially) transfer
                case SELL_XFER: // no net cash effect (transfer offsets sell)
                    switch (acctType) {
                        case SECURITY:
                            this.splitSell = amountLong;
                            this.splitSecQuantity = valueLong;
                            break;
                        case EXPENSE:
                            if(split.equals(TxnUtil.getCommissionPart(parentTxn))){
                                this.splitCommission = amountLong;
                                break;
                            } else {
                                if(isInvestmentExpense(thisSplit)){
                                    this.splitExpense = amountLong;
                                } else {
                                    this.splitTransfer = split.getAccount() == accountRef
                                            ? -amountLong : amountLong;
                                }
                                break;
                            }
                        case INCOME:
                            if (isInvestmentIncome(thisSplit)) {
                                this.splitIncome = amountLong;
                            } else {
                                this.splitTransfer = split.getAccount() == accountRef
                                        ? -amountLong : amountLong;
                            }
                            break;
                        default:
                            this.splitTransfer = split.getAccount() == accountRef
                                    ? -amountLong : amountLong;
                            break;
                    }
                    break;
                case BANK: // Account-level transfers, interest, and expenses
                    switch (acctType) {
                        case EXPENSE:// Only count if parentTxn is investment
                            if (parentAcctType == Account.AccountType.INVESTMENT) {
                                if (isInvestmentExpense(thisSplit)) {
                                    this.splitExpense = amountLong;
                                } else {
                                    if (split.getAccount() == accountRef) {
                                        this.splitTransfer = -amountLong;
                                    } else {
                                        this.splitTransfer = amountLong;
                                    }
                                }
                            }
                            break;
                        case INCOME: // Only count if parentTxn is investment
                            if (parentAcctType == Account.AccountType.INVESTMENT) {
                                if (isInvestmentIncome(thisSplit)) {
                                    this.splitIncome = amountLong;
                                } else {
                                    if (split.getAccount() == accountRef) {
                                        this.splitTransfer = -amountLong;
                                    } else {
                                        this.splitTransfer = amountLong;
                                    }
                                }                                
                            }
                            break;
                        // next cases cover transfer between Assets/Investments, Bank.
                        case INVESTMENT:
                        case BANK:
                        case ASSET:
                        case LIABILITY:
                            if (split.getAccount() == accountRef) {
                                this.splitTransfer = -amountLong;
                            } else {
                                this.splitTransfer = amountLong;
                            }
                            break;
                    }
                    break;
                case DIVIDEND:
                case DIVIDENDXFR: // income/expense transactions
                    switch (acctType) {
                        case EXPENSE -> this.splitExpense = amountLong;
                        case INCOME -> this.splitIncome = amountLong;
                        default -> this.splitTransfer = split.getAccount() == accountRef ?
                                -amountLong : amountLong;
                    }
                    break;
                case SHORT: // short sales + commission
                    switch (acctType) {
                        case SECURITY -> {
                            this.splitShortSell = amountLong;
                            this.splitSecQuantity = valueLong;
                        }
                        case EXPENSE -> this.splitCommission = amountLong;
                        case INCOME -> this.splitIncome = amountLong;
                    }
                    break;
                case COVER:// short covers + commission
                    switch (acctType) {
                        case SECURITY -> {
                            this.splitCoverShort = amountLong;
                            this.splitSecQuantity = valueLong;
                        }
                        case EXPENSE -> this.splitCommission = amountLong;
                        case INCOME -> this.splitIncome = amountLong;
                    }
                    break;
                case MISCINC: // misc income and expense
                    switch (acctType) {
                        case EXPENSE -> this.splitExpense = amountLong;
                        case INCOME -> this.splitIncome = amountLong;
                        case SECURITY -> {
                            this.splitBuy = amountLong; // provides for return of capital
                            this.splitSecQuantity = valueLong;
                        }
                    }
                    break;
                case MISCEXP: // misc income and expense
                    //possible treatment of dividend payment of short
                    switch (acctType) {
                        case EXPENSE -> this.splitExpense = amountLong;
                        case INCOME -> this.splitIncome = amountLong;
                        case SECURITY -> {
                            this.splitShortSell = amountLong; // provides for reduction
                            this.splitSecQuantity = valueLong;//of capital
                        }
                    }
                    break;
                case DIVIDEND_REINVEST: // income and buy with no net cash effect
                    switch (acctType) {
                        case EXPENSE -> this.splitCommission = amountLong;
                        case INCOME -> this.splitIncome = amountLong;
                        case SECURITY -> {
                            this.splitBuy = amountLong;
                            this.splitSecQuantity = valueLong;
                        }
                    }
            } // end txnType Switch Statement
        } // end splitValues Constructor
    } // end splitValues subClass

    private boolean isInvestmentExpense(SplitTxn split) {
        return BulkSecInfo.getReportConfig().getInvestmentExpenseIds().contains(split.getAccount().getUUID());
    }

    private boolean isInvestmentIncome(SplitTxn split) {
        return BulkSecInfo.getReportConfig().getInvestmentIncomeIds().contains(split.getAccount().getUUID());
    }
} // end TransactionValues Class


