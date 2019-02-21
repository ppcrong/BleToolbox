package com.ppcrong.bletoolbox.bpm;

import android.os.Bundle;
import android.widget.TextView;

import com.polidea.rxandroidble2.helpers.ValueInterpreter;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.socks.library.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;
import java.util.UUID;

import butterknife.BindView;
import io.reactivex.Observable;

public class BpmActivity extends ProfileBaseActivity {

    // region [Widget]
    @BindView(R.id.tv_systolic)
    TextView mTvSystolic;
    @BindView(R.id.tv_systolic_unit)
    TextView mTvSystolicUnit;
    @BindView(R.id.tv_diastolic)
    TextView mTvDiastolic;
    @BindView(R.id.tv_diastolic_unit)
    TextView mTvDiastolicUnit;
    @BindView(R.id.tv_mean_ap)
    TextView mTvMeanAp;
    @BindView(R.id.tv_mean_ap_unit)
    TextView mTvMeanApUnit;
    @BindView(R.id.tv_pulse)
    TextView mTvPulse;
    @BindView(R.id.tv_timestamp)
    TextView mTvTimestamp;
    // endregion [Widget]

    // region [Override Function]

    @Override
    protected void onResume() {
        super.onResume();
        setDefaultUI();
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_bpm);
    }

    @Override
    protected void onPreWorkDone() {

        // Enable ICP notification, then enable BPM indication
        setupCccNotification(getFilterCccUUID(), this::onIcpNotificationSetupDone);
    }

    private void onIcpNotificationSetupDone(Observable<byte[]> observable) {

        // Enable BPM indication
        setupCccIndication(getFilterCccUUID2());
    }

    @Override
    protected void onFilterCccNotified(byte[] bytes) {
        super.onFilterCccNotified(bytes);
        parseBPMValue(getFilterCccUUID(), bytes);
    }

    @Override
    protected void onFilterCccIndicated(byte[] bytes) {
        super.onFilterCccIndicated(bytes);
        parseBPMValue(getFilterCccUUID2(), bytes);
    }

    @Override
    protected UUID getFilterSvcUUID() {
        return BpmManager.BP_SERVICE_UUID;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return BpmManager.ICP_CHARACTERISTIC_UUID;
    }

    @Override
    protected UUID getFilterCccUUID2() {
        return BpmManager.BPM_CHARACTERISTIC_UUID;
    }

    @Override
    protected int getAboutTextId() {
        return R.string.bpm_about_text;
    }

    @Override
    protected void setDefaultUI() {
        super.setDefaultUI();

        // Clear all values
        mTvSystolic.setText(R.string.not_available_value);
        mTvSystolicUnit.setText(null);
        mTvDiastolic.setText(R.string.not_available_value);
        mTvDiastolicUnit.setText(null);
        mTvMeanAp.setText(R.string.not_available_value);
        mTvMeanApUnit.setText(null);
        mTvPulse.setText(R.string.not_available_value);
        mTvTimestamp.setText(R.string.not_available);
    }

    // endregion [Override Function]

    // region [Private Function]
    private void parseBPMValue(final UUID uuid, final byte[] bytes) {
        // Both BPM and ICP have the same structure.

        // first byte - flags
        int offset = 0;
        final int flags = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset++);
        // See BPMManagerCallbacks.UNIT_* for unit options
        final int unit = flags & 0x01;
        final boolean timestampPresent = (flags & 0x02) > 0;
        final boolean pulseRatePresent = (flags & 0x04) > 0;

        if (BpmManager.BPM_CHARACTERISTIC_UUID.equals(uuid)) {
            // following bytes - systolic, diastolic and mean arterial pressure
            final float systolic = ValueInterpreter.getFloatValue(bytes, ValueInterpreter.FORMAT_SFLOAT, offset);
            final float diastolic = ValueInterpreter.getFloatValue(bytes, ValueInterpreter.FORMAT_SFLOAT, offset + 2);
            final float meanArterialPressure = ValueInterpreter.getFloatValue(bytes, ValueInterpreter.FORMAT_SFLOAT, offset + 4);
            offset += 6;
            onBloodPressureMeasurementRead(systolic, diastolic, meanArterialPressure, unit);
        } else if (BpmManager.ICP_CHARACTERISTIC_UUID.equals(uuid)) {
            // following bytes - cuff pressure. Diastolic and MAP are unused
            final float cuffPressure = ValueInterpreter.getFloatValue(bytes, ValueInterpreter.FORMAT_SFLOAT, offset);
            offset += 6;
            onIntermediateCuffPressureRead(cuffPressure, unit);
        }

        // parse timestamp if present
        if (timestampPresent) {
            final Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.YEAR, ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset));
            calendar.set(Calendar.MONTH, ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 2) - 1); // months are 1-based
            calendar.set(Calendar.DAY_OF_MONTH, ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 3));
            calendar.set(Calendar.HOUR_OF_DAY, ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 4));
            calendar.set(Calendar.MINUTE, ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 5));
            calendar.set(Calendar.SECOND, ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 6));
            offset += 7;
            onTimestampRead(calendar);
        } else
            onTimestampRead(null);

        // parse pulse rate if present
        if (pulseRatePresent) {
            final float pulseRate = ValueInterpreter.getFloatValue(bytes, ValueInterpreter.FORMAT_SFLOAT, offset);
            // offset += 2;
            onPulseRateRead(pulseRate);
        } else {
            onPulseRateRead(-1.0f);
        }
    }

    /**
     * Called when new BPM value has been obtained from the sensor
     *
     * @param systolic
     * @param diastolic
     * @param meanArterialPressure
     * @param unit                 one of the following {@link BpmManager#UNIT_kPa} or {@link BpmManager#UNIT_mmHG}
     */
    void onBloodPressureMeasurementRead(final float systolic, final float diastolic, final float meanArterialPressure, final int unit) {

        runOnUiThread(() -> {
            mTvSystolic.setText(String.valueOf(systolic));
            mTvDiastolic.setText(String.valueOf(diastolic));
            mTvMeanAp.setText(String.valueOf(meanArterialPressure));

            mTvSystolicUnit.setText(unit == BpmManager.UNIT_mmHG ? R.string.bpm_unit_mmhg : R.string.bpm_unit_kpa);
            mTvDiastolicUnit.setText(unit == BpmManager.UNIT_mmHG ? R.string.bpm_unit_mmhg : R.string.bpm_unit_kpa);
            mTvMeanApUnit.setText(unit == BpmManager.UNIT_mmHG ? R.string.bpm_unit_mmhg : R.string.bpm_unit_kpa);
        });
    }

    /**
     * Called when new ICP value has been obtained from the device
     *
     * @param cuffPressure
     * @param unit         one of the following {@link BpmManager#UNIT_kPa} or {@link BpmManager#UNIT_mmHG}
     */
    void onIntermediateCuffPressureRead(final float cuffPressure, final int unit) {

        runOnUiThread(() -> {
            mTvSystolic.setText(String.valueOf(cuffPressure));
            mTvDiastolic.setText(R.string.not_available_value);
            mTvMeanAp.setText(R.string.not_available_value);

            mTvSystolicUnit.setText(unit == BpmManager.UNIT_mmHG ? R.string.bpm_unit_mmhg : R.string.bpm_unit_kpa);
            mTvDiastolicUnit.setText(null);
            mTvMeanApUnit.setText(null);
        });
    }

    /**
     * Called when new pulse rate value has been obtained from the device. If there was no pulse rate in the packet the parameter will be equal -1.0f
     *
     * @param pulseRate pulse rate or -1.0f
     */
    void onPulseRateRead(final float pulseRate) {

        runOnUiThread(() -> {
            if (pulseRate >= 0)
                mTvPulse.setText(String.valueOf(pulseRate));
            else
                mTvPulse.setText(R.string.not_available_value);
        });
    }

    /**
     * Called when the timestamp value has been read from the device. If there was no timestamp information the parameter will be <code>null</code>
     *
     * @param calendar the timestamp or <code>null</code>
     */
    void onTimestampRead(final Calendar calendar) {

        runOnUiThread(() -> {
            if (calendar != null)
                mTvTimestamp.setText(getString(R.string.bpm_timestamp, calendar));
            else
                mTvTimestamp.setText(R.string.not_available);
        });
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
