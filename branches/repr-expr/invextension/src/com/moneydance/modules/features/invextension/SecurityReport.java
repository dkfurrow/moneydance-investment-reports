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

import com.moneydance.modules.features.invextension.CompositeReport.COMPOSITE_TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;


/**
 * Generic SecurityReport, generates one data set for each Security in each investment account
 * Version 1.0
 *
 * @author Dale Furrow
 */
public abstract class SecurityReport extends ComponentReport {
    private ArrayList<Object> outputLine = new ArrayList<>();
    private DateRange dateRange;
    private SecurityAccountWrapper securityAccountWrapper;
    private InvestmentAccountWrapper investmentAccountWrapper;
    private Tradeable tradeable;
    private CurrencyWrapper currencyWrapper;
    private SecurityTypeWrapper securityTypeWrapper;
    private SecuritySubTypeWrapper securitySubTypeWrapper;

    protected class pair<V> {
        public V value;
        public ExtractorBase<?> extractor;

        pair(V v, ExtractorBase<?> e) {
            value = v;
            extractor = e;
        }
    }

    // Map from simpleMetric name -> <value, extractor> pairs.
    protected TreeMap<String, pair<Number>> simpleMetric;             // Simple calculations that do not depend on
    // other calculations or return multiple values
    protected TreeMap<String, pair<List<Number>>> multipleMetrics;    // Metrics that return multiple values
    protected TreeMap<String, pair<Double>> returnsMetric;            // Metrics that perform return calculations (uses
    // simple metrics)

    /**
     * Generic constructor populates all members based on securityAccountWrapper
     * or sets all to null
     *
     * @param securityAccountWrapper input security account wrapper
     * @param dateRange              input date range
     */
    public SecurityReport(SecurityAccountWrapper securityAccountWrapper, DateRange dateRange) {
        this.dateRange = dateRange;
        simpleMetric = new TreeMap<>();
        multipleMetrics = new TreeMap<>();
        returnsMetric = new TreeMap<>();

        if (securityAccountWrapper != null) {
            this.securityAccountWrapper = securityAccountWrapper;
            this.investmentAccountWrapper = securityAccountWrapper.getInvAcctWrapper();
            this.tradeable = securityAccountWrapper.getTradeable();
            this.currencyWrapper = securityAccountWrapper.getCurrencyWrapper();
            this.securityTypeWrapper = securityAccountWrapper.getSecurityTypeWrapper();
            this.securitySubTypeWrapper = securityAccountWrapper.getSecuritySubTypeWrapper();
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

    protected void doCalculations(SecurityAccountWrapper securityAccount) {
        if (securityAccount != null) {
            for (TransactionValues transaction : securityAccount.getTransactionValues()) {
                for (pair<Number> p : simpleMetric.values()) {
                    if (p.extractor != null) {
                        p.extractor.NextTransaction(transaction, securityAccount);
                    }
                }
                for (pair<List<Number>> p : multipleMetrics.values()) {
                    if (p.extractor != null) {
                        p.extractor.NextTransaction(transaction, securityAccount);
                    }
                }
                for (pair<Double> p : returnsMetric.values()) {
                    if (p.extractor != null) {
                        p.extractor.NextTransaction(transaction, securityAccount);
                    }
                }
            }

            for (pair<Number> p : simpleMetric.values()) {
                if (p.extractor != null) {
                    p.value = (Number) p.extractor.FinancialResults(securityAccount);
                }
            }
            for (pair<List<Number>> p : multipleMetrics.values()) {
                if (p.extractor != null) {
                    // Java compiler warning: unchecked cast -- Java type system can't handle this
                    p.value = (List<Number>) p.extractor.FinancialResults(securityAccount);
                }
            }
            for (pair<Double> p : returnsMetric.values()) {
                if (p.extractor != null) {
                    p.value = (Double) p.extractor.FinancialResults(securityAccount);
                }
            }
        }
    }

    // Exploit the triple stored under "resultName" into the three metrics whose names are given.
    protected void explode(String resultName, String name0, String name1, String name2) {
        List<Number> result = multipleMetrics.get(resultName).value;
        assert result != null && result.size() == 3;
        simpleMetric.get(name0).value = result.get(0);
        simpleMetric.get(name1).value = result.get(1);
        simpleMetric.get(name2).value = result.get(2);
    }

    public void addTo(SecurityReport operand) {
        assert securityAccountWrapper == null;
        assert this.getDateRange().equals(operand.getDateRange());

        // Fold in financial data from operand into this report

        if (this.getCurrencyWrapper() != null && operand.getCurrencyWrapper() != null
                && this.getCurrencyWrapper().equals(operand.getCurrencyWrapper())) {
            assert simpleMetric.get("StartPrice").value.equals(operand.simpleMetric.get("StartPrice").value);
            assert simpleMetric.get("EndPrice").value.equals(operand.simpleMetric.get("EndPrice").value);
            addValue("StartPosition", operand, "StartPosition");
            addValue("EndPosition", operand, "EndPosition");

        } else {
            // Different securities do not have a consolidated price or position
            simpleMetric.get("StartPrice").value = 0L;
            simpleMetric.get("StartPosition").value = 0L;
            simpleMetric.get("EndPrice").value = 0L;
            simpleMetric.get("EndPosition").value = 0L;
        }

        // Combine basic metrics
        addValue("StartValue", operand, "StartValue");
        addValue("EndValue", operand, "EndValue");

        // Now can recompute returns.
        combineReturns(operand);
    }

    protected void assignValue(String key1, SecurityReport operand, String key2) {
        pair<Number> entry = simpleMetric.get(key1);
        pair<Number> operandEntry = operand.simpleMetric.get(key2);
        if (entry != null && operandEntry != null) {
            entry.value = operandEntry.value;
        }
    }

    protected void addValue(String key1, SecurityReport operand, String key2) {
        pair<Number> entry = simpleMetric.get(key1);
        pair<Number> operandEntry = operand.simpleMetric.get(key2);
        if (entry != null && operandEntry != null) {
            entry.value = (Long) entry.value + (Long) operandEntry.value;
        }
    }

    protected void combineReturns(SecurityReport operand) {
        for (String name : returnsMetric.keySet()) {
            pair<Double> p = returnsMetric.get(name);
            assert p != null;
            if (p.extractor != null) {
                p.value = (Double) p.extractor.CombineFinancialResults(operand.returnsMetric.get(name).extractor);
            }
        }
    }

    protected SecurityReport initializeAggregateSecurityReport(SecurityReport aggregate) {
        // make aggregating classes the same except secAccountWrapper
        aggregate.setInvestmentAccountWrapper(this.getInvestmentAccountWrapper());
        aggregate.securityAccountWrapper = null;
        aggregate.setSecurityTypeWrapper(this.getSecurityTypeWrapper());
        aggregate.setSecuritySubTypeWrapper(this.getSecuritySubTypeWrapper());
        aggregate.setTradeable(this.getTradeable());
        aggregate.setCurrencyWrapper(this.getCurrencyWrapper());

        // Copy values
        aggregate.addTo(this);

        return aggregate;
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

    public abstract SecurityReport getAggregateSecurityReport();

    public abstract void addLineBody();

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





