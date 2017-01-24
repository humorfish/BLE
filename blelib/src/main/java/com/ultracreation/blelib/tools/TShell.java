package com.ultracreation.blelib.tools;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import static com.ultracreation.blelib.tools.TConnectionManager.ConnectionManager;


/**
 * Created by Administrator on 2016/11/28.
 */

public class TShell
{
    private final static String TAG = TShell.class.getSimpleName();
    private String deviceId;

    public TShell(String deviceId)
    {
    }

    public void disconnect()
    {
        ConnectionManager.disconnect(deviceId);
    }

    public void connect()
    {
        ConnectionManager.connect(deviceId);
    }

    public Subject<byte[]> getVersion()
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = new TShellSimpleRequest(">ver", datas -> new String(datas).startsWith("v."), requestTimeout, listener);
        addRequest(request);

        start();
        return listener;
    }

    public Subject<byte[]> startOutput()
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = new TShellSimpleRequest(">osta", datas -> new String(datas).startsWith("0: ok"), requestTimeout, listener);
        addRequest(request);

        start();
        return listener;
    }

    public Subject<byte[]> stopOutput()
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = new TShellSimpleRequest(">osto", datas -> new String(datas).startsWith("0: ok"), requestTimeout, listener);
        addRequest(request);

        start();
        return listener;
    }

    public Subject<Integer> catFile(String fileName, byte[] fileData)
    {
        Subject<Integer> listener = PublishSubject.create();
        TShellCatRequest request = new TShellCatRequest<Integer>(">cat " + fileName + " -l=" + fileData.length, datas -> new String(datas).contains("3: end of cat"), requestTimeout, fileData, listener);
        addRequest(request);

        start();
        return listener;
    }
}
