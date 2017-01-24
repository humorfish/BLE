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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ultracreation.blelib.bean.SampleGattAttributes;
import com.ultracreation.blelib.impl.INotification;
import com.ultracreation.blelib.impl.IService;
import com.ultracreation.blelib.utils.KLog;

import java.util.UUID;

import io.reactivex.ObservableEmitter;

import static android.bluetooth.BluetoothProfile.GATT_SERVER;


/**
 * Created by you on 2016/12/3.
 */
public enum  TService
{
    Instance;

    private final String TAG = TService.class.getSimpleName();
    private boolean isServiceBound = false;
    public BLEService mSevice;

    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {
            if (iBinder == null)
            {
                throw new Error("bind server failed!!");
            }

            mSevice = ((BLEService.LocalBinder) iBinder).getService();
            if (! mSevice.initialize())
            {
                KLog.e("mServiceConnection", "Unable to initialize Bluetooth");
                System.exit(0);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
        }
    };

    TService()
    {

    }

    public boolean bindBluetoothSevice(@NonNull Context mContext)
    {
        if (! mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(mContext, "ble not support", Toast.LENGTH_SHORT).show();
            System.exit(0);
        }

        Intent gattServiceIntent = new Intent(mContext, TService.class);
        isServiceBound = mContext.getApplicationContext().bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        KLog.i(TAG, "bindBluetoothSevice.isBindService:" + isServiceBound);
        return isServiceBound;
    }

    public void unBindBluetoothSevice(Context mContext)
    {
        KLog.i(TAG, "bindBluetoothSevice.isBindService:" + isServiceBound);
        if (isServiceBound)
            mContext.getApplicationContext().unbindService(mServiceConnection);
    }

    class BLEService extends Service implements IService
    {
        private final static int INIT_WRITE_SIZE = 20;
        private final IBinder mBinder = new LocalBinder();
        private final String LINE_BREAK = "\r\n";
        public int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
        private BluetoothManager mBluetoothManager;
        private BluetoothAdapter mBluetoothAdapter;
        private BluetoothGatt mBluetoothGatt;
        private INotification notification;
        private Handler connectionHandler = new Handler();

        private TMemoryStream mLineBuffer;
        private TLoopBuffer mReceiveDataBuffer;
        private byte[] lineBreakBytes = LINE_BREAK.getBytes();
        private int lineBreakMatched = 0;
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

                    onDisconnected(gatt.getDevice().getAddress());
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
                    onConnected(gatt.getDevice().getAddress());
                } else
                    KLog.v(TAG, "Callback: Error writing GATT Descriptor: " + status);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                super.onCharacteristicRead(gatt, characteristic, status);

                KLog.i(TAG, "onCharacteristicRead");
                if (status == BluetoothGatt.GATT_SUCCESS)
                    doCharacteristicDataDispatch(gatt.getDevice().getAddress(), characteristic);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
            {
                super.onCharacteristicChanged(gatt, characteristic);
                KLog.i(TAG, "onCharacteristicChanged");
                doCharacteristicDataDispatch(gatt.getDevice().getAddress(), characteristic);
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
            {
                super.onCharacteristicWrite(gatt, characteristic, status);
            }
        };

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

