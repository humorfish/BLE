package com.ultracreation.ble;

import android.app.Application;

import com.ultracreation.blelib.tools.TShell;

/**
 * Created by Administrator on 2016/12/2.
 */

public class MyApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        TShell.instance.setContext();
    }
}
