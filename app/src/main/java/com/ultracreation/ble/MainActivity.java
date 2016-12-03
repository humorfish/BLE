package com.ultracreation.ble;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import com.ultracreation.blelib.tools.TDataManager;
import com.ultracreation.blelib.tools.TShell;
import com.ultracreation.blelib.utils.KLog;

import java.util.ArrayList;
import java.util.TimerTask;

import io.reactivex.disposables.Disposable;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    Disposable mDisposable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TDataManager.instence.getData().subscribe(s -> {
            KLog.i(TAG, "s:" + s);
        });
        mDisposable = TShell.instance.startScan().subscribe(devices -> {
            showDeivces(devices);
        });

        new Handler().postDelayed(new TimerTask() {
            @Override
            public void run() {
                mDisposable.dispose();
            }
        }, 3000);
    }

    private void showDeivces(ArrayList<TShell.BleDevice> devices){}
}
