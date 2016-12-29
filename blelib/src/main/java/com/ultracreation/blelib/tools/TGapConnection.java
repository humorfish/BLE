package com.ultracreation.blelib.tools;

import android.bluetooth.BluetoothProfile;


/**
 * Created by Administrator on 2016/12/19.
 */

public class TGapConnection extends IGapConnection
{
    private String TAG = TGapConnection.class.getSimpleName();
    private INotification mNotification;
    private TService mSevice;

    public TGapConnection(String deviceId, TService mSevice, INotification mNotification)
    {
        this.deviceId = deviceId;
        this.mSevice = mSevice;
        this.mNotification = mNotification;

        this.connect();
    }

    @Override
    void connect()
    {
        this.mSevice.makeConnection(deviceId, new INotification()
        {
            @Override
            public void onConnected()
            {
                mNotification.onConnected();
            }

            @Override
            public void onConnectedFailed()
            {
                mNotification.onConnectedFailed();
            }

            @Override
            public void onDisconnected()
            {
                mNotification.onDisconnected();
            }

            @Override
            public void onReceiveData(String Line)
            {
                mNotification.onReceiveData(Line);
            }
        });
    }

    @Override
    void disconnect()
    {
        if (isConnected())
            mSevice.disconnect();
    }

    @Override
    boolean isConnected()
    {
        if (mSevice.mConnectionState == BluetoothProfile.STATE_DISCONNECTED)
            return false;
        else
            return true;
    }

    @Override
    void write(String cmd)
    {
        if (mSevice.mConnectionState == BluetoothProfile.STATE_CONNECTED)
        {
            cmd = cmd + "\r\n";
            this.mSevice.write(cmd.getBytes());
        }
    }

    @Override
    void writeNoResponse(byte[] buf)
    {

    }
}
