package com.ultracreation.blelib.tools;

import io.reactivex.Observable;

/**
 * Created by Administrator on 2016/12/4.
 */

interface IService {
    boolean initialize();
    void scanDevice(boolean isStart);
    Observable<String> write(byte[] datas, int timeOut);
    void makeConnection(String address, INotification mINotification);
    void disconnect();
}
