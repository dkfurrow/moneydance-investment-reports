/*
 *  Prefs.java version 1.0 Jun 27, 2012
 *  Copyright 2012 Dale Furrow . All rights reserved.
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

import java.util.prefs.Preferences;

/**
 * Preferences class
 *
 * Version 1.0 
 * @author Dale Furrow
 *
 */
public class Prefs {
    static Prefs uniqueInstance;
    static final Preferences reportPrefs = Preferences.userRoot().node(
	    "/com/moneydance/modules/features/invextension/SecReportPanel");
    static final String fromReportDate = "FROM_REPORT_DATE";
    static final String runSnapshot = "RUN_SNAPSHOT_REPORT";
    static final String runFromTo = "RUN_FROMTO_REPORT";
    static final String runTransActivity = "RUN_TRANS_ACTIVITY_REPORT";
    static final String runSecuritiesPrices = "RUN_SECURITY_PRICES_REPORT";
    static final String aggregationOptions = "AGGREGATION_OPTIONS";
    static final String costBasisUsed = "COST_BASIS_USED";
    static final String showSingletonAggregates = "SHOW_SINGLETON_AGGREGATES";
    static final String exportPathPref = "EXPORT_DIR_PATH";

    private Prefs() {
    }    
    
    

}
