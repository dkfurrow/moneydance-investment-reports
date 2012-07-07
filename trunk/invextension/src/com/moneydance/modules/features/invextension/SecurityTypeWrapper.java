/* SecurityTypeWrapper.java
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

import com.moneydance.apps.md.model.SecurityType;

/**
 * Wrapper for SecurityType property of Security
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class SecurityTypeWrapper extends Aggregator {
    AccountWrapper accountWrapper;
    SecurityType securityType;
    static String defaultName = "~All-SecType";
    // default column to sort on
    static Integer defaultColumn = 2;
    // name of aggregation method
    static String outputName = "Security Type";

    public SecurityTypeWrapper(AccountWrapper accountWrapper) throws Exception {
	this.accountWrapper = accountWrapper;
	SecurityType type = this.accountWrapper.getSecurityAccountWrapper()
		.getSecurityType();
	if (type == null) {
	    this.securityType = SecurityType.DEFAULT;
	} else {
	    this.securityType = type;
	}
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
	if (securityType != other.securityType)
	    return false;
	return true;
    }

    public String getName(){
	return this.securityType.name();
    }
    
    @Override
    public String getFirstAggregateName() {
	return this.securityType.toString();
    }



    @Override
    public String getSecondAggregateName() {
	return "*" +  this.securityType;
    }



    @Override
    public String getAllAggregateName() {
	return this.securityType.toString() + "*";
    }
    
    @Override
    public String getDefaultName(){
   	return "~All-Types";
       }
    

}
