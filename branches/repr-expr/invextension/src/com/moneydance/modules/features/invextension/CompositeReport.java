/*
 * CompositeReport.java
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

import java.util.ArrayList;
import java.util.HashSet;

/**
 * Aggregator for one or more Security (Leaf-Level) Reports
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */
public class CompositeReport extends ComponentReport {

    private AggregationController aggregationController;
    private Aggregator firstAggregator; //aggregator value of first Aggregate Type
    private Aggregator secondAggregator;//aggregator value of first Aggregate Type
    private COMPOSITE_TYPE compositeType; //composite type
    // security report which contains aggregated values
    private SecurityReport aggregateReport;
    // Hash set which contains references to SecurityReports aggregated by
    // this composite
    private HashSet<SecurityReport> securityReports;

    /**
     * Constructor which creates composite from "seed" SecurityReport
     *
     * @param securityReport        security report which requires composite
     * @param aggregationController input AggregationMode
     * @param compositeType         input Composite Type
     */
    public CompositeReport(SecurityReport securityReport, AggregationController aggregationController,
                           COMPOSITE_TYPE compositeType) {
        this.aggregationController = aggregationController;
        this.compositeType = compositeType;

        switch (compositeType) {
            case FIRST: //aggregate first value only
                this.firstAggregator = securityReport.getAggregator(aggregationController.getFirstAggregator());
                this.secondAggregator = aggregationController.getSecondAggregator();
                break;
            case SECOND://aggregate second value only
                this.firstAggregator = aggregationController.getFirstAggregator();
                this.secondAggregator = securityReport.getAggregator(aggregationController.getSecondAggregator());
                break;
            case BOTH://aggregate by both first and second value
                this.firstAggregator = securityReport.getAggregator(aggregationController.getFirstAggregator());
                this.secondAggregator = securityReport.getAggregator(aggregationController.getSecondAggregator());
                break;
            case ALL: //used for All-Security aggregate
                throw new UnsupportedOperationException();
        }
        this.securityReports = new HashSet<>();
        if(securityReport != null) {
            securityReports.add(securityReport);
            this.aggregateReport = securityReport.getAggregateSecurityReport(this);
        }
    }

    /**
     * Creates "All" Composite report
     * @param aggregationController report config aggregation controller
     */
    public CompositeReport(AggregationController aggregationController){
        this.aggregationController = aggregationController;
        this.compositeType = COMPOSITE_TYPE.ALL;
        this.firstAggregator = null;
        this.secondAggregator = null;
        this.securityReports = new HashSet<>();
        this.aggregateReport = null;
    }




    public HashSet<SecurityReport> getSecurityReports() {
        return securityReports;
    }

    public Aggregator getFirstAggregator() {
        return firstAggregator;
    }

    public Aggregator getSecondAggregator() {
        return secondAggregator;
    }

    public SecurityReport getAggregateReport() {

        return aggregateReport;
    }

    public void setAggregateReport(SecurityReport aggregateReport) {
        this.aggregateReport = aggregateReport;
    }

    public COMPOSITE_TYPE getCompositeType() {
        return compositeType;
    }

