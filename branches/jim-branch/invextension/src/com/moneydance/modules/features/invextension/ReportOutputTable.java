/* ReportOutputTable.java
 * Copyright 2011 Dale K. Furrow. All rights reserved.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Toolkit;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;

import java.awt.event.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

import java.text.DecimalFormat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.UIResource;
import javax.swing.table.*;

import com.moneydance.modules.features.invextension.FormattedTable.RowBackground;
import com.moneydance.modules.features.invextension.FormattedTable.ColFormat;
import com.moneydance.modules.features.invextension.FormattedTable.ColSizeOption;


/**
 * An extended and formatted of CoolTable by Kurt Riede added multi sort,
 * filter, rendering of columns, rows, and cells
 *
 * @author Dale Furrow
 */
public class ReportOutputTable extends JScrollPane {
    private static final long serialVersionUID = 1654873977162641532L;

    private final FormattedTable lockedTable;
    private final FormattedTable scrollTable;
    int frozenColumns = 0;
    private final JScrollPaneAdjuster adjuster;
    public int firstSort = 0;
    public int secondSort = 0;
    public int thirdSort = 0;
    public SortOrder firstOrder = SortOrder.ASCENDING;
    public SortOrder secondOrder = SortOrder.ASCENDING;
    public SortOrder thirdOrder = SortOrder.ASCENDING;
    public TableModel model;
    public boolean closedPosHidden = true;
    public int closedPosColumn;

    public ReportOutputTable(TableModel model,
                             ColFormat[] colFormats,
                             int numFrozenColumns,
                             int indClosedPosColumn,
                             int firstSort,
                             int secondSort,
                             ColSizeOption sizeOption,
                             RowBackground[] rowBacks) {
        super();
        adjuster = new JScrollPaneAdjuster(this);
        this.model = model;
        frozenColumns = numFrozenColumns;
        closedPosColumn = indClosedPosColumn;
        this.firstSort = firstSort;
        this.secondSort = secondSort;
        // create the two tables
        lockedTable = new FormattedTable(model, colFormats, sizeOption, rowBacks);
        scrollTable = new FormattedTable(model, colFormats, sizeOption, rowBacks);
        lockedTable.setName("lockedTable");
        scrollTable.setName("scrollTable");
        lockedTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        scrollTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        setViewportView(scrollTable);

        // Put the locked-column tablePane in the row header
        JViewport thisViewport = new JViewport();
        thisViewport.setBackground(Color.white);
        thisViewport.setView(lockedTable);
        setRowHeader(thisViewport);

        // Put the header of the locked-column tablePane in the top left corner
        // of the scoll pane
        JTableHeader lockedHeader = lockedTable.getTableHeader();
        lockedHeader.setReorderingAllowed(false);
        lockedHeader.setResizingAllowed(false);
        setCorner(JScrollPane.UPPER_LEFT_CORNER, lockedHeader);

        scrollTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lockedTable.setSelectionModel(scrollTable.getSelectionModel());
        lockedTable.getTableHeader().setReorderingAllowed(false);
        lockedTable.getTableHeader().setResizingAllowed(false);
        lockedTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        scrollTable.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // Remove the fixed columns from the main tablePane
        TableColumnModel scrollColumnModel = scrollTable.getColumnModel();
        for (int i = 0; i < frozenColumns; i++) {
            scrollColumnModel.removeColumn(scrollColumnModel.getColumn(0));
        }
        // Remove the non-fixed columns from the fixed tablePane
        TableColumnModel lockedColumnModel = lockedTable.getColumnModel();
        while (lockedTable.getColumnCount() > frozenColumns) {
            lockedColumnModel.removeColumn(lockedColumnModel.getColumn(frozenColumns));
        }
        // Add the fixed tablePane to the scroll pane
        lockedTable.setPreferredScrollableViewportSize(lockedTable.getPreferredSize());

        // set a new action for the tab key
        // todo search actions by action name (not by KeyStroke)
        final Action lockedTableNextColumnCellAction = getAction(lockedTable, KeyEvent.VK_TAB, 0);
        final Action scrollTableNextColumnCellAction = getAction(scrollTable, KeyEvent.VK_TAB, 0);
        final Action lockedTablePrevColumnCellAction = getAction(lockedTable,
                                                                 KeyEvent.VK_TAB,
                                                                 InputEvent.SHIFT_DOWN_MASK);
        final Action scrollTablePrevColumnCellAction = getAction(scrollTable,
                                                                 KeyEvent.VK_TAB,
                                                                 InputEvent.SHIFT_DOWN_MASK);

        setAction(lockedTable, "selectNextColumn",
                  new LockedTableSelectNextColumnCellAction(lockedTableNextColumnCellAction));
        setAction(scrollTable, "selectNextColumn",
                  new ScrollTableSelectNextColumnCellAction(scrollTableNextColumnCellAction));
        setAction(lockedTable, "selectPreviousColumn",
                  new LockedTableSelectPreviousColumnCellAction(lockedTablePrevColumnCellAction));
        setAction(scrollTable, "selectPreviousColumn",
                  new ScrollTableSelectPreviousColumnCellAction(scrollTablePrevColumnCellAction));

        setAction(lockedTable, "selectNextColumnCell",
                  new LockedTableSelectNextColumnCellAction(lockedTableNextColumnCellAction));
        setAction(scrollTable, "selectNextColumnCell",
                  new ScrollTableSelectNextColumnCellAction(scrollTableNextColumnCellAction));
        setAction(lockedTable, "selectPreviousColumnCell",
                  new LockedTableSelectPreviousColumnCellAction(lockedTablePrevColumnCellAction));
        setAction(scrollTable, "selectPreviousColumnCell",
                  new ScrollTableSelectPreviousColumnCellAction(scrollTablePrevColumnCellAction));

        setAction(scrollTable, "selectFirstColumn", new ScrollableSelectFirstColumnCellAction());
        setAction(lockedTable, "selectLastColumn", new LockedTableSelectLastColumnCellAction());
    }

