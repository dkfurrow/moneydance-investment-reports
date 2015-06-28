/*
 * AllAggregate.java
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

/**
 * Singleton class this is a "placeholder" aggregator
 * as it aggregates all security reports regardless of Investment Account,
 * Currency, SecurityType or SecuritySubType
 * <p/>
 * Version 1.0
 *
 * @author Dale Furrow
 */
@SuppressWarnings("unused")
//Suppressed warnings because of static field name use in method getDeclaredField
public class AllAggregate implements Aggregator {
    static String columnName = "InvAcct";
    private static AllAggregate uniqueInstance;

    private AllAggregate() {
    }

    public static AllAggregate getInstance() {
        if (uniqueInstance == null) {
            uniqueInstance = new AllAggregate();
        }
        return uniqueInstance;

    }

    @Override
    public String getName() {return null;}

    @Override
    public String getAggregateName() {
        return null;
    }

    @Override
    public String toString() {
        return null;
    }

    @Override
    public String getAllTypesName() {
        return null;
    }

    @Override
    public String getColumnName() {
        return columnName;
    }

    @Override
    public String getReportingName() {
        return null;
    }


}
