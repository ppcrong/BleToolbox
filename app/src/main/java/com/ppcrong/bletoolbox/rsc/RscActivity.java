package com.ppcrong.bletoolbox.rsc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.widget.TextView;

import com.polidea.rxandroidble2.helpers.ValueInterpreter;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.ppcrong.bletoolbox.parser.RSCMeasurementParser;
import com.ppcrong.bletoolbox.rsc.settings.RscSettingsFragment;
import com.socks.library.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Locale;
import java.util.UUID;

import butterknife.BindView;

import static com.ppcrong.bletoolbox.rsc.RscManager.ACTIVITY_RUNNING;
import static com.ppcrong.bletoolbox.rsc.RscManager.ACTIVITY_WALKING;
import static com.ppcrong.bletoolbox.rsc.RscManager.INSTANTANEOUS_STRIDE_LENGTH_PRESENT;
import static com.ppcrong.bletoolbox.rsc.RscManager.NOT_AVAILABLE;
import static com.ppcrong.bletoolbox.rsc.RscManager.TOTAL_DISTANCE_PRESENT;
import static com.ppcrong.bletoolbox.rsc.RscManager.WALKING_OR_RUNNING_STATUS_BITS;

public class RscActivity extends ProfileBaseActivity {

    // region [Variable]
    /**
     * The last value of a cadence
     */
    private float mCadence;
    /**
     * Trip distance in cm
     */
    private float mDistance;
    /**
     * Stride length in cm
     */
    private float mStrideLength;
    /**
     * Number of steps in the trip
     */
    private int mStepsNumber;
    private boolean mTaskInProgress;
    private final Handler mHandler = new Handler();
    // endregion [Variable]

    // region [Widget]
    @BindView(R.id.tv_speed)
    TextView mTvSpeed;
    @BindView(R.id.tv_speed_unit)
    TextView mTvSpeedUnit;
    @BindView(R.id.tv_cadence)
    TextView mTvCadence;
    @BindView(R.id.tv_distance)
    TextView mTvDistance;
    @BindView(R.id.tv_distance_unit)
    TextView mTvDistanceUnit;
    @BindView(R.id.tv_distance_total)
    TextView mTvDistanceTotal;
    @BindView(R.id.tv_distance_total_unit)
    TextView mTvDistanceTotalUnit;
    @BindView(R.id.tv_steps)
    TextView mTvSteps;
    @BindView(R.id.tv_activity)
    TextView mTvActivity;
    // endregion [Widget]

    // region [Override Function]

