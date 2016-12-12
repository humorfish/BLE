package com.ultracreation.ble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ultracreation.blelib.utils.KLog;

import static com.ultracreation.blelib.tools.TGattScaner.Scaner;
import static com.ultracreation.blelib.tools.TShell.Shell;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String[] filters = {".blt", "bluetensx", "bluetensq"};

        Scaner.start(deviceName ->
            {
                if (deviceName != null)
                    return true;
                else
                    return false;
            }, bleDevice ->
            {
                Scaner.stop();

                Shell.get(bleDevice.device.getAddress());
                Shell.versionRequest().subscribe(
                        value -> KLog.i(TAG, "ver:"),
                        error -> KLog.i(TAG, "error:" + error.getMessage())

                );
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shell.unBindBluetoothSevice();
    }
}
