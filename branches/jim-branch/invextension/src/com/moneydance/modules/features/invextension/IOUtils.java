/* IOUtils.java
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * generic methods to produce file output from ArrayLists of String Arrays
 * @author Dale Furrow
 * @version 1.0
 * @since 1.0
 */
public final class IOUtils {

    private IOUtils(){

    }

/**
 * reads CSV file into ArrayList of Strings
 * @param readFile file to be read from
 * @return ArrayList of Strings
 */
public static ArrayList<String[]> readCSVIntoArrayList(File readFile) {
        BufferedReader csvRead = null;
        ArrayList<String[]> readArray = new ArrayList<String[]>();
        String newLine = new String();
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

/**
 * reads ini file from home directory
 * @param readFile file to read
 * @return text of ini file (path for reporting)
 */
public static String readIniFile(File readFile) {
        BufferedReader iniRead = null;
        String filePath = new String();
        String newLine = new String();
        try {
            iniRead = new BufferedReader(new FileReader(readFile));
            while ((newLine = iniRead.readLine()) != null) {
                filePath = newLine;
            }
            return filePath;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        } catch (java.io.IOException ex1) {
            Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex1);
            return null;
        } finally {
            try {
                iniRead.close();
            } catch (IOException ex) {
                Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

/**
 * write ini file
 * @param pathString String representation of report path
 */
    public static void writeIniFile(String iniFilePath, String pathString) {
        PrintWriter outputStream = null;
        File iniFile = new File(iniFilePath);
        try {
            outputStream = new PrintWriter(new FileWriter(iniFile));
            outputStream.println(pathString);
        } catch (IOException ex) {
            Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            outputStream.close();
        }
    }

    public static String[] parseCSVLine(String s) {
        //return  s.split(",\\s*"); //old code replace with next line
        return  s.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
        //splits on commas, but ignores commas inside of quotes
        //http://stackoverflow.com/questions/2241758/regarding-java-split-command-parsing-csv-file
    }

    /**
     * Writes data to CSV file
     * @param header header text for csv file
     * @param writeArrayList body of csv file
     * @param outputFile file to be written to
     */
    public static void writeArrayListToCSV(StringBuffer header ,ArrayList<String[]> writeArrayList, File outputFile) {
        PrintWriter outputStream = null;
        try {
            outputStream = new PrintWriter(new FileWriter(outputFile));
            if(!(header == null)) outputStream.println(header.toString());
            for (Iterator<String[]> it = writeArrayList.iterator(); it.hasNext();) {
                String outputLine = writeCSVLine(it.next());
                outputStream.println(outputLine);
            }
        } catch (IOException ex) {
            Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            outputStream.close();
        }
    }

    /**
     * Writes data to CSV file
     * @param writeArrayList body of csv file
     * @param outputFile file to be written to
     */
    public static void appendArrayListToCSV(ArrayList<String[]> writeArrayList, File outputFile) {
        PrintWriter outputStream = null;
        try {
            outputStream = new PrintWriter(new FileWriter(outputFile, true)); //append mode
            for (Iterator<String[]> it = writeArrayList.iterator(); it.hasNext();) {
                String outputLine = writeCSVLine(it.next());
                outputStream.println(outputLine);
            }
        } catch (IOException ex) {
            Logger.getLogger(IOUtils.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            outputStream.close();
        }
    }



    public static void writeArrayListToScreen(ArrayList<String[]> readArrrayList) {

        for (Iterator<String[]> it = readArrrayList.iterator(); it.hasNext();) {
            String[] tempStrings = it.next();
            String outputString = new String();
            for (String eachString : tempStrings) {
                outputString = outputString + eachString + " ";
            }
            System.out.println(outputString);
            outputString = null;
        }
    }

    /**
     * writes single csv line from array of strings
     * @param inputStrings input string array
     * @return string w/ commas placed between elements
     */
    public static String writeCSVLine(String[] inputStrings) {
        String outputString = new String();

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
     * @param writeSB StringBuffer to write
     * @param outputFile file to be written to
     */
    public static void writeResultsToFile(StringBuffer writeSB, File outputFile) {
        PrintWriter outputStream = null;

        try {
            outputStream = new PrintWriter(new FileWriter(outputFile));
            outputStream.print(writeSB.toString());

        } catch (IOException ex) { 
           PrintStream output = System.err;
           output.println("File Cannot Be Opened!");
        } finally {
            outputStream.close();
        }
    }
}
