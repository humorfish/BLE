package com.ultracreation.blelib.tools;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2016/12/4.
 */

public enum TGattScaner {
    Scaner;

    private BluetoothAdapter mBluetoothAdapter;
    public Subject<Map<BluetoothDevice, Integer>> mSubject;
    private int timeoutInterval = 100000;
    private Map<BluetoothDevice, Integer> devices;
    private boolean isScanning = false;
    private Timer timer;
    private TimerTask timeOutTask;

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            addDevice(device, rssi);
        }
    };

    TGattScaner() {
        devices = new HashMap<>();
        mSubject = PublishSubject.create();
        refreshTimeout();
    }

    public void initBluetooth(Activity activity, BluetoothAdapter mBluetoothAdapter, final int REQUEST_ENABLE_BT) {
        this.mBluetoothAdapter = mBluetoothAdapter;

        if (! mBluetoothAdapter.isEnabled()) {
            if (! mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
    }

    public Subject<Map<BluetoothDevice, Integer>> getDevices() {
        return mSubject;
    }

    public void setTimeOut(int timeOut) {
        this.timeoutInterval = timeOut;
    }

    public void clear() {
        devices.clear();
    }

    public void addDevice(BluetoothDevice device, int rssi) {
        if (TextUtils.isEmpty(device.getAddress()))
            return;

        devices.put(device, rssi);
        mSubject.onNext(devices);
    }

    void refreshTimeout() {
        if (mSubject.hasComplete() || mSubject.hasThrowable())
            return;

        clearTimeout();

        if (timeoutInterval == 0)
            return;

        setTimeout();
    }

    void error(String message) {
        if (mSubject.hasObservers()) {
            mSubject.onError(new Throwable(message));
            refreshTimeout();
        }
    }

    public void start(DeviceCallBack callBack) {
        if (! isScanning) {
            isScanning = true;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }

    public void stop() {
        isScanning = false;
        mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    void setTimeout() {
        if (timer == null)
            timer = new Timer();

        if (timeOutTask != null)
            timeOutTask.cancel();

        timeOutTask = null;
        timeOutTask = new TimerTask() {
            @Override
            public void run() {
                error("time out");
            }
        };
        timer.schedule(timeOutTask, timeoutInterval);
    }

    void clearTimeout() {
        if (timeOutTask != null) {
            timeOutTask.cancel();
            timeOutTask = null;
        }

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    interface Filter {
        boolean onCall();
    }

    interface DeviceCallBack {
        Map<BluetoothDevice, Integer> onCall();
    }
}
