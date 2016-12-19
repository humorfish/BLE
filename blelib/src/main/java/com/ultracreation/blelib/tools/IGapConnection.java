package com.ultracreation.blelib.tools;

/**
 * Created by Administrator on 2016/12/15.
 */

public abstract class IGapConnection
{
    String deviceId;

    abstract void Disconnect();
    abstract boolean isConnected();
    abstract void SetTimeout(int Timeout, CallBack callBack);
    abstract void RefreshTimeout();

    abstract byte[] Read(String Service, String Characteristic);
    abstract void Write(String Service, String Characteristic, byte[] buf);
    abstract void WriteNoResponse(String Service, String Characteristic, byte[] buf);

    abstract void StopNotification(String Service, String Characteristic);

    abstract void NotificationDisconnect();
    abstract void NotificationError(Object err);
}