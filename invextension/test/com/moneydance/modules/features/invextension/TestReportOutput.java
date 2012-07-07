package com.moneydance.modules.features.invextension;

import java.io.File;
import java.util.ArrayList;

import javax.swing.JFrame;

import com.moneydance.apps.md.controller.io.FileUtils;
import com.moneydance.apps.md.model.RootAccount;
import com.moneydance.awt.AwtUtil;
import com.moneydance.modules.features.invextension.ReportOutputTable.ColSizeOption;
import com.moneydance.modules.features.invextension.ReportControlPanel.ReportTableModel;


public class TestReportOutput extends JFrame{
    
    /**
     * 
     */
    private static final long serialVersionUID = -2315625753772573103L;
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    public static final File mdTestFile = new File("./resources/testMD02.md");
    public static final File testFile = new File("E:\\Temp\\testFile.csv");
    
    
    
    public static void main(String[] args) throws Exception {
	RootAccount root = FileUtils.readAccountsFromFile(mdTestFile, null);
	JFrame frame = new TestReportOutput();
	ReportControlPanel panel = new ReportControlPanel(root);
	panel.setDates(fromDateInt, toDateInt, toDateInt);
	panel.setOpaque(true);
	frame.setContentPane(panel);
	frame.pack();
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	AwtUtil.centerWindow(frame);
	frame.setVisible(true);
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
