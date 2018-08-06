package com.ppcrong.bletoolbox.ahrs;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.ppcrong.bletoolbox.rsc.RscManager;
import com.ppcrong.unity.ahrs.UnityPlayerActivity;
import com.ppcrong.utils.MiscUtils;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class AhrsActivity extends ProfileBaseActivity {

    // region [Variable]
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

        // Bind ButterKnife
        ButterKnife.bind(this);
    }

    @Override
    protected void onPreWorkDone() {

        setupFilterCccNotification();
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_ahrs, menu);
        return true;
    }

    @Override
    protected boolean onOptionsItemSelected(final int itemId) {
        switch (itemId) {
            case R.id.action_ahrs_demo:
                Intent pageIntent = new Intent();
                pageIntent.setClass(this, UnityPlayerActivity.class);
                MiscUtils.startSafeIntent(this, pageIntent);
                break;
        }
        return true;
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
    // endregion [Override Function]

    // region [Private Function]
    // endregion [Private Function]
}
