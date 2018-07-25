package com.ppcrong.bletoolbox.csc;

import android.os.Bundle;

import com.polidea.rxandroidble2.helpers.ValueInterpreter;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.socks.library.KLog;

import java.util.UUID;

import butterknife.ButterKnife;

import static com.ppcrong.bletoolbox.csc.CscManager.CRANK_REVOLUTION_DATA_PRESENT;
import static com.ppcrong.bletoolbox.csc.CscManager.WHEEL_REVOLUTIONS_DATA_PRESENT;

public class CscActivity extends ProfileBaseActivity {

    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_csc);

        // Bind ButterKnife
        ButterKnife.bind(this);
    }

    @Override
    protected void onPreWorkDone() {

        setupFilterCccNotification();
    }

    @Override
    protected void onFilterCccNotified(byte[] bytes) {
        super.onFilterCccNotified(bytes);

        // Decode the new data
        int offset = 0;
        final int flags = bytes[offset]; // 1 byte
        offset += 1;

        final boolean wheelRevPresent = (flags & WHEEL_REVOLUTIONS_DATA_PRESENT) > 0;
        final boolean crankRevPreset = (flags & CRANK_REVOLUTION_DATA_PRESENT) > 0;

        if (wheelRevPresent) {
            final int wheelRevolutions = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT32, offset);
            offset += 4;

            final int lastWheelEventTime = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset); // 1/1024 s
            offset += 2;

            KLog.i("wheelRevolutions: " + wheelRevolutions + ", lastWheelEventTime: " + lastWheelEventTime);
        }

        if (crankRevPreset) {
            final int crankRevolutions = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset);
            offset += 2;

            final int lastCrankEventTime = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset);
            // offset += 2;

            KLog.i("crankRevolutions: " + crankRevolutions + ", lastCrankEventTime: " + lastCrankEventTime);
        }
    }

    @Override
    protected UUID getFilterSvcUUID() {
        return CscManager.CYCLING_SPEED_AND_CADENCE_SERVICE_UUID;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return CscManager.CSC_MEASUREMENT_CHARACTERISTIC_UUID;
    }
}
