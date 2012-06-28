/* SecurityFromToReport.java
 * Copyright 2012 Dale Furrow . All rights reserved.
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
import java.util.Iterator;
import java.util.SortedSet;

import com.moneydance.apps.md.model.CurrencyType;
import com.moneydance.modules.features.invextension.CompositeReport.COMPOSITE_TYPE;


/**
 * Report detailing performance attributes based on a specific "from" and
 * "to" date
 * Version 1.0 
 * @author Dale Furrow
 */
public class SecurityFromToReport extends SecurityReport {
    public int fromDateInt;            // start of report period
    public int toDateInt;              // end of report period

    public double startPos;            // starting position
    public double endPos;              // ending position
    public double startPrice;          // starting price
    public double endPrice;            // ending price
    public double startValue;          // starting value
    public double endValue;            // ending value
    
    public double buy;                 // cumulative cash effect of buys (including commission)
    public double sell;                // cumulative cash effect of sells (including commission)
    public double shortSell;           // cumulative cash effect of shorts (including commission)
    public double coverShort;          // cumulative cash effect of covers (including commission)

    public double income;              // cumulative income
    public double expense;             // cumulative expense

    public double longBasis;           // ending average cost basis of long positions
    public double shortBasis;          // ending average cost basis of short positions
    public double realizedGain;        // cumulative realized gains (against avg cost)
    public double unrealizedGain;      // cumulative unrealized gains
    public double totalGain;           // sum of realized and unrealized gains

    public double mdReturn;            // period total return (Mod-Dietz method)
    public double annualPercentReturn; // period annualized return (Mod-Dietz method)
    
