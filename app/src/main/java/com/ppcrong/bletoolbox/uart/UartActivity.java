package com.ppcrong.bletoolbox.uart;

import android.Manifest;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.ppcrong.bletoolbox.uart.adapter.FileBrowserAppsAdapter;
import com.ppcrong.bletoolbox.uart.adapter.UartConfigurationsAdapter;
import com.ppcrong.bletoolbox.uart.database.DatabaseHelper;
import com.ppcrong.bletoolbox.uart.domain.Command;
import com.ppcrong.bletoolbox.uart.domain.UartConfiguration;
import com.ppcrong.bletoolbox.uart.fragment.UartLogFragment;
import com.ppcrong.bletoolbox.uart.fragment.UartNewConfigurationDialogFragment;
import com.ppcrong.bletoolbox.uart.log.LogManager;
import com.ppcrong.bletoolbox.uart.utils.FileHelper;
import com.ppcrong.bletoolbox.widget.ClosableSpinner;
import com.ppcrong.utils.MiscUtils;
import com.socks.library.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.simpleframework.xml.strategy.Type;
import org.simpleframework.xml.strategy.Visitor;
import org.simpleframework.xml.strategy.VisitorStrategy;
import org.simpleframework.xml.stream.Format;
import org.simpleframework.xml.stream.HyphenStyle;
import org.simpleframework.xml.stream.InputNode;
import org.simpleframework.xml.stream.NodeMap;
import org.simpleframework.xml.stream.OutputNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.UUID;

import butterknife.BindView;

