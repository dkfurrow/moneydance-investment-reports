/*
 * DateMap.java
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


import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.math.BigDecimal;

/*
Map which matches dates to cash flows for returns
calculations
 */
public class DateMap {
    private TreeMap<Integer, BigDecimal> map;

    DateMap() {
        map = new TreeMap<>();
    }


    DateMap(DateMap old) {
        map = new TreeMap<>();
        map.putAll(old.map);
    }


    public BigDecimal put(Integer dateInt, BigDecimal value) { return map.put(dateInt, value); }


    public TreeMap<Integer, BigDecimal> getMap() {
        return map;
    }


    public BigDecimal get(Integer dateInt) {
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


    public Collection<BigDecimal> values() {
        return map.values();
    }


    /**
     * Adds date/cashflow pair to this date map, incrementing old value
     *
     * @param dateInt dateInto to add
     * @return old value
     */
    public BigDecimal add(Integer dateInt, BigDecimal incr) {
        BigDecimal oldVal = get(dateInt);
        if (oldVal == null) {
            return map.put(dateInt, incr);
        } else {
            return put(dateInt, oldVal.add(incr));
        }
    }


    /*
     * Combines date maps, either adding or subtracting cash flows.
     *
     * @param operand input map
     * @param combType either "add" or "subtract"
     * @return resultant output map
     */
    public DateMap combine(DateMap operand, String combType) {
        DateMap outMap = new DateMap(this);

        if (operand != null) {
            for (Integer operandDateInt : operand.keySet()) {
                BigDecimal value2 = operand.get(operandDateInt);

                if (outMap.get(operandDateInt) == null) {
                    if ("add".equals(combType)) {
                        outMap.put(operandDateInt, value2);
                    }
                    if ("subtract".equals(combType)) {
                        outMap.put(operandDateInt, value2.negate());
                    }
                } else {
                    if ("add".equals(combType)) {
                        outMap.put(operandDateInt, this.get(operandDateInt).add(value2));
                    }
                    if ("subtract".equals(combType)) {
                        outMap.put(operandDateInt, this.get(operandDateInt).subtract(value2));
                    }
                }
            }
        }

        return outMap;
    }

}