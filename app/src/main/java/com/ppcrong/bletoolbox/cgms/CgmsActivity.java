package com.ppcrong.bletoolbox.cgms;

import android.os.Bundle;
import android.util.SparseArray;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.PopupMenu;

import com.polidea.rxandroidble2.helpers.ValueInterpreter;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.ppcrong.bletoolbox.gls.GlucoseManager;
import com.ppcrong.bletoolbox.parser.CGMMeasurementParser;
import com.ppcrong.bletoolbox.parser.CGMSpecificOpsControlPointParser;
import com.ppcrong.bletoolbox.parser.RecordAccessControlPointParser;
import com.ppcrong.utils.MiscUtils;
import com.socks.library.KLog;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.UUID;

import butterknife.BindView;
import butterknife.OnClick;
import io.reactivex.Observable;

import static com.ppcrong.bletoolbox.cgms.CgmsManager.FILTER_TYPE_SEQUENCE_NUMBER;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OPERATOR_ALL_RECORDS;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OPERATOR_FIRST_RECORD;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OPERATOR_GREATER_THEN_OR_EQUAL;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OPERATOR_LAST_RECORD;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OPERATOR_NULL;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OP_CODE_ABORT_OPERATION;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OP_CODE_DELETE_STORED_RECORDS;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OP_CODE_NUMBER_OF_STORED_RECORDS_RESPONSE;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OP_CODE_REPORT_NUMBER_OF_RECORDS;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OP_CODE_REPORT_STORED_RECORDS;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OP_CODE_RESPONSE_CODE;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.OP_CODE_START_SESSION;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.RESPONSE_ABORT_UNSUCCESSFUL;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.RESPONSE_NO_RECORDS_FOUND;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.RESPONSE_OP_CODE_NOT_SUPPORTED;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.RESPONSE_PROCEDURE_NOT_COMPLETED;
import static com.ppcrong.bletoolbox.cgms.CgmsManager.RESPONSE_SUCCESS;

public class CgmsActivity extends ProfileBaseActivity {

    // region [Variable]
    private SparseArray<CgmsRecord> mRecords = new SparseArray<>();
    private boolean mAbort;
    private long mSessionStartTime;
    private CgmsRecordsAdapter mCgmsRecordsAdapter;
    // endregion [Variable]

    // region [Widget]
    @BindView(R.id.list)
    ListView mRecordsListView;
    @BindView(R.id.view_control_std)
    View mViewControlStd;
    @BindView(R.id.view_control_abort)
    View mViewControlAbort;
    // endregion [Widget]

