package com.ultracreation.ble.app;

import android.app.Application;

import static com.ultracreation.blelib.tools.TShell.Shell;

/**
 * Created by Administrator on 2016/12/13.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Shell.bindBluetoothSevice(this);
    }
}
