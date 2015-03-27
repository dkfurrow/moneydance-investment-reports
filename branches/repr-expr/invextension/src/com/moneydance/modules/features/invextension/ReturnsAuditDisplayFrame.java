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

/**
 * Diplays contents of ExtractorTotalReturn or ExtractorIRR
 */
public class ReturnsAuditDisplayFrame extends JFrame {
    private ExtractorReturnBase extractor;
    private Point location;
    private int maximumHeight;

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
        initComponents();
    }

    private void initComponents(){
        JTextPane textPane = new JTextPane();
        textPane.setText("Returns Information: " + extractor.getAuditString());
        // make it read-only
        textPane.setEditable(false);
        // create a scrollpane; modify its attributes as desired
        JScrollPane scrollPane = new JScrollPane(textPane);
        // now add it all to a frame
        this.setTitle("Return Calculation Elements");
        this.getContentPane().add(scrollPane, BorderLayout.CENTER);
        // make it easy to close the application
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }

    public void showFrame() {
        this.setLocation(location);
        this.setMaximumSize(new Dimension(500, maximumHeight));
        this.pack();
        this.setVisible(true);
    }
}
