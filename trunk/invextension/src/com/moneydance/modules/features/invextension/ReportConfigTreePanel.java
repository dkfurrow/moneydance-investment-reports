/*
 * ReportConfigTreePanel.java
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
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.prefs.BackingStoreException;

/**
 * Tree panel which stores the various report configurations
 */
public class ReportConfigTreePanel extends JPanel implements TreeSelectionListener {
    public static final ArrayList<Class<? extends TotalReport>> REPORT_CLASSES = new ArrayList<>();

    static {
        REPORT_CLASSES.add(TotalFromToReport.class);
        REPORT_CLASSES.add(TotalSnapshotReport.class);
    }

    private static final long serialVersionUID = -5728445896275033878L;
    DefaultTreeModel reportTreeModel;
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Custom Reports");
    JTree reportTree;
    JScrollPane reportTreeScrollPane;
    JButton removeAllNodesButton;
    JButton removeNodeButton;
    ReportConfigControlPanel reportConfigControlPanel;

    public ReportConfigTreePanel(ReportConfigControlPanel reportConfigControlPanel) throws IllegalAccessException,
            BackingStoreException,
            NoSuchFieldException {
        this.reportConfigControlPanel = reportConfigControlPanel;
        reportTreeModel = new DefaultTreeModel(rootNode);
        reportTree = new JTree(reportTreeModel);
        for (Class<? extends TotalReport> reportClass : REPORT_CLASSES) {
            addReportsNodeToRoot(reportClass);
        }
        reportTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        expandReportTree();
        reportTree.addTreeSelectionListener(this);
        reportTreeScrollPane = new JScrollPane();
        reportTreeScrollPane.setViewportView(reportTree);

        removeAllNodesButton = new JButton("<html><center>"+"Remove All Custom Reports"+"<br>"+"And Reset Run Configs"+"</center></html>");
        removeAllNodesButton.addActionListener(new RemoveAllNodesButtonListener());

        removeNodeButton = new JButton("Remove Selected Report");
        removeNodeButton.addActionListener(new RemoveNodeButtonListener());


        TitledBorder titledBorder = BorderFactory.createTitledBorder("Saved Reports");
        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        titledBorder.setTitleColor(new Color(100, 100, 100));
        this.setBorder(BorderFactory.createCompoundBorder(titledBorder, emptyBorder));


        GridBagConstraints c = new GridBagConstraints();
        this.setLayout(new GridBagLayout());
        c.gridwidth = 1;
        c.insets = new Insets(10, 0, 10, 0);
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;
        this.add(removeAllNodesButton, c);
        c.gridy++;
        this.add(removeNodeButton, c);
        c.gridy++;
        this.add(reportTreeScrollPane, c);

    }

    private void addReportsNodeToRoot(Class<? extends TotalReport> reportClass) throws BackingStoreException, NoSuchFieldException,
            IllegalAccessException {
        ReportConfig.setStandardConfigInPrefs(reportClass);
        rootNode.add(getReportTreeNode(reportClass));
    }

    public void refreshTree() throws IllegalAccessException, BackingStoreException, NoSuchFieldException {
        rootNode.removeAllChildren();
        for (Class<? extends TotalReport> reportClass : REPORT_CLASSES) {
            rootNode.add(getReportTreeNode(reportClass));
        }
        reportTreeModel.reload();
        expandReportTree();
        reportConfigControlPanel.getReportConfigControlFrame().pack();
    }

    public void expandReportTree() {
        for (int i = 0; i < reportTree.getRowCount(); i++) {
            reportTree.expandRow(i);
        }
    }

    public DefaultMutableTreeNode getReportTreeNode(Class<? extends TotalReport> reportClass) throws
            BackingStoreException, NoSuchFieldException, IllegalAccessException {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(ReportConfig.getReportTypeName(reportClass));
        String[] reportNames = ReportConfig.getReportNamesForClass(reportClass);
        for (String reportName : reportNames) {
            top.add(new DefaultMutableTreeNode(reportName));
        }
        return top;
    }

    /**
     * Called whenever the value of the selection changes.
     *
     * @param e the event that characterizes the change.
     */
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                reportTree.getLastSelectedPathComponent();

        if (node == null) return;
        if (node.isLeaf()) {
            DefaultMutableTreeNode reportClassNode = (DefaultMutableTreeNode) node.getParent();
            String reportTypeName = (String) reportClassNode.getUserObject();
            String reportName = (String) node.getUserObject();

            try {
                String reportClassSimpleName = TotalReport.getClassSimpleNameFromReportTypeName(reportTypeName);
                ReportConfig reportConfig = new ReportConfig(reportClassSimpleName, reportName);
                reportConfigControlPanel.setSelectedReportConfig(reportConfig);
                reportConfigControlPanel.populateSubPanels();
                reportConfigControlPanel.getReportConfigControlFrame().pack();
            } catch (Exception e1) {
                ReportConfigControlFrame.setExceptionThrown(true);
                LogController.logException(e1, "Error on Report Tree Panel: ");
                ReportConfigControlFrame.showErrorDialog(this);
            }
        }
    }

    class RemoveNodeButtonListener implements ActionListener {

        /**
         * Invoked when an action occurs.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    reportTree.getLastSelectedPathComponent();

            if (node == null) return;
            if (node.isLeaf()) {
                DefaultMutableTreeNode reportClassNode = (DefaultMutableTreeNode) node.getParent();
                String reportTypeName = (String) reportClassNode.getUserObject();
                String reportName = (String) node.getUserObject();
                if (!reportName.equals(Prefs.STANDARD_NAME)) {
                    try {
                        String reportClassSimpleName = TotalReport.getClassSimpleNameFromReportTypeName
                                (reportTypeName);
                        ReportConfig reportConfig = new ReportConfig(reportClassSimpleName, reportName);
                        reportConfig.clearReportConfig(reportClassSimpleName, reportName);
                        refreshTree();
                    } catch (Exception e1) {
                        LogController.logException(e1, "Error on Report Tree Panel: ");
                        ReportConfigControlFrame.setExceptionThrown(true);
                        ReportConfigControlFrame.showErrorDialog(ReportConfigTreePanel.this);
                    }
                } else {
                    JOptionPane.showMessageDialog(reportConfigControlPanel.getReportConfigControlFrame(),
                            "Cannot Remove Standard Configuration");

                }
            }
        }
    }

    class RemoveAllNodesButtonListener implements ActionListener {

        /**
         * Invoked when an action occurs.
         */
        @Override
        public void actionPerformed(ActionEvent e) {

            rootNode.removeAllChildren();
            try {
                Prefs.clearAllPrefs();
                ReportConfigTreePanel.this.reportConfigControlPanel.resetReportPanelPrefs();
            } catch (BackingStoreException e1) {
                e1.printStackTrace(); //TODO: Add better catch
            }
            for (Class<? extends TotalReport> reportClass : REPORT_CLASSES) {
                try {
//                    ReportConfig.clearAllReportConfigsForClass(reportClass);
                    addReportsNodeToRoot(reportClass);
                } catch (BackingStoreException | NoSuchFieldException | IllegalAccessException e1) {
                    LogController.logException(e1, "Error on Report Tree Panel: ");
                    ReportConfigControlFrame.setExceptionThrown(true);
                    ReportConfigControlFrame.showErrorDialog(ReportConfigTreePanel.this);
                }
                reportTreeModel.reload();
                expandReportTree();
                reportConfigControlPanel.getReportConfigControlFrame().pack();
            }
        }
    }


}
