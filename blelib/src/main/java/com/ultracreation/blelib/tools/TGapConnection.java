package com.ultracreation.blelib.tools;

import android.bluetooth.BluetoothProfile;
import android.support.annotation.NonNull;

import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by Administrator on 2016/12/19.
 */

public class TGapConnection extends IGapConnection
{
    private String TAG = TGapConnection.class.getSimpleName();
    private int connectTimeOut = 10000;

    private INotification mNotification;
    private TService mSevice;

    private Timer timer;
    private TimerTask timeOutTask;

    public TGapConnection(@NonNull String deviceId, @NonNull TService mSevice, @NonNull INotification mNotification)
    {
        this.deviceId = deviceId;
        this.mSevice = mSevice;
        this.mNotification = mNotification;

        this.connect();
    }

    @Override
    void connect()
    {
        refreshTimeOut();
        this.mSevice.makeConnection(deviceId, new INotification()
        {
            @Override
            public void onConnected()
            {
                clearTimeout();
                mNotification.onConnected();
            }

            @Override
            public void onConnectedFailed(String message)
            {
                clearTimeout();
                mNotification.onConnectedFailed(message);
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

    private void refreshTimeOut()
    {
        clearTimeout();

        setTimeout();
    }

    /// @override
    private void error(String message)
    {
        clearTimeout();
        mNotification.onConnectedFailed(message);
    }

    private void setTimeout()
    {
        timeOutTask = new TimerTask()
        {
            @Override
            public void run()
            {
                error("connect time out");
            }
        };

        timer = new Timer();
        timer.schedule(timeOutTask, connectTimeOut);
    }

    void clearTimeout()
    {
        if (timeOutTask != null)
        {
            timeOutTask.cancel();
            timeOutTask = null;
        }

        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
    }
}
