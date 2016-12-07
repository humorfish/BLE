package com.ultracreation.blelib.tools;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by you on 2016/12/7.
 */
abstract class TShellRequest{
    protected TShell shell;
    protected int TimeoutInterval;
    private Timer timer;
    private TimerTask timeOutTask;
    private Disposable disposable;
    protected Subject<String> mSubject;

    public TShellRequest(TShell shell, int Timeout, String cmd) {
        this.shell = shell;
        TimeoutInterval = Timeout;
        mSubject = PublishSubject.create();
        disposable = shell.disposableMap.get(cmd);
        refreshTimeout();
    }

    /* called by TShell.RequestStart */
    abstract void Start(String Cmd, CallBack callBack, Object[] ... args);

    /* called by TShell */
    abstract void Notification(String Line);

    void refreshTimeout() {
        if (disposable != null && disposable.isDisposed())
            return;

        // also delay Connection Timeout
        shell.refreshConnectionTimeout();

        clearTimeout();

        if (TimeoutInterval == 0)
            return;

        setTimeout();
    }

    private void Disponse() {
        clearTimeout();

        if (disposable != null && ! disposable.isDisposed())
            disposable.dispose();

        shell = null;
    }

    /* Subject */
    /// @override
    void complete() {
        if (disposable != null && !disposable.isDisposed()) {
            mSubject.onComplete();
            Disponse();
        }
    }

    void next(String datas) {
        if (disposable != null && !disposable.isDisposed())
            mSubject.onNext(datas);
    }

    /// @override
    void error() {
        if (disposable != null && !disposable.isDisposed()) {
            mSubject.onError(new Error("time out"));
            Disponse();
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
                error();
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