    public DateMap arMap;              // date map of annual return data
    public DateMap mdMap;              // date map of Mod-Dietz return data
    public DateMap transMap;           // date map of transfer data

    
    /**
     * Generic constructor, which produces either the SecurityReport associated
     * with a given SecurityAccountWrapper or a blank report
     * @param secAccountWrapper reference account
     * @param fromDateInt "from" date
     * @param toDateInt   "to" date
     * @throws Exception
     */
    public SecurityFromToReport(SecurityAccountWrapper secAccountWrapper,
	    DateRange dateRange) {

	super(secAccountWrapper, dateRange);

	this.fromDateInt = dateRange.fromDateInt();
	this.toDateInt = dateRange.toDateInt();
        
	this.startPos = 0.0;
        this.endPos = 0.0;
        this.startPrice = 0.0;
        this.endPrice = 0.0;
        this.startValue = 0.0;
        this.endValue = 0.0;        

        this.buy = 0.0;
        this.sell = 0.0;
        this.shortSell = 0.0;
        this.coverShort = 0.0;

        this.income = 0.0;
        this.expense = 0.0;

        this.longBasis = 0.0;
        this.shortBasis = 0.0;
        this.realizedGain = 0.0;
        this.unrealizedGain = 0.0;
        this.totalGain = 0.0;

        this.mdReturn = 0.0;
        this.annualPercentReturn = 0.0;

        this.arMap = new DateMap();
        this.mdMap = new DateMap();
        this.transMap = new DateMap();
        
        if (secAccountWrapper != null) {
            
            // Currency will never be null if accountWrapper is cash or security
            CurrencyType currency = secAccountWrapper.getCurrWrapper().curType; 
            
            this.startPrice = 1.0 / currency.getUserRateByDateInt(fromDateInt);
            this.endPrice = 1.0 / currency.getUserRateByDateInt(toDateInt);            

            // intialize intermediate calculation variables
            double startCumUnrealGain = 0;
            double endCumUnrealizedGain = 0;
            double startLongBasis = 0;
            double startShortBasis = 0;
            double toDateRate = currency.getUserRateByDateInt(toDateInt);
            double fromDateRate = currency == null ? 1.0
                : currency.getUserRateByDateInt(fromDateInt);
            
            SortedSet<TransValues> transSet = secAccountWrapper.getTransValues();

            // iterates through full transaction set
            for (Iterator<TransValues> it = transSet.iterator(); it.hasNext();) {
                TransValues transValues = it.next();
                
                double totalFlows = transValues.buy + transValues.sell
                	+ transValues.shortSell + transValues.coverShort +
                	transValues.commision + transValues.income +
                	transValues.expense;

                // Where transactions are before report dates
                if (transValues.dateint <= fromDateInt) {
                    double splitAdjust = currency == null
                        ? 1.0
                        : currency.adjustRateForSplitsInt(transValues.dateint,
                                                          fromDateRate,
                                                          fromDateInt) / fromDateRate;
                    // split adjusts last position from TransValuesCum
                    this.startPos = transValues.position * splitAdjust;
                    this.startValue = this.startPrice * this.startPos;
                    startLongBasis = transValues.longBasis;
                    startShortBasis = transValues.shortBasis;

                    // Initializes ending balance sheet values to start values (in
                    // case there are no transactions within report period).
                    this.endPos = this.startPos;
                    this.endValue = this.endPos * this.endPrice;
                    this.longBasis = transValues.longBasis;
                    this.shortBasis = transValues.shortBasis;
                }
                // Where transaction period intersects report period
                if (transValues.dateint > fromDateInt
                    && transValues.dateint <= toDateInt) {
                    // cf is net cash effect of buy/sell/short/cover, incl
                    // commission
                    double cf = -(transValues.buy
                                  + transValues.sell
                                  + transValues.shortSell
                                  + transValues.coverShort +
                                  transValues.commision);

                    // add values to date maps
                    this.arMap.add(transValues.dateint,
                                   totalFlows);
                    this.mdMap.add(transValues.dateint, cf);
                    this.transMap.add(transValues.dateint, transValues.transfer);

                    // Add the cumulative Values (note buys are defined by change in
                    // long basis, same with sells--commission is included).
                    this.buy += transValues.buy == 0.0
                        ? 0.0
                        : -transValues.buy - transValues.commision;
                    this.sell += transValues.sell == 0.0
                        ? 0.0
                        : -transValues.sell - transValues.commision;
                    this.shortSell += transValues.shortSell == 0.0
                        ? 0.0
                        : -transValues.shortSell - transValues.commision;
                    this.coverShort += transValues.coverShort == 0.0
                        ? 0.0
                        : -transValues.coverShort - transValues.commision;
                    this.income += transValues.income;
                    this.expense += transValues.expense;
                    this.realizedGain += transValues.perRealizedGain;

                    // retrieves ending balance sheet variables
                    double splitAdjust = currency == null
                        ? 1.0 : currency.adjustRateForSplitsInt
                        	(transValues.dateint, toDateRate,
                        		toDateInt) / toDateRate;
                    
                    this.endPos = transValues.position * splitAdjust;
                    this.endValue = this.endPos * this.endPrice;
                    this.longBasis = transValues.longBasis;
                    this.shortBasis = transValues.shortBasis;

                } // end--where transaction period intersects report period
            } // end of input transaction set loop

            // Calculate the total period unrealized gain
            if (this.startPos > 0) {
                startCumUnrealGain = this.startValue - startLongBasis;
            } else if (this.startPos < 0) {
                startCumUnrealGain = this.startValue - startShortBasis;
            }

            if (this.endPos > 0) {
                endCumUnrealizedGain = this.endValue - this.longBasis;
            } else if (this.endPos < 0) {
                endCumUnrealizedGain = this.endValue - this.shortBasis;
            }
            this.unrealizedGain = endCumUnrealizedGain - startCumUnrealGain;
            this.totalGain = this.realizedGain + this.unrealizedGain;

            // Get performance data--first Mod Dietz Returns
            
            // Add the first value in return arrays (if startpos != 0)
            if (this.startPos != 0) {
                this.arMap.add(fromDateInt, -this.startValue);
                this.mdMap.add(fromDateInt, 0.0); // adds dummy value for mod-dietz
            }
            // add the last value in return arrays (if endpos != 0)
            if (this.endPos != 0) {
                this.arMap.add(toDateInt, this.endValue);
                this.mdMap.add(toDateInt, 0.0); // adds dummy value for mod-dietz
            }

            this.mdReturn = computeMDReturn(this.startValue, this.endValue, this.income,
                                      this.expense, this.mdMap);
            // Now get annualized returns
            this.annualPercentReturn = computeAnnualReturn(this.arMap, this.mdReturn);

            // Remove start and end values from ar date map to enable aggregation
            if (this.startPos != 0) {
                this.arMap.add(fromDateInt, +this.startValue);
            }
            // Remove start and end values from date map for ease of aggregation
            if (this.endPos != 0) {
                this.arMap.add(toDateInt, -this.endValue);
            }
        }
    }
    
    @Override
    public void addTo(SecurityReport securityReport)  {
        SecurityFromToReport securityFromToReport = (SecurityFromToReport)securityReport;
        
	if (this.secAccountWrapper != null)
	    throw new UnsupportedOperationException(
		    "Illegal call to addTo method for SecurityReport");
        //if CurrencyWrappers are the same then prices and positions can be
	//added--if not, set prices and positions to zero
	if (this.currencyWrapper != null
		&& securityFromToReport.currencyWrapper != null
		&& this.currencyWrapper
			.equals(securityFromToReport.currencyWrapper)) {
	    this.startPos += securityFromToReport.startPos;
	    this.endPos += securityFromToReport.endPos;
	    this.startPrice = securityFromToReport.startPrice;
	    this.endPrice = securityFromToReport.endPrice;

	} else {
	    this.startPos = 0.0;
	    this.endPos = 0.0;
	    this.startPrice = 0.0;
	    this.endPrice = 0.0;
	}
	
	//populate other values from this SecurityReport
        this.startValue += securityFromToReport.startValue;
        this.endValue +=  securityFromToReport.endValue;
        this.buy += securityFromToReport.buy;
        this.sell += securityFromToReport.sell;
        this.shortSell += securityFromToReport.shortSell;
        this.coverShort += securityFromToReport.coverShort;
        this.income += securityFromToReport.income;
        this.expense += securityFromToReport.expense;
        this.longBasis += securityFromToReport.longBasis;
        this.shortBasis += securityFromToReport.shortBasis;
        this.realizedGain += securityFromToReport.realizedGain;
        this.unrealizedGain += securityFromToReport.unrealizedGain;
        this.totalGain += securityFromToReport.totalGain;
        
        this.arMap = this.arMap.combine(securityFromToReport.arMap, "add");
        this.mdMap = this.mdMap.combine(securityFromToReport.mdMap, "add");
        this.transMap = this.transMap.combine(securityFromToReport.transMap, "add");
        //set returns to zero
        this.mdReturn = 0.0;
        this.annualPercentReturn = 0.0;
        }
    