    // region [OnClick]
    @OnClick(R.id.btn_more)
    public void onClickMore(View v) {

        PopupMenu menu = new PopupMenu(CgmsActivity.this, v);
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

    private void clearRecords() {
        if (mCgmsRecordsAdapter != null) {
            mCgmsRecordsAdapter.clear();
            mCgmsRecordsAdapter.notifyDataSetChanged();
        }
    }

    @OnClick(R.id.btn_last)
    public void onClickLast() {

        onDatasetClear();
        getLastRecord();
    }

    @OnClick(R.id.btn_all)
    public void onClickAll() {

        clearRecords();
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

        setContentView(R.layout.activity_cgms);
    }

    @Override
    protected void onPreWorkDone() {

        KLog.i();

        // (1) Enable CGM_MEASUREMENT notification
        // (2) Enable CGM_OPS_CONTROL_POINT indication
        // (3) Write CCC with OP_CODE_START_SESSION
        // (4) Enable RACP indication
        setupCccNotification(getFilterCccUUID(), this::onCgmMeasurementNotificationSetupDone,
                this::onCgmMeasurementNotified, this::onCgmMeasurementNotifiedFailure);

        mSessionStartTime = System.currentTimeMillis();
    }

    private void onCgmMeasurementNotificationSetupDone(Observable<byte[]> observable) {

        KLog.i();

        // (1) Enable CGM_MEASUREMENT notification
        // (2) Enable CGM_OPS_CONTROL_POINT indication
        // (3) Write CCC with OP_CODE_START_SESSION
        // (4) Enable RACP indication
        setupCccIndication(getFilterCccUUID2(), this::onCgmOpsCpIndicationSetupDone,
                this::onCgmOpsCpIndicationIndicated, this::onCgmOpsCpIndicationIndicatedFailure);
    }

    private void onCgmOpsCpIndicationSetupDone(Observable<byte[]> observable) {

        KLog.i();

        // (1) Enable CGM_MEASUREMENT notification
        // (2) Enable CGM_OPS_CONTROL_POINT indication
        // (3) Write CCC with OP_CODE_START_SESSION
        // (4) Enable RACP indication
        writeCcc(CgmsManager.CGM_OPS_CONTROL_POINT_UUID,
                new byte[]{OP_CODE_START_SESSION},
                this::onOpCodeStartSessionWrite, this::onOpCodeStartSessionWriteFailure);
    }

    private void onOpCodeStartSessionWrite(byte[] bytes) {

        KLog.i("Data written to CGM_OPS_CONTROL_POINT_UUID, value: (0x) " +
                MiscUtils.getByteToHexString(bytes, ":", true));
        KLog.i("\"" + CGMSpecificOpsControlPointParser.parse(getMustCccs3().FilterCcc2) + "\" sent");

        // (1) Enable CGM_MEASUREMENT notification
        // (2) Enable CGM_OPS_CONTROL_POINT indication
        // (3) Write CCC with OP_CODE_START_SESSION
        // (4) Enable RACP indication
        setupCccIndication(getFilterCccUUID3(), this::onRcapIndicationSetupDone,
                this::onRcapIndicated, this::onRcapIndicatedFailure);
    }

    private void onOpCodeStartSessionWriteFailure(Throwable throwable) {

    }

    private void onRcapIndicationSetupDone(Observable<byte[]> observable) {

        KLog.i();

    }

    private void onCgmMeasurementNotified(byte[] bytes) {

        KLog.i("\"" + CGMMeasurementParser.parse(getMustCccs3().FilterCcc) + "\" received");

        // CGM Measurement characteristic may have one or more CGM records
        int totalSize = bytes.length;
        int offset = 0;
        while (offset < totalSize) {
            final int cgmSize = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, offset);
            final float cgmValue = ValueInterpreter.getFloatValue(bytes, ValueInterpreter.FORMAT_SFLOAT, offset + 2);
            final int sequenceNumber = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, offset + 4);
            final long timestamp = mSessionStartTime + (sequenceNumber * 60000L); // Sequence number is in minutes since Start Session

            //This will send callback to CGMSActivity when new concentration value is received from CGMS device
            final CgmsRecord cgmsRecord = new CgmsRecord(sequenceNumber, cgmValue, timestamp);
            mRecords.put(cgmsRecord.sequenceNumber, cgmsRecord);
            onCGMValueReceived(cgmsRecord);

            offset += cgmSize;
        }
    }

    private void onCgmOpsCpIndicationIndicated(byte[] bytes) {

        KLog.i("\"" + CGMSpecificOpsControlPointParser.parse(getMustCccs3().FilterCcc2) + "\" received");
    }

    private void onRcapIndicated(byte[] bytes) {

        KLog.i("\"" + RecordAccessControlPointParser.parse(getMustCccs3().FilterCcc3) + "\" received");

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

    }

    private void onCgmMeasurementNotifiedFailure(Throwable throwable) {

        KLog.i();
    }

    private void onCgmOpsCpIndicationIndicatedFailure(Throwable throwable) {

        KLog.i();
    }

    private void onRcapIndicatedFailure(Throwable throwable) {

        KLog.i();
    }

    @Override
    protected UUID getFilterSvcUUID() {
        return CgmsManager.CGMS_UUID;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return CgmsManager.CGM_MEASUREMENT_UUID;
    }

    @Override
    protected UUID getFilterCccUUID2() {
        return CgmsManager.CGM_OPS_CONTROL_POINT_UUID;
    }

    @Override
    protected UUID getFilterCccUUID3() {
        return CgmsManager.RACP_UUID;
    }

    @Override
    protected int getAboutTextId() {
        return R.string.cgms_about_text;
    }

    @Override
    protected void setDefaultUI() {
        super.setDefaultUI();

        clear();
    }

    // endregion [Override Function]

    // region [Private Function]

    private void loadAdapter(SparseArray<CgmsRecord> records) {
        mCgmsRecordsAdapter.clear();
        for (int i = 0; i < records.size(); i++) {
            mCgmsRecordsAdapter.addItem(records.valueAt(i));
        }
        mCgmsRecordsAdapter.notifyDataSetChanged();
    }

    /**
     * Writes given operation parameters to the characteristic
     *
     * @param opCode   the operation code
     * @param operator the operator (see {@link CgmsManager#OPERATOR_NULL} and others
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
     * Returns a list of CGM records obtained from this device. The key in the array is the
     */
    public SparseArray<CgmsRecord> getRecords() {
        return mRecords;
    }

    /**
     * Clears the records list locally
     */
    public void clear() {
        mRecords.clear();
        onDatasetClear();
    }

    /**
     * Sends the request to obtain the last (most recent) record from glucose device. The data will
     * be returned to Glucose Measurement characteristic as a notification followed by Record Access
     * Control Point indication with status code ({@link CgmsManager#RESPONSE_SUCCESS} or other in case of error.
     */
    public void getLastRecord() {

        if (null == getMustCccs3() || null == getMustCccs3().FilterCcc3) return;

        clear();
        onOperationStarted();

        writeCcc(GlucoseManager.RACP_CHARACTERISTIC,
                getOpCode(OP_CODE_REPORT_STORED_RECORDS, OPERATOR_LAST_RECORD),
                this::onOpCodeStartSessionWrite, this::onOpCodeStartSessionWriteFailure);
    }

    /**
     * Sends the request to obtain the first (oldest) record from glucose device. The data will be
     * returned to Glucose Measurement characteristic as a notification followed by Record Access
     * Control Point indication with status code ({@link CgmsManager#RESPONSE_SUCCESS} or other in case of error.
     */
    public void getFirstRecord() {

        if (null == getMustCccs3() || null == getMustCccs3().FilterCcc3) return;

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
     * Sends abort operation signal to the device
     */
    public void abort() {

        if (null == getMustCccs3() || null == getMustCccs3().FilterCcc3) return;

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
     * Sends the request to obtain all records from glucose device. Initially we want to notify
     * him/her about the number of the records so the {@link CgmsManager#OP_CODE_REPORT_NUMBER_OF_RECORDS}
     * is send. The data will be returned to Glucose Measurement characteristic as a notification
     * followed by Record Access Control Point indication with status code ({@link CgmsManager#RESPONSE_SUCCESS}
     * or other in case of error.
     */
    public void getAllRecords() {

        if (null == getMustCccs3() || null == getMustCccs3().FilterCcc3) return;

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
     * Sends the request to obtain all records from glucose device. Initially we want to notify
     * him/her about the number of the records so the {@link CgmsManager#OP_CODE_REPORT_NUMBER_OF_RECORDS}
     * is send. The data will be returned to Glucose Measurement characteristic as a notification
     * followed by Record Access Control Point indication with status code ({@link CgmsManager#RESPONSE_SUCCESS}
     * or other in case of error.
     */
    public void refreshRecords() {

        if (null == getMustCccs3() || null == getMustCccs3().FilterCcc3) return;

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
     * Sends the request to delete all data from the device. A Record Access Control Point indication
     * with status code ({@link GlucoseManager#RESPONSE_SUCCESS} (or other in case of error) will be send.
     * <p>
     */
    public void deleteAllRecords() {

        if (null == getMustCccs3() || null == getMustCccs3().FilterCcc3) return;

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
    public void onCGMValueReceived(final CgmsRecord record) {
        if (mCgmsRecordsAdapter == null) {
            mCgmsRecordsAdapter = new CgmsRecordsAdapter(this);
            mRecordsListView.setAdapter(mCgmsRecordsAdapter);
        }
        mCgmsRecordsAdapter.addItem(record);
        mCgmsRecordsAdapter.notifyDataSetChanged();
    }

    public void onOperationStarted() {
        // Update GUI
        setOperationInProgress(true);
    }

    public void onOperationCompleted() {
        // Update GUI
        setOperationInProgress(false);
    }

    public void onOperationFailed() {
        // Update GUI
        showToast(R.string.gls_operation_failed);
        // breakthrough intended
    }

    public void onOperationAborted() {
        // Update GUI
        setOperationInProgress(false);
    }

    public void onOperationNotSupported() {
        // Update GUI
        setOperationInProgress(false);
    }

    public void onDatasetClear() {
        // Update GUI
        clearRecords();
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
                final SparseArray<CgmsRecord> cgmsRecords = getRecords();
                if (cgmsRecords != null && cgmsRecords.size() > 0) {
                    if (mCgmsRecordsAdapter == null) {
                        mCgmsRecordsAdapter = new CgmsRecordsAdapter(this);
                        mRecordsListView.setAdapter(mCgmsRecordsAdapter);
                    }
                    loadAdapter(cgmsRecords);
                }
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
