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


import com.infinitekind.moneydance.model.CurrencyType;

/**
 * Created by larus on 11/27/14.
 * This is the base class for financial extractors, which process a sequence of transactions on a security in
 * an account to compute and return a financial metric.
 */
public class ExtractorBase<R> {
    protected SecurityAccountWrapper securityAccount;
    
    protected int startDateInt;
    protected int endDateInt;

    protected TransactionValues lastTransactionBeforeStartDate;
    protected TransactionValues lastTransactionBeforeEqualStartDate;
    protected TransactionValues lastTransactionWithinDateRange;

    private long pXqScale =  10000; // FIXME: this works when security quantity has 4 digits and currency has 2 digits
    private long qDpScale = 10000;    // 1/pDqScale to avoid fraction => multiply, not divide

    /*
     * <p>Constructor</p>
     *
     * @param secAccountWrapper The security account being scanned.
     * @param dateRange Time interval of transactions to be used to compute results.
     * NB processNextTransaction sees all transactions, even those outside the date range.
     *
     */
    ExtractorBase(SecurityAccountWrapper securityAccount, int startDateInt, int endDateInt) {
        this.securityAccount = securityAccount;
        this.startDateInt = startDateInt;
        this.endDateInt = endDateInt;

        lastTransactionBeforeStartDate = null;

        if (securityAccount != null)  {
            CurrencyWrapper securityCurrencyWrapper = securityAccount.getCurrencyWrapper();
            CurrencyType securityCurrency = securityCurrencyWrapper.getCurrencyType();
            int securityDecimalPlaces = securityCurrency.getDecimalPlaces();

            //CurrencyTable currencyTable = securityCurrencyWrapper.getCurrencyType().getTable();
            //CurrencyType baseCurrency = currencyTable.getBaseType();
            //int cashDecimalPlaces = baseCurrency.getDecimalPlaces();
            int cashDecimalPlaces = 2;  // No need to compute: it cancels out of scale factors to produce currency result

            pXqScale = (long) Math.pow(10.0, cashDecimalPlaces + securityDecimalPlaces - cashDecimalPlaces);
            qDpScale = (long) Math.pow(10.0, securityDecimalPlaces - cashDecimalPlaces + cashDecimalPlaces);  // 1/pDqScale to avoid fraction
        }
    }

    /**
     * Copies other extractor
     * @param extractor other extractor
     */
    public ExtractorBase (ExtractorBase<R> extractor){
        this.securityAccount = extractor.securityAccount;
        this.startDateInt = extractor.startDateInt;
        this.endDateInt = extractor.endDateInt;
        this.lastTransactionBeforeStartDate = extractor.lastTransactionBeforeStartDate;
        this.lastTransactionBeforeEqualStartDate = extractor.lastTransactionBeforeEqualStartDate;
        this.lastTransactionWithinDateRange = extractor.lastTransactionWithinDateRange;
        this.pXqScale = extractor.pXqScale;
        this.qDpScale = extractor.qDpScale;
    }

    /* <p>Processes the next transaction in sequence of transactions.</p>
     *
     * @param transaction Sequence of transactions in non-decreasing time order.
     * @param transactionDateInt Date of transition.
     *
     * @return true if transaction is processed, and false if error occurs.
     */
    public boolean processNextTransaction(TransactionValues transaction, int transactionDateInt) {
        if (transactionDateInt < startDateInt) {
            lastTransactionBeforeStartDate = transaction;
            lastTransactionBeforeEqualStartDate = transaction;
        } else if (transactionDateInt <= endDateInt) {
            if (transactionDateInt == startDateInt) {
                lastTransactionBeforeEqualStartDate = transaction;
            }
            lastTransactionWithinDateRange = transaction;
        }
        return true;
    }

    /* <p>Returns the result of processing the sequence of transactions up to the current point.</p>
     *
     * @param securityAccount The account containing the transactions.
     *
     * @return Result up to current point in scan.
     */
    public R getResult() {
        throw new UnsupportedOperationException();
    }

    /* <p>Aggregate the financial data from an extractor in this extractor. The financial results
     * for the aggregated data is not computed until ComputeAggregatedFinancialResults is invoked. </p>
     *
     * @param operand Another extractors whose metric is aggregated into this one. The argument is not modified.
     *
     * @return None.
     */
    public void aggregateResults(ExtractorBase<?> operand) {
        throw new UnsupportedOperationException();
    }

    //
    // Internal methods.
    //
    protected long getStartPosition(SecurityAccountWrapper securityAccount) {
        if (lastTransactionBeforeEqualStartDate != null) {
            return getSplitAdjustedPosition(securityAccount,
                    lastTransactionBeforeEqualStartDate.getPosition(),
                    lastTransactionBeforeEqualStartDate.getDateInt(),
                    startDateInt);
        }
        return 0;
    }

    protected long getEndPosition(SecurityAccountWrapper securityAccount) {
        if (lastTransactionWithinDateRange != null) {
            return getSplitAdjustedPosition(securityAccount,
                    lastTransactionWithinDateRange.getPosition(),
                    lastTransactionWithinDateRange.getDateInt(),
                    endDateInt);
        } else if (lastTransactionBeforeStartDate != null) {
            return getSplitAdjustedPosition(securityAccount,
                    lastTransactionBeforeStartDate.getPosition(),
                    lastTransactionBeforeStartDate.getDateInt(),
                    endDateInt);
        }
        return 0;
    }

    protected long getSplitAdjustedPosition(SecurityAccountWrapper securityAccount,
                                            long referencePosition, int referenceDateInt, int currentDateInt) {
        CurrencyWrapper currencyWrapper = securityAccount.getCurrencyWrapper();
        CurrencyType currency = currencyWrapper.getCurrencyType();
        double splitAdjust = 1.0;
        if (currency != null) {
            double currentRate = currency.getRate(null, currentDateInt);
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
}
