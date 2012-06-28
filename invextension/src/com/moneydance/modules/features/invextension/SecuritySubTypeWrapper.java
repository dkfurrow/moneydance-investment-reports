/* SecuritySubTypeWrapper.java
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
 * Wrapper for SecuritySubType
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class SecuritySubTypeWrapper extends AggregatingType {
    AccountWrapper accountWrapper;
    String securitySubType;
    static String defaultName = "~All-SubType";
    // default column to sort on
    static Integer defaultColumn = 3;
    // name of aggregation method
    static String outputName = "Security Sub Type";

    public SecuritySubTypeWrapper(AccountWrapper accountWrapper)
	    throws Exception {
	this.accountWrapper = accountWrapper;
	String subtypeStr = accountWrapper.getSecurityAccountWrapper().secAcct
		.getSecuritySubType();
	if (subtypeStr == null || subtypeStr.length() == 0) {
	    this.securitySubType = "None";
	} else {
	    this.securitySubType = subtypeStr;
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
	    if (other.securitySubType != null)
		return false;
	} else if (!securitySubType.equals(other.securitySubType))
	    return false;
	return true;
    }

    public String getName(){
	return this.securitySubType;
    }
    

    @Override
    public String getFirstAggregateName() {
	return this.securitySubType;
    }



    @Override
    public String getSecondAggregateName() {
	return "*" +  this.securitySubType;
    }



    @Override
    public String getAllAggregateName() {
	return this.securitySubType + "*";
    }
    
    @Override
    public String getDefaultName(){
   	return "~All-SubTypes";
       }

    

}
