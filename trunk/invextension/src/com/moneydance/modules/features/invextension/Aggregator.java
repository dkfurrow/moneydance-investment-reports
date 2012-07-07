/* Aggregator.java
 * Copyright 2012 Dale Furrow . All rights reserved.
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

/**
 * SuperClass for all classes which are used to aggregate Security Reports
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public abstract class Aggregator {
    

    /**Output name of first aggregate used in report output--e.g. for an 
     * investment account, returns the investment Account name.
     * @return
     */
    String getFirstAggregateName() {
	return "~Null";
    }
    
    /**Output name of second aggregate used in report output, e.g. for 
     * SecuritySubType, the output is a "*" followed by the SecuritySubType
     * name, so that the report line will sort to the bottom of SecuritySubTypes
     * @return
     */
    String getSecondAggregateName() {
	return "~Null";
    }
    
    /**Output name of aggregate used in report output in the case of one aggregator,
     *  e.g. if we're aggregating only by Currency, then the output  the output
     *   is the currency name followed by a "*" so that the report line will 
     *   sort to the bottom of all report lines for that currency.
     * name
     * @return
     */
    String getAllAggregateName() {
	return "~Null";
    }
    
    /**Output generic name (e.g. "All Currencies" for Currency Type)
     * @return
     */
    String getDefaultName(){
	return "~Null";
    }

}
