/*
 * HtmlEditorTestKit.java
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Displays Help File for user
 */
public class HelpFileDisplay extends JFrame {
    public static final String fileLocation = "/com/moneydance/modules/features/invextension/InvestmentReportsHelp.html";
    private static final long serialVersionUID = 2433263830156875464L;
    private Point location;

    HelpFileDisplay(Point point) {
        super();
        FileResources fileResources = new FileResources();
        String displayString = fileResources.getOutString();
        location = point;
        // create jeditorpane
        JEditorPane editor = new JEditorPane();

        editor.getDocument().putProperty("Ignore-Charset", "true");  // this line makes no difference either way
        editor.setContentType("text/html");
        editor.getDocument().putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        editor.setText(displayString);

        // make it read-only
        editor.setEditable(false);

        // create a scrollpane; modify its attributes as desired
        JScrollPane scrollPane = new JScrollPane(editor);

        // now add it all to a frame
        this.setTitle("Investment Reports: Help/Notes");
        this.getContentPane().add(scrollPane, BorderLayout.CENTER);

        // make it easy to close the application
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        // display the frame
        this.setSize(new Dimension(800, 600));

        // pack it, if you prefer
        //j.pack();

        // center the jframe, then make it visible
        editor.setCaretPosition(0);
    }

    public static void main(String[] args) {

        showHelpFile(new ReportConfig.FrameInfo().getPoint());
    }

    public static void showHelpFile(final Point locationOnScreen) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                HelpFileDisplay helpFileDisplay = new HelpFileDisplay(locationOnScreen);
                helpFileDisplay.showDisplay();

            }
        });

    }

    public void showDisplay() {
        this.setLocation(location);
        this.setVisible(true);
    }

    static class FileResources {
        String outString;

        FileResources() {
            InputStream in = getClass().getResourceAsStream(fileLocation);
            outString = getStringFromInputStream(in);

        }

        public String getOutString() {
            return outString;
        }

        private String getStringFromInputStream(InputStream is) {

            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();

            String line;
            try {

                br = new BufferedReader(new InputStreamReader(is));
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return sb.toString();
        }


    }

}
