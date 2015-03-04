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

import com.moneydance.apps.md.model.CurrencyTable;
import com.moneydance.apps.md.model.CurrencyType;
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
    private final ArrayList<Object> outputLine = new ArrayList<>();
    private final double positionScale;
    private final double priceScale;

    protected ReportConfig reportConfig;
    private final DateRange dateRange;
    private SecurityAccountWrapper securityAccount;
    private InvestmentAccountWrapper investmentAccount;
    private Tradeable tradeable;
    private CurrencyWrapper currency;
    private SecurityTypeWrapper securityType;
    private SecuritySubTypeWrapper securitySubType;

    // Map from simpleMetric name -> <value, extractor> pairs.
    protected final TreeMap<String, MetricEntry<Number>> simpleMetric;             // Simple calculations that do not depend on
    // other calculations or return multiple values
    protected final TreeMap<String, MetricEntry<List<Number>>> multipleMetrics;    // Metrics that return multiple values
    protected final TreeMap<String, MetricEntry<Double>> returnsMetric;            // Metrics that perform return calculations
    // (uses simple metrics)

    public static final String MMDividends = "_Dividends";
    public static final String MMGains = "_Gains";
    public static final String MMPriceChange = "_PriceChange";

    public static final String RM3MonthReturn = "3MonthReturn";
    public static final String RM3YearReturn = "3YearReturn";
    public static final String RMAllReturn = "AllReturn";
    public static final String RMAnnualReturn = "AnnualReturn";
    public static final String RMDayReturn = "DayReturn";
    public static final String RMMonthReturn = "MonthReturn";
    public static final String RMWeekReturn = "WeekReturn";
    public static final String RMYTDReturn = "YTDReturn";
    public static final String RMYearReturn = "YearReturn";

    public static final String SMAbsPriceChange = "AbsPriceChange";
    public static final String SMAbsValueChange = "AbsValueChange";
    public static final String SMAnnualizedDividend = "AnnualizedDividend";
    public static final String SMBuy = "Buy";
    public static final String SMCoveredShort = "CoveredShort";
    public static final String SMDividendYield = "DividendYield";
    public static final String SMEndPosition = "EndPosition";
    public static final String SMEndPrice = "EndPrice";
    public static final String SMEndValue = "EndValue";
    public static final String SMExpense = "Expense";
    public static final String SMIncome = "Income";
    public static final String SMLongBasis = "LongBasis";
    public static final String SMPctPriceChange = "PctPriceChange";
    public static final String SMRealizedGain = "RealizedGain";
    public static final String SMSell = "Sell";
    public static final String SMShortBasis = "ShortBasis";
    public static final String SMShortSell = "ShortSell";
    public static final String SMStartPosition = "StartPosition";
    public static final String SMStartPrice = "StartPrice";
    public static final String SMStartValue = "StartValue";
    public static final String SMTotalGain = "TotalGain";
    public static final String SMUnrealizedGain = "UnrealizedGain";
    public static final String SMYieldOnBasis = "YieldOnBasis";

    // Sentinel value indicating that return calculation cannot be performed over a given interval.
    public static final Double UndefinedReturn = Double.MAX_VALUE;

    /**
     * Generic constructor populates all members based on securityAccount
     * or sets all to null
     *
     * @param securityAccount input security account wrapper
     * @param dateRange              input date range
     */
    public SecurityReport(ReportConfig reportConfig, SecurityAccountWrapper securityAccount, DateRange dateRange) {
        this.reportConfig = reportConfig;
        this.dateRange = dateRange;
        simpleMetric = new TreeMap<>();
        multipleMetrics = new TreeMap<>();
        returnsMetric = new TreeMap<>();

        if (securityAccount != null) {
            this.securityAccount = securityAccount;
            this.investmentAccount = securityAccount.getInvAcctWrapper();
            this.tradeable = securityAccount.getTradeable();
            this.currency = securityAccount.getCurrencyWrapper();
            this.securityType = securityAccount.getSecurityTypeWrapper();
            this.securitySubType = securityAccount.getSecuritySubTypeWrapper();

            CurrencyType securityCurrency = currency.getCurrencyType();
            int securityDecimalPlaces = securityCurrency.getDecimalPlaces();

            CurrencyTable currencyTable = securityCurrency.getTable();
            CurrencyType baseCurrency = currencyTable.getBaseType();
            int cashDecimalPlaces = baseCurrency.getDecimalPlaces();

            positionScale = Math.pow(10.0, securityDecimalPlaces);
            priceScale = Math.pow(10.0, cashDecimalPlaces);

        } else {
            this.securityAccount = null;
            this.investmentAccount = null;
            this.tradeable = null;
            this.currency = null;
            this.securityType = null;
            this.securitySubType = null;

            positionScale = 10000.0;
            priceScale = 100.0;
        }
    }

    protected void doCalculations(SecurityAccountWrapper securityAccount) {
        if (securityAccount != null) {
            for (TransactionValues transaction : securityAccount.getTransactionValues()) {
                int transactionDateInt = transaction.getDateInt();  // CSE across loops and method invocations
                for (MetricEntry<Number> p : simpleMetric.values()) {
                    if (p.extractor != null) {
                        p.extractor.processNextTransaction(transaction, transactionDateInt);
                    }
                }
                for (MetricEntry<List<Number>> p : multipleMetrics.values()) {
                    if (p.extractor != null) {
                        p.extractor.processNextTransaction(transaction, transactionDateInt);
                    }
                }
                for (MetricEntry<Double> p : returnsMetric.values()) {
                    if (p.extractor != null) {
                        p.extractor.processNextTransaction(transaction, transactionDateInt);
                    }
                }
            }

            for (MetricEntry<Number> p : simpleMetric.values()) {
                if (p.extractor != null) {
                    p.value = (Number) p.extractor.getResult();
                }
            }
            for (MetricEntry<List<Number>> p : multipleMetrics.values()) {
                if (p.extractor != null) {
                    // Java compiler warning: unchecked cast -- Java type system can't handle this
                    p.value = (List<Number>) p.extractor.getResult();
                }
            }
            for (MetricEntry<Double> p : returnsMetric.values()) {
                if (p.extractor != null) {
                    p.value = (Double) p.extractor.getResult();
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
        assert securityAccount == null;
        assert getDateRange().equals(operand.getDateRange());

        // Fold in financial data from operand into this report
        if (currency != null && operand.currency != null && currency.equals(operand.currency)) {
            assignValue(SMStartPrice, operand);
            assignValue(SMEndPrice, operand);
            addValue(SMStartPosition, operand);
            addValue(SMEndPosition, operand);

        } else {
            // Different securities do not have a consolidated price or position
            simpleMetric.get(SMStartPrice).value = 0L;
            simpleMetric.get(SMStartPosition).value = 0L;
            simpleMetric.get(SMEndPrice).value = 0L;
            simpleMetric.get(SMEndPosition).value = 0L;
        }

        // Combine basic metrics
        addValue(SMStartValue, operand);
        addValue(SMEndValue, operand);

        // Now can recompute returns.
        combineReturns(operand);
    }

    protected void assignValue(String key, SecurityReport operand) {
        MetricEntry<Number> entry = simpleMetric.get(key);
        MetricEntry<Number> operandEntry = operand.simpleMetric.get(key);
        if (entry != null && operandEntry != null) {
            entry.value = operandEntry.value;
        }
    }

    protected void addValue(String key, SecurityReport operand) {
        MetricEntry<Number> entry = simpleMetric.get(key);
        MetricEntry<Number> operandEntry = operand.simpleMetric.get(key);
        if (entry != null && operandEntry != null) {
            entry.value = (Long) entry.value + (Long) operandEntry.value;
        }
    }

    private void combineReturns(SecurityReport operand) {
        for (String name : returnsMetric.keySet()) {
            MetricEntry<Double> p = returnsMetric.get(name);
            assert p != null;
            if (p.extractor != null) {
                p.extractor.aggregateResults(operand.returnsMetric.get(name).extractor);
            }
        }
    }

    protected SecurityReport initializeAggregateSecurityReport(SecurityReport aggregate) {
        // Make aggregating object the same except secAccountWrapper
        aggregate.investmentAccount = investmentAccount;
        aggregate.securityAccount = null;
        aggregate.securityType = securityType;
        aggregate.securitySubType = securitySubType;
        aggregate.tradeable = tradeable;
        aggregate.currency = currency;

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
            return investmentAccount;
        } else if (aggregator instanceof SecurityTypeWrapper) {
            return securityType;
        } else if (aggregator instanceof SecuritySubTypeWrapper) {
            return securitySubType;
        } else if (aggregator instanceof Tradeable) {
            return tradeable;
        } else if (aggregator instanceof CurrencyWrapper) {
            return currency;
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

    public abstract SecurityReport getAggregateSecurityReport();

    @Override
    public Object[] toTableRow() throws SecurityException, IllegalArgumentException,
            NoSuchFieldException, IllegalAccessException {
        return toTableRow(investmentAccount, securityAccount, securityType, securitySubType, currency);
    }

    public Object[] toTableRow(InvestmentAccountWrapper investmentAccount, SecurityAccountWrapper securityAccount,
                               SecurityTypeWrapper securityType, SecuritySubTypeWrapper securitySubType,
                               CurrencyWrapper currency) throws SecurityException, IllegalArgumentException {
        assert securityAccount != null;

        outputLine.add(investmentAccount);
        outputLine.add(securityAccount);
        outputLine.add(securityType);
        outputLine.add(securitySubType);
        outputLine.add(currency);
        recordMetrics();
        return outputLine.toArray();
    }

    protected abstract void recordMetrics();

    protected void outputSimplePrice(String name) {
        outputLine.add((Long) simpleMetric.get(name).value / priceScale);
    }

    protected void outputSimplePosition(String name) {
        outputLine.add((Long) simpleMetric.get(name).value / positionScale);
    }

    protected void outputSimpleValue(String name) {
        outputLine.add(simpleMetric.get(name).value);
    }

    protected void outputReturn(String name) {
        MetricEntry<Double> entry = returnsMetric.get(name);
        if(securityAccount == null && investmentAccount != null && investmentAccount.getName().equals("StockBrokerage2")){
            int i = 1; //TODO: Remove after debug
        }
        entry.value = (Double) entry.extractor.getResult(); // Force calculation if aggregate
        outputLine.add(entry.value);
    }

    public String getName() {
        if (securityAccount == null) {
            return "Null SecAccountWrapper";
        } else {
            return investmentAccount.getName() + ": " + securityAccount.getName();
        }
    }

    public Tradeable getTradeable() {
        return tradeable;
    }

    public void setTradeable(Tradeable tradeable) {
        this.tradeable = tradeable;
    }

    public CurrencyWrapper getCurrencyWrapper() {
        return currency;
    }

    public void setCurrencyWrapper(CurrencyWrapper currencyWrapper) {
        this.currency = currencyWrapper;
    }

    public SecurityTypeWrapper getSecurityTypeWrapper() {
        return securityType;
    }

    public SecuritySubTypeWrapper getSecuritySubTypeWrapper() {
        return securitySubType;
    }

    public void setSecuritySubTypeWrapper(
            SecuritySubTypeWrapper securitySubTypeWrapper) {
        this.securitySubType = securitySubTypeWrapper;
    }

    public InvestmentAccountWrapper getInvestmentAccountWrapper() {
        return investmentAccount;
    }

    public void setInvestmentAccountWrapper(InvestmentAccountWrapper investmentAccountWrapper) {
        this.investmentAccount = investmentAccountWrapper;
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public Number getSimpleMetric(String name) {
        return simpleMetric.get(name).value;
    }

    public Double getReturnMetric(String name) {
        return returnsMetric.get(name).value;
    }

    public int getReturnMetricStartDateInt(String name) {
        return returnsMetric.get(name).extractor.startDateInt;
    }


    // For each metric, we have its current value and an extractor (which has its own state).
    public class MetricEntry<V> {
        public V value;
        public final ExtractorBase<?> extractor;

        MetricEntry(V v, ExtractorBase<?> e) {
            value = v;
            extractor = e;
        }
    }
}





