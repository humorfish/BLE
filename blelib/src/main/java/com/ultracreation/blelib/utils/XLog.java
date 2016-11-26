/*
 * XLog.java
 *
 * yikui@e-tek.cn
 *
 * Created by:
 * 2012-06-03-AM
 *
 * Log can with global turn on / off.
 */


package com.ultracreation.blelib.utils;

import android.util.Log;

public class XLog {

	public static boolean mOn = true;

	public static void v(String tag, String content) {
		if (mOn) Log.v(tag, content);
	}

	public static void d(String tag, String content) {
        if (mOn) Log.d(tag, content);
    }

	public static void i(String tag, String content) {
        if (mOn) Log.i(tag, content);
    }

	public static void w(String tag, String content) {
        if (mOn) Log.w(tag, content);
    }

	public static void e(String tag, String content) {
        if (mOn) Log.e(tag, content);
    }

	public static void turnOn() {
		mOn = true;
	}

	public static void turnOff() {
		mOn = false;
	}

	public static boolean isOn() {
		return mOn;
	}
}