    public JTable getScrollTable() {
        return scrollTable;
    }

    public JTable getLockedTable() {
        return lockedTable;
    }

    public String[] getAllColumnNames() {

        String[] columnNames = new String[model.getColumnCount()];
        for (int i = 0; i < model.getColumnCount(); i++) {
            columnNames[i] = model.getColumnName(i);
        }
        return columnNames;
    }

    private void setAction(JComponent component, String name, Action action) {
        component.getActionMap().put(name, action);
    }

    private void setAction(JComponent component, String name, int keyCode,
                           int modifiers, Action action) {
        final int condition = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        component.getInputMap(condition).put(keyStroke, name);
        component.getActionMap().put(name, action);
    }

    private Action getAction(JComponent component, int keyCode, int modifiers) {
        final int condition = JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT;
        final KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        Object object = component.getInputMap(condition).get(keyStroke);
        if (object == null) {
            if (component.getParent() instanceof JComponent) {
                return getAction((JComponent) component.getParent(), keyCode,
                                 modifiers);
            } else {
                return null;
            }
        } else {
            return scrollTable.getActionMap().get(object);
        }
    }

    protected int nextRow(JTable table) {
        int row = table.getSelectedRow() + 1;
        if (row == table.getRowCount()) {
            row = 0;
        }
        return row;
    }

    private int previousRow(JTable table) {
        int row = table.getSelectedRow() - 1;
        if (row == -1) {
            row = table.getRowCount() - 1;
        }
        return row;
    }

    public final int getFrozenColumns() {
        return frozenColumns;
    }

    public final void setFrozenColumns(final int numFrozenColumns) {

        rearrangeColumns(numFrozenColumns);
        frozenColumns = numFrozenColumns;
    }

    private void rearrangeColumns(final int numFrozenColumns) {
        TableColumnModel scrollColumnModel = scrollTable.getColumnModel();
        TableColumnModel lockedColumnModel = lockedTable.getColumnModel();
        if (frozenColumns < numFrozenColumns) {
            // move columns from scrollable to fixed tablePane
            for (int i = frozenColumns; i < numFrozenColumns; i++) {
                TableColumn column = scrollColumnModel.getColumn(0);
                lockedColumnModel.addColumn(column);
                scrollColumnModel.removeColumn(column);
            }
            lockedTable.setPreferredScrollableViewportSize(lockedTable.getPreferredSize());
        } else if (frozenColumns > numFrozenColumns) {
            // move columns from fixed to scrollable tablePane
            for (int i = numFrozenColumns; i < frozenColumns; i++) {
                TableColumn column = lockedColumnModel.getColumn(lockedColumnModel.getColumnCount() - 1);
                scrollColumnModel.addColumn(column);
                scrollColumnModel.moveColumn(scrollColumnModel.getColumnCount() - 1, 0);
                lockedColumnModel.removeColumn(column);
            }
            lockedTable.setPreferredScrollableViewportSize(lockedTable.getPreferredSize());
        }
    }

