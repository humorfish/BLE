package com.ultracreation.blelib.tools;

import android.bluetooth.BluetoothProfile;


/**
 * Created by Administrator on 2016/12/19.
 */

public class TGapConnection extends IGapConnection {
    private String TAG = TGapConnection.class.getSimpleName();
    private INotification mNotification;
    private TService mSevice;

    public TGapConnection(String deviceId, TService mSevice, INotification mNotification) {
        this.deviceId = deviceId;
        this.mSevice = mSevice;
        this.mNotification = mNotification;
    }

    @Override
    void Disconnect() {
        if (isConnected())
            mSevice.disconnect();
    }

    @Override
    boolean isConnected() {
        if (mSevice == null)
            return false;

        if (mSevice.mConnectionState == BluetoothProfile.STATE_DISCONNECTED)
            return false;
        else
            return true;
    }

    @Override
    void SetTimeout(int Timeout, CallBack callBack) {

    }

    @Override
    void RefreshTimeout() {

    }

    @Override
    byte[] Read(String Service, String Characteristic) {
        return new byte[0];
    }

    @Override
    void Write(String Service, String Characteristic, byte[] buf) {

    }

    @Override
    void WriteNoResponse(String Service, String Characteristic, byte[] buf) {

    }

    @Override
    void StopNotification(String Service, String Characteristic) {

    }

    @Override
    void NotificationDisconnect() {

    }

    @Override
    void NotificationError(Object err) {

    }
}
