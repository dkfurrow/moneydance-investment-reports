/*
 * MDBusinessDayUtil.java
 *  Copyright (C) 2010 Dale Furrow
 *  dkfurrow@google.com
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program; if not, a copy may be found at
 *  http://www.gnu.org/licenses/lgpl.html
 */

package com.moneydance.modules.features.invextension;

import com.moneydance.apps.md.controller.FeatureModule;
import com.moneydance.apps.md.controller.FeatureModuleContext;
import java.io.*;
import java.awt.*;

/** Initiates extention in Moneydance, passes Moneydance data
 * to other classes
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
*/

public class Main
  extends FeatureModule
{
  private SecReportFrame reportWindow = null;

  public void init() {
    // the first thing we will do is register this module to be invoked
    // via the application toolbar
    FeatureModuleContext context = getContext();
    try {
	//relates to "invoke" method below
	context.registerFeature(this, "showreportwindow", 
        getIcon("invextension"), getName());
    }
    catch (Exception e) {
      e.printStackTrace(System.err);
    }
  }

  public void cleanup() { //API method to end program (no other usages)
    closeConsole();
  }
  
  private Image getIcon(String action) {
    try {
      ClassLoader cl = getClass().getClassLoader();
      java.io.InputStream in = 
        cl.getResourceAsStream("/com/moneydance/modules/features/myextension/icon.gif");
      if (in != null) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream(1000);
        byte buf[] = new byte[256];
        int n = 0;
        while((n=in.read(buf, 0, buf.length))>=0)
          bout.write(buf, 0, n);
        return Toolkit.getDefaultToolkit().createImage(bout.toByteArray());
      }
    } catch (Throwable e) { }
    return null;
  }
  
  /** Process an invocation of this module with the given URI */
  //no usages elsewhere, utilized by moneydance
  public void invoke(String uri) {
    String command = uri;
    @SuppressWarnings("unused")
    String parameters = "";
    int theIdx = uri.indexOf('?');
    if(theIdx>=0) {
      command = uri.substring(0, theIdx);
      parameters = uri.substring(theIdx+1);
    }
    else {
      theIdx = uri.indexOf(':');
      if(theIdx>=0) {
        command = uri.substring(0, theIdx);
      }
    }

    if(command.equals("showreportwindow")) { 
	//relates to "showreportpanel" in init
      showReportWindow();
    }    
  }

  public String getName() {
    return "Investment Reports";
  }

  private synchronized void showReportWindow() {
    if(reportWindow==null) {
      reportWindow = new SecReportFrame(this);
      reportWindow.setVisible(true);
    }
    else {
      reportWindow.setVisible(true);
      reportWindow.toFront();
      reportWindow.requestFocus();
    }
  }
  
  FeatureModuleContext getUnprotectedContext() {
    return getContext();
  }

  synchronized void closeConsole() { //called from SecReportFrame on Close Button
    if(reportWindow!=null) {
      reportWindow.goAway(); //method which sets visible to false and disposes
      reportWindow = null;
      System.gc(); //run garbage collector

    }
  }
}


