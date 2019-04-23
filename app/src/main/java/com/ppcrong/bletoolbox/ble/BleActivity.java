package com.ppcrong.bletoolbox.ble;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Switch;
import android.widget.TextView;

import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.ppcrong.bletoolbox.ccps.CcpsManager;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.ppcrong.otalib.utils.LogLib;
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

import static com.ppcrong.utils.MiscUtils.getFormattedTime;

public class BleActivity extends ProfileBaseActivity {

    // region [Variable]
    private static LogLib sLogConnection = new LogLib();
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
        return R.string.ble_about_text;
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

                writeLog("====================");

                sendTimes++;
                emitter.onNext(sendTimes);
                writeLog("Send times: " + sendTimes);

                // Write dummy data to CCPS CCC
                writeCcc(CcpsManager.CC_CHARACTERISTIC_UUID, new byte[]{0x00, 0x01, 0x02, 0x03},
                        this::onWriteData, this::onWriteError);

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
        writeLog("Data written to " + getFilterCccUUID().toString() + ", value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true) + "\n");
    }

    private void onWriteError(Throwable throwable) {

        KLog.i(throwable.toString());
        writeLog(throwable.toString() + "\n");
    }

    // region [BLE Connection Log]
    private static long sStartConnectTimestamp;

    public static void startLog(String deviceName) {

        sStartConnectTimestamp = System.currentTimeMillis();
        sLogConnection.openLogFile(sLogConnection.getExDir("Cloudchip/" + deviceName + "/BLE_CONNECTION"), sLogConnection.genFileName(deviceName + "_CONNECTION", "", "log"));
        sLogConnection.writeLog(getFormattedTime(sStartConnectTimestamp) + " ++++++++++START BLE CONNECTION++++++++++\n");
    }

    public static void writeLog(String message) {

        sLogConnection.writeLog(getFormattedTime(System.currentTimeMillis()) + " " + message + "\n");
    }

    public static void stopLog() {

        long timestamp = System.currentTimeMillis();
        sLogConnection.writeLog(getFormattedTime(timestamp) + " ----------END BLE CONNECTION----------\n\n");
        long elapsedTime = timestamp - sStartConnectTimestamp;
//        long elapsedTime = 3l * 24l * 60l * 60l * 1000l + // 3 days
//                12l * 60l * 60l * 1000l + // 12 hrs
//                43l * 60l * 1000l + // 43 minutes
//                55l * 1000l; // 55 seconds
        long days = elapsedTime / 60l / 60l / 1000l / 24l;
        long hours = (elapsedTime / 60l / 60l / 1000l) % 24l;
        long minutes = (elapsedTime / 60l / 1000l) % 60l;
        long seconds = (elapsedTime / 1000l) % 60l;
        String elapsedTimeString = String.format("%d day(s), %02d:%02d:%02d", days, hours, minutes, seconds);
        sLogConnection.writeLog(String.format("Elapsed time: %s, %dms\n",
                elapsedTimeString, elapsedTime));
        KLog.i(String.format(String.format("Elapsed time: %s, %dms\n",
                elapsedTimeString, elapsedTime)));

        String logPath = sLogConnection.closeLogFileReturnPath();
        KLog.i("BLE connection log path: " + logPath);
    }
    // endregion [BLE Connection Log]
    // endregion [Private Function]

    // region [EventBus]
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBleConnectionStateChange(BleEvents.BleConnectionState event) {

        KLog.i(event);

        switch (event.getState()) {
            case CONNECTING:
                startLog(getRxBleDevice().getName());
                writeLog("CONNECTING");
                break;
            case CONNECTED:
                writeLog("CONNECTED");
                break;
            case DISCONNECTED:
                mSwitchRepeatSend.setChecked(false);
                writeLog("DISCONNECTED");
                stopLog();
                break;
            case DISCONNECTING:
                writeLog("DISCONNECTING");
                break;
            default:
                break;
        }
    }
    // endregion [EventBus]
}
