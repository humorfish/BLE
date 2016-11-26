package com.ultracreation.blelib.bean;

import android.bluetooth.BluetoothDevice;

/**
 * Created by you on 2016/3/14.
 */
public class BleDevice {
    int rssi;
    BluetoothDevice bluetoothDevice;

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public BluetoothDevice getBluetoothDevice() {
        return bluetoothDevice;
    }

    public void setBluetoothDevice(BluetoothDevice bluetoothDevice) {
        this.bluetoothDevice = bluetoothDevice;
    }
}
