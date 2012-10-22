/* FormattedTable.java
 * Copyright 2011 Dale K. Furrow . All rights reserved.
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
import java.awt.Font;

import java.text.DecimalFormat;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import com.moneydance.modules.features.invextension.FormattedTable.ColSizeOption;


class FormattedTable extends JTable {
    private static final long serialVersionUID = 1616850162785345995L;

    public enum RowBackground {DEFAULT, LIGHTGRAY, LIGHTLIGHTGRAY, GREEN}

    public enum ColFormat {STRING, DOUBLE0, DOUBLE2, DOUBLE3, PERCENT1}

    public enum ColSizeOption {NORESIZE, MAXCONTRESIZE, MAXCONTCOLRESIZE}

    private RowBackground[] rowBackgrounds;

    private Color lightLightGray = new Color(230, 230, 230);

    public FormattedTable(TableModel model,
                          ColFormat[] colFormats,
                          ColSizeOption sizeOption,
                          RowBackground[] rowBacks) {
        super(model);

        rowBackgrounds = rowBacks;

        TableColumn tableColumn = new TableColumn();
        for (int i = 0; i < colFormats.length; i++) {
            ColFormat colFormat = colFormats[i];
            tableColumn = this.getColumnModel().getColumn(i);
            if (colFormat == ColFormat.DOUBLE0) {
                tableColumn.setCellRenderer(new NumberTableCellRenderer(0, 0));
            } else if (colFormat == ColFormat.DOUBLE2) {
                tableColumn.setCellRenderer(new NumberTableCellRenderer(2, 2));
            } else if (colFormat == ColFormat.DOUBLE3) {
                tableColumn.setCellRenderer(new NumberTableCellRenderer(3, 3));
            } else if (colFormat == ColFormat.PERCENT1) {
                tableColumn.setCellRenderer(new PercentTableCellRenderer(1, 1));
            } else {
                tableColumn.setCellRenderer(new StringTableCellRenderer());
            }
        }

        this.adjustColumnPreferredWidths(sizeOption);
    }

    public void adjustColumnPreferredWidths(ColSizeOption option) {
        // strategy - get max width for cells in column and make that
        // the preferred width
        TableColumnModel columnModel = getColumnModel();
        for (int col = 0; col < getColumnCount(); col++) {
            int maxwidth = 0;
            for (int row = 0; row < getRowCount(); row++) {
                TableCellRenderer rend = getCellRenderer(row, col);
                Object value = getValueAt(row, col);
                Component comp = rend.getTableCellRendererComponent(this, value, false, false, row, col);

                int upSize = 0;
                // workaround--getPreferredSize insufficient for (at
                // least some) negative numbers, so set width based on
                // one size larger
                if ((value instanceof Integer || value instanceof Double)
                    && ((Double) value < -1.0)) {
                    JLabel comp1 = (JLabel) comp;
                    Font f1 = new Font(comp1.getFont().getName(),
                                       comp1.getFont().getStyle(),
                                       comp1.getFont().getSize() + 1);
                    comp1.setFont(f1);
                    upSize = comp1.getPreferredSize().width;
                }
                int currentWidth = Math.max(getColumnModel().getColumn(col).getWidth(), upSize);
                // set to maximum of all obtained widths
                maxwidth = Math.max(comp.getPreferredSize().width, maxwidth);
                maxwidth = Math.max(currentWidth, maxwidth);
            }

            // for row following code resizes columns to the maximum of
            // header and contents
            TableColumn column = columnModel.getColumn(col);
            switch (option) {
            case MAXCONTCOLRESIZE:
                TableCellRenderer headerRenderer = column.getHeaderRenderer();
                if (headerRenderer == null) {
                    headerRenderer = getTableHeader().getDefaultRenderer();
                }
                Object headerValue = column.getHeaderValue();
                Component headerComp = headerRenderer
                    .getTableCellRendererComponent(this, headerValue, false, false, -1, col); // changed to -1
                maxwidth = Math.max(maxwidth,
                                    headerComp.getPreferredSize().width);
                column.setPreferredWidth(maxwidth);
                break;
            case MAXCONTRESIZE:
                column.setPreferredWidth(maxwidth);
                break;
            case NORESIZE:
                break;
            default:
                continue;
            }
        }
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        // Color row based on a cell value--overrides TableCellRenders
        // in Constructor
        if (!isRowSelected(row)) {
            c.setBackground(getBackground());

            int modelRow = convertRowIndexToModel(row);
            switch (rowBackgrounds[modelRow]) {
            case DEFAULT:
                break;

            case LIGHTGRAY:
                c.setBackground(Color.lightGray);
                break;

            case LIGHTLIGHTGRAY:
                c.setBackground(lightLightGray);
                break;

            case GREEN:
                c.setBackground(Color.GREEN);
                break;

            default:
                throw new AssertionError(rowBackgrounds[modelRow]);
            }
        }

        return c;
    }

    class NumberTableCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = -1219099935272135292L;

        int minDecPlaces;
        int maxDecPlaces;

        public NumberTableCellRenderer(int minDecPlaces, int maxDecPlaces) {
            super();
            this.minDecPlaces = minDecPlaces;
            this.maxDecPlaces = maxDecPlaces;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof Integer) {// set Integers to Right
                DecimalFormat numberFormat = new DecimalFormat("#,###;(#,###)");
                Integer i = (Integer) value;
                JLabel renderedLabel2 = (JLabel) cell;
                renderedLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
                String text = numberFormat.format(i);
                renderedLabel2.setText(text);
                renderedLabel2.setForeground(i < 0 ? Color.RED : Color.BLACK);
            }
            else if (value instanceof Double) {
                Double d = (Double) value;
                DecimalFormat numberFormat = new DecimalFormat("#,##0;(#,##0)");
                DecimalFormat zeroFormat = new DecimalFormat("");
                numberFormat.setMinimumFractionDigits(minDecPlaces);
                numberFormat.setMaximumFractionDigits(maxDecPlaces);

                JLabel renderedLabel2 = (JLabel) cell;
                renderedLabel2.setHorizontalAlignment(d == 0.0
                                                      ? SwingConstants.CENTER
                                                      : SwingConstants.RIGHT);

                String text = d == 0.0 ? "-" : numberFormat.format(d);
                renderedLabel2.setText(text);
                renderedLabel2.setForeground(d < 0 ? Color.RED : Color.BLACK);
            }

            return cell;
        }
    }

    class PercentTableCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = -7691747688266451996L;

        int minDecPlaces;
        int maxDecPlaces;

        public PercentTableCellRenderer(int minDecPlaces, int maxDecPlaces) {
            super();
            this.minDecPlaces = minDecPlaces;
            this.maxDecPlaces = maxDecPlaces;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof Double) {
                DecimalFormat pctFormat = new DecimalFormat("#.#%");
                pctFormat.setMinimumFractionDigits(minDecPlaces);
                pctFormat.setMaximumFractionDigits(maxDecPlaces);
                Double d = (Double) value;
                JLabel renderedLabel2 = (JLabel) cell;
                renderedLabel2.setHorizontalAlignment(SwingConstants.RIGHT);
                String text = pctFormat.format(d);
                renderedLabel2.setText(text);
                renderedLabel2.setForeground(d < 0 ? Color.RED : Color.BLACK);
            }

            return cell;
        }
    }

    class StringTableCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 2532342046629811880L;

        public StringTableCellRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            Component cell = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            if (value instanceof String) {
                JLabel renderedLabel = (JLabel) cell;
                renderedLabel.setHorizontalAlignment(SwingConstants.LEFT);
                renderedLabel.setForeground(Color.BLACK);
            }

            return cell;
        }
    }
}
