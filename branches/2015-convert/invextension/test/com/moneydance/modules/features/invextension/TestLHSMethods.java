/*
 * TestLHSMethods.java
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


import java.util.Arrays;
import java.util.LinkedList;

/**
 * Test methods for modelheader, viewed fields, etc
 */
class TestLHSMethods {
    private static LinkedList<String> testModelHeader = new LinkedList<>(Arrays.asList("InvAcct", "Security", "SecType",
            "SecSubType", "Ticker", "StartPos", "EndPos", "StartPrice", "EndPrice", "StartValue", "EndValue", "buy",
            "sell", "shortSell", "coverShort", "income", "Expense", "longBasis", "shortBasis", "realizedGain",
            "unrealizedGain", "periodReturn", "percentReturn", "annualPercentReturn"));

    private LinkedList<String> viewedFields;
    private LinkedList<String> hiddenCols;

    @SafeVarargs
    TestLHSMethods(LinkedList<String>... headers) throws Exception {
        LinkedList<String> modelHeader;
        if (headers.length == 2) {
            modelHeader = headers[0];
            this.viewedFields = headers[1];
            this.hiddenCols = modelHeader;
            this.hiddenCols.removeAll(this.viewedFields);
        } else if (headers.length == 1) {
            modelHeader = headers[0];
            this.viewedFields = modelHeader;
            this.hiddenCols = new LinkedList<>();
        } else {
            throw new Exception("Wrong number of parameters");
        }
    }

    public static void main(String[] args) {
        try {
            TestLHSMethods testMethod = new TestLHSMethods(testModelHeader);
            //testMethod.moveCol("EndValue", true);
            //testMethod.moveCol("Ticker", false);
            //testMethod.viewedFields.add(2, "foo");
            //testMethod.viewedFields.add("InvAcct");
            testMethod.moveCol("InvAcct", false);
            testMethod.hideColumn("periodReturn");
            testMethod.hideColumn("Expense");
            for (String string : testMethod.viewedFields) {
                System.out.println(string);
            }
            System.out.println("Here are hidden columns");
            for (String string : testMethod.hiddenCols) {
                System.out.println(string);
            }
            testMethod.restoreHidden("Expense");
            System.out.println("Here are hidden columns");
            for (String string : testMethod.hiddenCols) {
                System.out.println(string);
            }

            System.out.println("Here are new columns");
            for (String string : testMethod.viewedFields) {
                System.out.println(string);
            }

        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void restoreHidden(String inString) throws Exception {
        int indexOf = hiddenCols.indexOf(inString);
        if (indexOf == -1) {
            throw new ViewCreateException("restore string not found in hidden columns");
        }
        hiddenCols.remove(indexOf);
        viewedFields.addLast(inString);
    }

    public void hideColumn(String inString) throws Exception {
        int indexOf = viewedFields.indexOf(inString);
        if (indexOf == -1) {
            throw new ViewCreateException("hide string not found in view header");
        }
        viewedFields.remove(indexOf);
        hiddenCols.add(0, inString);
    }

    public void moveCol(String inString, boolean up) throws Exception {
        int indexOf = viewedFields.indexOf(inString);
        if (indexOf == -1) throw new ViewCreateException("move string not found in view header");
        if (indexOf != viewedFields.lastIndexOf(inString))
            throw new ViewCreateException("move string found more than once in view header");

        if (up) {
            if (indexOf == 0) {
                throw new ViewCreateException("Cannot Move Up--Already in First Place");
            }
            viewedFields.add(indexOf - 1, inString);
            viewedFields.remove(indexOf + 1);
        } else {
            if (indexOf == viewedFields.size() - 1) {
                throw new ViewCreateException("Cannot Move Down--Already in Last Place");
            }
            viewedFields.add(indexOf + 2, inString);
            viewedFields.remove(indexOf);
        }
    }

    class ViewCreateException extends Exception {
        @SuppressWarnings("unused")
        public ViewCreateException() {
        }

        public ViewCreateException(String message) {
            super(message);
        }
    }
}
