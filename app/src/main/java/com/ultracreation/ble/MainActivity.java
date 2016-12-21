package com.ultracreation.ble;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.ultracreation.blelib.tools.TGattScaner;
import com.ultracreation.blelib.tools.TShell;
import com.ultracreation.blelib.tools.TShellRequest;
import com.ultracreation.blelib.utils.KLog;

import static com.ultracreation.blelib.tools.TShell.Shell;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        String[] filters = {".blt", "bluetensx", "bluetensq"};

        TGattScaner.Scaner.start(deviceName -> {
                if (deviceName != null)
                    return true;
                else
                    return false;
            }, bleDevice -> {
                TGattScaner.Scaner.stop();

                TShell.Shell.get(bleDevice.device.getAddress());
                TShell.Shell.versionRequest(new TShellRequest.RequestListener() {
                    @Override
                    public void onSuccessful(String value) {
                        KLog.i(TAG, "ver:");
                    }

                    @Override
                    public void onFailed(String err) {
                        KLog.i(TAG, "error:" + err);
                    }
                });
            });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Shell.unBindBluetoothSevice();
    }
}
