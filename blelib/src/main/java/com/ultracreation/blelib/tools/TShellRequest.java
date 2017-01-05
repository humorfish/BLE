package com.ultracreation.blelib.tools;

import android.support.annotation.NonNull;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by you on 2016/12/7.
 */
public abstract class TShellRequest
{
    private RequestListener listener;
    private int timeOut;
    private Timer timer;
    private TimerTask timeOutTask;
    protected String cmd;

    public TShellRequest(@NonNull String cmd, int timeOut, @NonNull RequestListener listener)
    {
        this.cmd = cmd;
        this.timeOut = timeOut;
        this.listener = listener;
    }

    abstract void start(Object[]... args);

    abstract void onNotification(String Line);

    void refreshTimeout()
    {
        clearTimeout();

        setTimeout();
    }

    void next(String datas)
    {
        clearTimeout();
        listener.onSuccess(datas);
    }

    /// @override
    void error(String message)
    {
        clearTimeout();
        listener.onFailure(message);
    }

    void setTimeout()
    {
        if (timeOut == 0)
            return;

        timeOutTask = new TimerTask()
        {
            @Override
            public void run()
            {
                error(cmd + " request time out");
            }
        };

        timer = new Timer();
        timer.schedule(timeOutTask, timeOut);
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


    public interface RequestListener
    {
        void onSuccess(String value);

        void onFailure(String err);
    }
}