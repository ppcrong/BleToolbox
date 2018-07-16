package com.ppcrong.bletoolbox;

import android.app.Application;

import com.socks.library.KLog;

public class BleToolboxApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        KLog.init(BuildConfig.LOG_DEBUG, "BleToolboxDebug"); // Unify global debug flag
    }
}
