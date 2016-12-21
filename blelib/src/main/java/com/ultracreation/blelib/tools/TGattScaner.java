package com.ultracreation.blelib.tools;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.text.TextUtils;

import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2016/12/4.
 */

public enum TGattScaner {
    Scaner;

    private BluetoothAdapter mBluetoothAdapter;
    private Subject<BLEDevice> mSubject;
    private Disposable mDisposable;

    private int timeoutInterval = 100000;
    private boolean isScanning = false;
    private Timer timer;
    private TimerTask timeOutTask;

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            if (TextUtils.isEmpty(device.getAddress()))
                return;

            mSubject.onNext(new BLEDevice(device, rssi));
        }
    };

    TGattScaner() {
        mSubject = PublishSubject.create();
    }

    public void initBluetooth(Activity activity, BluetoothAdapter mBluetoothAdapter, final int REQUEST_ENABLE_BT) {
        this.mBluetoothAdapter = mBluetoothAdapter;

        if (activity == null || mBluetoothAdapter == null)
            error("init ble failed");
        else if (! mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    public void setTimeOut(int timeOut) {
        this.timeoutInterval = timeOut;
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
            clearTimeout();
        }
    }

    public void start(Filter filter, DeviceCallBack callBack) {
        refreshTimeout();
        if (! isScanning && mBluetoothAdapter != null) {
            isScanning = true;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            if (mDisposable != null && ! mDisposable.isDisposed())
                mDisposable = mSubject.filter(bleDevice ->
                    filter.onCall(bleDevice.device.getName())
                ).subscribe(bleDevice1 -> {
                    callBack.onCall(bleDevice1);
                });
        }
    }

    public void stop() {
        isScanning = false;
        if (mBluetoothAdapter != null)
            mBluetoothAdapter.stopLeScan(mLeScanCallback);

        if (mDisposable != null && ! mDisposable.isDisposed())
            mDisposable.dispose();

        clearTimeout();
    }

    void setTimeout() {
        if (timer == null)
            timer = new Timer();

        if (timeOutTask != null) {
            timeOutTask.cancel();
            timeOutTask = null;
        }

        timeOutTask = new TimerTask() {
            @Override
            public void run() {
                stop();
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

    public interface Filter {
        boolean onCall(String deviceName);
    }

    public interface DeviceCallBack {
        void onCall(BLEDevice device);
    }

    public class BLEDevice{
        public BluetoothDevice device;
        public int rssi;

        public BLEDevice(BluetoothDevice device, int rssi){
            this.device = device;
            this.rssi = rssi;
        }
    }
}
