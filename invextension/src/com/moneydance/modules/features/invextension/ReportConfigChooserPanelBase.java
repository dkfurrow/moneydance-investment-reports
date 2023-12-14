/*
 * ReportConfigAccountChooserPanel.java
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
import java.io.Serial;

/**
 * Field chooser panel to identify which expenses are investment expenses.
 */
public abstract class ReportConfigChooserPanelBase<ItemType> extends JPanel {
    @Serial
    private static final long serialVersionUID = -8990699863699414946L;
    protected final ReportControlPanel reportControlPanel;    
    private String removeButtonLabel;    
    private String addButtonLabel;
    private String resetButtonLabel;
    private String leftLabel;
    private String middleLabel;
    private String rightLabel;
    // JLists
    protected final DefaultListModel<ItemType> leftListModel = new DefaultListModel<>();
    private final JList<ItemType> leftList = new JList<>(leftListModel);
    private final JScrollPane leftPane = new JScrollPane(leftList);

    protected final DefaultListModel<ItemType> rightListModel = new DefaultListModel<>();
    private final JList<ItemType> rightList = new JList<>(rightListModel);
    private final JScrollPane rightPane = new JScrollPane(rightList);

    public static final Color HIGHLIGHT_COLOR = new Color(0, 0, 128);

    public ReportConfigChooserPanelBase(ReportControlPanel reportControlPanel,
                                        String removeButtonLabel, String addButtonLabel, String resetButtonLabel,
                                        String leftLabel, String middleLabel, String rightLabel) {
        this.reportControlPanel = reportControlPanel;
        this.removeButtonLabel = removeButtonLabel;
        this.addButtonLabel = addButtonLabel;
        this.resetButtonLabel = resetButtonLabel;
        this.leftLabel = leftLabel;
        this.middleLabel = middleLabel;
        this.rightLabel = rightLabel;
    }

    public void setupGui() {
        // Buttons
        JButton removeButton = new JButton(removeButtonLabel);
        JButton addButton = new JButton(addButtonLabel);
        JButton resetButton = new JButton(resetButtonLabel);

        // subPanels
        JPanel leftPanel = new JPanel();
        JPanel expenseControlPanel = new JPanel();
        JPanel rightPanel = new JPanel();

        String[] titles = {leftLabel, middleLabel, rightLabel};
        JPanel[] panels = {leftPanel, expenseControlPanel, rightPanel};
        for (int i = 0; i < panels.length; i++) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            panels[i].setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        }

        leftPanel.add(leftPane);
        rightPanel.add(rightPane);

        //button panel
        expenseControlPanel.setLayout(new GridLayout(3, 1));
        expenseControlPanel.add(removeButton);
        expenseControlPanel.add(addButton);
        expenseControlPanel.add(resetButton);

        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        c.anchor = GridBagConstraints.NORTH;
        c.gridx = 0;
        c.gridy = 0;
        this.add(leftPanel, c);
        c.gridx++;
        c.anchor = GridBagConstraints.CENTER;
        this.add(expenseControlPanel, c);
        c.gridx++;
        c.anchor = GridBagConstraints.NORTH;
        this.add(rightPanel, c);

        // selection model
        rightList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);

        // listeners
        removeButton.addActionListener(new removeItemListener());
        addButton.addActionListener(new addItemListener());
        resetButton.addActionListener(new resetItemListener());

        // renders
        leftList.setCellRenderer(new ItemCellRender());
        rightList.setCellRenderer(new ItemCellRender());
    }

    abstract void populateLeftList(ReportConfig reportConfig);

    abstract void populateRightList(ReportConfig reportConfig);

    protected void populateBothLists(ReportConfig reportConfig) {
        leftListModel.removeAllElements();
        rightListModel.removeAllElements();

        populateLeftList(reportConfig);
        populateRightList(reportConfig);

        JScrollPane sizingPane = leftListModel.getSize() >= rightListModel.getSize() ? leftPane : rightPane;
        Dimension dimension = reportControlPanel.getRelatedDimension(sizingPane);
        leftPane.setPreferredSize(dimension);
        rightPane.setPreferredSize(dimension);
    }

    protected void moveFromLeftToRight(int pos) {
        ItemType item = leftListModel.remove(pos);
        insertInAlphabeticalOrder(rightListModel, item);
    }

    protected void moveFromRightToLeft(int pos) {
        ItemType item = rightListModel.remove(pos);
        insertInAlphabeticalOrder(leftListModel, item);
    }

    abstract String fullName(ItemType item);

    private void insertInAlphabeticalOrder(DefaultListModel<ItemType> model, ItemType item) {
        for (int i = 0; i < model.size(); i++) {
            if (fullName(item).compareTo(fullName(model.get(i))) < 0) {
                model.insertElementAt(item, i);
                return;
            }
        }
        model.addElement(item); // At end
    }

    public static void setPanelBorders(String[] titles, JPanel[] panels) {
        for (int i = 0; i < panels.length; i++) {
            TitledBorder titledBorder = BorderFactory.createTitledBorder(titles[i]);
            Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
            titledBorder.setTitleColor(new Color(100, 100, 100));
            panels[i].setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));
        }
    }

    abstract void updateReportConfig();

    private class removeItemListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int[] indices = rightList.getSelectedIndices();

            for (int i = indices.length - 1; 0 <= i; i--) {
                moveFromRightToLeft(indices[i]);
            }

            if (reportControlPanel != null) {
                updateReportConfig();
            }
        }
    }

    private class addItemListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            int[] indices = leftList.getSelectedIndices();

            for (int i = indices.length - 1; 0 <= i; i--) {
                moveFromLeftToRight(indices[i]);
            }

            if (reportControlPanel != null) {
                updateReportConfig();
            }
        }
    }

    private class resetItemListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            for (int i = rightListModel.size() - 1; 0 <= i; i--) {
                moveFromRightToLeft(i);
            }
            if (reportControlPanel != null) {
                updateReportConfig();
            }
        }
    }


    private class ItemCellRender extends JLabel implements ListCellRenderer<ItemType> {
        @Serial
        private static final long serialVersionUID = 7586072864239449518L;


        public ItemCellRender() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList<? extends ItemType> list, ItemType item,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            setText(fullName(item));
            setSelectionBehavior(this, isSelected);
            return this;
        }
    }

    public static void setSelectionBehavior(JLabel label, boolean isSelected){
        if (isSelected) {
            label.setBackground(HIGHLIGHT_COLOR);
            label.setForeground(Color.white);
        } else {
            label.setBackground(Color.white);
            label.setForeground(Color.black);
        }

    }
}









