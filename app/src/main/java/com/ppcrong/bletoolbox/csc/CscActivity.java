package com.ppcrong.bletoolbox.csc;

import android.os.Bundle;
import android.view.MenuItem;

import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;

import butterknife.ButterKnife;

public class CscActivity extends ProfileBaseActivity {

    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_csc);

        // Bind ButterKnife
        ButterKnife.bind(this);
    }
}
