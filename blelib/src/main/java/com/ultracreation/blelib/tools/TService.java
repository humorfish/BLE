package com.ultracreation.blelib.tools;

/**
 * Created by Administrator on 2016/12/4.
 */

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
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.ultracreation.blelib.bean.SampleGattAttributes;
import com.ultracreation.blelib.utils.KLog;
import com.ultracreation.blelib.utils.XLog;

import java.util.LinkedList;
import java.util.UUID;

import io.reactivex.subjects.Subject;

/**
 * Created by you on 2016/12/3.
 */
public class TService extends Service implements IService {
    private final static String TAG = TService.class.getSimpleName();
    private final static int INIT_WRITE_SIZE = 20;
    private final IBinder mBinder = new LocalBinder();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private INotification notification;
    private StringBuilder mStringBuilder;
    private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                KLog.i(TAG, "Connected to GATT server.");
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                setupGattServices(false);
                gatt.disconnect();
                gatt.close();

                mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
                if (notification != null)
                    notification.onDisconnected();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS)
                setupGattServices(true);
            else
                KLog.w(TAG, "onServicesDiscovered received: " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            if (BluetoothGatt.GATT_SUCCESS == status) {
                KLog.v(TAG, "Callback: Wrote GATT Descriptor successfully.");
                mConnectionState = BluetoothProfile.STATE_CONNECTED;
                if (notification != null)
                    notification.onConnected();
            } else
                KLog.v(TAG, "Callback: Error writing GATT Descriptor: " + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                doCharacteristicDataDispatch(gatt, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            doCharacteristicDataDispatch(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            doReadWriteAction(gatt, characteristic);
        }
    };
    private LinkedList<byte[]> dataQueue = new LinkedList<>();
    private LinkedList<Subject<String>> readQueue = new LinkedList<>();

    // Device scan callback.
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            if (device != null) {
                TShell.instance.addDeivce(device.getAddress(), device.getName(), rssi);
            }
        }
    };
    private long totalPack = 0;
    private long sendingTimeoutRecord2 = 0;

    private void doReadWriteAction(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (BluetoothProfile.STATE_CONNECTED != mConnectionState) {
            return;
        }

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        KLog.i(TAG, "onUnbind");

        disconnect();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        KLog.i(TAG, "onDestroy");
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    @Override
    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                KLog.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        mStringBuilder = new StringBuilder();
        return true;
    }

    @Override
    public void scanDevice(boolean isStart) {
        if (isStart) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
    }

    private void setupGattServices(boolean isStop) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        final BluetoothGattCharacteristic readCharacteristic = getCharacteristicByUuid(
                mBluetoothGatt, SampleGattAttributes.SERVICE_BOLUTEK,
                SampleGattAttributes.BODY_TONER_BOLUTEK);
        if (null != readCharacteristic) {
            setCharacteristicNotification(readCharacteristic, isStop);
        }
    }

    public final BluetoothGattCharacteristic getCharacteristicByUuid(
            BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid) {
        BluetoothGattCharacteristic characteristic = null;

        final BluetoothGattService service = gatt.getService(serviceUuid);
        if (null != service) {
            characteristic = service.getCharacteristic(characteristicUuid);
        }
        return characteristic;
    }

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

    private void doCharacteristicDataDispatch(BluetoothGatt gatt,
                                              BluetoothGattCharacteristic characteristic) {
        final UUID characteristicUuid = characteristic.getUuid();
        final byte[] rawData = characteristic.getValue();
        if (null == rawData || rawData.length < 1) {
            return;
        }

        if (characteristicUuid.equals(SampleGattAttributes.BODY_TONER_BOLUTEK)) {
            String tmp = new String(rawData);
            if (! TextUtils.isEmpty(tmp)) {
                int index = tmp.indexOf("\r\n");
                KLog.i(TAG, "data:" + tmp.replace("\r\n", "") + "  index:" + index);

                if (index > 0) {
                    String before = tmp.substring(0, index);
                    String after = tmp.substring(index + 2, tmp.length());
                    mStringBuilder.append(before);

                    if (readQueue.size() > 0) {
                        Subject<String> subject = readQueue.peek();
                        subject.onNext(mStringBuilder.toString());
                        subject.onComplete();

                        dataQueue.remove();
                        readQueue.remove();
                    }
                    mStringBuilder.setLength(0);
                    mStringBuilder.append(after);
                } else
                    mStringBuilder.append(tmp);
            }
        }
    }

    public boolean writeDataDirect(BluetoothGattCharacteristic characteristic, byte[] data, int packWaitTime) {
        if (mBluetoothGatt == null)
            return false;

        int n = 0;
        int _minLen = 0;
        boolean b = false;
        int _countSleep = 0;

        while (n < data.length) {
            Log.d(TAG, "writeDataDirect .................waitTime:" + packWaitTime + " dectime:" + (System.currentTimeMillis() - sendingTimeoutRecord2));

            if (System.currentTimeMillis() - sendingTimeoutRecord2 < packWaitTime) {
                Log.d(TAG, "writeDataDirect ................._countSleep:" + _countSleep++);
                SystemClock.sleep(5);
                continue;
            }

            if ((data.length - n) > INIT_WRITE_SIZE)
                _minLen = INIT_WRITE_SIZE;
            else
                _minLen = data.length - n;


            byte[] writeData = new byte[_minLen];
            System.arraycopy(data, n, writeData, 0, _minLen);
            Log.d(TAG, "writeDataDirect .................n:" + n + " _minLen:" + _minLen + " totalPack:" + totalPack++);
            characteristic.setValue(writeData);
            characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
            b = mBluetoothGatt.writeCharacteristic(characteristic);
            int _count = 0;
            while (!b) {
                XLog.e(TAG, "Write fail!");
                SystemClock.sleep(15);
                b = mBluetoothGatt.writeCharacteristic(characteristic);
                _count++;
                //只重发三次
                if (_count >= 20)
                    break;
            }
            n += _minLen;
            _countSleep = 0;
            sendingTimeoutRecord2 = System.currentTimeMillis();
        }
        return b;
    }

    @Override
    public Subject<String> write(byte[] datas, int timeOut, Subject<String> isCallBack) {
        if (mConnectionState == BluetoothProfile.STATE_CONNECTED) {
            dataQueue.add(datas);
            readQueue.add(isCallBack);

            final BluetoothGattCharacteristic findCharacteristic = getCharacteristicByUuid(
                    mBluetoothGatt, SampleGattAttributes.SERVICE_BOLUTEK,
                    SampleGattAttributes.BODY_TONER_WRITE);

            if (null != findCharacteristic)
                writeDataDirect(findCharacteristic, datas, 0);

            return isCallBack;
        }

        return null;
    }

    @Override
    public void disconnect() {
    }

    @Override
    public void makeConnection(String address, INotification mINotification) {
        notification = mINotification;

        if (mBluetoothAdapter != null && !TextUtils.isEmpty(address)) {
            // TODO:Check if the address is in the gatt source.
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device == null) {
                KLog.e(TAG, "Device not found.  Unable to connect.");
                mINotification.onConnectedFailed();
                return;
            }

            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

            if (mBluetoothGatt != null)
                KLog.e(TAG, "Trying to create a new connection.");
        } else
            mINotification.onConnectedFailed();
    }

    protected class LocalBinder extends Binder {
        public TService getService() {
            return TService.this;
        }
    }
}