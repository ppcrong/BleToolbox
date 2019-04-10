package com.ppcrong.bletoolbox.gls;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.MenuInflater;
import android.view.View;
import android.widget.BaseExpandableListAdapter;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.polidea.rxandroidble2.helpers.ValueInterpreter;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ExpandableListActivity;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.ppcrong.bletoolbox.parser.GlucoseMeasurementContextParser;
import com.ppcrong.bletoolbox.parser.GlucoseMeasurementParser;
import com.ppcrong.bletoolbox.parser.RecordAccessControlPointParser;
import com.ppcrong.utils.MiscUtils;
import com.socks.library.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;
import java.util.UUID;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;

import static com.ppcrong.bletoolbox.gls.GlucoseManager.FILTER_TYPE_SEQUENCE_NUMBER;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.OPERATOR_ALL_RECORDS;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.OPERATOR_FIRST_RECORD;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.OPERATOR_GREATER_THEN_OR_EQUAL;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.OPERATOR_LAST_RECORD;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.OPERATOR_NULL;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.OP_CODE_ABORT_OPERATION;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.OP_CODE_DELETE_STORED_RECORDS;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.OP_CODE_NUMBER_OF_STORED_RECORDS_RESPONSE;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.OP_CODE_REPORT_NUMBER_OF_RECORDS;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.OP_CODE_REPORT_STORED_RECORDS;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.OP_CODE_RESPONSE_CODE;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.RESPONSE_ABORT_UNSUCCESSFUL;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.RESPONSE_NO_RECORDS_FOUND;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.RESPONSE_OP_CODE_NOT_SUPPORTED;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.RESPONSE_PROCEDURE_NOT_COMPLETED;
import static com.ppcrong.bletoolbox.gls.GlucoseManager.RESPONSE_SUCCESS;

public class GlucoseActivity extends ExpandableListActivity {

    // region [Variable]
    private final SparseArray<GlucoseRecord> mRecords = new SparseArray<>();
    private BaseExpandableListAdapter mAdapter;
    private boolean mAbort;
    // endregion [Variable]

    // region [Widget]
    @BindView(R.id.tv_unit)
    TextView mTvUnit;
    @BindView(R.id.view_control_std)
    View mViewControlStd;
    @BindView(R.id.view_control_abort)
    View mViewControlAbort;
    // endregion [Widget]

