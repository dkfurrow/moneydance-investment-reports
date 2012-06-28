/* DateMap.java
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


import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;


public class DateMap {
    private TreeMap<Integer, Double> map;

    DateMap() {
        map = new TreeMap<Integer, Double>();
    }


    DateMap(DateMap old) {
        map = new TreeMap<Integer, Double>();
        map.putAll(old.map);
    }


    public Double put(Integer dateInt, Double value)
    {
        return map.put(dateInt, value);
    }


    public TreeMap<Integer, Double> getMap() {
        return map;
    }


    public Double get(Integer dateInt) {
        return map.get(dateInt);
    }


    public Set<Integer> keySet() {
        return map.keySet();
    }


    public Integer firstKey() {
        return map.firstKey();
    }


    public Integer lastKey() {
        return map.lastKey();
    }


    public boolean isEmpty() {
        return map.isEmpty();
    }


    public Collection<Double> values() {
        return map.values();
    }



    /**
     * Adds date/cashflow pair to this date map, incrementing old value
     *
     * @param dateInt
     *            dateInto to add
     *
     * @param addValue
     *            cashflow to add
     *
     * @return old value
     */
    public Double add(Integer dateInt, Double incr) {
        Double oldVal = get(dateInt);
        if (oldVal == null) {
            return map.put(dateInt, incr);
        } else {
            return put(dateInt, oldVal + incr);
        }
    }


    /*
     * Combines date maps, either adding or subtracting cash flows.
     *
     * @param operand
     *          input map
     *
     * @param combType
     *          either "add" or "subtract"
     *
     * @return output map
     */
    public DateMap combine(DateMap operand, String combType) {
        DateMap outMap = new DateMap(this);

        if (operand != null) {
            for (Iterator<Integer> it = operand.keySet().iterator(); it.hasNext();) {
                Integer dateint2 = it.next();
                Double value2 = operand.get(dateint2);

                if (outMap.get(dateint2) == null) {
                    if ("add".equals(combType)) {
                        outMap.put(dateint2, value2);
                    }
                    if ("subtract".equals(combType)) {
                        outMap.put(dateint2, -value2);
                    }
                } else {
                    if ("add".equals(combType)) {
                        outMap.put(dateint2,
                                   this == null ? 0 : this.get(dateint2) + value2);
                    }
                    if ("subtract".equals(combType)) {
                        outMap.put(dateint2,
                                   this == null ? 0 : this.get(dateint2) - value2);
                    }
                }
            }
        }

        return outMap;
    }

}