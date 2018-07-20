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
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble2.RxBleConnection;
import com.polidea.rxandroidble2.RxBleDevice;
import com.polidea.rxandroidble2.RxBleDeviceServices;
import com.ppcrong.blescanner.BleScanner;
import com.ppcrong.blescanner.ScannerFragment;
import com.ppcrong.bletoolbox.BleToolboxApp;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.battery.BleBatteryManager;
import com.socks.library.KLog;
import com.trello.rxlifecycle2.components.support.RxAppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
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
    // endregion [Variable]

    // region [Widget]
    @BindView(R.id.tv_rx_ble_connection_state)
    TextView mTvRxBleConnectionState;
    @BindView(R.id.tv_ble_device)
    TextView mTvBleDevice;
    @BindView(R.id.tv_battery)
    TextView mTvBattery;
    // endregion [Widget]

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

        // Common nRF Toolbox view references are obtained here
        setUpView();
        // View is ready to be used
        onViewCreated(savedInstanceState);

        Observable.create(new ObservableOnSubscribe<Integer>() {
            @Override
            public void subscribe(ObservableEmitter<Integer> emitter) throws Exception {
                emitter.onNext(1);
                emitter.onNext(2);
                emitter.onNext(3);
            }
        }).flatMap(new Function<Integer, ObservableSource<String>>() {
            @Override
            public ObservableSource<String> apply(Integer integer) throws Exception {
//                final List<String> list = new ArrayList<>();
//                for (int i = 0; i < 4; i++) {
//                    list.add("I am value " + integer);
//                }
//                return Observable.fromIterable(list).delay(10, TimeUnit.MILLISECONDS);
                return Observable.just("I am value " + integer);
            }
        }).subscribe(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                KLog.d(s);
            }
        });
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
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

    private void handleMenu(Menu menu) {

        boolean isConnected = isConnected();
        MenuItem item;

        // BT icon
        item = menu.findItem(R.id.action_ble_scan);
        int btIcon = isConnected ?
                R.drawable.ic_menu_bluetooth_connected : R.drawable.ic_menu_bluetooth_white;
        item.setIcon(btIcon);
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
            case R.id.action_about:
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
     * Restores the default UI before reconnecting
     */
    protected void setDefaultUI() {
        mTvRxBleConnectionState.setText(R.string.not_available_value);
        mTvBleDevice.setText(R.string.not_available_value);
        mTvBattery.setText(R.string.not_available_value);
    }

    /**
     * The UUID filter is used to filter out available devices that does not have such UUID in their advertisement packet. See also:
     * {@link #isChangingConfigurations()}.
     *
     * @return the required UUID or <code>null</code>
     */
    protected abstract UUID getFilterSvcUUID();

    /**
     * The CCC UUID is used with filter UUID (Service UUID)
     *
     * @return the required UUID or <code>null</code>
     */
    protected abstract UUID getFilterCccUUID();

    protected void showSnackbar(final String message) {

        runOnUiThread(() -> Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show());
    }

    protected void showSnackbar(final int messageResId) {

        runOnUiThread(() -> Snackbar.make(findViewById(android.R.id.content), messageResId, Snackbar.LENGTH_SHORT).show());
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

    protected boolean isConnected() {
        return mBleDevice != null &&
                mBleDevice.getConnectionState() == RxBleConnection.RxBleConnectionState.CONNECTED;
    }

    private void triggerDisconnect() {

        disconnectTriggerSubject.onNext(true);
        updateUI();
    }

    private Observable<RxBleConnection> prepareConnectionObservable() {
        return mBleDevice
                .establishConnection(false)
                .takeUntil(disconnectTriggerSubject)
                .compose(bindUntilEvent(DESTROY));
//                .compose(ReplayingShare.instance());
    }

    private String describeProperties(BluetoothGattCharacteristic characteristic) {
        List<String> properties = new ArrayList<>();
        if (isCccReadable(characteristic)) properties.add("Read");
        if (isCccWritable(characteristic)) properties.add("Write");
        if (isCccNotifiable(characteristic)) properties.add("Notify");
        if (isCccIndicatable(characteristic)) properties.add("Indicate");
        return TextUtils.join(" ", properties);
    }

    private String getServiceType(BluetoothGattService service) {
        return service.getType() == BluetoothGattService.SERVICE_TYPE_PRIMARY ? "primary" : "secondary";
    }

    private boolean isCccNotifiable(BluetoothGattCharacteristic characteristic) {
        return hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_NOTIFY);
    }

    private boolean isCccIndicatable(BluetoothGattCharacteristic characteristic) {
        return hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_INDICATE);
    }

    private boolean isCccReadable(BluetoothGattCharacteristic characteristic) {
        return hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_READ);
    }

    private boolean isCccWritable(BluetoothGattCharacteristic characteristic) {
        return (hasProperty(characteristic, BluetoothGattCharacteristic.PROPERTY_WRITE
                | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE));
    }

    private boolean hasProperty(BluetoothGattCharacteristic characteristic, int property) {
        return characteristic != null && (characteristic.getProperties() & property) > 0;
    }
    // endregion [BLE]

    // region [Callback]
    @Override
    public void onDeviceSelected(BluetoothDevice device, String name) {

        KLog.i(name + "(" + device.getAddress() + ")");
        setDefaultUI();

        mBleDevice = BleToolboxApp.getRxBleClient(this).getBleDevice(device.getAddress());
        if (mSelectedDevices.contains(mBleDevice)) {

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

        // Connect to BLE device
        mConnectionObservable = prepareConnectionObservable();
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
        mTvRxBleConnectionState.setText(newState.toString());
        updateUI();
    }

    private void onConnectionFailure(Throwable throwable) {

        KLog.i("Connection error: " + throwable);
        mTvRxBleConnectionState.setText("Connection error: " + throwable);
        showSnackbar("Connection error: " + throwable);
    }

    @SuppressWarnings("unused")
    private void onConnectionReceived(RxBleConnection connection) {

        KLog.i("Connection received");
        showSnackbar("Connection received");
        mTvBleDevice.setText(mBleDevice.getName() + "(" + mBleDevice.getMacAddress() + ")");

        // When connected, discover services
        if (isConnected()) {

            connection.discoverServices()
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnSubscribe(disposable -> mTvRxBleConnectionState.append(" discovering services"))
                    .subscribe(services -> onSvcDiscovered(connection, services),
                            this::onConnectionFailure);
        }

        // Refresh BT icon
        supportInvalidateOptionsMenu();
    }

    /**
     * Must have 2 Characteristics
     * <br/>
     * 1. Filter CCC
     * <br/>
     * 2. Battery CCC
     */
    class MustCccs {

        BluetoothGattCharacteristic FilterCcc;
        BluetoothGattCharacteristic BatteryCcc;

        public MustCccs(BluetoothGattCharacteristic filterCcc, BluetoothGattCharacteristic batteryCcc) {
            FilterCcc = filterCcc;
            BatteryCcc = batteryCcc;
        }
    }

    private void onSvcDiscovered(RxBleConnection connection, RxBleDeviceServices services) {

        // When svc discovered, get filter ccc and battery ccc
        if (isConnected()) {
            Single.zip(services.getCharacteristic(getFilterCccUUID()),
                    services.getCharacteristic(BleBatteryManager.BATTERY_LEVEL_CHARACTERISTIC),
                    MustCccs::new)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            this::onMustCccsGet,
                            this::onConnectionFailure
                    );
        }
    }

    /**
     * When MUST filter CCC and battery CCC are GET!!!
     * @param mustCccs
     */
    private void onMustCccsGet(MustCccs mustCccs) {
        KLog.i("GET===" + mustCccs.FilterCcc.getUuid() + "===");
        KLog.i("GET===" + mustCccs.BatteryCcc.getUuid() + "===");
    }
    // endregion [Callback]
}
