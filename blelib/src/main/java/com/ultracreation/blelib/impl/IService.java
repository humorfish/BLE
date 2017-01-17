package com.ultracreation.blelib.impl;

/**
 * Created by Administrator on 2016/12/4.
 */

public interface IService {
    boolean initialize();
    void scanDevice(boolean isStart);
    void write(byte[] datas);
    void makeConnection(String address, INotification mINotification);
    void disconnect();
    void onConnected();
    void onConnectedFailed(String message);
    void onDisconnected();
    void onReceiveData(byte[] Line);
}
