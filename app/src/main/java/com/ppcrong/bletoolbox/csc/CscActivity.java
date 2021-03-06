package com.ppcrong.bletoolbox.csc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import com.polidea.rxandroidble2.helpers.ValueInterpreter;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.ppcrong.bletoolbox.csc.settings.CscSettingsFragment;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.ppcrong.bletoolbox.parser.CSCMeasurementParser;
import com.socks.library.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Locale;
import java.util.UUID;

import butterknife.BindView;

import static com.ppcrong.bletoolbox.csc.CscManager.CRANK_REVOLUTION_DATA_PRESENT;
import static com.ppcrong.bletoolbox.csc.CscManager.WHEEL_REVOLUTIONS_DATA_PRESENT;

public class CscActivity extends ProfileBaseActivity {

    // region [Variable]
    private int mFirstWheelRevolutions = -1;
    private int mLastWheelRevolutions = -1;
    private int mLastWheelEventTime = -1;
    private float mWheelCadence = -1;
    private int mLastCrankRevolutions = -1;
    private int mLastCrankEventTime = -1;
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
    @BindView(R.id.tv_ratio)
    TextView mTvRatio;
    // endregion [Widget]

    // region [Override Function]

    @Override
    protected void onResume() {
        super.onResume();
        setDefaultUI();
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_csc);

