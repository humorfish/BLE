package com.ultracreation.blelib.bean;

import android.bluetooth.BluetoothDevice;

/**
 * Created by Administrator on 2018/2/28.
 */

public class BLEDevice
{
    public BluetoothDevice device;
    public int rssi;

    public BLEDevice(BluetoothDevice device, int rssi)
    {
        this.device = device;
        this.rssi = rssi;
    }
}