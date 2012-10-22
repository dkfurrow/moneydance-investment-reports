/* CompositeReport.java
 * Copyright 2012 Dale K. Furrow. All rights reserved.
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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

import com.moneydance.modules.features.invextension.FormattedTable.RowBackground;


/**
 * Aggregator for one or more Security (Leaf-Level) Reports
 *
 * Version 1.0
 * @author Dale Furrow
 *
 * @param <T>
 * @param <U>
 */
public class CompositeReport<T extends Aggregator, U extends Aggregator>
        extends ComponentReport {
    //COMPOSITE_TYPE controls type of Aggregation (i.e. first aggregator only,
    // second aggregator only, or both (i.e. A, B, A && B)
    public static enum COMPOSITE_TYPE { FIRST, SECOND, BOTH, ALL}

    public Class<T> firstAggregateClass; //Class of first Aggregator
    public Class<U> secondAggregateClass; //Class of second Aggregator
    public T firstAggregateVal; //aggregator value of first Aggregate Type
    public U secondAggregateVal;//aggregator value of first Aggregate Type
    public COMPOSITE_TYPE compositeType; //composite type
    // security report which contains aggregated values
    public SecurityReport aggregateReport;
    // Hash set which contains references to SecurityReports aggregated by
    // this composite
    public HashSet<SecurityReport> securityReports;

    /**Generic constructor with null aggregate report value
     * (used to construct All-SecurityReport aggregate
     * @param dateRange
     * @param firstAggClass
     * @param secondAggClass
     */
    public CompositeReport(DateRange dateRange, Class<T> firstAggClass, Class<U> secondAggClass) {
        this.compositeType = COMPOSITE_TYPE.ALL;
        this.firstAggregateClass = firstAggClass;
        this.secondAggregateClass = secondAggClass;
        this.firstAggregateVal = null;
        this.secondAggregateVal = null;

        this.securityReports = new HashSet<SecurityReport>();
        this.aggregateReport = null;

    }

    /**Constructor which creates composite from "seed" SecurityReport
     * @param securityReport
     * @param dateRange
     * @param firstAggClass
     * @param secondAggClass
     * @param compositeType
     */
    public CompositeReport(SecurityReport securityReport, DateRange dateRange,
                           Class<T> firstAggClass, Class<U> secondAggClass,
                           COMPOSITE_TYPE compositeType) {

        this.firstAggregateClass = firstAggClass;
        this.secondAggregateClass = secondAggClass;
        this.compositeType = compositeType;

        switch (compositeType) {
        case FIRST: //aggregate first value only
            this.firstAggregateVal = securityReport.getAggregate(this.firstAggregateClass);
            this.secondAggregateVal = null;
            break;
        case SECOND://aggregate second value only
            this.firstAggregateVal = null;
            this.secondAggregateVal = securityReport.getAggregate(this.secondAggregateClass);
            break;
        case BOTH://aggregate by both first and second value
            this.firstAggregateVal = securityReport.getAggregate(this.firstAggregateClass);
            this.secondAggregateVal = securityReport.getAggregate(this.secondAggregateClass);
            break;
        case ALL: //used for All-Security aggregate
            throw new UnsupportedOperationException();
        }
        this.securityReports = new HashSet<SecurityReport>();
        securityReports.add(securityReport);
        this.aggregateReport = securityReport.getAggregateSecurityReport();
    }


    /** gets Default name for Aggregator (e.g. "All Securities + Cash"
     * for InvestmentAccount, "All Currencies" for CurrencyType)
     * @param aggClass
     * @return
     */
    public static String getDefaultName(Class<? extends Aggregator> aggClass)
        throws SecurityException, NoSuchFieldException,
        IllegalArgumentException, IllegalAccessException {
        Field defaultNameField = aggClass.getDeclaredField("defaultName");
        return (String) defaultNameField.get(String.class);
    }

    @Override
    public void addTo(SecurityReport securityReport) {
        if (this.isCompositeFor(securityReport)) {
            this.aggregateReport.addTo(securityReport);
            this.securityReports.add(securityReport);
            if (this.aggregateReport.getInvAccountWrapper() != null
                && !this.aggregateReport.getInvAccountWrapper().equals(securityReport.getInvAccountWrapper()))
                this.aggregateReport.setInvAccountWrapper(null);
            if (this.aggregateReport.getSecurityTypeWrapper() != null
                && !this.aggregateReport.getSecurityTypeWrapper().equals(securityReport.getSecurityTypeWrapper()))
                this.aggregateReport.setSecuritySubTypeWrapper(null);
            if (this.aggregateReport.getSecuritySubTypeWrapper() != null
                && !this.aggregateReport.getSecuritySubTypeWrapper().equals(securityReport.getSecuritySubTypeWrapper()))
                this.aggregateReport.setSecuritySubTypeWrapper(null);
            if (this.aggregateReport.getTradeable() != null
                && !this.aggregateReport.getTradeable().equals(securityReport.getTradeable()))
                this.aggregateReport.setTradeable(null);
            if (this.aggregateReport.getCurrencyWrapper() != null
                && !this.aggregateReport.getCurrencyWrapper().equals(securityReport.getCurrencyWrapper())){
                this.aggregateReport.setCurrencyWrapper(null);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CompositeReport<?, ?> other = (CompositeReport<?, ?>) obj;
        if (compositeType != other.compositeType)
            return false;
        if (firstAggregateVal == null) {
            if (other.firstAggregateVal != null)
                return false;
        } else if (!firstAggregateVal.equals(other.firstAggregateVal))
            return false;
        if (secondAggregateVal == null) {
            if (other.secondAggregateVal != null)
                return false;
        } else if (!secondAggregateVal.equals(other.secondAggregateVal))
            return false;
        return true;
    }

    public SecurityReport getAggregateReport() {
        return this.aggregateReport;
    }

    public String getName() throws SecurityException, IllegalArgumentException,
        NoSuchFieldException, IllegalAccessException {
        StringBuilder name = new StringBuilder();
        String firstValName = " ";
        String secondValName = " ";

        switch (compositeType) {
        case FIRST:
            firstValName = this.firstAggregateVal.getAllAggregateOutput();
            secondValName = this.secondAggregateVal.getDefaultOutput();
            break;
        case SECOND:
            firstValName = this.firstAggregateVal.getDefaultOutput();
            secondValName = this.secondAggregateVal.getAllAggregateOutput();
            break;
        case BOTH:
            firstValName = this.firstAggregateVal.getFirstAggregateOutput();
            secondValName = this.secondAggregateVal.getSecondAggregateOutput();
            break;
        case ALL:
            firstValName = "ALL-AGGREGATE";
            secondValName = "ALL-AGGREGATE";
        }
        name.append(firstValName).append(": ").append(secondValName).append(": ").append(this.compositeType);
        return name.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((compositeType == null) ? 0 : compositeType.hashCode());
        result = prime
            * result
            + ((firstAggregateVal == null) ? 0 : firstAggregateVal.hashCode());
        result = prime
            * result
            + ((secondAggregateVal == null) ? 0 : secondAggregateVal.hashCode());
        return result;
    }

    /**Determines whether this CompositeReport is a correct composite for
     * a given security based on composite type and aggregate values
     * @param securityReport
     * @return
     */
    public boolean isCompositeFor(SecurityReport securityReport) {
        boolean compositeFor = false;
        if (this.compositeType == COMPOSITE_TYPE.ALL) {
            compositeFor = true;
        } else if (this.compositeType == COMPOSITE_TYPE.SECOND) {
            if (this.secondAggregateVal.equals(securityReport.getAggregate(this.secondAggregateClass)))
                compositeFor = true;
        }

        else if (this.compositeType == COMPOSITE_TYPE.FIRST) {
            if (this.firstAggregateVal.equals(securityReport.getAggregate(this.firstAggregateClass)))
                compositeFor = true;

        } else {
            if (this.firstAggregateVal.equals(securityReport.getAggregate(this.firstAggregateClass))
                && this.secondAggregateVal.equals(securityReport.getAggregate(this.secondAggregateClass)))
                compositeFor = true;

        }
        return compositeFor;
    }

    @Override
    public void recomputeAggregateReturns() {
        this.aggregateReport.recomputeAggregateReturns();
    }

    @Override
    public Object[] toTableRow() throws SecurityException,
        IllegalArgumentException, NoSuchFieldException,
        IllegalAccessException {
        ArrayList<Object> rptValues = new ArrayList<Object>();

        String investmentAccountStr = "Accounts-ALL";
        String securityAccountStr = "Securities-ALL";
        String securityTypeStr = "";
        String securitySubTypeStr = "";
        String tickerStr = "";

        String firstAggStrName = "~Null";
        String secondAggStrName = "~Null";

        if (this.compositeType == COMPOSITE_TYPE.ALL) {
            firstAggStrName = CompositeReport.getDefaultName(this.firstAggregateClass) + " ";
            secondAggStrName = CompositeReport.getDefaultName(this.secondAggregateClass) + " ";
        } else if (this.compositeType == COMPOSITE_TYPE.FIRST) {
            firstAggStrName = this.firstAggregateVal.getAllAggregateOutput();
            secondAggStrName = CompositeReport.getDefaultName(this.secondAggregateClass);
        } else if (this.compositeType == COMPOSITE_TYPE.SECOND) {
            firstAggStrName = CompositeReport.getDefaultName(this.firstAggregateClass);
            secondAggStrName = this.secondAggregateVal.getAllAggregateOutput();
        } else {
            firstAggStrName = this.firstAggregateVal.getFirstAggregateOutput();
            secondAggStrName = this.secondAggregateVal.getSecondAggregateOutput();
        }

        if (this.firstAggregateClass == InvestmentAccountWrapper.class)
            investmentAccountStr = firstAggStrName;
        if (this.firstAggregateClass == SecurityTypeWrapper.class)
            securityTypeStr = firstAggStrName;
        if (this.firstAggregateClass == SecuritySubTypeWrapper.class)
            securitySubTypeStr = firstAggStrName;
        if (this.firstAggregateClass == Tradeable.class)
            securityAccountStr = firstAggStrName;
        if (this.firstAggregateClass == CurrencyWrapper.class
            && this.compositeType != COMPOSITE_TYPE.ALL) {
            tickerStr = firstAggStrName;
            //take remaining information from abitrary member of security reports
            //covers case where SecurityType, etc, different for same
            //currency (SecurityType is property of Security)
            Iterator<SecurityReport> iterator = securityReports.iterator();
            SecurityReport securityReport = iterator.next();
            securityTypeStr = securityReport.getSecurityTypeWrapper().getName();
            securitySubTypeStr = securityReport.getSecuritySubTypeWrapper().getName();
            securityAccountStr = securityReport.getSecAccountWrapper().getName();
        }
        if (this.firstAggregateClass == CurrencyWrapper.class
            && this.compositeType == COMPOSITE_TYPE.ALL) {
            tickerStr = firstAggStrName;
            // add space to investment account column to mimic 2-level report
            investmentAccountStr = investmentAccountStr + " ";
        }


        if (this.secondAggregateClass == InvestmentAccountWrapper.class)
            investmentAccountStr = secondAggStrName;
        if (this.secondAggregateClass == SecurityTypeWrapper.class)
            securityTypeStr = secondAggStrName;
        if (this.secondAggregateClass == SecuritySubTypeWrapper.class)
            securitySubTypeStr = secondAggStrName;
        if (this.secondAggregateClass == Tradeable.class){
            securityAccountStr = secondAggStrName;
        }
        //can't have currency as second aggregate, so not included here

        rptValues.add(investmentAccountStr);
        rptValues.add(securityAccountStr);
        rptValues.add(securityTypeStr);
        rptValues.add(securitySubTypeStr);
        rptValues.add(tickerStr);

        this.aggregateReport.addLineBody(rptValues);

        return rptValues.toArray();
    }


    @Override
    public RowBackground getTableRowBackground() {
        switch(compositeType) {
        case FIRST:
            return RowBackground.LIGHTGRAY;

        case SECOND:
            return RowBackground.LIGHTGRAY;

        case BOTH:
            return RowBackground.LIGHTLIGHTGRAY;

        case ALL:
            return RowBackground.GREEN;

        default:
            throw new AssertionError(compositeType);
        }
    }
}
