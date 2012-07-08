/* SecuritySnapshotReport.java
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
import java.util.Iterator;
import java.util.SortedSet;

import com.moneydance.apps.md.model.CurrencyType;
import com.moneydance.modules.features.invextension.CompositeReport.COMPOSITE_TYPE;

/**
 * Report detailing performance attributes based on a specific snapshot
 * date
 * Version 1.0 
 * @author Dale Furrow
 */
public class SecuritySnapshotReport extends SecurityReport {
    
    private int snapDateInt;
    
    
    private double lastPrice;           //ending price
    private double endPos;              //ending position
    private double endValue;            //ending value
    
    private double avgCostBasis;                //final average cost balance

    //one day values
    private double absPriceChange;      //absolute price change (from previous day to snapDate)
    private double pctPriceChange;      //percent price change (from previous day to snapDate)
    private double absValueChange;      //absolute value change (from previous day to snapDate)

    //total numbers
    private double income;              //total income (all dates)
    private double totalGain;           //total absolute gain (all dates)
    private double totRetAll;           //total Mod-Dietz return (all dates)
    private double annRetAll;           //annualized return (all dates)

    //returns
    private double totRet1Day;          //Mod-Dietz return (1 day)
    private double totRetWk;            //Mod-Dietz return (1 week)
    private double totRet4Wk;           //Mod-Dietz return (1 month)
    private double totRet3Mnth;         //Mod-Dietz return (3 month)
    private double totRetYTD;           //Mod-Dietz return (Year-to-Date)
    private double totRetYear;          //Mod-Dietz return (1 Year)
    private double totRet3year;         //Mod-Dietz return (1 Years)

    // intermediate values
    private CategoryMap<Integer> returnsStartDate;      //maps return category to start dates
    private CategoryMap<Double> startValues;            //maps return category to start values
    private CategoryMap<Double> startPoses;             //maps return category to start positions
    private CategoryMap<Double> startPrices;            //maps return category to start positions
    private CategoryMap<Double> incomes;                //maps return category to income
    private CategoryMap<Double> expenses;               //maps return category to expense
    private CategoryMap<Double> mdReturns;              //maps return category to Mod-Dietz returns
    

