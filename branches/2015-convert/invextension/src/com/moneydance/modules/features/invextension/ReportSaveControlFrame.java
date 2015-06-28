/*
 * ReportSaveControlFrame.java
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
import java.awt.*;
import java.util.prefs.BackingStoreException;

/**
 * Frame which controls custom report configuration
 */
public class ReportSaveControlFrame extends JFrame {
    private static final long serialVersionUID = -8818472017634165380L;
    ReportControlFrame reportControlFrame;
    ReportSaveControlPanel reportSaveControlPanel;

    public static void setReportSaveControlFrame(ReportControlFrame reportControlFrame)
            throws IllegalAccessException, BackingStoreException, NoSuchFieldException {
        ReportSaveControlFrame reportSaveControlFrame = new ReportSaveControlFrame();
        reportSaveControlFrame.reportControlFrame = reportControlFrame;
        reportSaveControlFrame.reportSaveControlPanel = new ReportSaveControlPanel(reportSaveControlFrame,
                reportControlFrame);
        reportSaveControlFrame.addPanel();
    }

    public void addPanel() {
        this.setTitle("Save Custom Report");
        reportSaveControlPanel.setOpaque(true);
        this.setContentPane(reportSaveControlPanel);
        this.pack();
        Point frameLocation = new Point(reportControlFrame.getLocationOnScreen());
        this.setLocation(frameLocation);
        this.setVisible(true);
    }

    public static void showErrorDialog(Component component){
        JOptionPane.showMessageDialog(component, "Error! See " +
                        ReportControlPanel.getOutputDirectoryPath() + " for details",
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}
