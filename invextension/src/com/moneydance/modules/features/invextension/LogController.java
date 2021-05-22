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


import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Class which controls in-program instantiation of logger functionality
 */
public class LogController {
    private final Logger logger;
    private static final Level loggerConfigLevel = Level.WARNING;
    private static final String errorLogFilenamePattern = "investment_reports_error%g.xml";
    private Handler errorFileHandler;
    private static final String verboseLogFilenamePattern = "investment_reports_log%g.txt";
    private boolean isVerbose;
    private Handler verboseFileHandler;
    private static LogController instance;

    /**
     * prevents instantiation
     */
    private LogController(boolean isVerbose){

        logger = Logger.getLogger(LogController.class.getName());
        this.isVerbose = isVerbose;
        try {
            String separator = File.separator;
            String errorFilePattern = ReportControlPanel.getOutputDirectoryPath() + separator + errorLogFilenamePattern;
            errorFileHandler = new FileHandler(errorFilePattern, 1000000, 5, true);
            errorFileHandler.setLevel(loggerConfigLevel);
            Formatter errorFormatter = new CustomXMLFormatter(true);
            errorFileHandler.setFormatter(errorFormatter);
            logger.addHandler(errorFileHandler);

            if (this.isVerbose) {
                String verboseFilePattern =
                        ReportControlPanel.getOutputDirectoryPath() + separator + verboseLogFilenamePattern;
                verboseFileHandler = new FileHandler(verboseFilePattern, 1000000, 5, true);
                verboseFileHandler.setLevel(Level.FINE);
                Formatter verboseFormatter = new SingleLineFormatter();
                verboseFileHandler.setFormatter(verboseFormatter);
                logger.addHandler(verboseFileHandler);
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void setVerbose(){
        instance = new LogController(true);
    }

    public static LogController getInstance(){
        if (instance == null){
            instance = new LogController(false);
        }
        return instance;
    }

    public Logger getLogger(){
        return getInstance().logger;
    }


    public static void logMessage(Level level, String msg){
        ReportControlPanel.setLogLevel(level);
        LogController.getInstance().getLogger().log(level, msg);
    }

    public static void logException(Exception e, String msg){
        ReportControlPanel.setLogLevel(Level.SEVERE);
        LogController.getInstance().getLogger().log(Level.SEVERE, msg, e);
    }

    public static void stopLogging() {
        Handler fileHandler = LogController.getInstance().errorFileHandler;
        if (fileHandler != null) {
            fileHandler.close();
            LogController.getInstance().getLogger().removeHandler(fileHandler);
            LogController.getInstance().getLogger().setUseParentHandlers(false);
        }
    }

    static class CustomXMLFormatter extends XMLFormatter{
        boolean partialFormat;

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

    public class SingleLineFormatter extends Formatter {

        Date dat = new Date();
        private final static String format = "{0,date} {0,time}";
        private MessageFormat formatter;
        private Object args[] = new Object[1];

        // Line separator string.  This is the value of the line.separator
        // property at the moment that the SimpleFormatter was created.
        //private String lineSeparator = (String) java.security.AccessController.doPrivileged(
        //        new sun.security.action.GetPropertyAction("line.separator"));
        private String lineSeparator = "\n";

        /**
         * Format the given LogRecord.
         * @param record the log record to be formatted.
         * @return a formatted log record
         */
        public synchronized String format(LogRecord record) {

            StringBuilder sb = new StringBuilder();

            // Minimize memory allocations here.
            dat.setTime(record.getMillis());
            args[0] = dat;


            // Date and time
            StringBuffer text = new StringBuffer();
            if (formatter == null) {
                formatter = new MessageFormat(format);
            }
            formatter.format(args, text, null);
            sb.append(text);
            sb.append(" ");


            // Class name
            if (record.getSourceClassName() != null) {
                sb.append(record.getSourceClassName());
            } else {
                sb.append(record.getLoggerName());
            }

            // Method name
            if (record.getSourceMethodName() != null) {
                sb.append(" ");
                sb.append(record.getSourceMethodName());
            }
            sb.append(" - "); // lineSeparator



            String message = formatMessage(record);

            // Level
            sb.append(record.getLevel().getLocalizedName());
            sb.append(": ");

            // Indent - the more serious, the more indented.
            //sb.append( String.format("% ""s") );
            int iOffset = (1000 - record.getLevel().intValue()) / 100;
            for( int i = 0; i < iOffset;  i++ ){
                sb.append(" ");
            }


            sb.append(message);
            sb.append(lineSeparator);
            if (record.getThrown() != null) {
                try {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    record.getThrown().printStackTrace(pw);
                    pw.close();
                    sb.append(sw.toString());
                } catch (Exception ex) {
                }
            }
            return sb.toString();
        }
    }

}
