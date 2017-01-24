package com.ultracreation.blelib.tools;

import android.support.annotation.NonNull;

import java.util.Timer;
import java.util.TimerTask;
import io.reactivex.subjects.Subject;


/**
 * Created by you on 2016/12/7.
 */
public abstract class TShellRequest<T>
{
    protected Subject<T> listener;
    protected int timeOut;
    protected Timer timer;
    protected TimerTask timeOutTask;
    protected String cmd;

    public TShellRequest(@NonNull String cmd, int timeOut, @NonNull Subject<T> listener)
    {
        this.cmd = cmd;
        this.timeOut = timeOut;
        this.listener = listener;
    }

    abstract void start();

    abstract void onNotification(byte[] line);

    void refreshTimeout()
    {
        clearTimeout();

        setTimeout();
    }

    void onNext(T datas)
    {
        listener.onNext(datas);
    }

    void onComplete()
    {
        clearTimeout();
        listener.onComplete();
    }

    void onError(String message)
    {
        clearTimeout();
        listener.onError(new IllegalStateException(message));
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
                onError(cmd + " request time out");
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