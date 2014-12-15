/*
 * ExtractorBase.java
 * Copyright (c) 2014, James R. Larus, Dale K. Furrow
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

import com.moneydance.apps.md.model.CurrencyType;

/**
 * Created by larus on 11/27/14.
 * This is the base class for financial extractors, which process a sequence of transactions on a security in
 * an account to compute and return a financial metric.
 */
public class ExtractorBase<R> {
    protected int startDateInt;
    protected int endDateInt;

    protected TransactionValues lastTransactionBeforeStartDate;
    protected TransactionValues lastTransactionWithinEndDate;
    private TransactionValues previousTransaction;

    protected long pXqScale;
    protected long qDpScale;    // 1/pDqScale to avoid fraction => multiply, not divide

    /*
     * <p>Constructor</p>
     *
     * @param secAccountWrapper The security account being scanned.
     * @param dateRange Time interval of transactions to be used to compute results.
     * NB NextTransaction sees all transactions, even those outside the date range.
     *
     */
    ExtractorBase(SecurityAccountWrapper securityAccount, int startDateInt, int endDateInt) {
        this.startDateInt = startDateInt;
        this.endDateInt = endDateInt;

        lastTransactionBeforeStartDate = null;
        previousTransaction = null;

        if (securityAccount == null) {
            pXqScale = 10000;   // FIXME: this works when security quanity has 4 digits and currency has 2 digits
            qDpScale = 10000;
        } else {
            CurrencyWrapper securityCurrencyWrapper = securityAccount.getCurrencyWrapper();
            CurrencyType securityCurrency = securityCurrencyWrapper.getCurrencyType();
            int securityDecimalPlaces = securityCurrency.getDecimalPlaces();

            //CurrencyTable currencyTable = securityCurrencyWrapper.getCurrencyType().getTable();
            //CurrencyType baseCurrency = currencyTable.getBaseType();
            //int cashDecimalPlaces = baseCurrency.getDecimalPlaces();
            int cashDecimalPlaces = 2;  // No need to compute: it cancels out of scale factors to produce currency result

            pXqScale = power10(cashDecimalPlaces + securityDecimalPlaces - cashDecimalPlaces);
            qDpScale = power10(securityDecimalPlaces - cashDecimalPlaces + cashDecimalPlaces);  // 1/pDqScale to avoid fraction
        }
    }

    /*
     * <p>Change the starting date for subsequent calculations.</p>
     *
     * @param newStartDateInt new start date
     */
    public void setStartDateInt(int newStartDateInt) {
        this.startDateInt = newStartDateInt;
    }

    /* <p>Processes the next transaction in sequence of transactions.</p>
     *
     * @param transaction Sequence of transactions in non-decreasing time order.
     * @param transactionIndex Index of next transition to process in this sequence.
     *
     * @return true if transaction is processed, and false if error occurs.
     */
    public boolean NextTransaction(TransactionValues transaction, SecurityAccountWrapper securityAccount) {
        int transactionDateInt = transaction.getDateInt();
        assert previousTransaction != null && previousTransaction.getDateInt() <= transactionDateInt;

        if (transactionDateInt < startDateInt) {
            lastTransactionBeforeStartDate = transaction;
        } else if (transactionDateInt <= endDateInt) {
            lastTransactionWithinEndDate = transaction;
        }
        return true;
    }

    /* <p>Returns the result of processing the sequence of transactions up to the current point.</p>
     *
     * @param securityAccount The account containing the transactions.
     *
     * @return Result up to current point in scan.
     */
    public R FinancialResults(SecurityAccountWrapper securityAccount) {
        throw new UnsupportedOperationException();
    }

    /* <p>Combine the financial results from two extractors into this extractor.</p>
     *
     * @param operand Another extractors whose metric is combine into this one. The argument is not modified.
     *
     * @return None.
     */
    public R CombineFinancialResults(ExtractorBase<?> operand) {
        throw new UnsupportedOperationException();
    }

    //
    // Internal methods.
    //
    protected long getSplitAdjustedPosition(SecurityAccountWrapper securityAccount,
                                            long referencePosition, int referenceDateInt, int currentDateInt) {
        CurrencyWrapper currencyWrapper = securityAccount.getCurrencyWrapper();
        CurrencyType currency = currencyWrapper.getCurrencyType();
        double splitAdjust = 1.0;
        if (currency != null) {
            double currentRate = currency.getUserRateByDateInt(currentDateInt);
            splitAdjust = currency.adjustRateForSplitsInt(referenceDateInt, currentRate, currentDateInt) / currentRate;
        }
        return Math.round(referencePosition * splitAdjust);
    }

    /* <p> Multiple quantity by price to compute value. Scale result properly for this security.
     *
     * @param quantity Quantity of securities, appropriately scaled.
     * @param price Price of security, appropriately scaled.
     *
     * @result Value of holding, appropriately scaled.
     */
    protected long qXp(long quantity, long price) {
        return price * quantity / pXqScale;
    }

    /* <p> Divide total price by quantity to unit cost. Scale result properly for this security.
     *
     * @param price Price of security, appropriately scaled.
     * @param quantity Quantity of securities, appropriately scaled.
     *
     * @result Unit cost, appropriately scaled.
     */
    protected long pDq(long price, long quantity) {
        return (price * qDpScale / quantity);
    }

    private long power10(int n) {
        int result = 1;
        for (int i = 0; i < n; i++) {
            result *= 10;
        }
        return result;
    }
}
