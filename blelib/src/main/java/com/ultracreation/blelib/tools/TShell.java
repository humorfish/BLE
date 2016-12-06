package com.ultracreation.blelib.tools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.widget.Toast;

import com.ultracreation.blelib.utils.KLog;

import java.util.ArrayList;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2016/11/28.
 */

public enum TShell {
    instance;

    private final static String TAG = TShell.class.getSimpleName();
    private Context context;
    private TGattScaner mTGattScaner;
    private Subject<TService> serviceSubject;

    TShell() {
        mTGattScaner = new TGattScaner();
        serviceSubject = PublishSubject.create();
    }


    public Subject<String> Request(TService service, String cmd, Subject<String> isCallBack, int timeOut){
        return service.write(cmd.getBytes(), timeOut, isCallBack);
    }

    public void setFilters(String[] filters) {
        mTGattScaner.setFilters(filters);
    }

    public void setTimeOut(int timeOut) {
        mTGattScaner.setTimeOut(timeOut);
    }

    public Subject<ArrayList<String>> getDevices() {
        return mTGattScaner.getDevices();
    }

    public void clear() {
        mTGattScaner.clear();
    }

    public void addDeivce(String address, String name, int rssi) {
        mTGattScaner.addDevice(address, name, rssi);
    }

    public Subject<TService> bindBluetoothSevice(Context mContext) {
        this.context = mContext;

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(context, "ble not support", Toast.LENGTH_SHORT).show();
            System.exit(0);
        }

        Intent gattServiceIntent = new Intent(context, TService.class);
        context.bindService(gattServiceIntent, mServiceConnection, context.BIND_AUTO_CREATE);

        return serviceSubject;
    }

    public void unBindBluetoothSevice() {
        context.unbindService(mServiceConnection);
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        private TService mSevice;

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            if (iBinder == null) {
                throw new Error("bind server failed!!");
            }

            mSevice = ((TService.LocalBinder) iBinder).getService();
            if (!mSevice.initialize()) {
                KLog.e("mServiceConnection", "Unable to initialize Bluetooth");
                System.exit(0);
            }

            serviceSubject.onNext(mSevice);
            serviceSubject.onComplete();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSevice = null;
            serviceSubject.onError(new Error("service disconned"));
        }
    };

}
