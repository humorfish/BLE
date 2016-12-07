package com.ultracreation.blelib.tools;

/**
 * Created by Administrator on 2016/12/4.
 */

interface IService {
    boolean initialize();
    void scanDevice(boolean isStart);
    void write(byte[] datas);
    void makeConnection(String address, INotification mINotification);
    void disconnect();
}
