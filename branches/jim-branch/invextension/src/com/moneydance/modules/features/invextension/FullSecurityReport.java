/* FullSecurityReport.java
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

import com.moneydance.apps.md.model.Account;
import com.moneydance.apps.md.model.CurrencyType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.SortedSet;

import com.moneydance.modules.features.invextension.BulkSecInfo.AGG_TYPE;
import com.moneydance.modules.features.invextension.ReportTable.ColType;


/**
 * Base class for collection of security reports.
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/


public abstract class FullSecurityReport {
    private ArrayList<SecurityReport> reports;
    private DateRange reportDate;

    public ArrayList<SecurityReport> getReports() {
        return reports;
    }


    public DateRange getReportDate() {
        return reportDate;
    }


    public FullSecurityReport(BulkSecInfo currentInfo, DateRange date) {
        reports = new ArrayList<SecurityReport>();
        reportDate = date;

        SecurityReport allInvestments = accountSecurityReport(null, date, AGG_TYPE.ALL_SEC);
        SecurityReport allCash = accountSecurityReport(null, date, AGG_TYPE.ALL_CASH);

        // Loop through investment accounts:
        for (Iterator it = currentInfo.invSec.keySet().iterator(); it.hasNext(); ) {
            Account invAcct = (Account) it.next();
            SecurityReport invReport = accountSecurityReport(invAcct, date, AGG_TYPE.ACCT_SEC);

            if (currentInfo.invSec.get(invAcct) != null) {
                // Loop through securitiesL
                for (Iterator it1 = currentInfo.invSec.get(invAcct).iterator(); it1.hasNext();) {
                    Account secAcct = (Account) it1.next();
                    SecurityReport securityReport
                        = transactionSecurityReport(secAcct,
                                                    currentInfo.transValuesCumMap.get(secAcct),
                                                    currentInfo.secCur.get(secAcct),
                                                    date,
                                                    AGG_TYPE.SEC);
                    reports.add(securityReport);
                    invReport.addTo(securityReport);
                }

                // Aggregated returns for securities:
                invReport.recomputeAggregateReturns();
                reports.add(invReport);

                // Add to aggregated securities:
                allInvestments.addTo(invReport);
            }

            // Investment account transactions (bank txns):
            SortedSet<TransValuesCum> parentSet = currentInfo.transValuesCumMap.get(invAcct);
            SecurityReport accountCash = transactionSecurityReport(invAcct,
                                                                   parentSet,
                                                                   currentInfo.secCur.get(invAcct),
                                                                   date,
                                                                   AGG_TYPE.ALL_CASH);
            // Add to all cash:
            allCash.addTo(accountCash);

            // Returns for cash account:
            SecurityReport cashReturn = invReport.computeCashReturns(accountCash, 
                                                                     AGG_TYPE.ACCT_CASH);
            reports.add(cashReturn);

            // Aggregated returns w/ cash accounted for:
            SecurityReport aggregateReturn
                = invReport.computeAggregateReturnWCash(accountCash, AGG_TYPE.ACCT_SEC_PLUS_CASH);
            reports.add(aggregateReturn);
        }

        // Returns for aggregated investment accounts:

        // Aggregated returns for all securities:
        allInvestments.recomputeAggregateReturns();
        reports.add(allInvestments);

        // cash returns for all accounts:
        SecurityReport allCashReport = allInvestments.computeCashReturns(allCash,
                                                                         AGG_TYPE.ALL_CASH);
        reports.add(allCashReport);

        // Aggregate returns (w/ cash) for all accounts:
        SecurityReport allAggRetSnap
            = allInvestments.computeAggregateReturnWCash(allCash, AGG_TYPE.ALL_SEC_PLUS_CASH);
        reports.add(allAggRetSnap);
    }


    protected abstract SecurityReport accountSecurityReport(Account account,
                                                            DateRange date,
                                                            AGG_TYPE aggType);



    protected abstract SecurityReport transactionSecurityReport(Account account,
                                                                SortedSet<TransValuesCum> transSet,
                                                                CurrencyType currency,
                                                                DateRange date,
                                                                AGG_TYPE aggType);

    public Object[][] getReportTable() {
        if (reports.isEmpty()) {
            return new Object[0][0];
        } else {
            Object[] firstRow = reports.get(0).toTableRow();
            Object[][] table = new Object[reports.size()][firstRow.length];

            for (int i = 0; i < reports.size(); i++) {
                Object[] row = reports.get(i).toTableRow();

                for (int j = 0; j < row.length; j++) {
                    table[i][j] = row[j];
                }
            }

            return table;
        }
    }

    public abstract int getClosedPosColumn();

    public abstract ColType[] getColumnTypes();

    public abstract int getFrozenColumn();

    public abstract String[] getReportHeader();

    public abstract String getReportTitle();
}