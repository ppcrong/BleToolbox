package com.ppcrong.bletoolbox.htm;

import java.util.UUID;

public class HtmManager {
    /**
     * Health Thermometer service UUID
     */
    public final static UUID HT_SERVICE_UUID = UUID.fromString("00001809-0000-1000-8000-00805f9b34fb");
    /**
     * Health Thermometer Measurement characteristic UUID
     */
    public static final UUID HT_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb");

    public final static int HIDE_MSB_8BITS_OUT_OF_32BITS = 0x00FFFFFF;
    public final static int HIDE_MSB_8BITS_OUT_OF_16BITS = 0x00FF;
    public final static int SHIFT_LEFT_8BITS = 8;
    public final static int SHIFT_LEFT_16BITS = 16;
    public final static int GET_BIT24 = 0x00400000;
    public final static int FIRST_BIT_MASK = 0x01;
}
