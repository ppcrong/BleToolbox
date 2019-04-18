package com.ppcrong.bletoolbox.base;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rx.ReplayingShare;
import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.polidea.rxandroidble2.helpers.ValueInterpreter;
import com.ppcrong.blescanner.BleScanner;
import com.ppcrong.blescanner.ScannerFragment;
import com.ppcrong.bletoolbox.BleToolboxApp;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.battery.BleBatteryManager;
import com.ppcrong.bletoolbox.csc.CscActivity;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.ppcrong.bletoolbox.htm.HtmActivity;
import com.ppcrong.bletoolbox.rsc.RscActivity;
import com.ppcrong.bletoolbox.uart.log.LogManager;
import com.ppcrong.utils.MiscUtils;
import com.socks.library.KLog;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;

import static com.trello.rxlifecycle2.android.ActivityEvent.DESTROY;

public abstract class ProfileBaseActivity extends RxAppCompatActivity implements ScannerFragment.OnDeviceSelectedListener {

    // region [Constant]
    protected static final int REQUEST_ENABLE_BT = 2;
    private static final String SIS_DEVICE_NAME = "device_name";
    private static final String SIS_DEVICE = "device";
    // endregion [Constant]

    // region [Variable]
    private BluetoothDevice mBluetoothDevice;
    private String mDeviceName;
    private RxBleDevice mBleDevice;
    private Observable<RxBleConnection> mConnectionObservable;
    private PublishSubject<Boolean> disconnectTriggerSubject = PublishSubject.create();
    CopyOnWriteArrayList<RxBleDevice> mSelectedDevices = new CopyOnWriteArrayList<>();
    private MustCccs2 mMustCccs2;
    private MustCccs3 mMustCccs3;
    private OptionalCccs2 mOptionalCccs2;
    private OptionalCccs3 mOptionalCccs3;

    /**
     * Show Detail Log
     */
    private int mShowDetailLogCount = 0;
    private Toast mToastShowDetailLog;
    // endregion [Variable]

    // region [Widget]
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    @BindView(R.id.tv_ble_device)
    TextView mTvBleDevice;
    @BindView(R.id.tv_rx_ble_connection_state)
    TextView mTvRxBleConnectionState;
    @BindView(R.id.tv_rx_ble_error)
    TextView mTvBleError;
    @BindView(R.id.tv_battery)
    TextView mTvBattery;
    // endregion [Widget]

    // region [OnClick]
    @OnClick(R.id.tv_ble_device)
    public void onClickTvBleDevice() {

        if (mShowDetailLogCount >= 5) {
            return;
        }

        mShowDetailLogCount++;
        if (mShowDetailLogCount > 2 && mShowDetailLogCount < 5) {
            if (mToastShowDetailLog != null) mToastShowDetailLog.cancel();
            mToastShowDetailLog = Toast.makeText(this, "" + (5 - mShowDetailLogCount) + " more clicks will show DetailLog", Toast.LENGTH_SHORT);
            mToastShowDetailLog.show();
        }
        if (mShowDetailLogCount == 5) {
            if (mToastShowDetailLog != null) mToastShowDetailLog.cancel();
            mToastShowDetailLog = Toast.makeText(this, "DetailLog mode...", Toast.LENGTH_SHORT);
            mToastShowDetailLog.show();
            mTvBleError.setVisibility(View.GONE);
        }
    }

    private boolean isShowDetailLog() {
        return mShowDetailLogCount >= 5;
    }
    // endregion [OnClick]

    // region [Life Cycle]
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ensureBLESupported();
        if (!isBLEEnabled()) {
            showBLEDialog();
        }

        // In onInitialize method a final class may register local broadcast receivers that will listen for events from the service
        onInitialize(savedInstanceState);
        // The onCreateView class should... create the view
        onCreateView(savedInstanceState);

        // Bind ButterKnife
        ButterKnife.bind(this);

