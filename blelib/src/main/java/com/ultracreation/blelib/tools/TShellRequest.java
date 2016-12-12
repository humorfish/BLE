package com.ultracreation.blelib.tools;

import com.ultracreation.blelib.utils.KLog;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.ultracreation.blelib.tools.TShell.Shell;

/**
 * Created by you on 2016/12/7.
 */
abstract class TShellRequest{
    private final String TAG = "TShellRequest";

    protected int TimeoutInterval;
    private Timer timer;
    private TimerTask timeOutTask;
    protected Subject<String> mSubject;

    public TShellRequest(int Timeout) {
        TimeoutInterval = Timeout;
        mSubject = PublishSubject.create();
        refreshTimeout();
    }

    /* called by TShell.RequestStart */
    abstract void Start(String Cmd, Object[] ... args);

    /* called by TShell */
    abstract void Notification(String Line);

    void refreshTimeout() {
        KLog.i(TAG, "refreshTimeout.hasObservers:" + mSubject.hasObservers());
        if (mSubject.hasComplete() || mSubject.hasThrowable())
            return;

        // also delay Connection Timeout
        Shell.refreshConnectionTimeout();

        clearTimeout();

        if (TimeoutInterval == 0)
            return;

        setTimeout();
    }

    /* Subject */
    /// @override
    void complete() {
        KLog.i(TAG, "complete.hasObservers:" + mSubject.hasObservers());
        if (mSubject.hasObservers()) {
            mSubject.onComplete();
            refreshTimeout();
        }
    }

    void next(String datas) {
        KLog.i(TAG, "next.hasObservers:" + mSubject.hasObservers());
        if (mSubject.hasObservers())
            mSubject.onNext(datas);
    }

    /// @override
    void error(String message) {
        if (mSubject.hasObservers()) {
            mSubject.onError(new Throwable(message));
            refreshTimeout();
        }
    }

    void setTimeout() {
        if (timer == null)
            timer = new Timer();

        if (timeOutTask != null)
            timeOutTask.cancel();

        timeOutTask = null;
        timeOutTask = new TimerTask() {
            @Override
            public void run() {
                error("time out");
            }
        };
        timer.schedule(timeOutTask, TimeoutInterval);
    }

    void clearTimeout() {
        if (timeOutTask != null) {
            timeOutTask.cancel();
            timeOutTask = null;
        }

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}