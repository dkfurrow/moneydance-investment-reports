/*
 * LogController.java
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


import java.io.IOException;
import java.util.logging.*;

/**
 * Class which controls in-program instantiation of logger functionality
 */
public class LogController {
    private Logger logger;
    private static final Level loggerConfigLevel = Level.SEVERE;
    private static final String logFileNamePattern = "investment_reports_error%g.xml";
    private Handler fileHandler;
    private static LogController instance;

    /**
     * prevents instantiation
     */
    private LogController(){

        logger = Logger.getLogger(LogController.class.getName());
        try {

            String filePattern = ReportControlPanel.getOutputDirectoryPath() + "\\" + logFileNamePattern;
            fileHandler = new FileHandler(filePattern, 1000000, 5, true);
            fileHandler.setLevel(loggerConfigLevel);
            Formatter formatter = new CustomXMLFormatter(true);
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static LogController getInstance(){
        if (instance == null){
            instance = new LogController();
        }
        return instance;
    }

    public Logger getLogger(){
        return getInstance().logger;
    }

    @SuppressWarnings("unused")
    public static void logMessage(Level level, String msg){
        if(level.equals(Level.SEVERE)) ReportControlPanel.setSevereError(true);
        LogController.getInstance().getLogger().log(level, msg);
    }

    public static void logException(Exception e, String msg){
        ReportControlPanel.setSevereError(true);
        LogController.getInstance().getLogger().log(Level.SEVERE, msg, e);
    }

    public static void stopLogging(){
        Handler handler = LogController.getInstance().fileHandler;
        handler.close();
        LogController.getInstance().getLogger().removeHandler(handler);
        LogController.getInstance().getLogger().setUseParentHandlers(false);

    }

    class CustomXMLFormatter extends XMLFormatter{
        boolean partialFormat = true;

        CustomXMLFormatter(boolean partialFormat){
            this.partialFormat = partialFormat;
        }

        @Override
        public String getHead(Handler h) {
            return partialFormat ? "" : super.getHead(h);
        }

        @Override
        public String getTail(Handler h) {
            return partialFormat ? "" : super.getTail(h);
        }
    }

}
