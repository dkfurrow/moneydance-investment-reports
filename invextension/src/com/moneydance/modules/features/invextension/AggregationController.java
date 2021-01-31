/*
 * AggregationController.java
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

/**
 * Aggregation controller sets overall method for aggregation for a given report
 */
public enum AggregationController {
    INVACCT("Investment Account, then Securities/Cash", new InvestmentAccountWrapper(), new Tradeable()),
    TICKER("Ticker", new CurrencyWrapper(), new InvestmentAccountWrapper()),
    SECTYPE("Security Type, then Security SubType", new SecurityTypeWrapper(), new SecuritySubTypeWrapper());
    private final String description;
    private final Aggregator firstAggregator;
    private final Aggregator secondAggregator;
    AggregationController(String description, Aggregator firstAggregator, Aggregator secondAggregator) {
        this.description = description;
        this.firstAggregator = firstAggregator;
        this.secondAggregator = secondAggregator;
    }

    public String getDescription() {
        return description;
    }

    public Aggregator getFirstAggregator() {
        return firstAggregator;
    }

    public Aggregator getSecondAggregator() {
        return secondAggregator;
    }

    public boolean isHierarchy() {
        return (this.firstAggregator instanceof SecurityTypeWrapper &&
                this.secondAggregator instanceof SecuritySubTypeWrapper);
    }

    @Override
    public String toString() {
        return this.description;
    }


}

