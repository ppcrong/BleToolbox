package com.ppcrong.bletoolbox.base;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
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
import android.widget.Toast;

import com.ppcrong.bletoolbox.R;
import com.socks.library.KLog;

public abstract class ProfileBaseActivity extends AppCompatActivity {

    // region [Constant]
    protected static final int REQUEST_ENABLE_BT = 2;
    private static final String SIS_DEVICE_NAME = "device_name";
    private static final String SIS_DEVICE = "device";
    // endregion [Constant]

    // region [Variable]
    private BluetoothDevice mBluetoothDevice;
    private String mDeviceName;
    // endregion [Variable]

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
    }

    /**
     * You may do some initialization here. This method is called from {@link #onCreate(Bundle)} before the view was created.
     */
    protected void onInitialize(final Bundle savedInstanceState) {
        // empty default implementation
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
        getMenuInflater().inflate(R.menu.menu_help, menu);
        return true;
    }

    /**
     * Use this method to handle menu actions other than home and about.
     *
     * @param itemId the menu item id
     * @return <code>true</code> if action has been handled
     */
    protected boolean onOptionsItemSelected(final int itemId) {
        // Overwrite when using menu other than R.menu.menu_help
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                onBackPressed();
                break;
            case R.id.action_about:
                break;
            default:
                return onOptionsItemSelected(id);
        }
        return true;
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
    // endregion [Protected Function]

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
}
