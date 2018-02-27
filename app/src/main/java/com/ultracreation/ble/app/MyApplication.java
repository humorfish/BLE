package com.ultracreation.ble.app;

import android.app.Application;

import com.ultracreation.blelib.tools.TBLEConnectionManager;

/**
 * Created by Administrator on 2016/12/13.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate()
    {
        super.onCreate();
        TBLEConnectionManager.ConnectionManager.bindBluetoothSevice(this);
    }

    public void releaseBinding()
    {
        TBLEConnectionManager.ConnectionManager.unBindBluetoothSevice(this);
    }
}
