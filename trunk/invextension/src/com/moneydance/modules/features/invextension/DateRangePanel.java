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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.prefs.BackingStoreException;

/**
 * Panel to allow user input for DateRange
 */
public class DateRangePanel extends JPanel {

    public static final String DATE_PATTERN = ((SimpleDateFormat) DateFormat
            .getDateInstance(DateFormat.SHORT, Locale.getDefault()))
            .toPattern();
    public static final CustomDateFormat DATE_FORMAT = new CustomDateFormat(DATE_PATTERN);
    public static final String DATE_RANGE_CHANGED = "dateRangeChanged";
    private List<PropertyChangeListener> listeners = new ArrayList<>();
    private JDateField snapDateField = new JDateField(DATE_FORMAT);
    private JDateField fromDateField = new JDateField(DATE_FORMAT);
    private JDateField toDateField = new JDateField(DATE_FORMAT);
    private static final long serialVersionUID = -5752555026802594107L;
    private DateRange dateRange;
    private JComboBox<DateRange.REF_DATE> refDateComboBox = new JComboBox<>(new DefaultComboBoxModel<>(DateRange.REF_DATE.values()));
    private JComboBox<DateRange.DATE_RULE> dateRuleComboBox = new JComboBox<>(new DefaultComboBoxModel<>(DateRange.DATE_RULE.values()));
    private JCheckBox isSnapDateRefDateCheckbox = new JCheckBox("Snapshot Date = 'To' Date (else 'From' Date)",
            true);
    private final Color notReadyColor = Color.RED;
    private final Color readyColor = new Color(0, 102, 0);
    private TitledBorder masterBorder;
    private JPanel masterPanel;
    private Focus focus;
    private enum Focus{DATE_RULE, DATE_INPUT}



    public DateRangePanel(DateRange dateRange) {

        this.dateRange = dateRange;
        snapDateField.setReformatOnFocusLost(false);
        fromDateField.setReformatOnFocusLost(false);
        toDateField.setReformatOnFocusLost(false);
        //set up listeners
        JButton resetButton = new JButton("Reset Dates");
        resetButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int lastCurrentDateInt = DateUtils.getLastCurrentDateInt();
                populateDateRangePanel(new DateRange(19700101, lastCurrentDateInt,
                        lastCurrentDateInt));
            }
        });
        ItemChangeListener itemChangeListener = new ItemChangeListener();
        refDateComboBox.addItemListener(itemChangeListener);
        dateRuleComboBox.addItemListener(itemChangeListener);
        isSnapDateRefDateCheckbox.addItemListener(itemChangeListener);
        BoxFocusListener boxFocusListener = new BoxFocusListener();
        refDateComboBox.addFocusListener(boxFocusListener);
        dateRuleComboBox.addFocusListener(boxFocusListener);
        isSnapDateRefDateCheckbox.addFocusListener(boxFocusListener);

        DateFieldDocumentListener dateFieldDocumentListener = new DateFieldDocumentListener();
        fromDateField.getDocument().addDocumentListener(dateFieldDocumentListener);
        toDateField.getDocument().addDocumentListener(dateFieldDocumentListener);
        snapDateField.getDocument().addDocumentListener(dateFieldDocumentListener);

        DateFieldFocusListener dateFieldFocusListener = new DateFieldFocusListener();
        fromDateField.addFocusListener(dateFieldFocusListener);
        toDateField.addFocusListener(dateFieldFocusListener);
        snapDateField.addFocusListener(dateFieldFocusListener);

        //initialize sub-panels
        JPanel ruleInputPanel = new JPanel();
        JPanel directInputPanel = new JPanel();
        masterPanel = new JPanel();
        //format sub-panels
        String masterTitle = "Date Range--Initial Title";
        String[] titles = {"Rule Input", "Direct Input", masterTitle};
        JPanel[] subPanels = {ruleInputPanel, directInputPanel, masterPanel};
        for (int i = 0; i < subPanels.length; i++) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            subPanels[i].setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        }
        //make master panel title red
        CompoundBorder compoundBorder = (CompoundBorder) masterPanel.getBorder();
        masterBorder = (TitledBorder) compoundBorder.getOutsideBorder();


        // Layout sub-panels
        GridBagConstraints c;
        //*rule input*
        ruleInputPanel.setLayout(new GridBagLayout());
        c = new GridBagConstraints();
        c.insets = new Insets(0, 5, 0, 5); // left-right padding only
