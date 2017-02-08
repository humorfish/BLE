package com.ultracreation.blelib.tools;

/**
 * Created by Administrator on 2016/12/4.
 */

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.ultracreation.blelib.service.BLEService;
import com.ultracreation.blelib.utils.KLog;


/**
 * Created by you on 2016/12/3.
 */
public enum TService
{
    Instance;

    private final String TAG = TService.class.getSimpleName();
    private boolean isServiceBound = false;
    public BLEService mSevice;

    TService(){}

    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {
            if (iBinder == null)
            {
                throw new Error("bind server failed!!");
            }

            mSevice = ((BLEService.LocalBinder) iBinder).getService();
            if (! mSevice.initialize())
            {
                KLog.e("mServiceConnection", "Unable to initialize Bluetooth");
                System.exit(0);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
        }
    };

    public boolean bindBluetoothSevice(@NonNull Context mContext)
    {
        if (! mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(mContext, "ble not support", Toast.LENGTH_SHORT).show();
            System.exit(0);
        }

        Intent gattServiceIntent = new Intent(mContext, BLEService.class);
        isServiceBound = mContext.getApplicationContext().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        KLog.i(TAG, "bindBluetoothSevice.isBindService:" + isServiceBound);
        return isServiceBound;
    }

    public void unBindBluetoothSevice(Context mContext)
    {
        KLog.i(TAG, "bindBluetoothSevice.isBindService:" + isServiceBound);
        if (isServiceBound)
            mContext.getApplicationContext().unbindService(mServiceConnection);
    }
}