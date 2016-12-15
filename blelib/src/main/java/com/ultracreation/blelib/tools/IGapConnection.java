package com.ultracreation.blelib.tools;

/**
 * Created by Administrator on 2016/12/15.
 */

public abstract class IGapConnection
{
    String DeviceId;

    abstract void Disconnect();
    abstract void SetTimeout(int Timeout, CallBack callBack);
    abstract void RefreshTimeout();

    abstract byte[] Read(String Service, String Characteristic);
    abstract void Write(String Service, String Characteristic, byte[] buf);
    abstract void WriteNoResponse(String Service, String Characteristic, byte[] buf);

    TCharacteristicStream StartNotification(String Service, String Characteristic,
                      CharacteristicStreamType?: ClassConstructor<TCharacteristicStream>)
    abstract void StopNotification(String Service, String Characteristic);

    abstract void NotificationDisconnect();
    abstract void NotificationError(Object err);
}