    @Override
    public void addTo(SecurityReport securityReport) {
        if (this.isCompositeFor(securityReport)) {
            this.aggregateReport.addTo(securityReport);
            this.securityReports.add(securityReport);
            if (this.aggregateReport.getInvestmentAccountWrapper() != null
                    && !this.aggregateReport.getInvestmentAccountWrapper()
                    .equals(securityReport.getInvestmentAccountWrapper()))
                this.aggregateReport.setInvestmentAccountWrapper(null);

            if (this.aggregateReport.getSecurityTypeWrapper() != null
                    && !this.aggregateReport.getSecurityTypeWrapper()
                    .equals(securityReport.getSecurityTypeWrapper()))
                this.aggregateReport.setSecuritySubTypeWrapper(null);

            if (this.aggregateReport.getSecuritySubTypeWrapper() != null
                    && !this.aggregateReport.getSecuritySubTypeWrapper()
                    .equals(securityReport.getSecuritySubTypeWrapper()))
                this.aggregateReport.setSecuritySubTypeWrapper(null);

            if (this.aggregateReport.getTradeable() != null
                    && !this.aggregateReport.getTradeable()
                    .equals(securityReport.getTradeable()))
                this.aggregateReport.setTradeable(null);

            if (this.aggregateReport.getCurrencyWrapper() != null
                    && !this.aggregateReport.getCurrencyWrapper()
                    .equals(securityReport.getCurrencyWrapper())) {
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
        CompositeReport other = (CompositeReport) obj;
        if (compositeType != other.compositeType)
            return false;
        if (firstAggregator == null) {
            if (other.firstAggregator != null)
                return false;
        } else if (!firstAggregator.equals(other.firstAggregator))
            return false;
        if (secondAggregator == null) {
            if (other.secondAggregator != null)
                return false;
        } else if (!secondAggregator.equals(other.secondAggregator))
            return false;
        return true;
    }

    public String getName() throws SecurityException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException {
        StringBuilder name = new StringBuilder();
        String firstAggStrName;
        String secondAggStrName;

        if (this.compositeType == COMPOSITE_TYPE.ALL) {
            firstAggStrName = aggregationController.getFirstAggregator().getAllTypesName();
            secondAggStrName = aggregationController.getSecondAggregator().getAllTypesName();
        } else if (this.compositeType == COMPOSITE_TYPE.FIRST) {
            firstAggStrName = firstAggregator.getAggregateName() + " ";
            secondAggStrName = secondAggregator.getAllTypesName();
        } else if (this.compositeType == COMPOSITE_TYPE.SECOND) {
            firstAggStrName = firstAggregator.getAllTypesName();
            secondAggStrName = secondAggregator.getAggregateName();
        } else { //"Both" Case
            firstAggStrName = firstAggregator.getAggregateName();
            secondAggStrName = secondAggregator.getAggregateName();
        }
        name.append(firstAggStrName).append(": ").append(secondAggStrName)
                .append(": ").append(this.compositeType);
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
                + ((firstAggregator == null) ? 0 : firstAggregator
                .hashCode());
        result = prime
                * result
                + ((secondAggregator == null) ? 0 : secondAggregator
                .hashCode());
        return result;
    }

    /**
     * Determines whether this CompositeReport is a correct composite for
     * a given security based on composite type and aggregate values
     *
     * @param securityReport security report to test
     * @return test of whether the security report aggregates into this
     * composite report
     */
    public boolean isCompositeFor(SecurityReport securityReport) {
        boolean compositeFor = false;
        if (this.compositeType == COMPOSITE_TYPE.ALL) {
            compositeFor = true;
        } else if (this.compositeType == COMPOSITE_TYPE.SECOND) {
            if (this.secondAggregator.equals(securityReport
                    .getAggregator(aggregationController.getSecondAggregator())))
                compositeFor = true;
        } else if (this.compositeType == COMPOSITE_TYPE.FIRST) {
            if (this.firstAggregator.equals(securityReport
                    .getAggregator(aggregationController.getFirstAggregator())))
                compositeFor = true;

        } else {
            if (this.firstAggregator.equals(securityReport
                    .getAggregator(aggregationController.getFirstAggregator()))
                    && this.secondAggregator.equals(securityReport
                    .getAggregator(aggregationController.getSecondAggregator())))
                compositeFor = true;

        }
        return compositeFor;
    }

    @Override
    public Object[] toTableRow() throws SecurityException,
            IllegalArgumentException, NoSuchFieldException,
            IllegalAccessException {

        return aggregateReport.toTableRow();
    }


    public AggregationController getAggregationController() {
        return aggregationController;
    }

    //COMPOSITE_TYPE controls type of Aggregation (i.e. first aggregator only,
    // second aggregator only, or both (i.e. A, B, A && B)
    public static enum COMPOSITE_TYPE {
        FIRST, SECOND, BOTH, ALL
    }

}
