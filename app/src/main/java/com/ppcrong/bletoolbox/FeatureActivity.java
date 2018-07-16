package com.ppcrong.bletoolbox;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.ppcrong.rxpermlib.RxPermLib;
import com.socks.library.KLog;

import butterknife.ButterKnife;

public class FeatureActivity extends AppCompatActivity {

    // region [Life Cycle]
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        KLog.i();
        initView();
    }

    @Override
    protected void onStart() {
        super.onStart();
        KLog.d();
    }

    @Override
    protected void onResume() {
        super.onResume();
        KLog.d();
    }

    @Override
    protected void onPause() {
        super.onPause();
        KLog.d();
    }

    @Override
    protected void onStop() {
        super.onStop();
        KLog.d();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        KLog.d();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RxPermLib.SETTINGS_REQ_CODE) {

            KLog.d("Back from settings");
        } else {

            Toast.makeText(this, R.string.bt_not_enabled, Toast.LENGTH_SHORT).show();
            KLog.d("Finish activity");
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_feature, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    // endregion [Life Cycle]

    // region [Private Function]
    private void initView() {

        // Init view
        setContentView(R.layout.activity_feature);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setSubtitle(BuildConfig.VERSION_NAME);

        // Bind ButterKnife
        ButterKnife.bind(this);

        // Request needful permissions
        RxPermLib.checkPermissions(this, () -> {

            KLog.i("ACCESS_COARSE_LOCATION, WRITE_EXTERNAL_STORAGE granted");
        }, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }
    // endregion [Private Function]
}
