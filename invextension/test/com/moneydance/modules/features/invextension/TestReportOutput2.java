package com.moneydance.modules.features.invextension;

import java.io.File;
import java.util.ArrayList;

import com.moneydance.apps.md.controller.io.FileUtils;
import com.moneydance.apps.md.model.RootAccount;
import com.moneydance.modules.features.invextension.ReportOutputTable.ColSizeOption;
import com.moneydance.modules.features.invextension.ReportControlPanel.ReportTableModel;


public class TestReportOutput2 {
//    
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final File mdTestFile = new File("./resources/testMD02.md");
    public static final File testFile = new File("E:\\Temp\\testFile.csv");
    public static final Class<InvestmentAccountWrapper> invAggClass = InvestmentAccountWrapper.class;
    public static final Class<Tradeable> tradeableAggClass = Tradeable.class;
    public static final Class<CurrencyWrapper> currencyAggClass = CurrencyWrapper.class;
    public static final Class<AllAggregate> allAggClass = AllAggregate.class;
    public static final Class<SecurityTypeWrapper> secTypeAggClass = SecurityTypeWrapper.class;
    public static final Class<SecuritySubTypeWrapper> secSubTypeAggClass = SecuritySubTypeWrapper.class;
    public static final boolean catHierarchy = false;
    public static final boolean rptOutputSingle = true;
//    
//    @SuppressWarnings({ "unchecked", "rawtypes" })
//    public static void main(String[] args) throws Exception {
//	RootAccount root = FileUtils.readAccountsFromFile(mdTestFile, null);
//	BulkSecInfo currentInfo = new BulkSecInfo(root, new GainsAverageCalc());
//	ArrayList<String[]> transActivityReport = currentInfo
//		.listTransValuesSet(currentInfo.invs);
//	File transActivityReportFile = new File("E:\\Temp"
//		+ "\\transActivityReport.csv");
//	IOUtils.writeArrayListToCSV(TransactionValues.listTransValuesHeader(),
//		transActivityReport, transActivityReportFile);
//	
//	TotalReport report = null;
//
//	report = new TotalFromToReport(currentInfo, invAggClass,
//		tradeableAggClass, catHierarchy, rptOutputSingle, fromDateInt,
//		toDateInt);
//
//	printReport(report, 0, 4);
//	
//	
//
//    }
//
//
//
//    private static void printReport(TotalReport report, int firstSort, int secondSort)
//	    throws NoSuchFieldException, IllegalAccessException {
//	System.out.println("report size" + report.getReports().size());
//	System.out.println("Object Length" + report.getReportTable().length);
//
//	Object[][] reportTable = report.getReportTable();
//	System.out.println("Report Table Length: " + reportTable.length);
//	System.out.println("Report Table Width: " + reportTable[0].length);
//	ReportTableModel model = new ReportTableModel(reportTable,
//		report.getReportHeader());
//	Object[][] object = reportTable;
//	StringBuffer outBuffer = writeObjectToStringBuffer(object);
//	IOUtils.writeResultsToFile(outBuffer, testFile);
//	System.out.println("Finished!");
//
//	ReportOutputTable.CreateAndShowTable(model, report.getColumnTypes(),
//		report.getClosedPosColumn(), report.getFrozenColumn(),
//		firstSort, secondSort, ColSizeOption.MAXCONTCOLRESIZE,
//		report.getReportTitle());
//    }
//    
//    
//    
//    public static StringBuffer writeObjectToStringBuffer(Object[][] object) {
//	StringBuffer outBuffer = new StringBuffer();
//	for (int i = 0; i < object.length; i++) {
//	    Object[] objects = object[i];
//	    for (int j = 0; j < objects.length; j++) {
//		Object element = objects[j] == null ? "*NULL*": objects[j];
//		if (j == objects.length - 1) {
//		    outBuffer.append(element.toString()).append("\r\n");
//		} else {
//		    outBuffer.append(element.toString()).append(",");
//		}
//	    }
//	}
//	return outBuffer;
//
//    }
    
   
}
