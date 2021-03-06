package com.ppcrong.bletoolbox.ahrs;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.lsxiao.apollo.core.Apollo;
import com.polidea.rxandroidble2.helpers.ValueInterpreter;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.ppcrong.bletoolbox.rsc.RscManager;
import com.ppcrong.unity.ahrs.UnityPlayerActivity;
import com.ppcrong.unity.ahrs.apollo.BleEvents;
import com.ppcrong.utils.MiscUtils;
import com.socks.library.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.UUID;

import butterknife.BindView;

public class AhrsActivity extends ProfileBaseActivity {

    // region [Variable]
    private float mMoveFactor = 1;
    // endregion [Variable]

    // region [Widget]
    @BindView(R.id.tv_quaternion_x)
    TextView mTvQuaternionX;
    @BindView(R.id.tv_quaternion_y)
    TextView mTvQuaternionY;
    @BindView(R.id.tv_quaternion_z)
    TextView mTvQuaternionZ;
    @BindView(R.id.tv_quaternion_w)
    TextView mTvQuaternionW;
    @BindView(R.id.tv_euler_x)
    TextView mTvEulerX;
    @BindView(R.id.tv_euler_y)
    TextView mTvEulerY;
    @BindView(R.id.tv_euler_z)
    TextView mTvEulerZ;
    @BindView(R.id.tv_movement_x)
    TextView mTvMovementX;
    @BindView(R.id.tv_movement_y)
    TextView mTvMovementY;
    @BindView(R.id.tv_movement_z)
    TextView mTvMovementZ;
    // endregion [Widget]

    // region [Override Function]
    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_ahrs);
    }

    @Override
    protected void onPreWorkDone() {

        setupFilterCccNotification();
    }

    @Override
    protected int getAboutTextId() {
        return R.string.ahrs_about_text;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {

        getMenuInflater().inflate(R.menu.menu_ahrs, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected boolean onOptionsItemSelected(final int itemId) {
        switch (itemId) {
            case R.id.action_ahrs_demo:
                Intent pageIntent = new Intent();
                pageIntent.setClass(this, UnityPlayerActivity.class);
                MiscUtils.startSafeIntent(this, pageIntent);
                break;
            case R.id.action_set_move_factor:
                showMoveFactorDialog(true);
                break;
        }
        return true;
    }

    @Override
    protected void onFilterCccNotified(byte[] bytes) {
        super.onFilterCccNotified(bytes);

        if (bytes.length < 14) {
            KLog.i("bytes.length is less than 14");
            return;
        }

        // Decode the new data
        int offset = 0;

        // region [Quaternion]
        final int qw = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_SINT16, offset); // ALG qx
        offset += 2;
        final int qz = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_SINT16, offset); // ALG qy
        offset += 2;
        final int qx = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_SINT16, offset); // ALG qz
        offset += 2;
        final int qy = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_SINT16, offset); // ALG qw
        // endregion [Quaternion]

        // region [Euler]
        offset += 2;
        final int ez = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_SINT16, offset); // ALG ex
        offset += 2;
        final int ex = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_SINT16, offset); // ALG ey
        offset += 2;
        final int ey = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_SINT16, offset); // ALG ez
        // endregion [Euler]

        // region [Movement]
        offset += 2;
        final int z = -ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_SINT16, offset); // ALG x
        offset += 2;
        final int x = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_SINT16, offset); // ALG y
        offset += 2;
        final int y = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_SINT16, offset); // ALG z
        // endregion [Movement]

        KLog.i("qx: " + qx + ", qy: " + qy + ", qz: " + qz + ", qw: " + qw +
                ", ex: " + ex + ", ey: " + ey + ", ez: " + ez +
                ", x: " + x + ", y: " + y + ", z: " + z);

        // Update UI
        mTvQuaternionX.setText(Float.toString(((float) qx / 10000f)));
        mTvQuaternionY.setText(Float.toString(((float) qy / 10000f)));
        mTvQuaternionZ.setText(Float.toString(((float) qz / 10000f)));
        mTvQuaternionW.setText(Float.toString(((float) qw / 10000f)));
        mTvEulerX.setText(Integer.toString(ex));
        mTvEulerY.setText(Integer.toString(ey));
        mTvEulerZ.setText(Integer.toString(ez));
        mTvMovementX.setText(Integer.toString(x));
        mTvMovementY.setText(Integer.toString(y));
        mTvMovementZ.setText(Integer.toString(z));

        // Notify Unity
        Apollo.emit("BleEvents.NotifyAhrsRotateQuaternionEvent",
                new BleEvents.NotifyAhrsRotateQuaternionEvent(
                        ((float) qx / 10000f),
                        ((float) qy / 10000f),
                        ((float) qz / 10000f),
                        ((float) qw / 10000f)));
        Apollo.emit("BleEvents.NotifyAhrsRotateEulerEvent", new BleEvents.NotifyAhrsRotateEulerEvent(ex, ey, ez));
        Apollo.emit("BleEvents.NotifyAhrsMoveEvent",
                new BleEvents.NotifyAhrsMoveEvent(x / mMoveFactor, y / mMoveFactor, z / mMoveFactor));
    }

    @Override
    protected UUID getFilterSvcUUID() {
        return RscManager.RUNNING_SPEED_AND_CADENCE_SERVICE_UUID;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return RscManager.RSC_MEASUREMENT_CHARACTERISTIC_UUID;
    }
    // endregion [Override Function]

    // region [Private Function]
    MaterialDialog mDialogSetMoveFactor;

    private void showMoveFactorDialog(boolean b) {

        KLog.i((b ? "SHOW" : "HIDE") + " MoveFactorDialog");

        if (isFinishing()) {
            KLog.i("Activity is finishing, ignore show dialog");
            return;
        }

        if (b) {

            mDialogSetMoveFactor = new MaterialDialog.Builder(this)
                    .title("Set Move Factor")
                    .titleColor(Color.DKGRAY)
                    .content("The factor is like (x/factor), (y/factor), (z/factor)")
                    .contentColor(Color.BLUE)
                    .negativeText(R.string.cancel)
                    .inputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
                    .input("Please input number", String.valueOf(mMoveFactor), (dialog, input) -> {

                        KLog.i("input: " + input);
                        try {

                            mMoveFactor = Float.parseFloat(input.toString());
                        } catch (Exception ex) {

                            KLog.i(Log.getStackTraceString(ex));
                        }
                        KLog.i("mMoveFactor: " + mMoveFactor);
                        showSnackbar("Now Move Factor is " + mMoveFactor);
                    })
                    .show();
        } else {

            if (mDialogSetMoveFactor != null && mDialogSetMoveFactor.isShowing()) {
                mDialogSetMoveFactor.dismiss();
                mDialogSetMoveFactor = null;
            }
        }
    }
    // endregion [Private Function]

    // region [EventBus]
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBleConnectionStateChange(com.ppcrong.bletoolbox.eventbus.BleEvents.BleConnectionState event) {

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
