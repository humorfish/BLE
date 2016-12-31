package com.ultracreation.ble;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.ultracreation.blelib.tools.TGattScaner;
import com.ultracreation.blelib.tools.TShell;
import com.ultracreation.blelib.tools.TShellRequest;
import com.ultracreation.blelib.utils.KLog;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity
{
    private final static String TAG = MainActivity.class.getSimpleName();
    private final int REQUEST_ENABLE_BT = 1;

    @BindView(R.id.test)
    Button test;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        String[] filters = {".blt", "bluetensx", "bluetensq"};
        TGattScaner.Scaner.initBluetooth(this, REQUEST_ENABLE_BT);
        TGattScaner.Scaner.start(deviceName ->
        {
            if (deviceName != null)
                return true;
            else
                return false;
        }, bleDevice -> {
            TGattScaner.Scaner.stop();

            TShell.Shell.get(bleDevice.device.getAddress());
            TShell.Shell.versionRequest(new TShellRequest.RequestListener()
            {
                @Override
                public void onSuccessful(String value)
                {
                    KLog.i(TAG, "ver:" + value);
                }

                @Override
                public void onFailed(String err)
                {
                    KLog.i(TAG, "error:" + err);
                }
            });
        });
    }

    @OnClick({R.id.test})
    void onClick(View v)
    {
        TShell.Shell.versionRequest(new TShellRequest.RequestListener()
        {
            @Override
            public void onSuccessful(String value)
            {
                KLog.i(TAG, "ver:" + value);
            }

            @Override
            public void onFailed(String err)
            {
                KLog.i(TAG, "error:" + err);
            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == RESULT_OK)
            {
                Toast.makeText(this, "蓝牙已经开启", Toast.LENGTH_SHORT).show();
            } else
            {
                Toast.makeText(this, "不允许蓝牙开启", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }
}
