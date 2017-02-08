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
import com.ultracreation.blelib.utils.KLog;

import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.content.res.AssetManager.ACCESS_BUFFER;

public class MainActivity extends AppCompatActivity
{
    private final static String TAG = MainActivity.class.getSimpleName();
    private final int REQUEST_ENABLE_BT = 1;
    private TShell shell;

    @BindView(R.id.ver_text)
    Button ver_text;

    @BindView(R.id.model_text)
    Button model_text;

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
            if (TextUtils.isEmpty(deviceName))
                return false;
            else
            {
                String lowerCaseName = deviceName.toLowerCase();
                if (lowerCaseName.endsWith(filters[0]))
                    return true;
                else
                {
                    for (int i = 1; i < filters.length; i++)
                    {
                        if (filters[i].equals(lowerCaseName))
                            return true;
                    }
                }

                return false;
            }

        }, bleDevice -> {
            TGattScaner.Scaner.stop();
            shell = new TShell(bleDevice.device.getAddress());

            shell.connect();
            shell.getVersion().subscribe(s ->
            {
                KLog.i(TAG, "versionRequest.ver:" + new String(s));
                shell.startOutput().subscribe(
                        s1 -> KLog.i(TAG, "startOutPut:" + new String(s1)),
                        error -> KLog.i(TAG, "startOutPut.error:" + error.getMessage())
                );

            }, error ->
            {
                KLog.i(TAG, "versionRequest.error:" + error.getMessage());
            });
        });
    }

    @OnClick({R.id.ver_text, R.id.model_text})
    void onClick(View v)
    {
        switch (v.getId())
        {
            case R.id.ver_text:
                if (shell != null)
                {
                    shell.getVersion().subscribe(
                            s -> KLog.i(TAG, "onClick.versionRequest.ver:" + new String(s)),
                            error -> KLog.i(TAG, "onClick.versionRequest.error:" + error.getMessage()));
                }
                break;

            case R.id.model_text:
                sentFile();
                break;

            default:
                break;
        }
    }

    private void sentFile()
    {
        new Thread(() ->
        {
            String fileName = "pain.lok";

            byte[] fileData = getFileDatas(fileName);
            if (shell != null)
            {
                shell.stopOutput().subscribe(
                        bytes ->
                        {
                            shell.catFile(fileName, fileData).subscribe(
                                    progress -> KLog.i(TAG, "onClick.versionRequest.catFile.progress:" + progress),
                                    err -> KLog.i(TAG, "onClick.versionRequest.error:" + err.getMessage())
                            );
                        },
                        err -> KLog.i(TAG, "onClick.versionRequest.error:" + err.getMessage())
                );
            }

        }).start();
    }

    private byte[] getFileDatas(String fileName)
    {
        InputStream fileIn = null;
        try
        {
            fileIn = this.getAssets().open(fileName, ACCESS_BUFFER);
            byte[] fileData = new byte[fileIn.available()];
            fileIn.read(fileData);

            return fileData;

        } catch (IOException e)
        {
            e.printStackTrace();

        } finally
        {
            if (fileIn != null)
            {
                try
                {
                    fileIn.close();
                    fileIn = null;

                } catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

        return new byte[0];
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
                TGattScaner.Scaner.reStart();

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
        if (shell != null)
        {
            shell.disconnect();
        }
    }
}
