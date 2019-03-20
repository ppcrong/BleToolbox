/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ppcrong.bletoolbox.gls;

import java.util.UUID;

public class GlucoseManager {

    /**
     * Glucose service UUID
     */
    public final static UUID GLS_SERVICE_UUID = UUID.fromString("00001808-0000-1000-8000-00805f9b34fb");
    /**
     * Glucose Measurement characteristic UUID
     */
    public final static UUID GM_CHARACTERISTIC = UUID.fromString("00002A18-0000-1000-8000-00805f9b34fb");
    /**
     * Glucose Measurement Context characteristic UUID
     */
    public final static UUID GM_CONTEXT_CHARACTERISTIC = UUID.fromString("00002A34-0000-1000-8000-00805f9b34fb");
    /**
     * Glucose Feature characteristic UUID
     */
    public final static UUID GF_CHARACTERISTIC = UUID.fromString("00002A51-0000-1000-8000-00805f9b34fb");
    /**
     * Record Access Control Point characteristic UUID
     */
    public final static UUID RACP_CHARACTERISTIC = UUID.fromString("00002A52-0000-1000-8000-00805f9b34fb");

    public final static int OP_CODE_REPORT_STORED_RECORDS = 1;
    public final static int OP_CODE_DELETE_STORED_RECORDS = 2;
    public final static int OP_CODE_ABORT_OPERATION = 3;
    public final static int OP_CODE_REPORT_NUMBER_OF_RECORDS = 4;
    public final static int OP_CODE_NUMBER_OF_STORED_RECORDS_RESPONSE = 5;
    public final static int OP_CODE_RESPONSE_CODE = 6;

    public final static int OPERATOR_NULL = 0;
    public final static int OPERATOR_ALL_RECORDS = 1;
    public final static int OPERATOR_LESS_THEN_OR_EQUAL = 2;
    public final static int OPERATOR_GREATER_THEN_OR_EQUAL = 3;
    public final static int OPERATOR_WITHING_RANGE = 4;
    public final static int OPERATOR_FIRST_RECORD = 5;
    public final static int OPERATOR_LAST_RECORD = 6;

    /**
     * The filter type is used for range operators ({@link #OPERATOR_LESS_THEN_OR_EQUAL},
     * {@link #OPERATOR_GREATER_THEN_OR_EQUAL}, {@link #OPERATOR_WITHING_RANGE}.<br/>
     * The syntax of the operand is: [Filter Type][Minimum][Maximum].<br/>
     * This filter selects the records by the sequence number.
     */
    public final static int FILTER_TYPE_SEQUENCE_NUMBER = 1;
    /**
     * The filter type is used for range operators ({@link #OPERATOR_LESS_THEN_OR_EQUAL},
     * {@link #OPERATOR_GREATER_THEN_OR_EQUAL}, {@link #OPERATOR_WITHING_RANGE}.<br/>
     * The syntax of the operand is: [Filter Type][Minimum][Maximum].<br/>
     * This filter selects the records by the user facing time (base time + offset time).
     */
    public final static int FILTER_TYPE_USER_FACING_TIME = 2;

    public final static int RESPONSE_SUCCESS = 1;
    public final static int RESPONSE_OP_CODE_NOT_SUPPORTED = 2;
    public final static int RESPONSE_INVALID_OPERATOR = 3;
    public final static int RESPONSE_OPERATOR_NOT_SUPPORTED = 4;
    public final static int RESPONSE_INVALID_OPERAND = 5;
    public final static int RESPONSE_NO_RECORDS_FOUND = 6;
    public final static int RESPONSE_ABORT_UNSUCCESSFUL = 7;
    public final static int RESPONSE_PROCEDURE_NOT_COMPLETED = 8;
    public final static int RESPONSE_OPERAND_NOT_SUPPORTED = 9;
}
