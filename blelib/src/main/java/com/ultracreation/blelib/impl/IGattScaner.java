package com.ultracreation.blelib.impl;

import java.util.ArrayList;

import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2016/12/4.
 */
interface IGattScaner {
    Subject<ArrayList<String>> getDevices();
    void setFilters(String[] filters);
    void setTimeOut(int timeOut);
    void addDevice(String address, String name, int rssi);
    void clear();
}

