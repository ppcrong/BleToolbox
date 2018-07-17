package com.ppcrong.bletoolbox;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.ppcrong.bletoolbox.adapter.FeatureAdapter;
import com.ppcrong.rxpermlib.RxPermLib;
import com.socks.library.KLog;

import butterknife.BindView;
import butterknife.ButterKnife;

public class FeatureActivity extends AppCompatActivity {

    // region [Widget]
    @BindView(R.id.rv_feature_list)
    RecyclerView mRvFeatureList;
    @BindView(R.id.tv_empty)
    TextView mTvEmpty;
    // endregion [Widget]

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
        if (id == R.id.action_about) {
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

        // Config feature list
        mRvFeatureList.setLayoutManager(new GridLayoutManager(this, 3));
        mRvFeatureList.setAdapter(new FeatureAdapter(this));
        if (mRvFeatureList.getAdapter().getItemCount() == 0) {

            // No profiles found
            mTvEmpty.setVisibility(View.VISIBLE);
        }
    }
    // endregion [Private Function]
}
