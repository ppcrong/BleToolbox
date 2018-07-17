package com.ppcrong.bletoolbox.csc;

import android.os.Bundle;
import android.view.MenuItem;

import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;

import java.util.UUID;

import butterknife.ButterKnife;

public class CscActivity extends ProfileBaseActivity {

    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_csc);

        // Bind ButterKnife
        ButterKnife.bind(this);
    }

    @Override
    protected void setDefaultUI() {

    }

    @Override
    protected UUID getFilterUUID() {
        return CscManager.CYCLING_SPEED_AND_CADENCE_SERVICE_UUID;
    }
}