    private CategoryMap<DateMap> mdMap;                 //maps return category to Mod-Dietz date map
    private CategoryMap<DateMap> arMap;                 //maps return category to Annualized Return Date Map
    private CategoryMap<DateMap> transMap;              //maps return category to transfer date map

    
    /*** Generic constructor, which produces either the SecurityReport associated
     * with a given SecurityAccountWrapper or a blank report
     * @param secAccountWrapper
     * @param dateRange
     */
    public SecuritySnapshotReport(SecurityAccountWrapper secAccountWrapper,
	    DateRange dateRange) {

	super(secAccountWrapper, dateRange);

	this.snapDateInt = dateRange.snapDateInt();

	this.lastPrice = 0.0;
	this.endPos = 0.0;
	this.endValue = 0.0;

	this.avgCostBasis = 0.0;

	this.absPriceChange = 0.0;
	this.pctPriceChange = 0.0;
	this.absValueChange = 0.0;

	this.income = 0.0;
	this.totalGain = 0.0;
	this.totRetAll = 0.0;
	this.annRetAll = 0.0;

	this.totRet1Day = 0.0;
	this.totRetWk = 0.0;
	this.totRet4Wk = 0.0;
	this.totRet3Mnth = 0.0;
	this.totRetYTD = 0.0;
	this.totRetYear = 0.0;
	this.totRet3year = 0.0;

	this.returnsStartDate = new CategoryMap<Integer>();
	this.startValues = new CategoryMap<Double>();
	this.startPoses = new CategoryMap<Double>();
	this.startPrices = new CategoryMap<Double>();
	this.incomes = new CategoryMap<Double>();
	this.expenses = new CategoryMap<Double>();
	this.mdReturns = new CategoryMap<Double>();

	this.mdMap = new CategoryMap<DateMap>();
	this.arMap = new CategoryMap<DateMap>();
	this.transMap = new CategoryMap<DateMap>();
	

	// Calculate return dates (use snapDate for "ALL" as it is latest
	// possible
	int fromDateInt = snapDateInt;
	int prevFromDateInt = DateUtils.getPrevBusinessDay(snapDateInt);
	int wkFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
		.addDaysInt(snapDateInt, -7));
	int mnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
		.addMonthsInt(snapDateInt, -1));
	int threeMnthFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
		.addMonthsInt(snapDateInt, -3));
	int oneYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
		.addMonthsInt(snapDateInt, -12));
	int threeYearFromDateInt = DateUtils.getLatestBusinessDay(DateUtils
		.addMonthsInt(snapDateInt, -36));
	int ytdFromDateInt = DateUtils.getStartYear(snapDateInt);

	// put dates in return map
	this.returnsStartDate = new CategoryMap<Integer>();

	this.returnsStartDate.put("All", fromDateInt);
	this.returnsStartDate.put("PREV", prevFromDateInt);
	this.returnsStartDate.put("1Wk", wkFromDateInt);
	this.returnsStartDate.put("4Wk", mnthFromDateInt);
	this.returnsStartDate.put("3Mnth", threeMnthFromDateInt);
	this.returnsStartDate.put("1Yr", oneYearFromDateInt);
	this.returnsStartDate.put("3Yr", threeYearFromDateInt);
	this.returnsStartDate.put("YTD", ytdFromDateInt);

	// initialize ArrayLists values to zero
	for (Iterator<String> it1 = this.returnsStartDate.keySet().iterator(); it1
		.hasNext();) {
	    String retCat = it1.next();
	    startPoses.put(retCat, 0.0);
	    this.startValues.put(retCat, 0.0);
	    this.incomes.put(retCat, 0.0);
	    this.expenses.put(retCat, 0.0);
	    this.mdReturns.put(retCat, 0.0);

	    this.arMap.put(retCat, new DateMap());
	    this.mdMap.put(retCat, new DateMap());
	    this.transMap.put(retCat, new DateMap());
	}

	if (secAccountWrapper != null) {
	    // Currency will never be null if accountWrapper is cash or security
	    CurrencyType currency = secAccountWrapper.getCurrencyWrapper().curType;

	    this.lastPrice = 1.0 / currency.getUserRateByDateInt(snapDateInt);

	    // create dates for returns calculations
	    // ensures all dates for appropriate variables
	    SortedSet<TransactionValues> transSet = secAccountWrapper
		    .getTransactionValues();

	    fromDateInt = transSet.isEmpty() ? snapDateInt : DateUtils
		    .getPrevBusinessDay(transSet.first().getDateint());

	    // put dates in return map
	    this.returnsStartDate.put("All", fromDateInt);

	    // these values dependent only on snapDate
	    double endCumUnrealizedGain = 0.0;
	    double longBasis = 0.0;
	    double shortBasis = 0.0;

	    // these values calculated once, based on total transaction history
	    double realizedGain = 0.0;
	    double unrealizedGain = 0.0;
	    double startCumUnrealGain = 0.0; // by definition, since this
	    // calculation covers all transactions
	    double annualPercentReturn = 0.0;

	    // fill startPrice Array List
	    for (Iterator<String> it = this.returnsStartDate.keySet()
		    .iterator(); it.hasNext();) {
		String retCat = it.next();
		int thisDateInt = this.returnsStartDate.get(retCat);
		startPrices.put(retCat,
			1.0 / currency.getUserRateByDateInt(thisDateInt));
	    }

	    // iterate through transaction values list
	    for (Iterator<TransactionValues> it = transSet.iterator(); it.hasNext();) {
		TransactionValues transactionValues = it.next();
		double totalFlows = transactionValues.getBuy() + transactionValues.getSell()
			+ transactionValues.getShortSell() + transactionValues.getCoverShort()
			+ transactionValues.getCommision() + transactionValues.getIncome()
			+ transactionValues.getExpense();

		// iterate through return dates
		for (Iterator<String> it1 = this.returnsStartDate.keySet()
			.iterator(); it1.hasNext();) {
		    String retCat = it1.next();
		    int thisFromDateInt = this.returnsStartDate.get(retCat);

		    // where transactions are before report dates
		    if (transactionValues.getDateint() <= thisFromDateInt) {
			double currentRate = currency == null ? 1.0 : currency
				.getUserRateByDateInt(thisFromDateInt);
			double splitAdjust = currency == null ? 1.0 : currency
				.adjustRateForSplitsInt(transactionValues.getDateint(),
					currentRate, thisFromDateInt)
				/ currentRate;
			startPoses.put(retCat, transactionValues.getPosition()
				* splitAdjust); // split adjusts last position
						// from
						// TransValuesCum
			this.startValues.put(retCat, startPrices.get(retCat)
				* startPoses.get(retCat));

		    }

		    // where transaction period intersects report period
		    if (transactionValues.getDateint() > thisFromDateInt
			    && transactionValues.getDateint() <= snapDateInt) {

			// MDCalc variable--net effect of calculation is to
			// return buys and sells, including commission
			double cf = -(transactionValues.getBuy() + transactionValues.getSell()
				+ transactionValues.getShortSell()
				+ transactionValues.getCoverShort() + transactionValues.getCommision());

			// add variables to arrays needed for returns
			// calculation

			this.arMap.get(retCat).add(transactionValues.getDateint(),
				totalFlows);
			this.mdMap.get(retCat).add(transactionValues.getDateint(), cf);
			this.transMap.get(retCat).add(transactionValues.getDateint(),
				transactionValues.getTransfer());

			this.incomes.put(retCat, this.incomes.get(retCat)
				+ transactionValues.getIncome());
			this.expenses.put(retCat, this.expenses.get(retCat)
				+ transactionValues.getExpense());

			if ("All".equals(retCat)) {// end cash increment--only
						   // needs to be done once
			    realizedGain = transactionValues.getPerRealizedGain()
				    + realizedGain;
			}

			double currentRate = currency == null ? 1.0 : currency
				.getUserRateByDateInt(snapDateInt);
			double splitAdjust = currency == null ? 1.0 : currency
				.adjustRateForSplitsInt(transactionValues.getDateint(),
					currentRate, snapDateInt)
				/ currentRate;

			this.endPos = transactionValues.getPosition() * splitAdjust;
			this.endValue = this.endPos * this.lastPrice;
			longBasis = transactionValues.getLongBasis();
			shortBasis = transactionValues.getShortBasis();
			this.avgCostBasis = longBasis + shortBasis;
		    } // end--where transaction period intersects report period
		} // end of start date iterative loop
	    } // end of input transaction set loop

	    if (this.endPos > 0) {
		endCumUnrealizedGain = this.endValue - longBasis;
	    } else if (this.endPos < 0) {
		endCumUnrealizedGain = this.endValue - shortBasis;
	    }
	    // startCumUnrealGain is zero by definition
	    unrealizedGain = endCumUnrealizedGain - startCumUnrealGain;
	    this.totalGain = realizedGain + unrealizedGain;

	    // now go through arrays and get returns/calc values
	    for (Iterator<String> it1 = this.returnsStartDate.keySet()
		    .iterator(); it1.hasNext();) {
		String retCat = it1.next();
		int thisFromDateInt = this.returnsStartDate.get(retCat);
		// add the first value in return arrays (if startpos != 0)
		if (startPoses.get(retCat) != 0) {
		    this.arMap.get(retCat).add(thisFromDateInt,
			    -this.startValues.get(retCat));
		    // dummy values for Mod-dietz
		    this.mdMap.get(retCat).add(thisFromDateInt, 0.0);
		}
		// add the last value in return arrays (if endpos != 0)
		if (this.endPos != 0) {
		    this.arMap.get(retCat).add(snapDateInt, this.endValue);
		    // dummy values for Mod-dietz
		    this.mdMap.get(retCat).add(snapDateInt, 0.0);
		}

		// get MD returns on all start dates, only get annualized return
		// on all dates

		this.mdReturns.put(
			retCat,
			computeMDReturn(this.startValues.get(retCat),
				this.endValue, this.incomes.get(retCat),
				this.expenses.get(retCat),
				this.mdMap.get(retCat)));
		//get annualized returns only for total period
		if ("All".equals(retCat)) {
		    annualPercentReturn = computeAnnualReturn(
			    this.arMap.get(retCat), this.mdReturns.get("All"));
		    this.annRetAll = annualPercentReturn;
		    this.income = this.incomes.get(retCat);
		}

		// remove start and end values from return date maps for ease of
		// aggregation
		if (startPoses.get(retCat) != 0) {
		    this.arMap.get(retCat).add(thisFromDateInt,
			    +this.startValues.get(retCat));
		}
		// remove start and end values from return date maps for ease of
		// aggregation
		if (this.endPos != 0) {
		    this.arMap.get(retCat).add(snapDateInt, -this.endValue);
		}
	    } // end of start date iterateration

	    // Produce output, get returns

	    if (this.returnsStartDate.get("PREV") == null) {
		this.absPriceChange = Double.NaN;
		this.absValueChange = Double.NaN;
		this.pctPriceChange = Double.NaN;
	    } else {
		double prevPrice = currency == null ? 1.0
			: 1.0 / currency
				.getUserRateByDateInt(this.returnsStartDate
					.get("PREV"));
		this.absPriceChange = this.lastPrice - prevPrice;
		this.absValueChange = this.endPos * this.absPriceChange;
		this.pctPriceChange = this.lastPrice / prevPrice - 1.0;
	    }
	    this.totRet1Day = this.mdReturns.get("PREV") == null ? Double.NaN
		    : this.mdReturns.get("PREV");
	    this.totRetAll = this.mdReturns.get("All") == null ? Double.NaN
		    : this.mdReturns.get("All");
	    this.totRetWk = this.mdReturns.get("1Wk") == null ? Double.NaN
		    : this.mdReturns.get("1Wk");
	    this.totRet4Wk = this.mdReturns.get("4Wk") == null ? Double.NaN
		    : this.mdReturns.get("4Wk");
	    this.totRet3Mnth = this.mdReturns.get("3Mnth") == null ? Double.NaN
		    : this.mdReturns.get("3Mnth");
	    this.totRetYear = this.mdReturns.get("1Yr") == null ? Double.NaN
		    : this.mdReturns.get("1Yr");
	    this.totRet3year = this.mdReturns.get("3Yr") == null ? Double.NaN
		    : this.mdReturns.get("3Yr");
	    this.totRetYTD = this.mdReturns.get("YTD") == null ? Double.NaN
		    : this.mdReturns.get("YTD");
	}

    }

    @Override
    public <T extends Aggregator, U extends 
    Aggregator> CompositeReport<T, U> getCompositeReport(
	    Class<T> firstAggClass, Class<U> secondAggClass,
	    COMPOSITE_TYPE compType) {
	CompositeReport<T, U> thisComposite = new CompositeReport<T, U>(this,
		this.getDateRange(), firstAggClass, secondAggClass, compType);
	return thisComposite;
    }

    @Override
    public void recomputeAggregateReturns() {
	for (Iterator<String> it1 = returnsStartDate.keySet().iterator(); it1
		.hasNext();) {
	    String retCat = it1.next();

	    // get MD returns on all start dates, only get annualized return for
	    // "All" dates
	    mdReturns.put(retCat, computeMDReturn(startValues.get(retCat), endValue,
		    incomes.get(retCat), expenses.get(retCat), mdMap.get(retCat)));

	    if ("All".equals(retCat)) {
		// add start and end values to return date maps
		if (startValues.get(retCat) != 0.0) {
		    arMap.get(retCat).add(returnsStartDate.get(retCat),
			    -startValues.get(retCat));
		}
		if (endValue != 0.0) {
		    arMap.get(retCat).add(snapDateInt, endValue);
		}
		// get return
		annRetAll = computeAnnualReturn(arMap.get(retCat),
			mdReturns.get("All"));
		income = incomes.get(retCat);

	    }
	}
	totRet1Day = returnsStartDate.get("PREV") == null ? Double.NaN
		: mdReturns.get("PREV");
	totRetAll = returnsStartDate.get("All") == null ? Double.NaN
		: mdReturns.get("All");
	totRetWk = returnsStartDate.get("1Wk") == null ? Double.NaN : mdReturns
		.get("1Wk");
	totRet4Wk = returnsStartDate.get("4Wk") == null ? Double.NaN
		: mdReturns.get("4Wk");
	totRet3Mnth = returnsStartDate.get("3Mnth") == null ? Double.NaN
		: mdReturns.get("3Mnth");
	totRetYear = returnsStartDate.get("1Yr") == null ? Double.NaN
		: mdReturns.get("1Yr");
	totRet3year = returnsStartDate.get("3Yr") == null ? Double.NaN
		: mdReturns.get("3Yr");
	totRetYTD = returnsStartDate.get("YTD") == null ? Double.NaN
		: mdReturns.get("YTD");

    }

    @Override
    public SecurityReport getAggregateSecurityReport() {
	SecuritySnapshotReport thisAggregate = new SecuritySnapshotReport(null,
		getDateRange());
	// add report body values (except Returns)
	thisAggregate.lastPrice = this.lastPrice;
	thisAggregate.endPos = this.endPos;
	thisAggregate.endValue = this.endValue;

	thisAggregate.avgCostBasis = this.avgCostBasis;

	// one day values
	thisAggregate.absPriceChange = this.absPriceChange;
	thisAggregate.pctPriceChange = this.pctPriceChange;
	thisAggregate.absValueChange = this.absValueChange;

	// total numbers
	thisAggregate.income = this.income;
	thisAggregate.totalGain = this.totalGain;

	// intermediate values
	thisAggregate.returnsStartDate = this.returnsStartDate;
	thisAggregate.startValues = this.startValues;
	thisAggregate.startPoses = this.startPoses;
	thisAggregate.startPrices = this.startPrices;
	thisAggregate.incomes = this.incomes;
	thisAggregate.expenses = this.expenses;
	thisAggregate.mdReturns = this.mdReturns;

	thisAggregate.mdMap = this.mdMap;
	thisAggregate.arMap = this.arMap;
	thisAggregate.transMap = this.transMap;

	// make aggregating classes the same except secAccountWrapper
	thisAggregate.setInvAccountWrapper(this.getInvAccountWrapper());
	thisAggregate.setSecAccountWrapper(null);
	thisAggregate.setSecurityTypeWrapper(this.getSecurityTypeWrapper());
	thisAggregate.setSecuritySubTypeWrapper(this
		.getSecuritySubTypeWrapper());
	thisAggregate.setTradeable(this.getTradeable());
	thisAggregate.setCurrencyWrapper(this.getCurrencyWrapper());

	return thisAggregate;

    }

    @Override
    public String getName() {
	if (this.getSecAccountWrapper() == null) {
	    return "Null SecAccountWrapper";
	} else {
	    return this.getInvAccountWrapper().getName() + ": "
		    + this.getSecAccountWrapper().getAccountName();
	}
    }

    @Override
    public void addTo(SecurityReport securityReport) {
	SecuritySnapshotReport operand = (SecuritySnapshotReport) securityReport;
	if (this.getSecAccountWrapper() != null)
	    throw new UnsupportedOperationException(
		    "Illegal call to addTo method for SecurityReport");

	if (this.getCurrencyWrapper() != null && operand.getCurrencyWrapper() != null
		&& this.getCurrencyWrapper().equals(operand.getCurrencyWrapper())) {

	    this.endPos += operand.endPos;
	    this.lastPrice = operand.lastPrice;

	} else {
	    this.endPos = 0.0;
	    this.lastPrice = 0.0;
	}

	this.endValue += operand.endValue;

	this.avgCostBasis += operand.avgCostBasis;
	this.absPriceChange = 0.0;
	this.pctPriceChange = 0.0;
	this.absValueChange += operand.absValueChange;
	this.income += operand.income;
	this.totalGain += operand.totalGain;
	this.totRetAll = 0.0;
	this.annRetAll = 0.0;
	this.totRet1Day = 0.0;
	this.totRetWk = 0.0;
	this.totRet4Wk = 0.0;
	this.totRet3Mnth = 0.0;
	this.totRetYTD = 0.0;
	this.totRetYear = 0.0;
	this.totRet3year = 0.0;

	this.returnsStartDate = combineReturns(this.returnsStartDate,
		operand.returnsStartDate);
	this.startValues = addDoubleMap(this.startValues, operand.startValues);
	this.incomes = addDoubleMap(this.incomes, operand.incomes);
	this.expenses = addDoubleMap(this.expenses, operand.expenses);

	this.mdMap = combineDateMapMap(this.mdMap, operand.mdMap, "add");
	this.arMap = combineDateMapMap(this.arMap, operand.arMap, "add");
	this.transMap = combineDateMapMap(this.transMap, operand.transMap,
		"add");

    }

    @Override
    public Object[] toTableRow() throws SecurityException,
	    IllegalArgumentException, NoSuchFieldException,
	    IllegalAccessException {
	ArrayList<Object> snapValues = new ArrayList<Object>();

	snapValues.add(this.getInvAccountWrapper().getName());
	snapValues.add(this.getSecAccountWrapper().getName());
	snapValues.add(this.getSecurityTypeWrapper().getName());
	snapValues.add(this.getSecuritySubTypeWrapper().getName());
	snapValues.add(this.getCurrencyWrapper().ticker);

	addLineBody(snapValues);
	
	return snapValues.toArray();

    }
    
    @Override
    public void addLineBody(ArrayList<Object> rptValues) {
	rptValues.add(this.lastPrice);
        rptValues.add(this.endPos);
        rptValues.add(this.endValue);
        rptValues.add(this.absPriceChange);
        rptValues.add(this.absValueChange);
        rptValues.add(this.pctPriceChange);
        rptValues.add(this.totRet1Day);
        rptValues.add(this.totRetWk);
        rptValues.add(this.totRet4Wk);
        rptValues.add(this.totRet3Mnth);
        rptValues.add(this.totRetYTD);
        rptValues.add(this.totRetYear);
        rptValues.add(this.totRet3year);
        rptValues.add(this.totRetAll);
        rptValues.add(this.annRetAll);
        rptValues.add(this.avgCostBasis);
        rptValues.add(this.income);
        rptValues.add(this.totalGain);
    }

    /*
     * Combine returns category maps.
     */
    private CategoryMap<Integer> combineReturns(CategoryMap<Integer> map1,
	    CategoryMap<Integer> map2) {
	CategoryMap<Integer> outMap = new CategoryMap<Integer>(map1);

	for (Iterator<String> it = map2.keySet().iterator(); it.hasNext();) {
	    String map2Key = it.next();
	    Integer map2Value = map2.get(map2Key);
	    if (map1.get(map2Key) == null) {
		outMap.put(map2Key, map2Value);
	    } else {
		outMap.put(map2Key, Math.min(map1.get(map2Key), map2Value));
	    }
	}

	return outMap;
    }

    /*
     * Combines intermediate values for start value, end value, income, expense
     * for aggregate mod-dietz returns calculations.
     */
    private CategoryMap<Double> addDoubleMap(CategoryMap<Double> map1,
	    CategoryMap<Double> map2) {
	CategoryMap<Double> outMap = new CategoryMap<Double>(map1);

	if (map2 != null) {
	    for (Iterator<String> it = map2.keySet().iterator(); it.hasNext();) {
		String retCat2 = it.next();
		Double value2 = map2.get(retCat2);
		if (map1.get(retCat2) == null) {
		    outMap.put(retCat2, value2);
		} else {
		    outMap.put(retCat2, map1.get(retCat2) + value2);
		}
	    }
	}

	return outMap;
    }

    /*
     * Combines map of datemaps for Snap Reports, either adding or subtracting
     * cash flows.
     * @param map1 input map
     * @param map2 input map
     * @param combType either "add" or "subtract"
     * @return output map
     */
    private CategoryMap<DateMap> combineDateMapMap(CategoryMap<DateMap> map1,
	    CategoryMap<DateMap> map2, String combType) {
	CategoryMap<DateMap> outMap = new CategoryMap<DateMap>(map1);

	if (map2 != null) {
	    for (Iterator<String> it = map2.keySet().iterator(); it.hasNext();) {
		String retCat2 = it.next();
		DateMap treeMap2 = map2.get(retCat2);
		if (map1.get(retCat2) == null) {
		    outMap.put(retCat2, treeMap2);
		} else {
		    DateMap treeMap1 = map1.get(retCat2);
		    DateMap tempMap = new DateMap(treeMap1.combine(treeMap2,
			    combType));
		    outMap.put(retCat2, tempMap);
		}
	    }
	}

	return outMap;
    }

    public int getSnapDateInt() {
        return snapDateInt;
    }

    public double getLastPrice() {
        return lastPrice;
    }

    public double getEndPos() {
        return endPos;
    }

    public double getEndValue() {
        return endValue;
    }

    public double getAvgCostBasis() {
        return avgCostBasis;
    }

    public double getAbsPriceChange() {
        return absPriceChange;
    }

    public double getPctPriceChange() {
        return pctPriceChange;
    }

    public double getAbsValueChange() {
        return absValueChange;
    }

    public double getIncome() {
        return income;
    }

    public double getTotalGain() {
        return totalGain;
    }

    public double getTotRetAll() {
        return totRetAll;
    }

    public double getAnnRetAll() {
        return annRetAll;
    }

    public double getTotRet1Day() {
        return totRet1Day;
    }

    public double getTotRetWk() {
        return totRetWk;
    }

    public double getTotRet4Wk() {
        return totRet4Wk;
    }

    public double getTotRet3Mnth() {
        return totRet3Mnth;
    }

    public double getTotRetYTD() {
        return totRetYTD;
    }

    public double getTotRetYear() {
        return totRetYear;
    }

    public double getTotRet3year() {
        return totRet3year;
    }

    public CategoryMap<Integer> getReturnsStartDate() {
        return returnsStartDate;
    }

    public CategoryMap<Double> getStartValues() {
        return startValues;
    }

    public CategoryMap<Double> getStartPoses() {
        return startPoses;
    }

    public CategoryMap<Double> getStartPrices() {
        return startPrices;
    }

    public CategoryMap<Double> getIncomes() {
        return incomes;
    }

    public CategoryMap<Double> getExpenses() {
        return expenses;
    }

    public CategoryMap<DateMap> getMdMap() {
        return mdMap;
    }

    public CategoryMap<DateMap> getArMap() {
        return arMap;
    }

    public CategoryMap<DateMap> getTransMap() {
        return transMap;
    }

    public CategoryMap<Double> getMdReturns() {
        return mdReturns;
    }
}
