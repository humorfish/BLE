package com.ultracreation.blelib.tools;

import android.text.TextUtils;

import com.ultracreation.blelib.tools.TGapConnection.TShellCatRequest;
import com.ultracreation.blelib.tools.TGapConnection.TShellSimpleRequest;

import java.util.HashMap;
import java.util.Map;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;


/**
 * Created by Administrator on 2016/11/28.
 */

public class TShell extends TAbstractShell
{
    private final static String TAG = TShell.class.getSimpleName();

    private String deviceId;
    private final int REQUEST_TIMEOUT = 3000;
    private int ConnectionTimeout = 5;
    private TGapConnection mConnection;
    private static Map<String, TShell> cache = new HashMap<>();

    private TShell(String deviceId, int connectionTimeout)
    {
        super();

        this.deviceId = deviceId;
        this.ConnectionTimeout = connectionTimeout;
    }

    public static TShell Get(String deviceId, int connectionTimeout)
    {
        TShell shell = cache.get(deviceId);
        if (shell == null)
        {
            shell = new TShell(deviceId, connectionTimeout);
            cache.put(deviceId, shell);
        }

        return shell;
    }

    public TGapConnection connect()
    {
        return mConnection;
    }

    public void disconnect()
    {
        if (! TextUtils.isEmpty(deviceId))
            TBLEConnectionManager.ConnectionManager.disconnect(deviceId);
    }

    public Subject<byte[]> getVersion()
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = mConnection.new TShellSimpleRequest(">ver", datas -> new String(datas).startsWith("v."), REQUEST_TIMEOUT, listener);
        mConnection.addRequest(request);

        TBLEConnectionManager.ConnectionManager.start(deviceId);
        return listener;
    }

    public Subject<byte[]> startOutput()
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = mConnection.new TShellSimpleRequest(">osta", datas -> new String(datas).startsWith("0: ok"), REQUEST_TIMEOUT, listener);
        mConnection.addRequest(request);

        TBLEConnectionManager.ConnectionManager.start(deviceId);
        return listener;
    }

    public Subject<byte[]> stopOutput()
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = mConnection.new TShellSimpleRequest(">osto", datas -> new String(datas).startsWith("0: ok"), REQUEST_TIMEOUT, listener);
        mConnection.addRequest(request);

        TBLEConnectionManager.ConnectionManager.start(deviceId);
        return listener;
    }

    public Subject<Integer> catFile(String fileName, byte[] fileData)
    {
        Subject<Integer> listener = PublishSubject.create();
        TShellCatRequest request = mConnection.new TShellCatRequest<Integer>(">cat " + fileName + " -l=" + fileData.length, datas -> new String(datas).contains("3: end of cat"), REQUEST_TIMEOUT, fileData, listener);
        mConnection.addRequest(request);

        TBLEConnectionManager.ConnectionManager.start(deviceId);
        return listener;
    }

}
