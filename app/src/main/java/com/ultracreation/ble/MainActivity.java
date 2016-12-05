package com.ultracreation.ble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ultracreation.blelib.tools.INotification;
import com.ultracreation.blelib.tools.TDataManager;
import com.ultracreation.blelib.tools.TService;
import com.ultracreation.blelib.tools.TShell;
import com.ultracreation.blelib.utils.KLog;

import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    Disposable mDisposable;
    private TService service;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TShell.instance.bindBluetoothSevice(this).subscribe(
                tService -> {
                    service = tService;
                    service.scanDevice(true);
                },
                error -> KLog.i(TAG, "ERROR:" + error.getMessage()));

        mDisposable = TShell.instance.getDevices().subscribe(devices -> {
            KLog.i(TAG, devices.get(0).substring(0, 17));
            mDisposable.dispose();

            service.makeConnection(devices.get(0).substring(0, 17), new INotification() {
                @Override
                public void onConnected() {
                    KLog.i(TAG, "--1-->onConnected");
                    service.write();
                }

                @Override
                public void onConnectedFailed() {
                    KLog.i(TAG, "--2-->onConnectedFailed");
                }

                @Override
                public void onDisconnected() {
                    KLog.i(TAG, "--3-->onDisconnected");
                }
            });
        });

        TDataManager.instence.getData().subscribe(data -> {
            KLog.i(TAG, "data:" + data);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TShell.instance.unBindBluetoothSevice();
    }
}
