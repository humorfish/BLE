package com.ultracreation.ble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ultracreation.ble.shell.TDataManager;
import com.ultracreation.blelib.utils.KLog;

public class MainActivity extends AppCompatActivity {
    private final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TDataManager.instence.getData().subscribe(s -> {
            KLog.i(TAG, "s:" + s);
        });
    }
}
