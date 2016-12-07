package com.ultracreation.ble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ultracreation.blelib.tools.TShell;
import com.ultracreation.blelib.utils.KLog;

import io.reactivex.disposables.Disposable;

import static com.ultracreation.blelib.tools.TShell.instance;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    Disposable mDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        instance.bindBluetoothSevice(this);

        mDisposable = instance.getDevices().subscribe(devices -> {
            KLog.i(TAG, devices.get(0).substring(0, 17));
            mDisposable.dispose();

            Disposable mDisposable = TShell.instance.versionRequest().subscribe(value -> KLog.i(TAG, "ver:"));
            TShell.instance.disposableMap.put(mDisposable);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance.unBindBluetoothSevice();
    }
}
