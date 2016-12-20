package com.ultracreation.blelib.tools;

import android.bluetooth.BluetoothProfile;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;


/**
 * Created by Administrator on 2016/12/19.
 */

public class TGapConnection extends IGapConnection {
    private String TAG = TGapConnection.class.getSimpleName();
    private INotification mNotification;
    private TService mSevice;
    private Subject<String> mSubject;

    public TGapConnection(String deviceId, TService mSevice, INotification mNotification) {
        this.deviceId = deviceId;
        this.mSevice = mSevice;
        this.mNotification = mNotification;
        this.mSubject = PublishSubject.create();

        this.connect();
    }

    @Override
    void connect() {
        TShell.Shell.refreshConnectionTimeout();

        this.mSevice.makeConnection(deviceId, new INotification() {
            @Override
            public void onConnected() {
                mNotification.onConnected();
                TShell.Shell.clearConnectTimeOut();
            }

            @Override
            public void onConnectedFailed() {
                mNotification.onConnectedFailed();
                TShell.Shell.clearConnectTimeOut();
            }

            @Override
            public void onDisconnected() {
                mNotification.onDisconnected();
                TShell.Shell.clearConnectTimeOut();
            }

            @Override
            public void onReceiveData(String Line) {
                mNotification.onReceiveData(Line);
                refreshTimeout();
            }
        });
    }

    @Override
    void disconnect() {
        if (isConnected())
            mSevice.disconnect();
    }

    @Override
    boolean isConnected() {
        if (mSevice.mConnectionState == BluetoothProfile.STATE_DISCONNECTED)
            return false;
        else
            return true;
    }

    @Override
    void setTimeout(int Timeout, CallBack callBack) {

    }

    @Override
    void refreshTimeout() {

    }

    @Override
    void write(String cmd) {
        if (mSevice.mConnectionState == BluetoothProfile.STATE_CONNECTED) {
            cmd = cmd + "\r\n";
            this.mSevice.write(cmd.getBytes());
        }
    }

    @Override
    void writeNoResponse(byte[] buf) {

    }
}
