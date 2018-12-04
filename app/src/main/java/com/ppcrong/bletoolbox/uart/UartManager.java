package com.ppcrong.bletoolbox.uart;

import java.util.UUID;

public class UartManager {
    /**
     * Nordic UART Service UUID
     */
    public final static UUID UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E");
    /**
     * RX characteristic UUID
     */
    public final static UUID UART_RX_CHARACTERISTIC_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E");
    /**
     * TX characteristic UUID
     */
    public final static UUID UART_TX_CHARACTERISTIC_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E");
    /**
     * The maximum packet size is 20 bytes.
     */
    public static final int MAX_PACKET_SIZE = 20;
}
