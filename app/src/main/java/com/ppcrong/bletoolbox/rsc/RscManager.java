package com.ppcrong.bletoolbox.rsc;

import java.util.UUID;

public class RscManager {

    private static final byte INSTANTANEOUS_STRIDE_LENGTH_PRESENT = 0x01; // 1 bit
    private static final byte TOTAL_DISTANCE_PRESENT = 0x02; // 1 bit
    private static final byte WALKING_OR_RUNNING_STATUS_BITS = 0x04; // 1 bit

    /**
     * Running Speed and Cadence Measurement service UUID
     */
    public static final UUID RUNNING_SPEED_AND_CADENCE_SERVICE_UUID = UUID.fromString("00001814-0000-1000-8000-00805f9b34fb");
    /**
     * Running Speed and Cadence Measurement characteristic UUID
     */
    public static final UUID RSC_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A53-0000-1000-8000-00805f9b34fb");
}
