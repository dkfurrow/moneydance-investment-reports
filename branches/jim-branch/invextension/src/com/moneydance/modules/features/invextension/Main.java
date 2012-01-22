/* Main.java
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
    }
  }
}


