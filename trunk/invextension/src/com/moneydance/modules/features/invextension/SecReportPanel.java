/*
 *  SecReportPanel.java
 *  Copyright (C) 2010 Dale Furrow
 *  dkfurrow@google.com
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, a copy may be found at
 *  http://www.gnu.org/licenses/lgpl.html
 */

package com.moneydance.modules.features.invextension;

import com.jgoodies.forms.factories.Borders.EmptyBorder;
import com.moneydance.apps.md.model.RootAccount;
import com.moneydance.awt.AwtUtil;
import com.moneydance.awt.JDateField;
import com.moneydance.util.CustomDateFormat;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;

/** produces panel for reports
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/
public class SecReportPanel extends javax.swing.JPanel { //implements ActionListener
    private static final long serialVersionUID = 1020488211446251526L;
    //variable declarations
    //MD Data
    RootAccount root;
    BulkSecInfo currentInfo;
    //GUI Fields
    CustomDateFormat df = new CustomDateFormat("M/d/yyyy");
    JLabel snapDateLabel = new javax.swing.JLabel("Report Snapshot Date");
    JDateField snapDateField = new JDateField(df);
    JLabel fromDateLabel = new javax.swing.JLabel("Report \"From\" Date");
    JDateField fromDateField = new JDateField(df);
    JLabel toDateLabel = new javax.swing.JLabel("Report \"To\" Date");
    JDateField toDateField = new JDateField(df);
    JButton dirChooserButton = new javax.swing.JButton("Set output folder");
    JTextField directoryOutputField = new javax.swing.JTextField(getLastFileUsed());
    JCheckBox snapReportCheckbox = new javax.swing.JCheckBox("Shapshot Report");
    JCheckBox fromToReportCheckbox = new javax.swing.JCheckBox("\"From/To\" Report");
    JCheckBox transActivityCheckbox = new javax.swing.JCheckBox("Transaction Activity");
    JCheckBox secPricesCheckbox = new javax.swing.JCheckBox("Securities Prices");
    JButton runReportsButton = new javax.swing.JButton("Run Reports");
    JTextField reportStatusField = new javax.swing.JTextField("Choose Reports to Run");

    JFileChooser chooser;
    
    /** Creates new form NewJPanel */
    public SecReportPanel(RootAccount root) {
        initComponents();
        this.root = root;
        if(root != null){
            this.currentInfo = new BulkSecInfo(root);
        } else {
            this.currentInfo = null;
        }
        
    }


    private void initComponents() {

        //Set up  date input fields to defaults
        int todayDateInt = DateUtils.getLastCurrentDateInt();
        int yearAgoDateInt = DateUtils.addMonthsInt(todayDateInt, -12);

        snapDateField.setDateInt(todayDateInt);
        toDateField.setDateInt(todayDateInt);
        fromDateField.setDateInt(yearAgoDateInt);
        //alignment
        snapDateField.setHorizontalAlignment(JTextField.RIGHT);
        toDateField.setHorizontalAlignment(JTextField.RIGHT);
        fromDateField.setHorizontalAlignment(JTextField.RIGHT);
        
        //set text field width, button color
        //arbitrary max width for text fields
	Dimension textFields = new Dimension(Math.max(
		directoryOutputField.getPreferredSize().width, 400), 14);  
        directoryOutputField.setPreferredSize(textFields);
        reportStatusField.setPreferredSize(textFields);
        runReportsButton.setForeground(Color.red);
        //add action listeners

        //button action listeners
        dirChooserButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dirChooserButtonActionPerformed(evt);
            }
        });
        runReportsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                runReportsButtonActionPerformed(evt);
            }
        });  
        
        //create and format sub-panels to load into main panel
        JPanel datePanel = new JPanel();
        JPanel checkBoxPanel = new JPanel();
        JPanel folderPanel = new JPanel();
        JPanel runPanel = new JPanel();
        
        // set all panel borders the same
        
	String[] titles = { "Report Dates", "Reports to Run",
		"Download Location", "Report Status" };
	JPanel[] panels = { datePanel, checkBoxPanel, folderPanel, runPanel };

	for (int i = 0; i < panels.length; i++) {
	    JPanel panel = panels[i];
	    String title = titles[i];
	    TitledBorder titledBorder = BorderFactory
		    .createTitledBorder(titles[i]);
	    Border emptyBorder = BorderFactory
		    .createEmptyBorder(5, 5, 5, 5);
	    titledBorder.setTitleColor(new Color(100, 100, 100));
	    panel.setBorder(BorderFactory.createCompoundBorder(titledBorder,
		    emptyBorder));
	}
       
        //layout sub-panels
	
        //date sub-panel
        datePanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(0, 5, 0, 5); // left-right padding only
        c.weighty = 1; //justify everything vertically
        //labels
        c.anchor = GridBagConstraints.WEST;
        c.weightx = 1; // fill space w/ labels
        c.gridx = 0;
        datePanel.add(snapDateLabel, c);
        c.gridy = 1;
        datePanel.add(fromDateLabel, c);
        c.gridy = 2;
        datePanel.add(toDateLabel, c);
        // date fields 
        c.weightx = 0; //normal-width date fields
        c.gridx = 1;
        c.gridy = 0;
        datePanel.add(snapDateField, c);
        c.gridy = 1;
        datePanel.add(fromDateField, c);
        c.gridy = 2;
        datePanel.add(toDateField, c);
        
        //check box sub-panel
        checkBoxPanel.setLayout(new GridLayout(4, 1));
        checkBoxPanel.add(snapReportCheckbox);
        checkBoxPanel.add(fromToReportCheckbox);
        checkBoxPanel.add(transActivityCheckbox);
        checkBoxPanel.add(secPricesCheckbox);
        
        // directory sub-panel
        folderPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 0, 5);
        folderPanel.add(dirChooserButton, c);
        c.gridx = 0;
        c.gridy = 1;
        c.ipady = 12;
        folderPanel.add(directoryOutputField, c);

        // run sub-panel (for program results)
        runPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.ipady = 12;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        runPanel.add(reportStatusField, c);

        //lay out main panel
        this.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH; //fill all components to button
        //add date sub-panel
        c.gridx = 0;
        c.gridy = 0;
        c.insets = new Insets(5, 5, 5, 5);
        this.add(datePanel, c);
        // add check box sub-panel
        c.gridx = 1;
        c.gridy = 0;
        this.add(checkBoxPanel, c);
        // all remaining components center, panel-width
        c.anchor = GridBagConstraints.CENTER;
        c.gridwidth = 2;
        // add folder sub-panel
        c.gridx = 0;
        c.gridy = 1;        
        this.add(folderPanel, c);
        // add Run Reports Button
        c.fill = GridBagConstraints.NONE; //want normal-sized button
        c.gridy = 2;
        this.add(runReportsButton, c);
        // add Run sub-panel (Program Results)
        c.fill = GridBagConstraints.BOTH;// back to fill all components
        c.gridy = 3;
        this.add(runPanel, c);

    } // end initcomponents

    private void dirChooserButtonActionPerformed(java.awt.event.ActionEvent evt) {
        showFileChooser();
    }

    private void runReportsButtonActionPerformed(java.awt.event.ActionEvent evt) {
	reportStatusField.setText(null);
	reportStatusField.setText("Reports are running...");
	int fromDateInt = fromDateField.getDateInt();
	int toDateInt = toDateField.getDateInt();
	int snapDateInt = snapDateField.getDateInt();

	try {
	    if (snapReportCheckbox.isSelected()) {
		Object[][] snapData = ReportProd.getSnapReportObj(currentInfo,
			snapDateInt);
		ReportProd.outputSnapObjToTable(snapDateInt, snapData);
	    }
	    if (fromToReportCheckbox.isSelected()) {
		Object[][] ftData = ReportProd.getFromToReportObjs(currentInfo,
			fromDateInt, toDateInt);
		ReportProd.outputFTObjToTable(fromDateInt, toDateInt, ftData);
	    }

	    if (transActivityCheckbox.isSelected()) {
		ArrayList<String[]> transActivityReport = currentInfo
			.listTransValuesCumMap(currentInfo.transValuesCumMap);
		File transActivityReportFile = new File(
			directoryOutputField.getText()
				+ "\\transActivityReport.csv");
		IOUtils.writeArrayListToCSV(
			TransValuesCum.listTransValuesCumHeader(),
			transActivityReport, transActivityReportFile);

	    }

	    if (secPricesCheckbox.isSelected()) {
		ArrayList<String[]> secPricesReport = BulkSecInfo
			.ListAllCurrenciesInfo(currentInfo.allCurrTypes);
		File secPricesReportFile = new File(
			directoryOutputField.getText()
				+ "\\secPricesReport.csv");
		IOUtils.writeArrayListToCSV(
			BulkSecInfo.listCurrencySnapshotHeader(),
			secPricesReport, secPricesReportFile);
	    }
	} catch (Exception e) {

	    File errorFile = new File(directoryOutputField.getText()
		    + "\\errlog.txt");
	    StringBuffer erLOG = getStackTrace(e);
	    IOUtils.writeResultsToFile(erLOG, errorFile);
	}

	reportStatusField.setText(null);
	IOUtils.writeIniFile(directoryOutputField.getText());
	reportStatusField.setText("Reports have been run!");

    }
    
    public static void main(String[] args) {
	
	JFrame testFrame = new JFrame("Test Investment Reports Panel");
	testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	SecReportPanel testPanel = new SecReportPanel(null);
	testPanel.setOpaque(true);
	testFrame.setContentPane(testPanel);
	testFrame.pack();
	AwtUtil.centerWindow(testFrame);
	testFrame.setVisible(true);
	
	
    }


    public void showFileChooser() {
	chooser = new JFileChooser();
	chooser.setCurrentDirectory(new File("."));

	chooser.setDialogTitle("Choose Output Directory");
	chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
	chooser.setAcceptAllFileFilterUsed(false);
	if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
	    System.out.println("getCurrentDirectory(): "
		    + chooser.getCurrentDirectory().getAbsolutePath());
	    directoryOutputField.setText(null);
	    directoryOutputField.setText(chooser.getCurrentDirectory()
		    .getAbsolutePath());

	} else {
	    System.out.println("No Selection ");
	}
    }
    

     public static StringBuffer getStackTrace(Exception e)
    {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        e.printStackTrace(pw);
        pw.flush();
        sw.flush();
        return new StringBuffer(sw.toString());
    }

     public String getLastFileUsed(){
         String filePath = new String();
         String defaultDir = new File(".").getAbsolutePath();
         File iniFile = new File(defaultDir + "invextension.ini");
         if(iniFile.exists()){
             filePath = IOUtils.readIniFile(iniFile);
             return filePath;
         } else{
             return defaultDir;
         }

     }

}