    // region [OnClick]
    @OnClick(R.id.btn_more)
    public void onClickMore(View v) {

        PopupMenu menu = new PopupMenu(GlucoseActivity.this, v);
        menu.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case R.id.action_refresh:
                    refreshRecords();
                    break;
                case R.id.action_first:
                    getFirstRecord();
                    break;
                case R.id.action_clear:
                    clear();
                    break;
                case R.id.action_delete_all:
                    deleteAllRecords();
                    break;
            }
            return true;
        });
        MenuInflater inflater = menu.getMenuInflater();
        inflater.inflate(R.menu.menu_gls_more, menu.getMenu());
        menu.show();
    }

    @OnClick(R.id.btn_last)
    public void onClickLast() {

        getLastRecord();
    }

    @OnClick(R.id.btn_all)
    public void onClickAll() {

        getAllRecords();
    }

    @OnClick(R.id.btn_abort)
    public void onClickAbort() {

        abort();
    }
    // endregion [OnClick]

    // region [Override Function]

    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_gls);

        setListAdapter(mAdapter = new ExpandableRecordAdapter(this, this));
    }

    @Override
    protected void onPreWorkDone() {

        KLog.i();

        // (1) Enable GM notification
        // (2) Enable RACP indication
        // (3) Enable GM_CONTEXT notification
        setupCccNotification(getFilterCccUUID(), this::onGmNotificationSetupDone,
                this::onGmNotified, this::onGmNotifiedFailure);
    }

    private void onGmNotificationSetupDone(Observable<byte[]> observable) {

        KLog.i();

        // (1) Enable GM notification
        // (2) Enable RACP indication
        // (3) Enable GM_CONTEXT notification
        setupCccIndication(getFilterCccUUID2(), this::onRcapIndicationSetupDone,
                this::onRcapIndicated, this::onRcapIndicatedFailure);
    }

    private void onRcapIndicationSetupDone(Observable<byte[]> observable) {

        KLog.i();

        // If optional GM_CONTEXT exists, enable notification
        if (null != getOptionalCccs2() && null != getOptionalCccs2().OptionalCcc) {
            // (1) Enable GM notification
            // (2) Enable RACP indication
            // (3) Enable GM_CONTEXT notification
            setupCccNotification(getOptionalCccUUID(), this::onGmContextNotificationSetupDone,
                    this::onGmContextNotified, this::onGmContextNotifiedFailure);
        }
    }

    private void onGmContextNotificationSetupDone(Observable<byte[]> observable) {

        KLog.i();
    }

    private void onGmNotified(byte[] bytes) {

        KLog.i("\"" + GlucoseMeasurementParser.parse(getMustCccs2().FilterCcc) + "\" received");

        int offset = 0;
        final int flags = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset);
        offset += 1;

        final boolean timeOffsetPresent = (flags & 0x01) > 0;
        final boolean typeAndLocationPresent = (flags & 0x02) > 0;
        final int concentrationUnit = (flags & 0x04) > 0 ? GlucoseRecord.UNIT_molpl : GlucoseRecord.UNIT_kgpl;
        final boolean sensorStatusAnnunciationPresent = (flags & 0x08) > 0;
        final boolean contextInfoFollows = (flags & 0x10) > 0;

        // create and fill the new record
        final GlucoseRecord record = new GlucoseRecord();
        record.sequenceNumber = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset);
        offset += 2;

        final int year = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset);
        final int month = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 2) - 1; // months are 1-based
        final int day = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 3);
        final int hours = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 4);
        final int minutes = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 5);
        final int seconds = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 6);
        offset += 7;

        final Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hours, minutes, seconds);
        record.time = calendar;

        if (timeOffsetPresent) {
            record.timeOffset = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_SINT16, offset);
            calendar.add(Calendar.MINUTE, record.timeOffset);
            offset += 2;
        }

        if (typeAndLocationPresent) {
            record.glucoseConcentration = ValueInterpreter.getFloatValue(bytes, ValueInterpreter.FORMAT_SFLOAT, offset);
            record.unit = concentrationUnit;
            final int typeAndLocation = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 2);
            record.type = (typeAndLocation & 0x0F);
            record.sampleLocation = (typeAndLocation & 0xF0) >> 4;
            offset += 3;
        }

        if (sensorStatusAnnunciationPresent) {
            record.status = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset);
        }
        // This allows you to check other values that are not provided by the Nordic Semiconductor's Glucose Service in SDK 4.4.2.
        //				record.status = 0x1A;
        //				record.context = new GlucoseRecord.MeasurementContext();
        //				record.context.carbohydrateId = 1;
        //				record.context.carbohydrateUnits = 0.23f;
        //				record.context.meal = 2;
        //				record.context.tester = 2;
        //				record.context.health = 4;
        // the following values are not implemented yet (see ExpandableRecordAdapter#getChildrenCount() and #getChild(...)
        //				record.context.exerciseDuration = 3600;
        //				record.context.exerciseIntensity = 45;
        //				record.context.medicationId = 3;
        //				record.context.medicationQuantity = 0.03f;
        //				record.context.medicationUnit = GlucoseRecord.MeasurementContext.UNIT_kg;
        //				record.context.HbA1c = 213.3f;

        // data set modifications must be done in UI thread
        runOnUiThread(() -> {

            // insert the new record to storage
            mRecords.put(record.sequenceNumber, record);

            // if there is no context information following the measurement data, notify callback about the new record
            if (!contextInfoFollows)
                onDatasetChanged();
        });
    }

    private void onGmContextNotified(byte[] bytes) {

        KLog.i("\"" + GlucoseMeasurementContextParser.parse(getOptionalCccs2().OptionalCcc) + "\" received");

        int offset = 0;
        final int flags = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset);
        offset += 1;

        final boolean carbohydratePresent = (flags & 0x01) > 0;
        final boolean mealPresent = (flags & 0x02) > 0;
        final boolean testerHealthPresent = (flags & 0x04) > 0;
        final boolean exercisePresent = (flags & 0x08) > 0;
        final boolean medicationPresent = (flags & 0x10) > 0;
        final int medicationUnit = (flags & 0x20) > 0 ? GlucoseRecord.MeasurementContext.UNIT_l : GlucoseRecord.MeasurementContext.UNIT_kg;
        final boolean hbA1cPresent = (flags & 0x40) > 0;
        final boolean moreFlagsPresent = (flags & 0x80) > 0;

        final int sequenceNumber = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset);
        offset += 2;

        final GlucoseRecord record = mRecords.get(sequenceNumber);
        if (record == null) {
            KLog.w("Context information with unknown sequence number: " + sequenceNumber);
            return;
        }

        final GlucoseRecord.MeasurementContext context = new GlucoseRecord.MeasurementContext();
        record.context = context;

        if (moreFlagsPresent)
            offset += 1;

        if (carbohydratePresent) {
            context.carbohydrateId = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset);
            context.carbohydrateUnits = ValueInterpreter.getFloatValue(bytes, ValueInterpreter.FORMAT_SFLOAT, offset + 1);
            offset += 3;
        }

        if (mealPresent) {
            context.meal = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset);
            offset += 1;
        }

        if (testerHealthPresent) {
            final int testerHealth = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset);
            context.tester = (testerHealth & 0xF0) >> 4;
            context.health = (testerHealth & 0x0F);
            offset += 1;
        }

        if (exercisePresent) {
            context.exerciseDuration = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset);
            context.exerciseIntensity = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 2);
            offset += 3;
        }

        if (medicationPresent) {
            context.medicationId = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset);
            context.medicationQuantity = ValueInterpreter.getFloatValue(bytes, ValueInterpreter.FORMAT_SFLOAT, offset + 1);
            context.medicationUnit = medicationUnit;
            offset += 3;
        }

        if (hbA1cPresent) {
            context.HbA1c = ValueInterpreter.getFloatValue(bytes, ValueInterpreter.FORMAT_SFLOAT, offset);
        }

        // notify callback about the new record
        onDatasetChanged();
    }

    private void onRcapIndicated(byte[] bytes) {

        KLog.i("\"" + RecordAccessControlPointParser.parse(getMustCccs2().FilterCcc2) + "\" received");

        // Record Access Control Point characteristic
        int offset = 0;
        final int opCode = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset);
        offset += 2; // skip the operator

        if (opCode == OP_CODE_NUMBER_OF_STORED_RECORDS_RESPONSE) {
            // We've obtained the number of all records
            final int number = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset);

            onNumberOfRecordsRequested(number);

            // Request the records
            if (number > 0) {
                writeCcc(GlucoseManager.RACP_CHARACTERISTIC,
                        getOpCode(OP_CODE_REPORT_STORED_RECORDS, OPERATOR_ALL_RECORDS),
                        this::onRequestRecordsWrite, this::onRequestRecordsWriteFailure);
            } else {
                onOperationCompleted();
            }
        } else if (opCode == OP_CODE_RESPONSE_CODE) {
            final int requestedOpCode = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset);
            final int responseCode = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset + 1);
            KLog.i("Response result for: " + requestedOpCode + " is: " + responseCode);

            switch (responseCode) {
                case RESPONSE_SUCCESS:
                    if (!mAbort)
                        onOperationCompleted();
                    else
                        onOperationAborted();
                    break;
                case RESPONSE_NO_RECORDS_FOUND:
                    onOperationCompleted();
                    break;
                case RESPONSE_OP_CODE_NOT_SUPPORTED:
                    onOperationNotSupported();
                    break;
                case RESPONSE_PROCEDURE_NOT_COMPLETED:
                case RESPONSE_ABORT_UNSUCCESSFUL:
                default:
                    onOperationFailed();
                    break;
            }
            mAbort = false;
        }
    }

    private void onRequestRecordsWrite(byte[] bytes) {

        KLog.i("Data written to RACP, value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true));
    }

    private void onRequestRecordsWriteFailure(Throwable throwable) {

        KLog.i();
    }

    private void onGmNotifiedFailure(Throwable throwable) {

        KLog.i();
    }

    private void onRcapIndicatedFailure(Throwable throwable) {

        KLog.i();
    }

    private void onGmContextNotifiedFailure(Throwable throwable) {

        KLog.i();
    }

    @Override
    protected UUID getFilterSvcUUID() {
        return GlucoseManager.GLS_SERVICE_UUID;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return GlucoseManager.GM_CHARACTERISTIC;
    }

    @Override
    protected UUID getFilterCccUUID2() {
        return GlucoseManager.RACP_CHARACTERISTIC;
    }

    @Override
    protected UUID getOptionalCccUUID() {
        return GlucoseManager.GM_CONTEXT_CHARACTERISTIC;
    }

    @Override
    protected int getAboutTextId() {
        return R.string.gls_about_text;
    }

    @Override
    protected void setDefaultUI() {
        super.setDefaultUI();

        clear();
    }

    // endregion [Override Function]

    // region [Private Function]

    /**
     * Writes given operation parameters to the characteristic
     *
     * @param opCode   the operation code
     * @param operator the operator (see {@link GlucoseManager#OPERATOR_NULL} and others
     * @param params   optional parameters (one for >=, <=, two for the range, none for other operators)
     */
    private byte[] getOpCode(final int opCode, final int operator, final Integer... params) {
        // 1 byte for opCode, 1 for operator, 1 for filter type (if parameters exists) and 2 for each parameter
        final int size = 2 + ((params.length > 0) ? 1 : 0) + params.length * 2;
        final byte[] data = new byte[size];

        // Write the operation code
        int offset = 0;
        data[offset++] = (byte) opCode;

        // Write the operator. This is always present but may be equal to OPERATOR_NULL
        data[offset++] = (byte) operator;

        // If parameters exists, append them. Parameters should be sorted from minimum to maximum.
        // Currently only one or two params are allowed
        if (params.length > 0) {
            // Our implementation use only sequence number as a filer type
            data[offset++] = FILTER_TYPE_SEQUENCE_NUMBER;

            for (final Integer i : params) {
                data[offset++] = (byte) (i & 0xFF);
                data[offset++] = (byte) ((i >> 8) & 0xFF);
            }
        }
        return data;
    }

    // region [Operation Records]

    /**
     * Returns all records as a sparse array where sequence number is the key.
     *
     * @return the records list
     */
    public SparseArray<GlucoseRecord> getRecords() {
        return mRecords;
    }

    /**
     * Clears the records list locally
     */
    public void clear() {
        mRecords.clear();
        onOperationCompleted();
    }

    /**
     * Sends the request to obtain the last (most recent) record from glucose device. The data will
     * be returned to Glucose Measurement characteristic as a notification followed by Record Access
     * Control Point indication with status code ({@link GlucoseManager#RESPONSE_SUCCESS} or other in case of error.
     */
    public void getLastRecord() {

        if (null == getMustCccs2() || null == getMustCccs2().FilterCcc2) return;

        clear();
        onOperationStarted();

        writeCcc(GlucoseManager.RACP_CHARACTERISTIC,
                getOpCode(OP_CODE_REPORT_STORED_RECORDS, OPERATOR_LAST_RECORD),
                this::onGetLastRecordWrite, this::onGetLastRecordWriteFailure);
    }

    private void onGetLastRecordWrite(byte[] bytes) {

        KLog.i("Data written to RACP, value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true));
    }

    private void onGetLastRecordWriteFailure(Throwable throwable) {

    }

    /**
     * Sends the request to obtain the first (oldest) record from glucose device. The data will be
     * returned to Glucose Measurement characteristic as a notification followed by Record Access
     * Control Point indication with status code ({@link GlucoseManager#RESPONSE_SUCCESS} or other in case of error.
     */
    public void getFirstRecord() {

        if (null == getMustCccs2() || null == getMustCccs2().FilterCcc2) return;

        clear();
        onOperationStarted();

        writeCcc(GlucoseManager.RACP_CHARACTERISTIC,
                getOpCode(OP_CODE_REPORT_STORED_RECORDS, OPERATOR_FIRST_RECORD),
                this::onGetFirstRecordWrite, this::onGetFirstRecordWriteFailure);
    }

    private void onGetFirstRecordWrite(byte[] bytes) {

        KLog.i("Data written to RACP, value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true));
    }

    private void onGetFirstRecordWriteFailure(Throwable throwable) {

    }

    /**
     * Sends the request to obtain all records from glucose device. Initially we want to notify
     * him/her about the number of the records so the {@link GlucoseManager#OP_CODE_REPORT_NUMBER_OF_RECORDS}
     * is send. The data will be returned to Glucose Measurement characteristic as a notification
     * followed by Record Access Control Point indication with status code ({@link GlucoseManager#RESPONSE_SUCCESS}
     * or other in case of error.
     */
    public void getAllRecords() {

        if (null == getMustCccs2() || null == getMustCccs2().FilterCcc2) return;

        clear();
        onOperationStarted();

        writeCcc(GlucoseManager.RACP_CHARACTERISTIC,
                getOpCode(OP_CODE_REPORT_NUMBER_OF_RECORDS, OPERATOR_ALL_RECORDS),
                this::onGetAllRecordsWrite, this::onGetAllRecordsWriteFailure);
    }

    private void onGetAllRecordsWrite(byte[] bytes) {

        KLog.i("Data written to RACP, value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true));
    }

    private void onGetAllRecordsWriteFailure(Throwable throwable) {

    }

    /**
     * Sends the request to obtain from the glucose device all records newer than the newest one
     * from local storage. The data will be returned to Glucose Measurement characteristic as a
     * notification followed by Record Access Control Point indication with status code
     * ({@link GlucoseManager#RESPONSE_SUCCESS} or other in case of error.
     * <p>
     * Refresh button will not download records older than the oldest in the local memory.
     * E.g. if you have pressed Last and then Refresh, than it will try to get only newer records.
     * However, if there are no records, it will download all existing (using {@link #getAllRecords()}).
     */
    public void refreshRecords() {

        if (null == getMustCccs2() || null == getMustCccs2().FilterCcc2) return;

        if (mRecords.size() == 0) {
            getAllRecords();
        } else {
            onOperationStarted();

            // obtain the last sequence number
            final int sequenceNumber = mRecords.keyAt(mRecords.size() - 1) + 1;

            writeCcc(GlucoseManager.RACP_CHARACTERISTIC,
                    getOpCode(OP_CODE_REPORT_STORED_RECORDS, OPERATOR_GREATER_THEN_OR_EQUAL, sequenceNumber),
                    this::onRefreshRecordsWrite, this::onRefreshRecordsWriteFailure);
        }
    }

    private void onRefreshRecordsWrite(byte[] bytes) {

        KLog.i("Data written to RACP, value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true));
    }

    private void onRefreshRecordsWriteFailure(Throwable throwable) {

    }

    /**
     * Sends abort operation signal to the device
     */
    public void abort() {

        if (null == getMustCccs2() || null == getMustCccs2().FilterCcc2) return;

        mAbort = true;
        writeCcc(GlucoseManager.RACP_CHARACTERISTIC,
                getOpCode(OP_CODE_ABORT_OPERATION, OPERATOR_NULL),
                this::onAbortWrite, this::onAbortWriteFailure);
    }

    private void onAbortWrite(byte[] bytes) {

        KLog.i("Data written to RACP, value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true));
    }

    private void onAbortWriteFailure(Throwable throwable) {

    }

    /**
     * Sends the request to delete all data from the device. A Record Access Control Point indication
     * with status code ({@link GlucoseManager#RESPONSE_SUCCESS} (or other in case of error) will be send.
     * <p>
     */
    public void deleteAllRecords() {

        if (null == getMustCccs2() || null == getMustCccs2().FilterCcc2) return;

        clear();
        onOperationStarted();

        writeCcc(GlucoseManager.RACP_CHARACTERISTIC,
                getOpCode(OP_CODE_DELETE_STORED_RECORDS, OPERATOR_ALL_RECORDS),
                this::onDeleteAllRecordsWrite, this::onDeleteAllRecordsWriteFailure);
    }

    private void onDeleteAllRecordsWrite(byte[] bytes) {

        KLog.i("Data written to RACP, value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true));
    }

    private void onDeleteAllRecordsWriteFailure(Throwable throwable) {

    }
    // endregion [Operation Records]

    // region [Operation UI]
    public void onOperationCompleted() {
        setOperationInProgress(false);

        runOnUiThread(() -> {
            final SparseArray<GlucoseRecord> records = getRecords();
            if (records.size() > 0) {
                final int unit = records.valueAt(0).unit;
                mTvUnit.setVisibility(View.VISIBLE);
                mTvUnit.setText(unit == GlucoseRecord.UNIT_kgpl ? R.string.gls_unit_mgpdl : R.string.gls_unit_mmolpl);
            } else {
                mTvUnit.setVisibility(View.GONE);
            }
            mAdapter.notifyDataSetChanged();
        });
    }

    public void onOperationStarted() {
        setOperationInProgress(true);
    }

    public void onOperationAborted() {
        setOperationInProgress(false);
    }

    public void onOperationNotSupported() {
        setOperationInProgress(false);
        showToast(R.string.gls_operation_not_supported);
    }

    public void onOperationFailed() {
        setOperationInProgress(false);
        showToast(R.string.gls_operation_failed);
    }

    public void onDatasetChanged() {
        // Do nothing. Refreshing the list is done in onOperationCompleted
    }

    public void onNumberOfRecordsRequested(final int value) {
        showToast(getResources().getQuantityString(R.plurals.gls_progress, value, value));
    }
    // endregion [Operation UI]

    private void setOperationInProgress(final boolean progress) {
        runOnUiThread(() -> {
            mViewControlStd.setVisibility(!progress ? View.VISIBLE : View.GONE);
            mViewControlAbort.setVisibility(progress ? View.VISIBLE : View.GONE);
        });
    }
    // endregion [Private Function]

    // region [EventBus]
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBleConnectionStateChange(BleEvents.BleConnectionState event) {

        KLog.i(event);

        switch (event.getState()) {
            case CONNECTING:
                break;
            case CONNECTED:
                break;
            case DISCONNECTED:
                setOperationInProgress(false);
                break;
            case DISCONNECTING:
                break;
            default:
                break;
        }
    }
    // endregion [EventBus]
}
