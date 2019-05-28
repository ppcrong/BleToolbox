package com.ppcrong.bletoolbox.ota;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.folderselector.FileChooserDialog;
import com.ppcrong.blescanner.BleScanner;
import com.ppcrong.blescanner.ScannerFragment;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.AppHelpFragment;
import com.ppcrong.otalib.OtaLib;
import com.ppcrong.otalib.ota.OtaService;
import com.ppcrong.otalib.ota.eventbus.OtaEvents;
import com.ppcrong.otalib.ota.info.OtaInfo;
import com.ppcrong.otalib.ota.info.OtaProgressInfo;
import com.ppcrong.otalib.utils.StressTest;
import com.ppcrong.utils.MiscUtils;
import com.socks.library.KLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.File;

import butterknife.BindString;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OtaActivity extends AppCompatActivity implements FileChooserDialog.FileCallback,
        ScannerFragment.OnDeviceSelectedListener {

    // region [Constant]
    protected static final int REQUEST_ENABLE_BT = 2;
    // endregion [Constant]

    // region [Variable]
    private String mFilePath;
    private BluetoothDevice mBleDevice;
    private BluetoothGatt mBleGatt;
    // endregion [Variable]

    // region [Widget]
    @BindView(R.id.tv_device_name)
    TextView mTvDeviceName;
    @BindView(R.id.tv_file_name)
    TextView mTvFileName;
    @BindView(R.id.tv_file_size)
    TextView mTvFileSize;
    @BindView(R.id.tv_status)
    TextView mTvStatus;
    @BindView(R.id.btn_select_file)
    Button mBtnSelectFile;
    @BindView(R.id.btn_upload)
    Button mBtnUpload;
    @BindView(R.id.tv_uploading)
    TextView mTvUploading;
    @BindView(R.id.pb_progress)
    ProgressBar mPbProgress;
    @BindView(R.id.tv_progress)
    TextView mTvProgress;
    @BindView(R.id.tv_result)
    TextView mTvResult;
    // endregion [Widget]

    // region [String]
    @BindString(R.string.ota_file_status_ok)
    String mStringOtaFileStatusOk;
    // endregion [String]

    // region [OnClick]
    @OnClick(R.id.btn_select_file)
    public void onClickBtnSelectFile() {

        KLog.i();

        selectFile();
    }

    @OnClick(R.id.btn_upload)
    public void onClickBtnUpload() {

        KLog.i();

        upload(false);
    }

    public int mShowHiddenMenuCount = 0;

    @OnClick(R.id.tv_device_name)
    public void onClickTvDeviceName() {

        mShowHiddenMenuCount++;
        if (mShowHiddenMenuCount > 5) {
            return;
        }
        if (mShowHiddenMenuCount > 2 && mShowHiddenMenuCount < 5) {

            showToast("" + (5 - mShowHiddenMenuCount) + " more clicks will show Status Hidden MenuItems");
        }
        if (mShowHiddenMenuCount == 5) {

            showToast("Hidden Status MenuItems are shown....");
            supportInvalidateOptionsMenu();
        }
    }
    // endregion [OnClick]

    // region [Life Cycle]
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        KLog.d();

        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }

        initView();

        // Keep Screen ON
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        MiscUtils.initWakeLock(this);
        MiscUtils.acquireWakeLock();
    }

    @Override
    protected void onStart() {

        KLog.i();

        EventBus.getDefault().register(this);

        super.onStart();
    }

    @Override
    protected void onStop() {

        KLog.i();

        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        // Disable screen always on
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        MiscUtils.releaseWakeLock();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_ota, menu);

        // Handle MenuItem
        handleMenu(menu);

        return true;
    }

    private boolean mStressTest = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_ble_scan) {

            // [TODO] Change uuid to CcpsManager.CC_OTA_CHARACTERISTIC_UUID when FW is ready to be filtered scan
            BleScanner.showScanner(this, null);
            return true;
        } else if (id == R.id.action_stress_test) {

            mStressTest = !mStressTest;
            if (mStressTest) {

                if (isOtaSvcRunning(this)) {

                    mStressTest = false;
                    showToast("Please wait for current OTA completed...");
                    stopOtaStress(item);
                } else {

                    startOtaStress(item);
                }

            } else {

                stopOtaStress(item);
            }
        } else if (id == R.id.action_about) {
            final AppHelpFragment fragment = AppHelpFragment.getInstance(R.string.ota_about_text);
            fragment.show(getSupportFragmentManager(), "help_fragment");
        } else if (id == android.R.id.home) {
            onBackPressed();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
        } else if (requestCode == REQUEST_ENABLE_BT) {
            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            KLog.d("Finish activity");
            finish();
        }
    }
    // endregion [Life Cycle]

    // region [Private Function]
    private void initView() {

        KLog.i();

        setContentView(R.layout.activity_ota);
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("CC OTA");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void handleMenu(Menu menu) {

        MenuItem item;
        boolean showHiddenMenuItems = mShowHiddenMenuCount >= 5;
        item = menu.findItem(R.id.action_stress_test);
        item.setVisible(showHiddenMenuItems);
    }

    private void selectFile() {

        KLog.i();

        new FileChooserDialog.Builder(this)
                .extensionsFilter(".bin", ".zip")
                .tag("fw_file")
                .show(this);
    }

    private void upload(boolean bStressTest) {

        KLog.i();

        // Check if selected file is ok
        if (!isSelectedFileOk() || !isBleDeviceSelected()) {

            // Show file status invalid
            return;
        }

        if (mBleDevice != null) {

            OtaLib.startOta(this, new OtaInfo.Builder()
                    .setDeviceAddress(mBleDevice.getAddress())
                    .setDeviceName(mBleDevice.getName())
                    .setSelectedFilePath(mFilePath)
                    .setConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_BALANCED)
                    .setStressTest(bStressTest)
                    .build());

            // region [TEST]
//            // Trigger OTA
//            byte[] bytesStart = new byte[]{
//                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
//                    , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
//                    , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
//                    , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
//                    , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
//            BleUtils.sendCcOtaData(mBleGatt, UUIDs.CC_OTA_SERVICE, UUIDs.CC_OTA_CHARACTERISTIC, bytesStart);
//
//            // Read file data
//            byte[] data = sLogLib.readFile(mFilePath);
//            KLog.i(MiscUtils.getByteToHexString(data, ":", true));
//
//            // Send file data
//            int write_size_once = 20;
//            int loop = (int) Math.ceil((double) data.length / (double) write_size_once);
//            for (int i = 0; i < loop; i ++) {
//
//                int from = i * write_size_once;
//                int to = (data.length >= (i + 1) * write_size_once) ? (i + 1) * write_size_once - 1 : data.length - 1;
//                KLog.i("from: " + from + ", to: " + to);
//
//                byte[] bytesToSend = Arrays.copyOfRange(data, from, to + 1);
//                BleUtils.sendCcOtaData(mBleGatt, UUIDs.CC_OTA_SERVICE, UUIDs.CC_OTA_CHARACTERISTIC, bytesToSend);
//            }
//
//            // End OTA
//            byte[] bytesEnd = new byte[]{
//                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
//                    , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
//                    , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
//                    , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00
//                    , (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
//            BleUtils.sendCcOtaData(mBleGatt, UUIDs.CC_OTA_SERVICE, UUIDs.CC_OTA_CHARACTERISTIC, bytesEnd);
            // endregion [TEST]
        } else {

            KLog.i("mBleDevice is null");
            showToast("mBleDevice is null");
        }
    }

    private void updateFileInfo(File file) {

        if (file == null) {

        } else {

            KLog.i("filename: " + file.getName() + ", filesize: " + file.length() + " bytes");
            mTvFileName.setText(file.getName());
            mTvFileSize.setText(file.length() + " bytes");
            mTvStatus.setText(mStringOtaFileStatusOk);
            mBtnUpload.setEnabled(isSelectedFileOk() && isBleDeviceSelected());
        }
    }

    private void showProgressBar() {
        mPbProgress.setVisibility(View.VISIBLE);
        mTvProgress.setVisibility(View.VISIBLE);
        mTvProgress.setText(null);
        mTvUploading.setText(R.string.ota_status_uploading);
        mTvUploading.setVisibility(View.VISIBLE);
        mBtnSelectFile.setEnabled(false);
        mBtnUpload.setEnabled(false);
//        mBtnUpload.setEnabled(true);
//        mBtnUpload.setText(R.string.ota_action_upload_cancel);
    }

    private void clearUI() {

        mPbProgress.setVisibility(View.INVISIBLE);
        mTvProgress.setVisibility(View.INVISIBLE);
        mTvUploading.setVisibility(View.INVISIBLE);
        mBtnSelectFile.setEnabled(true);
        mBtnUpload.setEnabled(true);
        mTvResult.setText("");
    }

    private void clearUI(final boolean clearDevice) {
        mPbProgress.setVisibility(View.INVISIBLE);
        mTvProgress.setVisibility(View.INVISIBLE);
        mTvUploading.setVisibility(View.INVISIBLE);
        mBtnSelectFile.setEnabled(true);
        mBtnUpload.setEnabled(false);
        mBtnUpload.setText(R.string.ota_action_upload);
        if (clearDevice) {
            mBleDevice = null;
            mTvDeviceName.setText(R.string.device);
        }
        // Application may have lost the right to these files if Activity was closed during upload (grant uri permission). Clear file related values.
        mTvFileName.setText(null);
        mTvFileSize.setText(null);
        mTvStatus.setText(R.string.ota_file_status_no_file);
        mFilePath = null;
    }

    private Toast mToastHiddenMenu;

    private void showToast(final int messageResId) {

        if (mToastHiddenMenu != null) mToastHiddenMenu.cancel();
        mToastHiddenMenu = Toast.makeText(this, messageResId, Toast.LENGTH_SHORT);
        mToastHiddenMenu.show();
    }

    private void showToast(final String message) {

        if (mToastHiddenMenu != null) mToastHiddenMenu.cancel();
        mToastHiddenMenu = Toast.makeText(this, message, Toast.LENGTH_SHORT);
        mToastHiddenMenu.show();
    }

    private boolean isSelectedFileOk() {
        boolean b = mTvStatus.getText().toString().equalsIgnoreCase(mStringOtaFileStatusOk);
        KLog.i(b);
        return b;
    }

    private boolean isBleDeviceSelected() {
        boolean b = mBleDevice != null;
        KLog.i(b);
        return b;
    }

    private void startOtaStress(MenuItem item) {

        KLog.i("Start OTA Stress Test");

        /*
         * StressTest init
         */
        StressTest.enable(true);

        upload(true);
        item.setTitle("Cancel Stress Test");
    }

    private void stopOtaStress(MenuItem item) {

        KLog.i("Stop OTA Stress Test");

        OtaLib.stopOtaStressTest();
        item.setTitle("Start Stress Test");
    }

    public static boolean isOtaSvcRunning(Context ctx) {
        final ActivityManager manager = (ActivityManager) ctx.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (com.ppcrong.otalib.ota.OtaService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    // region [BLE]
    private void ensureBLESupported() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    protected boolean isBLEEnabled() {
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter adapter = bluetoothManager.getAdapter();
        return adapter != null && adapter.isEnabled();
    }

    protected void showBLEDialog() {
        final Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
    }
    // endregion [BLE]
    // endregion [Private Function]

    // region [Callback]
    @Override
    public void onFileSelection(@NonNull FileChooserDialog dialog, @NonNull File file) {

        KLog.i("tag: " + dialog.getTag());
        mFilePath = file.getAbsolutePath();
        updateFileInfo(file);
    }

    @Override
    public void onFileChooserDismissed(@NonNull FileChooserDialog dialog) {

    }

    @Override
    public void onDeviceSelected(BluetoothDevice device, String name) {

        mBleDevice = device;
        mTvDeviceName.setText(name);
        mBtnUpload.setEnabled(isSelectedFileOk() && isBleDeviceSelected());
    }

    @Override
    public void onDialogCanceled() {

    }
    // endregion [Callback]

    // region [EventBus]
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onMessageEvent(OtaEvents.NotifyProgressInfoEvent event) {

        OtaProgressInfo info = event.getOtaProgressInfo();
        String uploading = info.getUploading();
        String status = info.getStatus();
        int percent = info.getPercentage();
        KLog.i(String.format("NotifyProgressInfoEvent\nupload: %s\nstatus: %s\npercent:%d%%", uploading, status, percent));

        if (0 <= percent && percent <= 100) {

            mPbProgress.setIndeterminate(false);
            mPbProgress.setProgress(percent);
            mTvProgress.setText(String.format("%d%%", percent));
        } else {

            mPbProgress.setIndeterminate(true);
            mTvProgress.setText(status);

            switch (percent) {
                case OtaService.STATUS_START_OTA:
                    showProgressBar();
                    break;
                case OtaService.STATUS_DONE:
                case OtaService.STATUS_BLE_CONNECT_ERROR:
                case OtaService.STATUS_TIMEOUT_ERROR:
                case OtaService.STATUS_OTA_RECONNECT_TOO_MANY_ERROR:
                case OtaService.STATUS_INIT_FIRMWARE_ERROR:
                case OtaService.STATUS_UNEXPECTED_ERROR:
//                    showToast(getFormattedTime(System.currentTimeMillis()) + " " + status);
                    clearUI();
                    mTvResult.setText(MiscUtils.getFormattedTime(System.currentTimeMillis()) + " " + status);
                    break;
                default:
                    break;
            }
        }
    }
    // endregion [EventBus]
}
