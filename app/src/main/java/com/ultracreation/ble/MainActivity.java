package com.ultracreation.ble;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
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
            if (! TextUtils.isEmpty(deviceName) && deviceName.substring(deviceName.length() - 4, deviceName.length()).equalsIgnoreCase(filters[0]))
                return true;
            else
            {
                for (int i=1; i<filters.length; i++)
                {
                    if (filters[i].equalsIgnoreCase(deviceName))
                        return true;
                }
            }

            return false;
        }, bleDevice -> {
            TGattScaner.Scaner.stop();

            TShell.Shell.start(bleDevice.device.getAddress());
            TShell.Shell.versionRequest(new TShellRequest.RequestListener()
            {
                @Override
                public void onSuccess(String value)
                {
                    KLog.i(TAG, "versionRequest.ver:" + value);
                    TShell.Shell.startOutPut(new TShellRequest.RequestListener()
                    {
                        @Override
                        public void onSuccess(String value)
                        {
                            KLog.i(TAG, "startOutPut.osta:" + value);
                        }

                        @Override
                        public void onFailure(String err)
                        {
                            KLog.i(TAG, "startOutPut.error:" + err);
                        }
                    });
                }

                @Override
                public void onFailure(String err)
                {
                    KLog.i(TAG, "versionRequest.error:" + err);
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
            public void onSuccess(String value)
            {
                KLog.i(TAG, "onClick.versionRequest.ver:" + value);
            }

            @Override
            public void onFailure(String err)
            {
                KLog.i(TAG, "onClick.versionRequest.error:" + err);
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
        TGattScaner.Scaner.stop();
        TShell.Shell.stop();
    }
}
