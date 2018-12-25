package com.ppcrong.bletoolbox.eventbus;

import com.polidea.rxandroidble2.RxBleConnection;

/**
 * The BLE events for notify
 */

public class BleEvents {

    public static class BleConnectionState {

        private RxBleConnection.RxBleConnectionState state;

        public BleConnectionState(RxBleConnection.RxBleConnectionState state) {
            this.state = state;
        }

        @Override
        public String toString() {
            return state.toString();
        }

        public RxBleConnection.RxBleConnectionState getState() {
            return state;
        }

        public void setState(RxBleConnection.RxBleConnectionState state) {
            this.state = state;
        }
    }
}
