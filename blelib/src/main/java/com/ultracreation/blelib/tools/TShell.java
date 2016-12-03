package com.ultracreation.blelib.tools;

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
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ultracreation.blelib.bean.SampleGattAttributes;
import com.ultracreation.blelib.service.BluetoothLeService;
import com.ultracreation.blelib.utils.KLog;

import java.util.ArrayList;
import java.util.UUID;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2016/11/28.
 */

public enum  TShell {
    instance;

    private final static String TAG = TShell.class.getSimpleName();
    private Context context;
    private TGattScaner mTGattScaner;
    private TSevice mSevice;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (iBinder == null) {
                throw new Error("bind server failed!!");
            }

            mSevice = ((TSevice.LocalBinder) iBinder).getService();
            if (!mSevice.initialize()) {
                Log.e("mServiceConnection", "Unable to initialize Bluetooth");
                System.exit(0);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSevice = null;
        }
    };

    TShell(){
        mTGattScaner = new TGattScaner();
        initBluetooth();
    }

    public void setFilters(String[] filters){
        mTGattScaner.setFilters(filters);
    }

    public void setTimeOut(int timeOut){
        mTGattScaner.setTimeOut(timeOut);
    }

    public Subject<ArrayList<BleDevice>> startScan(){
        return mTGattScaner.startScan();
    }

    public void stopScan(){
        mSevice.scanDevice(true);
    }

    public void clear(){
        mTGattScaner.clear();
    }

    public void addDeivce(String address, String name, int rssi) {
        mTGattScaner.addDeivce(address, name, rssi);
    }

    public void setContext(Context mContext) {
        this.context = mContext;
    }

    private void initBluetooth() {
        if (! context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(context, "ble not support", Toast.LENGTH_SHORT).show();
            System.exit(0);
        }

        Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
        context.bindService(gattServiceIntent, mServiceConnection, context.BIND_AUTO_CREATE);
    }

    class TGattScaner implements IGattScaner{
        private String[] filters = null;
        private int timeOut = 0;
        private ArrayList<BleDevice> devices;
        private ArrayList<Integer> rssis;
        public Subject<ArrayList<BleDevice>> mSubject;

        public TGattScaner(){
            devices = new ArrayList<>();
            rssis = new ArrayList<>();
            mSubject = PublishSubject.create();
        }

        @Override
        public Subject<ArrayList<BleDevice>> startScan() {
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
        public void addDeivce(String address, String name, int rssi) {
            if (TextUtils.isEmpty(name))
                return;

            BleDevice device = new BleDevice(address, name, rssi);
            if (mTGattScaner.devices.contains(device)){
                mTGattScaner.rssis.set(mTGattScaner.devices.indexOf(device), rssi);
            } else{
                mTGattScaner.rssis.add(rssi);
                mTGattScaner.devices.add(device);
            }

            mSubject.onNext(mTGattScaner.devices);
        }
    }

    /**
     * Created by you on 2016/12/3.
     */
    public class TSevice extends Service implements IService{
        private final IBinder mBinder = new LocalBinder();
        private BluetoothManager mBluetoothManager;
        private BluetoothAdapter mBluetoothAdapter;
        private BluetoothGatt mBluetoothGatt;
        private INotification notification;
        private int mConnectionState = BluetoothProfile.STATE_DISCONNECTED;

        // Device scan callback.
        private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(final BluetoothDevice device, final int rssi,
                                 byte[] scanRecord) {
                if (device != null) {
                    TShell.instance.addDeivce(device.getAddress(), device.getName(), rssi);
                }
            }
        };

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

        private void doReadWriteAction(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            boolean b = false;

            if (BluetoothProfile.STATE_CONNECTED != mConnectionState) {
                return;
            }

//            if (!(b=doWrite(bRewrite))) {
//                b = doRead();
//            }
//
//
//            if (writerQueue.size()<=0 || writerDataQueue.size()<=0) {
//                return false;
//            }
//            //XLog.v(TAG, "doWrite()");
//
//            BluetoothGattCharacteristic charac = writerQueue.peek();
//            XData data = writerDataQueue.peek();
//
//
//            if (null != charac && null != data) {
//
//                if (data.length()>0) {
//                    int nLeftSize = (int)data.length();
//                    XLog.i(TAG, "nLeftSize="+nLeftSize);
//                    if (nLeftSize > BEST_WRITE_SIZE) {
//                        nLeftSize = BEST_WRITE_SIZE;
//                    }
//                    byte[] writeData = data.subData(0, nLeftSize);
//                    charac.setValue(writeData);
//                    //b = XTcpClient.defaultTcpClient().write(writeData, 0, writeData.length);
//                    //mCharacLatestWrtingData = charac;
//                    mWriteBeginAt = System.currentTimeMillis();
//                    b = gattMain.writeCharacteristic(charac);
////				mSendBytesCount += nLeftSize;
//                    int _count = 0;
//                    while (!b) {
//                        XLog.e(TAG, "Write fail!");
//                        SystemClock.sleep(15);
//                        b = gattMain.writeCharacteristic(charac);
//                        _count++;
//                        //只重发三次
//                        if(_count>=20)
//                            break;
//                    }
//                    if (b) {
//                        data.replace(0, nLeftSize, null);
//                    }
//                    return b;
//                }
//                else {
//                    // Last write request is finished.
////				System.out.println(" Sleep 20");
////				SystemClock.sleep(20);
//
//                    //XLog.v(TAG, "Remove");
//                    writerQueue.remove();
//                    writerDataQueue.remove();
//                    charac = writerQueue.peek();
//                    data = writerDataQueue.peek();
//                    if (null != charac && null != data) {
//
//                        if (data.length()>0) {
//                            int nLeftSize = (int)data.length();
////						XLog.i(TAG, "nLeftSize="+nLeftSize);
//                            if (nLeftSize > BEST_WRITE_SIZE) {
//                                nLeftSize = BEST_WRITE_SIZE;
//                            }
//                            byte[] writeData = data.subData(0, nLeftSize);
//                            charac.setValue(writeData);
//                            //b = XTcpClient.defaultTcpClient().write(writeData, 0, writeData.length);
//                            mWriteBeginAt = System.currentTimeMillis();
//                            b = gattMain.writeCharacteristic(charac);
////						mSendBytesCount += nLeftSize;
//                            int _count = 0;
//                            while (!b) {
//                                XLog.e(TAG, "Write fail!");
//                                SystemClock.sleep(15);
//                                b = gattMain.writeCharacteristic(charac);
//                                _count++;
//                                //只重发三次
//                                if(_count>=20)
//                                    break;
//                            }
//                            if (b) {
//                                data.replace(0, nLeftSize, null);
//                            }
//                            return b;
//                        }
//                    }
//                }
//            }
//
//            return false;
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return mBinder;
        }

        @Override
        public boolean onUnbind(Intent intent) {
            disconnect();
            return super.onUnbind(intent);
        }

        /**
         * Initializes a reference to the local Bluetooth adapter.
         *
         * @return Return true if the initialization is successful.
         */
        @Override
        public boolean initialize() {
            // For API level 18 and above, get a reference to BluetoothAdapter through
            // BluetoothManager.
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

            return true;
        }

        @Override
        public void setNotification(INotification notification) {
            this.notification = notification;
        }

        @Override
        public void scanDevice(boolean isStart) {
            if (isStart) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mBluetoothAdapter.startLeScan(mLeScanCallback);
            } else
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }

        private void setupGattServices(boolean isStop){
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

            final String macAddr = gatt.getDevice().getAddress();

            if (characteristicUuid.equals(SampleGattAttributes.BODY_TONER_BOLUTEK)) {
                TDataManager.instence.receiveData(rawData, macAddr);
            }
        }

        @Override
        public void write() {
        }

        @Override
        public void connect() {
        }

        @Override
        public void disconnect() {
        }

        protected class LocalBinder extends Binder{
            public TSevice getService() {
                return TSevice.this;
            }
        }

    }

    public class BleDevice{
        int rssi;
        String address;
        String name;

        public BleDevice(String address, String name, int rssi){
            this.address = address;
            this.name = name;
            this.rssi = rssi;
        }
    }

    interface IService {
        boolean initialize();
        void scanDevice(boolean isStart);
        void write();
        void connect();
        void disconnect();
        void setNotification(INotification notification);
    }

    interface INotification{
        void onConnected();
        void onDisconnected();
    }

    interface IGattScaner {
        Subject<ArrayList<BleDevice>> startScan();
        void setFilters(String[] filters);
        void setTimeOut(int timeOut);
        void addDeivce(String address, String name, int rssi);
        void clear();
    }
}
