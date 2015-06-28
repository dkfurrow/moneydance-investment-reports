/*
 * Prefs.java
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;



/**
 * Preferences parameters for report configuration
 *
 * @author Dale Furrow
 */
public class Prefs {


    public static final String REPORT_CONFIG_PATH = "/com/moneydance/modules/features/invextension/ReportConfig";
    public static Preferences REPORT_CONFIG_PREFS = Preferences.userRoot().node(REPORT_CONFIG_PATH);
    public static final String STANDARD_NAME = " Standard"; //note space to preserve sort order!
    public static final String REPORT_PREFS_PATH = "/com/moneydance/modules/features/invextension/ReportControlPanel";
    static Preferences REPORT_PREFS = Preferences.userRoot().node(REPORT_PREFS_PATH);
    static final String RUN_ACTIVITY = "RUN_TRANS_ACTIVITY_REPORT";
    static final String RUN_SECURITIES_PRICES = "RUN_SECURITY_PRICES_REPORT";
    static final String LAST_REPORT_TYPE_RUN = "LAST_REPORT_TYPE_RUN";
    static final String LAST_REPORT_NAME_RUN = "LAST_REPORT_NAME_RUN";
    static final String USE_AVERAGE_COST_BASIS = "USE_AVERAGE_COST_BASIS";
    static final String USE_ORDINARY_RETURN = "USE_ORDINARY_RETURN";
    static final String EXPORT_DIR_PATH = "EXPORT_DIR_PATH";
    static final String AGGREGATION_MODE = "AGGREGATION_MODE";
    static final String OUTPUT_SINGLE = "OUTPUT_SINGLE";
    static final String NUM_FROZEN_COLUMNS = "NUM_FROZEN_COLUMNS";
    static final String CLOSED_POS_HIDDEN = "CLOSED_POS_HIDDEN";
    static final String VIEWHEADER = "VIEWHEADER";
    static final String EXCLUDEDACCOUNTNUMS = "EXCLUDEDACCOUNTNUMS";
    static final String INVESTMENTEXPENSENUMS = "INVESTMENTEXPENSENUMS";
    static final String INVESTMENTINCOMENUMS = "INVESTMENTINCOMENUMS";
    static final String DATERANGE = "DATERANGE";
    static final String ISSTANDARD = "ISSTANDARD";
    static final String FRAMEINFO = "FRAMEINFO";

    private Prefs() {
    }

    static void clearAllPrefs() throws BackingStoreException {
        REPORT_PREFS.removeNode();
        REPORT_CONFIG_PREFS.removeNode();
        REPORT_PREFS.flush();
        REPORT_CONFIG_PREFS.flush();
        REPORT_PREFS = Preferences.userRoot().node(REPORT_PREFS_PATH);
        REPORT_CONFIG_PREFS = Preferences.userRoot().node(REPORT_CONFIG_PATH);


    }

    static void resetToStart() throws BackingStoreException {
        REPORT_PREFS.removeNode();
        REPORT_CONFIG_PREFS.removeNode();
        REPORT_PREFS.flush();
        REPORT_CONFIG_PREFS.flush();
    }

    public static Preferences getRootPrefs() {
        return Preferences.userRoot().node("/com/moneydance/modules/features/invextension");
    }

    //utility methods follow
    public static void printPrefNodes(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            getRootPrefs().exportSubtree(baos);
            String outString = baos.toString("UTF-8");
            System.out.println(outString);
        } catch (IOException | BackingStoreException e) {
            e.printStackTrace();
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        try {
//            resetToStart();
//            clearAllPrefs();
            printPrefNodes();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


}
