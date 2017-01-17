package com.ultracreation.blelib.tools;

import android.support.annotation.NonNull;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.subjects.Subject;


/**
 * Created by you on 2016/12/7.
 */
public abstract class TShellRequest
{
    private Subject<byte[]> listener;
    private int timeOut;
    private Timer timer;
    private TimerTask timeOutTask;
    protected String cmd;

    public TShellRequest(@NonNull String cmd, int timeOut, @NonNull Subject<byte[]> listener)
    {
        this.cmd = cmd;
        this.timeOut = timeOut;
        this.listener = listener;
    }

    abstract void start(Object[]... args);

    abstract void onNotification(byte[] line);

    void refreshTimeout()
    {
        clearTimeout();

        setTimeout();
    }

    void next(byte[] datas)
    {
        clearTimeout();
        listener.onNext(datas);
        listener.onComplete();
    }

    /// @override
    void error(String message)
    {
        clearTimeout();
        listener.onError(new IllegalStateException(message));
    }

    private void setTimeout()
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
}