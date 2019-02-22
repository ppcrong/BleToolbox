package com.ppcrong.bletoolbox.htm;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.TextView;

import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.ppcrong.bletoolbox.htm.settings.HtmSettingsFragment;
import com.socks.library.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.DecimalFormat;
import java.util.UUID;

import butterknife.BindView;

public class HtmActivity extends ProfileBaseActivity {

    // region [Variable]
    private static final DecimalFormat mFormattedTemp = new DecimalFormat("#0.00");
    private Double mValueC;
    // endregion [Variable]

    // region [Widget]
    @BindView(R.id.tv_hts_value)
    TextView mTvHtsValue;
    @BindView(R.id.tv_hts_unit)
    TextView mTvHtsUnit;
    // endregion [Widget]

    // region [Override Function]

    @Override
    protected void onResume() {
        super.onResume();
        setUnits();
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_htm);
    }

    @Override
    protected void onPreWorkDone() {

        // Enable HTM indication
        setupCccIndication(getFilterCccUUID());
    }

    @Override
    protected void onFilterCccIndicated(byte[] bytes) {
        super.onFilterCccIndicated(bytes);

        try {
            final double tempValue = decodeTemperature(bytes);
            onHTValueReceived(tempValue);
        } catch (Exception e) {
            KLog.e("Invalid temperature value\n" + Log.getStackTraceString(e));
        }
    }

    @Override
    protected UUID getFilterSvcUUID() {
        return HtmManager.HT_SERVICE_UUID;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return HtmManager.HT_MEASUREMENT_CHARACTERISTIC_UUID;
    }

    @Override
    protected int getAboutTextId() {
        return R.string.hts_about_text;
    }

    @Override
    protected void setDefaultUI() {
        super.setDefaultUI();

        mValueC = null;
        mTvHtsValue.setText(R.string.not_available_value);

        setUnits();
    }

    // endregion [Override Function]

    // region [Private Function]

    /**
     * Called when Health Thermometer value has been received
     *
     * @param value the new value
     */
    void onHTValueReceived(double value) {

        setHTSValueOnView(value);
    }

    private void setUnits() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int unit = Integer.parseInt(preferences.getString(HtmSettingsFragment.SETTINGS_UNIT, String.valueOf(HtmSettingsFragment.SETTINGS_UNIT_DEFAULT)));

        switch (unit) {
            case HtmSettingsFragment.SETTINGS_UNIT_C:
                mTvHtsUnit.setText(R.string.hts_unit_celsius);
                break;
            case HtmSettingsFragment.SETTINGS_UNIT_F:
                mTvHtsUnit.setText(R.string.hts_unit_fahrenheit);
                break;
            case HtmSettingsFragment.SETTINGS_UNIT_K:
                mTvHtsUnit.setText(R.string.hts_unit_kelvin);
                break;
        }
        if (mValueC != null)
            setHTSValueOnView(mValueC);
    }

    private void setHTSValueOnView(double value) {
        mValueC = value;
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        final int unit = Integer.parseInt(preferences.getString(HtmSettingsFragment.SETTINGS_UNIT, String.valueOf(HtmSettingsFragment.SETTINGS_UNIT_DEFAULT)));

        switch (unit) {
            case HtmSettingsFragment.SETTINGS_UNIT_F:
                value = value * 1.8 + 32;
                break;
            case HtmSettingsFragment.SETTINGS_UNIT_K:
                value += 273.15;
                break;
            case HtmSettingsFragment.SETTINGS_UNIT_C:
                break;
        }
        mTvHtsValue.setText(mFormattedTemp.format(value));
    }

    /**
     * This method decode temperature value received from Health Thermometer device First byte {0} of data is flag and first bit of flag shows unit information of temperature. if bit 0 has value 1
     * then unit is Fahrenheit and Celsius otherwise Four bytes {1 to 4} after Flag bytes represent the temperature value in IEEE-11073 32-bit Float format
     */
    private double decodeTemperature(byte[] data) throws Exception {
        double temperatureValue;
        byte flag = data[0];
        byte exponential = data[4];
        short firstOctet = convertNegativeByteToPositiveShort(data[1]);
        short secondOctet = convertNegativeByteToPositiveShort(data[2]);
        short thirdOctet = convertNegativeByteToPositiveShort(data[3]);
        int mantissa = ((thirdOctet << HtmManager.SHIFT_LEFT_16BITS) |
                (secondOctet << HtmManager.SHIFT_LEFT_8BITS) |
                (firstOctet)) & HtmManager.HIDE_MSB_8BITS_OUT_OF_32BITS;
        mantissa = getTwosComplimentOfNegativeMantissa(mantissa);
        temperatureValue = (mantissa * Math.pow(10, exponential));

        /*
         * Conversion of temperature unit from Fahrenheit to Celsius if unit is in Fahrenheit
         * Celsius = (Fahrenheit -32) 5/9
         */
        if ((flag & HtmManager.FIRST_BIT_MASK) != 0) {
            temperatureValue = (float) ((temperatureValue - 32) * (5 / 9.0));
        }
        return temperatureValue;
    }

    private short convertNegativeByteToPositiveShort(byte octet) {
        if (octet < 0) {
            return (short) (octet & HtmManager.HIDE_MSB_8BITS_OUT_OF_16BITS);
        } else {
            return octet;
        }
    }

    private int getTwosComplimentOfNegativeMantissa(int mantissa) {
        if ((mantissa & HtmManager.GET_BIT24) != 0) {
            return ((((~mantissa) & HtmManager.HIDE_MSB_8BITS_OUT_OF_32BITS) + 1) * (-1));
        } else {
            return mantissa;
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
