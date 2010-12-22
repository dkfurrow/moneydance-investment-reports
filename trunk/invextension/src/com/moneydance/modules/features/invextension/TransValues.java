/*
 *  SecReportPanel.java
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
import com.moneydance.apps.md.model.ParentTxn;
import com.moneydance.apps.md.model.SplitTxn;
import com.moneydance.apps.md.model.TxnUtil;

/** produces basic transaction data
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public class TransValues implements Comparable<TransValues> {

    ParentTxn parent;       //parent account
    Account accountRef;     // reference account (to determine correct sign for transfers)
    Account secAccount;     //security account
    Integer dateint;        //transaction date
    Long txnID;             //transaction ID
    double buy;             //buy amount
    double sell;            //sell amount
    double shortSell;       //short sell amount
    double coverShort;      //cover short amount
    double commision;       //commission amount
    double income;          //income amount
    double expense;         //expense amount
    double transfer;        //transfer amount
    double cashEffect;      //net cash effect on Investment Account
    double secQuantity;     //security quantitiy

    /**
     * generates values in appropriate categories for each Parent transaction
     * constructed at Parent by adding relevant split values for each split
     * @param thisParent Parent Transaction
     * @param accountRef Investment Account associated with Security or bank transaction
     */
    public TransValues(ParentTxn thisParent, Account accountRef) {
        //intitalize values
        this.parent = thisParent;
        this.accountRef = accountRef;
        this.secAccount = accountRef;
        this.dateint = Integer.valueOf(thisParent.getDateInt());
        this.txnID = Long.valueOf(thisParent.getTxnId());
        this.buy = 0;
        this.sell = 0;
        this.shortSell = 0;
        this.coverShort = 0;
        this.commision = 0;
        this.income = 0;
        this.expense = 0;
        this.transfer = 0;
        this.cashEffect = 0;
        this.secQuantity = 0;
        //iterate through splits
        for (int i = 0; i < parent.getSplitCount(); i++) {

            SplitValues thisSplit = new SplitValues(parent.getSplit(i),
                    accountRef.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT
                    ? accountRef : accountRef.getParentAccount());

            this.buy = this.buy + thisSplit.buy;
            this.sell = this.sell + thisSplit.sell;
            this.shortSell = this.shortSell + thisSplit.shortSell;
            this.coverShort = this.coverShort + thisSplit.coverShort;
            this.commision = this.commision + thisSplit.commision;
            this.income = this.income + thisSplit.income;
            this.expense = this.expense + thisSplit.expense;
            this.transfer = this.transfer + thisSplit.transfer;
            this.cashEffect = this.cashEffect + thisSplit.cashEffect;
            this.secQuantity = this.secQuantity + thisSplit.secQuantity;

        }

    }

    
    public int compareTo(TransValues transValues) {
        // sort by date, then by date if like transaction types
        //then make a custom order based on transtype
        // to ensure that buys will precede sells, shorts before covers.
        Integer dateCmp = dateint.compareTo(transValues.dateint);
        Long txnID = transValues.parent.getTxnId();
        Integer transType = TxnUtil.getInvstTxnType(parent);
        if (dateCmp == 0) {
            if (TxnUtil.getInvstTxnType(parent) == transType) {
                return Long.valueOf(parent.getTxnId()).compareTo(txnID);
            } else {
                return getTxnSortOrder().compareTo(transValues.getTxnSortOrder());
            }
        } else {
            return dateCmp;
        }
    }

    public Integer getTxnSortOrder() {
        Integer transType = TxnUtil.getInvstTxnType(parent);
        Integer txnOrder = 0;
        switch (transType) {
            case TxnUtil.TXN_TYPE_BUY:
                txnOrder = 0;
                break;
            case TxnUtil.TXN_TYPE_BUY_XFER:
                txnOrder = 1;
                break;
            case TxnUtil.TXN_TYPE_DIVIDEND_REINVEST:
                txnOrder = 2;
                break;
            case TxnUtil.TXN_TYPE_SELL:
                txnOrder = 3;
                break;
            case TxnUtil.TXN_TYPE_SELL_XFER:
                txnOrder = 4;
                break;
            case TxnUtil.TXN_TYPE_SHORT:
                txnOrder = 5;
                break;
            case TxnUtil.TXN_TYPE_COVER:
                txnOrder = 6;
                break;
            case TxnUtil.TXN_TYPE_MISCINC:
                txnOrder = 7;
                break;
            case TxnUtil.TXN_TYPE_MISCEXP:
                txnOrder = 8;
                break;
            case TxnUtil.TXN_TYPE_DIVIDEND:
                txnOrder = 9;
                break;
            case TxnUtil.TXN_TYPE_DIVIDENDXFR:
                txnOrder = 10;
                break;
            case TxnUtil.TXN_TYPE_BANK:
                txnOrder = 11;
                break;
        }
        return txnOrder;
    }

    /**
     * determines buy/sell/income, etc values for split based upon
     * parent transaction type.  variable names are same as TransValues
     */
    private class SplitValues {

        SplitTxn split;
        Account accountRef;
        int dateint;
        double buy;
        double sell;
        double shortSell;
        double coverShort;
        double commision;
        double income;
        double expense;
        double transfer;
        double cashEffect;
        double secQuantity;

        public SplitValues(SplitTxn thisSplit, Account accountRef) {
            this.split = thisSplit;
            this.accountRef = accountRef;
            this.dateint = thisSplit.getDateInt();
            int txnType = TxnUtil.getInvstTxnType(thisSplit.getParentTxn());
            int acctType = thisSplit.getAccount().getAccountType();
            int parentAcctType = thisSplit.getParentTxn().getAccount().getAccountType();
            Long amountLong = thisSplit.getAmount();
            double amountDouble = (Double.valueOf(amountLong.toString())) / 100;
            Long valueLong = thisSplit.getValue();
            double valueDouble = (Double.valueOf(valueLong.toString())) / 10000;


            this.buy = 0;
            this.sell = 0;
            this.shortSell = 0;
            this.coverShort = 0;
            this.commision = 0;
            this.income = 0;
            this.expense = 0;
            this.transfer = 0;
            this.cashEffect = 0;
            this.secQuantity = 0;

            /*goes through each transaction type, assigns values for each variable based
             on indicated transaction type and account type*/
            switch (txnType) {
                case TxnUtil.TXN_TYPE_BUY://consists of buy, commission, and (potentially) transfer
                case TxnUtil.TXN_TYPE_BUY_XFER: //no net cash effect (transfer offsets buy)
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.buy = amountDouble;
                            this.secQuantity = valueDouble;
                            break;
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.commision = amountDouble;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.income = amountDouble;
                            break;
                        default:
                            this.transfer = split.getAccount() == accountRef
                                    ? -amountDouble : amountDouble;
                            break;
                    }
                    this.cashEffect = this.buy + this.commision
                            + this.income + this.transfer;
                    break;
                case TxnUtil.TXN_TYPE_SELL://consists of sell, commission, and (potentially) transfer
                case TxnUtil.TXN_TYPE_SELL_XFER: //no net cash effect (transfer offsets sell)
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.sell = amountDouble;
                            this.secQuantity = valueDouble;
                            break;
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.commision = amountDouble;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.income = amountDouble;
                            break;
                        default:
                            this.transfer = split.getAccount() == accountRef
                                    ? -amountDouble : amountDouble;
                            break;
                    }
                    this.cashEffect = this.sell + this.commision
                            + this.income + this.transfer;
                    break;
                case TxnUtil.TXN_TYPE_BANK: //Account-level transfers, interest, and expenses
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_EXPENSE:// Only count if parent is investment
                            if (parentAcctType == Account.ACCOUNT_TYPE_INVESTMENT) {
                                this.expense = amountDouble;
                                this.cashEffect = amountDouble;
                            }
                            break;
                        case Account.ACCOUNT_TYPE_INCOME: // Only count if parent is investment
                            if (parentAcctType == Account.ACCOUNT_TYPE_INVESTMENT) {
                                this.income = amountDouble;
                                this.cashEffect = amountDouble;
                            }
                            break;
                        //next cases cover transfer between Assets/Investments, Bank.
                        case Account.ACCOUNT_TYPE_INVESTMENT:
                        case Account.ACCOUNT_TYPE_BANK:
                        case Account.ACCOUNT_TYPE_ASSET:
                        case Account.ACCOUNT_TYPE_LIABILITY:
                            if (split.getAccount() == accountRef) {
                                this.transfer = -amountDouble;
                                this.cashEffect = -amountDouble;
                            } else {
                                this.transfer = amountDouble;
                                this.cashEffect = amountDouble;
                            }
                            break;
                    }  
                    break;
                case TxnUtil.TXN_TYPE_DIVIDEND:
                case TxnUtil.TXN_TYPE_DIVIDENDXFR: //income/expense transactions
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.expense = amountDouble;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.income = amountDouble;
                            break;
                        default:
                            this.transfer = split.getAccount() == accountRef
                                    ? -amountDouble : amountDouble;
                            break;
                    }
                    this.cashEffect = this.expense + this.income + this.transfer;
                    break;
                case TxnUtil.TXN_TYPE_SHORT: //short sales + commission
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.shortSell = amountDouble;
                            this.secQuantity = valueDouble;
                            break;
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.commision = amountDouble;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.income = amountDouble;
                            break;
                    }
                    this.cashEffect = this.shortSell + this.commision + this.income;
                    break;
                case TxnUtil.TXN_TYPE_COVER://short covers + commission
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.coverShort = amountDouble;
                            this.secQuantity = valueDouble;
                            break;
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.commision = amountDouble;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.income = amountDouble;
                            break;
                    }
                    this.cashEffect = this.coverShort + this.commision + this.income;
                    break;
                case TxnUtil.TXN_TYPE_MISCINC: //misc income and expense
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.expense = amountDouble;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.income = amountDouble;
                            break;
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.buy = amountDouble; //provides for return of capital
                            break;
                    }
                    this.cashEffect = amountDouble;
                    break;
                case TxnUtil.TXN_TYPE_MISCEXP: //misc income and expense
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.expense = amountDouble;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.income = amountDouble;
                            break;
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.shortSell = amountDouble; //provides for adjustment of short basis
                            break;
                    }
                    this.cashEffect = amountDouble;
                    break;
                case TxnUtil.TXN_TYPE_DIVIDEND_REINVEST: //income and buy with no net cash effect
                    switch (acctType) {
                        case Account.ACCOUNT_TYPE_EXPENSE:
                            this.commision = amountDouble;
                            break;
                        case Account.ACCOUNT_TYPE_INCOME:
                            this.income = amountDouble;
                            break;
                        case Account.ACCOUNT_TYPE_SECURITY:
                            this.buy = amountDouble;
                            this.secQuantity = valueDouble;
                            break;
                    }
                    this.cashEffect = this.buy + this.commision
                            + this.income;
            } // end txnType Switch Statement
        } // end splitValues Contstructor
    } // end splitValues subClass
} // end TransValues Class

