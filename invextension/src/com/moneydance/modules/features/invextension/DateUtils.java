/*
 * DateUtils.java
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

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * generic methods to handle business date math
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
@SuppressWarnings("ALL")
public final class DateUtils {
    private static class intPair implements Comparable<intPair> {
        public int v1;
        public int v2;

        public intPair(int a1, int a2) {
            v1 = a1;
            v2 = a2;
        }

        public int compareTo(@NotNull intPair operand) {
            return this.v1 == operand.v1 ? this.v2 - operand.v2 : this.v1 - operand.v1;
        }
    }

    private static final long MillisPerDay = (24 * 60 * 60 * 1000);
    private static final GregorianCalendar dateStart = new GregorianCalendar(1899, Calendar.DECEMBER, 31);
    private static final transient Map<Integer, List<Date>> computedDates = new HashMap<>();

    private DateUtils() {
    }

    private static final HashMap<Integer, Boolean> ibdMemo = new HashMap<>();

    /*
     * This method will calculate the next business day after the one input.
     * This means that if the next day falls on a weekend or one of the
     * following holidays then it will try the next day.
     *
     * Holidays Accounted For: New Year's Day Martin Luther King Jr. Day
     * President's Day Memorial Day Independence Day Labor Day Columbus Day
     * Veterans Day Thanksgiving Day Christmas Day Good Friday
     */
    private static boolean isBusinessDay(int dateIntToCheck) {
        Boolean result = ibdMemo.get(dateIntToCheck);
        if (result != null) return result;

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
        boolean onWeekend = dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;

        result = !(offlimitDates.contains(dateToCheck) || onWeekend);
        ibdMemo.put(dateIntToCheck, result);
        return result;
    }

    /**
     * This method will calculate the next business day after the one input.
     * This leverages the isBusinessDay heavily, so look at that documentation
     * for further information.
     *
     * @param startDateInt the Date of which you need the next business day.
     * @return The next business day. I.E. it doesn't fall on a weekend, a
     * holiday or the official observance of that holiday if it fell on
     * a weekend.
     */
    @SuppressWarnings("unused")
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

    private static final HashMap<Integer, Integer> gpbdMemo = new HashMap<>();

    public static int getPrevBusinessDay(int startDateInt) {
        Integer result = gpbdMemo.get(startDateInt);
        if (result != null) {
            return result;
        }

        int prevDayInt = addDaysInt(startDateInt, -1);
        // If tomorrow is a valid business day, return it
        if (isBusinessDay(prevDayInt)) {
            gpbdMemo.put(startDateInt, prevDayInt);
            return prevDayInt;
        } // Else we recursively call our function until we find one.
        else {
            return getPrevBusinessDay(prevDayInt);
        }
    }

    private static final HashMap<Integer, Integer> glbdMemo = new HashMap<>();

    public static int getLatestBusinessDay(int startDateInt) {
        Integer result = glbdMemo.get(startDateInt);
        if (result != null) {
            return result;
        }

        // If today is a valid business day, return it
        if (isBusinessDay(startDateInt)) {
            glbdMemo.put(startDateInt, startDateInt);
            return startDateInt;
        } // Else we recursively call our function until we find one.
        else {
            int prevDayInt = addDaysInt(startDateInt, -1);
            return getLatestBusinessDay(prevDayInt);
        }
    }

    private static final HashMap<Integer, Integer> gsyMemo = new HashMap<>();

    public static int getStartYear(int startDateInt) {
        Integer result = gsyMemo.get(startDateInt);
        if (result != null) {
            return result;
        }

        Calendar tempCal = GregorianCalendar.getInstance();
        tempCal.setTime(convertToDate(startDateInt));
        int tempYear = tempCal.get(Calendar.YEAR) * 10000 + 101;
        result = getPrevBusinessDay(tempYear);
        gsyMemo.put(startDateInt, result);
        return result;
    }

    private static final HashMap<Integer, Integer> gsmMemo = new HashMap<>();

    public static int getStartMonth(int startDateInt) {
        Integer result = gsmMemo.get(startDateInt);
        if (result != null) {
            return result;
        }

        Calendar tempCal = GregorianCalendar.getInstance();
        tempCal.setTime(convertToDate(startDateInt));
        int tempYear = tempCal.get(Calendar.YEAR) * 10000 + (tempCal.get(Calendar.MONTH) + 1) * 100 + 1;

        result = getPrevBusinessDay(tempYear);
        gsmMemo.put(startDateInt, result);
        return result;
    }

    private static final HashMap<Integer, Integer> gsqMemo = new HashMap<>();
    private static final int[] quarterStarts = {1, 1, 1, 3, 3, 3, 6, 6, 6, 9, 9, 9};

    public static int getStartQuarter(int startDateInt) {
        Integer result = gsqMemo.get(startDateInt);
        if (result != null) {
            return result;
        }

        Calendar tempCal = GregorianCalendar.getInstance();
        tempCal.setTime(convertToDate(startDateInt));
        int tempYear = tempCal.get(Calendar.YEAR) * 10000 + quarterStarts[tempCal.get(Calendar.MONTH)] * 100 + 1;

        result = getPrevBusinessDay(tempYear);
        gsqMemo.put(startDateInt, result);
        return result;
    }

    // No need to memoize, as this method is only invoked once per year.
    /*
     * Based on a year, this will compute the actual dates of
     *
     * Holidays Accounted For: New Year's Day Martin Luther King Jr. Day
     * President's Day Memorial Day Independence Day Labor Day Columbus Day
     * Veterans Day Thanksgiving Day Christmas Day
     */
    private static List<Date> getOfflimitDates(int year) {
        List<Date> offlimitDates = new ArrayList<>();

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
        offlimitDates.add(calculateFloatingHoliday(3, Calendar.MONDAY, year, Calendar.JANUARY));

        // Presidents Day
        offlimitDates.add(calculateFloatingHoliday(3, Calendar.MONDAY, year, Calendar.FEBRUARY));

        // Memorial Day
        offlimitDates.add(calculateFloatingHoliday(0, Calendar.MONDAY, year, Calendar.MAY));

        // Labor Day
        offlimitDates.add(calculateFloatingHoliday(1, Calendar.MONDAY, year, Calendar.SEPTEMBER));

        // Columbus Day -- not NYSE Holiday
        // offlimitDates.add(calculateFloatingHoliday(2, Calendar.MONDAY, year,
        // Calendar.OCTOBER));

        // Thanksgiving Day and Thanksgiving Friday -- Friday not NYSE Holiday
        Date thanksgiving = calculateFloatingHoliday(4, Calendar.THURSDAY, year, Calendar.NOVEMBER);
        offlimitDates.add(thanksgiving);
        // Friday after Thanksgiving is not NYSE Holiday

        // Good Friday
        Date goodFriday = GoodFridayObserved(year);
        offlimitDates.add(goodFriday);

        return offlimitDates;
    }

    private static Date GoodFridayObserved(int nYear) {
        // Get Easter Sunday and subtract two days
        int nEasterMonth;
        int nEasterDay;
        int nGoodFridayMonth;
        int nGoodFridayDay;
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
                case 3 -> {
                    nGoodFridayMonth = nEasterMonth - 1;
                    nGoodFridayDay = nEasterDay - 2;
                }
                case 2 -> {
                    nGoodFridayMonth = nEasterMonth - 1;
                    nGoodFridayDay = 31;
                }
                case 1 -> {
                    nGoodFridayMonth = nEasterMonth - 1;
                    nGoodFridayDay = 31;
                }
                default -> {
                    nGoodFridayMonth = nEasterMonth;
                    nGoodFridayDay = nEasterDay - 2;
                }
            }
        } else {
            nGoodFridayMonth = nEasterMonth;
            nGoodFridayDay = nEasterDay - 2;
        }

        // return new Date(nYear, nGoodFridayMonth, nGoodFridayDay); // old date
        // format
        return new GregorianCalendar(nYear, nGoodFridayMonth, nGoodFridayDay).getTime();
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
	 * Using higher precision variables will cause inaccurate calculations.
	 */

        int nA;
        int nB;
        int nC;
        int nD;
        int nE;
        int nF;
        int nG;
        int nH;
        int nI;
        int nK;
        int nL;
        int nM;
        int nP;
        int nEasterMonth;
        int nEasterDay;

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

        // Un-correct our earlier correction.
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
     * the method call would be:
     *
     * calculateFloatingHoliday(3, Calendar.MONDAY, year, Calendar.JANUARY);
     *
     * Reference material can be found at:
     * http://michaelthompson.org/technikos/holidays.php#MemorialDay
     *
     * @param nth       0 for Last, 1 for 1st, 2 for 2nd, etc.
     * @param dayOfWeek Use Calendar.MONDAY, Calendar.TUESDAY, etc.
     * @param year      year
     * @param month     Use Calendar.JANUARY, etc.
     * @return date which corresponds to inputs
     */
    private static Date calculateFloatingHoliday(int nth, int dayOfWeek,
                                                 int year, int month) {
        Calendar baseCal = Calendar.getInstance();
        baseCal.clear();
        // Determine what the very earliest day this could occur.
        // If the value was 0 for the nth parameter, increment to the following
        // month so that it can be subtracted after.
        baseCal.set(year, month, 1);
        if (nth <= 0) baseCal.add(Calendar.MONTH, 1);
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
     * Add given number of days to a date.
     *
     * @param dateToAdd  The date
     * @param numberOfDay The number of days to add
     * @return new date
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

    private static final HashMap<intPair, Integer> adiMemo = new HashMap<>();

    public static int addDaysInt(int dateIntToAdd, int numberOfDays) {
        if (dateIntToAdd == 0) throw new IllegalArgumentException("Date can't be zero!");

        intPair p = new intPair(dateIntToAdd, numberOfDays);
        Integer result = adiMemo.get(p);
        if (result != null) {
            return result;
        }

        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(convertToDate(dateIntToAdd));
        tempCal.add(Calendar.DATE, numberOfDays);

        result = convertToDateInt(tempCal.getTime());
        adiMemo.put(p, result);
        return result;
    }

    private static final HashMap<intPair, Integer> admMemo = new HashMap<>();

    public static int addMonthsInt(int dateIntToAdd, int numberOfMonths) {
        if (dateIntToAdd == 0) {
            throw new IllegalArgumentException("Date can't be zero!");
        }

        intPair p = new intPair(dateIntToAdd, numberOfMonths);
        Integer result = admMemo.get(p);
        if (result != null) {
            return result;
        }

        Calendar tempCal = Calendar.getInstance();
        tempCal.setTime(convertToDate(dateIntToAdd));
        tempCal.add(Calendar.MONTH, numberOfMonths);

        result = convertToDateInt(tempCal.getTime());
        admMemo.put(p, result);
        return result;
    }

    /**
     * converts dateInto to date
     *
     * @param dateInt dateInt to be converted
     * @return date
     */
    public static Date convertToDate(int dateInt) {
        @SuppressWarnings("deprecation")
        Date nd = new Date(dateInt / 10000 - 1900, (dateInt / 100) % 100 - 1, dateInt % 100);

        return nd;
    }

    public static boolean isToday(int dateInt){
        Calendar nowCal = GregorianCalendar.getInstance();
        nowCal.setTime(new Date());
        Calendar compareCal = convertToCal(dateInt);
        return nowCal.get(Calendar.DAY_OF_MONTH) == compareCal.get(Calendar.DAY_OF_MONTH) &&
                nowCal.get(Calendar.MONTH) == compareCal.get(Calendar.MONTH) &&
                nowCal.get(Calendar.YEAR) == compareCal.get(Calendar.YEAR);

    }

    /**
     * converts date to dateInt
     *
     * @param thisDate date
     * @return date as integer
     */
    public static int convertToDateInt(Date thisDate) {
        @SuppressWarnings("deprecation")
        int nd = (thisDate.getYear() + 1900) * 10000 + (thisDate.getMonth() + 1) * 100 + thisDate.getDate();

        return nd;
    }

    public static String convertToShort(int dateInt) {
        Date date = convertToDate(dateInt);
        SimpleDateFormat sdf = new SimpleDateFormat(DateRangePanel.DATE_PATTERN, Locale.getDefault());
        return sdf.format(date);
    }

    private static final HashMap<Integer, Calendar> ctcMemo = new HashMap<>();

    /**
     * converts dateInt to Calendar
     *
     * @param dateInt date int to convert
     * @return calendar value
     */
    private static Calendar convertToCal(int dateInt) {
        Calendar result = ctcMemo.get(dateInt);
        if (result != null) {
            return result;
        }

        Calendar gc = new GregorianCalendar();
        gc.setTime(convertToDate(dateInt));

        ctcMemo.put(dateInt, gc);
        return gc;
    }

    /**
     * gets days between any two dates (integer form) regardless of order
     *
     * @param dateInt1 first date (integer)
     * @param dateInt2 second date (integer)
     * @return number of days between dates
     */
    public static int getDaysBetween(int dateInt1, int dateInt2) {
        Calendar di1 = convertToCal(dateInt1);
        Calendar di2 = convertToCal(dateInt2);

        return Math.round(Math.abs(di1.getTimeInMillis() - di2.getTimeInMillis()) / (float) MillisPerDay);
    }

    private static final HashMap<Integer, Long> gedvMemo = new HashMap<>();

    /**
     * converts dateInt to excel date
     *
     * @param dateInt dateInt to be converted
     * @return excel date (days from 1/1/1900)
     */
    public static long getExcelDateValue(int dateInt) {
        Long result = gedvMemo.get(dateInt);
        if (result != null) {
            return result;
        }

        int dateStartInt = convertToDateInt(dateStart.getTime());
        int daysBetwInt = getDaysBetween(dateStartInt, dateInt);

        gedvMemo.put(dateInt, (long) daysBetwInt);
        return daysBetwInt;
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
