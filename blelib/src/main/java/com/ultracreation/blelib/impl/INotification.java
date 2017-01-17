package com.ultracreation.blelib.impl;

/**
 * Created by Administrator on 2016/12/4.
 */

public interface INotification{
    void onConnected();
    void onConnectedFailed(String message);
    void onDisconnected();
    void onReceiveData(byte[] Line);
}