        // Common Toolbox view references are obtained here
        setUpView();
        // View is ready to be used
        onViewCreated(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * You may do some initialization here. This method is called from {@link #onCreate(Bundle)} before the view was created.
     */
    protected void onInitialize(final Bundle savedInstanceState) {
        mSelectedDevices.clear();
    }

    /**
     * Called from {@link #onCreate(Bundle)}. This method should build the activity UI, f.e. using {@link #setContentView(int)}. Use to obtain references to
     * views. Connect/Disconnect button, the device name view and battery level view are manager automatically.
     *
     * @param savedInstanceState contains the data it most recently supplied in {@link #onSaveInstanceState(Bundle)}. Note: <b>Otherwise it is null</b>.
     */
    protected abstract void onCreateView(final Bundle savedInstanceState);

    /**
     * Called after the view has been created.
     *
     * @param savedInstanceState contains the data it most recently supplied in {@link #onSaveInstanceState(Bundle)}. Note: <b>Otherwise it is null</b>.
     */
    protected void onViewCreated(final Bundle savedInstanceState) {
        // empty default implementation
    }

    /**
     * Called after the view and the toolbar has been created.
     */
    protected final void setUpView() {
        // set Toolbar
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Called after all pre-works are done
     */
    protected void onPreWorkDone() {
        // empty default implementation
    }

    /**
     * Called when filter ccc changed for notification
     */
    protected void onFilterCccNotified(byte[] bytes) {
        // empty default implementation
    }

    /**
     * Called when filter ccc changed for indication
     */
    protected void onFilterCccIndicated(byte[] bytes) {
        // empty default implementation
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(SIS_DEVICE_NAME, mDeviceName);
        outState.putParcelable(SIS_DEVICE, mBluetoothDevice);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mDeviceName = savedInstanceState.getString(SIS_DEVICE_NAME);
        mBluetoothDevice = savedInstanceState.getParcelable(SIS_DEVICE);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_base, menu);

        // Handle MenuItem
        handleMenu(menu);

        return true;
    }

    protected void handleMenu(Menu menu) {

        boolean isConnected = isConnected();
        MenuItem item;

        // BT icon
        item = menu.findItem(R.id.action_ble_scan);
        int btIcon = isConnected ?
                R.drawable.ic_menu_bluetooth_connected : R.drawable.ic_menu_bluetooth_white;
        item.setIcon(btIcon);

        // Settings item (for CSC, RSC, HTM)
        item = menu.findItem(R.id.action_settings);
        if (this instanceof CscActivity || this instanceof RscActivity ||
                this instanceof HtmActivity) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }
    }

    /**
     * Use this method to handle menu actions other than home and about.
     *
     * @param itemId the menu item id
     * @return <code>true</code> if action has been handled
     */
    protected boolean onOptionsItemSelected(final int itemId) {
        // Overwrite when using menu other than R.menu.menu_base
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_ble_scan:
                if (isConnected()) {
                    triggerDisconnect();
                } else {
                    scanBle();
                }
                break;
            case R.id.action_settings:
                final Intent intent = new Intent(this, SettingsActivity.class);
                if (this instanceof CscActivity) {
                    intent.putExtra(SettingsActivity.SETTINGS_TITLE, getString(R.string.csc_settings_title));
                } else if (this instanceof RscActivity) {
                    intent.putExtra(SettingsActivity.SETTINGS_TITLE, getString(R.string.rsc_settings_title));
                } else if (this instanceof HtmActivity) {
                    intent.putExtra(SettingsActivity.SETTINGS_TITLE, getString(R.string.hts_settings_title));
                }
                MiscUtils.startSafeIntent(this, intent);
                break;
            case R.id.action_about:
                final AppHelpFragment fragment = AppHelpFragment.getInstance(getAboutTextId());
                fragment.show(getSupportFragmentManager(), "help_fragment");
                break;
            default:
                return onOptionsItemSelected(id);
        }
        return true;
    }

