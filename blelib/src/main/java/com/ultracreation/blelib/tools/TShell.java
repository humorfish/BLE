package com.ultracreation.blelib.tools;

import android.text.TextUtils;

import com.ultracreation.blelib.tools.TGapConnection.TShellCatRequest;
import com.ultracreation.blelib.tools.TGapConnection.TShellSimpleRequest;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;


/**
 * Created by Administrator on 2016/11/28.
 */

public class TShell
{
    private final static String TAG = TShell.class.getSimpleName();

    private String deviceId;
    private final int REQUEST_TIMEOUT = 3000;
    private TGapConnection mConnection;

    public TShell(String deviceId)
    {
        this.deviceId = deviceId;

        mConnection = TBLEConnectionManager.ConnectionManager.getConnection(deviceId);
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
