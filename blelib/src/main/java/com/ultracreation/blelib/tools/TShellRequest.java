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
    public String cmd;

    public TShellRequest(@NonNull String cmd, int timeOut, @NonNull RequestListener listener)
    {
        this.cmd = cmd;
        this.timeOut = timeOut;
        this.listener = listener;
    }

    /* called by TShell.RequestStart */
    abstract void start(Object[]... args);

    /* called by TShell */
    abstract void onNotification(String Line);

    void refreshTimeout()
    {
        clearTimeout();

        setTimeout();
    }

    void next(String datas)
    {
        clearTimeout();
        listener.onSuccessful(datas);
    }

    /// @override
    void error(String message)
    {
        clearTimeout();
        listener.onFailed(message);
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
        void onSuccessful(String value);

        void onFailed(String err);
    }
}