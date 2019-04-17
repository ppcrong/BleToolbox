package com.ppcrong.bletoolbox.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;

import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.ppcrong.bletoolbox.ccps.CcpsManager;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.ppcrong.utils.MiscUtils;
import com.socks.library.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.UUID;

import butterknife.BindView;
import butterknife.OnCheckedChanged;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class BleActivity extends ProfileBaseActivity {

    // region [Variable]
    // endregion [Variable]

    // region [Widget]
    @BindView(R.id.switch_repeat_send)
    Switch mSwitchRepeatSend;
    @BindView(R.id.tv_repeat_times)
    TextView mTvRepeatTimes;
    // endregion [Widget]

    // region [OnCheckChanged]
    @OnCheckedChanged(R.id.switch_repeat_send)
    public void OnCheckedChangedRepeatSend(Switch view, boolean isChecked) {

        KLog.i("isChecked: " + isChecked);
        if (isChecked) {

            if (false == isConnected()) {

                showToast(R.string.ble_repeat_send_remind_connect);
                mSwitchRepeatSend.setChecked(false);
            } else {

                // Repeat sending data
                repeatSendData().subscribe(this::onSendDataNext, this::onSendDataException, this::onSendDataComplete);
            }
        }
    }

    private void onSendDataNext(Integer integer) {

        mTvRepeatTimes.setText(integer.toString());
    }

    private void onSendDataException(Throwable throwable) {

        KLog.i(throwable.toString());
    }

    private void onSendDataComplete() {

        KLog.i("Stop repeat send data");
    }
    // endregion [OnCheckChanged]

    // region [Override Function]

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_ble);

        // Keep screen always on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        MiscUtils.initWakeLock(this);
        MiscUtils.acquireWakeLock();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Disable screen always on
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        MiscUtils.releaseWakeLock();
    }

    @Override
    protected void onSvcDiscovered(RxBleDeviceServices services) {
        super.onSvcDiscovered(services);

    }

    @Override
    protected void onMustCccGet(BluetoothGattCharacteristic ccc) {
        // Ignore calling ProfileBaseActivity.onMustCccGet to pass readBattery because CC6801 samples don't have battery profile yet
        //super.onMustCccGet(ccc);
        KLog.i("1st CCC found");
        KLog.i("GET===" + ccc.getUuid() + "===");

        onPreWorkDone();
    }

    @Override
    protected void onPreWorkDone() {

    }

    @Override
    protected UUID getFilterSvcUUID() {
        return null;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return CcpsManager.CC_CHARACTERISTIC_UUID;
    }

    @Override
    protected int getAboutTextId() {
        return R.string.csc_about_text;
    }

    @Override
    protected void setDefaultUI() {
        super.setDefaultUI();

        // Clear all values
    }

    // endregion [Override Function]

    // region [Private Function]
    private Observable<Integer> repeatSendData() {

        return Observable.create((ObservableOnSubscribe<Integer>) emitter -> {

            int sendTimes = 0;

            do {

                // Write dummy data to CCPS CCC
                writeCcc(CcpsManager.CC_CHARACTERISTIC_UUID, new byte[]{0x00, 0x01, 0x02, 0x03},
                        this::onWriteData, this::onWriteError);

                sendTimes++;
                emitter.onNext(sendTimes);

                // Delay 60s
                Thread.sleep(60000);

                KLog.i("isConnected: " + isConnected() + ", isChecked: " + mSwitchRepeatSend.isChecked());
            } while (isConnected() && mSwitchRepeatSend.isChecked());

            emitter.onComplete();

        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private void onWriteData(byte[] bytes) {

        KLog.i("Data written to " + getFilterCccUUID().toString() + ", value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true));
    }

    private void onWriteError(Throwable throwable) {

        KLog.i(throwable.toString());
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
                mSwitchRepeatSend.setChecked(false);
                break;
            case DISCONNECTING:
                break;
            default:
                break;
        }
    }
    // endregion [EventBus]
}
