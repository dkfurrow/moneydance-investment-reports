/*
 * ReportControlFrame.java
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

import com.moneydance.awt.AwtUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.Serial;

/**
 * Main controller frame which controls report generation
 */
public class ReportControlFrame
        extends JFrame

{
    @Serial
    private static final long serialVersionUID = 334888425056725292L;
    private final ReportControlPanel reportControlPanel;
    private final boolean runInApplication;


    /**
     * Constructor which initiates from application
     */
    public ReportControlFrame() throws Exception {
        super("Investment Reports/Raw Data Downloads"); // sets text on JFrame
        runInApplication = true;
        reportControlPanel = new ReportControlPanel(this);
        populateReportFrame();
    }

    /**
     * Constructor which initiates from saved MD data
     *
     * @param mdFolder valid MD files
     */
    public ReportControlFrame(File mdFolder) throws Exception {
        super("Investment Reports/Raw Data Downloads"); // sets text on JFrame
        runInApplication = false;
        reportControlPanel = new ReportControlPanel(this);
        populateReportFrame();
        MDData.getInstance().loadMDFile(mdFolder, reportControlPanel);
    }

    public boolean isRunInApplication(){
        return runInApplication;
    }

    public ReportControlPanel getReportControlPanel() {
        return reportControlPanel;
    }

    @SuppressWarnings("unused")
    public void setCustomDateRange(DateRange dateRange) {
        DateRangePanel datePanel = reportControlPanel.getDateRangePanel();
        datePanel.populateDateRangePanel(dateRange);
    }

    private void populateReportFrame() {
        reportControlPanel.setOpaque(true);
        this.setVisible(false);
        this.setContentPane(reportControlPanel);
        this.pack();
        // Change default behavior (hide a JFrame on close) to act like a Frame (actually close).
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.enableEvents(WindowEvent.WINDOW_CLOSING | WindowEvent.WINDOW_OPENED);
        AwtUtil.centerWindow(this);
        this.setVisible(true);
    }


    @Override
    public final void processEvent(AWTEvent evt) {
        if (evt.getID() == WindowEvent.WINDOW_CLOSING) {
            if (MDData.getInstance().getExtension() != null) {
                MDData.getInstance().getExtension().closeConsole();
            } else {
                goAway();
            }
            return;
        }
        super.processEvent(evt);
    }

    void goAway() { // invoked from Main by closeConsole
        reportControlPanel.savePreferences();
        setVisible(false);
        dispose();
        if (MDData.getInstance().getExtension() == null) System.exit(0); //exit if run as headless
    }
}
