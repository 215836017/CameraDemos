package com.cakes.democamera2.utils;

import android.util.Log;

public class LogUtil {

    public static boolean isWriteLog = true;

    public static void setIsWriteLog(boolean flag) {
        isWriteLog = flag;
    }

    private static final String LOG_TAG = "demoCamera2_log ";

    public static void v(String tag, String msg) {
        if (isWriteLog) {
            Log.v(tag, LOG_TAG + msg);
        }
    }

    public static void w(String tag, String msg) {
        if (isWriteLog) {
            Log.w(tag, LOG_TAG + msg);
        }
    }

    public static void i(String tag, String msg) {
        if (isWriteLog) {
            Log.i(tag, LOG_TAG + msg);
        }
    }

    public static void d(String tag, String msg) {
        if (isWriteLog) {
            Log.d(tag, LOG_TAG + msg);
        }
    }

    public static void e(String tag, String msg) {
        if (isWriteLog) {
            Log.e(tag, LOG_TAG + msg);
        }
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (isWriteLog) {
            Log.e(tag, LOG_TAG + msg, tr);
        }
    }
}