//        c.weightx = 1;  //justify everything vertically
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
        refDateComboBox.setPreferredSize(dateRuleComboBox.getPreferredSize());
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




        //initialize fields
        if(dateRange == null){
            resetComboBoxes();
            nullDateInputs();
        } else if (dateRange.getRefDate() != DateRange.REF_DATE.NONE && dateRange.getRefDate() != DateRange.REF_DATE.NONE) {
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

        setStatus();

    }

    private void setStatus(){
        if((hasValidDateRule() || hasValidEnteredDates()) && dateRange != null){
            masterBorder.setTitleColor(readyColor);
            masterBorder.setTitle("Date Range--Valid Range Entered");
            repaint();
        } else {
            masterBorder.setTitleColor(notReadyColor);
            masterBorder.setTitle("Date Range--Choose Input Method");
            repaint();
        }

    }

    public void addChangeListener(PropertyChangeListener newListener){
        this.listeners.add(newListener);
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
        DateRangePanel testPanel = new DateRangePanel(null);
        @SuppressWarnings("unused")
        ReportControlPanel.TestFrame frame = new ReportControlPanel.TestFrame(testPanel);
    }



    DateRange getDateRange() {
        refreshInputDates();
        return dateRange;
    }

    private void notifyListeners(String property, DateRange oldValue, DateRange newValue) {
        for (PropertyChangeListener listener : listeners) {
            listener.propertyChange(new PropertyChangeEvent(this, property, oldValue, newValue));
        }
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

    private boolean hasValidEnteredDates() {
        return new DateRange(fromDateField.getDateInt(),
                toDateField.getDateInt(), snapDateField.getDateInt()).isValid();
    }

    private boolean hasValidDateRule() {
        return refDateComboBox.getSelectedItem() != DateRange.REF_DATE.NONE && dateRuleComboBox.getSelectedItem() !=
                DateRange.DATE_RULE.NONE;
    }

    private void resetComboBoxes(){
        refDateComboBox.setSelectedItem(DateRange.REF_DATE.NONE);
        dateRuleComboBox.setSelectedItem(DateRange.DATE_RULE.NONE);
    }

    private boolean isDateInputNulled(){
        int nullDateInt = DateRange.getNullDateInt();
        return(fromDateField.getDateInt() == nullDateInt &&
                toDateField.getDateInt() == nullDateInt &&
                snapDateField.getDateInt() == nullDateInt);
    }

    private void nullDateInputs() {
        int nullDateInt = DateRange.getNullDateInt();
        fromDateField.setDateInt(nullDateInt);
        toDateField.setDateInt(nullDateInt);
        snapDateField.setDateInt(nullDateInt);
        dateRange = null;
    }


    /**
     * checkAndRefresh dates to make sure dateRange matches inputs
     * important: rule input overrides direct input!
     */
    public void refreshInputDates() {
        if (hasValidDateRule()) {
            DateRange oldDateRange = dateRange == null ? new DateRange() : dateRange;
            DateRange newDateRange = new DateRange((DateRange.REF_DATE) refDateComboBox.getSelectedItem(),
                    (DateRange.DATE_RULE) dateRuleComboBox.getSelectedItem(),
                    isSnapDateRefDateCheckbox.isSelected());
            fromDateField.setDateInt(newDateRange.getFromDateInt());
            toDateField.setDateInt(newDateRange.getToDateInt());
            snapDateField.setDateInt(newDateRange.getSnapDateInt());
            dateRange = newDateRange;
            setStatus();
            notifyListeners(DATE_RANGE_CHANGED, oldDateRange, newDateRange);
        } else {
            if(focus == Focus.DATE_RULE && !isDateInputNulled()){
                nullDateInputs();
                setStatus();
            }
        }
    }



    class ItemChangeListener implements ItemListener {
        @Override
        public void itemStateChanged(ItemEvent event) {
            Object item = event.getItem();
            if (event.getStateChange() == ItemEvent.SELECTED) {
                refreshInputDates();
            }
            else if(event.getStateChange() == ItemEvent.DESELECTED){
                if(item instanceof JCheckBox) refreshInputDates();
            }
        }
    }

    class DateFieldDocumentListener implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent e) {
            checkAndRefresh(e);
        }
        @Override
        public void removeUpdate(DocumentEvent e) {  }
        @Override
        public void changedUpdate(DocumentEvent e) { }
        void checkAndRefresh(DocumentEvent e) {
            if(hasValidEnteredDates()) {
                DateRange oldDateRange = dateRange;
                dateRange = new DateRange(fromDateField.getDateInt(),
                        toDateField.getDateInt(), snapDateField.getDateInt());
                notifyListeners(DATE_RANGE_CHANGED, oldDateRange, dateRange);
            } else {
                notifyListeners(DATE_RANGE_CHANGED, new DateRange(), new DateRange());
            }
            setStatus();
        }
    }

    class DateFieldFocusListener implements FocusListener{

        @Override
        public void focusGained(FocusEvent e) {
            focus = Focus.DATE_INPUT;
            resetComboBoxes();
        }

        @Override
        public void focusLost(FocusEvent e) {

        }
    }

    class BoxFocusListener implements FocusListener{
        @Override
        public void focusGained(FocusEvent e) {
            focus = Focus.DATE_RULE;
        }
        @Override
        public void focusLost(FocusEvent e) { }
    }

}
