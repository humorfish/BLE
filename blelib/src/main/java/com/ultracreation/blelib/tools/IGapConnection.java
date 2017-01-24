package com.ultracreation.blelib.tools;

import android.support.annotation.NonNull;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;

/**
 * Created by Administrator on 2016/12/15.
 */

public abstract class IGapConnection
{
    String deviceId;
    private Timer timer;
    private TimerTask timeOutTask;
    private int connectTimeout = 10000;

    public IGapConnection(@NonNull String deviceId)
    {
        this.deviceId = deviceId;
    }

    abstract void start();

    abstract void addRequest(TShellRequest request);

    abstract void writeCmd(String cmd);

    abstract Observable<Integer> writeBuf(byte[] buf);

    abstract void receivedData(byte[] datas);

    abstract void destory();

    void refreshTimeout()
    {
        clearTimeout();

        setTimeout();
    }

    /// @override
    void error(String message)
    {
        clearTimeout();
    }

    void setTimeout()
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
        timer.schedule(timeOutTask, connectTimeout);
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