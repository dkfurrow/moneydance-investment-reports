/************************************************************\
 *       Copyright (C) 2001 Appgen Personal Software        *
\************************************************************/

package com.moneydance.modules.features.invextension;

import com.moneydance.awt.*;
import com.moneydance.apps.md.model.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/** Window used for Account List interface
  ------------------------------------------------------------------------
*/

public class ReportWindow
  extends JFrame

{
  private Main extension;

    public ReportWindow(Main extension) {
    super("Investment Reports Download to CSV File"); // sets text on JFrame
    this.extension = extension;
    RootAccount root = extension.getUnprotectedContext().getRootAccount();
    SecReportPanel panel = new SecReportPanel(extension, root);
        this.getContentPane().add(panel, "Center");
        this.setSize(panel.getPreferredSize());

    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.enableEvents(WindowEvent.WINDOW_CLOSING);

    AwtUtil.centerWindow(this);
  }


  public final void processEvent(AWTEvent evt) { //relates to window event--should add to SecReportPanel
    if(evt.getID()==WindowEvent.WINDOW_CLOSING) {
      extension.closeConsole();
      return;
    }
    if(evt.getID()==WindowEvent.WINDOW_OPENED) {
    }
    super.processEvent(evt);
  }

  void goAway() { //invoked from Main by closeConsole
    setVisible(false);
    dispose();
  }
}