    private final class LockedTableSelectLastColumnCellAction extends
                                                              AbstractAction {
        private static final long serialVersionUID = -7498538141653234651L;

        private LockedTableSelectLastColumnCellAction() {
            super();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == lockedTable) {
                lockedTable.transferFocus();
            }
            scrollTable.changeSelection(scrollTable.getSelectedRow(),
                                        scrollTable.getColumnCount() - 1,
                                        false,
                                        false);
        }
    }

    private final class ScrollableSelectFirstColumnCellAction extends
                                                              AbstractAction {
        private static final long serialVersionUID = 7004700943224579977L;

        private ScrollableSelectFirstColumnCellAction() {
            super();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == scrollTable) {
                scrollTable.transferFocusBackward();
            }
            lockedTable.changeSelection(lockedTable.getSelectedRow(),
                                        0,
                                        false,
                                        false);
        }
    }

    private final class LockedTableSelectNextColumnCellAction extends
                                                              AbstractAction {
        private static final long serialVersionUID = 2820241653505999596L;

        private final Action lockedTableNextColumnCellAction;

        private LockedTableSelectNextColumnCellAction(Action lockedTableNextColumnCellAction) {
            super();
            this.lockedTableNextColumnCellAction = lockedTableNextColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (lockedTable.getSelectedColumn() == lockedTable.getColumnCount() - 1) {
                lockedTable.transferFocus();
                scrollTable.changeSelection(lockedTable.getSelectedRow(),
                                            0,
                                            false,
                                            false);
            } else {
                lockedTableNextColumnCellAction.actionPerformed(e);
            }
        }
    }

    private final class ScrollTableSelectNextColumnCellAction extends
                                                              AbstractAction {
        private static final long serialVersionUID = 135412121274189994L;

        private final Action scrollTableNextColumnCellAction;

        private ScrollTableSelectNextColumnCellAction(Action scrollTableNextColumnCellAction) {
            super();
            this.scrollTableNextColumnCellAction = scrollTableNextColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (scrollTable.getSelectedColumn() == scrollTable.getColumnCount() - 1) {
                scrollTable.transferFocusBackward();
                lockedTable.changeSelection(nextRow(scrollTable),
                                            0,
                                            false,
                                            false);
                return;
            } else {
                scrollTableNextColumnCellAction.actionPerformed(e);
            }
        }
    }

    private final class ScrollTableSelectPreviousColumnCellAction extends
                                                                  AbstractAction {
        private static final long serialVersionUID = -6293074638490971318L;
        private final Action scrollTablePrevColumnCellAction;

        private ScrollTableSelectPreviousColumnCellAction(Action scrollTablePrevColumnCellAction) {
            super();
            this.scrollTablePrevColumnCellAction = scrollTablePrevColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (scrollTable.getSelectedColumn() == 0) {
                scrollTable.transferFocusBackward();
                lockedTable.changeSelection(scrollTable.getSelectedRow(),
                                            lockedTable.getColumnCount() - 1,
                                            false,
                                            false);
                return;
            } else {
                scrollTablePrevColumnCellAction.actionPerformed(e);
            }
        }
    }

    private final class LockedTableSelectPreviousColumnCellAction extends
                                                                  AbstractAction {
        private static final long serialVersionUID = -290336911634305126L;

        private final Action lockedTablePrevColumnCellAction;

        private LockedTableSelectPreviousColumnCellAction(Action lockedTablePrevColumnCellAction) {
            super();
            this.lockedTablePrevColumnCellAction = lockedTablePrevColumnCellAction;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (lockedTable.getSelectedColumn() == 0) {
                lockedTable.transferFocus();
                scrollTable.changeSelection(previousRow(scrollTable),
                                            scrollTable.getColumnCount() - 1,
                                            false,
                                            false);
                return;
            } else {
                lockedTablePrevColumnCellAction.actionPerformed(e);
            }
        }
    }

    public class JScrollPaneAdjuster implements PropertyChangeListener,
                                     Serializable {
        private static final long serialVersionUID = -6372520752839570952L;

        private JScrollPane pane;
        private transient Adjuster x, y;

        public JScrollPaneAdjuster(JScrollPane pane) {
            this.pane = pane;
            this.x = new Adjuster(pane.getViewport(), pane.getColumnHeader(), Adjuster.X);
            this.y = new Adjuster(pane.getViewport(), pane.getRowHeader(), Adjuster.Y);
            pane.addPropertyChangeListener(this);
        }

        public void dispose() {
            x.dispose();
            y.dispose();
            pane.removePropertyChangeListener(this);
            pane = null;
        }

        @Override
        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();
            if (name.equals("viewport")) {
                x.setViewport((JViewport) e.getNewValue());
                y.setViewport((JViewport) e.getNewValue());
            } else if (name.equals("rowHeader")) {
                y.setHeader((JViewport) e.getNewValue());
            } else if (name.equals("columnHeader")) {
                x.setHeader((JViewport) e.getNewValue());
            }
        }

        private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {
            in.defaultReadObject();
            x = new Adjuster(pane.getViewport(), pane.getColumnHeader(), Adjuster.X);
            y = new Adjuster(pane.getViewport(), pane.getRowHeader(), Adjuster.Y);
        }

        private class Adjuster implements ChangeListener, Runnable {

            public static final int X = 1, Y = 2;
            private JViewport viewport, header;
            private int type;

            public Adjuster(JViewport viewport, JViewport header, int type) {
                this.viewport = viewport;
                this.header = header;
                this.type = type;
                if (header != null) {
                    header.addChangeListener(this);
                }
            }

            public void setViewport(JViewport newViewport) {
                viewport = newViewport;
            }

            public void setHeader(JViewport newHeader) {
                if (header != null) {
                    header.removeChangeListener(this);
                }
                header = newHeader;
                if (header != null) {
                    header.addChangeListener(this);
                }
            }

            @Override
            public void stateChanged(ChangeEvent e) {
                if (viewport == null || header == null) {
                    return;
                }
                if (type == X) {
                    if (viewport.getViewPosition().x != header.getViewPosition().x) {
                        SwingUtilities.invokeLater(this);
                    }
                } else {
                    if (viewport.getViewPosition().y != header.getViewPosition().y) {
                        SwingUtilities.invokeLater(this);
                    }
                }
            }

            @Override
            public void run() {
                if (viewport == null || header == null) {
                    return;
                }
                Point v = viewport.getViewPosition(), h = header.getViewPosition();
                if (type == X) {
                    if (v.x != h.x) {
                        viewport.setViewPosition(new Point(h.x, v.y));
                    }
                } else {
                    if (v.y != h.y) {
                        viewport.setViewPosition(new Point(v.x, h.y));
                    }
                }
            }

            public void dispose() {
                if (header != null) {
                    header.removeChangeListener(this);
                }
                viewport = header = null;
            }
        }
    }

    public void sortRows() {

        TableRowSorter<TableModel> rowSorter = new TableRowSorter<TableModel>(this.model);
        // apply row sorter
        RowFilter<TableModel, Object> rf = null;
        if (closedPosHidden) {
            rf = RowFilter.numberFilter(RowFilter.ComparisonType.NOT_EQUAL, 0.0, closedPosColumn);
            rowSorter.setRowFilter(rf);
        } else {
            rowSorter.setRowFilter(rf);
        }

        // apply custom comparator for 1st 5 rows (Strings)
        // IMPORTANT! Must implement comparator before Sortkeys!
        rowSorter.setComparator(0, stringComp);
        rowSorter.setComparator(1, stringComp);
        rowSorter.setComparator(2, stringComp);
        rowSorter.setComparator(3, stringComp);
        rowSorter.setComparator(4, stringComp);

        // Apply sortKeys
        List<RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
        sortKeys.add(new RowSorter.SortKey(firstSort, firstOrder));
        sortKeys.add(new RowSorter.SortKey(secondSort, secondOrder));
        sortKeys.add(new RowSorter.SortKey(thirdSort, thirdOrder));
        rowSorter.setSortKeys(sortKeys);

        this.scrollTable.setRowSorter(rowSorter);
        this.lockedTable.setRowSorter(rowSorter);
        setSortedTableHeader();
    }

    public void sortRows(Point loc) {
        // Create and set up the window.
        JFrame frame = new JFrame("Row Sort");

        // Create and set up the content pane.
        JComponent newContentPane = new RowSortGui(this);
        newContentPane.setOpaque(true); // content panes must be opaque
        frame.setContentPane(newContentPane);
        // Display the window.
        frame.pack();
        loc.x = loc.x + 75; // moved spawned window to right
        loc.y = loc.y + 75; // moved spawned window down
        frame.setLocation(loc);
        frame.setVisible(true);
    }

    Comparator<String> stringComp = new Comparator<String>() {

        @Override
        public int compare(String o1, String o2) {
            LinkedList<String> endStrings = new LinkedList<String>();
            endStrings.add("CASH");
            endStrings.add("CASH "); //handles aggregate by ticker
            endStrings.add("-ALL");
            endStrings.add("-ALL "); //handles last row of all-aggregate

            int o1Rank = -1;
            int o2Rank = -1;

            for (String string : endStrings) {
                if (o1.endsWith(string))
                    o1Rank = endStrings.indexOf(string);
                if (o2.endsWith(string))
                    o2Rank = endStrings.indexOf(string);
            }

            if (o1Rank == o2Rank) {
                return o1.compareTo(o2);
            } else {
                return o1Rank - o2Rank;
            }
        }
    };

    private class RowSortGui extends JPanel {
        private static final long serialVersionUID = -8349629256510555172L;

        public ReportOutputTable tablePane;

        public RowSortGui(ReportOutputTable thisTable) {
            tablePane = thisTable;
            String[] colNames = tablePane.getAllColumnNames();
            JPanel boxPanel = new JPanel();
            JComboBox<String> firstSortBox = new JComboBox<String>(colNames);
            JComboBox<String> secondSortBox = new JComboBox<String>(colNames);
            JComboBox<String> thirdSortBox = new JComboBox<String>(colNames);
            JComboBox<SortOrder> firstOrderBox = new JComboBox<SortOrder>(SortOrder.values());
            JComboBox<SortOrder> secondOrderBox = new JComboBox<SortOrder>(SortOrder.values());
            JComboBox<SortOrder> thirdOrderBox = new JComboBox<SortOrder>(SortOrder.values());
            // set defaults to previous values
            firstSortBox.setSelectedIndex(tablePane.firstSort);
            secondSortBox.setSelectedIndex(tablePane.secondSort);
            thirdSortBox.setSelectedIndex(tablePane.thirdSort);
            firstOrderBox.setSelectedItem(tablePane.firstOrder);
            secondOrderBox.setSelectedItem(tablePane.secondOrder);
            thirdOrderBox.setSelectedItem(tablePane.thirdOrder);

            JButton sortButton = new JButton("Sort Table");
            sortButton.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        tablePane.sortRows();
                    }
                });
            // set sorts
            firstSortBox.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        @SuppressWarnings("unchecked")
                            JComboBox<String> cb = (JComboBox<String>) e.getSource();
                        firstSort = cb.getSelectedIndex();
                    }
                });
            secondSortBox.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        @SuppressWarnings("unchecked")
                            JComboBox<String> cb = (JComboBox<String>) e.getSource();
                        secondSort = cb.getSelectedIndex();
                    }
                });
            thirdSortBox.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        @SuppressWarnings("unchecked")
                            JComboBox<String> cb = (JComboBox<String>) e.getSource();
                        thirdSort = cb.getSelectedIndex();
                    }
                });
            // set orders within sorts
            firstOrderBox.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        @SuppressWarnings("unchecked")
                            JComboBox<SortOrder> cb = (JComboBox<SortOrder>) e.getSource();
                        firstOrder = (SortOrder) cb.getSelectedItem();
                    }
                });
            secondOrderBox.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        @SuppressWarnings("unchecked")
                            JComboBox<SortOrder> cb = (JComboBox<SortOrder>) e.getSource();
                        secondOrder = (SortOrder) cb.getSelectedItem();
                    }
                });
            thirdOrderBox.addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        @SuppressWarnings("unchecked")
                            JComboBox<SortOrder> cb = (JComboBox<SortOrder>) e.getSource();
                        thirdOrder = (SortOrder) cb.getSelectedItem();
                    }
                });

            // build frame
            // set layouts
            boxPanel.setLayout(new GridLayout(3, 2));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            boxPanel.add(firstSortBox);
            boxPanel.add(firstOrderBox);
            boxPanel.add(secondSortBox);
            boxPanel.add(secondOrderBox);
            boxPanel.add(thirdSortBox);
            boxPanel.add(thirdOrderBox);
            add(boxPanel);
            sortButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(sortButton);

        }

        public Point getParentLoc() {
            return tablePane.getLocation();
        }
    }

    public void setSortedTableHeader() {

        for (int i = 0; i < model.getColumnCount(); i++) {
            int viewCol = 0;
            TableColumn column = new TableColumn();
            if (i < this.frozenColumns) {
                viewCol = this.lockedTable.convertColumnIndexToView(i);
                column = this.lockedTable.getColumnModel().getColumn(viewCol);

                if (i == firstSort
                    && (firstOrder == SortOrder.ASCENDING || firstOrder == SortOrder.DESCENDING)) {
                    column.setHeaderRenderer(new ArrowHeader(this.lockedTable,
                                                             1, firstOrder == SortOrder.DESCENDING ? true
                                                             : false));
                } else if (i == secondSort
                           && (secondOrder == SortOrder.ASCENDING || secondOrder == SortOrder.DESCENDING)) {
                    column.setHeaderRenderer(new ArrowHeader(this.lockedTable,
                                                             2, secondOrder == SortOrder.DESCENDING ? true
                                                             : false));
                } else if (i == thirdSort
                           && (thirdOrder == SortOrder.ASCENDING || thirdOrder == SortOrder.DESCENDING)) {
                    column.setHeaderRenderer(new ArrowHeader(this.lockedTable,
                                                             3, thirdOrder == SortOrder.DESCENDING ? true
                                                             : false));
                } else {
                    column.setHeaderRenderer(new RegularHeader());
                }
            } else {
                viewCol = this.scrollTable.convertColumnIndexToView(i);
                column = this.scrollTable.getColumnModel().getColumn(viewCol);

                if (i == firstSort
                    && (firstOrder == SortOrder.ASCENDING || firstOrder == SortOrder.DESCENDING)) {
                    column.setHeaderRenderer(new ArrowHeader(this.scrollTable,
                                                             1, firstOrder == SortOrder.DESCENDING ? true
                                                             : false));
                } else if (i == secondSort
                           && (secondOrder == SortOrder.ASCENDING || secondOrder == SortOrder.DESCENDING)) {
                    column.setHeaderRenderer(new ArrowHeader(this.scrollTable,
                                                             2, secondOrder == SortOrder.DESCENDING ? true
                                                             : false));
                } else if (i == thirdSort
                           && (thirdOrder == SortOrder.ASCENDING || thirdOrder == SortOrder.DESCENDING)) {
                    column.setHeaderRenderer(new ArrowHeader(this.scrollTable,
                                                             3, thirdOrder == SortOrder.DESCENDING ? true
                                                             : false));
                } else {
                    column.setHeaderRenderer(new RegularHeader());
                }
            }
            // TOD0: Review this section for elimination
            if (i < this.frozenColumns) {
                JLabel header = (JLabel) column.getHeaderRenderer().getTableCellRendererComponent(lockedTable,
                                                                                                  new Object(),
                                                                                                  false,
                                                                                                  false,
                                                                                                  -1,
                                                                                                  viewCol);
                header.repaint();
            } else {
                JLabel header = (JLabel) column.getHeaderRenderer().getTableCellRendererComponent(scrollTable,
                                                                                                  new Object(),
                                                                                                  false,
                                                                                                  false,
                                                                                                  -1,
                                                                                                  viewCol);
                header.repaint();
            } //
        }

        lockedTable.adjustColumnPreferredWidths(ColSizeOption.MAXCONTCOLRESIZE);
        scrollTable.adjustColumnPreferredWidths(ColSizeOption.MAXCONTCOLRESIZE);
        this.scrollTable.getTableHeader().repaint();
        this.lockedTable.getTableHeader().repaint();
    }

    class ArrowHeader extends JLabel implements TableCellRenderer {
        private static final long serialVersionUID = -1175683155743555445L;

        JTable table;
        int column;
        int sortPriority;
        boolean descending;
        TableCellRenderer renderer;

        public ArrowHeader(JTable table, int sortPriority, boolean descending) {
            this.table = table;
            this.sortPriority = sortPriority;
            this.descending = descending;
            this.renderer = table.getCellRenderer(-1, column);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            this.setHorizontalTextPosition(JLabel.LEFT);
            setText(value.toString());
            Font f = new Font(this.getFont().getName(), Font.PLAIN, this.getFont().getSize());
            this.setFont(f);
            this.setForeground(Color.red);
            this.setBackground(setSortColor(sortPriority)); // doesn't appear

            JLabel newLabel = new JLabel(value.toString());
            newLabel.setHorizontalTextPosition(JLabel.LEFT);
            newLabel.setForeground(Color.red);
            newLabel.setBackground(setSortColor(sortPriority)); // doesn't
                                                                // appear

            newLabel.setToolTipText("Sort Priority: " + sortPriority
                                    + " Order: " + (descending ? "Descending" : "Ascending"));
            newLabel.setIcon(createArrow(descending, this.getFont().getSize(), sortPriority));
            return newLabel;
        }

        private Color setSortColor(int sortColor) {
            if (sortColor == 1) {
                return new Color(100, 100, 100);
            } else if (sortColor == 2) {
                return new Color(160, 160, 160);
            } else {
                return new Color(220, 220, 220);
            }
        }

        private Icon createArrow(boolean descending, int size, int priority) {
            return new Arrow(descending, size, priority);
        }

        private class Arrow implements Icon {

            private boolean descending;
            private int priority;
            private int size;

            public Arrow(boolean descending, int size, int priority) {
                this.descending = descending;
                this.size = size;
                this.priority = priority;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {

                // Override base size with a value calculated from the
                // component's font.
                updateSize(c);

                Color color = c == null ? Color.BLACK : c.getForeground();
                g.setColor(color);

                int npoints = 3;
                int[] xpoints = new int[] { 0, size / 2, size };
                int[] ypoints = descending ? new int[] { 0, size, 0 } : new int[] { size, 0, size };

                Polygon triangle = new Polygon(xpoints, ypoints, npoints);

                // Center icon vertically within the column heading label.
                int dy = (c.getHeight() - size) / 2;

                g.translate(x, dy);
                g.drawPolygon(triangle);
                g.fillPolygon(triangle);
                g.translate(-x, -dy);

            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }

            private void updateSize(Component c) {
                if (c != null) {
                    FontMetrics fm = c.getFontMetrics(c.getFont());
                    int baseHeight = fm.getAscent();

                    // In a compound sort, make each succesive triangle 20%
                    // smaller than the previous one.
                    size = (int) (baseHeight * 3 / 4 * Math.pow(0.8, priority));
                }
            }
        }
    } // end ArrowHeader Class

    class RegularHeader extends DefaultTableCellRenderer implements UIResource {

        private static final long serialVersionUID = 191983300055624975L;
        private boolean horizontalTextPositionSet;

        public RegularHeader() {
            setHorizontalAlignment(JLabel.CENTER);
        }

        @Override
        public void setHorizontalTextPosition(int textPosition) {
            horizontalTextPositionSet = true;
            super.setHorizontalTextPosition(textPosition);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            JTableHeader header = table.getTableHeader();

            if (header != null) {
                Color fgColor = null;
                Color bgColor = null;
                if (hasFocus) {
                    fgColor = UIManager.getColor("TableHeader.focusCellForeground");
                    bgColor = UIManager.getColor("TableHeader.focusCellBackground");
                }
                if (fgColor == null) {
                    fgColor = header.getForeground();
                }
                if (bgColor == null) {
                    bgColor = header.getBackground();
                }
                setForeground(fgColor);
                setBackground(bgColor);

                setFont(header.getFont());
            }

            setText(value == null ? "" : value.toString());
            Border border = null;
            if (hasFocus) {
                border = UIManager.getBorder("TableHeader.focusCellBorder");
            }
            if (border == null) {
                border = UIManager.getBorder("TableHeader.cellBorder");
            }
            setBorder(border);

            return this;
        }
    } // end RegularHeader Class

    class LocationFrame extends JFrame implements ComponentListener {
        private static final long serialVersionUID = -624700278151394528L;
        private Point frameLoc;

        public LocationFrame() {
            super();
            frameLoc = this.getLocation();
        }

        @Override
        public void componentResized(ComponentEvent e) {
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            frameLoc = this.getLocation();
        }

        @Override
        public void componentShown(ComponentEvent e) {
        }

        @Override
        public void componentHidden(ComponentEvent e) {
        }
    }

    public void copyTableToClipboard() {
        StringBuffer copyIn = new StringBuffer();
        int numCols = model.getColumnCount();
        int numRowsView = lockedTable.getRowCount(); // allows for filtering of
                                                     // closed positions

        for (int j = 0; j < numCols; j++) {
            if (j < frozenColumns) {

                copyIn.append(lockedTable.getColumnName(j));
                if (j < numCols - 1) {
                    copyIn.append("\t");
                }
            } else {

                copyIn.append(scrollTable.getColumnName(j - frozenColumns));
                // System.out.println("j: " + j + " ViewCol: " + viewCol +
                // " Value: " + scrollTable.getColumnName(j - frozenColumns));
                if (j < numCols - 1) {
                    copyIn.append("\t");
                }
            }
        }
        copyIn.append("\n");

        for (int i = 0; i < numRowsView; i++) {
            for (int j = 0; j < numCols; j++) {
                if (j < frozenColumns) {
                    copyIn.append(lockedTable.getValueAt(i, j));
                    if (j < numCols - 1)
                        copyIn.append("\t");

                } else {
                    copyIn.append(scrollTable.getValueAt(i, j - frozenColumns));
                    if (j < numCols - 1)
                        copyIn.append("\t");
                }

            }
            copyIn.append("\n");
        }
        StringSelection stsel = new StringSelection(copyIn.toString());
        Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();

        system.setContents(stsel, stsel);
    }

    public static void CreateAndShowTable(TableModel thisModel,
                                          ColFormat[] colFormats,
                                          int indClosedPosColumn,
                                          int numFreezeCols,
                                          int firstSort,
                                          int secondSort,
                                          ColSizeOption sizeOption,
                                          String frameText,
                                          RowBackground[] rowBacks) {
        final ReportOutputTable thisTable = new ReportOutputTable(thisModel,
                                                                  colFormats,
                                                                  numFreezeCols,
                                                                  indClosedPosColumn,
                                                                  firstSort,
                                                                  secondSort,
                                                                  sizeOption,
                                                                  rowBacks);

        final JFrame outerFrame = new JFrame(frameText);

        outerFrame.getContentPane().setLayout(new GridBagLayout());
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        JPanel freezeColsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));

        Integer[] freezeCols = new Integer[] { 0, 1, 2, 3, 4, 5 };
        JLabel freezeColsLabel = new JLabel("Set Frozen Columns  ");
        JComboBox<Integer> freezeColsBox = new JComboBox<Integer>(freezeCols);
        freezeColsBox.setSelectedIndex(numFreezeCols);
        JButton buttonSort = new JButton("Sort Table");
        JCheckBox hideClosedBox = new JCheckBox("Hide Closed Positions", true);
        JButton copyCB = new JButton("Copy Table to Clipboard");

        Border blackline = BorderFactory.createLineBorder(Color.BLACK);

        hideClosedBox.setBorder(blackline);
        freezeColsPanel.setBorder(blackline);
        controlPanel.setBorder(blackline);

        freezeColsBox.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    @SuppressWarnings("unchecked")
                        JComboBox<Integer> cb = (JComboBox<Integer>) e.getSource();
                    thisTable.setFrozenColumns(cb.getSelectedIndex());
                }
            });
        buttonSort.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    thisTable.sortRows(new Point(outerFrame.getLocationOnScreen()));
                }
            });

        hideClosedBox.addItemListener(new ItemListener() {

                @Override
                public void itemStateChanged(ItemEvent e) {
                    if (e.getStateChange() == ItemEvent.SELECTED) {
                        thisTable.closedPosHidden = true;
                        thisTable.sortRows();
                    } else {
                        thisTable.closedPosHidden = false;
                        thisTable.sortRows();
                    }
                }
            });

        copyCB.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    thisTable.copyTableToClipboard();
                }
            });

        freezeColsPanel.add(freezeColsLabel);
        freezeColsPanel.add(freezeColsBox);

        controlPanel.add(freezeColsPanel);
        controlPanel.add(buttonSort);
        controlPanel.add(hideClosedBox);
        controlPanel.add(copyCB);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 0.0;
        c.weighty = 0.0;
        c.insets = new Insets(0, 0, 10, 0);

        outerFrame.add(controlPanel, c);
        c.insets = new Insets(0, 0, 0, 0);
        c.gridy = 1;
        c.weightx = 1;
        c.weighty = 1;
        c.gridwidth = 3;
        c.fill = GridBagConstraints.BOTH;

        outerFrame.getContentPane().add(thisTable, c);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final Dimension frameSize = new Dimension((int) (screenSize.getWidth() * 0.8),
                                                  (int) (screenSize.getHeight() * 0.8));
        outerFrame.setSize(frameSize);
        outerFrame.setLocation((screenSize.width - frameSize.width) / 2,
                               (screenSize.height - frameSize.height) / 2);
        outerFrame.setVisible(true);
        thisTable.sortRows();

    }

    // public static void main(String[] args) {
    //
    // }
} // end EnTable1 Class
