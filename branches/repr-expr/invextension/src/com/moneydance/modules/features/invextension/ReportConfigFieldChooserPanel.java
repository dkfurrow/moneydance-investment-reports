/*
 * ReportConfigFieldChooserPanel.java
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

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.prefs.BackingStoreException;

/**
 * Field chooser panel to control the order and identity of fields to be included
 * in a given report
 */
public class ReportConfigFieldChooserPanel extends JPanel {

    private static final long serialVersionUID = -8990699863699414946L;
    private ReportControlPanel reportControlPanel;
    private LinkedList<String> orderedHeadersFromModel;

    //JLists
    private DefaultListModel<String> modelHeaderListModel = new DefaultListModel<>();
    private JList<String> modelHeaderList = new JList<>(modelHeaderListModel);
    private JScrollPane modelHeaderPane = new JScrollPane(modelHeaderList);
    private DefaultListModel<String> viewedFieldsListModel = new DefaultListModel<>();
    private JList<String> viewedFieldsList = new JList<>(viewedFieldsListModel);
    private JScrollPane viewedFieldsPane = new JScrollPane(viewedFieldsList);

    //buttons
    private JButton addButton = new JButton("Add Fields->>");
    private JButton removeButton = new JButton("<<-Remove Fields");
    private JButton moveUpButton = new JButton("Move Fields Up \u2191");
    private JButton moveDownButton = new JButton("Move Fields Down \u2193");
    private JButton resetButton = new JButton("Reset");

    private ReportConfigFieldChooserPanel(ReportConfig reportConfig) throws NoSuchFieldException,
            IllegalAccessException {
        initComponents(reportConfig);
    }

    public ReportConfigFieldChooserPanel(ReportControlPanel reportControlPanel) throws NoSuchFieldException,
            IllegalAccessException {
        this.reportControlPanel = reportControlPanel;
        initComponents(reportControlPanel.getReportConfig());

    }

    public static void main(String[] args) throws IllegalAccessException, BackingStoreException, NoSuchFieldException {
        Class<? extends TotalReport> reportClass = TotalSnapshotReport.class;
        ReportConfig reportConfig = ReportConfig.getStandardReportConfig(reportClass);
        ReportConfigFieldChooserPanel testPanel = new ReportConfigFieldChooserPanel(reportConfig);
        @SuppressWarnings("unused")
        ReportControlPanel.TestFrame frame = new ReportControlPanel.TestFrame(testPanel);
    }

    private void initComponents(ReportConfig reportConfig) throws NoSuchFieldException, IllegalAccessException {
        //subPanels
        JPanel availableFieldsPanel = new JPanel();
        JPanel fieldControlPanel = new JPanel();
        JPanel viewedFieldsPanel = new JPanel();

        String[] titles = {"Available Fields", "Add/Remove/Up/Down", "Viewed Fields"};
        JPanel[] panels = {availableFieldsPanel, fieldControlPanel, viewedFieldsPanel};
        for (int i = 0; i < panels.length; i++) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            panels[i].setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        }
        //available fields
        if (reportConfig != null) {
            orderedHeadersFromModel = ReportConfig.getModelHeader(reportConfig.getReportClass());
            populateFieldChooser(reportConfig);
        }

        //model header
        modelHeaderPane.setPreferredSize(new Dimension(260, 500));
        availableFieldsPanel.add(modelHeaderPane);
        viewedFieldsPane.setPreferredSize(new Dimension(260, 500));
        viewedFieldsPanel.add(viewedFieldsPane);

        //button panel
//        fieldControlPanel.add(Box.createHorizontalStrut(3));
        fieldControlPanel.setLayout(new GridLayout(5, 1));
        fieldControlPanel.add(addButton);
        fieldControlPanel.add(removeButton);
        fieldControlPanel.add(moveUpButton);
        fieldControlPanel.add(moveDownButton);
        fieldControlPanel.add(resetButton);


        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 0;
        this.add(availableFieldsPanel, c);
        c.gridx++;
        c.anchor = GridBagConstraints.CENTER;
        this.add(fieldControlPanel, c);
        c.gridx++;
        c.anchor = GridBagConstraints.NORTH;
        this.add(viewedFieldsPanel, c);

