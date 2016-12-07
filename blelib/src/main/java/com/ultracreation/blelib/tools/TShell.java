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
import java.util.HashMap;
import java.util.Map;

import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2016/11/28.
 */

public enum TShell {
    instance;

    private final static String TAG = TShell.class.getSimpleName();
    public Map<String, Disposable> disposableMap;
    private Context context;
    private TGattScaner mTGattScaner;
    private Subject<TService> serviceSubject;
    private TService mSevice;

    private ServiceConnection mServiceConnection = new ServiceConnection() {
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

    TShell() {
        mTGattScaner = new TGattScaner();
        serviceSubject = PublishSubject.create();
        disposableMap = new HashMap<>();
    }

    public Subject<String> Request(TService service, String cmd, Subject<String> isCallBack, int timeOut) {
        return service.write(cmd.getBytes(), timeOut, isCallBack);
    }

    public void setFilters(String[] filters) {
        mTGattScaner.setFilters(filters);
    }

    public void setTimeOut(int timeOut) {
        mTGattScaner.setTimeOut(timeOut);
    }

    public void refreshConnectionTimeout() {
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

    /* TShellSimpleRequest */

    /**
     * the request narrow to 1 ack 1 answer simple request, most cases toPromise
     */

    class TShellSimpleRequest extends TShellRequest {
        private String Cmd;
        private CallBack callBack;

        public TShellSimpleRequest(TShell shell, int Timeout, String tag) {
            super(shell, Timeout, tag);
        }

        @Override
        void Start(String Cmd, CallBack callBack, Object[] ...args) {
            this.Cmd = Cmd;
            this.callBack = callBack;

            if (mSevice == null){
                error();
                return;
            }

            ObserveSend(this.Cmd)
                    .then(Observer -> Observer.subscribe(next -> this.refreshTimeout()))
            .catch(err -> this.error(err));
        }

        @Override
        void Notification(String line) {
            try {
                if (this.callBack.onCall(line)) {
                    this.doOnNext(line);
                    this.complete();
                }
            } catch (Exception e) {
                this.error();
            }
        }

        @Override
        protected void subscribeActual(Observer observer) {

        }
    }
}
