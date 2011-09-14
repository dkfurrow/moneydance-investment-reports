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

public class SecReportFrame
  extends JFrame

{
    private static final long serialVersionUID = 334888425056725292L;
    private Main extension;

    public SecReportFrame(Main extension) {
    super("Investment Reports/Raw Data Downloads"); // sets text on JFrame
    this.extension = extension;
    RootAccount root = extension.getUnprotectedContext().getRootAccount();
    SecReportPanel panel = new SecReportPanel(root);

    panel.setOpaque(true);
    this.setContentPane(panel);
    this.pack();
    this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    this.enableEvents(WindowEvent.WINDOW_CLOSING);

    AwtUtil.centerWindow(this);
  }


  public final void processEvent(AWTEvent evt) { 
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
