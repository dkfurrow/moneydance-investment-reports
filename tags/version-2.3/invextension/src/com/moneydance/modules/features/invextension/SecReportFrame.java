/* SecReportFrame.java
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

import com.moneydance.awt.*;
import com.moneydance.apps.md.model.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Window used for Account List interface
  ------------------------------------------------------------------------
*/

public class SecReportFrame
  extends JFrame

{
    private static final long serialVersionUID = 334888425056725292L;
    private Main extension;
    private SecReportPanel panel;

    public SecReportFrame(Main extension) {
    super("Investment Reports/Raw Data Downloads"); // sets text on JFrame
    this.extension = extension;
    RootAccount root = extension.getUnprotectedContext().getRootAccount();
    panel = new SecReportPanel(root);

    panel.setOpaque(true);
    this.setContentPane(panel);
    this.pack();
    // Change default behavior (hide a JFrame on close) to act like a Frame (actually close).
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.enableEvents(WindowEvent.WINDOW_CLOSING | WindowEvent.WINDOW_OPENED);

    AwtUtil.centerWindow(this);
  }


  @Override
public final void processEvent(AWTEvent evt) { 
    if(evt.getID()==WindowEvent.WINDOW_CLOSING) {
      extension.closeConsole();
      return;
    }
    if(evt.getID()==WindowEvent.WINDOW_OPENED) {
    }
    super.processEvent(evt);
  }

    void goAway() { // invoked from Main by closeConsole
	panel.savePreferences();
	setVisible(false);
	dispose();
    }
}
