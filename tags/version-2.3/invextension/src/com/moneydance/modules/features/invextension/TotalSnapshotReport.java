/* TotalSnapshotReport.java
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

import java.util.ArrayList;

import com.moneydance.modules.features.invextension.ReportTable.ColType;

/**
 * Generates total output for "Snapshot" Report
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */

public class TotalSnapshotReport<T extends AggregatingType, U extends AggregatingType>
	extends TotalReport<T, U> {
    private int reportDateInt;

   
    public TotalSnapshotReport(BulkSecInfo currentInfo, Class<T> firstAggClass,
	    Class<U> secondAggClass, Boolean isHierarchy, Boolean outputSingle,
	    int snapDateInt)
	    throws Exception {
	super(currentInfo, firstAggClass, secondAggClass, isHierarchy,
		outputSingle, new DateRange(snapDateInt));
	assert (snapDateInt != 0);
	reportDateInt = snapDateInt;
    }
    
    @Override
    public int getClosedPosColumn() {
	return 7;
    }

    private static ColType[] colTypes = new ColType[] { ColType.STRING,
	    ColType.STRING, ColType.STRING, ColType.STRING, ColType.STRING,
	    ColType.DOUBLE2, ColType.DOUBLE3, ColType.DOUBLE2, ColType.DOUBLE2,
	    ColType.DOUBLE2, ColType.PERCENT1, ColType.PERCENT1,
	    ColType.PERCENT1, ColType.PERCENT1, ColType.PERCENT1,
	    ColType.PERCENT1, ColType.PERCENT1, ColType.PERCENT1,
	    ColType.PERCENT1, ColType.PERCENT1, ColType.DOUBLE2,
	    ColType.DOUBLE2, ColType.DOUBLE2 };

    @Override
    public ColType[] getColumnTypes() {
	return colTypes;
    }

    @Override
    public int getFrozenColumn() {
	return 5;
    }

    @Override
    public String[] getReportHeader() {
	ArrayList<String> snapValues = new ArrayList<String>();
	snapValues.add("InvAcct");
	snapValues.add("Security");
	snapValues.add("SecType");
	snapValues.add("SecSubType");
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

    @Override
    public String getReportTitle() throws SecurityException,
	    IllegalArgumentException, NoSuchFieldException,
	    IllegalAccessException {
	StringBuilder output = new StringBuilder();
	output.append("Investment Performance Snapshot: ");
	output.append(DateUtils.convertToShort(reportDateInt));
	output.append(" -- Aggregate By: "
		+ super.getOutputName(firstAggregateClass));
	if (!secondAggregateClass.equals(AllAggregate.class)) {
	    output.append(" Then By: "
		    + super.getOutputName(secondAggregateClass));
	}

	return output.toString();
    }

    @Override
    public SecurityReport getLeafSecurityReport(
	    SecurityAccountWrapper securityAccountWrapper, DateRange dateRange) {
	return new SecuritySnapshotReport(securityAccountWrapper, dateRange);
    }
    
}
