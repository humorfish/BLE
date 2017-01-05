package com.ultracreation.blelib.tools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.widget.Toast;

import com.ultracreation.blelib.utils.KLog;

import java.util.LinkedList;

/**
 * Created by Administrator on 2016/11/28.
 */

public enum TShell
{
    Shell;

    private final static String TAG = TShell.class.getSimpleName();
    private int requestTimeOut = 3000;

    private boolean isBindService = false;
    private Context context;
    private TService mSevice;
    private String address;
    private TGapConnection connection;
    private TShellRequestManager requestManager;

    private ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder)
        {
            if (iBinder == null)
            {
                throw new Error("bind server failed!!");
            }

            mSevice = ((TService.LocalBinder) iBinder).getService();
            if (! mSevice.initialize())
            {
                KLog.e("mServiceConnection", "Unable to initialize Bluetooth");
                System.exit(0);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName)
        {
            mSevice = null;
        }
    };

    TShell()
    {
        requestManager = new TShellRequestManager();
    }

    public void start(String address)
    {
        this.address = address;
        connect();
    }

    public void stop()
    {
        requestManager.clearRequest();
        if (connection != null)
        {
            connection.disconnect();
            connection = null;
        }
    }

    public void versionRequest(TShellRequest.RequestListener listener)
    {
        requestStart(">ver", requestTimeOut, datas -> datas.contains("v."), listener);
    }

    public void startOutPut(TShellRequest.RequestListener listener)
    {
        requestStart(">osta", requestTimeOut, datas -> datas.contains("0: ok"), listener);
    }

    public void startScriptFile(String fileName, TShellRequest.RequestListener listener)
    {
        requestStart(">ssta " + fileName, requestTimeOut, datas -> datas.contains("0: ok"), listener);
    }

    private void requestStart(String cmd, int timeOut, RequestCallBackFilter callBackFilter, TShellRequest.RequestListener listener)
    {
        TShellSimpleRequest request = new TShellSimpleRequest(cmd, callBackFilter, timeOut, listener);
        requestManager.addRequest(request);

        connect();
    }

    private void connect()
    {
        if (connection != null && connection.isConnected())
        {
            KLog.i(TAG, "-----1--->");
            requestManager.execute();
        } else
        {
            KLog.i(TAG, "-----3--->connection:" + connection);
            if (! TextUtils.isEmpty(address) && connection == null)
            {
                connection = new TGapConnection(address, mSevice, new INotification()
                {
                    @Override
                    public void onConnected()
                    {
                        KLog.i(TAG, "onConnected");
                        requestManager.execute();
                    }

                    @Override
                    public void onConnectedFailed(String message)
                    {
                        KLog.i(TAG, "-----4--->onConnectedFailed");
                        connection = null;
                        requestManager.onError(message);
                    }

                    @Override
                    public void onDisconnected()
                    {
                        connection = null;
                        requestManager.clearRequest();
                    }

                    @Override
                    public void onReceiveData(String Line)
                    {
                        // 获取特殊 信息
                        requestManager.onNotification(Line);
                    }
                });
            } else
            {
                if (TextUtils.isEmpty(address))
                    KLog.i(TAG, "wait device");
                else
                    KLog.i(TAG, "connecting......");
            }
        }
    }

    public boolean bindBluetoothSevice(Context mContext)
    {
        this.context = mContext;

        if (! context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(context, "ble not support", Toast.LENGTH_SHORT).show();
            System.exit(0);
        }

        Intent gattServiceIntent = new Intent(context, TService.class);
        isBindService = context.getApplicationContext().bindService(gattServiceIntent, mServiceConnection, context.BIND_AUTO_CREATE);
        KLog.i(TAG, "bindBluetoothSevice.isBindService:" + isBindService);
        return isBindService;
    }

    public void unBindBluetoothSevice()
    {
        KLog.i(TAG, "bindBluetoothSevice.isBindService:" + isBindService);
        if (isBindService)
            context.getApplicationContext().unbindService(mServiceConnection);
    }


    private class TShellRequestManager implements IShellRequestManager
    {
        private LinkedList<TShellSimpleRequest> requests;
        private TShellSimpleRequest currentRequest;

        public TShellRequestManager()
        {
            requests = new LinkedList<>();
        }

        @Override
        public void addRequest(TShellSimpleRequest request)
        {
            requests.add(request);
            KLog.i(TAG, "addRequest.size:" + requests.size());
        }

        @Override
        public void removeRequest(TShellSimpleRequest request)
        {
            request.clearTimeout();
            if (requests.size() > 0)
                requests.removeFirst();
            KLog.i(TAG, "removeRequest.size:" + requests.size());
        }

        @Override
        public void execute()
        {
            if (requests.size() > 0)
            {
                if (requests.peekFirst() != currentRequest)
                {
                    currentRequest = requests.peekFirst();
                    currentRequest.start();
                }
            }
        }

        @Override
        public void onError(String message)
        {
            if (currentRequest != null)
            {
                removeRequest(currentRequest);
                currentRequest.error(message);
            }
        }

        @Override
        public void onNotification(String message)
        {
            if (currentRequest != null)
            {
                removeRequest(currentRequest);
                execute();
                currentRequest.onNotification(message);
            }
        }

        @Override
        public void clearRequest()
        {
            if (currentRequest != null)
            {
                currentRequest.clearTimeout();
                currentRequest = null;
            }

            requests.clear();
        }
    }

    /* TShellSimpleRequest */

    /**
     * the request narrow to 1 ack 1 answer simple request
     */

    public class TShellSimpleRequest extends TShellRequest
    {
        private RequestCallBackFilter callBackFilter;

        public TShellSimpleRequest(@NonNull String cmd, @NonNull RequestCallBackFilter callBackFilter, int timeOut, @NonNull RequestListener listener)
        {
            super(cmd, timeOut, listener);
            this.callBackFilter = callBackFilter;
        }

        @Override
        void start(Object[]... args)
        {
            connection.write(cmd);
            refreshTimeout();
        }

        @Override
        void onNotification(String line)
        {
            if (callBackFilter.onCall(line))
            {
                next(line);
            }
        }
    }
}
