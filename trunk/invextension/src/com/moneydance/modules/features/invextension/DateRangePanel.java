/*
 * DateRangePanel.java
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

import com.moneydance.awt.JDateField;
import com.moneydance.util.CustomDateFormat;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.prefs.BackingStoreException;

/**
 * Panel to allow user input for DateRange
 */
public class DateRangePanel extends JPanel {

    public static final String DATE_PATTERN = ((SimpleDateFormat) DateFormat
            .getDateInstance(DateFormat.SHORT, Locale.getDefault()))
            .toPattern();
    public static final CustomDateFormat DATE_FORMAT = new CustomDateFormat(DATE_PATTERN);
    private JDateField snapDateField = new JDateField(DATE_FORMAT);
    private JDateField fromDateField = new JDateField(DATE_FORMAT);
    private JDateField toDateField = new JDateField(DATE_FORMAT);
    private static final long serialVersionUID = -5752555026802594107L;
    private DateRange dateRange;
    private JComboBox<DateRange.REF_DATE> refDateComboBox = new JComboBox<>(new DefaultComboBoxModel<>(DateRange.REF_DATE.values()));
    private JComboBox<DateRange.DATE_RULE> dateRuleComboBox = new JComboBox<>(new DefaultComboBoxModel<>(DateRange.DATE_RULE.values()));
    private JCheckBox isSnapDateRefDateCheckbox = new JCheckBox("Snapshot Date = 'To' Date (else 'From' Date)",
            true);

