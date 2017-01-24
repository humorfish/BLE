package com.ultracreation.blelib.impl;

/**
 * Created by Administrator on 2016/12/4.
 */

public interface INotification{
    void onConnected(String deviceId);
    void onConnectedFailed(String deviceId, String message);
    void onDisconnected(String deviceId);
    void onReceiveData(String deviceId, byte[] Line);
}