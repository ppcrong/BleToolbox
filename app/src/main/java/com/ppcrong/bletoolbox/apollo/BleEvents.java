package com.ppcrong.bletoolbox.apollo;

import com.polidea.rxandroidble2.RxBleConnection;

/**
 * The BLE events for notify
 */

public class BleEvents {

    public static class NotifyBleConnectionStateEvent {

        private RxBleConnection.RxBleConnectionState state;

        // Apollo MUST: no-arg ctr
        public NotifyBleConnectionStateEvent() {
        }

        public NotifyBleConnectionStateEvent(RxBleConnection.RxBleConnectionState state) {
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
