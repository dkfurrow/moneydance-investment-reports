/* DateUtils.java
 * Copyright 2011 Dale K. Furrow . All rights reserved.
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * generic methods to handle business date math
 * 
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
public final class DateUtils {

    private DateUtils() {

    }

    // private static Log log = LogFactory.getLog(MDBusinessDayUtil.class);
    private static transient Map<Integer, List<Date>> computedDates = 
	    new HashMap<Integer, List<Date>>();

    /*
     * This method will calculate the next business day after the one input.
     * This means that if the next day falls on a weekend or one of the
     * following holidays then it will try the next day.
     * 
     * Holidays Accounted For: New Year's Day Martin Luther King Jr. Day
     * President's Day Memorial Day Independence Day Labor Day Columbus Day
     * Veterans Day Thanksgiving Day Christmas Day Good Friday
     */
    public static boolean isBusinessDay(int dateIntToCheck) {
	Date dateToCheck = convertToDate(dateIntToCheck);
	// Setup the calendar to have the start date truncated
	Calendar baseCal = Calendar.getInstance();
	// baseCal.setTime(DateUtils.truncate(dateToCheck, Calendar.DATE));
	// uses apache package--truncates dateToCheck to midnight of that date
	baseCal.setTime(dateToCheck);
	List<Date> offlimitDates;

	// Grab the list of dates for the year. These SHOULD NOT be modified.
	synchronized (computedDates) {
	    int year = baseCal.get(Calendar.YEAR);

	    // If the map doesn't already have the dates computed, create them.
	    if (!computedDates.containsKey(year)) {
		computedDates.put(year, getOfflimitDates(year));
	    }
	    offlimitDates = computedDates.get(year);
	}

	// Determine if the date is on a weekend.
	int dayOfWeek = baseCal.get(Calendar.DAY_OF_WEEK);
	boolean onWeekend = dayOfWeek == Calendar.SATURDAY
		|| dayOfWeek == Calendar.SUNDAY;

	// If it's on a holiday, increment and test again
	// If it's on a weekend, increment necessary amount and test again
	if (offlimitDates.contains(dateToCheck) || onWeekend) {// changed this,
							       // was baseCal
	    return false;
	} else {
	    return true;
	}
    }

    /**
     * 
     * This method will calculate the next business day after the one input.
     * This leverages the isBusinessDay heavily, so look at that documentation
     * for further information.
     * 
     * @param startDate
     *            the Date of which you need the next business day.
     * @return The next business day. I.E. it doesn't fall on a weekend, a
     *         holiday or the official observance of that holiday if it fell on
     *         a weekend.
     * 
     */
    public static int getNextBusinessDay(int startDateInt) {
	// Increment the Date object by a Day and clear out hour/min/sec
	// information
	// Date startDate = convertToDate(startDateInt);

	// int nextDayInt =
	// convertToDateInt(DateUtils.truncate(addDays(startDate, 1),
	// Calendar.DATE));
	int nextDayInt = addDaysInt(startDateInt, 1);
	// If tomorrow is a valid business day, return it
	if (isBusinessDay(nextDayInt)) {
	    return nextDayInt;
	} // Else we recursively call our function until we find one.
	else {
	    return getNextBusinessDay(nextDayInt);
	}
    }

    public static int getPrevBusinessDay(int startDateInt) {
	// Increment the Date object by a Day and clear out hour/min/sec
	// information

	// int prevDayInt =
	// convertToDateInt(DateUtils.truncate(addDays(startDate, -1),
	// Calendar.DATE));
	int prevDayInt = addDaysInt(startDateInt, -1);
	// If tomorrow is a valid business day, return it
	if (isBusinessDay(prevDayInt)) {
	    return prevDayInt;
	} // Else we recursively call our function until we find one.
	else {
	    return getPrevBusinessDay(prevDayInt);
	}
    }

    public static int getLatestBusinessDay(int startDateInt) {
	// Increment the Date object by a Day and clear out hour/min/sec
	// information

	// int prevDayInt =
	// convertToDateInt(DateUtils.truncate(addDays(startDate, -1),
	// Calendar.DATE));
	int prevDayInt = addDaysInt(startDateInt, -1);
	// If tomorrow is a valid business day, return it
	if (isBusinessDay(startDateInt)) {
	    return startDateInt;
	} // Else we recursively call our function until we find one.
	else {
	    return getLatestBusinessDay(prevDayInt);
	}
    }

    public static int getStartYear(int startDateInt) {

	Calendar tempCal = GregorianCalendar.getInstance();
	tempCal.setTime(convertToDate(startDateInt));
	int tempYear = tempCal.get(Calendar.YEAR) * 10000 + 101;
	return getPrevBusinessDay(tempYear);
    }

    /*
     * Based on a year, this will compute the actual dates of
     * 
     * Holidays Accounted For: New Year's Day Martin Luther King Jr. Day
     * President's Day Memorial Day Independence Day Labor Day Columbus Day
     * Veterans Day Thanksgiving Day Christmas Day
     */
    private static List<Date> getOfflimitDates(int year) {
	List<Date> offlimitDates = new ArrayList<Date>();

	Calendar baseCalendar = GregorianCalendar.getInstance();
	baseCalendar.clear();

	// Add in the static dates for the year.
	// New years day
	baseCalendar.set(year, Calendar.JANUARY, 1);
	offlimitDates.add(offsetForWeekend(baseCalendar));

	// Independence Day
	baseCalendar.set(year, Calendar.JULY, 4);
	offlimitDates.add(offsetForWeekend(baseCalendar));

	// Veterans Day --not NYSE Holiday
	// baseCalendar.set(year, Calendar.NOVEMBER, 11);
	// offlimitDates.add(offsetForWeekend(baseCalendar));

	// Christmas
	baseCalendar.set(year, Calendar.DECEMBER, 25);
	offlimitDates.add(offsetForWeekend(baseCalendar));

	// Now deal with floating holidays.
	// Martin Luther King Day
	offlimitDates.add(calculateFloatingHoliday(3, Calendar.MONDAY, year,
		Calendar.JANUARY));

	// Presidents Day
	offlimitDates.add(calculateFloatingHoliday(3, Calendar.MONDAY, year,
		Calendar.FEBRUARY));

	// Memorial Day
	offlimitDates.add(calculateFloatingHoliday(0, Calendar.MONDAY, year,
		Calendar.MAY));

	// Labor Day
	offlimitDates.add(calculateFloatingHoliday(1, Calendar.MONDAY, year,
		Calendar.SEPTEMBER));

	// Columbus Day -- not NYSE Holiday
	// offlimitDates.add(calculateFloatingHoliday(2, Calendar.MONDAY, year,
	// Calendar.OCTOBER));

	// Thanksgiving Day and Thanksgiving Friday -- Friday not NYSE Holiday
	Date thanksgiving = calculateFloatingHoliday(4, Calendar.THURSDAY,
		year, Calendar.NOVEMBER);
	offlimitDates.add(thanksgiving);
	// offlimitDates.add(addDays(thanksgiving, 1));// --Friday not NYSE
	// Holiday

	// add Good Friday
	Date goodFriday = GoodFridayObserved(year);
	offlimitDates.add(goodFriday);

	return offlimitDates;
    }

    private static Date GoodFridayObserved(int nYear) {
	// Get Easter Sunday and subtract two days
	int nEasterMonth = 0;
	int nEasterDay = 0;
	int nGoodFridayMonth = 0;
	int nGoodFridayDay = 0;
	Date dEasterSunday;

	dEasterSunday = EasterSunday(nYear);
	GregorianCalendar gc = new GregorianCalendar();
	gc.setTime(dEasterSunday);
	// nEasterMonth = dEasterSunday.getMonth();
	nEasterMonth = gc.get(Calendar.MONTH);
	// nEasterDay = dEasterSunday.getDate();
	nEasterDay = gc.get(Calendar.DAY_OF_MONTH);
	if (nEasterDay <= 3 && nEasterMonth == 3) // Check if <= April 3rd
	{
	    switch (nEasterDay) {
	    case 3:
		nGoodFridayMonth = nEasterMonth - 1;
		nGoodFridayDay = nEasterDay - 2;
		break;
	    case 2:
		nGoodFridayMonth = nEasterMonth - 1;
		nGoodFridayDay = 31;
		break;
	    case 1:
		nGoodFridayMonth = nEasterMonth - 1;
		nGoodFridayDay = 31;
		break;
	    default:
		nGoodFridayMonth = nEasterMonth;
		nGoodFridayDay = nEasterDay - 2;
	    }
	} else {
	    nGoodFridayMonth = nEasterMonth;
	    nGoodFridayDay = nEasterDay - 2;
	}

	// return new Date(nYear, nGoodFridayMonth, nGoodFridayDay); // old date
	// format
	return new GregorianCalendar(nYear, nGoodFridayMonth, nGoodFridayDay)
		.getTime();
    }

    private static Date EasterSunday(int nYear) {
	/*
	 * Calculate Easter Sunday
	 * 
	 * Written by Gregory N. Mirsky
	 * 
	 * Source: 2nd Edition by Peter Duffett-Smith. It was originally from
	 * Butcher's Ecclesiastical Calendar, published in 1876. This algorithm
	 * has also been published in the 1922 book General Astronomy by Spencer
	 * Jones; in The Journal of the British Astronomical Association
	 * (Vol.88, page 91, December 1977); and in Astronomical Algorithms
	 * (1991) by Jean Meeus.
	 * 
	 * This algorithm holds for any year in the Gregorian Calendar, which
	 * (of course) means years including and after 1583.
	 * 
	 * a=year%19 b=year/100 c=year%100 d=b/4 e=b%4 f=(b+8)/25 g=(b-f+1)/3
	 * h=(19*a+b-d-g+15)%30 i=c/4 k=c%4 l=(32+2*e+2*i-h-k)%7
	 * m=(a+11*h+22*l)/451 Easter Month =(h+l-7*m+114)/31 [3=March, 4=April]
	 * p=(h+l-7*m+114)%31 Easter Date=p+1 (date in Easter Month)
	 * 
	 * Note: Integer truncation is already factored into the calculations.
	 * Using higher percision variables will cause inaccurate calculations.
	 */

	int nA = 0;
	int nB = 0;
	int nC = 0;
	int nD = 0;
	int nE = 0;
	int nF = 0;
	int nG = 0;
	int nH = 0;
	int nI = 0;
	int nK = 0;
	int nL = 0;
	int nM = 0;
	int nP = 0;
	int nEasterMonth = 0;
	int nEasterDay = 0;

	if (nYear < 1900) {
	    // if year is in java format put it into standard
	    // format for the calculation
	    nYear += 1900;
	}
	nA = nYear % 19;
	nB = nYear / 100;
	nC = nYear % 100;
	nD = nB / 4;
	nE = nB % 4;
	nF = (nB + 8) / 25;
	nG = (nB - nF + 1) / 3;
	nH = (19 * nA + nB - nD - nG + 15) % 30;
	nI = nC / 4;
	nK = nC % 4;
	nL = (32 + 2 * nE + 2 * nI - nH - nK) % 7;
	nM = (nA + 11 * nH + 22 * nL) / 451;

	// [3=March, 4=April]
	nEasterMonth = (nH + nL - 7 * nM + 114) / 31;
	--nEasterMonth;
	nP = (nH + nL - 7 * nM + 114) % 31;

	// Date in Easter Month.
	nEasterDay = nP + 1;

	// Uncorrect for our earlier correction.
	nYear -= 1900;

	// Populate the date object...
	// return new Date(nYear, nEasterMonth, nEasterDay); //old format for
	// date
	return new GregorianCalendar(nYear, nEasterMonth, nEasterDay).getTime();

    }

    /**
     * This method will take in the various parameters and return a Date object
     * that represents that value.
     * 
     * Ex. To get Martin Luther Kings BDay, which is the 3rd Monday of January,
     * the method call woudl be:
     * 
     * calculateFloatingHoliday(3, Calendar.MONDAY, year, Calendar.JANUARY);
     * 
     * Reference material can be found at:
     * http://michaelthompson.org/technikos/holidays.php#MemorialDay
     * 
     * @param nth
     *            0 for Last, 1 for 1st, 2 for 2nd, etc.
     * @param dayOfWeek
     *            Use Calendar.MODAY, Calendar.TUESDAY, etc.
     * @param year
     * @param month
     *            Use Calendar.JANUARY, etc.
     * @return
     */
    private static Date calculateFloatingHoliday(int nth, int dayOfWeek,
	    int year, int month) {
	Calendar baseCal = Calendar.getInstance();
	baseCal.clear();

	// Determine what the very earliest day this could occur.
	// If the value was 0 for the nth parameter, increment to the following
	// month so that it can be subtracted after.
	baseCal.set(year, month + ((nth <= 0) ? 1 : 0), 1);
	Date baseDate = baseCal.getTime();

	// Figure out which day of the week that this "earliest" could occur on
	// and then determine what the offset is for our day that we actually
	// need.
	int baseDayOfWeek = baseCal.get(Calendar.DAY_OF_WEEK);
	int fwd = dayOfWeek - baseDayOfWeek;

	// Based on the offset and the nth parameter, we are able to determine
	// the offset of days and then
	// adjust our base date.
	return addDays(baseDate, (fwd + (nth - (fwd >= 0 ? 1 : 0)) * 7));
    }

    /*
     * If the given date falls on a weekend, the method will adjust to the
     * closest weekday. I.E. If the date is on a Saturday, then the Friday will
     * be returned, if it's a Sunday, then Monday is returned.
     */
    private static Date offsetForWeekend(Calendar baseCal) {
	Date returnDate = baseCal.getTime();
	if (baseCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
	    // if (log.isDebugEnabled()) {
	    // log.debug("Offsetting the Saturday by -1: " + returnDate);
	    // }
	    return addDays(returnDate, -1);
	} else if (baseCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
	    // if (log.isDebugEnabled()) {
	    // log.debug("Offsetting the Sunday by +1: " + returnDate);
	    // }
	    return addDays(returnDate, 1);
	} else {
	    return returnDate;
	}
    }

    /**
     * Private method simply adds
     * 
     * @param dateToAdd
     * @param numberOfMonths
     * @return
     */
    private static Date addDays(Date dateToAdd, int numberOfDay) {
	if (dateToAdd == null) {
	    throw new IllegalArgumentException("Date can't be null!");
	}
	Calendar tempCal = Calendar.getInstance();
	tempCal.setTime(dateToAdd);
	tempCal.add(Calendar.DATE, numberOfDay);
	return tempCal.getTime();
    }

    public static int addDaysInt(int dateIntToAdd, int numberOfDay) {
	if (dateIntToAdd == 0) {
	    throw new IllegalArgumentException("Date can't be zero!");
	}
	Calendar tempCal = Calendar.getInstance();

	tempCal.setTime(convertToDate(dateIntToAdd));
	tempCal.add(Calendar.DATE, numberOfDay);
	return convertToDateInt(tempCal.getTime());
    }

    public static int addMonthsInt(int dateIntToAdd, int numberOfMonths) {
	if (dateIntToAdd == 0) {
	    throw new IllegalArgumentException("Date can't be zero!");
	}
	Calendar tempCal = Calendar.getInstance();

	tempCal.setTime(convertToDate(dateIntToAdd));
	tempCal.add(Calendar.MONTH, numberOfMonths);
	return convertToDateInt(tempCal.getTime());
    }

    /**
     * converts dateInto to date
     * 
     * @param dateInt
     *            dateInt to be converted
     * @return date
     */
    public static Date convertToDate(int dateInt) {
	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US); // mm.dd.yyyy
	try {
	    return sdf.parse(Integer.toString(dateInt));
	} catch (ParseException ex) {
	    Logger.getLogger(DateUtils.class.getName()).log(Level.SEVERE, null,
		    ex);
	    return null;
	}
    }

    /**
     * converts date to dateInt
     * 
     * @param thisDate
     *            date
     * @return
     */
    public static int convertToDateInt(Date thisDate) {
	SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.US); // mm.dd.yyyy
	return Integer.parseInt(sdf.format(thisDate));
    }

    public static String convertToShort(int dateInt) {
	Date date = convertToDate(dateInt);
	SimpleDateFormat sdf = new SimpleDateFormat(
		ReportControlPanel.getDateFormat(), Locale.getDefault());
	return sdf.format(date);
    }

    /**
     * converts dateInt to Calendar
     * 
     * @param thisDateInt
     * @return
     */
    public static Calendar convertToCal(int thisDateInt) {
	Calendar gc = new GregorianCalendar();
	gc.setTime(convertToDate(thisDateInt));
	return gc;
    }

    /**
     * gets days between any two dates (integer form) regardless of order
     * 
     * @param dateInt1
     *            first date (integer)
     * @param dateInt2
     *            second date (integer)
     * @return
     */
    public static int getDaysBetween(int dateInt1, int dateInt2) {
	Calendar d1 = convertToCal(dateInt1);
	Calendar d2 = convertToCal(dateInt2);

	if (d1.after(d2)) {
	    // swap dates so that d1 is start and d2 is end
	    Calendar swap = d1;
	    d1 = d2;
	    d2 = swap;
	}

	int days = d2.get(Calendar.DAY_OF_YEAR) - d1.get(Calendar.DAY_OF_YEAR);
	int y2 = d2.get(Calendar.YEAR);
	if (d1.get(Calendar.YEAR) != y2) {
	    d1 = (Calendar) d1.clone();
	    do {
		days += d1.getActualMaximum(Calendar.DAY_OF_YEAR);
		d1.add(Calendar.YEAR, 1);
	    } while (d1.get(Calendar.YEAR) != y2);
	}
	return days;
    }

    /**
     * converts dateInt to excel date
     * 
     * @param dateInt
     *            dateInt to be converted
     * @return exceldate (days from 1/1/1900)
     */
    public static double getExcelDateValue(int dateInt) {
	GregorianCalendar dateStart = new GregorianCalendar(1899, 11, 30);
	int dateStartInt = convertToDateInt(dateStart.getTime());
	Integer daysBetwInt = getDaysBetween(dateStartInt, dateInt);
	return daysBetwInt.doubleValue();
    }

    public static String getExcelDateString(int dateInt) {
	GregorianCalendar dateStart = new GregorianCalendar(1899, 11, 30);
	int dateStartInt = convertToDateInt(dateStart.getTime());
	Integer daysBetwInt = getDaysBetween(dateStartInt, dateInt);
	double daysBetwD = daysBetwInt.doubleValue();
	return Double.toString(daysBetwD);
    }

    /**
     * gets latest business day before "today" (system date)
     * 
     * @return dateInt of last business day
     */
    public static int getLastCurrentDateInt() {
	Date now = new Date();
	int dateIntNow = convertToDateInt(now);
	return getPrevBusinessDay(dateIntNow);
    }

}
