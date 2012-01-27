package com.moneydance.modules.features.invextension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.moneydance.apps.md.controller.io.FileUtils;
import com.moneydance.apps.md.model.RootAccount;
import com.moneydance.modules.features.invextension.ReportTable.ColSizeOption;
import com.moneydance.modules.features.invextension.SecReportPanel.ReportTableModel;


public class TestReportOutput {
    
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final File mdTestFile = new File("./resources/testMD01.md");
    public static final File testFile = new File("E:\\Temp\\testFile.csv");
    
    
    public static void main(String[] args) throws Exception {
	RootAccount root = FileUtils.readAccountsFromFile(mdTestFile, null);
	BulkSecInfo currentInfo = new BulkSecInfo(root);
	FullSecurityReport report
        = new FullFromToReport(currentInfo, fromDateInt, toDateInt);
	System.out.println("report size" + report.getReports().size());
	System.out.println("Object Length" + report.getReportTable().length);
	ReportTableModel model
        = new ReportTableModel(report.getReportTable(), report.getReportHeader());
	Object[][] object = model.data;
	StringBuffer outBuffer = writeObjectToStringBuffer(object);
	IOUtils.writeResultsToFile(outBuffer, testFile);
	System.out.println("Finished!");
	
	 ReportTable.CreateAndShowTable(model,
                 report.getColumnTypes(),
                 report.getClosedPosColumn(),
                 ColSizeOption.MAXCONTCOLRESIZE,
                 report.getFrozenColumn(),
                 report.getReportTitle());
	
    }
    
    
    
    public static StringBuffer writeObjectToStringBuffer(Object[][] object) {
	StringBuffer outBuffer = new StringBuffer();
	for (int i = 0; i < object.length; i++) {
	    Object[] objects = object[i];
	    for (int j = 0; j < objects.length; j++) {
		Object element = objects[j] == null ? "*NULL*": objects[j];
		if (j == objects.length - 1) {
		    outBuffer.append(element.toString()).append("\r\n");
		} else {
		    outBuffer.append(element.toString()).append(",");
		}
	    }
	}
	return outBuffer;

    }
    
   
}
