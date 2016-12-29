package com.ultracreation.blelib.tools;

import com.ultracreation.blelib.utils.KLog;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by you on 2016/12/7.
 */
public abstract class TShellRequest
{
    private final String TAG = "TShellRequest";

    protected int TimeoutInterval;
    protected Subject<String> mSubject;
    private Timer timer;
    private TimerTask timeOutTask;

    public TShellRequest(int Timeout)
    {
        TimeoutInterval = Timeout;
        mSubject = PublishSubject.create();
    }

    /* called by TShell.RequestStart */
    abstract void start(String Cmd, Object[]... args);

    /* called by TShell */
    abstract void onNotification(String Line);

    void refreshTimeout()
    {
        KLog.i(TAG, "refreshTimeout.hasObservers:" + mSubject.hasObservers());
        if (mSubject.hasComplete() || mSubject.hasThrowable())
            return;

        clearTimeout();

        if (TimeoutInterval == 0)
            return;

        setTimeout();
    }

    /* Subject */
    /// @override
    void complete()
    {
        KLog.i(TAG, "complete.hasObservers:" + mSubject.hasObservers());
        if (mSubject.hasObservers())
        {
            mSubject.onComplete();
            refreshTimeout();
        }
    }

    void next(String datas)
    {
        KLog.i(TAG, "next.hasObservers:" + mSubject.hasObservers());
        if (mSubject.hasObservers())
            mSubject.onNext(datas);
    }

    /// @override
    void error(String message)
    {
        if (mSubject.hasObservers())
        {
            mSubject.onError(new Throwable(message));
            refreshTimeout();
        }
    }

    void setTimeout()
    {
        if (timer == null)
            timer = new Timer();

        if (timeOutTask != null)
            timeOutTask.cancel();

        timeOutTask = null;
        timeOutTask = new TimerTask()
        {
            @Override
            public void run()
            {
                error("time out");
            }
        };
        timer.schedule(timeOutTask, TimeoutInterval);
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