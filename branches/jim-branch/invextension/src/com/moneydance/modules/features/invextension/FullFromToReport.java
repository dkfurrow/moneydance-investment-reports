/* FullFromToReport.java
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

public class FullFromToReport extends FullSecurityReport {
    private int reportFromDateInt;
    private int reportToDateInt;

    /**
     * Constructor for a FromTo report on all securities, accounts, and aggregate
     * of accounts.
     *
     * @param currentInfo
     *          current transaction info
     *
     * @param snapDateInt
     *          report date
     */
    public FullFromToReport(BulkSecInfo currentInfo, int fromDateInt, int toDateInt) {
        super(currentInfo, new DateRange(fromDateInt, toDateInt));
        assert(fromDateInt != 0 && toDateInt != 0);
        reportFromDateInt = fromDateInt;
        reportToDateInt = toDateInt;
    }


    protected SecurityReport accountSecurityReport(Account account,
                                                   DateRange date,
                                                   AGG_TYPE aggType) {
        return new SecurityFromToReport(account, date.fromDateInt(), date.toDateInt(), aggType);
    }


    protected SecurityReport transactionSecurityReport(Account account,
                                                       SortedSet<TransValuesCum> transSet,
                                                       CurrencyType currency,
                                                       DateRange date,
                                                       AGG_TYPE aggType) {
        return new SecurityFromToReport(account,
                                        transSet,
                                        currency,
                                        date.fromDateInt(),
                                        date.toDateInt(),
                                        aggType);
    }


    public int getClosedPosColumn() {
        return 8;
    }


    private static ColType[] colTypes
    = new ColType[] { ColType.STRING,
                      ColType.STRING, ColType.STRING, ColType.DOUBLE3, ColType.DOUBLE3,
                      ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2,
                      ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2,
                      ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2,
                      ColType.DOUBLE2, ColType.DOUBLE2, ColType.DOUBLE2,
                      ColType.PERCENT1, ColType.PERCENT1 };


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
        snapValues.add("StartPos");
        snapValues.add("EndPos");
        snapValues.add("StartPrice");
        snapValues.add("EndPrice");
        snapValues.add("StartValue");
        snapValues.add("EndValue");
        snapValues.add("buy");
        snapValues.add("sell");
        snapValues.add("shortSell");
        snapValues.add("coverShort");
        snapValues.add("income");
        snapValues.add("Expense");
        snapValues.add("longBasis");
        snapValues.add("shortBasis");
        snapValues.add("realizedGain");
        snapValues.add("unrealizedGain");
        snapValues.add("periodReturn");
        snapValues.add("percentReturn");
        snapValues.add("annualPercentReturn");

        return snapValues.toArray(new String[snapValues.size()]);
    }


    public String getReportTitle() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
        return "Investment Performance--From: "
            + DateUtils.convertToShort(reportFromDateInt) + " To: "
            + DateUtils.convertToShort(reportToDateInt);
    }
}
