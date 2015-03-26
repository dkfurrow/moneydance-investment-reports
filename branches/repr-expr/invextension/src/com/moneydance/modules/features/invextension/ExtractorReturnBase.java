/*
 * ExtractorReturnBase.java
 * Copyright (c) 2015, Dale K. Furrow
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

import org.jetbrains.annotations.NotNull;

/**
 * Created by larus on 3/1/15.
 * <p/>
 * Class that allows dynamic selection between Modified-Dietz and ordinary return calculations.
 */
@SuppressWarnings("ALL")
public class ExtractorReturnBase extends ExtractorBase<Double> {


    private String description = "";
    public ExtractorReturnBase(SecurityAccountWrapper secAccountWrapper, SecurityReport securityReport, int startDateInt, int endDateInt,
                               ReturnWindowType returnWindowType) {
        super(secAccountWrapper, startDateInt, endDateInt);
        if(securityReport != null) description = securityReport.getDescription();
    }

    protected static ExtractorReturnBase factory(SecurityAccountWrapper secAccountWrapper, SecurityReport securityReport, int startDateInt,
                                                 int endDateInt, ReturnWindowType returnWindowType, boolean useOrdinary) {
        if (useOrdinary) {
            return new ExtractorOrdinaryReturn(secAccountWrapper, securityReport, startDateInt, endDateInt, returnWindowType);
        } else {
            return new ExtractorModifiedDietzReturn(secAccountWrapper, securityReport, startDateInt, endDateInt, returnWindowType);
        }
    }

    public String getDescription() {
        return description;
    }


    public enum ReturnWindowType {
        DEFAULT("Requires NonZero Initial Value at Window Start"),
        ALL("Adjust Start Date to Day Before First Transaction"),
        ANY("Any open position between window start and window end"),
        STUB("Returns 'ANY' only if Zero Initial Value at Window Start");

        private final String description;

        ReturnWindowType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return this.description;
        }
    }

    public class ReturnValueElement implements Comparable<ReturnValueElement> {
        public final int date;
        public long value;
        public  double txnId;

        public ReturnValueElement(int d, long v, double id) {
            date = d;
            value = v;
            txnId = id;
        }

        public int compareTo(@NotNull ReturnValueElement operand) {
            if(date!= operand.date) {
                return date - operand.date;
            } else {
                return (int) Math.round((txnId - operand.txnId) * 10.0);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ReturnValueElement)) return false;

            ReturnValueElement that = (ReturnValueElement) o;

            if (date != that.date) return false;
            if (Double.compare(that.txnId, txnId) != 0) return false;
            if (value != that.value) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result;
            long temp;
            result = date;
            result = 31 * result + (int) (value ^ (value >>> 32));
            temp = Double.doubleToLongBits(txnId);
            result = 31 * result + (int) (temp ^ (temp >>> 32));
            return result;
        }

        public ReturnValueElement clone(){
            return new ReturnValueElement(this.date, this.value, this.txnId);
        }

    }
}