    @Override
    public Object[] toTableRow() throws SecurityException,
	    IllegalArgumentException, NoSuchFieldException,
	    IllegalAccessException {
	
	ArrayList<Object> snapValues = new ArrayList<Object>();

	snapValues.add(this.invAccountWrapper.getName());
	snapValues.add(this.secAccountWrapper.getName());
	snapValues.add(this.securityTypeWrapper.getName());
	snapValues.add(this.securitySubTypeWrapper.getName());
	snapValues.add(this.currencyWrapper.ticker);

	addLineBody(snapValues);
	
	return snapValues.toArray();
    }

    @Override
    public void addLineBody(ArrayList<Object> rptValues) {
	rptValues.add(this.startPos);
	rptValues.add(this.endPos);
	rptValues.add(this.startPrice);
	rptValues.add(this.endPrice);
	rptValues.add(this.startValue);
	rptValues.add(this.endValue);
	rptValues.add(this.buy);
	rptValues.add(this.sell);
	rptValues.add(this.shortSell);
	rptValues.add(this.coverShort);
	rptValues.add(this.income);
	rptValues.add(this.expense);
	rptValues.add(this.longBasis);
	rptValues.add(this.shortBasis);
	rptValues.add(this.realizedGain);
	rptValues.add(this.unrealizedGain);
	rptValues.add(this.totalGain);
	rptValues.add(this.mdReturn);
	rptValues.add(this.annualPercentReturn);
	
    }
    
    
    @Override
    public void recomputeAggregateReturns() {
	// get Mod-Dietz Returns
	double mdReturnVal = computeMDReturn(startValue, endValue, income,
		expense, mdMap);

	// SecurityFromToReport.mdReturn = thisReturn;
	mdReturn = mdReturnVal;

	// add start and end values to return date maps
	if (startValue != 0) {
	    arMap.add(fromDateInt, -startValue);
	}
	if (endValue != 0) {
	    arMap.add(toDateInt, endValue);
	}
	// get annualized returns
	annualPercentReturn = computeAnnualReturn(arMap, mdReturnVal);
    }

    @Override
    public <T extends AggregatingType, U extends AggregatingType> CompositeReport<T, U> getCompositeReport(
	    Class<T> firstAggClass, Class<U> secondAggClass,
	    COMPOSITE_TYPE compType) {
	CompositeReport<T, U> thisComposite = new CompositeReport<T, U>(this,
		this.dateRange, firstAggClass, secondAggClass, compType);
	return thisComposite;
    }

    @Override
    public SecurityReport getAggregateSecurityReport() {
	SecurityFromToReport thisAggregate = new SecurityFromToReport(null, dateRange);
	//add report body values (except Returns)
	thisAggregate.startPos = this.startPos;
	thisAggregate.endPos = this.endPos;
	thisAggregate.startPrice = this.startPrice;
	thisAggregate.endPrice = this.endPrice;
	thisAggregate.startValue = this.startValue;
	thisAggregate.endValue = this.endValue;
	thisAggregate.buy = this.buy;
	thisAggregate.sell = this.sell;
	thisAggregate.shortSell = this.shortSell;
	thisAggregate.coverShort = this.coverShort;
	thisAggregate.income = this.income;
	thisAggregate.expense = this.expense;
	thisAggregate.longBasis = this.longBasis;
	thisAggregate.shortBasis = this.shortBasis;
	thisAggregate.realizedGain = this.realizedGain;
	thisAggregate.unrealizedGain = this.unrealizedGain;
	thisAggregate.totalGain = this.totalGain;
	
	thisAggregate.mdMap = this.mdMap;
	thisAggregate.arMap = this.arMap;
	thisAggregate.transMap = this.transMap;
	
	//make aggregating classes the same except secAccountWrapper
	thisAggregate.invAccountWrapper = this.invAccountWrapper;
	thisAggregate.secAccountWrapper = null;
	thisAggregate.securityTypeWrapper = this.securityTypeWrapper;
	thisAggregate.securitySubTypeWrapper = this.securitySubTypeWrapper;
	thisAggregate.tradeable = this.tradeable;
	thisAggregate.currencyWrapper = this.currencyWrapper;
	
	return thisAggregate;
    }

    @Override
    public String getName() {
	if (this.secAccountWrapper == null) {
	    return "Null SecAccountWrapper";
	} else {
	    return this.invAccountWrapper.getName() + ": "
		    + this.secAccountWrapper.getAccountName();
	}
    }

}
