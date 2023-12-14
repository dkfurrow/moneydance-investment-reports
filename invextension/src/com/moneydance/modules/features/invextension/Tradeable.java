/*
 * Tradeable.java
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

import org.jetbrains.annotations.NotNull;

/**
 * Tradeable class is a derivative of Currency.
 * Main member: Boolean isTradeableCurrency: true for Securities of all currencies
 * except the currency representing uninvested cash
 * (BulkSecInfo.cashCurrencyWrapper)
 * Version 1.0
 *
 * @author Dale Furrow
 */

//Suppressed warnings because of static field name use in method getDeclaredField
public final class Tradeable implements Aggregator {
    // name of aggregation method
    static String reportingName = "Tradeable Security/Uninvested Cash";
    // column name for sorting
    static String columnName = "Security";
    CurrencyWrapper currencyWrapper;
    Boolean isTradeableCurrency;

    public Tradeable(@NotNull CurrencyWrapper currencyWrapper, @NotNull CurrencyWrapper cashCurrencyWrapper) {
        this.currencyWrapper = currencyWrapper;
        this.isTradeableCurrency = this.currencyWrapper.currencyType
                != cashCurrencyWrapper.getCurrencyType();
    }

    public Tradeable(Boolean isTradeableCurrency) {
        this.currencyWrapper = null;
        this.isTradeableCurrency = isTradeableCurrency;
    }

    /**
     * empty constructor for Aggregator
     */
    public Tradeable() {
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((isTradeableCurrency == null) ? 0 : isTradeableCurrency.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Tradeable other = (Tradeable) obj;
        if (isTradeableCurrency == null) {
            return other.isTradeableCurrency == null;
        } else return isTradeableCurrency.equals(other.isTradeableCurrency);
    }

    @Override
    public String getName() {
        return getAggregateName();
    }

    @Override
    public String getAggregateName() {
        return this.isTradeableCurrency ? "All Securities " : "All CASH ";
    }

    @Override
    public String getAllTypesName() {
        return "Securities/CASH-ALL";
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public String getReportingName() {
        return reportingName;
    }


}
