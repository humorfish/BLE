package com.ultracreation.ble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ultracreation.blelib.tools.TShell;
import com.ultracreation.blelib.utils.KLog;

import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    Disposable mDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TShell.instance.bindBluetoothSevice(this);

        String[] filters = {"bluetensx", "bluetensq"};

        mDisposable = TShell.instance.getDevices(filters).subscribe(devices -> {
            KLog.i(TAG, devices.get(0).substring(0, 17));
            mDisposable.dispose();
            TShell.instance.get(devices.get(0).substring(0, 17));
            TShell.instance.versionRequest().subscribe(
                    value -> KLog.i(TAG, "ver:"),
                    error -> KLog.i(TAG, "error:" + error.getMessage())

            );
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        TShell.instance.unBindBluetoothSevice();
    }
}
