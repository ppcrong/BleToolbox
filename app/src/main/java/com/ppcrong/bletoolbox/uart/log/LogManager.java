package com.ppcrong.bletoolbox.uart.log;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.SparseIntArray;

import org.greenrobot.eventbus.EventBus;

import java.util.Calendar;

/**
 * LogManager
 */

public class LogManager {

    public static final SparseIntArray LOG_COLORS = new SparseIntArray();

    static {
        LOG_COLORS.put(Level.DEBUG, 0xFF009CDE);
        LOG_COLORS.put(Level.VERBOSE, 0xFFB8B056);
        LOG_COLORS.put(Level.INFO, Color.BLACK);
        LOG_COLORS.put(Level.APPLICATION, 0xFF238C0F);
        LOG_COLORS.put(Level.WARNING, 0xFFD77926);
        LOG_COLORS.put(Level.ERROR, Color.RED);
    }

    /**
     * A helper class that contains predefined static level values:
     * <ul>
     * <li>{@link #DEBUG}</li>
     * <li>{@link #VERBOSE}</li>
     * <li>{@link #INFO}</li>
     * <li>{@link #APPLICATION}</li>
     * <li>{@link #WARNING}</li>
     * <li>{@link #ERROR}</li>
     * </ul>
     */
    public final class Level {
        /**
         * Level used just for debugging purposes. It has lowest level
         */
        public final static int DEBUG = 0;
        /**
         * Log entries with minor importance
         */
        public final static int VERBOSE = 1;
        /**
         * Default logging level for important entries
         */
        public final static int INFO = 5;
        /**
         * Log entries level for applications
         */
        public final static int APPLICATION = 10;
        /**
         * Log entries with high importance
         */
        public final static int WARNING = 15;
        /**
         * Log entries with very high importance, like errors
         */
        public final static int ERROR = 20;

        private Level() {
            // empty
        }
    }

    /**
     * Add log to event bus
     *
     * @param data
     */
    static public void addLog(@NonNull LogData data) {

        EventBus.getDefault().post(data);
    }

    /**
     * Add log to event bus
     *
     * @param level
     * @param data
     */
    static public void addLog(@NonNull int level, @NonNull String data) {

        EventBus.getDefault().post(new LogData.Builder()
                .setLevel(level)
                .setTime(Calendar.getInstance().getTimeInMillis())
                .setData(data)
                .build());
    }

    /**
     * Add log to event bus
     * @param level
     * @param time
     * @param data
     */
    static public void addLog(@NonNull int level, @NonNull long time, @NonNull String data) {

        EventBus.getDefault().post(new LogData.Builder()
                .setLevel(level)
                .setTime(time)
                .setData(data)
                .build());
    }
}
