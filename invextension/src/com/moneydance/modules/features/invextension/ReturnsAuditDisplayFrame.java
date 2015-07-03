/*
 * ReturnsAuditDisplayPanel.java
 * Copyright (c) 2015, Dale K. Furrow
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
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Diplays contents of ExtractorTotalReturn or ExtractorIRR
 */
public class ReturnsAuditDisplayFrame extends JFrame implements ActionListener {
    private static final long serialVersionUID = -3102906929058309264L;
    public static final String COPY_CLIPBOARD = "copyClipboard";
    private String auditString;
    private ExtractorReturnBase extractor;
    private Point location;
    private int maximumHeight;
    private JTextPane textPane;
    private JScrollPane scrollPane;
    private JButton copyToClipboardButton;

    public static void showReturnsAuditDisplay(final ExtractorReturnBase extractor,
                                               final Point location, final int maximumHeight){
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ReturnsAuditDisplayFrame returnsAuditDisplayFrame =
                        new ReturnsAuditDisplayFrame(extractor, location, maximumHeight);
                returnsAuditDisplayFrame.showFrame();
            }
        });
    }

    public ReturnsAuditDisplayFrame(ExtractorReturnBase extractor, Point location, int maximumHeight)  {
        this.extractor = extractor;
        this.location = location;
        this.maximumHeight = maximumHeight;
        auditString = extractor.getAuditString();
        initComponents();
    }

    private void initComponents(){
        copyToClipboardButton = new JButton("Copy to Clipboard");
        copyToClipboardButton. addActionListener(this);
        copyToClipboardButton.setActionCommand(COPY_CLIPBOARD);
        textPane = new JTextPane();
//        textPane.setSize(new Dimension(500, 500));
        textPane.setText("Returns Information: " + auditString);
        textPane.setCaretPosition(0);
        // make it read-only
        textPane.setEditable(false);
        // create a scrollpane; modify its attributes as desired

        scrollPane = new JScrollPane(textPane);
        scrollPane.setPreferredSize(new Dimension(500, Math.min(maximumHeight, textPane.getPreferredSize().height)));
        // now add it all to a panel

        this.getContentPane().setLayout(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.WEST;

        this.setTitle("Return Calculation Elements");
        this.add(copyToClipboardButton, gc);
        gc.gridy = 1;
        this.getContentPane().add(scrollPane, gc);
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    public void showFrame() {
        this.setLocation(location);
        this.pack();
        this.setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String actionCommand = e.getActionCommand();
        if (actionCommand.equals(COPY_CLIPBOARD)) {
            try {
                copyAuditStringToClipboard();
            } catch (Exception e1) {
                LogController.logException(e1, "Error on Copy Audit String to Clipboard: ");
                JOptionPane.showMessageDialog(this, "Error! See " +
                                ReportControlPanel.getOutputDirectoryPath() + " for details", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

    }

    public void copyAuditStringToClipboard() throws Exception {

        StringSelection stsel = new StringSelection(auditString);
        Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
        system.setContents(stsel, stsel);
    }

}
