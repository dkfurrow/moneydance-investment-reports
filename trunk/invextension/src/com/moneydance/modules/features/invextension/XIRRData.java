/*
 *  XIRRData.java
 *  Copyright (C) 2005 Gautam Satpathy
 *  gautam@satpathy.in
 *  www.satpathy.in
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package com.moneydance.modules.features.invextension;

/*
 *  Imports
 */

/**
 * Data structure to hold XIRR data.
 *
 * @author : gsatpath
 * @version : 1.0.0 Date: Oct 19, 2005, Time: 9:29:49 AM
 */
public class XIRRData {

    public int n;
    public double guess;
    public double[] values;
    public double[] dates;

    /**
     * Default Constructor.
     */
    public XIRRData() {
    }

    /**
     * Constructor.
     *
     * @param n       iteration number
     * @param guess   estimate of values
     * @param pValues cash flows
     * @param pDates  dates
     */
    public XIRRData(int n, double guess, double[] pValues, double[] pDates) {
        this.n = n;
        this.guess = guess;
        this.values = pValues;
        this.dates = pDates;
    }

    /**
     * Expensive method. Don't call in loops etc.
     *
     * @return results of iteration
     */
    public String toString() {
        String text;
        String valuesStr;
        String datesStr;

        text = "XIRRData - n = " + n + ", Guess = " + this.guess;
        valuesStr = ", Values = ";
        datesStr = ", Dates = ";
        for (int i = 0; i < this.values.length; i++) {
            valuesStr = valuesStr + this.values[i];
            if (i < this.values.length - 1) {
                valuesStr = valuesStr + ",";
            }
        }
        for (int i = 0; i < this.dates.length; i++) {
            datesStr = datesStr + this.dates[i];
            if (i < this.dates.length - 1) {
                datesStr = datesStr + ",";
            }
        }
        return text + valuesStr + datesStr;
    }

}   /*  End of the XIRRData class. */