    public void scanBle() {
        if (isBLEEnabled()) {

            // Show scanner
            BleScanner.showScanner(this, getFilterSvcUUID());
        } else {

            // Ask user to enable BT
            showBLEDialog();
        }
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

    // region [Protected Function]

    /**
     * Shows a message as a Toast notification. This method is thread safe, you can call it from any thread
     *
     * @param message a message to be shown
     */
    protected void showToast(final String message) {
        runOnUiThread(() -> Toast.makeText(ProfileBaseActivity.this, message, Toast.LENGTH_LONG).show());
    }

    /**
     * Shows a message as a Toast notification. This method is thread safe, you can call it from any thread
     *
     * @param messageResId an resource id of the message to be shown
     */
    protected void showToast(final int messageResId) {
        runOnUiThread(() -> Toast.makeText(ProfileBaseActivity.this, messageResId, Toast.LENGTH_SHORT).show());
    }

    /**
     * Returns the name of the device that the phone is currently connected to or was connected last time
     */
    protected String getDeviceName() {
        return mDeviceName;
    }

    /**
     * Returns the RxBleDevice object of the device that the phone is currently connected to or was connected last time
     */
    protected RxBleDevice getRxBleDevice() {
        return mBleDevice;
    }

    /**
     * Restores the default UI before reconnecting
     */
    protected void setDefaultUI() {
        mTvRxBleConnectionState.setText(R.string.not_available_value);
        showBleError(false, R.string.not_available_value);
        mTvBleDevice.setText(R.string.not_available_value);
        mTvBattery.setText(R.string.not_available_value);
    }

    /**
     * The UUID filter is used to filter out available devices that does not have such UUID in their advertisement packet. See also:
     * {@link #isChangingConfigurations()}.
     *
     * @return the required service UUID or <code>null</code>
     */
    protected abstract UUID getFilterSvcUUID();

    /**
     * The CCC UUID is used (with filter service UUID) to make sure device that does have such CCC
     *
     * @return the required ccc UUID or <code>null</code>
     */
    protected abstract UUID getFilterCccUUID();

    /**
     * The 2nd CCC UUID is used (with filter service UUID) to make sure device that does have such CCC.
     * <br/>
     * This API is used for the profile needs 2 must CCCs, like UART.
     *
     * @return the required ccc UUID or <code>null</code>
     */
    protected UUID getFilterCccUUID2() {
        return null;
    }

    /**
     * The 3nd CCC UUID is used (with filter service UUID) to make sure device that does have such CCC.
     * <br/>
     * This API is used for the profile needs 3 must CCCs, like BGM
     *
     * @return the required ccc UUID or <code>null</code>
     */
    protected UUID getFilterCccUUID3() {
        return null;
    }

    /**
     * Get MustCccs2 which has filterUUID and filterUUID2
     *
     * @return MustCccs2
     */
    protected MustCccs2 getMustCccs2() {

        return mMustCccs2;
    }

    /**
     * Get MustCccs3 which has filterUUID and filterUUID2 and filterUUID3
     *
     * @return MustCccs3
     */
    protected MustCccs3 getMustCccs3() {

        return mMustCccs3;
    }

    /**
     * Get OptionalCccs2
     *
     * @return OptionalCccs2
     */
    protected OptionalCccs2 getOptionalCccs2() {

        return mOptionalCccs2;
    }

    /**
     * Get OptionalCccs3
     *
     * @return OptionalCccs3
     */
    protected OptionalCccs3 getOptionalCccs3() {

        return mOptionalCccs3;
    }

    /**
     * The optional CCC UUID is used (with filter service UUID) to make sure device that does have such CCC.
     * <br/>
     * This API is used for the profile like BPM's ICP CCC.
     *
     * @return the optional ccc UUID or <code>null</code>
     */
    protected UUID getOptionalCccUUID() {
        return null;
    }

    /**
     * Returns the string resource id that will be shown in About box
     *
     * @return the about resource id
     */
    protected abstract int getAboutTextId();

    protected void showSnackbar(final String message) {

        runOnUiThread(() -> Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show());
    }

    protected void showSnackbar(final int messageResId) {

        runOnUiThread(() -> Snackbar.make(findViewById(android.R.id.content), messageResId, Snackbar.LENGTH_SHORT).show());
    }

    protected void showBleError(final boolean show, final int messageResId) {

        runOnUiThread(() -> {
            if (show && isShowDetailLog()) {

                // TextView
                mTvBleError.setVisibility(View.VISIBLE);
                mTvBleError.setText(messageResId);

                // Snackbar
                showSnackbar(messageResId);
            } else {

                mTvBleError.setVisibility(View.GONE);
                mTvBleError.setText(R.string.not_available_value);
            }
        });
    }

    protected void showBleError(final boolean show, final String message) {

        runOnUiThread(() -> {
            if (show && isShowDetailLog()) {

                // TextView
                mTvBleError.setVisibility(View.VISIBLE);
                mTvBleError.setText(message);

                // Snackbar
                showSnackbar(message);
            } else {

                mTvBleError.setVisibility(View.GONE);
                mTvBleError.setText(R.string.not_available_value);
            }
        });
    }
    // endregion [Protected Function]

    // region [Private Function]
    private void updateUI() {
        // Refresh BT icon
        supportInvalidateOptionsMenu();
    }
    // endregion [Private Function]

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

    public boolean isConnected() {
        boolean isConnected = mBleDevice != null &&
                mBleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
        KLog.i("isConnected: " + isConnected);
        return isConnected;
    }

    private void triggerDisconnect() {

        disconnectTriggerSubject.onNext(true);
        updateUI();
    }

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return mBleDevice
                .establishConnection(false)
                .takeUntil(disconnectTriggerSubject)
                .compose(bindUntilEvent(DESTROY))
                .compose(ReplayingShare.instance());
    }

