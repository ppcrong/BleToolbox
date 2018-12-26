package com.ppcrong.bletoolbox.uart.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.eventbus.BleEvents;
import com.ppcrong.bletoolbox.uart.UartActivity;
import com.ppcrong.bletoolbox.uart.UartInterface;
import com.ppcrong.bletoolbox.uart.log.LogData;
import com.ppcrong.bletoolbox.uart.log.LogListAdapter;
import com.ppcrong.bletoolbox.uart.log.LogManager;
import com.socks.library.KLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Calendar;
import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class UartLogFragment extends Fragment {

    /**
     * The service UART interface that may be used to send data to the target.
     */
    private UartInterface mUartInterface;

    // region [Adapter]
    private LogListAdapter mLogListAdapter;
    private CopyOnWriteArrayList<LogData> mLogDataList = new CopyOnWriteArrayList<>();
    // endregion [Adapter]

    // region [Widget]
    @BindView(R.id.rv_log)
    RecyclerView mRvLogList;
    @BindView(R.id.tv_empty)
    TextView mTvEmpty;
    @BindView(R.id.field)
    EditText mField;
    @BindView(R.id.action_send)
    Button mSendButton;
    // endregion [Widget]

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Assign mUartInterface from parent activity
        if (getActivity() instanceof UartActivity) {

            mUartInterface = (UartActivity) getActivity();
        }

        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {

        EventBus.getDefault().unregister(this);

        super.onStop();

        try {
            mUartInterface = null;
        } catch (final IllegalArgumentException e) {
            // do nothing, we were not connected to the sensor
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_feature_uart_log, container, false);

        // Bind ButterKnife
        ButterKnife.bind(this, view);

        final EditText field = mField;
        field.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                onSendClicked();
                return true;
            }
            return false;
        });

        final Button sendButton = mSendButton;
        sendButton.setOnClickListener(v -> onSendClicked());
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // LogData list
        Context ctx = getActivity();
        mLogListAdapter = new LogListAdapter(ctx, mLogDataList);
        mRvLogList.setLayoutManager(new LinearLayoutManager(ctx));
        mRvLogList.setAdapter(mLogListAdapter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void onSendClicked() {
        final String text = mField.getText().toString();

        mUartInterface.send(text);
//        addLog(LogManager.Level.DEBUG, Calendar.getInstance().getTimeInMillis(), text);

        mField.setText(null);
        mField.requestFocus();
    }

    /**
     * This method is called when user closes the pane in horizontal orientation. The EditText is no longer visible so we need to close the soft keyboard here.
     */
    public void onFragmentHidden() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(mField.getWindowToken(), 0);
    }

    // region [Add log]
    private void addLog(@NonNull int level, @NonNull long time, @NonNull String data) {

        if (mTvEmpty.getVisibility() == View.VISIBLE) {

            mTvEmpty.setVisibility(View.GONE);
        }

        LogManager.addLog(level, time, data);
    }
    // endregion [Add log]

    // region [EventBus]
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBleConnectionStateChange(BleEvents.BleConnectionState event) {

        KLog.i(event);

        switch (event.getState()) {
            case CONNECTING:
                break;
            case CONNECTED:
                mField.setEnabled(true);
                mSendButton.setEnabled(true);
                break;
            case DISCONNECTED:
                mField.setEnabled(false);
                mSendButton.setEnabled(false);
                break;
            case DISCONNECTING:
                break;
            default:
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAddLog(LogData data) {

        KLog.i(data);

        // Add log to rv_log
        mLogDataList.add(data);
        mLogListAdapter.notifyDataSetChanged();
    }
    // endregion [EventBus]
}
