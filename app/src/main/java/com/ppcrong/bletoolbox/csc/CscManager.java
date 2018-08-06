package com.ppcrong.bletoolbox.csc;

import java.util.UUID;

public class CscManager {

    public static final byte WHEEL_REVOLUTIONS_DATA_PRESENT = 0x01; // 1 bit
    public static final byte CRANK_REVOLUTION_DATA_PRESENT = 0x02; // 1 bit

    /**
     * Cycling Speed and Cadence service UUID
     */
    public static final UUID CYCLING_SPEED_AND_CADENCE_SERVICE_UUID = UUID.fromString("00001816-0000-1000-8000-00805f9b34fb");
    /**
     * Cycling Speed and Cadence Measurement characteristic UUID
     */
    public static final UUID CSC_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A5B-0000-1000-8000-00805f9b34fb");
}
