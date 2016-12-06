package com.ultracreation.blelib.tools;

import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2016/12/4.
 */

interface IService {
    boolean initialize();
    void scanDevice(boolean isStart);
    Subject<String> write(byte[] datas, int timeOut, Subject<String> isCallBack);
    void makeConnection(String address, INotification mINotification);
    void disconnect();
}
