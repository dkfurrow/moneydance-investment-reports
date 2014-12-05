/*
 * TestReportOutput.java
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
import java.io.File;


@SuppressWarnings("unused")
public class TestReportOutput extends JFrame {

    public static final DateRange testDateRange = new DateRange(20090601, 20100601, 20100601);
    public static final File testFile = new File("E:\\Temp\\testFile.csv");
    private static final long serialVersionUID = -2315625753772573103L;
    private static final int fromDateInt = 20090601;
    private static final int toDateInt = 20100601;
    /**
     *
     */
    //
    //
    private static String testFileStr = "./resources/testMD02.moneydance/root.mdinternal";
    private static String testFileStr1 = "E:\\\\RECORDS\\moneydance\\\\Test\\\\20141014test.moneydance\\\\root.mdinternal";
    private static String testFileStr2 = "E:\\\\RECORDS\\moneydance\\\\Test\\\\TestSave.moneydance\\\\root.mdinternal";
    public static final File mdTestFile = new File(testFileStr);

    public static void main(String[] args) throws Exception {

        ReportControlFrame frame = new ReportControlFrame(mdTestFile);


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


}
