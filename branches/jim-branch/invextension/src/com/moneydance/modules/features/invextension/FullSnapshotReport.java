/* FullSnapshotReport.java
 * Copyright 2011 Dale K. Furrow . All rights reserved.
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


import java.util.ArrayList;
import java.text.DateFormat;
import java.util.Iterator;
import java.util.SortedSet;

import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.CurrencyType;
import com.moneydance.modules.features.invextension.BulkSecInfo.AGG_TYPE;
import com.moneydance.modules.features.invextension.ReportTable.ColType;


/**
 * Base class for collection of security reports.
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/

public class FullSnapshotReport extends FullSecurityReport {
    private int reportDateInt;

    /**
     * Constructor for a snap report on all securities, accounts, and aggregate
     * of accounts.
     *
     * @param currentInfo
     *          current transaction info
     *
     * @param snapDateInt
     *          report date
     */
    public FullSnapshotReport(BulkSecInfo currentInfo, int snapDateInt) {
        super(currentInfo, new DateRange(snapDateInt));
        assert(snapDateInt != 0);
        reportDateInt = snapDateInt;
    }


    protected SecurityReport accountSecurityReport(Account account,
                                                   DateRange date,
                                                   AGG_TYPE aggType) {
        return new SecuritySnapshotReport(account, date.snapDateInt(), aggType);
    }


    protected SecurityReport transactionSecurityReport(Account account,
                                                       SortedSet<TransValuesCum> transSet,
                                                       CurrencyType currency,
                                                       DateRange date,
                                                       AGG_TYPE aggType) {
        return new SecuritySnapshotReport(account, transSet, currency, date.snapDateInt(), aggType);
    }


    public int getClosedPosColumn() {
        return 5;
    }


    private static ColType[] colTypes
    = new ColType[] { ColType.STRING, ColType.STRING, ColType.STRING, ColType.DOUBLE2,
                      ColType.DOUBLE3, ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2,
                      ColType.PERCENT1, ColType.PERCENT1, ColType.PERCENT1,
                      ColType.PERCENT1, ColType.PERCENT1, ColType.PERCENT1,
                      ColType.PERCENT1, ColType.PERCENT1, ColType.PERCENT1,
                      ColType.PERCENT1, ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2 };


    public ColType[] getColumnTypes() {
        return colTypes;
    }


    public int getFrozenColumn() {
        return 3;
    }


    public String[] getReportHeader() {
        ArrayList<String> snapValues = new ArrayList<String>();
        snapValues.add("PrntAcct");
        snapValues.add("Security");
        snapValues.add("Ticker");
        snapValues.add("LastPrice");
        snapValues.add("EndPos");
        snapValues.add("EndValue");
        snapValues.add("AbsPrcChg");
        snapValues.add("AbsValChg");
        snapValues.add("PctPrcChg");
        snapValues.add("TR1Day");
        snapValues.add("TR1Wk");
        snapValues.add("TR1Mth");
        snapValues.add("TR3Mth");
        snapValues.add("TR_YTD");
        snapValues.add("TR1Year");
        snapValues.add("TR3Year");
        snapValues.add("TR_ALL");
        snapValues.add("AnnRet_ALL");
        snapValues.add("AvgCost");
        snapValues.add("income");
        snapValues.add("TotGain");

        return snapValues.toArray(new String[snapValues.size()]);
    }


    public String getReportTitle() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        return "Investment Performance Snapshot: " + DateUtils.convertToShort(reportDateInt);
    }
}
