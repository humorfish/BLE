package com.ultracreation.blelib.tools;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2016/12/4.
 */

class TGattScaner implements IGattScaner {
    private final static String TAG = TGattScaner.class.getSimpleName();
    private String[] filters = null;
    private int timeOut = 0;
    private ArrayList<String> devices;
    private ArrayList<Integer> rssis;
    public Subject<ArrayList<String>> mSubject;

    public TGattScaner(){
        devices = new ArrayList<>();
        rssis = new ArrayList<>();
        mSubject = PublishSubject.create();
    }

    @Override
    public Subject<ArrayList<String>> getDevices() {
        return mSubject;
    }

    @Override
    public void setFilters(String[] filters) {
        this.filters = filters;
    }

    @Override
    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    @Override
    public void clear() {
        devices.clear();
        rssis.clear();
    }

    @Override
    public void addDevice(String address, String name, int rssi) {
        if (TextUtils.isEmpty(address))
            return;

        if (TextUtils.isEmpty(name))
            name = "unknown device";

        String device = address + name;

        if (! devices.contains(device)) {
            Log.i(TAG, "not contains." + "bleDeviceAddress=" + address + "  rssi=" + rssi);
            devices.add(device);
            rssis.add(rssi);
        } else {
            Log.i(TAG, "contains." + "device=" + device + "  rssi=" + rssi);
            rssis.set(devices.indexOf(device), rssi);
        }

        mSubject.onNext(devices);
    }
}
