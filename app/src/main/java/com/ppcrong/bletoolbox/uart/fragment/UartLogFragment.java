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
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.lsxiao.apollo.core.Apollo;
import com.lsxiao.apollo.core.annotations.Receive;
import com.lsxiao.apollo.core.contract.ApolloBinder;
import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.apollo.BleEvents;
import com.ppcrong.bletoolbox.uart.UartActivity;
import com.ppcrong.bletoolbox.uart.UartInterface;
import com.ppcrong.bletoolbox.uart.adapter.UartLogAdapter;
import com.socks.library.KLog;

import no.nordicsemi.android.log.ILogSession;
import no.nordicsemi.android.log.LogContract;

public class UartLogFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String SIS_LOG_SCROLL_POSITION = "sis_scroll_position";
    private static final int LOG_SCROLL_NULL = -1;
    private static final int LOG_SCROLLED_TO_BOTTOM = -2;

    private static final int LOG_REQUEST_ID = 1;
    private static final String[] LOG_PROJECTION = {LogContract.Log._ID, LogContract.Log.TIME, LogContract.Log.LEVEL, LogContract.Log.DATA};

    /**
     * The service UART interface that may be used to send data to the target.
     */
    private UartInterface mUARTInterface;
    /**
     * The adapter used to populate the list with log entries.
     */
    private CursorAdapter mLogAdapter;
    /**
     * The log session created to log events related with the target device.
     */
    private ILogSession mLogSession;

    private EditText mField;
    private Button mSendButton;

    /**
     * The last list view position.
     */
    private int mLogScrollPosition;

    /**
     * Apollo
     */
    private ApolloBinder mBinder;

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mCommonBroadcastReceiver, makeIntentFilter());

        // Load the last log list view scroll position
        if (savedInstanceState != null) {
            mLogScrollPosition = savedInstanceState.getInt(SIS_LOG_SCROLL_POSITION);
        }

        // Apollo
        mBinder = Apollo.bind(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Assign mUartInterface from parent activity
        if (getActivity() instanceof UartActivity) {

            mUARTInterface = (UartActivity) getActivity();
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        try {
            mUARTInterface = null;
        } catch (final IllegalArgumentException e) {
            // do nothing, we were not connected to the sensor
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        // Save the last log list view scroll position
        final ListView list = getListView();
        final boolean scrolledToBottom = list.getCount() > 0 && list.getLastVisiblePosition() == list.getCount() - 1;
        outState.putInt(SIS_LOG_SCROLL_POSITION, scrolledToBottom ? LOG_SCROLLED_TO_BOTTOM : list.getFirstVisiblePosition());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Apollo
        if (mBinder != null) {
            mBinder.unbind();
        }
    }

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_feature_uart_log, container, false);

        final EditText field = mField = view.findViewById(R.id.field);
        field.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                onSendClicked();
                return true;
            }
            return false;
        });

        final Button sendButton = mSendButton = view.findViewById(R.id.action_send);
        sendButton.setOnClickListener(v -> onSendClicked());
        return view;
    }

    @Override
    public void onViewCreated(final View view, final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Create the log adapter, initially with null cursor
        mLogAdapter = new UartLogAdapter(getActivity());
        setListAdapter(mLogAdapter);
    }

    @Override
    public Loader<Cursor> onCreateLoader(final int id, final Bundle args) {
        switch (id) {
            case LOG_REQUEST_ID: {
                return new CursorLoader(getActivity(), mLogSession.getSessionEntriesUri(), LOG_PROJECTION, null, null, LogContract.Log.TIME);
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(final Loader<Cursor> loader, final Cursor data) {
        // Here we have to restore the old saved scroll position, or scroll to the bottom if before adding new events it was scrolled to the bottom.
        final ListView list = getListView();
        final int position = mLogScrollPosition;
        final boolean scrolledToBottom = position == LOG_SCROLLED_TO_BOTTOM || (list.getCount() > 0 && list.getLastVisiblePosition() == list.getCount() - 1);

        mLogAdapter.swapCursor(data);

        if (position > LOG_SCROLL_NULL) {
            list.setSelectionFromTop(position, 0);
        } else {
            if (scrolledToBottom)
                list.setSelection(list.getCount() - 1);
        }
        mLogScrollPosition = LOG_SCROLL_NULL;
    }

    @Override
    public void onLoaderReset(final Loader<Cursor> loader) {
        mLogAdapter.swapCursor(null);
    }

    private void onSendClicked() {
        final String text = mField.getText().toString();

        mUARTInterface.send(text);

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

    // region [Apollo]
    @Receive("BleEvents.NotifyBleConnectionStateEvent")
    public void onNotifyBleStateUartLogFragment(BleEvents.NotifyBleConnectionStateEvent event) {

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
    // endregion [Apollo]
}
