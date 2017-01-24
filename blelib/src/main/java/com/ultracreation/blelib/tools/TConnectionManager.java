package com.ultracreation.blelib.tools;

import android.support.annotation.NonNull;

import com.ultracreation.blelib.impl.IConnectionManager;
import com.ultracreation.blelib.impl.INotification;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by you on 2017/1/19.
 */

public enum TConnectionManager implements IConnectionManager
{
    ConnectionManager;

    private final static String TAG = TConnectionManager.class.getSimpleName();
    private Map<String, TGapConnection> mConnectionList;

    TConnectionManager()
    {
        mConnectionList = new HashMap<>();
    }

    @Override
    public void disconnect(@NonNull String deviceId)
    {
        if (isConnected(deviceId))
            TService.Instance.mSevice.disconnect(deviceId);
    }

    @Override
    public boolean isConnected(@NonNull String deviceId)
    {
        return TService.Instance.mSevice.isConnected(deviceId);
    }

    @Override
    public void connect(@NonNull String deviceId)
    {
        TService.Instance.mSevice.makeConnection(deviceId, new INotification()
        {
            @Override
            public void onConnected(String deviceId)
            {
                TGapConnection connection = new TGapConnection(deviceId);
                mConnectionList.put(deviceId, connection);
                connection.start();
            }

            @Override
            public void onConnectedFailed(String deviceId, String message)
            {
                mConnectionList.remove(deviceId);
            }

            @Override
            public void onDisconnected(String deviceId)
            {
                if (mConnectionList.containsKey(deviceId))
                {
                    TGapConnection connection = mConnectionList.get(deviceId);
                    connection.destory();
                }

                mConnectionList.remove(deviceId);
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
    }

    @Override
    public void clear()
    {
        mConnectionList.clear();
    }
}