        //selection model
        viewedFieldsList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        //listeners
        removeButton.addActionListener(new RemoveFieldsListener());
        addButton.addActionListener(new AddFieldsListener());
        moveUpButton.addActionListener(new MoveUpListener());
        moveDownButton.addActionListener(new MoveDownListener());
        resetButton.addActionListener(new ResetListener());
    }

    private void populateModelHeaderList(ReportConfig reportConfig) {
        for (int i = 0; i < orderedHeadersFromModel.size(); i++) {
            String header = orderedHeadersFromModel.get(i);
            if (!reportConfig.getViewHeader().contains(i)) {
                modelHeaderListModel.addElement(header);
            }
        }
    }

    private void populateViewedFieldsList(ReportConfig reportConfig) {
        for (int i = 0; i < orderedHeadersFromModel.size(); i++) {
            String header = orderedHeadersFromModel.get(i);
            if (reportConfig.getViewHeader().contains(i)) {
                viewedFieldsListModel.addElement(header);
            }
        }
    }

    public void populateFieldChooser(ReportConfig reportConfig) throws NoSuchFieldException,
            IllegalAccessException {
        modelHeaderListModel.removeAllElements();
        viewedFieldsListModel.removeAllElements();

        populateModelHeaderList(reportConfig);
        populateViewedFieldsList(reportConfig);

        Dimension dimension = reportControlPanel.getRelatedDimension(modelHeaderPane);
        modelHeaderPane.setPreferredSize(dimension);
        viewedFieldsPane.setPreferredSize(dimension);
    }

    public void updateReportConfig() {
        LinkedList<Integer> thisViewHeader = new LinkedList<>();
        try {
            for (int i = 0; i < viewedFieldsListModel.size(); i++) {
                int fieldIndex = orderedHeadersFromModel.indexOf(viewedFieldsListModel.get(i));
                thisViewHeader.add(fieldIndex);
            }
            reportControlPanel.getReportConfig().setViewHeader(thisViewHeader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void moveToViewedBottom(int fromPosition) {
        String header = modelHeaderListModel.remove(fromPosition);
        viewedFieldsListModel.addElement(header);   // At end
    }

    private void moveToViewOrdered(int fromPosition) {
        String header = modelHeaderListModel.remove(fromPosition);
        int toPosition = orderedHeadersFromModel.indexOf(header);
        viewedFieldsListModel.add(toPosition, header);
    }

    private void moveFromViewed(int fromPosition) {
        String header = viewedFieldsListModel.remove(fromPosition);
        insertInAlphabeticalOrder(modelHeaderListModel, header);
    }

    private void insertInAlphabeticalOrder(DefaultListModel<String> model, String header) {
        for (int i = 0; i < model.size(); i++) {
            if (header.compareTo(model.get(i)) < 0) {
                model.insertElementAt(header, i);
                return;
            }
        }
        model.addElement(header); // At end
    }

    private class RemoveFieldsListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int[] indices = viewedFieldsList.getSelectedIndices();
            for (int i = indices.length - 1; 0 <= i; i--) {
                moveFromViewed(indices[i]);
            }

            int sizeRemaining = viewedFieldsListModel.size();
            if (sizeRemaining == 0) { //Nobody's left, disable firing.
                removeButton.setEnabled(false);
            }

            if (reportControlPanel != null) updateReportConfig();
        }
    }

    private class AddFieldsListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int[] indices = modelHeaderList.getSelectedIndices();
            for (int i = indices.length - 1; 0 <= i; i--) {
                moveToViewedBottom(indices[i]);
            }

            if (reportControlPanel != null) updateReportConfig();
        }
    }

    private class MoveUpListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            int[] indices = viewedFieldsList.getSelectedIndices();
            if (indices.length > 0 && indices[0] != 0) {
                String tmp = viewedFieldsListModel.get(indices[0] - 1);
                viewedFieldsListModel.remove(indices[0] - 1);
                viewedFieldsListModel.insertElementAt(tmp, indices[indices.length - 1]);
                int[] newIndices = new int[indices.length];
                for (int i = 0; i < indices.length; i++) {
                    newIndices[i] = indices[i] - 1;
                }
                viewedFieldsList.setSelectedIndices(newIndices);
            }

            if (reportControlPanel != null) updateReportConfig();
        }
    }

    private class MoveDownListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            int[] indices = viewedFieldsList.getSelectedIndices();
            if (indices.length > 0 && indices[indices.length - 1] != viewedFieldsListModel.size() - 1) {
                String tmp = viewedFieldsListModel.get(indices[indices.length - 1] + 1);
                viewedFieldsListModel.remove(indices[indices.length - 1] + 1);
                viewedFieldsListModel.insertElementAt(tmp, indices[0]);
                int[] newIndices = new int[indices.length];
                for (int i = 0; i < indices.length; i++) {
                    newIndices[i] = indices[i] + 1;
                }
                viewedFieldsList.setSelectedIndices(newIndices);
            }

            if (reportControlPanel != null) updateReportConfig();
        }
    }

    private class ResetListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            for (int i = modelHeaderListModel.size() - 1; 0 <= i; i--) {
                moveToViewOrdered(i);
            }

            if (reportControlPanel != null) updateReportConfig();
        }
    }

}









