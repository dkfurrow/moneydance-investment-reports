/* TransactionValues.java
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


import java.util.ArrayList;
import java.util.Comparator;
import java.util.SortedSet;

import com.moneydance.apps.md.model.AbstractTxn;
import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.CurrencyType;
import com.moneydance.apps.md.model.InvestTxnType;
import com.moneydance.apps.md.model.ParentTxn;
import com.moneydance.apps.md.model.SplitTxn;
import com.moneydance.apps.md.model.TxnUtil;

/** produces basic transaction data
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public class TransactionValues implements Comparable<TransactionValues> {

    private ParentTxn parentTxn; // parentTxn account
    // reference account (to determine correct sign for transfers)
    private Account referenceAccount;
    private Integer dateint; // transaction date
    private double txnID; // transaction ID
    private String desc; // transaction description
    private double buy; // buy amount
    private double sell; // sell amount
    private double shortSell; // short sell amount
    private double coverShort; // cover short amount
    private double commision; // commission amount
    private double income; // income amount
    private double expense; // expense amount
    private double transfer; // transfer amount
    private double secQuantity; // security quantity
    // net position after completion of transaction
    private double position;
    // market price on close of transaction day
    private double mktPrice;
    // net average cost long basis after completion of transaction
    private double longBasis;
    // net average cost short basis after completion of transaction
    private double shortBasis;
    // net open value after completion of transaction
    private double openValue;
    // net cumulative unrealized gains after completion of transaction
    private double cumUnrealizedGain;
    // period unrealized gain (one transaction to next) after completion of
    // transaction
    private double perUnrealizedGain;
    // period realized gain (one transaction to next) after completion of
    // transaction
    private double perRealizedGain;
    // period income and expense gain (one transaction to next) after completion
    // of transaction
    private double perIncomeExpense;
    // period total gain (one transaction to next) after completion of
    // transaction
    private double perTotalGain;
    private static final double positionThreshold = 0.0005;

    // cumulative total gain after completion of transaction
    public double cumTotalGain;
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
   
    /**Constructor to create a cash transaction from the Investment Account 
     * initial balance
    * @param transValuesSet TransactionValues from Investment or Security Account
    * @param prevTrans previous TransactionValues (to get position)
    * @param invAcctWrapper Investment Account
     * @throws Exception 
    */
    public TransactionValues(InvestmentAccountWrapper invAcctWrapper, int firstDateInt)
	    throws Exception {
	// copy base values from Security Transaction
	String memo = "Inserted for Inital Balance: "
		+ invAcctWrapper.getInvestmentAccount().getAccountName();
	this.parentTxn = new ParentTxn(firstDateInt, firstDateInt,
		System.currentTimeMillis(), "",
		invAcctWrapper.getCashAccountWrapper().getSecurityAccountWrapper(), memo, memo,
		BulkSecInfo.getNextTxnNumber(), AbstractTxn.STATUS_UNRECONCILED);
	this.txnID = BulkSecInfo.getNextTxnNumber();
	BulkSecInfo.setNextTxnNumber(BulkSecInfo.getNextTxnNumber() + 1L);

	this.referenceAccount = invAcctWrapper.getCashAccountWrapper().getSecurityAccountWrapper();
	this.dateint = firstDateInt;

	this.desc = this.parentTxn.getDescription();
	this.mktPrice = 1.0;

	this.transfer = 0.0;

	Double initBal = Double.valueOf(invAcctWrapper.getInvestmentAccount()
		.getStartBalance()) / 100.0;

	this.buy = 0.0;
	this.sell = 0.0;
	this.shortSell = 0.0;
	this.coverShort = 0.0;
	this.commision = 0.0;
	this.income = 0.0;
	this.expense = 0.0;
	this.secQuantity = 0.0;

	if (initBal > 0.0) {
	    this.buy = -initBal;
	}
	if (initBal < 0.0) {
	    this.shortSell = -initBal;
	}
	this.secQuantity = -this.buy - this.coverShort - this.sell
		- this.shortSell;

	this.position = this.secQuantity;
	this.longBasis = this.position >= 0.0 ? this.position : 0.0;
	this.shortBasis = this.position < 0.0 ? this.position : 0.0;
	// OpenValue
	this.openValue = this.position * this.mktPrice;
	// mkt price is always 1, so no realized/unrealized gains
	this.cumUnrealizedGain = 0.0;
	this.perUnrealizedGain = 0.0;
	this.perRealizedGain = 0.0;
	// other fields derive
	this.perIncomeExpense = this.income + this.expense;
	this.perTotalGain = this.perUnrealizedGain + this.perRealizedGain
		+ this.perIncomeExpense;
	this.cumTotalGain = 0.0;
    }
    /**Constructor which creates an appropriate TransactionValues object from a 
     *  primary security-level or Investment Account-level transaction 
     * @param thisParentTxn Parent Transaction
     * @param referenceAccount Investment Account associated with Security or bank transaction
     * @param bulkSecInfo 
     * @param transValuesSet 
     */
    public TransactionValues(ParentTxn thisParentTxn, Account referenceAccount,
	    SortedSet<TransactionValues> prevTransLines, BulkSecInfo currentInfo) {
        //intitalize values
        this.parentTxn = thisParentTxn;
        this.referenceAccount = referenceAccount;
        this.dateint = Integer.valueOf(thisParentTxn.getDateInt());
        this.txnID = Long.valueOf(thisParentTxn.getTxnId()).doubleValue();
        this.desc = thisParentTxn.getDescription();
        this.buy = 0;
        this.sell = 0;
        this.shortSell = 0;
        this.coverShort = 0;
        this.commision = 0;
        this.income = 0;
        this.expense = 0;
        this.transfer = 0;
        this.secQuantity = 0;
        //iterate through splits
        for (int i = 0; i < parentTxn.getSplitCount(); i++) {
            //gets values for each split.  Account Reference is parentTxn 
            //account in the case of a security, investment account
            //(i.e. itself) in the case of an investment account.
            SplitValues thisSplit = new SplitValues(parentTxn.getSplit(i),
                    referenceAccount.getAccountType() == Account.ACCOUNT_TYPE_INVESTMENT
                    ? referenceAccount : referenceAccount.getParentAccount());

            this.buy = this.buy + thisSplit.splitBuy;
            this.sell = this.sell + thisSplit.splitSell;
            this.shortSell = this.shortSell + thisSplit.splitShortSell;
            this.coverShort = this.coverShort + thisSplit.splitCoverShort;
            this.commision = this.commision + thisSplit.splitCommision;
            this.income = this.income + thisSplit.splitIncome;
            this.expense = this.expense + thisSplit.splitExpense;
            this.transfer = this.transfer + thisSplit.splitTransfer;
            this.secQuantity = this.secQuantity + thisSplit.splitSecQuantity;
        }
        
        //fill in rest of transValues
	TransactionValues prevTransLine = prevTransLines.isEmpty() ? null : prevTransLines
		.last();
	CurrencyType cur = this.referenceAccount.getCurrencyType();
	int currentDateInt = this.parentTxn.getDateInt();
	double currentRate = cur == null ? 1.0 : cur
		.getUserRateByDateInt(currentDateInt);
	int prevDateInt = prevTransLine == null ? Integer.MIN_VALUE
		: prevTransLine.parentTxn.getDateInt();
	double splitAdjust = (cur == null ? 1.0 : cur.adjustRateForSplitsInt(
		prevDateInt, currentRate, currentDateInt) / currentRate);
	double adjPrevPos = prevTransLine == null ? 0.0
		: prevTransLine.position * splitAdjust;
	double adjPrevMktPrc = prevTransLine == null ? 0.0
		: prevTransLine.mktPrice / splitAdjust;
	// mktPrice (Set to 1 if cur is null: Implies (Cash) Investment Account
	this.mktPrice = (cur == null ? 1.0 : 1 / cur
		.getUserRateByDateInt(currentDateInt));

	// position
	if (prevTransLine == null) { // first transaction (buy || shortSell) 
	    this.position = this.secQuantity;
	} else { // subsequent transaction
	    //round to zero if negligibly small  
	    this.position = Math.abs(this.secQuantity + adjPrevPos) < positionThreshold ? 0.0
		    : this.secQuantity + adjPrevPos;
	}

	
	//get long and short basis
	GainsCalc gainsCalc = currentInfo.getGainsCalc();
	gainsCalc.intializeGainsCalc(currentInfo, this, prevTransLines);
	this.longBasis = gainsCalc.getLongBasis();
	this.shortBasis = gainsCalc.getShortBasis();
	
	
	// OpenValue
	this.openValue = this.position * this.mktPrice;

	// cumulative unrealized gains
	if (this.position > 0.0) {
	    this.cumUnrealizedGain = this.openValue - this.longBasis;
	} else if (this.position < 0.0) {
	    this.cumUnrealizedGain = this.openValue - this.shortBasis;
	} else {
	    this.cumUnrealizedGain = 0.0;
	}

	// period unrealized gains
	if (this.position == 0.0) {
	    this.perUnrealizedGain = 0.0;
	} else {	    
	    if (this.secQuantity == 0.0) {
		// income/expense transaction, period gain is
		// change in cum unreal gains
		this.perUnrealizedGain = this.cumUnrealizedGain
			- (prevTransLine == null ? 0.0
				: prevTransLine.cumUnrealizedGain);
	    } else {// buy, sell, short, or cover transaction
		    // first case, add to long or add to short
		    // change in cumulative gains accounts for trans quantity
		if (this.secQuantity * this.position > 0.0) {
		    this.perUnrealizedGain = this.cumUnrealizedGain
			    - (prevTransLine == null ? 0.0
				    : prevTransLine.cumUnrealizedGain);
		} else { // reduce long or short
			 // unrealized gains equal 0 on position-closing
			 // transaction
		    this.perUnrealizedGain = this.position
			    * (this.mktPrice - adjPrevMktPrc);
		}
	    }
	}
	
	// Period Realized gains
	if (this.sell > 0) { // sale transaction
	    this.perRealizedGain = (this.sell + this.commision)
		    + (this.longBasis - prevTransLine.longBasis) ;
	} else if (this.coverShort < 0) { // cover transaction
	    this.perRealizedGain = (this.coverShort + this.commision)
		    + (this.shortBasis - prevTransLine.shortBasis);
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
	

    }
    /**Generic Constructor for TransactionValues which are inserted into
     * synthetically created cash account
     * @param transactionValues TransactionValues from Investment or Security Account
     * @param prevTransValues previous TransactionValues (to get position)
     * @param invAcctWrapper Investment Account
     * @throws Exception 
     */
    public TransactionValues(TransactionValues transactionValues, TransactionValues prevTransValues,
    	    InvestmentAccountWrapper invAcctWrapper) throws Exception {
    	// copy base values from Security Transaction
    	this.parentTxn = transactionValues.parentTxn;
    	this.referenceAccount = invAcctWrapper.getCashAccountWrapper().getSecurityAccountWrapper();
    	this.dateint = transactionValues.dateint;
    	// adding 0.1 to related transValues id to ensure unique cash id
    	this.txnID = TxnUtil.getInvestTxnType(transactionValues.parentTxn) == 
    		InvestTxnType.BANK ? transactionValues.parentTxn.getTxnId() : 
    		    transactionValues.parentTxn.getTxnId() + 0.1;
    	this.desc = "INSERTED: " + parentTxn.getDescription();
    	this.mktPrice = 1.0;
    	
    	double thisTransfer = transactionValues.transfer;
    	double acctEntry = -transactionValues.buy - transactionValues.coverShort
    		- transactionValues.sell - transactionValues.shortSell - transactionValues.income
    		- transactionValues.expense - transactionValues.commision;
    	double prevPos = prevTransValues.position;
    	
    	this.buy = 0.0;
    	this.sell = 0.0;
    	this.shortSell = 0.0;
    	this.coverShort = 0.0;
    	this.commision = 0.0;
    	this.income = 0.0;
    	this.expense = 0.0;
    	this.secQuantity = 0.0;	
    	InvestTxnType txnType = TxnUtil.getInvestTxnType(this.parentTxn);
    	
	switch (txnType) {
	case BANK: // transfer in/out, account-level income or expense
	    if (thisTransfer > 0.0) {// transfer in
		if (prevPos < 0.0) {
		    this.coverShort = Math.max(-thisTransfer, prevPos);
		    this.buy = Math.min(-thisTransfer - prevPos, 0.0);
		} else {
		    this.buy = -thisTransfer;
		}

	    } else if (thisTransfer < 0.0) {// transfer out
		if (prevPos > 0.0) {
		    this.sell = Math.min(-thisTransfer, prevPos);
		    this.shortSell = Math.max(-thisTransfer - prevPos, 0.0);
		} else {
		    this.shortSell = -thisTransfer;
		}
	    } else { // income or expense
		if (acctEntry <= 0.0) {// Account level Income
		    // like dividend/reinvest)
		    if (prevPos < 0.0) {
			this.coverShort = Math.max(acctEntry, prevPos);
			this.buy = Math.min(acctEntry - prevPos, 0.0);
		    } else {
			this.buy = acctEntry;
		    }
		    this.income = -acctEntry;
		} else {// Account level expense
			// like capital call (debit expense credit security)
		    if (prevPos > 0.0) {
			this.sell = Math.min(acctEntry, prevPos);
			this.shortSell = Math.max(acctEntry - prevPos, 0.0);
		    } else {
			this.shortSell = acctEntry;
		    }
		    this.expense = -acctEntry;

		}
	    }
	    break;
	case BUY:
	case COVER:
	case MISCEXP:
	    if (prevPos > 0.0) {
		this.sell = Math.min(acctEntry, prevPos);
		this.shortSell = Math.max(acctEntry - prevPos, 0.0);
	    } else {
		this.shortSell = acctEntry;
	    }
	    break;
	case SELL:
	case SHORT:
	case MISCINC:
	case DIVIDEND:
	    if (prevPos < 0.0) {
		this.coverShort = Math.max(acctEntry, prevPos);
		this.buy = Math.min(acctEntry - prevPos, 0.0);
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
	
        default:
            throw new AssertionError(txnType);
	}
    	
	this.secQuantity = -this.buy - this.coverShort - this.sell
		- this.shortSell;

	this.position = Math.abs(this.secQuantity + prevPos) < positionThreshold ? 0.0
		: this.secQuantity + prevPos;
    	this.longBasis = this.position >= 0.0 ? this.position : 0.0;
    	this.shortBasis = this.position < 0.0 ? this.position : 0.0;
    	// OpenValue
    	this.openValue = this.position * this.mktPrice;
    	//mkt price is always 1, so no realized/unrealized gains
    	this.cumUnrealizedGain = 0.0;
    	this.perUnrealizedGain = 0.0;
    	this.perRealizedGain = 0.0;
    	//other fields derive
    	this.perIncomeExpense = this.income + this.expense;
    	this.perTotalGain = this.perUnrealizedGain + this.perRealizedGain
    		+ this.perIncomeExpense;
    	this.cumTotalGain = prevTransValues == null ? this.perTotalGain : 
    	    this.perTotalGain + prevTransValues.cumTotalGain;
       }
    /*
     * lists header for transaction report
     */
    public static StringBuffer listTransValuesHeader() {
        StringBuffer txnInfo = new StringBuffer();
        txnInfo.append("ParentAcct " + ",");    
        txnInfo.append("Security " + ",");
        txnInfo.append("TxnNum " + ",");
        txnInfo.append("Date" + ",");
        txnInfo.append("TxnType"+ ",");
        txnInfo.append("Desc"+ ",");
        txnInfo.append("Buy"+ ",");
        txnInfo.append("Sell"+ ",");
        txnInfo.append("Short"+ ",");
        txnInfo.append("Cover"+ ",");
        txnInfo.append("Commison"+ ",");
        txnInfo.append("Income"+ ",");
        txnInfo.append("Expense"+ ",");
        txnInfo.append("transfer"+ ",");
        txnInfo.append("secQuantity"+ ",");
        txnInfo.append("MktPrice"+ ",");
        txnInfo.append("Position"+ ",");
        txnInfo.append("LongBasis"+ ",");
        txnInfo.append("ShortBasis"+ ",");
        txnInfo.append("OpenValue"+ ",");
        txnInfo.append("CumUnrealGain"+ ",");
        txnInfo.append("PerUnrealGain"+ ",");
        txnInfo.append("PerRealGain"+ ",");
        txnInfo.append("PerInc_Exp"+ ",");
        txnInfo.append("PerTotalGain"+ ",");
        txnInfo.append("CumTotalGain");
        return txnInfo;
    }
    /* loads TransValuesCum into String Array
        * @param transValuesCum
        * @return String Array
        */
       public static String[] loadArrayTransValues(TransactionValues transactionValues) {
    	ArrayList<String> txnInfo = new ArrayList<String>();
    	InvestTxnType transType = TxnUtil
    		.getInvestTxnType(transactionValues.parentTxn);
    
    	txnInfo.add(transactionValues.referenceAccount.getParentAccount()
    		.getAccountName());
    	txnInfo.add(transactionValues.referenceAccount.getAccountName());
    	txnInfo.add(Double.toString(transactionValues.txnID));
    	txnInfo.add(DateUtils
    		.convertToShort(transactionValues.dateint));
    	txnInfo.add(transType.toString());
    	txnInfo.add(transactionValues.desc);
    	txnInfo.add(Double.toString(transactionValues.buy));
    	txnInfo.add(Double.toString(transactionValues.sell));
    	txnInfo.add(Double.toString(transactionValues.shortSell));
    	txnInfo.add(Double.toString(transactionValues.coverShort));
    	txnInfo.add(Double.toString(transactionValues.commision));
    	txnInfo.add(Double.toString(transactionValues.income));
    	txnInfo.add(Double.toString(transactionValues.expense));
    	txnInfo.add(Double.toString(transactionValues.transfer));
    	txnInfo.add(Double.toString(transactionValues.secQuantity));
    	txnInfo.add(Double.toString(transactionValues.mktPrice));
    	txnInfo.add(Double.toString(transactionValues.position));
    	txnInfo.add(Double.toString(transactionValues.longBasis));
    	txnInfo.add(Double.toString(transactionValues.shortBasis));
    	txnInfo.add(Double.toString(transactionValues.openValue));
    	txnInfo.add(Double.toString(transactionValues.cumUnrealizedGain));
    	txnInfo.add(Double.toString(transactionValues.perUnrealizedGain));
    	txnInfo.add(Double.toString(transactionValues.perRealizedGain));
    	txnInfo.add(Double.toString(transactionValues.perIncomeExpense));
    	txnInfo.add(Double.toString(transactionValues.perTotalGain));
    	txnInfo.add(Double.toString(transactionValues.cumTotalGain));
    	return txnInfo.toArray(new String[txnInfo.size()]);
    
       }
    @Override
    public int compareTo(TransactionValues transactionValues) {
	return TransactionValues.transComp.compare(this, transactionValues);
    }
    public Account getAccountRef() {
        return referenceAccount;
    }
    public double getBuy() {
        return buy;
    }
    public double getCommision() {
        return commision;
    }
    public double getCoverShort() {
        return coverShort;
    }
    public double getCumTotalGain() {
        return cumTotalGain;
    }
    
    public double getCumUnrealizedGain() {
        return cumUnrealizedGain;
    }
    
    public Integer getDateint() {
        return dateint;
    }
    public String getDesc(){
	return desc;
    }
    public double getExpense() {
        return expense;
    }
    public double getIncome() {
        return income;
    }
    public double getLongBasis() {
        return longBasis;
    }
    public double getMktPrice() {
        return mktPrice;
    }
    public double getOpenValue() {
        return openValue;
    }
    //getters and setters (supports use of compiled JAR)
    public ParentTxn getParent() {
        return parentTxn;
    }
    public ParentTxn getParentTxn() {
        return parentTxn;
    }
    public double getPerIncomeExpense() {
        return perIncomeExpense;
    }
    public double getPerRealizedGain() {
        return perRealizedGain;
    }
    public double getPerTotalGain() {
        return perTotalGain;
    }
    public double getPerUnrealizedGain() {
        return perUnrealizedGain;
    }    
   
   public double getPosition() {
    return position;
}

public double getSecQuantity() {
    return secQuantity;
}
    
    
    public double getSell() {
    return sell;
}
    
    
    
    public double getShortBasis() {
        return shortBasis;
    }

    
    public double getShortSell() {
        return shortSell;
    }
    
    public double getTransfer() {
        return transfer;
    }

    public double getTxnID() {
        return txnID;
    }

    public Account getReferenceAccount() {
        return referenceAccount;
    }
    /**Custom sort order to put buys before sells, shorts before covers
     * @return
     */
    public Integer getTxnSortOrder() {
        InvestTxnType txnType = this.parentTxn != null ? 
        	TxnUtil.getInvestTxnType(parentTxn) : InvestTxnType.BANK;
        Integer txnOrder = 0;
        switch (txnType) {
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
        default:
            throw new AssertionError(txnType);
        }
        return txnOrder;
    }
    
    /**
     * determines buy/sell/income, etc values for split based upon
     * parentTxn transaction type.  variable names are same as TransactionValues
     */
    private class SplitValues {

	SplitTxn split;
	double splitBuy;
	double splitSell;
	double splitShortSell;
	double splitCoverShort;
	double splitCommision;
	double splitIncome;
	double splitExpense;
	double splitTransfer;
	double splitSecQuantity;

	public SplitValues(SplitTxn thisSplit, Account accountRef) {
	    this.split = thisSplit;
	    thisSplit.getDateInt();
	    InvestTxnType txnType = TxnUtil.getInvestTxnType(thisSplit
		    .getParentTxn());
	    int acctType = thisSplit.getAccount().getAccountType();
	    int parentAcctType = thisSplit.getParentTxn().getAccount()
		    .getAccountType();
	    Long amountLong = thisSplit.getAmount();
	    double amountDouble = (Long.valueOf(amountLong).doubleValue()) / 100.0;
	    Long valueLong = thisSplit.getValue();
	    double valueDouble = (Long.valueOf(valueLong).doubleValue()) / 10000.0;

	    this.splitBuy = 0;
	    this.splitSell = 0;
	    this.splitShortSell = 0;
	    this.splitCoverShort = 0;
	    this.splitCommision = 0;
	    this.splitIncome = 0;
	    this.splitExpense = 0;
	    this.splitTransfer = 0;
	    this.splitSecQuantity = 0;

	    /*
	     * goes through each transaction type, assigns values for each
	     * variable based on indicated transaction type and account type
	     */
	    switch (txnType) {
	    case BUY:// consists of buy, commission, and (potentially) transfer
	    case BUY_XFER: // no net cash effect (transfer offsets buy)
		switch (acctType) {
		case Account.ACCOUNT_TYPE_SECURITY:
		    this.splitBuy = amountDouble;
		    this.splitSecQuantity = valueDouble;
		    break;
		case Account.ACCOUNT_TYPE_EXPENSE:
		    this.splitCommision = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_INCOME:
		    this.splitIncome = amountDouble;
		    break;
		default:
		    this.splitTransfer = split.getAccount() == accountRef 
		    ? -amountDouble : amountDouble;
		    break;
		}
		break;
	    case SELL:// consists of sell, commission, and (potentially)
		      // transfer
	    case SELL_XFER: // no net cash effect (transfer offsets sell)
		switch (acctType) {
		case Account.ACCOUNT_TYPE_SECURITY:
		    this.splitSell = amountDouble;
		    this.splitSecQuantity = valueDouble;
		    break;
		case Account.ACCOUNT_TYPE_EXPENSE:
		    this.splitCommision = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_INCOME:
		    this.splitIncome = amountDouble;
		    break;
		default:
		    this.splitTransfer = split.getAccount() == accountRef 
		    ? -amountDouble : amountDouble;
		    break;
		}
		break;
	    case BANK: // Account-level transfers, interest, and expenses
		switch (acctType) {
		case Account.ACCOUNT_TYPE_EXPENSE:// Only count if parentTxn is
		     // investment
		    if (parentAcctType == Account.ACCOUNT_TYPE_INVESTMENT) {
			this.splitExpense = amountDouble;
		    }
		    break;
		case Account.ACCOUNT_TYPE_INCOME: // Only count if parentTxn is
		    // investment
		    if (parentAcctType == Account.ACCOUNT_TYPE_INVESTMENT) {
			this.splitIncome = amountDouble;
		    }
		    break;
		// next cases cover transfer between Assets/Investments, Bank.
		case Account.ACCOUNT_TYPE_INVESTMENT:
		case Account.ACCOUNT_TYPE_BANK:
		case Account.ACCOUNT_TYPE_ASSET:
		case Account.ACCOUNT_TYPE_LIABILITY:
                case Account.ACCOUNT_TYPE_CREDIT_CARD:
                case Account.ACCOUNT_TYPE_LOAN:
		    if (split.getAccount() == accountRef) {
			this.splitTransfer = -amountDouble;
		    } else {
			this.splitTransfer = amountDouble;
		    }
		    break;
                default:
                    throw new AssertionError(txnType);
		}
		break;
	    case DIVIDEND:
	    case DIVIDENDXFR: // income/expense transactions
		switch (acctType) {
		case Account.ACCOUNT_TYPE_EXPENSE:
		    this.splitExpense = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_INCOME:
		    this.splitIncome = amountDouble;
		    break;
		default:
		    this.splitTransfer = split.getAccount() == accountRef ?
			    -amountDouble : amountDouble;
		    break;
		}
		break;
	    case SHORT: // short sales + commission
		switch (acctType) {
		case Account.ACCOUNT_TYPE_SECURITY:
		    this.splitShortSell = amountDouble;
		    this.splitSecQuantity = valueDouble;
		    break;
		case Account.ACCOUNT_TYPE_EXPENSE:
		    this.splitCommision = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_INCOME:
		    this.splitIncome = amountDouble;
		    break;
                default:
                    throw new AssertionError(txnType);
		}
		break;
	    case COVER:// short covers + commission
		switch (acctType) {
		case Account.ACCOUNT_TYPE_SECURITY:
		    this.splitCoverShort = amountDouble;
		    this.splitSecQuantity = valueDouble; 
		    break;
		case Account.ACCOUNT_TYPE_EXPENSE:
		    this.splitCommision = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_INCOME:
		    this.splitIncome = amountDouble;
		    break;
                default:
                    throw new AssertionError(txnType);
		}
		break;
	    case MISCINC: // misc income and expense
		switch (acctType) {
		case Account.ACCOUNT_TYPE_EXPENSE:
		    this.splitExpense = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_INCOME:
		    this.splitIncome = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_SECURITY:
		    this.splitBuy = amountDouble; // provides for return of capital
		    this.splitSecQuantity = valueDouble; 
		    break;
		}
		break;
	    case MISCEXP: // misc income and expense
		switch (acctType) {
		case Account.ACCOUNT_TYPE_EXPENSE:
		    this.splitExpense = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_INCOME:
		    this.splitIncome = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_SECURITY:
		    this.splitShortSell = amountDouble; // provides for reduction
		    this.splitSecQuantity = valueDouble;//of capital 
		    //possible treatment of dividend payment of short
		    break;
                default:
                    throw new AssertionError(txnType);
		}
		break;
	    case DIVIDEND_REINVEST: // income and buy with no net cash effect
		switch (acctType) {
		case Account.ACCOUNT_TYPE_EXPENSE:
		    this.splitCommision = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_INCOME:
		    this.splitIncome = amountDouble;
		    break;
		case Account.ACCOUNT_TYPE_SECURITY:
		    this.splitBuy = amountDouble;
		    this.splitSecQuantity = valueDouble;
		    break;
                default:
                    throw new AssertionError(txnType);
		}
                break;
            default:
                throw new AssertionError(txnType);
	    } // end txnType Switch Statement
	} // end splitValues Constructor
    } // end splitValues subClass
    
} // end TransactionValues Class


