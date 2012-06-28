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
    public DateRange dateRange;
    public SecurityAccountWrapper secAccountWrapper; 
    public InvestmentAccountWrapper invAccountWrapper;
    public Tradeable tradeable;
    public CurrencyWrapper currencyWrapper;
    public SecurityTypeWrapper securityTypeWrapper;
    public SecuritySubTypeWrapper securitySubTypeWrapper;
    
    
    
    /**Generic constructor populates all members based on secAccountWrapper
     * or sets all to null
     * @param secAccountWrapper
     * @param dateRange
     */
    public SecurityReport(SecurityAccountWrapper secAccountWrapper,
	    DateRange dateRange)  {
	this.dateRange = dateRange;
	if (secAccountWrapper != null) {
	    this.secAccountWrapper = secAccountWrapper;
	    this.invAccountWrapper = secAccountWrapper.invAcctWrapper;
	    this.tradeable = secAccountWrapper.tradeable;
	    this.currencyWrapper = secAccountWrapper.currWrapper;
	    this.securityTypeWrapper = secAccountWrapper.securityTypeWrapper;
	    this.securitySubTypeWrapper = secAccountWrapper.securitySubTypeWrapper;
	    
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
     * which subclasses AggregatingType
     * @param aggregateClass
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getAggregate(Class<? extends AggregatingType> aggregateClass) {
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
     * @param compType
     * @return
     */
    public abstract <T extends AggregatingType, U extends AggregatingType> 
    CompositeReport<T, U> getCompositeReport(Class<T> firstAggClass, 
	    Class<U> secondAggClass, COMPOSITE_TYPE compType);    
    
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

}





