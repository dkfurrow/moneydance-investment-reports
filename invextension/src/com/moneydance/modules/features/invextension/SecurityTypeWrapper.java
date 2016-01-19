/*
 * SecurityTypeWrapper.java
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


/**
 * Wrapper for Moneydance Class SecurityType, adds increased functionality
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */

public class SecurityTypeWrapper implements Aggregator {


    // name of aggregation method
    static String reportingName = "Security Type";
    // column name for sorting
    static String columnName = "SecType";
    SecurityType securityType;
    String name;

    /**
     * empty constructor for aggregator
     */
    public SecurityTypeWrapper() {
    }

    public SecurityTypeWrapper(String name) {
        this.name = name;
    }

    public SecurityTypeWrapper(SecurityAccountWrapper securityAccountWrapper) throws Exception {

        SecurityType securityType = securityAccountWrapper.getSecurityType();
        if (securityType == null) {
            this.securityType = null;
        } else {
            this.securityType = securityType;
        }
        assert this.securityType != null;
        this.name = this.securityType.name().trim();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((securityType == null) ? 0 : securityType.hashCode());
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
        SecurityTypeWrapper other = (SecurityTypeWrapper) obj;
        return securityType == other.securityType;
    }

    public String getName() {
        if (securityType != null) {
            return securityType.name().trim();
        } else {
            return name;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getAggregateName() {
        return this.securityType.toString() + " ";
    }

    @Override
    public String getAllTypesName() {
        return "SECTYPES-ALL";
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
