/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ppcrong.bletoolbox.uart.fragment;

import android.content.Context;
import android.os.Bundle;
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
import com.socks.library.KLog;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.CopyOnWriteArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import no.nordicsemi.android.log.LogContract;

public class UartLogFragment extends Fragment {
    private static final String SIS_LOG_SCROLL_POSITION = "sis_scroll_position";
    private static final int LOG_SCROLL_NULL = -1;
    private static final int LOG_SCROLLED_TO_BOTTOM = -2;

    private static final int LOG_REQUEST_ID = 1;
    private static final String[] LOG_PROJECTION = {LogContract.Log._ID, LogContract.Log.TIME, LogContract.Log.LEVEL, LogContract.Log.DATA};

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

        // Save the last log list view scroll position
//        final ListView list = getListView();
//        final boolean scrolledToBottom = list.getCount() > 0 && list.getLastVisiblePosition() == list.getCount() - 1;
//        outState.putInt(SIS_LOG_SCROLL_POSITION, scrolledToBottom ? LOG_SCROLLED_TO_BOTTOM : list.getFirstVisiblePosition());
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

        // Create the log adapter, initially with null cursor
//        mLogAdapter = new UartLogAdapter(getActivity());
//        setListAdapter(mLogAdapter);

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
//        addLog(LogListAdapter.Level.DEBUG, Calendar.getInstance().getTimeInMillis(), text);

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
    private void addLog(int level, long time, String data) {

        if (mTvEmpty.getVisibility() == View.VISIBLE) {

            mTvEmpty.setVisibility(View.GONE);
        }

        mLogDataList.add(new LogData.Builder().setLevel(level).setTime(time).setData(data).build());
        mLogListAdapter.notifyDataSetChanged();
    }

    private void addLog(LogData data) {

        mLogDataList.add(data);
        mLogListAdapter.notifyDataSetChanged();
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
    // endregion [EventBus]
}
