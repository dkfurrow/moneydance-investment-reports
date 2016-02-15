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

import java.awt.*;
import java.io.*;

/**
 * Displays Help File for user
 */
public class HelpFileDisplay {
    public static final String fileLocation = "/com/moneydance/modules/features/invextension/InvestmentReportsHelp.html";


    private FileResources fileResources;

    HelpFileDisplay() {
        super();
        try {
            fileResources = new FileResources();
        } catch (Exception e) {
            String message = e.getClass().getSimpleName() +  " thrown in HelpFileDisplay! ";
            LogController.logException(e, message);
        }
    }

    public boolean showHelpFile() throws IOException {


        File tempFile = fileResources.createTempFile();
        boolean success = false;
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(tempFile.toURI());
                    success = true;
                }
            } else {
                success = false;
            }
        return success;
    }


    public static void main(String[] args) {
        HelpFileDisplay helpFileDisplay = new HelpFileDisplay();
        try {
            boolean success = helpFileDisplay.showHelpFile();
            System.out.println("Helpfile Displayed in Browser -- " + success);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }






    static class FileResources {
        File tempFile;

        FileResources() throws IOException {

            tempFile = File.createTempFile("helpfile", ".html");
        }

        public File createTempFile() throws IOException {
            InputStream is = getClass().getResourceAsStream(fileLocation);
            BufferedReader br = null;
            BufferedWriter bw = null;

            String line;
            try {

                br = new BufferedReader(new InputStreamReader(is));
                bw = new BufferedWriter(new FileWriter(tempFile));
                while ((line = br.readLine()) != null) {
                    bw.write(line);
                }

            }  finally {
                if(bw != null){
                    bw.close();
                }
                if (br != null) {
                    br.close();
                }
            }
            return tempFile;
        }

    }

}