    protected String describeProperties(BluetoothGattCharacteristic characteristic) {
        List<String> properties = new ArrayList<>();
        if (isCccReadable(characteristic)) properties.add("Read");
        if (isCccWritable(characteristic)) properties.add("Write");
        if (isCccNotifiable(characteristic)) properties.add("Notify");
        if (isCccIndicatable(characteristic)) properties.add("Indicate");
        return TextUtils.join(" ", properties);
    }

    protected String getServiceType(BluetoothGattService service) {
        return service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY ? "primary" : "secondary";
    }

    protected boolean isCccNotifiable(BluetoothGattCharacteristic characteristic) {
        return hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY);
    }

    protected boolean isCccIndicatable(BluetoothGattCharacteristic characteristic) {
        return hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_INDICATE);
    }

    protected boolean isCccReadable(BluetoothGattCharacteristic characteristic) {
        return hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ);
    }

    protected boolean isCccWritable(BluetoothGattCharacteristic characteristic) {
        return (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE
                | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE));
    }

    protected boolean hasProperty(BluetoothGattCharacteristic characteristic, int property) {
        return characteristic != null && (characteristic.getProperties() & property) > 0;
    }

    // endregion [BLE]

    // region [Callback]
    @Override
    public void onDeviceSelected(BluetoothDevice device, String name) {

        KLog.i(name + "(" + device.getAddress() + ")");
        setDefaultUI();

        RxBleDevice bleDevice = BleToolboxApp.getRxBleClient(this).getBleDevice(device.getAddress());
        mBleDevice = bleDevice;
        if (mSelectedDevices.contains(bleDevice)) {

            KLog.i("The device is selected before, skip subscribe observeConnectionStateChanges");
        } else {

            // Not connect before, add into list
            mSelectedDevices.add(mBleDevice);

            // Subscribe ConnectionStateChanges
            mBleDevice.observeConnectionStateChanges()
                    .compose(bindUntilEvent(DESTROY))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onConnectionStateChange);
        }

        // Reference from rxandroidble sample
