package com.ultracreation.blelib.tools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ultracreation.blelib.service.BluetoothLeService;

import java.util.ArrayList;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2016/11/28.
 */

public enum  TShell {
    instance;

    private Context context;
    private TGattScaner mTGattScaner;
    private BluetoothLeService mBluetoothLeService;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (iBinder == null) {
                throw new Error("bind server failed!!");
            }

            mBluetoothLeService = ((BluetoothLeService.LocalBinder) iBinder).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e("mServiceConnection", "Unable to initialize Bluetooth");
                System.exit(0);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
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
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
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

    interface IGattScaner {
        Subject<ArrayList<BleDevice>> startScan();
        void setFilters(String[] filters);
        void setTimeOut(int timeOut);
        void addDeivce(String address, String name, int rssi);
        void clear();
    }
}
