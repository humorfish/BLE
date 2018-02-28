package com.ultracreation.blelib.tools;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.SystemClock;
import android.text.TextUtils;

import com.ultracreation.blelib.bean.BLEDevice;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2016/12/4.
 */

public enum TGattScaner
{
    Scaner;

    private final int STOPED = 0;
    private final int SCANNING = 1;

    private int scanStatus = STOPED;
    private PublishSubject<BLEDevice> mSubject;

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord)
        {
            if (TextUtils.isEmpty(device.getAddress()))
                return;

            mSubject.onNext(new BLEDevice(device, rssi));
        }
    };

    TGattScaner()
    {
        mSubject = PublishSubject.create();
    }

    public boolean isScanning()
    {
        return (scanStatus == SCANNING);
    }

    public Subject<BLEDevice> start(Filter filter)
    {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
            throw new IllegalStateException("ble not support");
        else
        {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            SystemClock.sleep(100);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            if (! isScanning())
            {
                if (filter != null)
                    mSubject.filter(filter::onCall);
                scanStatus = SCANNING;
            }

            return mSubject;
        }
    }

    public void reStart()
    {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null)
            throw new IllegalStateException("ble not support");
        else
        {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            SystemClock.sleep(100);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        }
    }

    public void stop()
    {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter != null)
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    public interface Filter
    {
        boolean onCall(BLEDevice device);
    }
}
