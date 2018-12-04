package com.ppcrong.bletoolbox.uart;

import android.os.Bundle;

import com.ppcrong.bletoolbox.R;
import com.ppcrong.bletoolbox.base.ProfileBaseActivity;

import java.util.Locale;
import java.util.UUID;

import butterknife.ButterKnife;

public class UartActivity extends ProfileBaseActivity {

    // region [Variable]
    // endregion [Variable]

    // region [Widget]
//    @BindView(R.id.tv_speed)
//    TextView mTvSpeed;
    // endregion [Widget]

    // region [Override Function]
    @Override
    protected void onCreateView(Bundle savedInstanceState) {

        setContentView(R.layout.activity_uart);

        // Bind ButterKnife
        ButterKnife.bind(this);
    }

    @Override
    protected void onPreWorkDone() {

        setupFilterCccNotification();
    }

    @Override
    protected void onFilterCccNotified(byte[] bytes) {
        super.onFilterCccNotified(bytes);

    }

    @Override
    protected UUID getFilterSvcUUID() {
        return UartManager.UART_SERVICE_UUID;
    }

    @Override
    protected UUID getFilterCccUUID() {
        return UartManager.UART_TX_CHARACTERISTIC_UUID;
    }

    @Override
    protected UUID getFilterCccUUID2() {
        return UartManager.UART_RX_CHARACTERISTIC_UUID;
    }
    // endregion [Override Function]

    // region [Private Function]
    // endregion [Private Function]
}
