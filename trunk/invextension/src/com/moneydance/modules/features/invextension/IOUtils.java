/*
 * IOUtils.java
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

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * generic methods to produce file output from ArrayLists of String Arrays
 *
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
public final class IOUtils {

    private IOUtils() {

    }

    /**
     * reads CSV file into ArrayList of Strings
     *
     * @param readFile file to be read from
     * @return ArrayList of Strings
     */
    public static ArrayList<String[]> readCSVIntoArrayList(File readFile) {
        BufferedReader csvRead = null;
        ArrayList<String[]> readArray = new ArrayList<>();
        String newLine;
        try {
            csvRead = new BufferedReader(new FileReader(readFile));
            while ((newLine = csvRead.readLine()) != null) {
                String[] parsedNewLine = parseCSVLine(newLine);
                readArray.add(parsedNewLine);
            }
            return readArray;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (java.io.IOException ex1) {
            Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex1);
            return null;
        } finally {
            try {
                csvRead.close();
            } catch (IOException ex) {
                Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public static String[] parseCSVLine(String s) {
        //return  s.split(",\\s*"); //old code replace with next line
        return s.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        //splits on commas, but ignores commas inside of quotes
        //http://stackoverflow.com/questions/2241758/regarding-java-split-command-parsing-csv-file
    }

    /**
     * Writes data to CSV file
     *
     * @param header         header text for csv file
     * @param writeArrayList body of csv file
     * @param outputFile     file to be written to
     */
    public static void writeArrayListToCSV(StringBuffer header, ArrayList<String[]> writeArrayList, File outputFile) {
        try (PrintWriter outputStream = new PrintWriter(new FileWriter(outputFile))) {
            if (!(header == null)) outputStream.println(header.toString());
            for (String[] aWriteArrayList : writeArrayList) {
                String outputLine = writeCSVLine(aWriteArrayList);
                outputStream.println(outputLine);
            }
        } catch (IOException ex) {
            Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Writes data to CSV file
     *
     * @param writeArrayList body of csv file
     * @param outputFile     file to be written to
     */
    @SuppressWarnings("unused")
    public static void appendArrayListToCSV(ArrayList<String[]> writeArrayList, File outputFile) {
        try (PrintWriter outputStream = new PrintWriter(new FileWriter(outputFile, true))) {
            for (String[] csvLine : writeArrayList) {
                String outputLine = writeCSVLine(csvLine);
                outputStream.println(outputLine);
            }
        } catch (IOException ex) {
            Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
    }


    @SuppressWarnings("unused")
    public static void writeArrayListToScreen(ArrayList<String[]> readArrrayList) {

        for (String[] tempStrings : readArrrayList) {
            String outputString = "";
            for (String eachString : tempStrings) {
                outputString = outputString + eachString + " ";
            }
            System.out.println(outputString);
        }
    }

    @SuppressWarnings("unused")
    public static StringBuffer writeObjectToStringBuffer(Object[][] object) {
        StringBuffer outBuffer = new StringBuffer();
        for (Object[] objects : object) {
            for (int j = 0; j < objects.length; j++) {
                Object element = objects[j] == null ? "*NULL*" : objects[j];
                if (j == objects.length - 1) {
                    outBuffer.append(element.toString()).append("\r\n");
                } else {
                    outBuffer.append(element.toString()).append(",");
                }
            }
        }
        return outBuffer;

    }

    /**
     * writes single csv line from array of strings
     *
     * @param inputStrings input string array
     * @return string w/ commas placed between elements
     */
    public static String writeCSVLine(String[] inputStrings) {
        String outputString = "";

        for (int i = 0; i < inputStrings.length; i++) {
            if (i == inputStrings.length - 1) {
                outputString = outputString + inputStrings[i]; // + "\b\n"
            } else {
                outputString = outputString + inputStrings[i] + ",";
            }
        }
        return outputString;
    }

    /**
     * generic method to write stringbuffer to file
     *
     * @param writeSB    StringBuffer to write
     * @param outputFile file to be written to
     */
    public static void writeResultsToFile(StringBuffer writeSB, File outputFile) {

        try (PrintWriter outputStream = new PrintWriter(new FileWriter(outputFile))) {
            outputStream.print(writeSB.toString());

        } catch (IOException ex) {
            PrintStream output = System.err;
            output.println("File Cannot Be Opened!");
        }
    }
}
