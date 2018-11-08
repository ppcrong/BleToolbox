package com.ppcrong.bletoolbox.hrm;

import android.os.Bundle;

import com.polidea.rxandroidble2.helpers.ValueInterpreter;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;

import java.util.UUID;

import butterknife.ButterKnife;

public class HrmActivity extends ProfileBaseActivity {

    // region [Variable]
    // endregion [Variable]

    // region [Widget]
//    @BindView(R.id.tv_speed)
//    TextView mTvSpeed;
    // endregion [Widget]

    // region [Override Function]
    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_hrm);

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

        int hrValue;
        if (isHeartRateInUINT16(bytes[0])) {
            hrValue = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, 1);
        } else {
            hrValue = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, 1);
        }

        // New HR value is received from HR device
        onHRValueReceived(hrValue);
    }

    @Override
    protected UUID getFilterSvcUUID() {
        return HrmManager.HR_SERVICE_UUID;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return HrmManager.HR_CHARACTERISTIC_UUID;
    }
    // endregion [Override Function]

    // region [Private Function]
    public void onHRValueReceived(int value) {
//        mHrmValue = value;
//        setHRSValueOnView(mHrmValue);
    }

    /**
     * This method will decode and return Heart rate sensor position on body
     */
    private String getBodySensorPosition(final byte bodySensorPositionValue) {
        final String[] locations = getResources().getStringArray(R.array.hrm_locations);
        if (bodySensorPositionValue > locations.length)
            return getString(R.string.hrm_location_other);
        return locations[bodySensorPositionValue];
    }

    /**
     * This method will check if Heart rate value is in 8 bits or 16 bits
     */
    private boolean isHeartRateInUINT16(final byte value) {
        return ((value & 0x01) != 0);
    }
    // endregion [Private Function]
}
