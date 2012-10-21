/* CurrencyWrapper.java
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

import java.util.LinkedHashSet;

import com.moneydance.apps.md.model.CurrencyType;

/**
 * Wrapper for CurrencyType
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class CurrencyWrapper extends Aggregator {
    CurrencyType curType;
    int curID;
    String ticker;
    LinkedHashSet<SecurityAccountWrapper> secAccts;
    static String defaultName = "Ticker-ALL";
    // default column to sort on
    static Integer defaultColumn = 4;
    // name of aggregation method
    static String outputName = "Currency";

    public CurrencyWrapper(CurrencyType curType) {
	this.curType = curType;
	this.secAccts = new LinkedHashSet<SecurityAccountWrapper>();
	this.curID = this.curType.getID();
	if(curType.getTickerSymbol().isEmpty()){
	    this.ticker = "";
	} else {
	    this.ticker = curType.getTickerSymbol().trim();
	}
    }
    
    
    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + curID;
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
	CurrencyWrapper other = (CurrencyWrapper) obj;
	if (curID != other.curID)
	    return false;
	return true;
    }


    public void addSecAcct(SecurityAccountWrapper secAcct){
	this.secAccts.add(secAcct);
	
    } @Override
    public String getFirstAggregateOutput() {
	//should never happen
	return null;
    }



    @Override
    public String getSecondAggregateOutput() {
	//should never happen
	return null;
    }



    @Override
    public String getAllAggregateOutput() {
	return this.ticker + " ";
    }
    
    @Override
    public String getDefaultOutput(){
   	return "~All-Currencies";
       }
    
    

}