        // Print current setting values
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int circumference = Integer.parseInt(preferences.getString(CscSettingsFragment.SETTINGS_WHEEL_SIZE, String.valueOf(CscSettingsFragment.SETTINGS_WHEEL_SIZE_DEFAULT))); // [mm]
        final int unit = Integer.parseInt(preferences.getString(CscSettingsFragment.SETTINGS_UNIT, String.valueOf(CscSettingsFragment.SETTINGS_UNIT_DEFAULT)));
        KLog.i("circumference: " + circumference + ", unit: " + unit);
    }

    @Override
    protected void onPreWorkDone() {

        setupFilterCccNotification();
    }

    @Override
    protected void onFilterCccNotified(byte[] bytes) {
        super.onFilterCccNotified(bytes);

        KLog.i("\"" + CSCMeasurementParser.parse(getMustCccs2().FilterCcc) + "\" received");

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

            KLog.i("wheelRevolutions: " + wheelRevolutions + ", lastWheelEventTime: " + lastWheelEventTime);
            onWheelMeasurementReceived(wheelRevolutions, lastWheelEventTime);
        }

        if (crankRevPreset) {
            final int crankRevolutions = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset);
            offset += 2;

            final int lastCrankEventTime = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset);

            KLog.i("crankRevolutions: " + crankRevolutions + ", lastCrankEventTime: " + lastCrankEventTime);
            onCrankMeasurementReceived(crankRevolutions, lastCrankEventTime);
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

    @Override
    protected int getAboutTextId() {
        return R.string.csc_about_text;
    }

    @Override
    protected void setDefaultUI() {
        super.setDefaultUI();

        // Clear all values
        mTvCadence.setText(R.string.not_available_value);
        mTvSpeed.setText(R.string.not_available_value);
        mTvDistance.setText(R.string.not_available_value);
        mTvDistanceTotal.setText(R.string.not_available_value);
        mTvRatio.setText(R.string.not_available_value);

        setUnits();
    }

    // endregion [Override Function]

    // region [Private Function]
    private void onWheelMeasurementReceived(final int wheelRevolutions, final int lastWheelEventTime) {

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int circumference = Integer.parseInt(preferences.getString(CscSettingsFragment.SETTINGS_WHEEL_SIZE, String.valueOf(CscSettingsFragment.SETTINGS_WHEEL_SIZE_DEFAULT))); // [mm]

        if (mFirstWheelRevolutions < 0)
            mFirstWheelRevolutions = wheelRevolutions;

        if (mLastWheelEventTime == lastWheelEventTime)
            return;

        if (mLastWheelRevolutions >= 0) {
            float timeDifference;
            if (lastWheelEventTime < mLastWheelEventTime)
                timeDifference = (65535 + lastWheelEventTime - mLastWheelEventTime) / 1024.0f; // [s]
            else
                timeDifference = (lastWheelEventTime - mLastWheelEventTime) / 1024.0f; // [s]
            final float distanceDifference = (wheelRevolutions - mLastWheelRevolutions) * circumference / 1000.0f; // [m]
            final float totalDistance = (float) wheelRevolutions * (float) circumference / 1000.0f; // [m]
            final float distance = (float) (wheelRevolutions - mFirstWheelRevolutions) * (float) circumference / 1000.0f; // [m]
            final float speed = distanceDifference / timeDifference;

            // Update UI
            onMeasurementReceived(speed, distance, totalDistance);

            mWheelCadence = (wheelRevolutions - mLastWheelRevolutions) * 60.0f / timeDifference;
        }
        mLastWheelRevolutions = wheelRevolutions;
        mLastWheelEventTime = lastWheelEventTime;
    }

    private void onMeasurementReceived(float speed, float distance, float totalDistance) {

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int unit = Integer.parseInt(preferences.getString(CscSettingsFragment.SETTINGS_UNIT, String.valueOf(CscSettingsFragment.SETTINGS_UNIT_DEFAULT)));

        switch (unit) {
            case 1:
                speed = speed * 3.6f;
                // pass through intended
            case 0:
                if (distance < 1000) { // 1 km in m
                    mTvDistance.setText(String.format(Locale.US, "%.0f", distance));
                    mTvDistanceUnit.setText(R.string.csc_distance_unit_m);
                } else {
                    mTvDistance.setText(String.format(Locale.US, "%.2f", distance / 1000.0f));
                    mTvDistanceUnit.setText(R.string.csc_distance_unit_km);
                }

                mTvDistanceTotal.setText(String.format(Locale.US, "%.2f", totalDistance / 1000.0f));
                break;
            case 2:
                speed = speed * 2.2369f;
                if (distance < 1760) { // 1 mile in yrs
                    mTvDistance.setText(String.format(Locale.US, "%.0f", distance));
                    mTvDistanceUnit.setText(R.string.csc_distance_unit_yd);
                } else {
                    mTvDistance.setText(String.format(Locale.US, "%.2f", distance / 1760.0f));
                    mTvDistanceUnit.setText(R.string.csc_distance_unit_mile);
                }

                mTvDistanceTotal.setText(String.format(Locale.US, "%.2f", totalDistance / 1609.31f));
                break;
        }

        mTvSpeed.setText(String.format(Locale.US, "%.1f", speed));
    }

    public void onCrankMeasurementReceived(int crankRevolutions, int lastCrankEventTime) {

        if (mLastCrankEventTime == lastCrankEventTime)
            return;

        if (mLastCrankRevolutions >= 0) {
            float timeDifference;
            if (lastCrankEventTime < mLastCrankEventTime)
                timeDifference = (65535 + lastCrankEventTime - mLastCrankEventTime) / 1024.0f; // [s]
            else
                timeDifference = (lastCrankEventTime - mLastCrankEventTime) / 1024.0f; // [s]

            final float crankCadence = (crankRevolutions - mLastCrankRevolutions) * 60.0f / timeDifference;
            if (crankCadence > 0) {
                final float gearRatio = mWheelCadence / crankCadence;

                // Update UI
                onGearRatioUpdate(gearRatio, (int) crankCadence);
            }
        }
        mLastCrankRevolutions = crankRevolutions;
        mLastCrankEventTime = lastCrankEventTime;
    }

    private void onGearRatioUpdate(final float ratio, final int cadence) {
        mTvRatio.setText(String.format(Locale.US, "%.1f", ratio));
        mTvCadence.setText(String.format(Locale.US, "%d", cadence));
    }

    private void setUnits() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int unit = Integer.parseInt(preferences.getString(CscSettingsFragment.SETTINGS_UNIT, String.valueOf(CscSettingsFragment.SETTINGS_UNIT_DEFAULT)));

        switch (unit) {
            case CscSettingsFragment.SETTINGS_UNIT_M_S: // [m/s]
                mTvSpeedUnit.setText(R.string.csc_speed_unit_m_s);
                mTvDistanceUnit.setText(R.string.csc_distance_unit_m);
                mTvDistanceTotalUnit.setText(R.string.csc_total_distance_unit_km);
                break;
            case CscSettingsFragment.SETTINGS_UNIT_KM_H: // [km/h]
                mTvSpeedUnit.setText(R.string.csc_speed_unit_km_h);
                mTvDistanceUnit.setText(R.string.csc_distance_unit_m);
                mTvDistanceTotalUnit.setText(R.string.csc_total_distance_unit_km);
                break;
            case CscSettingsFragment.SETTINGS_UNIT_MPH: // [mph]
                mTvSpeedUnit.setText(R.string.csc_speed_unit_mph);
                mTvDistanceUnit.setText(R.string.csc_distance_unit_yd);
                mTvDistanceTotalUnit.setText(R.string.csc_total_distance_unit_mile);
                break;
        }
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
}
