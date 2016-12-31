package com.ultracreation.ble.app;

import android.app.Application;

import com.ultracreation.blelib.tools.TShell;

/**
 * Created by Administrator on 2016/12/13.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate()
    {
        super.onCreate();
        TShell.Shell.bindBluetoothSevice(this);
    }

    public void releaseBinding()
    {
        TShell.Shell.unBindBluetoothSevice();
    }
}