public class UartActivity extends ProfileBaseActivity implements UartInterface,
        UartConfigurationsAdapter.ActionListener, AdapterView.OnItemSelectedListener,
        UartNewConfigurationDialogFragment.NewConfigurationDialogListener {

    // region [Constant]
    public static final String FILE_SAVED_CHANNEL = "file_saved_channel";
    private final static String PREFS_BUTTON_ENABLED = "prefs_uart_enabled_";
    private final static String PREFS_BUTTON_COMMAND = "prefs_uart_command_";
    private final static String PREFS_BUTTON_ICON = "prefs_uart_icon_";
    /**
     * This preference keeps the ID of the selected configuration.
     */
    private final static String PREFS_CONFIGURATION = "configuration_id";
    private final static String SIS_EDIT_MODE = "sis_edit_mode";

    private final static int SELECT_FILE_REQ = 2678; // random
    private final static int PERMISSION_REQ = 24; // random, 8-bit
    // endregion [Constant]

    // region [Variable]
    // region [Current Config]
    private UartConfiguration mConfiguration;
    private DatabaseHelper mDatabaseHelper;
    private SharedPreferences mPreferences;
    private UartConfigurationsAdapter mConfigurationsAdapter;
    private ClosableSpinner mConfigurationSpinner;
    //    private SlidingPaneLayout mSlider;
    private View mContainer;
    private ConfigurationListener mConfigurationListener;
    private boolean mEditMode;
    // endregion [Current Config]
    // endregion [Variable]

    // region [Widget]
//    @BindView(R.id.tv_speed)
//    TextView mTvSpeed;
    @BindView(R.id.sliding_pane)
    SlidingPaneLayout mSlider;
    // endregion [Widget]

    // region [Listener]
    public interface ConfigurationListener {
        void onConfigurationModified();

        void onConfigurationChanged(final UartConfiguration configuration);

        void setEditMode(final boolean editMode);
    }

    public void setConfigurationListener(final ConfigurationListener listener) {
        mConfigurationListener = listener;
    }
    // endregion [Listener]

    // region [Override Function]
    // region [ProfileBase]
    @Override
    protected void onInitialize(Bundle savedInstanceState) {
        super.onInitialize(savedInstanceState);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mDatabaseHelper = new DatabaseHelper(this);
        ensureFirstConfiguration(mDatabaseHelper);
        mConfigurationsAdapter = new UartConfigurationsAdapter(this, this, mDatabaseHelper.getConfigurationsNames());
    }

    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_uart);
    }

    @Override
    protected void onViewCreated(Bundle savedInstanceState) {
        super.onViewCreated(savedInstanceState);

        // SlidingPane
        mContainer = findViewById(R.id.container);
        // Setup the sliding pane if it exists
//        final SlidingPaneLayout slidingPane = mSlider = findViewById(R.id.sliding_pane);
        if (mSlider != null) {
            mSlider.setSliderFadeColor(Color.TRANSPARENT);
            mSlider.setShadowResourceLeft(R.drawable.shadow_r);
            mSlider.setPanelSlideListener(new SlidingPaneLayout.SimplePanelSlideListener() {
                @Override
                public void onPanelClosed(final View panel) {
                    // Close the keyboard
                    final UartLogFragment logFragment = (UartLogFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_log);
                    logFragment.onFragmentHidden();
                }
            });
        }

        // Empty Toolbar title to avoid overlap with spinner
        getSupportActionBar().setTitle("");

        // Toolbar spinner
        final ClosableSpinner configurationSpinner = mConfigurationSpinner = findViewById(R.id.toolbar_spinner);
        configurationSpinner.setOnItemSelectedListener(this);
        configurationSpinner.setAdapter(mConfigurationsAdapter);
        configurationSpinner.setSelection(mConfigurationsAdapter.getItemPosition(mPreferences.getLong(PREFS_CONFIGURATION, 0)));
    }

    @Override
    protected void onRestoreInstanceState(final @NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        mEditMode = savedInstanceState.getBoolean(SIS_EDIT_MODE);
        setEditMode(mEditMode, false);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.uart_menu_configurations, menu);
        getMenuInflater().inflate(mEditMode ? R.menu.uart_menu_config : R.menu.uart_menu, menu);

        final int configurationsCount = mDatabaseHelper.getConfigurationsCount();
        menu.findItem(R.id.action_remove).setVisible(configurationsCount > 1);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected boolean onOptionsItemSelected(int itemId) {
        final String name = mConfiguration.getName();
        switch (itemId) {
            case R.id.action_configure:
                setEditMode(!mEditMode);
                return true;
            case R.id.action_show_log:
                mSlider.openPane();
                return true;
            case R.id.action_share: {
                final String xml = mDatabaseHelper.getConfiguration(mConfigurationSpinner.getSelectedItemId());

                final Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setType("text/xml");
                intent.putExtra(Intent.EXTRA_TEXT, xml);
                intent.putExtra(Intent.EXTRA_SUBJECT, mConfiguration.getName());
                try {
                    startActivity(intent);
                } catch (final ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.no_uri_application, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
            case R.id.action_export: {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    exportConfiguration();
                } else {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQ);
                }
                return true;
            }
            case R.id.action_rename: {
                final DialogFragment fragment = UartNewConfigurationDialogFragment.getInstance(name, false);
                fragment.show(getSupportFragmentManager(), null);
                // onNewConfiguration(name, false) will be called when user press OK
                return true;
            }
            case R.id.action_duplicate: {
                final DialogFragment fragment = UartNewConfigurationDialogFragment.getInstance(name, true);
                fragment.show(getSupportFragmentManager(), null);
                // onNewConfiguration(name, true) will be called when user press OK
                return true;
            }
            case R.id.action_remove: {
                mDatabaseHelper.removeDeletedServerConfigurations(); // just to be sure nothing has left
                final UartConfiguration removedConfiguration = mConfiguration;
                final long id = mDatabaseHelper.deleteConfiguration(name);
//                if (id >= 0)
//                    mWearableSynchronizer.onConfigurationDeleted(id);
                refreshConfigurations();

                final Snackbar snackbar = Snackbar.make(mContainer, R.string.uart_configuration_deleted, Snackbar.LENGTH_INDEFINITE).setAction(R.string.uart_action_undo, v -> {
                    final long id1 = mDatabaseHelper.restoreDeletedServerConfiguration(name);
//                    if (id1 >= 0)
//                        mWearableSynchronizer.onConfigurationAddedOrEdited(id1, removedConfiguration);
                    refreshConfigurations();
                });
                snackbar.setDuration(5000); // This is not an error
                snackbar.show();
                return true;
            }
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions, final @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQ: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // We have been granted the Manifest.permission.WRITE_EXTERNAL_STORAGE permission. Now we may proceed with exporting.
                    exportConfiguration();
                } else {
                    Toast.makeText(this, R.string.no_required_permission, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    @Override
    protected void onPreWorkDone() {

        setupFilterCccNotification();
    }

    @Override
    protected void onFilterCccNotified(byte[] bytes) {
        super.onFilterCccNotified(bytes);

        String data = "Notification received from " + getFilterCccUUID().toString() + ", value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true);
        KLog.i(LogManager.addLog(LogManager.Level.INFO,
                Calendar.getInstance().getTimeInMillis(), data));
        KLog.i(MiscUtils.getByteToHexString(bytes, ":", true));
        try {
            data = new String(bytes, "UTF-8");
            KLog.i(LogManager.addLog(LogManager.Level.APPLICATION,
                    Calendar.getInstance().getTimeInMillis(), "\"" + data + "\" received"));
        } catch (UnsupportedEncodingException e) {
            KLog.i(Log.getStackTraceString(e));
        }
    }

    @Override
    protected UUID getFilterSvcUUID() {
        return UartManager.UART_SERVICE_UUID;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return UartManager.UART_TX_CHARACTERISTIC_UUID; // TX needs to enable notification
    }

    @Override
    protected int getAboutTextId() {
        return R.string.uart_about_text;
    }

    @Override
    protected UUID getFilterCccUUID2() {
        return UartManager.UART_RX_CHARACTERISTIC_UUID;
    }

    // endregion [ProfileBase]

    // region [UART]
    @Override
    public void send(String text) {

        KLog.i("send text: " + text);

        if (!isConnected()) {

            KLog.i("Not connected yet!!!");
            showBleError(true, "Not connected yet!!!");
            return;
        }

        if (!TextUtils.isEmpty(text)) {
            final byte[] buffer = text.getBytes();

            // Depending on whether the characteristic has the WRITE REQUEST property or not, we will either send it as it is (hoping the long write is implemented),
            // or divide it into up to 20 bytes chunks and send them one by one.
            final boolean writeRequest = isCccWritable(getMustCccs().FilterCcc2);
            KLog.i("writeRequest: " + writeRequest);
            KLog.i("text.length: " + text.length());

            if (!writeRequest) { // no WRITE REQUEST property
                writeCcc(UartManager.UART_RX_CHARACTERISTIC_UUID, buffer, this::onRxWrite, this::onRxWriteFailure);
            } else { // there is WRITE REQUEST property, let's try Long Write
                longWriteCcc(UartManager.UART_RX_CHARACTERISTIC_UUID, buffer, this::onRxWrite, this::onRxWriteFailure);
            }
        }
    }

    private void onRxWrite(byte[] bytes) {

        String data = "Data written to " + getFilterCccUUID2().toString() + ", value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true);
        KLog.i(LogManager.addLog(LogManager.Level.INFO,
                Calendar.getInstance().getTimeInMillis(), data));
        try {
            data = new String(bytes, "UTF-8");
            KLog.i(LogManager.addLog(LogManager.Level.APPLICATION,
                    Calendar.getInstance().getTimeInMillis(), "\"" + data + "\" sent"));
        } catch (UnsupportedEncodingException e) {
            KLog.i(Log.getStackTraceString(e));
        }
    }

    private void onRxWriteFailure(Throwable throwable) {

        KLog.i(throwable.toString());
    }

    public void setEditMode(final boolean editMode) {
        setEditMode(editMode, true);
        invalidateOptionsMenu();
    }

    @Override
    public void onNewConfigurationClick() {

        // No item has been selected. We must close the spinner manually.
        mConfigurationSpinner.close();

        // Open the dialog
        final DialogFragment fragment = UartNewConfigurationDialogFragment.getInstance(null, false);
        fragment.show(getSupportFragmentManager(), null);

        // onNewConfiguration(null, false) will be called when user press OK
    }

    @Override
    public void onImportClick() {

        // No item has been selected. We must close the spinner manually.
        mConfigurationSpinner.close();

        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("text/xml");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // file browser has been found on the device
            startActivityForResult(intent, SELECT_FILE_REQ);
        } else {
            // there is no any file browser app, let's try to download one
            final View customView = getLayoutInflater().inflate(R.layout.app_file_browser, null);
            final ListView appsList = customView.findViewById(android.R.id.list);
            appsList.setAdapter(new FileBrowserAppsAdapter(this));
            appsList.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
            appsList.setItemChecked(0, true);
            new AlertDialog.Builder(this).setTitle(R.string.dfu_alert_no_filebrowser_title).setView(customView).setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss()).setPositiveButton(R.string.yes, (dialog, which) -> {
                final int pos = appsList.getCheckedItemPosition();
                if (pos >= 0) {
                    final String query = getResources().getStringArray(R.array.dfu_app_file_browser_action)[pos];
                    final Intent storeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(query));
                    startActivity(storeIntent);
                }
            }).show();
        }
    }

    @Override
    public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_CANCELED)
            return;

        switch (requestCode) {
            case SELECT_FILE_REQ: {
                // clear previous data
                final Uri uri = data.getData();
                /*
                 * The URI returned from application may be in 'file' or 'content' schema.
                 * 'File' schema allows us to create a File object and read details from if directly.
                 * Data from 'Content' schema must be read with use of a Content Provider. To do that we are using a Loader.
                 */
                if (uri.getScheme().equals("file")) {
                    // The direct path to the file has been returned
                    final String path = uri.getPath();
                    try {
                        final FileInputStream fis = new FileInputStream(path);
                        loadConfiguration(fis);
                    } catch (final FileNotFoundException e) {
                        Toast.makeText(this, R.string.uart_configuration_load_error, Toast.LENGTH_LONG).show();
                    }
                } else if (uri.getScheme().equals("content")) {
                    // An Uri has been returned
                    Uri u = uri;

                    // If application returned Uri for streaming, let's us it. Does it works?
                    final Bundle extras = data.getExtras();
                    if (extras != null && extras.containsKey(Intent.EXTRA_STREAM))
                        u = extras.getParcelable(Intent.EXTRA_STREAM);

                    try {
                        final InputStream is = getContentResolver().openInputStream(u);
                        loadConfiguration(is);
                    } catch (final FileNotFoundException e) {
                        Toast.makeText(this, R.string.uart_configuration_load_error, Toast.LENGTH_LONG).show();
                    }
                }
                break;
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if (position > 0) { // FIXME this is called twice after rotation.
            try {
                final String xml = mDatabaseHelper.getConfiguration(id);
                final Format format = new Format(new HyphenStyle());
                final Serializer serializer = new Persister(format);
                mConfiguration = serializer.read(UartConfiguration.class, xml);
                mConfigurationListener.onConfigurationChanged(mConfiguration);
            } catch (final Exception e) {
                KLog.i("Selecting configuration failed" + e.getMessage());

                String message;
                if (e.getLocalizedMessage() != null)
                    message = e.getLocalizedMessage();
                else if (e.getCause() != null && e.getCause().getLocalizedMessage() != null)
                    message = e.getCause().getLocalizedMessage();
                else
                    message = "Unknown error";
                final String msg = message;
                Snackbar.make(mContainer, R.string.uart_configuration_loading_failed, Snackbar.LENGTH_INDEFINITE)
                        .setAction(R.string.uart_action_details, v -> new AlertDialog.Builder(UartActivity.this)
                                .setMessage(msg).setTitle(R.string.uart_action_details)
                                .setPositiveButton(R.string.ok, null)
                                .show())
                        .show();
                return;
            }

            mPreferences.edit().putLong(PREFS_CONFIGURATION, id).apply();
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onNewConfiguration(String name, boolean duplicate) {

        final boolean exists = mDatabaseHelper.configurationExists(name);
        if (exists) {
            Toast.makeText(this, R.string.uart_configuration_name_already_taken, Toast.LENGTH_LONG).show();
            return;
        }

        UartConfiguration configuration = mConfiguration;
        if (!duplicate)
            configuration = new UartConfiguration();
        configuration.setName(name);

        try {
            final Format format = new Format(new HyphenStyle());
            final Strategy strategy = new VisitorStrategy(new CommentVisitor());
            final Serializer serializer = new Persister(strategy, format);
            final StringWriter writer = new StringWriter();
            serializer.write(configuration, writer);
            final String xml = writer.toString();

            final long id = mDatabaseHelper.addConfiguration(name, xml);
//            mWearableSynchronizer.onConfigurationAddedOrEdited(id, configuration);
            refreshConfigurations();
            selectConfiguration(mConfigurationsAdapter.getItemPosition(id));
        } catch (final Exception e) {
            KLog.i("Error while creating a new configuration", e);
        }
    }

    @Override
    public void onRenameConfiguration(String newName) {

        final boolean exists = mDatabaseHelper.configurationExists(newName);
        if (exists) {
            Toast.makeText(this, R.string.uart_configuration_name_already_taken, Toast.LENGTH_LONG).show();
            return;
        }

        final String oldName = mConfiguration.getName();
        mConfiguration.setName(newName);

        try {
            final Format format = new Format(new HyphenStyle());
            final Strategy strategy = new VisitorStrategy(new CommentVisitor());
            final Serializer serializer = new Persister(strategy, format);
            final StringWriter writer = new StringWriter();
            serializer.write(mConfiguration, writer);
            final String xml = writer.toString();

            mDatabaseHelper.renameConfiguration(oldName, newName, xml);
//            mWearableSynchronizer.onConfigurationAddedOrEdited(mPreferences.getLong(PREFS_CONFIGURATION, 0), mConfiguration);
            refreshConfigurations();
        } catch (final Exception e) {
            KLog.i("Error while renaming configuration", e);
        }
    }
    // endregion [UART]
    // endregion [Override Function]

    // region [Public Function]
    public void onCommandChanged(final int index, final String message, final boolean active, final int eol, final int iconIndex) {
        final Command command = mConfiguration.getCommands()[index];

        command.setCommand(message);
        command.setActive(active);
        command.setEol(eol);
        command.setIconIndex(iconIndex);
        mConfigurationListener.onConfigurationModified();
        saveConfiguration();
    }
    // endregion [Public Function]

    // region [Private Function]

    /**
     * Saves the given configuration in the database.
     */
    private void saveConfiguration() {
        final UartConfiguration configuration = mConfiguration;
        try {
            final Format format = new Format(new HyphenStyle());
            final Strategy strategy = new VisitorStrategy(new CommentVisitor());
            final Serializer serializer = new Persister(strategy, format);
            final StringWriter writer = new StringWriter();
            serializer.write(configuration, writer);
            final String xml = writer.toString();

            mDatabaseHelper.updateConfiguration(configuration.getName(), xml);
//            mWearableSynchronizer.onConfigurationAddedOrEdited(mPreferences.getLong(PREFS_CONFIGURATION, 0), configuration);
        } catch (final Exception e) {
            KLog.i("Error while creating a new configuration", e);
        }
    }

    /**
     * Loads the configuration from the given input stream.
     *
     * @param is the input stream
     */
    private void loadConfiguration(final InputStream is) {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            final StringBuilder builder = new StringBuilder();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                builder.append(line).append("\n");
            }
            final String xml = builder.toString();

            final Format format = new Format(new HyphenStyle());
            final Serializer serializer = new Persister(format);
            final UartConfiguration configuration = serializer.read(UartConfiguration.class, xml);

            final String name = configuration.getName();
            if (!mDatabaseHelper.configurationExists(name)) {
                final long id = mDatabaseHelper.addConfiguration(name, xml);
                refreshConfigurations();
                new Handler().post(() -> selectConfiguration(mConfigurationsAdapter.getItemPosition(id)));
            } else {
                Toast.makeText(this, R.string.uart_configuration_name_already_taken, Toast.LENGTH_LONG).show();
            }
        } catch (final Exception e) {
            KLog.i("Loading configuration failed", e);

            String message;
            if (e.getLocalizedMessage() != null)
                message = e.getLocalizedMessage();
            else if (e.getCause() != null && e.getCause().getLocalizedMessage() != null)
                message = e.getCause().getLocalizedMessage();
            else
                message = "Unknown error";
            final String msg = message;
            Snackbar.make(mContainer, R.string.uart_configuration_loading_failed, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.uart_action_details, v -> new AlertDialog.Builder(UartActivity.this)
                            .setMessage(msg).setTitle(R.string.uart_action_details)
                            .setPositiveButton(R.string.ok, null)
                            .show())
                    .show();
        }
    }

    private void exportConfiguration() {
        // TODO this may not work if the SD card is not available. (Lenovo A806, email from 11.03.2015)
        final File folder = new File(Environment.getExternalStorageDirectory(), FileHelper.CC_FOLDER);
        if (!folder.exists())
            folder.mkdir();
        final File serverFolder = new File(folder, FileHelper.UART_FOLDER);
        if (!serverFolder.exists())
            serverFolder.mkdir();

        final String fileName = mConfiguration.getName() + ".xml";
        final File file = new File(serverFolder, fileName);
        try {
            file.createNewFile();
            final FileOutputStream fos = new FileOutputStream(file);
            final OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.append(mDatabaseHelper.getConfiguration(mConfigurationSpinner.getSelectedItemId()));
            writer.close();

            // Notify user about the file
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(FileHelper.getContentUri(this, file), "text/xml");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            showNotification(this, fileName, getText(R.string.uart_configuration_export_succeeded),
                    getText(R.string.uart_configuration_export_succeeded_ticker), android.R.drawable.stat_notify_sdcard, intent);
        } catch (final Exception e) {
            KLog.i("Error while exporting configuration", e);
            Toast.makeText(this, R.string.uart_configuration_save_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void showNotification(Context context, CharSequence contentTitle, CharSequence contentText,
                                 CharSequence ticker, int smallIconId, Intent intent) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        int notificationId = 1;
        String channelId = "CC-channel-01";
        String channelName = "CC-channel-UART-export-xml";
        int importance = NotificationManager.IMPORTANCE_HIGH;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(
                    channelId, channelName, importance);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(contentTitle)
                .setContentText(contentText)
                .setAutoCancel(true)
                .setShowWhen(true)
                .setTicker(ticker)
                .setSmallIcon(smallIconId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addNextIntent(intent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(
                0,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        mBuilder.setContentIntent(resultPendingIntent);

        notificationManager.notify(notificationId, mBuilder.build());
    }

    private void refreshConfigurations() {
        mConfigurationsAdapter.swapCursor(mDatabaseHelper.getConfigurationsNames());
        mConfigurationsAdapter.notifyDataSetChanged();
        invalidateOptionsMenu();
    }

    private void selectConfiguration(final int position) {
        mConfigurationSpinner.setSelection(position);
    }

    /**
     * Converts the old configuration, stored in preferences, into the first XML configuration and saves it to the database.
     * If there is already any configuration in the database this method does nothing.
     */
    private void ensureFirstConfiguration(final DatabaseHelper mDatabaseHelper) {
        // This method ensures that the "old", single configuration has been saved to the database.
        if (mDatabaseHelper.getConfigurationsCount() == 0) {
            final UartConfiguration configuration = new UartConfiguration();
            configuration.setName("First configuration");
            final Command[] commands = configuration.getCommands();

            for (int i = 0; i < 9; ++i) {
                final String cmd = mPreferences.getString(PREFS_BUTTON_COMMAND + i, null);
                if (cmd != null) {
                    final Command command = new Command();
                    command.setCommand(cmd);
                    command.setActive(mPreferences.getBoolean(PREFS_BUTTON_ENABLED + i, false));
                    command.setEol(0); // default one
                    command.setIconIndex(mPreferences.getInt(PREFS_BUTTON_ICON + i, 0));
                    commands[i] = command;
                }
            }

            try {
                final Format format = new Format(new HyphenStyle());
                final Strategy strategy = new VisitorStrategy(new CommentVisitor());
                final Serializer serializer = new Persister(strategy, format);
                final StringWriter writer = new StringWriter();
                serializer.write(configuration, writer);
                final String xml = writer.toString();

                mDatabaseHelper.addConfiguration(configuration.getName(), xml);
            } catch (final Exception e) {
                KLog.i("Error while creating default configuration", e);
            }
        }
    }

    /**
     * Updates the ActionBar background color depending on whether we are in edit mode or not.
     *
     * @param editMode <code>true</code> to show edit mode, <code>false</code> otherwise
     * @param change   if <code>true</code> the background will change with animation, otherwise immediately
     */
    @SuppressLint("NewApi")
    private void setEditMode(final boolean editMode, final boolean change) {
        mEditMode = editMode;
        mConfigurationListener.setEditMode(editMode);
        if (!change) {
            final ColorDrawable color = new ColorDrawable();
            int darkColor = 0;
            if (editMode) {
                color.setColor(ContextCompat.getColor(this, R.color.orange));
                darkColor = ContextCompat.getColor(this, R.color.dark_orange);
            } else {
                color.setColor(ContextCompat.getColor(this, R.color.actionBarColor));
                darkColor = ContextCompat.getColor(this, R.color.actionBarColorDark);
            }
            getSupportActionBar().setBackgroundDrawable(color);

            // Since Lollipop the status bar color may also be changed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                getWindow().setStatusBarColor(darkColor);
        } else {
            final TransitionDrawable transition = (TransitionDrawable) getResources().getDrawable(
                    editMode ? R.drawable.start_edit_mode : R.drawable.stop_edit_mode);
            transition.setCrossFadeEnabled(true);
            getSupportActionBar().setBackgroundDrawable(transition);
            transition.startTransition(200);

            // Since Lollipop the status bar color may also be changed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                final int colorFrom = ContextCompat.getColor(this, editMode ? R.color.actionBarColorDark : R.color.dark_orange);
                final int colorTo = ContextCompat.getColor(this, !editMode ? R.color.actionBarColorDark : R.color.dark_orange);

                final ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                anim.setDuration(200);
                anim.addUpdateListener(animation -> getWindow().setStatusBarColor((Integer) animation.getAnimatedValue()));
                anim.start();
            }

            if (mSlider != null && editMode) {
                mSlider.closePane();
            }
        }
    }
    // endregion [Private Function]

    // region [Private Class]

    /**
     * The comment visitor will add comments to the XML during saving.
     */
    private class CommentVisitor implements Visitor {
        @Override
        public void read(final Type type, final NodeMap<InputNode> node) throws Exception {
            // do nothing
        }

        @Override
        public void write(final Type type, final NodeMap<OutputNode> node) throws Exception {
            if (type.getType().equals(Command[].class)) {
                OutputNode element = node.getNode();

                StringBuilder builder = new StringBuilder("A configuration must have 9 commands, one for each button.\n        Possible icons are:");
                for (Command.Icon icon : Command.Icon.values())
                    builder.append("\n          - ").append(icon.toString());
                element.setComment(builder.toString());
            }
        }
    }
    // endregion [Private Class]

    // region [EventBus]
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBleConnectionStateChange(BleEvents.BleConnectionState event) {

        KLog.i(event);

        switch (event.getState()) {
            case CONNECTING:
                break;
            case CONNECTED:
                new Thread(() -> {
                    final Cursor cursor = mDatabaseHelper.getConfigurations();
                    try {
                        while (cursor.moveToNext()) {
                            final long id = cursor.getLong(0 /* _ID */);
                            try {
                                final String xml = cursor.getString(2 /* XML */);
                                final Format format = new Format(new HyphenStyle());
                                final Serializer serializer = new Persister(format);
                                final UartConfiguration configuration = serializer.read(UartConfiguration.class, xml);
                            } catch (final Exception e) {
                                KLog.i("Deserializing configuration with id " + id + " failed", e);
                            }
                        }
                    } finally {
                        cursor.close();
                    }
                }).start();
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
