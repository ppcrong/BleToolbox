package com.ppcrong.bletoolbox.hrm;

import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.widget.TextView;

import com.polidea.rxandroidble2.helpers.ValueInterpreter;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;
import com.socks.library.KLog;

import org.achartengine.GraphicalView;

import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;

public class HrmActivity extends ProfileBaseActivity {

    // region [Variable]
    private final static String GRAPH_STATUS = "graph_status";
    private final static String GRAPH_COUNTER = "graph_counter";
    private final static String HR_VALUE = "hr_value";

    private final static int MAX_HR_VALUE = 65535;
    private final static int MIN_POSITIVE_VALUE = 0;
    private final static int REFRESH_INTERVAL = 1000; // 1 second interval

    private Handler mHandler = new Handler();

    private boolean isGraphInProgress = false;

    private GraphicalView mGraphView;
    private LineGraphView mLineGraph;

    private int mHrmValue = 0;
    private int mCounter = 0;
    // endregion [Variable]

    // region [Widget]
    @BindView(R.id.tv_hr_sensor_position)
    TextView mTvHrSensorPosition;
    @BindView(R.id.tv_hr)
    TextView mTvHr;
    @BindView(R.id.graph_hrs)
    ViewGroup mViewGroupGraphHrs;
    // endregion [Widget]

    // region [Life Cycle]
    @Override
    protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        isGraphInProgress = savedInstanceState.getBoolean(GRAPH_STATUS);
        mCounter = savedInstanceState.getInt(GRAPH_COUNTER);
        mHrmValue = savedInstanceState.getInt(HR_VALUE);

        if (isGraphInProgress)
            startShowGraph();
    }

    @Override
    protected void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(GRAPH_STATUS, isGraphInProgress);
        outState.putInt(GRAPH_COUNTER, mCounter);
        outState.putInt(HR_VALUE, mHrmValue);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        stopShowGraph();
    }
    // endregion [Life Cycle]

    // region [Override Function]
    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_hrm);

        // Bind ButterKnife
        ButterKnife.bind(this);

        setGUI();
    }

    @Override
    protected void onPreWorkDone() {

        KLog.i("Now read sensor location");

        // Read HR Sensor Location
        readCcc(HrmManager.HR_SENSOR_LOCATION_CHARACTERISTIC_UUID,
                this::onHrSensorLocationRead, this::onHrSensorLocationReadFailure);
    }

    @Override
    protected void onFilterCccNotified(byte[] bytes) {
        super.onFilterCccNotified(bytes);

        int hrValue;
        if (isHeartRateInUINT16(bytes[0])) {
            hrValue = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT16, 1);
        } else {
            hrValue = ValueInterpreter.getIntValue(bytes, ValueInterpreter.FORMAT_UINT8, 1);
        }

        // New HR value is received from HR device
        onHRValueReceived(hrValue);
    }

    @Override
    protected UUID getFilterSvcUUID() {
        return HrmManager.HR_SERVICE_UUID;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return HrmManager.HR_CHARACTERISTIC_UUID;
    }

    @Override
    protected void setDefaultUI() {

        super.setDefaultUI();
        mTvHr.setText(R.string.not_available_value);
        mTvHrSensorPosition.setText(R.string.not_available);
        clearGraph();
    }
    // endregion [Override Function]

    // region [Private Function]
    public void onHRValueReceived(int value) {
        mHrmValue = value;
        setHRSValueOnView(mHrmValue);
    }

    /**
     * This method will decode and return Heart rate sensor position on body
     */
    private String getBodySensorPosition(final byte bodySensorPositionValue) {
        final String[] locations = getResources().getStringArray(R.array.hrs_locations);
        if (bodySensorPositionValue > locations.length)
            return getString(R.string.hrs_location_other);
        return locations[bodySensorPositionValue];
    }

    /**
     * This method will check if Heart rate value is in 8 bits or 16 bits
     */
    private boolean isHeartRateInUINT16(final byte value) {
        return ((value & 0x01) != 0);
    }

    private void setHRSValueOnView(final int value) {
        runOnUiThread(() -> {
            if (value >= MIN_POSITIVE_VALUE && value <= MAX_HR_VALUE) {
                mTvHr.setText(Integer.toString(value));
            } else {
                mTvHr.setText(R.string.not_available_value);
            }
        });
    }

    private void setHrSensorPositionOnView(final String position) {
        runOnUiThread(() -> {
            if (position != null) {
                mTvHrSensorPosition.setText(position);
            } else {
                mTvHrSensorPosition.setText(R.string.not_available);
            }
        });
    }

    // region [Heart Rate Graph]
    private void setGUI() {
        mLineGraph = LineGraphView.getLineGraphView();
        showGraph();
    }

    private void showGraph() {
        mGraphView = mLineGraph.getView(this);
        mViewGroupGraphHrs.addView(mGraphView);
    }

    private void clearGraph() {
        mLineGraph.clearGraph();
        mGraphView.repaint();
        mCounter = 0;
        mHrmValue = 0;
    }

    private void updateGraph(final int hrmValue) {
        mCounter++;
        mLineGraph.addValue(new Point(mCounter, hrmValue));
        mGraphView.repaint();
    }

    private Runnable mRepeatTask = new Runnable() {
        @Override
        public void run() {
            if (mHrmValue > 0)
                updateGraph(mHrmValue);
            if (isGraphInProgress)
                mHandler.postDelayed(mRepeatTask, REFRESH_INTERVAL);
        }
    };

    void startShowGraph() {
        isGraphInProgress = true;
        mRepeatTask.run();
    }

    void stopShowGraph() {
        isGraphInProgress = false;
        mHandler.removeCallbacks(mRepeatTask);
    }
    // endregion [Heart Rate Graph]
    // endregion [Private Function]

    // region [Callback]
    private void onHrSensorLocationRead(byte[] bytes) {

        // HR sensor position on body is found in HR device
        final String sensorPosition = getBodySensorPosition(bytes[0]);
        setHrSensorPositionOnView(sensorPosition);
        KLog.i("Sensor Position: " + sensorPosition);

        // Start heart rate graph
        startShowGraph();

        // Enable notify
        setupFilterCccNotification();
    }

    private void onHrSensorLocationReadFailure(Throwable throwable) {

    }
    // endregion [Callback]
}
