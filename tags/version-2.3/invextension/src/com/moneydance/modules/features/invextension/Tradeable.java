/* Tradeable.java
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
 * Tradeable class is a derivative of Currency.
 * Represents Securities of all currencies Except the currency representing
 * uninvested cash (BulkSecInfo.cashCurType)
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class Tradeable extends AggregatingType {
    CurrencyWrapper currencyWrapper;
    Boolean isTradeable;
    static String defaultName = "~AllSec+Cash";
    // default column to sort on
    static Integer defaultColumn = 1;
    // name of aggregation method
    static String outputName = "Tradeable Security/Uninvested Cash";
    
    public Tradeable(CurrencyWrapper currencyWrapper){
	this.currencyWrapper = currencyWrapper;
	if(this.currencyWrapper.curType == BulkSecInfo.cashCurType){
	    this.isTradeable = false;
	} else {
	    this.isTradeable = true;
	}
    }
    
    public Tradeable(Boolean isTradeable){
	this.currencyWrapper = null;
	this.isTradeable = isTradeable;
    }
    
    public Boolean isTradeable(){
	return this.isTradeable;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result
		+ ((isTradeable == null) ? 0 : isTradeable.hashCode());
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
	Tradeable other = (Tradeable) obj;
	if (isTradeable == null) {
	    if (other.isTradeable != null)
		return false;
	} else if (!isTradeable.equals(other.isTradeable))
	    return false;
	return true;
    }
    
    @Override
    public String getFirstAggregateName() {
	return this.isTradeable ? "*AllSec" : "*Cash";
    }


    @Override
    public String getSecondAggregateName() {
	return this.isTradeable ? "*AllSec" : "*Cash";
    }


    @Override
    public String getAllAggregateName() {
	return this.isTradeable ? "*AllSec" : "*Cash";
    }
    
    @Override
    public String getDefaultName(){
	return "~All Sec + Cash";
    }

}
