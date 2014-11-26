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


import com.moneydance.apps.md.model.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.logging.Level;

import java.math.BigDecimal;


/**
 * produces basic transaction data
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
public class TransactionValues implements Comparable<TransactionValues> {

    // cumulative total gain after completion of transaction
    public BigDecimal cumTotalGain;
    private ParentTxn parentTxn; // parentTxn account
    // reference account (to determine correct sign for transfers)
    private Account referenceAccount;
    private SecurityAccountWrapper securityAccountWrapper;
    private Integer dateint; // transaction date
    private double txnID; // transaction ID
    static Comparator<TransactionValues> transComp = new Comparator<TransactionValues>() {
        @Override
        public int compare(TransactionValues t1, TransactionValues t2) {
            Integer d1 = t1.dateint;
            Integer d2 = t2.dateint;
            Double id1 = t1.txnID;
            Double id2 = t2.txnID;
            Integer assocAcctNum1 = t1.referenceAccount.getAccountNum();
            Integer assocAcctNum2 = t1.referenceAccount.getAccountNum();
            Integer transTypeSort1 = t1.getTxnSortOrder();
            Integer transTypeSort2 = t2.getTxnSortOrder();

            if (d1.compareTo(d2) != 0) {// different dates
                return d1.compareTo(d2); // return date order
            } else { // same date
                // if Associated Accounts are different, sort Acct Nums
                if (assocAcctNum1.compareTo(assocAcctNum2) != 0) {
                    return assocAcctNum1.compareTo(assocAcctNum2);
                } else {
                    // if transaction types are different, sort on custom order
                    if (transTypeSort1.compareTo(transTypeSort2) != 0) {
                        return transTypeSort1.compareTo(transTypeSort2);
                    } else { // sort on transIDs
                        return id1.compareTo(id2);
                    } // end transIDs order
                }// end custom order
            } // end date order
        }
    }; // end inner class
    private String desc; // transaction description
    private BigDecimal buy; // buy amount
    private BigDecimal sell; // sell amount
    private BigDecimal shortSell; // short sell amount
    private BigDecimal coverShort; // cover short amount
    private BigDecimal commission; // commission amount
    private BigDecimal income; // income amount
    private BigDecimal expense; // expense amount
    private BigDecimal transfer; // transfer amount
    private BigDecimal secQuantity; // security quantity
    // net position after completion of transaction
    private BigDecimal position;
    // market price on close of transaction day
    private BigDecimal mktPrice;
    // net average cost BigDecimal basis after completion of transaction
    private BigDecimal longBasis;
    // net average cost short basis after completion of transaction
    private BigDecimal shortBasis;
    // net open value after completion of transaction
    private BigDecimal openValue;
    // net cumulative unrealized gains after completion of transaction
    private BigDecimal cumUnrealizedGain;
    // period unrealized gain (one transaction to next) after completion of
    // transaction
    private BigDecimal perUnrealizedGain;
    // period realized gain (one transaction to next) after completion of
    // transaction
    private BigDecimal perRealizedGain;
    // period income and expense gain (one transaction to next) after completion
    // of transaction
    private BigDecimal perIncomeExpense;
    // period total gain (one transaction to next) after completion of
    // transaction
    private BigDecimal perTotalGain;

    /**
     * Constructor to create a cash transaction from the Investment Account
     * initial balance
     *
     * @param invAcctWrapper Investment Account
     * @throws Exception
     */
    public TransactionValues(InvestmentAccountWrapper invAcctWrapper, int firstDateInt)
            throws Exception {
        // copy base values from Security Transaction
        String memo = "Inserted for Initial Balance: "
                + invAcctWrapper.getInvestmentAccount().getAccountName();
        this.securityAccountWrapper = invAcctWrapper.getCashAccountWrapper();
        this.parentTxn = new ParentTxn(firstDateInt, firstDateInt,
                System.currentTimeMillis(), "",
                invAcctWrapper.getCashAccountWrapper().getSecurityAccount(), memo, memo,
                BulkSecInfo.getNextTxnNumber(), AbstractTxn.STATUS_UNRECONCILED);
        this.txnID = BulkSecInfo.getNextTxnNumber();
        BulkSecInfo.setNextTxnNumber(BulkSecInfo.getNextTxnNumber() + 1L);

        this.referenceAccount = invAcctWrapper.getCashAccountWrapper().getSecurityAccount();
        this.dateint = firstDateInt;

        this.desc = this.parentTxn.getDescription();
        this.mktPrice = BigDecimal.ONE; 

        BigDecimal initBal = BigDecimal.valueOf(invAcctWrapper.getInvestmentAccount().getStartBalance())
                .movePointLeft(SecurityReport.moneyScale);

        this.buy = BigDecimal.ZERO;
        this.sell = BigDecimal.ZERO;
        this.shortSell = BigDecimal.ZERO;
        this.coverShort = BigDecimal.ZERO;
        this.commission = BigDecimal.ZERO;
        this.income = BigDecimal.ZERO;
        this.expense = BigDecimal.ZERO;
        this.transfer = BigDecimal.ZERO;
        this.secQuantity = BigDecimal.ZERO;

        if (initBal.compareTo(BigDecimal.ZERO) > 0) {
            this.buy = initBal.negate();
        }
        if (initBal.compareTo(BigDecimal.ZERO) < 0) {
            this.shortSell = initBal.negate();
        }
        this.secQuantity = this.buy.negate().subtract(this.coverShort.subtract(this.sell.subtract(this.shortSell)));

        this.position = this.secQuantity;
        this.longBasis = this.position.max(BigDecimal.ZERO);
        this.shortBasis = this.position.max(BigDecimal.ZERO);
        // OpenValue
        this.openValue = this.position.multiply(this.mktPrice)
                .setScale(SecurityReport.moneyScale, BigDecimal.ROUND_HALF_EVEN);
        // mkt price is always 1, so no realized/unrealized gains
        this.cumUnrealizedGain = BigDecimal.ZERO;
        this.perUnrealizedGain = BigDecimal.ZERO;
        this.perRealizedGain = BigDecimal.ZERO;
        // other fields derive
        this.perIncomeExpense = this.income.add(this.expense);
        this.perTotalGain = this.perUnrealizedGain.add(this.perRealizedGain.add(this.perIncomeExpense));
        this.cumTotalGain = BigDecimal.ZERO;
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
                             ArrayList<TransactionValues> prevTransLines, BulkSecInfo currentInfo) throws Exception {
        //intitalize values
        this.parentTxn = thisParentTxn;
        this.referenceAccount = referenceAccount;
        this.securityAccountWrapper = securityAccountWrapper;
        this.dateint = thisParentTxn.getDateInt();
        this.txnID = thisParentTxn.getTxnId();
        this.desc = thisParentTxn.getDescription();
        this.buy = BigDecimal.ZERO;
        this.sell = BigDecimal.ZERO;
        this.shortSell = BigDecimal.ZERO;
        this.coverShort = BigDecimal.ZERO;
        this.commission = BigDecimal.ZERO;
        this.income = BigDecimal.ZERO;
        this.expense = BigDecimal.ZERO;
        this.transfer = BigDecimal.ZERO;
        this.secQuantity = BigDecimal.ZERO;
        this.position = BigDecimal.ZERO;
        this.mktPrice = BigDecimal.ZERO;
        try {
            //iterate through splits
            for (int i = 0; i < parentTxn.getSplitCount(); i++) {
                //gets values for each split.  Account Reference is parentTxn
                //account in the case of a security, investment account
                //(i.e. itself) in the case of an investment account.
                SplitValues thisSplit = new SplitValues(parentTxn.getSplit(i),
                        referenceAccount.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT
                                ? referenceAccount : referenceAccount.getParentAccount());

                this.buy = this.buy.add(thisSplit.splitBuy);
                this.sell = this.sell.add(thisSplit.splitSell);
                this.shortSell = this.shortSell.add(thisSplit.splitShortSell);
                this.coverShort = this.coverShort.add(thisSplit.splitCoverShort);
                this.commission = this.commission.add(thisSplit.splitCommision);
                this.income = this.income.add(thisSplit.splitIncome);
                this.expense = this.expense.add(thisSplit.splitExpense);
                this.transfer = this.transfer.add(thisSplit.splitTransfer);
                this.secQuantity = this.secQuantity.add(thisSplit.splitSecQuantity);
            }

            //fill in rest of transValues
            TransactionValues prevTransLine = prevTransLines.isEmpty() ? null :
                    prevTransLines.get(prevTransLines.size() - 1);
            CurrencyType cur = this.referenceAccount.getCurrencyType();
            int currentDateInt = this.parentTxn.getDateInt();
            double currentRate = cur == null ? 1.0 : cur
                    .getUserRateByDateInt(currentDateInt);
            int prevDateInt = prevTransLine == null ? Integer.MIN_VALUE
                    : prevTransLine.parentTxn.getDateInt();
            double splitAdjust = (cur == null ? 1.0 : cur.adjustRateForSplitsInt(
                    prevDateInt, currentRate, currentDateInt) / currentRate);
            BigDecimal adjPrevPos = prevTransLine == null ? BigDecimal.ZERO
                : prevTransLine.position.multiply(BigDecimal.valueOf(splitAdjust))
                    .setScale(SecurityReport.quantityScale, BigDecimal.ROUND_HALF_EVEN);
            BigDecimal adjPrevMktPrc = prevTransLine == null ? BigDecimal.ZERO
                : prevTransLine.mktPrice.divide(BigDecimal.valueOf(splitAdjust), BigDecimal.ROUND_HALF_EVEN)
                    .setScale(SecurityReport.moneyScale, BigDecimal.ROUND_HALF_EVEN);
            // mktPrice (Set to 1 if cur is null: Implies (Cash) Investment Account
            this.mktPrice = (cur == null ? BigDecimal.ONE
                    : BigDecimal.ONE.divide(BigDecimal.valueOf(cur.getUserRateByDateInt(currentDateInt)), BigDecimal.ROUND_HALF_EVEN)
                    .setScale(SecurityReport.moneyScale, BigDecimal.ROUND_HALF_EVEN));

            // position
            if (prevTransLine == null) { // first transaction (buy || shortSell)
                // if first transaction improper, throw an exception
                InvestTxnType transactionType = TxnUtil.getInvestTxnType(parentTxn);
                boolean validStartTransaction = (transactionType == InvestTxnType.BUY ||
                        transactionType == InvestTxnType.BUY_XFER || transactionType == InvestTxnType.SHORT);
                if (!validStartTransaction && securityAccountWrapper.isTradeable()) {
                    throwInitialTransactionException(transactionType);
                }
                this.position = this.secQuantity;
            } else { // subsequent transaction
                if(securityAccountWrapper.isTradeable()) testSubsequentTransaction(TxnUtil.getInvestTxnType(parentTxn),
                        secQuantity, adjPrevPos);
                this.position = this.secQuantity.multiply(adjPrevPos)
                        .setScale(SecurityReport.moneyScale, BigDecimal.ROUND_HALF_EVEN);
            }


            //get long and short basis
            GainsCalc gainsCalc = currentInfo.getGainsCalc();
            gainsCalc.initializeGainsCalc(currentInfo, this, prevTransLines);
            this.longBasis = gainsCalc.getLongBasis();
            this.shortBasis = gainsCalc.getShortBasis();


            // OpenValue
            this.openValue = this.position.multiply(this.mktPrice);

            // cumulative unrealized gains
            if (this.position.compareTo(BigDecimal.ZERO) > 0) {
                this.cumUnrealizedGain = this.openValue.subtract(this.longBasis);
            } else if (this.position.compareTo(BigDecimal.ZERO) < 0) {
                this.cumUnrealizedGain = this.openValue.subtract(this.shortBasis);
            } else {
                this.cumUnrealizedGain = BigDecimal.ZERO;
            }

            // period unrealized gains
            if (this.position.compareTo(BigDecimal.ZERO) == 0) {
                this.perUnrealizedGain = BigDecimal.ZERO;
            } else {
                if (this.secQuantity.compareTo(BigDecimal.ZERO) == 0) {
                    // income/expense transaction, period gain is
                    // change in cum unreal gains
                    this.perUnrealizedGain = this.cumUnrealizedGain.subtract
                            (prevTransLine == null ? BigDecimal.ZERO
                            : prevTransLine.cumUnrealizedGain);
                } else {// buy, sell, short, or cover transaction
                    // first case, add to long or add to short
                    // change in cumulative gains accounts for trans quantity
                    if (this.secQuantity.multiply(this.position).compareTo(BigDecimal.ZERO) > 0) {
                        this.perUnrealizedGain = this.cumUnrealizedGain.subtract
                                (prevTransLine == null ? BigDecimal.ZERO
                                : prevTransLine.cumUnrealizedGain);
                    } else { // reduce long or short
                        // unrealized gains equal 0 on position-closing
                        // transaction
                        this.perUnrealizedGain = this.position.multiply(this.mktPrice.subtract(adjPrevMktPrc));
                    }
                }
            }

            // Period Realized gains
            if (this.sell.compareTo(BigDecimal.ZERO) > 0) { // sale transaction
                if (prevTransLine != null) {
                    this.perRealizedGain = this.sell.add(this.commission).add(this.longBasis.subtract(prevTransLine.longBasis));
                } else {
                    throw new Exception(securityAccountWrapper.getName() + " : SELL/SELLXFER cannot be first transaction: ");

                }
            } else if (this.coverShort.compareTo(BigDecimal.ZERO) < 0) { // cover transaction
                if (prevTransLine != null) {
                    this.perRealizedGain = this.coverShort.add(this.commission).add(this.shortBasis.subtract(prevTransLine.shortBasis));
                } else {
                    throw new Exception(securityAccountWrapper.getName() + " : COVER cannot be first transaction: ");
                }
            } else {
                // implies for closed pos, cumUnrealized-cumRealized =
                // commission (on last trade)
                this.perRealizedGain = BigDecimal.ZERO;
            }


            // period income/expense
            this.perIncomeExpense = this.income.add(this.expense);


            // period total gain
            this.perTotalGain = this.perUnrealizedGain.add(this.perRealizedGain);

            // cumulative total gain
            this.cumTotalGain = prevTransLine == null ? this.perTotalGain :
                    this.perTotalGain.add(prevTransLine.cumTotalGain);
        } catch (Exception e) {
            String dateString = " Date: " + DateUtils.convertToShort(dateint);
            String errorString = "Error in transaction values calculation: " + securityAccountWrapper.getInvAcctWrapper().getName() +
                    " Security: " + securityAccountWrapper.getName() + dateString;
            LogController.logException(e, errorString);
        }


    }

    /**
     * Generic Constructor for TransactionValues which are inserted into
     * synthetically created cash account
     *
     * @param transactionValues TransactionValues from Investment or Security Account
     * @param prevTransValues   previous TransactionValues (to get position)
     * @param invAcctWrapper    Investment Account
     * @throws Exception
     */
    public TransactionValues(TransactionValues transactionValues, TransactionValues prevTransValues,
                             InvestmentAccountWrapper invAcctWrapper) throws Exception {
        // copy base values from Security Transaction
        this.parentTxn = transactionValues.parentTxn;
        this.referenceAccount = invAcctWrapper.getCashAccountWrapper().getSecurityAccount();
        this.securityAccountWrapper = prevTransValues.getSecurityAccountWrapper();
        this.dateint = transactionValues.dateint;
        // adding 0.1 to related transValues id to ensure unique cash id
        this.txnID = TxnUtil.getInvestTxnType(transactionValues.parentTxn) ==
                InvestTxnType.BANK ? transactionValues.parentTxn.getTxnId() :
                transactionValues.parentTxn.getTxnId() + 0.1;
        this.desc = "INSERTED: " + parentTxn.getDescription();
        this.mktPrice = BigDecimal.ONE;

        BigDecimal thisTransfer = transactionValues.transfer;
        BigDecimal acctEntry = transactionValues.buy.negate().subtract(transactionValues.coverShort)
                .subtract(transactionValues.sell).subtract(transactionValues.shortSell)
                .subtract(transactionValues.income).subtract(transactionValues.expense)
                .subtract(transactionValues.commission);
        BigDecimal prevPos = prevTransValues.position;

        this.buy = BigDecimal.ZERO;
        this.sell = BigDecimal.ZERO;
        this.shortSell = BigDecimal.ZERO;
        this.coverShort = BigDecimal.ZERO;
        this.commission = BigDecimal.ZERO;
        this.income = BigDecimal.ZERO;
        this.expense = BigDecimal.ZERO;
        this.transfer = BigDecimal.ZERO;
        this.secQuantity = BigDecimal.ZERO;
        InvestTxnType txnType = TxnUtil.getInvestTxnType(this.parentTxn);

        switch (txnType) {
            case BANK: // transfer in/out, account-level income or expense
                if (thisTransfer.compareTo(BigDecimal.ZERO) > 0) {// transfer in
                    if (prevPos.compareTo(BigDecimal.ZERO) < 0) {
                        this.coverShort = thisTransfer.negate().max(prevPos);
                        this.buy = thisTransfer.negate().subtract(prevPos).min(BigDecimal.ZERO);
                    } else {
                        this.buy = thisTransfer.negate();
                    }
                } else if (thisTransfer.compareTo(BigDecimal.ZERO) < 0) {// transfer out
                    if (prevPos.compareTo(BigDecimal.ZERO) > 0) {
                        this.sell = thisTransfer.negate().min(prevPos);
                        this.shortSell = thisTransfer.negate().subtract(prevPos).max(BigDecimal.ZERO);
                    } else {
                        this.shortSell = thisTransfer.negate();
                    }
                } else { // income or expense
                    if (acctEntry.compareTo(BigDecimal.ZERO) <= 0) {// Account level Income
                        // like dividend/reinvest)
                        if (prevPos.compareTo(BigDecimal.ZERO) < 0) {
                            this.coverShort = acctEntry.max(prevPos);
                            this.buy = acctEntry.subtract(prevPos).min(BigDecimal.ZERO);
                        } else {
                            this.buy = acctEntry;
                        }
                        this.income = acctEntry.negate();
                    } else {// Account level expense
                        // like capital call (debit expense credit security)
                        if (prevPos.compareTo(BigDecimal.ZERO) > 0) {
                            this.sell = acctEntry.min(prevPos);
                            this.shortSell = acctEntry.subtract(prevPos).max(BigDecimal.ZERO);
                        } else {
                            this.shortSell = acctEntry;
                        }
                        this.expense = acctEntry.negate();

                    }
                }
                break;
            case BUY:
            case COVER:
            case MISCEXP:
                if (prevPos.compareTo(BigDecimal.ZERO) > 0) {
                    this.sell = acctEntry.min(prevPos);
                    this.shortSell = acctEntry.subtract(prevPos).max(BigDecimal.ZERO);
                } else {
                    this.shortSell = acctEntry;
                }
                break;
            case SELL:
            case SHORT:
            case MISCINC:
            case DIVIDEND:
                if (prevPos.compareTo(BigDecimal.ZERO) < 0) {
                    this.coverShort = acctEntry.max(prevPos);
                    this.buy = acctEntry.subtract(prevPos).min(BigDecimal.ZERO);
                } else {
                    this.buy = acctEntry;
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

        this.secQuantity = (this.buy.negate().subtract(this.coverShort).subtract(this.sell).subtract(this.shortSell));

        this.position = this.secQuantity.add(prevPos);
        this.longBasis = this.position.max(BigDecimal.ZERO);
        this.shortBasis = this.position.min(BigDecimal.ZERO);;
        // OpenValue
        this.openValue = this.position.multiply(this.mktPrice)
                .setScale(SecurityReport.moneyScale, BigDecimal.ROUND_HALF_EVEN);
        //mkt price is always 1, so no realized/unrealized gains
        this.cumUnrealizedGain = BigDecimal.ZERO;
        this.perUnrealizedGain = BigDecimal.ZERO;
        this.perRealizedGain = BigDecimal.ZERO;
        //other fields derive
        this.perIncomeExpense = this.income.add(this.expense);
        this.perTotalGain = this.perUnrealizedGain.add(this.perRealizedGain).add(this.perIncomeExpense);
        this.cumTotalGain = this.perTotalGain.add(prevTransValues.cumTotalGain);
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
        txnInfo.append("TxnNum " + ",");
        txnInfo.append("Date" + ",");
        txnInfo.append("TxnType" + ",");
        txnInfo.append("Desc" + ",");
        txnInfo.append("Buy" + ",");
        txnInfo.append("Sell" + ",");
        txnInfo.append("Short" + ",");
        txnInfo.append("Cover" + ",");
        txnInfo.append("Commison" + ",");
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


    class InitialTransactionException extends Exception {
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

    private void testSubsequentTransaction(InvestTxnType investTxnType, BigDecimal secQuantity, BigDecimal adjPrevPos) throws Exception {
        String warningStr = "";
        String dateString = " Date: " + DateUtils.convertToShort(dateint);
        BigDecimal thisPosition = this.secQuantity.add(adjPrevPos);
        if (adjPrevPos.compareTo(BigDecimal.ZERO) > 0) {
            if (thisPosition.compareTo(BigDecimal.ZERO) < 0) warningStr = "Error in investment account: " + securityAccountWrapper.getInvAcctWrapper().getName() +
                    " Security: " + securityAccountWrapper.getName() + dateString + " takes position from long to short in one transaction, " +
                    "Please create two transactions--a SELL or SELLXFER to flatten the position, and a separate SHORT " +
                    "transaction to create the final short position";
        } else if (adjPrevPos.compareTo(BigDecimal.ZERO) < 0) {
            if (thisPosition.compareTo(BigDecimal.ZERO) > 0) warningStr = "Error in investment account: " + securityAccountWrapper.getInvAcctWrapper().getName() +
                    " Security: " + securityAccountWrapper.getName() + dateString + " takes position from short to long in one transaction, " +
                    "Please create two transactions--a COVER to flatten the position, and a separate BUY or BUYXFER " +
                    "transaction to create the final long position";

        } else {
            if (secQuantity.compareTo(BigDecimal.ZERO) > 0) {
                boolean validTradeType = (investTxnType == InvestTxnType.BUY
                        || investTxnType == InvestTxnType.BUY_XFER);
                if (!validTradeType)
                    warningStr = "Error in investment account: " + securityAccountWrapper.getInvAcctWrapper().getName() +
                            " Security: " + securityAccountWrapper.getName() + dateString + " takes position from flat to long, " +
                            "so must be a BUY or BUYXFER, " + "but instead is a " + investTxnType.name();
            } else if (secQuantity.compareTo(BigDecimal.ZERO) < 0) {
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
    public BigDecimal getCumTotalGain() {
        return cumTotalGain;
    }

    @SuppressWarnings("unused")
    public BigDecimal getCumUnrealizedGain() {
        return cumUnrealizedGain;
    }

    public Integer getDateint() {
        return dateint;
    }

    @SuppressWarnings("unused")
    public String getDesc() {
        return desc;
    }

    public BigDecimal getLongBasis() {
        return longBasis;
    }

    @SuppressWarnings("unused")
    public BigDecimal getMktPrice() {
        return mktPrice;
    }

    @SuppressWarnings("unused")
    public BigDecimal getOpenValue() {
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
    public BigDecimal getPerIncomeExpense() {
        return perIncomeExpense;
    }

    public BigDecimal getPerRealizedGain() {
        return perRealizedGain;
    }

    @SuppressWarnings("unused")
    public BigDecimal getPerTotalGain() {
        return perTotalGain;
    }

    @SuppressWarnings("unused")
    public BigDecimal getPerUnrealizedGain() {
        return perUnrealizedGain;
    }

    @SuppressWarnings("unused")
    public BigDecimal getPosition() {
        return position;
    }

    public BigDecimal getSecQuantity() {
        return secQuantity;
    }

    public BigDecimal getShortBasis() {
        return shortBasis;
    }

    public BigDecimal getTransfer() {
        return transfer;
    }

    public double getTxnID() {
        return txnID;
    }

    public Account getReferenceAccount() {
        return referenceAccount;
    }

    public String[] listInfo() {
        ArrayList<String> txnInfo = new ArrayList<>();
        InvestTxnType transType = TxnUtil.getInvestTxnType(parentTxn);
        txnInfo.add(referenceAccount.getParentAccount()
                .getAccountName());
        txnInfo.add(referenceAccount.getAccountName());
        txnInfo.add(securityAccountWrapper.getCurrencyWrapper().getTicker() == null ? "NoTicker" : securityAccountWrapper.getCurrencyWrapper().getTicker());
        txnInfo.add(securityAccountWrapper.getDivFrequency().toString());
        txnInfo.add(Double.toString(txnID));
        txnInfo.add(DateUtils.convertToShort(dateint));
        txnInfo.add(transType.toString());
        txnInfo.add(desc);
        txnInfo.add(buy.toString());
        txnInfo.add(sell.toString());
        txnInfo.add(shortSell.toString());
        txnInfo.add(coverShort.toString());
        txnInfo.add(commission.toString());
        txnInfo.add(income.toString());
        txnInfo.add(expense.toString());
        txnInfo.add(transfer.toString());
        txnInfo.add(secQuantity.toString());
        txnInfo.add(mktPrice.toString());
        txnInfo.add(position.toString());
        txnInfo.add(longBasis.toString());
        txnInfo.add(shortBasis.toString());
        txnInfo.add(openValue.toString());
        txnInfo.add(cumUnrealizedGain.toString());
        txnInfo.add(perUnrealizedGain.toString());
        txnInfo.add(perRealizedGain.toString());
        txnInfo.add(perIncomeExpense.toString());
        txnInfo.add(perTotalGain.toString());
        txnInfo.add(cumTotalGain.toString());
        return txnInfo.toArray(new String[txnInfo.size()]);
    }

    /**
     * Custom sort order to put buys before sells, shorts before covers
     *
     * @return custom transaction order based on method
     */
    public Integer getTxnSortOrder() {
        InvestTxnType transType = this.parentTxn != null ?
                TxnUtil.getInvestTxnType(parentTxn) : InvestTxnType.BANK;
        Integer txnOrder = 0;
        switch (transType) {
            case BUY:
                txnOrder = 0;
                break;
            case BUY_XFER:
                txnOrder = 1;
                break;
            case DIVIDEND_REINVEST:
                txnOrder = 2;
                break;
            case SELL:
                txnOrder = 3;
                break;
            case SELL_XFER:
                txnOrder = 4;
                break;
            case SHORT:
                txnOrder = 5;
                break;
            case COVER:
                txnOrder = 6;
                break;
            case MISCINC:
                txnOrder = 7;
                break;
            case MISCEXP:
                txnOrder = 8;
                break;
            case DIVIDEND:
                txnOrder = 9;
                break;
            case DIVIDENDXFR:
                txnOrder = 10;
                break;
            case BANK:
                txnOrder = 11;
                break;
        }
        return txnOrder;
    }

    /**
     * totalFlows are all cash flows (including income/expense)
     * note buys are < 0 by convention for IRR
     *
     * @return total cash effect of transaction
     */
    public BigDecimal getTotalFlows() {
        return getBuy().add(getSell()).add(getShortSell())
                .add(getCoverShort()).add(getCommission()).add(getIncome()).add(getExpense());
    }

    public BigDecimal getBuy() {
        return buy;
    }

    public BigDecimal getCommission() {
        return commission;
    }

    public BigDecimal getCoverShort() {
        return coverShort;
    }

    public BigDecimal getExpense() {
        return expense;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public BigDecimal getSell() {
        return sell;
    }

    public BigDecimal getShortSell() {
        return shortSell;
    }

    /**
     * buySellFlows is net cash effect of buy/sell/short/cover, incl commission
     * note sign convention for modified-dietz calc...buys are > 0
     *
     * @return cash effect of buy/sell/short/cover transaction
     */
    public BigDecimal getBuySellFlows() {
        return (getBuy().add(getSell()).add(getShortSell()).add(getCoverShort()).add(getCommission())).negate();
    }

    /**
     * determines buy/sell/income, etc values for split based upon
     * parentTxn transaction type.  variable names are same as TransactionValues
     */
    private class SplitValues {

        SplitTxn split;
        BigDecimal splitBuy;
        BigDecimal splitSell;
        BigDecimal splitShortSell;
        BigDecimal splitCoverShort;
        BigDecimal splitCommision;
        BigDecimal splitIncome;
        BigDecimal splitExpense;
        BigDecimal splitTransfer;
        BigDecimal splitSecQuantity;

        public SplitValues(SplitTxn thisSplit, Account accountRef) {
            this.split = thisSplit;
            thisSplit.getDateInt();
            InvestTxnType txnType = TxnUtil.getInvestTxnType(thisSplit.getParentTxn());
            int acctType = thisSplit.getAccount().getAccountType();
            int parentAcctType = thisSplit.getParentTxn().getAccount().getAccountType();
            BigDecimal amountLong = BigDecimal.valueOf(thisSplit.getAmount())
                    .movePointLeft(SecurityReport.quantityScale);
            BigDecimal valueLong = BigDecimal.valueOf(thisSplit.getValue())
                    .movePointLeft(SecurityReport.moneyScale);

            this.splitBuy = BigDecimal.ZERO;
            this.splitSell = BigDecimal.ZERO;
            this.splitShortSell = BigDecimal.ZERO;
            this.splitCoverShort = BigDecimal.ZERO;
            this.splitCommision = BigDecimal.ZERO;
            this.splitIncome = BigDecimal.ZERO;
            this.splitExpense = BigDecimal.ZERO;
            this.splitTransfer = BigDecimal.ZERO;
            this.splitSecQuantity = BigDecimal.ZERO;

	    /*
         * goes through each transaction type, assigns values for each
	     * variable based on indicated transaction type and account type
	     */
            switch (txnType) {
                case BUY:// consists of buy, commission, and (potentially) transfer
                case BUY_XFER: // no net cash effect (transfer offsets buy)
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.splitBuy = amountLong;
                            this.splitSecQuantity = valueLong;
                            break;
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.splitCommision = amountLong;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.splitIncome = amountLong;
                            break;
                        default:
                            this.splitTransfer = split.getAccount() == accountRef
                                    ? amountLong.negate() : amountLong;
                            break;
                    }
                    break;
                case SELL:// consists of sell, commission, and (potentially)
                    // transfer
                case SELL_XFER: // no net cash effect (transfer offsets sell)
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.splitSell = amountLong;
                            this.splitSecQuantity = valueLong;
                            break;
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.splitCommision = amountLong;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.splitIncome = amountLong;
                            break;
                        default:
                            this.splitTransfer = split.getAccount() == accountRef
                                    ? amountLong.negate() : amountLong;
                            break;
                    }
                    break;
                case BANK: // Account-level transfers, interest, and expenses
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_EXPENSE:// Only count if parentTxn is
                            // investment
                            if (parentAcctType == Account.ACCOUNT_TYPE_INVESTMENT) {
                                this.splitExpense = amountLong;
                            }
                            break;
                        case Account.ACCOUNT_TYPE_INCOME: // Only count if parentTxn is
                            // investment
                            if (parentAcctType == Account.ACCOUNT_TYPE_INVESTMENT) {
                                this.splitIncome = amountLong;
                            }
                            break;
                        // next cases cover transfer between Assets/Investments, Bank.
                        case Account.ACCOUNT_TYPE_INVESTMENT:
                        case Account.ACCOUNT_TYPE_BANK:
                        case Account.ACCOUNT_TYPE_ASSET:
                        case Account.ACCOUNT_TYPE_LIABILITY:
                            if (split.getAccount() == accountRef) {
                                this.splitTransfer = amountLong.negate();
                            } else {
                                this.splitTransfer = amountLong;
                            }
                            break;
                    }
                    break;
                case DIVIDEND:
                case DIVIDENDXFR: // income/expense transactions
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.splitExpense = amountLong;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.splitIncome = amountLong;
                            break;
                        default:
                            this.splitTransfer = split.getAccount() == accountRef ?
                                    amountLong.negate() : amountLong;
                            break;
                    }
                    break;
                case SHORT: // short sales + commission
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.splitShortSell = amountLong;
                            this.splitSecQuantity = valueLong;
                            break;
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.splitCommision = amountLong;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.splitIncome = amountLong;
                            break;
                    }
                    break;
                case COVER:// short covers + commission
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.splitCoverShort = amountLong;
                            this.splitSecQuantity = valueLong;
                            break;
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.splitCommision = amountLong;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.splitIncome = amountLong;
                            break;
                    }
                    break;
                case MISCINC: // misc income and expense
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.splitExpense = amountLong;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.splitIncome = amountLong;
                            break;
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.splitBuy = amountLong; // provides for return of capital
                            this.splitSecQuantity = valueLong;
                            break;
                    }
                    break;
                case MISCEXP: // misc income and expense
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.splitExpense = amountLong;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.splitIncome = amountLong;
                            break;
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.splitShortSell = amountLong; // provides for reduction
                            this.splitSecQuantity = valueLong;//of capital
                            //possible treatment of dividend payment of short
                            break;
                    }
                    break;
                case DIVIDEND_REINVEST: // income and buy with no net cash effect
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.splitCommision = amountLong;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.splitIncome = amountLong;
                            break;
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.splitBuy = amountLong;
                            this.splitSecQuantity = valueLong;
                            break;
                    }
            } // end txnType Switch Statement
        } // end splitValues Constructor
    } // end splitValues subClass

} // end TransactionValues Class


