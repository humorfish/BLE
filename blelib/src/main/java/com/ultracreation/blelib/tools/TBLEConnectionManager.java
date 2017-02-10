package com.ultracreation.blelib.tools;

import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.ultracreation.blelib.impl.IConnectionManager;
import com.ultracreation.blelib.impl.INotification;
import com.ultracreation.blelib.service.BLEService;
import com.ultracreation.blelib.utils.KLog;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by you on 2017/1/19.
 */

public enum TBLEConnectionManager implements IConnectionManager
{
    ConnectionManager;

    private final String TAG = TBLEConnectionManager.class.getSimpleName();

    private Map<String, TGapConnection> mConnectionList;
    private BLEService mService;
    private boolean isServiceBound = false;

    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {
            if (iBinder == null)
            {
                throw new Error("bind server failed!!");
            }

            mService = ((BLEService.LocalBinder) iBinder).getService();
            KLog.i(TAG, "mService:" + mService);
            if (!mService.initialize())
            {
                KLog.e("mServiceConnection", "Unable to initialize Bluetooth");
                System.exit(0);
            }

            addDisconnectListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
        }
    };

    TBLEConnectionManager()
    {
        mConnectionList = new HashMap<>();
    }

    @Override
    public TGapConnection getConnection(@NonNull String deviceId)
    {
        if (mConnectionList.containsKey(deviceId))
        {
            return mConnectionList.get(deviceId);

        } else
        {
            return connect(deviceId);
        }
    }

    @Override
    public void disconnect(@NonNull String deviceId)
    {
        mService.disconnect(deviceId);
    }

    @Override
    public boolean isConnected(@NonNull String deviceId)
    {
        return mService.isConnected(deviceId);
    }

    @Override
    public TGapConnection connect(@NonNull String deviceId)
    {
        KLog.i(TAG, "-----3---->deviceId:"+ deviceId);

        TGapConnection connection = new TGapConnection(deviceId, mService);
        connection.mConnectionState = BluetoothProfile.STATE_CONNECTING;
        mConnectionList.put(deviceId, connection);

        mService.makeConnection(deviceId, new INotification()
        {
            @Override
            public void onConnected(String deviceId)
            {
                connection.mConnectionState = BluetoothProfile.STATE_CONNECTED;
                connection.start();
            }

            @Override
            public void onConnectedFailed(String deviceId, String message)
            {
                connection.mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                connection.destory();
                if (mConnectionList.containsKey(deviceId))
                    mConnectionList.remove(deviceId);
            }

            @Override
            public void onDisconnected(String deviceId)
            {
                connection.mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                if (mConnectionList.containsKey(deviceId))
                {
                    TGapConnection connection = mConnectionList.get(deviceId);
                    connection.destory();
                    mConnectionList.remove(deviceId);
                }
            }

            @Override
            public void onReceiveData(String deviceId, byte[] line)
            {
                if (mConnectionList.containsKey(deviceId))
                {
                    TGapConnection connection = mConnectionList.get(deviceId);
                    connection.receivedData(line);
                }
            }
        });

        return connection;
    }

    @Override
    public void clear()
    {
        mConnectionList.clear();
    }

    @Override
    public void addDisconnectListener()
    {
        mService.getDisconnectListenter().subscribe(deviceId ->
        {
            KLog.i(TAG, "addDisconnectListener.deviceId:" + deviceId);
            mConnectionList.remove(deviceId);
        });
    }

    @Override
    public void start(String deviceId)
    {
        TGapConnection connection = getConnection(deviceId);
        KLog.i(TAG, "deviceId:" + deviceId + "  mConnectionState:" + connection.mConnectionState);
        if (connection.mConnectionState ==  BluetoothProfile.STATE_CONNECTED)
        {
            KLog.i(TAG, "-----1---->deviceId:"+ deviceId);
            connection.start();

        } else if (connection.mConnectionState ==  BluetoothProfile.STATE_DISCONNECTED)
        {
            KLog.i(TAG, "-----2---->deviceId:"+ deviceId);
            connect(deviceId);
        }
    }

    public boolean bindBluetoothSevice(@NonNull Context mContext)
    {
        if (!mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
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
