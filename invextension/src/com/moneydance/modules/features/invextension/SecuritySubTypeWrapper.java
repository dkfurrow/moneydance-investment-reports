/*
 * SecuritySubTypeWrapper.java
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


import com.infinitekind.moneydance.model.SecurityType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * Wrapper for Moneydance class SecuritySubType, adds increased functionality
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */

public class SecuritySubTypeWrapper implements Aggregator {
    // build static map of Moneydance included sub types
    static final String[] stockSubsetVals = new String[]{"None", "Large Cap", "Mid Cap", "Small Cap", "Growth", "Value"};
    static final String[] mutualSubsetVals = new String[]{"None", "U.S. Stocks", "International Stocks", "Bond",
            "Balanced/Targeted", "Money Market", "Exchange Traded Fund"};
    static final String[] cdSubsetVals = new String[]{"None", "Fixed Rate", "Variable Rate", "Callable",
            "Liquid", "Bump Up", "Brokered", "Zero Coupon"};
    static final String[] bondSubsetVals = new String[]{"None", "Corporate", "Municipal", "Revenue", "Junk"};
    static final String[] optionSubsetVals = new String[]{"None", "Nonqualified", "Incentive"};
    static final String[] otherSubsetVals = new String[]{"None", "Real Estate", "Gold", "Oil"};
    // name of aggregation method
    static String reportingName = "Security Sub Type";
    // column name for sorting
    static String columnName = "SecSubType";
    private static final HashMap<SecurityType, LinkedHashSet<String>> securitySubtypeMap = new HashMap<>();
    static {
        securitySubtypeMap.put(SecurityType.STOCK, new LinkedHashSet<>(Arrays.asList(stockSubsetVals)));
        securitySubtypeMap.put(SecurityType.MUTUAL, new LinkedHashSet<>(Arrays.asList(mutualSubsetVals)));
        securitySubtypeMap.put(SecurityType.CD, new LinkedHashSet<>(Arrays.asList(cdSubsetVals)));
        securitySubtypeMap.put(SecurityType.BOND, new LinkedHashSet<>(Arrays.asList(bondSubsetVals)));
        securitySubtypeMap.put(SecurityType.OPTION, new LinkedHashSet<>(Arrays.asList(optionSubsetVals)));
        securitySubtypeMap.put(SecurityType.OTHER, new LinkedHashSet<>(Arrays.asList(otherSubsetVals)));
    }
    String securitySubType;
    SecurityType securityType;
    SecurityAccountWrapper securityAccountWrapper;


    /**
     * empty constructor for AggregationController
     */
    public SecuritySubTypeWrapper() {
    }

    public SecuritySubTypeWrapper(String name) {
        securityAccountWrapper = null;
        securityType = null;
        this.securitySubType = name;
    }

    public SecuritySubTypeWrapper(SecurityAccountWrapper securityAccountWrapper) {
        this.securityAccountWrapper = securityAccountWrapper;
        securityType = securityAccountWrapper.getSecurityType();
        String subtypeStr = securityAccountWrapper.getSecuritySubType().trim();
        this.securitySubType = getModifiedSecuritySubType(subtypeStr);
        // if not null add to securitySubTypeMap
        if (securityType != null) {
            LinkedHashSet<String> securitySubTypes = securitySubtypeMap.get(securityType);
            securitySubTypes.add(securitySubType);
        }
    }

    public static HashMap<SecurityType, LinkedHashSet<String>> getSecuritySubtypeMap() {
        return securitySubtypeMap;
    }

    private String getModifiedSecuritySubType(String baseSubTypeStr) {
        if (baseSubTypeStr == null || baseSubTypeStr.length() == 0) {
            return "None";
        } else {
            return baseSubTypeStr;
        }

    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((securitySubType == null) ? 0 : securitySubType.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SecuritySubTypeWrapper other = (SecuritySubTypeWrapper) obj;
        if (securitySubType == null) {
            return other.securitySubType == null;
        } else return securitySubType.equals(other.securitySubType);
    }

    public String getName() {
        if (securityAccountWrapper != null) {
            String subTypeStr = securityAccountWrapper.getSecuritySubType();
            return getModifiedSecuritySubType(subTypeStr);
        } else {
            return securitySubType;
        }

    }

    public void setName(String name) {
        securitySubType = name;
    }

    @Override
    public String getAggregateName() {
        return this.securitySubType + " ";
    }

    @Override
    public String getAllTypesName() {
        return "SUBTYPES-ALL";
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public String getReportingName() {
        return reportingName;
    }

}