//unused methods
// <editor-fold defaultstate="collapsed" desc="comment">
/*
public static StringBuffer listTransValues(TransValues txnValues) {
StringBuffer txnInfo = new StringBuffer();
txnInfo.append("ParentValueAccountRef; " + txnValues.accountRef.getAccountName()
+ "(Num " + txnValues.accountRef.getAccountNum() + "),");
txnInfo.append("TxnNum; " + txnValues.parent.getTxnId() + ",");
txnInfo.append("Date;");
txnInfo.append(txnValues.dateint + ",");
txnInfo.append("TxnType;");
txnInfo.append(TxnUtil.getInvstTxnType(txnValues.parent) + ",");
txnInfo.append("Desc;");
txnInfo.append(txnValues.parent.getDescription() + ",");
txnInfo.append("Buy;");
txnInfo.append(txnValues.buy + ",");
txnInfo.append("Sell;");
txnInfo.append(txnValues.sell + ",");
txnInfo.append("Short;");
txnInfo.append(txnValues.shortSell + ",");
txnInfo.append("Cover;");
txnInfo.append(txnValues.coverShort + ",");
txnInfo.append("Commison;");
txnInfo.append(txnValues.commision + ",");
txnInfo.append("Income;");
txnInfo.append(txnValues.income + ",");
txnInfo.append("Expense;");
txnInfo.append(txnValues.expense + ",");
txnInfo.append("transfer;");
txnInfo.append(txnValues.transfer + ",");
txnInfo.append("cashEffect;");
txnInfo.append(txnValues.cashEffect + ", ");
txnInfo.append("secQuantity;");
txnInfo.append(txnValues.secQuantity + ", ");
return txnInfo;
}

 */// </editor-fold>

