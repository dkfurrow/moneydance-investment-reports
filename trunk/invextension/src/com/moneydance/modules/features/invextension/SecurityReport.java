/*
 * SecurityReport.java
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

import com.moneydance.apps.md.model.CurrencyType;
import com.moneydance.modules.features.invextension.CompositeReport.COMPOSITE_TYPE;

import java.util.ArrayList;


/**
 * Generic SecurityReport, generates one data set for each Security in each
 * investment account
 * Version 1.0
 *
 * @author Dale Furrow
 */
public abstract class SecurityReport extends ComponentReport {
    protected ArrayList<Object> outputLine = new ArrayList<>();
    private DateRange dateRange;
    private SecurityAccountWrapper securityAccountWrapper;
    private InvestmentAccountWrapper investmentAccountWrapper;
    private Tradeable tradeable;
    private CurrencyWrapper currencyWrapper;
    private SecurityTypeWrapper securityTypeWrapper;
    private SecuritySubTypeWrapper securitySubTypeWrapper;


    /**
     * Generic constructor populates all members based on securityAccountWrapper
     * or sets all to null
     *
     * @param securityAccountWrapper input security account wrapper
     * @param dateRange              input date range
     */
    public SecurityReport(SecurityAccountWrapper securityAccountWrapper,
                          DateRange dateRange) {
        this.dateRange = dateRange;
        if (securityAccountWrapper != null) {
            this.securityAccountWrapper = securityAccountWrapper;
            this.investmentAccountWrapper = securityAccountWrapper.getInvAcctWrapper();
            this.tradeable = securityAccountWrapper.getTradeable();
            this.currencyWrapper = securityAccountWrapper.getCurrencyWrapper();
            this.securityTypeWrapper = securityAccountWrapper
                    .getSecurityTypeWrapper();
            this.securitySubTypeWrapper = securityAccountWrapper
                    .getSecuritySubTypeWrapper();
            outputLine.add(investmentAccountWrapper);
            outputLine.add(securityAccountWrapper);
            outputLine.add(securityTypeWrapper);
            outputLine.add(securitySubTypeWrapper);
            outputLine.add(currencyWrapper);

        } else {
            this.securityAccountWrapper = null;
            this.investmentAccountWrapper = null;
            this.tradeable = null;
            this.currencyWrapper = null;
            this.securityTypeWrapper = null;
            this.securitySubTypeWrapper = null;

        }

    }

    /**
     * returns Aggregate value for Security based on an input of any Class
     * which subclasses Aggregator
     *
     * @param aggregator subclass of Aggregator
     * @return aggregate value
     */

    public Aggregator getAggregator(Aggregator aggregator) {
        if (aggregator instanceof InvestmentAccountWrapper) {
            return this.investmentAccountWrapper;
        } else if (aggregator instanceof SecurityTypeWrapper) {
            return this.securityTypeWrapper;
        } else if (aggregator instanceof SecuritySubTypeWrapper) {
            return this.securitySubTypeWrapper;
        } else if (aggregator instanceof Tradeable) {
            return this.tradeable;
        } else if (aggregator instanceof CurrencyWrapper) {
            return this.currencyWrapper;
        } else if (aggregator instanceof AllAggregate) {
            return AllAggregate.getInstance();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public double getSplitAdjustedPosition(double referencePosition, int referenceDateInt,
                                           int currentDateInt) {
        CurrencyType currency = currencyWrapper.currencyType;
        double currentRate = currency == null ? 1.0 : currency
                .getUserRateByDateInt(currentDateInt);
        double splitAdjust = currency == null ? 1.0 : currency
                .adjustRateForSplitsInt(referenceDateInt,
                        currentRate, currentDateInt) / currentRate;
        return referencePosition * splitAdjust;
    }

    /**
     * Generates composite report consistent with this SecurityReport
     *
     * @param aggregationController input aggregation mode
     * @param compType              Composite Type
     * @return Composite Report consistent with this security
     */
    public abstract CompositeReport getCompositeReport(AggregationController aggregationController,
                                                       COMPOSITE_TYPE compType);

    public ArrayList<Object> getOutputLine() {
        return outputLine;
    }

    /**
     * generates aggregate security report
     *
     * @return appropriate Security Report
     */
    public abstract SecurityReport getAggregateSecurityReport();

    public abstract String getName();

    public abstract void addLineBody();

    public SecurityAccountWrapper getSecurityAccountWrapper() {
        return securityAccountWrapper;
    }

    public void setSecurityAccountWrapper(SecurityAccountWrapper securityAccountWrapper) {
        this.securityAccountWrapper = securityAccountWrapper;
    }

    public Tradeable getTradeable() {
        return tradeable;
    }

    public void setTradeable(Tradeable tradeable) {
        this.tradeable = tradeable;
    }

    public CurrencyWrapper getCurrencyWrapper() {
        return currencyWrapper;
    }

    public void setCurrencyWrapper(CurrencyWrapper currencyWrapper) {
        this.currencyWrapper = currencyWrapper;
    }

    public SecurityTypeWrapper getSecurityTypeWrapper() {
        return securityTypeWrapper;
    }

    public void setSecurityTypeWrapper(SecurityTypeWrapper securityTypeWrapper) {
        this.securityTypeWrapper = securityTypeWrapper;
    }

    public SecuritySubTypeWrapper getSecuritySubTypeWrapper() {
        return securitySubTypeWrapper;
    }

    public void setSecuritySubTypeWrapper(
            SecuritySubTypeWrapper securitySubTypeWrapper) {
        this.securitySubTypeWrapper = securitySubTypeWrapper;
    }

    public InvestmentAccountWrapper getInvestmentAccountWrapper() {
        return investmentAccountWrapper;
    }

    public void setInvestmentAccountWrapper(InvestmentAccountWrapper investmentAccountWrapper) {
        this.investmentAccountWrapper = investmentAccountWrapper;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

}





