/* TransValues.java
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


import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.InvestTxnType;
import com.moneydance.apps.md.model.ParentTxn;
import com.moneydance.apps.md.model.SplitTxn;
import com.moneydance.apps.md.model.TxnUtil;

/** produces basic transaction data
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public class TransValues implements Comparable<TransValues> {

    public ParentTxn parentTxn;       //parentTxn account
    // reference account (to determine correct sign for transfers)
    public Account accountRef;     
    public Account secAccount;     //security account
    public Integer dateint;        //transaction date
    public Long txnID;             //transaction ID
    public double buy;             //buy amount
    public double sell;            //sell amount
    public double shortSell;       //short sell amount
    public double coverShort;      //cover short amount
    public double commision;       //commission amount
    public double income;          //income amount
    public double expense;         //expense amount
    public double transfer;        //transfer amount
    public double cashEffect;      //net cash effect on Investment Account
    public double secQuantity;     //security quantitiy

    //getters and setters (supports use of compiled JAR)
    public ParentTxn getParent() {
        return parentTxn;
    }
    public Account getAccountRef() {
        return accountRef;
    }
    public Account getSecAccount() {
        return secAccount;
    }
    public Integer getDateint() {
        return dateint;
    }
    public Long getTxnID() {
        return txnID;
    }
    public double getBuy() {
        return buy;
    }
    public double getSell() {
        return sell;
    }
    public double getShortSell() {
        return shortSell;
    }
    public double getCoverShort() {
        return coverShort;
    }
    public double getCommision() {
        return commision;
    }
    public double getIncome() {
        return income;
    }
    public double getExpense() {
        return expense;
    }
    public double getTransfer() {
        return transfer;
    }
    public double getCashEffect() {
        return cashEffect;
    }
    public double getSecQuantity() {
        return secQuantity;
    }


    /**
     * generates values in appropriate categories for each Parent transaction
     * constructed at Parent by adding relevant split values for each split
     * @param thisParentTxn Parent Transaction
     * @param accountRef Investment Account associated with Security or bank transaction
     */
    public TransValues(ParentTxn thisParentTxn, Account accountRef) {
        //intitalize values
        this.parentTxn = thisParentTxn;
        this.accountRef = accountRef;
        this.secAccount = accountRef;
        this.dateint = Integer.valueOf(thisParentTxn.getDateInt());
        this.txnID = Long.valueOf(thisParentTxn.getTxnId());
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
        for (int i = 0; i < parentTxn.getSplitCount(); i++) {
            //gets values for each split.  Account Reference is parentTxn 
            //account in the case of a security, investment account
            //(i.e. itself) in the case of an investment account.
            SplitValues thisSplit = new SplitValues(parentTxn.getSplit(i),
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
	// sort by date, then by a custom order based on transtype
	// then by TxnId if like transaction types to ensure that buys
	// will precede sells, shorts before covers.
	Integer dateCmp = this.dateint.compareTo(transValues.dateint);
	Long cmpTxnID = transValues.parentTxn.getTxnId();
	Integer cmpTransType = TxnUtil.getInvstTxnType(transValues.parentTxn);
	if (dateCmp == 0) {// same date
	    // if like trans types, use TxnID
	    if (TxnUtil.getInvstTxnType(this.parentTxn) == cmpTransType) {
		return Long.valueOf(this.parentTxn.getTxnId()).compareTo(cmpTxnID);
	    } else {// use custom sort order
		return this.getTxnSortOrder().compareTo(
			transValues.getTxnSortOrder());
	    }
	} else { // sort by date
	    return dateCmp;
	}
    }

    /**Custom sort order to put buys before sells, shorts before covers
     * @return
     */
    public Integer getTxnSortOrder() {
        InvestTxnType transType = TxnUtil.getInvestTxnType(parentTxn);
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
     * determines buy/sell/income, etc values for split based upon
     * parentTxn transaction type.  variable names are same as TransValues
     */
    private class SplitValues {

	SplitTxn split;
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
	    thisSplit.getDateInt();
	    InvestTxnType txnType = TxnUtil.getInvestTxnType(thisSplit
		    .getParentTxn());
	    int acctType = thisSplit.getAccount().getAccountType();
	    int parentAcctType = thisSplit.getParentTxn().getAccount()
		    .getAccountType();
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

	    /*
	     * goes through each transaction type, assigns values for each
	     * variable based on indicated transaction type and account type
	     */
	    switch (txnType) {
	    case BUY:// consists of buy, commission, and (potentially) transfer
	    case BUY_XFER: // no net cash effect (transfer offsets buy)
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
		this.cashEffect = this.buy + this.commision + this.income
			+ this.transfer;
		break;
	    case SELL:// consists of sell, commission, and (potentially)
		      // transfer
	    case SELL_XFER: // no net cash effect (transfer offsets sell)
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
		this.cashEffect = this.sell + this.commision + this.income
			+ this.transfer;
		break;
	    case BANK: // Account-level transfers, interest, and expenses
		switch (acctType) {
		case Account.ACCOUNT_TYPE_EXPENSE:// Only count if parentTxn is
		     // investment
		    if (parentAcctType == Account.ACCOUNT_TYPE_INVESTMENT) {
			this.expense = amountDouble;
			this.cashEffect = amountDouble;
		    }
		    break;
		case Account.ACCOUNT_TYPE_INCOME: // Only count if parentTxn is
		    // investment
		    if (parentAcctType == Account.ACCOUNT_TYPE_INVESTMENT) {
			this.income = amountDouble;
			this.cashEffect = amountDouble;
		    }
		    break;
		// next cases cover transfer between Assets/Investments, Bank.
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
	    case DIVIDEND:
	    case DIVIDENDXFR: // income/expense transactions
		switch (acctType) {
		case Account.ACCOUNT_TYPE_EXPENSE:
		    this.expense = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_INCOME:
		    this.income = amountDouble;
		    break;
		default:
		    this.transfer = split.getAccount() == accountRef ?
			    -amountDouble : amountDouble;
		    break;
		}
		this.cashEffect = this.expense + this.income + this.transfer;
		break;
	    case SHORT: // short sales + commission
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
	    case COVER:// short covers + commission
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
		this.cashEffect = this.coverShort + this.commision
			+ this.income;
		break;
	    case MISCINC: // misc income and expense
		switch (acctType) {
		case Account.ACCOUNT_TYPE_EXPENSE:
		    this.expense = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_INCOME:
		    this.income = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_SECURITY:
		    this.buy = amountDouble; // provides for return of capital
		    break;
		}
		this.cashEffect = amountDouble;
		break;
	    case MISCEXP: // misc income and expense
		switch (acctType) {
		case Account.ACCOUNT_TYPE_EXPENSE:
		    this.expense = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_INCOME:
		    this.income = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_SECURITY:
		    this.shortSell = amountDouble; // provides for adjustment of
						   // short basis
		    break;
		}
		this.cashEffect = amountDouble;
		break;
	    case DIVIDEND_REINVEST: // income and buy with no net cash effect
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
		this.cashEffect = this.buy + this.commision + this.income;
	    } // end txnType Switch Statement
	} // end splitValues Contstructor
    } // end splitValues subClass
} // end TransValues Class

//unused methods

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