    @Override
    protected void onResume() {
        super.onResume();
        setDefaultUI();
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_rsc);

    }

    @Override
    protected void onPreWorkDone() {

        setupFilterCccNotification();
    }

    @Override
    protected void onFilterCccNotified(byte[] bytes) {
        super.onFilterCccNotified(bytes);

        KLog.i("\"" + RSCMeasurementParser.parse(getMustCccs2().FilterCcc) + "\" received");

        // Decode the new data
        int offset = 0;
        final int flags = bytes[offset]; // 1 byte
        offset += 1;

        final boolean islmPresent = (flags & INSTANTANEOUS_STRIDE_LENGTH_PRESENT) > 0;
        final boolean tdPreset = (flags & TOTAL_DISTANCE_PRESENT) > 0;
        final boolean running = (flags & WALKING_OR_RUNNING_STATUS_BITS) > 0;

        final float instantaneousSpeed = (float) ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset) / 256.0f; // 1/256 m/s in [m/s]
        offset += 2;

        final int instantaneousCadence = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset); // [SPM]
        offset += 1;

        float instantaneousStrideLength = NOT_AVAILABLE;
        if (islmPresent) {
            instantaneousStrideLength = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset); // [cm]
            offset += 2;
        }

        float totalDistance = NOT_AVAILABLE;
        if (tdPreset) {
            totalDistance = (float) ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT32, offset) / 10.0f; // 1/10 m in [m]
            //offset += 4;
        }

        // Notify about the new measurement
        onMeasurementReceived(instantaneousSpeed, instantaneousCadence, totalDistance, instantaneousStrideLength,
                running ? ACTIVITY_RUNNING : ACTIVITY_WALKING);
    }

    @Override
    protected UUID getFilterSvcUUID() {
        return RscManager.RUNNING_SPEED_AND_CADENCE_SERVICE_UUID;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return RscManager.RSC_MEASUREMENT_CHARACTERISTIC_UUID;
    }

    @Override
    protected int getAboutTextId() {
        return R.string.rsc_about_text;
    }

    @Override
    protected void setDefaultUI() {
        super.setDefaultUI();

        // Clear all values
        mTvCadence.setText(R.string.not_available_value);
        mTvSpeed.setText(R.string.not_available_value);
        mTvDistance.setText(R.string.not_available_value);
        mTvDistanceTotal.setText(R.string.not_available_value);
        mTvSteps.setText(R.string.not_available_value);
        mTvActivity.setText(R.string.not_available);

        setUnits();
    }

    // endregion [Override Function]

    // region [Private Function]

    private void setUnits() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int unit = Integer.parseInt(preferences.getString(RscSettingsFragment.SETTINGS_UNIT, String.valueOf(RscSettingsFragment.SETTINGS_UNIT_DEFAULT)));

        switch (unit) {
            case RscSettingsFragment.SETTINGS_UNIT_M_S: // [m/s]
                mTvSpeedUnit.setText(R.string.csc_speed_unit_m_s);
                mTvDistanceUnit.setText(R.string.csc_distance_unit_m);
                mTvDistanceTotalUnit.setText(R.string.csc_total_distance_unit_km);
                break;
            case RscSettingsFragment.SETTINGS_UNIT_KM_H: // [km/h]
                mTvSpeedUnit.setText(R.string.csc_speed_unit_km_h);
                mTvDistanceUnit.setText(R.string.csc_distance_unit_m);
                mTvDistanceTotalUnit.setText(R.string.csc_total_distance_unit_km);
                break;
            case RscSettingsFragment.SETTINGS_UNIT_MPH: // [mph]
                mTvSpeedUnit.setText(R.string.csc_speed_unit_mph);
                mTvDistanceUnit.setText(R.string.csc_distance_unit_yd);
                mTvDistanceTotalUnit.setText(R.string.csc_total_distance_unit_mile);
                break;
        }
    }

    public void onMeasurementReceived(float speed, int cadence, float totalDistance, float strideLen, int activity) {

        // Start strides counter if not in progress
        mCadence = cadence;
        mStrideLength = strideLen;
        if (!mTaskInProgress && cadence > 0) {
            mTaskInProgress = true;

            final long interval = (long) (1000.0f * 65.0f / mCadence); // 60s + 5s for calibration in milliseconds
            mHandler.postDelayed(mUpdateStridesTask, interval);
        }

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int unit = Integer.parseInt(preferences.getString(RscSettingsFragment.SETTINGS_UNIT, String.valueOf(RscSettingsFragment.SETTINGS_UNIT_DEFAULT)));

        switch (unit) {
            case RscSettingsFragment.SETTINGS_UNIT_KM_H:
                speed = speed * 3.6f;
                // pass through intended
            case RscSettingsFragment.SETTINGS_UNIT_M_S:
                if (totalDistance == NOT_AVAILABLE) {
                    mTvDistanceTotal.setText(R.string.not_available);
                    mTvDistanceTotalUnit.setText(null);
                } else {
                    mTvDistanceTotal.setText(String.format(Locale.US, "%.2f", totalDistance / 1000.0f)); // 1 km in m
                    mTvDistanceTotalUnit.setText(R.string.rsc_total_distance_unit_km);
                }
                break;
            case RscSettingsFragment.SETTINGS_UNIT_MPH:
                speed = speed * 2.2369f;
                if (totalDistance == NOT_AVAILABLE) {
                    mTvDistanceTotal.setText(R.string.not_available);
                    mTvDistanceTotalUnit.setText(null);
                } else {
                    mTvDistanceTotal.setText(String.format(Locale.US, "%.2f", totalDistance / 1609.31f)); // 1 mile in m
                    mTvDistanceTotalUnit.setText(R.string.rsc_total_distance_unit_mile);
                }
                break;
        }

        mTvSpeed.setText(String.format(Locale.US, "%.1f", speed));
        mTvCadence.setText(String.format(Locale.US, "%d", cadence));
        mTvActivity.setText(activity == ACTIVITY_RUNNING ? R.string.rsc_running : R.string.rsc_walking);
    }
    // endregion [Private Function]

    // region [EventBus]
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBleConnectionStateChange(BleEvents.BleConnectionState event) {

        KLog.i(event);

        switch (event.getState()) {
            case CONNECTING:
                break;
            case CONNECTED:
                break;
            case DISCONNECTED:
                break;
            case DISCONNECTING:
                break;
            default:
                break;
        }
    }
    // endregion [EventBus]

    // region [Task]
    private final Runnable mUpdateStridesTask = new Runnable() {
        @Override
        public void run() {
            if (!isConnected())
                return;

            mStepsNumber++;
            mDistance += mStrideLength;
            onStridesUpdate(mDistance, mStepsNumber);

            if (mCadence > 0) {
                final long interval = (long) (1000.0f * 65.0f / mCadence); // 60s + 5s for calibration in milliseconds
                mHandler.postDelayed(mUpdateStridesTask, interval);
            } else {
                mTaskInProgress = false;
            }
        }
    };

    private void onStridesUpdate(final float distance, final int strides) {
        if (distance == NOT_AVAILABLE) {
            mTvDistance.setText(R.string.not_available);
            mTvDistanceUnit.setText(R.string.rsc_distance_unit_m);
        } else {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            final int unit = Integer.parseInt(preferences.getString(RscSettingsFragment.SETTINGS_UNIT, String.valueOf(RscSettingsFragment.SETTINGS_UNIT_DEFAULT)));

            switch (unit) {
                case RscSettingsFragment.SETTINGS_UNIT_KM_H:
                case RscSettingsFragment.SETTINGS_UNIT_M_S:
                    if (distance < 100000) { // 1 km in cm
                        mTvDistance.setText(String.format(Locale.US, "%.0f", distance / 100.0f));
                        mTvDistanceUnit.setText(R.string.rsc_distance_unit_m);
                    } else {
                        mTvDistance.setText(String.format(Locale.US, "%.2f", distance / 100000.0f));
                        mTvDistanceUnit.setText(R.string.rsc_distance_unit_km);
                    }
                    break;
                case RscSettingsFragment.SETTINGS_UNIT_MPH:
                    if (distance < 160931) { // 1 mile in cm
                        mTvDistance.setText(String.format(Locale.US, "%.0f", distance / 91.4392f));
                        mTvDistanceUnit.setText(R.string.rsc_distance_unit_yd);
                    } else {
                        mTvDistance.setText(String.format(Locale.US, "%.2f", distance / 160931.23f));
                        mTvDistanceUnit.setText(R.string.rsc_distance_unit_mile);
                    }
                    break;
            }
        }

        mTvSteps.setText(String.valueOf(strides));
    }
    // endregion [Task]
}
