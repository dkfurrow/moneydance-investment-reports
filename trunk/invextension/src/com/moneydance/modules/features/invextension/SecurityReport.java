/* SecurityReport.java
 * Copyright 2012 Dale K. Furrow . All rights reserved.
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

import com.moneydance.modules.features.invextension.CompositeReport.COMPOSITE_TYPE;


/**Generic SecurityReport, generates one data set for each Security in each
 * investment account
 * Version 1.0
 * @author Dale Furrow
 *
 */

public abstract class SecurityReport extends ComponentReport {
    private DateRange dateRange;
    private SecurityAccountWrapper secAccountWrapper;
    private InvestmentAccountWrapper invAccountWrapper;
    private Tradeable tradeable;
    private CurrencyWrapper currencyWrapper;
    private SecurityTypeWrapper securityTypeWrapper;
    private SecuritySubTypeWrapper securitySubTypeWrapper;


    /**Generic constructor populates all members based on secAccountWrapper
     * or sets all to null
     * @param secAccountWrapper
     * @param dateRange
     */
    public SecurityReport(SecurityAccountWrapper secAccountWrapper, DateRange dateRange)  {
        this.dateRange = dateRange;
        if (secAccountWrapper != null) {
            this.secAccountWrapper = secAccountWrapper;
            this.invAccountWrapper = secAccountWrapper.getInvAcctWrapper();
            this.tradeable = secAccountWrapper.getTradeable();
            this.currencyWrapper = secAccountWrapper.getCurrencyWrapper();
            this.securityTypeWrapper = secAccountWrapper
                .getSecurityTypeWrapper();
            this.securitySubTypeWrapper = secAccountWrapper.getSecuritySubTypeWrapper();
        
        } else {
            this.secAccountWrapper = null;
            this.invAccountWrapper = null;
            this.tradeable = null;
            this.currencyWrapper = null;
            this.securityTypeWrapper = null;
            this.securitySubTypeWrapper = null;
        
        }

    }

    /**returns Aggregate value for Security based on an input of any Class
     * which subclasses Aggregator
     * @param aggregateClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getAggregate(Class<? extends Aggregator> aggregateClass) {
        if (aggregateClass == InvestmentAccountWrapper.class) {
            return (T) this.invAccountWrapper;
        } else if (aggregateClass == SecurityTypeWrapper.class) {
            return (T) this.securityTypeWrapper;
        } else if (aggregateClass == SecuritySubTypeWrapper.class) {
            return (T) this.securitySubTypeWrapper;
        } else if (aggregateClass == Tradeable.class) {
            return (T) this.tradeable;
        } else if (aggregateClass == CurrencyWrapper.class) {
            return (T) this.currencyWrapper;    
        } else if (aggregateClass == AllAggregate.class) {
            return (T) AllAggregate.getInstance();      
        }else {
            throw new UnsupportedOperationException();
        }
    }

    /**Generates composite report consistent with this SecurityReport
     * @param firstAggClass
     * @param secondAggClass
     * @param compositeType
     * @return
     */
    public abstract <T extends Aggregator, U extends Aggregator>
                                             CompositeReport<T, U> getCompositeReport(Class<T> firstAggClass,
                                                                                      Class<U> secondAggClass,
                                                                                      COMPOSITE_TYPE compType);

    /**Generates aggregateSecurity member of composite report based on the
     * fields of this SecurityReport
     * @return
     */
    public abstract SecurityReport getAggregateSecurityReport();

    public abstract String getName();

    /**adds line body (unique fields for classes which subclass SecurityReport)
     * @param rptValues
     */
    public abstract void addLineBody(ArrayList<Object> rptValues);

    public SecurityAccountWrapper getSecAccountWrapper() {
        return secAccountWrapper;
    }

    public Tradeable getTradeable() {
        return tradeable;
    }

    public CurrencyWrapper getCurrencyWrapper() {
        return currencyWrapper;
    }

    public SecurityTypeWrapper getSecurityTypeWrapper() {
        return securityTypeWrapper;
    }

    public SecuritySubTypeWrapper getSecuritySubTypeWrapper() {
        return securitySubTypeWrapper;
    }

    public InvestmentAccountWrapper getInvAccountWrapper() {
        return invAccountWrapper;
    }

    public DateRange getDateRange(){
        return dateRange;
    }

    public void setSecAccountWrapper(SecurityAccountWrapper secAccountWrapper) {
        this.secAccountWrapper = secAccountWrapper;
    }

    public void setInvAccountWrapper(InvestmentAccountWrapper invAccountWrapper) {
        this.invAccountWrapper = invAccountWrapper;
    }

    public void setTradeable(Tradeable tradeable) {
        this.tradeable = tradeable;
    }

    public void setSecurityTypeWrapper(SecurityTypeWrapper securityTypeWrapper) {
        this.securityTypeWrapper = securityTypeWrapper;
    }

    public void setSecuritySubTypeWrapper(
                                          SecuritySubTypeWrapper securitySubTypeWrapper) {
        this.securitySubTypeWrapper = securitySubTypeWrapper;
    }

    public void setCurrencyWrapper(CurrencyWrapper currencyWrapper){
        this.currencyWrapper = currencyWrapper;
    }

}





