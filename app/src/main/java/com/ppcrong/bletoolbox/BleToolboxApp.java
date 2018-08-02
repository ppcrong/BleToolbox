package com.ppcrong.bletoolbox;

import android.app.Application;
import android.content.Context;

import com.lsxiao.apollo.core.Apollo;
import com.polidea.rxandroidble2.RxBleClient;
import com.polidea.rxandroidble2.internal.RxBleLog;
import com.socks.library.KLog;

import io.reactivex.android.schedulers.AndroidSchedulers;

public class BleToolboxApp extends Application {

    /**
     * In practise you will use some kind of dependency injection pattern.
     */
    private static volatile RxBleClient sInst = null;

    public static RxBleClient getRxBleClient(Context context) {
        RxBleClient inst = sInst;
        if (inst == null) {
            synchronized (BleToolboxApp.class) {
                inst = sInst;
                if (inst == null) {
                    inst = RxBleClient.create(context);
                    RxBleClient.setLogLevel(RxBleLog.VERBOSE);
                    sInst = inst;
                }
            }
        }
        return inst;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        KLog.init(BuildConfig.LOG_DEBUG, "BleToolboxDebug"); // Unify global debug flag
        Apollo.init(AndroidSchedulers.mainThread(), this, true);
    }
}
