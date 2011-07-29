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

    public TransValues transValues;       //transValues (basic trans data)
    public double position;               //net position after completion of transaction
    public double mktPrice;               //market price on close of transaction day
    public double longBasis;              //net average cost long basis after completion of transaction
    public double shortBasis;             //net average cost short basis after completion of transaction
    public double openValue;              //net open value after completion of transaction
    public double cumUnrealizedGain;      //net cumulative unrealized gains after completion of transaction
    public double perUnrealizedGain;      //period unrealized gain (one transaction to next) after completion of transaction
    public double perRealizedGain;        //period realized gain (one transaction to next) after completion of transaction
    public double perIncomeExpense;       //period income and expense gain (one transaction to next) after completion of transaction
    public double perTotalGain;           //period total gain (one transaction to next) after completion of transaction
    public double cumTotalGain;           //cumulative total gain after completion of transaction

    public static SortedSet<TransValuesCum> getTransValuesCum
            (SortedSet<TransValues> theseTransValues, BulkSecInfo currentInfo) {
        TreeSet<TransValuesCum> transSet = new TreeSet<TransValuesCum>();
        //fill in transvalues
         for (Iterator<TransValues> it = theseTransValues.iterator(); it.hasNext();) {
            TransValues thisParentValue = it.next();
            TransValuesCum trans = new TransValuesCum();
            trans.transValues = thisParentValue;
            transSet.add(trans);
        }
        // fill in rest of  transvaluesCum (values which are dependent on previous activity)
        for (Iterator<TransValuesCum> it = transSet.iterator(); it.hasNext();) {
            TransValuesCum transValuesCum = it.next();
            TransValuesCum prevTransValuesCum = transSet.lower(transValuesCum);
            CurrencyType cur = currentInfo.secCur.get(transValuesCum.transValues.secAccount);

            int currentDateInt = transValuesCum.transValues.parent.getDateInt();
            // position
            if (prevTransValuesCum == null) { //first transaction (must be buy or short)
                transValuesCum.position = transValuesCum.transValues.secQuantity;
            } else { //subsequent transaction
                int prevDateInt = prevTransValuesCum.transValues.parent.getDateInt();
                double currentRate = cur == null ? 1.0 :
                    cur.getUserRateByDateInt(currentDateInt);
                double splitAdjust = cur == null ? 1.0 :
                    cur.adjustRateForSplitsInt(prevDateInt, currentRate, currentDateInt) / currentRate;
                transValuesCum.position = prevTransValuesCum.position * splitAdjust
                        + transValuesCum.transValues.secQuantity;
                if(Math.abs(transValuesCum.position)<0.0005)
                    transValuesCum.position = 0; // added to prevent closed pos rounding issues
            }
            // mktPrice (Set to 1 if cur is null: Implies Investment Account (i.e. cash)
            transValuesCum.mktPrice =  (cur == null ? 1.0 : 1/ cur.getUserRateByDateInt(currentDateInt));

            // long basis (includes commission)

            if (transValuesCum.position <= 0.00001) {//added to avoid rounding errors
                transValuesCum.longBasis = 0;
            } else if (transValuesCum.position >= (prevTransValuesCum == null
                    ? 0 : prevTransValuesCum.position)) {
                transValuesCum.longBasis = - transValuesCum.transValues.buy
                        - transValuesCum.transValues.commision
                        + (prevTransValuesCum == null ? 0
                        : prevTransValuesCum.longBasis);
            } else {
                transValuesCum.longBasis = prevTransValuesCum.longBasis
                        + prevTransValuesCum.longBasis
                        * (transValuesCum.position / prevTransValuesCum.position - 1);
            }

            // short basis (includes commission)

            if (transValuesCum.position >= -0.00001) {//added to avoid rounding errors
                transValuesCum.shortBasis = 0;
            } else if (transValuesCum.position <= (prevTransValuesCum == null
                    ? 0 : prevTransValuesCum.position)) {
                transValuesCum.shortBasis = - transValuesCum.transValues.shortSell
                        - transValuesCum.transValues.commision
                        + (prevTransValuesCum == null ? 0
                        : +prevTransValuesCum.shortBasis);
            } else {
                transValuesCum.shortBasis = prevTransValuesCum.shortBasis
                        + prevTransValuesCum.shortBasis
                        * (transValuesCum.position / prevTransValuesCum.position - 1);
            }

            // OpenValue
            transValuesCum.openValue = transValuesCum.position * transValuesCum.mktPrice;

            // cumulative unrealized gains
            if(transValuesCum.position > 0){
                transValuesCum.cumUnrealizedGain = transValuesCum.openValue -
                        transValuesCum.longBasis;
            } else if(transValuesCum.position < 0) {
                transValuesCum.cumUnrealizedGain = transValuesCum.openValue -
                        transValuesCum.shortBasis;
            } else {
                transValuesCum.cumUnrealizedGain = 0;
            }

            //period unrealized gains
            if (transValuesCum.position == 0) {
                transValuesCum.perUnrealizedGain = 0;
            } else {
                transValuesCum.perUnrealizedGain = transValuesCum.cumUnrealizedGain
                        - (prevTransValuesCum == null ? 0 : prevTransValuesCum.cumUnrealizedGain);
            }

            // Period Realized gains
            if (transValuesCum.transValues.sell > 0) {
                transValuesCum.perRealizedGain = transValuesCum.transValues.sell
                        + (transValuesCum.longBasis - ( prevTransValuesCum == null ? 0 : prevTransValuesCum.longBasis))
                        + transValuesCum.transValues.commision;
            } else if (transValuesCum.transValues.coverShort < 0) {
                transValuesCum.perRealizedGain = transValuesCum.transValues.coverShort
                        + (transValuesCum.shortBasis - ( prevTransValuesCum == null ? 0 : prevTransValuesCum.shortBasis))
                        + transValuesCum.transValues.commision;
            } else {
                transValuesCum.perRealizedGain = 0;  //implies for closed pos, cumUnrealized-cumRealized = commission (on last trade)
            }

            //period income/expense
            transValuesCum.perIncomeExpense = transValuesCum.transValues.income +
                    transValuesCum.transValues.expense;

            //period total gain
            transValuesCum.perTotalGain = transValuesCum.perUnrealizedGain +
                    transValuesCum.perRealizedGain + transValuesCum.perIncomeExpense;

            //cumulative total gain
            transValuesCum.cumTotalGain = transValuesCum.perTotalGain +
                    ( prevTransValuesCum == null ? 0 :prevTransValuesCum.cumTotalGain);
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
        InvestTxnType transType = TxnUtil.getInvestTxnType(transValuesCum.transValues.parent);

        txnInfo.add(transValuesCum.transValues.accountRef.getParentAccount().getAccountName());
        txnInfo.add(transValuesCum.transValues.accountRef.getAccountName());
        txnInfo.add(Long.toString(transValuesCum.transValues.parent.getTxnId()));
        txnInfo.add(DateUtils.convertToShort(transValuesCum.transValues.dateint));
        txnInfo.add(transType.toString());
        txnInfo.add(transValuesCum.transValues.parent.getDescription());
        txnInfo.add(Double.toString(transValuesCum.transValues.buy));
        txnInfo.add(Double.toString(transValuesCum.transValues.sell));
        txnInfo.add(Double.toString(transValuesCum.transValues.shortSell));
        txnInfo.add(Double.toString(transValuesCum.transValues.coverShort));
        txnInfo.add(Double.toString(transValuesCum.transValues.commision));
        txnInfo.add(Double.toString(transValuesCum.transValues.income ));
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
