package com.ultracreation.blelib.tools;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * Created by you on 2016/12/7.
 */
abstract class TShellRequest extends Observable<Object> {
    protected TShell shell;
    protected int TimeoutInterval;
    private Timer timer;
    private TimerTask timeOutTask;
    private Disposable disposable;

    public TShellRequest(TShell shell, int Timeout, String tag) {
        this.shell = shell;
        TimeoutInterval = Timeout;
        disposable = shell.disposableMap.get(tag);
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
            complete();
            Disponse();
        }
    }

    void next(String datas) {
        if (disposable != null && !disposable.isDisposed())
        {

        }
    }

    /// @override
    void error() {
        if (disposable != null && !disposable.isDisposed()) {
            doOnError(new )
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