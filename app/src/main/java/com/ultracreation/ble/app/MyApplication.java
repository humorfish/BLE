package com.ultracreation.ble.app;

import android.app.Application;

import com.ultracreation.blelib.tools.TService;

/**
 * Created by Administrator on 2016/12/13.
 */

public class MyApplication extends Application {

    @Override
    public void onCreate()
    {
        super.onCreate();
        TService.Instance.bindBluetoothSevice(this);
    }

    public void releaseBinding()
    {
        TService.Instance.unBindBluetoothSevice(this);
    }
}
