package com.ultracreation.blelib.impl;

import io.reactivex.ObservableEmitter;

/**
 * Created by Administrator on 2016/12/4.
 */

public interface IService {
    boolean initialize();
    void scanDevice(boolean isStart);
    void write(byte[] datas, ObservableEmitter<Integer> progress);
    void makeConnection(String address, INotification mINotification);

    void disconnect(String deviceId);
    void onConnected(String deviceId);
    void onConnectedFailed(String deviceId, String message);
    void onDisconnected(String deviceId);
    void onReceiveData(String deviceId, byte[] Line);
}