    public DateRangePanel(DateRange dateRange) {
        this.dateRange = dateRange == null ? new DateRange() : dateRange;
        //initialize fields
        if (dateRange.getRefDate() != DateRange.REF_DATE.NONE && dateRange.getRefDate() != DateRange.REF_DATE.NONE) {
            refDateComboBox.setSelectedItem(dateRange.getRefDate());
            dateRuleComboBox.setSelectedItem(dateRange.getDateRule());
            refreshInputDates();
        } else {
            refDateComboBox.setSelectedItem(DateRange.REF_DATE.NONE);
            dateRuleComboBox.setSelectedItem(DateRange.DATE_RULE.NONE);
            fromDateField.setDateInt(dateRange.getFromDateInt());
            toDateField.setDateInt(dateRange.getToDateInt());
            snapDateField.setDateInt(dateRange.getSnapDateInt());
        }

        //set up listeners
        JButton resetButton = new JButton("Reset");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                populateDateRangePanel(new DateRange());
            }
        });
        refDateComboBox.addActionListener(new PanelActionListener());
        dateRuleComboBox.addActionListener(new PanelActionListener());
        isSnapDateRefDateCheckbox.addActionListener(new PanelActionListener());
        fromDateField.addActionListener(new PanelActionListener());
        toDateField.addActionListener(new PanelActionListener());
        snapDateField.addActionListener(new PanelActionListener());
        //initialize sub-panels
        JPanel ruleInputPanel = new JPanel();
        JPanel directInputPanel = new JPanel();
        JPanel masterPanel = new JPanel();
        //format sub-panels
        String[] titles = {"Rule Input", "Direct Input", "Date Range--Choose Input Method"};
        JPanel[] subPanels = {ruleInputPanel, directInputPanel, masterPanel};
        for (int i = 0; i < subPanels.length; i++) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            subPanels[i].setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        }
        //make master panel title red
        CompoundBorder compundBorder = (CompoundBorder) masterPanel.getBorder();
        TitledBorder titledBorder = (TitledBorder) compundBorder.getOutsideBorder();
        titledBorder.setTitleColor(Color.RED);
        // Layout sub-panels
        GridBagConstraints c;
        //*rule input*
        ruleInputPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(0, 5, 0, 5); // left-right padding only
        c.weightx = 1;  //justify everything vertically
        //labels
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        //add labels
        JLabel refDateLabel = new JLabel("Reference Date");
        ruleInputPanel.add(refDateLabel, c);
        c.gridy++;
        JLabel dateRuleLabel = new JLabel("Date Rule");
        ruleInputPanel.add(dateRuleLabel, c);
        c.gridy++;
        ruleInputPanel.add(isSnapDateRefDateCheckbox, c);
        //data
        c.gridx = 1;
        c.gridy = 0;
        //add data fields
        ruleInputPanel.add(refDateComboBox, c);
        c.gridy++;
        ruleInputPanel.add(dateRuleComboBox, c);
        c.gridy++;
        //*direct input*
        directInputPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(0, 5, 0, 5); // left-right padding only
        c.weightx = 1;  //justify everything vertically
        //labels
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        //add labels
        JLabel snapDateLabel = new JLabel("Report Snapshot Date");
        directInputPanel.add(snapDateLabel, c);
        c.gridy++;
        JLabel fromDateLabel = new JLabel("Report \"From\" Date");
        directInputPanel.add(fromDateLabel, c);
        c.gridy++;
        JLabel toDateLabel = new JLabel("Report \"To\" Date");
        directInputPanel.add(toDateLabel, c);
        //data
        c.gridx = 1;
        c.gridy = 0;
        c.insets = new Insets(0, 80, 0, 5);  //push data right to align
        // add data fields
        directInputPanel.add(snapDateField, c);
        c.gridy++;
        directInputPanel.add(fromDateField, c);
        c.gridy++;
        directInputPanel.add(toDateField, c);
        // lay out master panel
        masterPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        masterPanel.add(resetButton, c);
        c.gridy++;
        masterPanel.add(ruleInputPanel, c);
        c.gridy++;
        masterPanel.add(directInputPanel, c);
        // add to this
        this.setLayout(new BorderLayout());
        this.add(masterPanel, BorderLayout.CENTER);
    }

    /**
     * Test method for DateRangePanel
     *
     * @param args unused
     * @throws IllegalAccessException
     * @throws java.util.prefs.BackingStoreException
     * @throws NoSuchFieldException
     */
    public static void main(String[] args) throws IllegalAccessException, BackingStoreException,
            NoSuchFieldException {
        DateRange dateRange = new DateRange();
        DateRangePanel testPanel = dateRange.getDateRangePanel();
        @SuppressWarnings("unused")
        ReportControlPanel.TestFrame frame = new ReportControlPanel.TestFrame(testPanel);
    }

    /**
     * Returns Date in 2000, System Day, System Month
     *
     * @return "zero" date
     */
    public static int getZeroDateInt() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.YEAR, 2000);
        return DateUtils.convertToDateInt(cal.getTime());
    }

    DateRange getDateRange() {
        refreshInputDates();
        return dateRange;
    }


    public void populateDateRangePanel(DateRange dateRange) {
        this.dateRange = dateRange == null ? new DateRange() : dateRange;
        //initialize fields
        if (dateRange.getRefDate() != DateRange.REF_DATE.NONE && dateRange.getRefDate() != DateRange.REF_DATE.NONE) {
            refDateComboBox.setSelectedItem(dateRange.getRefDate());
            dateRuleComboBox.setSelectedItem(dateRange.getDateRule());
            refreshInputDates();
        } else {
            refDateComboBox.setSelectedItem(DateRange.REF_DATE.NONE);
            dateRuleComboBox.setSelectedItem(DateRange.DATE_RULE.NONE);
            fromDateField.setDateInt(dateRange.getFromDateInt());
            toDateField.setDateInt(dateRange.getToDateInt());
            snapDateField.setDateInt(dateRange.getSnapDateInt());
        }
    }

    /**
     * refresh dates to make sure dateRange matches inputs
     * important: rule input overrides direct input!
     */
    public void refreshInputDates() {
        if (refDateComboBox.getSelectedItem() != DateRange.REF_DATE.NONE && dateRuleComboBox.getSelectedItem() !=
                DateRange.DATE_RULE.NONE) {
            DateRange newDateRange = new DateRange((DateRange.REF_DATE) refDateComboBox.getSelectedItem(),
                    (DateRange.DATE_RULE) dateRuleComboBox.getSelectedItem(),
                    isSnapDateRefDateCheckbox.isSelected());
            fromDateField.setDateInt(newDateRange.getFromDateInt());
            toDateField.setDateInt(newDateRange.getToDateInt());
            snapDateField.setDateInt(newDateRange.getSnapDateInt());
            dateRange = newDateRange;
        } else {
            int nonSelectedGetDateInt = getZeroDateInt();
            DateRange newDateRange;
            if (!(fromDateField.getDateInt() == nonSelectedGetDateInt && toDateField.getDateInt() ==
                    nonSelectedGetDateInt && snapDateField.getDateInt() == nonSelectedGetDateInt)) {
                newDateRange = new DateRange(fromDateField.getDateInt() == nonSelectedGetDateInt ? 0 : fromDateField.getDateInt(),
                        toDateField.getDateInt() == nonSelectedGetDateInt ? 0 : toDateField.getDateInt(),
                        snapDateField.getDateInt() == nonSelectedGetDateInt ? 0 : snapDateField.getDateInt());
                if (fromDateField.getDateInt() == nonSelectedGetDateInt) fromDateField.setDateInt(0);
                if (toDateField.getDateInt() == nonSelectedGetDateInt) toDateField.setDateInt(0);
                if (snapDateField.getDateInt() == nonSelectedGetDateInt) snapDateField.setDateInt(0);
            } else {
                newDateRange = new DateRange(0, 0, 0);
                fromDateField.setDateInt(0);
                toDateField.setDateInt(0);
                snapDateField.setDateInt(0);
            }
            dateRange = newDateRange;
        }
    }

    final class PanelActionListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            refreshInputDates();
        }
    }
}