//        mConnectionObservable
//                .flatMapSingle(RxBleConnection::discoverServices)
//                .flatMapSingle(rxBleDeviceServices -> rxBleDeviceServices.getCharacteristic(getFilterCccUUID()))
//                .observeOn(AndroidSchedulers.mainThread())
//                .doOnSubscribe(disposable -> runOnUiThread(() -> showSnackbar("discovering services")))
//                .subscribe(this::onCccGet, this::onConnectionFailure, this::onConnectionFinished);

        // Prepare observable
        mConnectionObservable = prepareConnectionObservable();

        // Connect to BLE device
        mConnectionObservable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::onConnectionReceived, this::onConnectionFailure);
    }

    @Override
    public void onDialogCanceled() {
        // Do nothing
    }

    private void onConnectionStateChange(RxBleConnection.RxBleConnectionState newState) {

        KLog.i(newState.toString());

        // EventBus
        EventBus.getDefault().post(new BleEvents.BleConnectionState(newState));

        // Add log for new state
        switch (newState) {
            case CONNECTING:
                LogManager.addLog(LogManager.Level.VERBOSE,
                        Calendar.getInstance().getTimeInMillis(), "Connecting...");
                break;
            case CONNECTED:
                LogManager.addLog(LogManager.Level.INFO,
                        Calendar.getInstance().getTimeInMillis(),
                        "Connected to " + mBleDevice.getMacAddress());
                break;
            case DISCONNECTED:
                LogManager.addLog(LogManager.Level.INFO,
                        Calendar.getInstance().getTimeInMillis(), "Disconnected");
                break;
            case DISCONNECTING:
                LogManager.addLog(LogManager.Level.VERBOSE,
                        Calendar.getInstance().getTimeInMillis(), "Disconnecting...");
                break;
            default:
                break;
        }

        mTvRxBleConnectionState.setText(newState.toString());
        updateUI();
    }

    private void onConnectionFailure(Throwable throwable) {

        KLog.i("Connection error: " + throwable);
        showBleError(true, "Connection error: " + throwable);
    }

    @SuppressWarnings("unused")
    private void onConnectionReceived(RxBleConnection connection) {

        KLog.i("Connection received");
        showSnackbar("Connection received");
        mTvBleDevice.setText(mBleDevice.getName() + " (" + mBleDevice.getMacAddress() + ")");

        // When connected, discover services
        if (isConnected()) {

            mConnectionObservable
                    .flatMapSingle(RxBleConnection::discoverServices)
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(disposable -> runOnUiThread(() -> {
                        showSnackbar("Discovering Services...");
                        KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                                Calendar.getInstance().getTimeInMillis(), "Discovering Services..."));
                    }))
                    .subscribe(this::onSvcDiscovered, this::onConnectionFailure);
        }

        // Refresh BT icon
        supportInvalidateOptionsMenu();
    }

    /**
     * Must have 2 Characteristics
     * <br/>
     * 1. Filter CCC
     * <br/>
     * 2. Filter CCC2
     */
    public class MustCccs2 {

        public BluetoothGattCharacteristic FilterCcc;
        public BluetoothGattCharacteristic FilterCcc2;

        public MustCccs2(BluetoothGattCharacteristic filterCcc, BluetoothGattCharacteristic filterCcc2) {
            FilterCcc = filterCcc;
            FilterCcc2 = filterCcc2;
        }
    }

    /**
     * Must have 3 Characteristics
     * <br/>
     * 1. Filter CCC
     * <br/>
     * 2. Filter CCC2
     * <br/>
     * 3. Filter CCC3
     */
    public class MustCccs3 {

        public BluetoothGattCharacteristic FilterCcc;
        public BluetoothGattCharacteristic FilterCcc2;
        public BluetoothGattCharacteristic FilterCcc3;

        public MustCccs3(BluetoothGattCharacteristic filterCcc,
                         BluetoothGattCharacteristic filterCcc2, BluetoothGattCharacteristic filterCcc3) {
            FilterCcc = filterCcc;
            FilterCcc2 = filterCcc2;
            FilterCcc3 = filterCcc3;
        }
    }

    /**
     * Optional 2 Characteristics
     * <br/>
     * 1. CCC
     * <br/>
     * 2.  CCC2
     */
    public class OptionalCccs2 {

        public BluetoothGattCharacteristic OptionalCcc;
        public BluetoothGattCharacteristic OptionalCcc2;

        public OptionalCccs2(BluetoothGattCharacteristic optionalCcc,
                             BluetoothGattCharacteristic optionalCcc2) {
            OptionalCcc = optionalCcc;
            OptionalCcc2 = optionalCcc2;
        }
    }

    /**
     * Optional 3 Characteristics
     * <br/>
     * 1. CCC
     * <br/>
     * 2.  CCC2
     * <br/>
     * 2.  CCC2
     */
    public class OptionalCccs3 {

        public BluetoothGattCharacteristic OptionalCcc;
        public BluetoothGattCharacteristic OptionalCcc2;
        public BluetoothGattCharacteristic OptionalCcc3;

        public OptionalCccs3(BluetoothGattCharacteristic optionalCcc,
                             BluetoothGattCharacteristic optionalCcc2,
                             BluetoothGattCharacteristic optionalCcc3) {
            OptionalCcc = optionalCcc;
            OptionalCcc2 = optionalCcc2;
            OptionalCcc3 = optionalCcc3;
        }
    }

    protected void onSvcDiscovered(RxBleDeviceServices services) {

        // When svc discovered, get filter ccc/ccc2 and battery ccc
        if (isConnected()) {

            if (getFilterCccUUID() != null) {

                KLog.i("1st UUID is NOT null");

                if (getFilterCccUUID2() != null) {

                    KLog.i("2nd UUID is NOT null");

                    if (getFilterCccUUID3() != null) {

                        KLog.i("3rd UUID is NOT null");
                        mConnectionObservable
                                .firstOrError()
                                .flatMap(rxBleConnection -> Single.zip(
                                        services.getCharacteristic(getFilterCccUUID()),
                                        services.getCharacteristic(getFilterCccUUID2()),
                                        services.getCharacteristic(getFilterCccUUID3()),
                                        MustCccs3::new))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(this::onMustCccs3Get, this::onConnectionFailure);
                    } else {

                        KLog.i("3rd UUID is null");
                        mConnectionObservable
                                .firstOrError()
                                .flatMap(rxBleConnection -> Single.zip(
                                        services.getCharacteristic(getFilterCccUUID()),
                                        services.getCharacteristic(getFilterCccUUID2()),
                                        MustCccs2::new))
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(this::onMustCccs2Get, this::onConnectionFailure);
                    }
                } else {

                    KLog.i("2nd UUID is null");
                    mConnectionObservable
                            .flatMapSingle(rxBleConnection -> services.getCharacteristic(getFilterCccUUID()))
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(this::onMustCccGet, this::onConnectionFailure);
                }
            } else {

                KLog.i("1st UUID is null");
            }

            // If optional ccc isn't null, get it
            if (null != getOptionalCccUUID()) {
                KLog.i("Optional UUID is NOT null");
                mConnectionObservable
                        .flatMapSingle(rxBleConnection -> services.getCharacteristic(getOptionalCccUUID()))
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::onOptionalCccGet, this::onConnectionFailure);
            }
        }
    }

    /**
     * Read characteristic
     *
     * @param uuid      The ccc to be read
     * @param onSuccess The callback when read ok
     * @param onError   The callback when error
     */
    protected void readCcc(UUID uuid,
                           final Consumer<byte[]> onSuccess,
                           final Consumer<? super Throwable> onError) {

        if (isConnected()) {

            KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                    Calendar.getInstance().getTimeInMillis(),
                    "Reading characteristic  " + uuid));

            mConnectionObservable
                    .firstOrError()
                    .flatMap(rxBleConnection ->
                            rxBleConnection.readCharacteristic(uuid))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onSuccess, onError);
        }
    }

    /**
     * Write characteristic
     *
     * @param uuid      The ccc to be written
     * @param onSuccess The callback when write ok
     * @param onError   The callback when error
     */
    protected void writeCcc(UUID uuid, byte[] bytes,
                            final Consumer<byte[]> onSuccess,
                            final Consumer<? super Throwable> onError) {

        if (isConnected()) {

            KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                    Calendar.getInstance().getTimeInMillis(),
                    "Writing characteristic  " + uuid));

            mConnectionObservable
                    .firstOrError()
                    .flatMap(rxBleConnection ->
                            rxBleConnection.writeCharacteristic(uuid, bytes))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onSuccess, onError);
        }
    }

    /**
     * Write characteristic
     *
     * @param uuid      The ccc to be written
     * @param onSuccess The callback when write ok
     * @param onError   The callback when error
     */
    protected void longWriteCcc(UUID uuid, byte[] bytes,
                                final Consumer<byte[]> onSuccess,
                                final Consumer<? super Throwable> onError) {

        if (isConnected()) {

            KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                    Calendar.getInstance().getTimeInMillis(),
                    "Long Writing characteristic  " + getFilterCccUUID2()));

            mConnectionObservable
                    .flatMap(rxBleConnection ->
                            rxBleConnection.createNewLongWriteBuilder()
                                    .setCharacteristicUuid(uuid)
                                    .setBytes(bytes)
                                    .build())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onSuccess, onError);
        }
    }

    /**
     * When MUST filter CCC is GET!!!
     *
     * @param ccc
     */
    protected void onMustCccGet(BluetoothGattCharacteristic ccc) {

        KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                Calendar.getInstance().getTimeInMillis(), "1st CCC found"));

        mMustCccs2 = new MustCccs2(ccc, null);

        showCccLog(mMustCccs2);

        readBattery();
    }

    /**
     * When MUST filter CCC and CCC2 are GET!!!
     *
     * @param mustCccs2
     */
    protected void onMustCccs2Get(MustCccs2 mustCccs2) {

        KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                Calendar.getInstance().getTimeInMillis(), "1st and 2nd CCCs found"));

        mMustCccs2 = mustCccs2;

        showCccLog(mustCccs2);

        readBattery();
    }

    /**
     * When MUST filter CCC and CCC2 and CCC3 are GET!!!
     *
     * @param mustCccs3
     */
    protected void onMustCccs3Get(MustCccs3 mustCccs3) {

        KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                Calendar.getInstance().getTimeInMillis(), "1st and 2nd and 3rd CCCs found"));

        mMustCccs3 = mustCccs3;

        showCccLog(mustCccs3);

        readBattery();
    }

    /**
     * When OPTIONAL CCC is GET!!!
     *
     * @param ccc
     */
    protected void onOptionalCccGet(BluetoothGattCharacteristic ccc) {

        KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                Calendar.getInstance().getTimeInMillis(), "Optional CCC found"));

        mOptionalCccs2 = new OptionalCccs2(ccc, null);

        showCccLog(mOptionalCccs2);
    }

    /**
     * Show log to print filter CCC and CCC2 UUIDs
     *
     * @param mustCccs2
     */
    private void showCccLog(MustCccs2 mustCccs2) {

        UUID filterCcc = mustCccs2.FilterCcc != null ? mustCccs2.FilterCcc.getUuid() : null;
        UUID filterCcc2 = mustCccs2.FilterCcc2 != null ? mustCccs2.FilterCcc2.getUuid() : null;

        KLog.i("GET===" + filterCcc + "===");
        KLog.i("GET===" + filterCcc2 + "===");
    }

    /**
     * Show log to print filter CCC and CCC2 and CCC3 UUIDs
     *
     * @param mustCccs3
     */
    private void showCccLog(MustCccs3 mustCccs3) {

        UUID filterCcc = mustCccs3.FilterCcc != null ? mustCccs3.FilterCcc.getUuid() : null;
        UUID filterCcc2 = mustCccs3.FilterCcc2 != null ? mustCccs3.FilterCcc2.getUuid() : null;
        UUID filterCcc3 = mustCccs3.FilterCcc3 != null ? mustCccs3.FilterCcc3.getUuid() : null;

        KLog.i("GET===" + filterCcc + "===");
        KLog.i("GET===" + filterCcc2 + "===");
        KLog.i("GET===" + filterCcc3 + "===");
    }

    /**
     * Show log to print optoinal CCC and CCC2 UUIDs
     *
     * @param optionalCccs2
     */
    private void showCccLog(OptionalCccs2 optionalCccs2) {

        UUID ccc = optionalCccs2.OptionalCcc != null ? optionalCccs2.OptionalCcc.getUuid() : null;
        UUID ccc2 = optionalCccs2.OptionalCcc2 != null ? optionalCccs2.OptionalCcc2.getUuid() : null;

        KLog.i("GET===" + ccc + "=== (Optional)");
        KLog.i("GET===" + ccc2 + "=== (Optional)");
    }

    protected void readBattery() {

        KLog.i();

        // Read battery percentage
        if (isConnected()) {

            KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                    Calendar.getInstance().getTimeInMillis(), "Read battery"));

            mConnectionObservable
                    .firstOrError()
                    .flatMap(rxBleConnection ->
                            rxBleConnection.readCharacteristic(BleBatteryManager.BATTERY_LEVEL_CHARACTERISTIC))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onBatteryRead, this::onReadFailure);
        }
    }

    private void onBatteryRead(byte[] bytes) {

        onBatteryChanged(bytes);

        // Battery read ok, then enable notify of battery and selected ccc
        if (isConnected()) {

            KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                    Calendar.getInstance().getTimeInMillis(),
                    "Enabling notifications for " + BleBatteryManager.BATTERY_LEVEL_CHARACTERISTIC));

            mConnectionObservable
                    .flatMap(rxBleConnection ->
                            rxBleConnection.setupNotification(BleBatteryManager.BATTERY_LEVEL_CHARACTERISTIC))
                    .doOnNext(this::onBatteryNotificationSetupDone)
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::onBatteryNotificationReceived, this::onBatteryNotificationSetupFailure);
        }
    }

    private void onBatteryNotificationSetupDone(Observable<byte[]> observable) {

        runOnUiThread(() -> showSnackbar("Battery notify is setup"));
        onPreWorkDone();
    }

    /**
     * For easy use, enable UUID1's notification (CSC, RSC, HRM...etc)
     */
    protected void setupFilterCccNotification() {

        setupCccNotification(getFilterCccUUID());
    }

    protected void setupCccNotification(final UUID uuid) {

        setupCccNotification(uuid, this::onFilterCccNotificationSetupDone);
    }

    protected void setupCccNotification(final UUID uuid,
                                        final Consumer<Observable<byte[]>> doOnNext) {

        setupCccNotification(uuid, doOnNext,
                this::onFilterCccNotificationReceived, this::onCccNotificationSetupFailure);
    }

    protected void setupCccNotification(final UUID uuid,
                                        final Consumer<Observable<byte[]>> doOnNext,
                                        final Consumer<byte[]> onNotified,
                                        final Consumer<Throwable> onError) {

        // Enable notify of selected ccc
        if (isConnected()) {

            KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                    Calendar.getInstance().getTimeInMillis(),
                    "Enabling notifications for " + uuid));

            mConnectionObservable
                    .flatMap(rxBleConnection ->
                            rxBleConnection.setupNotification(uuid))
                    .doOnNext(doOnNext)
                    .flatMap(notificationObservable -> notificationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onNotified, onError);
        }
    }

    protected void setupCccIndication(final UUID uuid) {

        setupCccIndication(uuid, this::onFilterCccIndicationSetupDone);
    }

    protected void setupCccIndication(final UUID uuid,
                                      final Consumer<Observable<byte[]>> doOnNext) {

        setupCccIndication(uuid, doOnNext,
                this::onFilterCccIndicationReceived, this::onCccIndicationSetupFailure);
    }

    protected void setupCccIndication(final UUID uuid,
                                      final Consumer<Observable<byte[]>> doOnNext,
                                      final Consumer<byte[]> onIndicated,
                                      final Consumer<Throwable> onError) {

        // Enable indication of selected ccc
        if (isConnected()) {

            KLog.i(LogManager.addLog(LogManager.Level.VERBOSE,
                    Calendar.getInstance().getTimeInMillis(),
                    "Enabling indications for " + uuid));

            mConnectionObservable
                    .flatMap(rxBleConnection ->
                            rxBleConnection.setupIndication(uuid))
                    .doOnNext(doOnNext)
                    .flatMap(indicationObservable -> indicationObservable)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(onIndicated, onError);
        }
    }

    private void onCccNotificationSetupFailure(Throwable throwable) {

        KLog.i("Setup Filter CCC notify error: " + throwable);
        showBleError(true, "Setup Filter CCC notify error: " + throwable);
    }

    private void onFilterCccNotificationReceived(byte[] bytes) {

        String raw = MiscUtils.getByteToHexString(bytes, ":", true);
        KLog.i(raw);
        onFilterCccNotified(bytes);
    }

    private void onFilterCccNotificationSetupDone(Observable<byte[]> observable) {

        runOnUiThread(() -> showSnackbar("Filter CCC notify is setup"));
    }

    private void onCccIndicationSetupFailure(Throwable throwable) {

        KLog.i("Setup Filter CCC indicate error: " + throwable);
        showBleError(true, "Setup Filter CCC indicate error: " + throwable);
    }

    private void onFilterCccIndicationReceived(byte[] bytes) {

        String raw = MiscUtils.getByteToHexString(bytes, ":", true);
        KLog.i(raw);
        onFilterCccIndicated(bytes);
    }

    private void onFilterCccIndicationSetupDone(Observable<byte[]> observable) {

        runOnUiThread(() -> showSnackbar("Filter CCC indicate is setup"));
    }

    protected void onBatteryChanged(byte[] bytes) {

        String rawData = MiscUtils.getByteToHexString(bytes, ":", true); // Print raw data for debug
        KLog.i(rawData);
        int percent = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, 0);
        KLog.i("Battery: " + percent + "%");
        mTvBattery.setText("" + percent);
    }

    private void onReadFailure(Throwable throwable) {

        KLog.i("Read CCC error: " + throwable);
        showBleError(true, "Read CCC error: " + throwable);
        onPreWorkDone();
    }

    private void onBatteryNotificationReceived(byte[] bytes) {

        onBatteryChanged(bytes);
    }

    private void onBatteryNotificationSetupFailure(Throwable throwable) {

        KLog.i("Setup battery notify error: " + throwable);
        showBleError(true, "Setup battery notify error: " + throwable);
    }
    // endregion [Callback]
}
