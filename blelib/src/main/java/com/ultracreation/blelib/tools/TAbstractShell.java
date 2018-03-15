package com.ultracreation.blelib.tools;


import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2018/3/9.
 */

public abstract class TAbstractShell
{
    public static final String CONNECTED = "CONNECTED";
    public static final String DISCONNECTED = "DISCONNECTED";
    public static final String CONNECTING = "CONNECTING";

    private String status = DISCONNECTED;
    private Subject<String> statusSubject;

    protected TAbstractShell()
    {
        statusSubject = PublishSubject.create();
    }

    public void attach()
    {
    }

    public void detach()
    {
        status = DISCONNECTED;
    }

    public TGapConnection connect()
    {
        return null;
    }

    public void disconnect()
    {
    }

    protected Subject<String> afterConnected()
    {
        return statusSubject;
    }

    protected void refreshConnectionTimeout()
    {
    }
}