            disconnectAll();
            stopSelf();
            return super.onUnbind(intent);
        }

        private void disconnectAll() //需要重写
        {
            if (mBluetoothGatt != null)
            {
                String deviceId = mBluetoothGatt.getDevice().getAddress();
                disconnect(deviceId);
            }
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

            mReceiveDataBuffer = new TLoopBuffer(512);
            mLineBuffer = new TMemoryStream(256);
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

        private void doCharacteristicDataDispatch(String deviceId, BluetoothGattCharacteristic characteristic)
        {

            final UUID characteristicUuid = characteristic.getUuid();
            if (characteristicUuid.equals(SampleGattAttributes.BODY_TONER_BOLUTEK))
            {
                final byte[] rawData = characteristic.getValue();
                if (null == rawData || rawData.length < 1)
                    return;

                KLog.i(TAG, "receiveData:" + new String(rawData));
                // do not inherited, the buffer consumed and notified immdiately
                mReceiveDataBuffer.push(rawData, rawData.length);
                byte[] mBytes = new byte[1];

                while (! mReceiveDataBuffer.isEmpty())
                {
                    mBytes = mReceiveDataBuffer.extractTo(mBytes.length);
                    mLineBuffer.write(mBytes);

                    if (mBytes[0] == lineBreakBytes[lineBreakMatched])
                    {
                        lineBreakMatched ++;

                        if (lineBreakMatched == lineBreakBytes.length)
                        {
                            byte[] lineData = new byte[mLineBuffer.mPosition - lineBreakBytes.length];
                            System.arraycopy(mLineBuffer.mMemory, 0, lineData, 0, lineData.length);
                            onReceiveData(deviceId, lineData);

                            mLineBuffer.clear();
                            lineBreakMatched = 0;
                        }

                    } else
                        lineBreakMatched = 0;
                }
            }
        }

        private boolean writeDataDirect(BluetoothGattCharacteristic characteristic, byte[] data, ObservableEmitter<Integer> progress)
        {
            if (mBluetoothGatt == null)
            {
                if (progress != null)
                    progress.onError(new IllegalStateException("not connected"));

                return false;
            }

            int sendedLength = 0;
            int currenSendLength;

            boolean b = false;
            while (sendedLength < data.length)
            {
                if ((data.length - sendedLength) > INIT_WRITE_SIZE)
                    currenSendLength = INIT_WRITE_SIZE;
                else
                    currenSendLength = data.length - sendedLength;

                if (mConnectionState == BluetoothProfile.STATE_CONNECTED)
                {
                    byte[] sendData = new byte[currenSendLength];
                    System.arraycopy(data, sendedLength, sendData, 0, currenSendLength);
                    characteristic.setValue(sendData);
                    characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
                    b = mBluetoothGatt.writeCharacteristic(characteristic);

                    int reSendCount = 0;
                    while (! b)
                    {
                        KLog.e(TAG, "reSend");
                        SystemClock.sleep(15);
                        b = mBluetoothGatt.writeCharacteristic(characteristic);
                        reSendCount ++;

                        if (reSendCount >= 4)
                            return false;
                    }

                    sendedLength += currenSendLength;
                    if (progress != null)
                    {
                        int index = (int)(sendedLength*1.0f/data.length * 100);
                        progress.onNext(index);
                    }

                    SystemClock.sleep(20);

                }else
                    return false;
            }

            if (progress != null)
            {
                progress.onNext(100);
                progress.onComplete();
            }

            return b;
        }

        @Override
        public void write(byte[] datas, ObservableEmitter<Integer> progress)
        {
            KLog.i(TAG, "service.write:" + new String(datas));
            if (mConnectionState == BluetoothProfile.STATE_CONNECTED)
            {
                final BluetoothGattCharacteristic findCharacteristic = getCharacteristicByUuid(
                        mBluetoothGatt, SampleGattAttributes.SERVICE_BOLUTEK,
                        SampleGattAttributes.BODY_TONER_WRITE);

                if (null != findCharacteristic)
                    writeDataDirect(findCharacteristic, datas, progress);
            }
        }

        @Override
        public void disconnect(String deviceId)
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
        public void onConnected(String deviceId)
        {
            mConnectionState = BluetoothProfile.STATE_CONNECTED;
            scanDevice(false);

            if (notification != null)
                notification.onConnected(deviceId);
        }

        @Override
        public void onConnectedFailed(String deviceId, String message)
        {
            mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
            scanDevice(false);

            if (notification != null)
                notification.onConnectedFailed(deviceId, message);
        }

        @Override
        public void onDisconnected(String deviceId)
        {
            KLog.i(TAG, "onDisconnected:mConnectionState" + mConnectionState);

            mConnectionState = BluetoothProfile.STATE_DISCONNECTED;
            scanDevice(false);

            if (notification != null)
                notification.onDisconnected(deviceId);
        }

        @Override
        public void onReceiveData(String deviceId, byte[] line)
        {
            if (notification != null)
                notification.onReceiveData(deviceId, line);
        }

        @Override
        public void makeConnection(String deviceId, INotification mINotification)
        {
            notification = mINotification;

            if (mBluetoothAdapter == null)
            {
                onConnectedFailed(deviceId, "BLE is not ready");
            } else
            {
                scanDevice(true);

                if (! TextUtils.isEmpty(deviceId))
                {
                    connectionHandler.postDelayed(() -> {

                        // TODO:Check if the address is in the gatt source.
                        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceId);
                        if (device == null)
                        {
                            KLog.e(TAG, "Device not found.  Unable to connect.");
                            onConnectedFailed(deviceId, "Device not found.  Unable to connect.");
                            return;
                        }

                        mBluetoothGatt = device.connectGatt(BLEService.this, false, mGattCallback);
                        if (mBluetoothGatt != null)
                            KLog.e(TAG, "Trying to create a new connection.");

                    }, 1000);

                } else
                    onConnectedFailed(deviceId, "device id is empty");
            }
        }

        public boolean isConnected(String address)
        {
            //return mConnectionState == BluetoothProfile.STATE_CONNECTED;
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            if (device == null)
            {
                return false;
            } else
            {
                if (mBluetoothGatt != null && mBluetoothManager.getConnectionState(device, GATT_SERVER) == BluetoothProfile.STATE_CONNECTED)
                    return true;
                else
                    return false;
            }
        }

        protected class LocalBinder extends Binder
        {
            public BLEService getService()
            {
                return BLEService.this;
            }
        }
    }
}