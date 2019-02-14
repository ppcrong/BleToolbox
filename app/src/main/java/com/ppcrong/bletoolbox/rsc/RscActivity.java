package com.ppcrong.bletoolbox.rsc;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.widget.TextView;

import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.ppcrong.bletoolbox.rsc.settings.RscSettingsFragment;
import com.socks.library.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.UUID;

import butterknife.BindView;

public class RscActivity extends ProfileBaseActivity {

    // region [Variable]
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
