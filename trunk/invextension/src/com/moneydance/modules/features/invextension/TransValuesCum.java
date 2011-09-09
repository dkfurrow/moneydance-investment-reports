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

import com.moneydance.apps.md.model.*;
import java.util.*;
import com.moneydance.apps.md.model.TxnUtil;


/** produces cumulative transaction data from basic transaction data
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public class TransValuesCum implements Comparable<TransValuesCum> {

    // transValues (basic trans data)
    public TransValues transValues;
    // net position after completion of transaction
    public double position;
    // market price on close of transaction day
    public double mktPrice;
    // net average cost long basis after completion of transaction
    public double longBasis;
    // net average cost short basis after completion of transaction
    public double shortBasis;
    // net open value after completion of transaction
    public double openValue;
    // net cumulative unrealized gains after completion of transaction
    public double cumUnrealizedGain;
    // period unrealized gain (one transaction to next) after completion of
    // transaction
    public double perUnrealizedGain;
    // period realized gain (one transaction to next) after completion of
    // transaction
    public double perRealizedGain;
    // period income and expense gain (one transaction to next) after completion
    // of transaction
    public double perIncomeExpense;
    // period total gain (one transaction to next) after completion of
    // transaction
    public double perTotalGain;
    // cumulative total gain after completion of transaction
    public double cumTotalGain;

    //getters and setters (supports use of compiled JAR)
    public TransValues getTransValues() {
        return transValues;
    }
    public double getPosition() {
        return position;
    }
    public double getMktPrice() {
        return mktPrice;
    }
    public double getLongBasis() {
        return longBasis;
    }
    public double getShortBasis() {
        return shortBasis;
    }
    public double getOpenValue() {
        return openValue;
    }
    public double getCumUnrealizedGain() {
        return cumUnrealizedGain;
    }
    public double getPerUnrealizedGain() {
        return perUnrealizedGain;
    }
    public double getPerRealizedGain() {
        return perRealizedGain;
    }
    public double getPerIncomeExpense() {
        return perIncomeExpense;
    }
    public double getPerTotalGain() {
        return perTotalGain;
    }
    public double getCumTotalGain() {
        return cumTotalGain;
    }
    
    public static SortedSet<TransValuesCum> getTransValuesCum(
	    SortedSet<TransValues> theseTransValues, BulkSecInfo currentInfo) {
	TreeSet<TransValuesCum> transSet = new TreeSet<TransValuesCum>();
	// fill in transvalues
	for (Iterator<TransValues> it = theseTransValues.iterator(); it
		.hasNext();) {
	    TransValues thisParentValue = it.next();
	    TransValuesCum trans = new TransValuesCum();
	    trans.transValues = thisParentValue;
	    transSet.add(trans);
	}
	// fill in rest of transvaluesCum (values which are dependent on
	// previous activity)
	for (Iterator<TransValuesCum> it = transSet.iterator(); it.hasNext();) {
	    TransValuesCum transValuesCum = it.next();
	    TransValuesCum prevTransValuesCum = transSet.lower(transValuesCum);
	    CurrencyType cur = currentInfo.secCur
		    .get(transValuesCum.transValues.secAccount);

	    int currentDateInt = transValuesCum.transValues.parentTxn
		    .getDateInt();
	    // position
	    if (prevTransValuesCum == null) { // first transaction (must be buy
					      // or short)
		transValuesCum.position = transValuesCum.transValues.secQuantity;
	    } else { // subsequent transaction
		int prevDateInt = prevTransValuesCum.transValues.parentTxn
			.getDateInt();
		double currentRate = cur == null ? 1.0 : cur
			.getUserRateByDateInt(currentDateInt);
		double splitAdjust = cur == null ? 1.0 : cur
			.adjustRateForSplitsInt(prevDateInt, currentRate,
				currentDateInt)
			/ currentRate;
		transValuesCum.position = prevTransValuesCum.position
			* splitAdjust + transValuesCum.transValues.secQuantity;
		// added to prevent rounding issues from creating an apparent
		//open position where none exists
		if (Math.abs(transValuesCum.position) < 0.0005)
		    transValuesCum.position = 0; 
						
	    }
	    // mktPrice (Set to 1 if cur is null: Implies Investment Account
	    // (i.e. cash)
	    transValuesCum.mktPrice = (cur == null ? 1.0 : 1 / cur
		    .getUserRateByDateInt(currentDateInt));

	    // long basis (includes commission)
	    
	    
	    if (transValuesCum.position <= 0.00001) {//position short or closed
		transValuesCum.longBasis = 0;
	    } else if (transValuesCum.position >=  
		    (prevTransValuesCum == null ? 0 
			    : prevTransValuesCum.position)) { 
		// add current buy to previous long basis
		transValuesCum.longBasis = -transValuesCum.transValues.buy
			- transValuesCum.transValues.commision
			+ (prevTransValuesCum == null ? 0
				: prevTransValuesCum.longBasis);
	    } else { 
		//no new buy, long basis is previous adjusted for split
		transValuesCum.longBasis = prevTransValuesCum.longBasis
			+ prevTransValuesCum.longBasis
			* (transValuesCum.position
				/ prevTransValuesCum.position - 1);
	    }

	    // short basis (includes commission)

	    // added to avoid rounding errors
	    if (transValuesCum.position >= -0.00001) { //position long or closed
		transValuesCum.shortBasis = 0;
	    } else if (transValuesCum.position <=
		    (prevTransValuesCum == null ? 0
		    : prevTransValuesCum.position)) {
		// add current short sale to previous short basis
		transValuesCum.shortBasis = -transValuesCum.transValues.shortSell
			- transValuesCum.transValues.commision
			+ (prevTransValuesCum == null ? 0
				: +prevTransValuesCum.shortBasis);
	    } else {
		//no new short sale, short basis is previous adjusted for split
		transValuesCum.shortBasis = prevTransValuesCum.shortBasis
			+ prevTransValuesCum.shortBasis
			* (transValuesCum.position
				/ prevTransValuesCum.position - 1);
	    }

	    // OpenValue
	    transValuesCum.openValue = transValuesCum.position
		    * transValuesCum.mktPrice;

	    // cumulative unrealized gains
	    if (transValuesCum.position > 0) {
		transValuesCum.cumUnrealizedGain = transValuesCum.openValue
			- transValuesCum.longBasis;
	    } else if (transValuesCum.position < 0) {
		transValuesCum.cumUnrealizedGain = transValuesCum.openValue
			- transValuesCum.shortBasis;
	    } else {
		transValuesCum.cumUnrealizedGain = 0;
	    }

	    // period unrealized gains
	    if (transValuesCum.position == 0) {
		transValuesCum.perUnrealizedGain = 0;
	    } else {
		transValuesCum.perUnrealizedGain = transValuesCum.cumUnrealizedGain
			- (prevTransValuesCum == null ? 0
				: prevTransValuesCum.cumUnrealizedGain);
	    }

	    // Period Realized gains
	    if (transValuesCum.transValues.sell > 0) { // sale transaction
		transValuesCum.perRealizedGain = transValuesCum.transValues.sell
			+ (transValuesCum.longBasis - (prevTransValuesCum == null ? 0
				: prevTransValuesCum.longBasis))
			+ transValuesCum.transValues.commision;
	    } else if (transValuesCum.transValues.coverShort < 0) { // cover
								    // transaction
		transValuesCum.perRealizedGain = transValuesCum.transValues.coverShort
			+ (transValuesCum.shortBasis - (prevTransValuesCum == null ? 0
				: prevTransValuesCum.shortBasis))
			+ transValuesCum.transValues.commision;
	    } else {
		// implies for closed pos, cumUnrealized-cumRealized =
		// commission
		// (on last trade)
		transValuesCum.perRealizedGain = 0;
	    }

	    // period income/expense
	    transValuesCum.perIncomeExpense = transValuesCum.transValues.income
		    + transValuesCum.transValues.expense;

	    // period total gain
	    transValuesCum.perTotalGain = transValuesCum.perUnrealizedGain
		    + transValuesCum.perRealizedGain
		    + transValuesCum.perIncomeExpense;

	    // cumulative total gain
	    transValuesCum.cumTotalGain = transValuesCum.perTotalGain
		    + (prevTransValuesCum == null ? 0
			    : prevTransValuesCum.cumTotalGain);
	}
	return transSet;

    }

    public int compareTo(TransValuesCum theseValues){
        return  transValues.compareTo(theseValues.transValues);
    }

    /*
     * lists header for transaction report
     */
    public static StringBuffer listTransValuesCumHeader() {
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
        txnInfo.append("cashEffect"+ ",");
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
    /**\
     * loads TransValuesCum into String Array
     * @param transValuesCum
     * @return String Array
     */
    public static String[] loadArrayTransValuesCum(TransValuesCum transValuesCum) {
	ArrayList<String> txnInfo = new ArrayList<String>();
	InvestTxnType transType = TxnUtil
		.getInvestTxnType(transValuesCum.transValues.parentTxn);

	txnInfo.add(transValuesCum.transValues.accountRef.getParentAccount()
		.getAccountName());
	txnInfo.add(transValuesCum.transValues.accountRef.getAccountName());
	txnInfo.add(Long.toString(transValuesCum.transValues.parentTxn
		.getTxnId()));
	txnInfo.add(DateUtils
		.convertToShort(transValuesCum.transValues.dateint));
	txnInfo.add(transType.toString());
	txnInfo.add(transValuesCum.transValues.parentTxn.getDescription());
	txnInfo.add(Double.toString(transValuesCum.transValues.buy));
	txnInfo.add(Double.toString(transValuesCum.transValues.sell));
	txnInfo.add(Double.toString(transValuesCum.transValues.shortSell));
	txnInfo.add(Double.toString(transValuesCum.transValues.coverShort));
	txnInfo.add(Double.toString(transValuesCum.transValues.commision));
	txnInfo.add(Double.toString(transValuesCum.transValues.income));
	txnInfo.add(Double.toString(transValuesCum.transValues.expense));
	txnInfo.add(Double.toString(transValuesCum.transValues.transfer));
	txnInfo.add(Double.toString(transValuesCum.transValues.cashEffect));
	txnInfo.add(Double.toString(transValuesCum.transValues.secQuantity));
	txnInfo.add(Double.toString(transValuesCum.mktPrice));
	txnInfo.add(Double.toString(transValuesCum.position));
	txnInfo.add(Double.toString(transValuesCum.longBasis));
	txnInfo.add(Double.toString(transValuesCum.shortBasis));
	txnInfo.add(Double.toString(transValuesCum.openValue));
	txnInfo.add(Double.toString(transValuesCum.cumUnrealizedGain));
	txnInfo.add(Double.toString(transValuesCum.perUnrealizedGain));
	txnInfo.add(Double.toString(transValuesCum.perRealizedGain));
	txnInfo.add(Double.toString(transValuesCum.perIncomeExpense));
	txnInfo.add(Double.toString(transValuesCum.perTotalGain));
	txnInfo.add(Double.toString(transValuesCum.cumTotalGain));
	return txnInfo.toArray(new String[txnInfo.size()]);

    }

    }
