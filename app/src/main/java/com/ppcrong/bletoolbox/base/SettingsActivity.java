package com.ppcrong.bletoolbox.base;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.csc.settings.CscSettingsFragment;
import com.ppcrong.bletoolbox.rsc.settings.RscSettingsFragment;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingsActivity extends AppCompatActivity {

    // region [Constant]
    public static final String SETTINGS_TITLE = "settings_title";
    // endregion [Constant]

    // region [Widget]
    @BindView(R.id.toolbar)
    Toolbar mToolbar;
    // endregion [Widget]

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Bind ButterKnife
        ButterKnife.bind(this);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Set title
        String title = "";
        Intent intent = getIntent();
        if (intent.hasExtra(SETTINGS_TITLE)) {

            title = intent.getStringExtra(SETTINGS_TITLE);
            getSupportActionBar().setTitle(title);
        }

        // Display the fragment as the main content.
        if (title.equalsIgnoreCase(getString(R.string.csc_settings_title))) {
            getFragmentManager().beginTransaction().replace(R.id.content, new CscSettingsFragment()).commit();
        } else if (title.equalsIgnoreCase(getString(R.string.rsc_settings_title))) {
            getFragmentManager().beginTransaction().replace(R.id.content, new RscSettingsFragment()).commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
