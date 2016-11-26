/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ultracreation.blelib.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import com.ultracreation.blelib.bean.GattDataSource;
import com.ultracreation.blelib.bean.NewDeviceDataSource;
import com.ultracreation.blelib.bean.SampleGattAttributes;
import com.ultracreation.blelib.impl.BodyTonerCmdFormaterListener;
import com.ultracreation.blelib.utils.BluetoothUtils;
import com.ultracreation.blelib.utils.BodyTonerCmdFormater;
import com.ultracreation.blelib.utils.KLog;
import com.ultracreation.blelib.utils.XBluetoothGatt;
import com.ultracreation.blelib.utils.XLog;

import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    //刷新rssi
    public final static String ACTION_RSSI_UPDATE =
            "com.example.bluetooth.le.ACTION_RSSI_UPDATE";
    public final static String RSSI_UPDATE_DATA = "rssi_update_data";
    //刷新设备列表
    public final static String ACTION_DEVICES_UPDATE =
            "com.example.bluetooth.le.ACTION_DEVICES_UPDATE";
    public final static String DEVICES_UPDATE_DATA = "device_update_data";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    /**
     * Indicate scanning ?
     */
    private boolean mScanning = false;
    /**
     * Address of the connected.
     */
    private String mCurAddress = null;
    /**
     * The handler for the start/stop scan.
     */
    private Handler mScanHandler = null;
    /**
     * Bluetooth Gatt map container.
     */
    private GattDataSource mGattSrc = GattDataSource.defaultSrc();
    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             byte[] scanRecord) {
            if (device != null) {
                String name = device.getName();
                // 只显示我们自己的设备
                if (BluetoothUtils.MINITENS.equals(name)) {//过滤 bluetens minitens BluetoothUtils.isBluetensDeviceName(name) ||
                    KLog.i(TAG, "LeScanCallback:address=" + device.getAddress() + " rssi=" + rssi + "   contain=" + NewDeviceDataSource.defaultSrc().count());
                    //add by 发送rssi 值
                    if (!NewDeviceDataSource.defaultSrc().contain(device.getAddress())) {
                        mScanHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                NewDeviceDataSource.defaultSrc().addItem(device.getAddress());
                                //将回调的RSSI值赋值
                                broadcastUpdateDevices(device.getAddress());
                            }
                        }, 10);
                    }

                    mScanHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            broadcastUpdateRssi(device.getAddress(), rssi);
                        }
                    }, 10);
                }
            }
        }
    };
    /**
     * The command formatter.
     */
    private BodyTonerCmdFormater btFormatter = new BodyTonerCmdFormater();
    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            String macAddr = gatt.getDevice().getAddress();

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                setupGattServices(false);
                gatt.disconnect();
                gatt.close();
                mGattSrc.removeItem(macAddr);
                mCurAddress = null;
                btFormatter.clear();

                KLog.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                setupGattServices(true);
            } else
                KLog.w(TAG, "onServicesDiscovered received: " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            String intentAction;
            if (BluetoothGatt.GATT_SUCCESS == status) {
                KLog.v(TAG, "Callback: Wrote GATT Descriptor successfully.");
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
            } else
                KLog.v(TAG, "Callback: Error writing GATT Descriptor: " + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                doCharacteristicDataDispatch(gatt, characteristic);
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            // broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            doCharacteristicDataDispatch(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            if (BluetoothGatt.GATT_SUCCESS == status) {
            } else {
            }

            XBluetoothGatt gat = mGattSrc
                    .itemFor(gatt.getDevice().getAddress());
            if (null != gat) {
                gat.doSpeedTestOnWriteOver();
                gat.onWriteResult(characteristic, status);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            // TODO Auto-generated method stub
            super.onReadRemoteRssi(gatt, rssi, status);
        }
    };

    private void broadcastUpdateRssi(String deviceAddress, int rssi) {
        final Intent intent = new Intent(ACTION_RSSI_UPDATE);
        intent.putExtra(DEVICES_UPDATE_DATA, deviceAddress);
        intent.putExtra(RSSI_UPDATE_DATA, rssi);
        sendBroadcast(intent);
    }

    //将回调的RSSI值赋值
    private void broadcastUpdateDevices(String deviceAddress) {
        final Intent intent = new Intent(ACTION_DEVICES_UPDATE);
        intent.putExtra(DEVICES_UPDATE_DATA, deviceAddress);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdateData(byte[] mData, String mCurAddress) {
        if (mData != null && mData.length > 0) {
            final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
            final byte[] data = mData;
            Bundle bundle = new Bundle();
            bundle.putByteArray("data", data);
            bundle.putString("address", mCurAddress);
            intent.putExtras(bundle);
            sendBroadcast(intent);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        disconnect(mCurAddress);
        close();
        return super.onUnbind(intent);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        mScanHandler = new Handler();
        btFormatter.registerListener(new DataListenter());
        return true;
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter != null && mBluetoothGatt != null) {
            KLog.i(TAG, "disconnect");
            mBluetoothGatt.disconnect();
        }
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt != null) {
            KLog.i(TAG, "close");
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        // This is specific to Heart Rate Measurement.
        if (SampleGattAttributes.BODY_TONER_WRITE.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    /**
     * Set up the gatt services with notify or other ones.
     *
     * @param bNotify true - notify.
     */
    private void setupGattServices(boolean bNotify) {
        btFormatter.clear();
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        final BluetoothGattCharacteristic readCharacteristic = getCharacteristicByUuid(
                mBluetoothGatt, SampleGattAttributes.SERVICE_BOLUTEK,
                SampleGattAttributes.BODY_TONER_BOLUTEK);
        if (null != readCharacteristic) {
            setCharacteristicNotification(readCharacteristic, bNotify);
        }
    }

    /**
     * Get the spec characteristic by the given Gatt and UUID of the service and
     * characterisc.
     *
     * @param gatt               Bluetooth Gatt.
     * @param serviceUuid        the uuid of the service.
     * @param characteristicUuid the uuid of the characteristic.
     * @return If null, not find. not null find one.
     */
    public final BluetoothGattCharacteristic getCharacteristicByUuid(
            BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid) {
        BluetoothGattCharacteristic characteristic = null;

        final BluetoothGattService service = gatt.getService(serviceUuid);
        if (null != service) {
            characteristic = service.getCharacteristic(characteristicUuid);
        }
        return characteristic;
    }

    public void startScanBle(boolean startOrStop) {
        NewDeviceDataSource.defaultSrc().clear();
        if (startOrStop) {
            // Stops scanning after a pre-defined scan period.
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    /**
     * Dispatch the arriving data.
     *
     * @param gatt
     * @param characteristic
     */
    private void doCharacteristicDataDispatch(BluetoothGatt gatt,
                                              BluetoothGattCharacteristic characteristic) {
        final UUID characteristicUuid = characteristic.getUuid();
        final byte[] rawData = characteristic.getValue();
        if (null == rawData || rawData.length < 1) {
            return;
        }

        final String macAddr = gatt.getDevice().getAddress();

        if (characteristicUuid.equals(SampleGattAttributes.BODY_TONER_BOLUTEK)) {
            btFormatter.pushReceivedData(rawData, macAddr);
        }
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The
     * connection result is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            XLog.e(TAG,
                    "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (address.length() != 17) {
            XLog.e(TAG, "Invalid address to connect:" + address);
            return false;
        }

        mBluetoothGatt = mGattSrc.itemForCore(address);
        if (null != mBluetoothGatt) {
            XLog.e(TAG, "Reconnt");
            return mBluetoothGatt.connect();
        }

        // TODO:Check if the address is in the gatt source.
        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            XLog.e(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        if (null != mBluetoothGatt) {
            mGattSrc.addItem(mBluetoothGatt.getDevice().getAddress(), new XBluetoothGatt(
                    mBluetoothGatt));
            XLog.e(TAG, "Trying to create a new connection.");
            return true;
        }

        return false;
    }

    /**
     * Disconnect the connection.
     *
     * @param address
     * @return
     */
    public boolean disconnect(final String address) {
        if (!TextUtils.isEmpty(address) && mGattSrc != null) {
            KLog.i(TAG, "-------->"+ address);
            XBluetoothGatt gat = mGattSrc.itemFor(address);
            if (null != gat) {
                gat.clearAll();
            }
            BluetoothGatt gatt = mGattSrc.itemForCore(address);
            if (null != gatt) {
                XLog.e(TAG, "********* disconnect");
                gatt.disconnect();
            }

            disconnect();
        }

        return true;
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    private class DataListenter implements BodyTonerCmdFormaterListener {
        @Override
        public void OnCommandReady(byte[] data, String macAddress) {
            broadcastUpdateData(data, macAddress);
        }
    }
}
