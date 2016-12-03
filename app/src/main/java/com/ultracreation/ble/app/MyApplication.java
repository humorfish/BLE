package com.ultracreation.ble.app;

import android.app.Application;

import com.ultracreation.blelib.tools.TShell;

/**
 * Created by you on 2016/12/1.
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        TShell.instance.setContext(this);
    }

}
