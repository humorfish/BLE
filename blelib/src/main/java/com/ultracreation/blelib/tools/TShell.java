package com.ultracreation.blelib.tools;

import android.bluetooth.BluetoothProfile;
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
import java.util.LinkedList;
import java.util.Map;

import io.reactivex.disposables.Disposable;
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
    private TService mSevice;
    private String address;
    private INotification mNotification;
    private LinkedList<TShellSimpleRequest> requests;

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

            mSevice.scanDevice(true);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mSevice = null;
        }
    };

    TShell() {
        mTGattScaner = new TGattScaner();
        disposableMap = new HashMap<>();
        requests = new LinkedList<>();
    }

    public void get(String address, String[] filters, int timeOut) {
        this.address = address;
        mTGattScaner.setFilters(filters);
        mTGattScaner.setTimeOut(timeOut);

        mNotification = new INotification() {
            @Override
            public void onConnected() {
                if (requests.size() > 0) {
                    TShellSimpleRequest request = requests.peekFirst();
                    request.Start(request.cmd, request.callBack);
                }
            }

            @Override
            public void onConnectedFailed() {
            }

            @Override
            public void onDisconnected() {
                requests.clear();
            }
        };
    }

    public Subject<String> versionRequest() {
        return requestStart(">ver", 3000, new CallBack() {
            @Override
            public boolean onCall(String datas) {
                if (datas.contains("ver"))
                    return true;
                else
                    return false;
            }
        });
    }

    private Subject<String> requestStart(String cmd, int timeOut, CallBack callBack) {
        TShellSimpleRequest request = new TShellSimpleRequest(this, timeOut, cmd);
        requests.add(request);

        if (mSevice.mConnectionState != BluetoothProfile.STATE_CONNECTED)
            mSevice.makeConnection(address, mNotification);
        else
            request.Start(cmd, callBack);

        return request.mSubject;
    }

    public void receiveData(String line) {
        if (requests.size() > 0) {
            requests.peekFirst().Notification(line);
            requests.removeFirst();
        }
    }

    public void refreshConnectionTimeout() {
    }

    public Subject<ArrayList<String>> getDevices() {
        return mTGattScaner.getDevices();
    }

    public void addDeivce(String address, String name, int rssi) {
        mTGattScaner.addDevice(address, name, rssi);
    }

    public boolean bindBluetoothSevice(Context mContext) {
        this.context = mContext;

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(context, "ble not support", Toast.LENGTH_SHORT).show();
            System.exit(0);
        }

        Intent gattServiceIntent = new Intent(context, TService.class);
        return context.bindService(gattServiceIntent, mServiceConnection, context.BIND_AUTO_CREATE);
    }

    public void unBindBluetoothSevice() {
        context.unbindService(mServiceConnection);
    }

    /* TShellSimpleRequest */

    /**
     * the request narrow to 1 ack 1 answer simple request, most cases toPromise
     */

    class TShellSimpleRequest extends TShellRequest {
        public CallBack callBack;
        public String cmd;

        public TShellSimpleRequest(TShell shell, int Timeout, String cmd) {
            super(shell, Timeout, cmd);
            this.cmd = cmd;
        }

        @Override
        void Start(String cmd, CallBack callBack, Object[]... args) {
            this.callBack = callBack;
            mSevice.write((cmd + "\r\n").getBytes());
        }

        @Override
        void Notification(String line) {
            if (callBack.onCall(line)) {
                next(line);
                complete();
            }
        }
    }
}
