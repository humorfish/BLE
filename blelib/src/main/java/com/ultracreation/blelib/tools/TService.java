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
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;

import com.ultracreation.blelib.bean.SampleGattAttributes;
import com.ultracreation.blelib.impl.INotification;
import com.ultracreation.blelib.impl.IService;
import com.ultracreation.blelib.utils.KLog;
import com.ultracreation.blelib.utils.XLog;

import java.util.UUID;

/**
 * Created by you on 2016/12/3.
 */
public class TService extends Service implements IService
{
    private final static String TAG = TService.class.getSimpleName();
    private final static int INIT_WRITE_SIZE = 20;
    private final IBinder mBinder = new LocalBinder();
    public int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private INotification notification;
    private TLoopBuffer mReceiveDataBuffer;

    private Handler connectionHandler = new Handler();


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                KLog.i(TAG, "Connected to GATT server.");
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                setupGattServices(false);
                gatt.disconnect();
                gatt.close();

                onDisconnected();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS)
                setupGattServices(true);
            else
                KLog.w(TAG, "onServicesDiscovered received: " + status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            if (BluetoothGatt.GATT_SUCCESS == status)
            {
                KLog.v(TAG, "Callback: Wrote GATT Descriptor successfully.");
                onConnected();
            } else
                KLog.v(TAG, "Callback: Error writing GATT Descriptor: " + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            super.onCharacteristicRead(gatt, characteristic, status);

            KLog.i(TAG, "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS)
                doCharacteristicDataDispatch(characteristic);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicChanged(gatt, characteristic);
            KLog.i(TAG, "onCharacteristicChanged");
            doCharacteristicDataDispatch(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    private long totalPack = 0;
    private long sendingTimeoutRecord2 = 0;

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        KLog.i(TAG, "onUnbind");

        disconnect();
        stopSelf();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        KLog.i(TAG, "onDestroy");
        scanDevice(false);
    }

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    @Override
    public boolean initialize()
    {
        if (mBluetoothManager == null)
        {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null)
            {
                KLog.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null)
        {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        mReceiveDataBuffer = new TLoopBuffer(1000);
        return true;
    }

    @Override
    public void scanDevice(boolean isStart)
    {
        if (isStart)
        {
            mBluetoothAdapter.stopLeScan(null);
            mBluetoothAdapter.startLeScan(null);
        } else
        {
            mBluetoothAdapter.stopLeScan(null);
        }
    }

    private void setupGattServices(boolean isStop)
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        final BluetoothGattCharacteristic readCharacteristic = getCharacteristicByUuid(
                mBluetoothGatt, SampleGattAttributes.SERVICE_BOLUTEK,
                SampleGattAttributes.BODY_TONER_BOLUTEK);
        if (null != readCharacteristic)
        {
            setCharacteristicNotification(readCharacteristic, isStop);
        }
    }

    public final BluetoothGattCharacteristic getCharacteristicByUuid(
            BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid)
    {
        BluetoothGattCharacteristic characteristic = null;

        final BluetoothGattService service = gatt.getService(serviceUuid);
        if (null != service)
        {
            characteristic = service.getCharacteristic(characteristicUuid);
        }
        return characteristic;
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled)
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }

        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        // This is specific to Heart Rate Measurement.
        if (SampleGattAttributes.BODY_TONER_WRITE.equals(characteristic.getUuid()))
        {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(
                    SampleGattAttributes.CLIENT_CHARACTERISTIC_CONFIG);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }
    }

    private void doCharacteristicDataDispatch(BluetoothGattCharacteristic characteristic)
    {
        final UUID characteristicUuid = characteristic.getUuid();
        final byte[] rawData = characteristic.getValue();
        if (null == rawData || rawData.length < 1)
            return;

        if (characteristicUuid.equals(SampleGattAttributes.BODY_TONER_BOLUTEK))
        {
           
        }
    }

    private boolean writeDataDirect(BluetoothGattCharacteristic characteristic, byte[] data, int packWaitTime)
    {
        if (mBluetoothGatt == null)
            return false;

        int n = 0;
        int _minLen = 0;
        boolean b = false;
        int _countSleep = 0;

        while (n < data.length)
        {
            Log.d(TAG, "writeDataDirect .................waitTime:" + packWaitTime + " dectime:" + (System.currentTimeMillis() - sendingTimeoutRecord2));

            if (System.currentTimeMillis() - sendingTimeoutRecord2 < packWaitTime)
            {
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
            while (!b)
            {
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
    public void write(byte[] datas)
    {
        KLog.i(TAG, "service.write:" + new String(datas));
        if (mConnectionState == BluetoothProfile.STATE_CONNECTED)
        {
            final BluetoothGattCharacteristic findCharacteristic = getCharacteristicByUuid(
                    mBluetoothGatt, SampleGattAttributes.SERVICE_BOLUTEK,
                    SampleGattAttributes.BODY_TONER_WRITE);

            if (null != findCharacteristic)
                writeDataDirect(findCharacteristic, datas, 0);
        }
    }

    @Override
    public void disconnect()
    {
        if (mBluetoothGatt != null)
        {
            mBluetoothGatt.disconnect();
            mBluetoothGatt.close();
            mBluetoothGatt = null;
        }

        mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
    }

    @Override
    public void onConnected()
    {
        mConnectionState = BluetoothProfile.STATE_CONNECTED;
        scanDevice(false);

        if (notification != null)
            notification.onConnected();
    }

    @Override
    public void onConnectedFailed(String message)
    {
        mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        scanDevice(false);

        if (notification != null)
            notification.onConnectedFailed(message);
    }

    @Override
    public void onDisconnected()
    {
        KLog.i(TAG, "onDisconnected:mConnectionState" + mConnectionState);

        mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        scanDevice(false);

        if (notification != null)
            notification.onDisconnected();
    }

    @Override
    public void onReceiveData(String Line)
    {
        if (notification != null)
            notification.onReceiveData(Line);
    }

    @Override
    public void makeConnection(String address, INotification mINotification)
    {
        notification = mINotification;

        if (mBluetoothAdapter == null )
        {
            onConnectedFailed("BLE is not ready");
        }
        else
        {
            scanDevice(true);

            if (! TextUtils.isEmpty(address))
            {
                connectionHandler.postDelayed(new ConnectionRunnable(address), 1000);
            } else
                onConnectedFailed("device id is empty");
        }
    }

    public boolean isConnected(String address)
    {
        return mConnectionState == BluetoothProfile.STATE_CONNECTED;
//        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
//        if (device == null)
//        {
//           return false;
//        }
//        else
//        {
//            if (mBluetoothGatt != null && mBluetoothManager.getConnectionState(device, GATT_SERVER) == BluetoothProfile.STATE_CONNECTED)
//                return true;
//            else
//                return false;
//        }
    }

    private class ConnectionRunnable implements Runnable
    {
        private String address;
        public ConnectionRunnable(String address)
        {
            this.address = address;
        }

        @Override
        public void run()
        {
            // TODO:Check if the address is in the gatt source.
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device == null)
            {
                KLog.e(TAG, "Device not found.  Unable to connect.");
                onConnectedFailed("Device not found.  Unable to connect.");
                return;
            }

            mBluetoothGatt = device.connectGatt(TService.this, false, mGattCallback);
            if (mBluetoothGatt != null)
                KLog.e(TAG, "Trying to create a new connection.");
        }
    }

    protected class LocalBinder extends Binder
    {
        public TService getService()
        {
            return TService.this;
        }
    }
}