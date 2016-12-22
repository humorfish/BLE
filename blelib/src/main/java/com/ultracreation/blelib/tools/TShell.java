package com.ultracreation.blelib.tools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
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
    private boolean isBindService = false;
    private Context context;
    private TService mSevice;
    private String address;
    private TGapConnection connection;

    private LinkedList<TShellSimpleRequest> requests;

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
            if (!mSevice.initialize())
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
        requests = new LinkedList<>();
    }


    public void get(String address)
    {
        this.address = address;
    }

    public void versionRequest(TShellRequest.RequestListener listener)
    {
        requestStart(">ver", 10000, datas -> datas.contains("v."), listener);
    }

    private void PromiseSend(TShellSimpleRequest request, String cmd)
    {
        this.makeConnection(new INotification()
        {
            @Override
            public void onConnected()
            {
                request.Start(cmd);
            }

            @Override
            public void onConnectedFailed()
            {
                connection = null;
            }

            @Override
            public void onDisconnected()
            {
                connection = null;
            }

            @Override
            public void onReceiveData(String Line)
            {
                if (requests.size() > 0)
                {
                    requests.peekFirst().Notification(Line);
                    requests.removeFirst();
                }
            }
        });
    }

    private void requestStart(String cmd, int timeOut, CallBack callBack, TShellRequest.RequestListener listener)
    {
        TShellSimpleRequest request = new TShellSimpleRequest(callBack, timeOut, cmd);
        requests.add(request);

        if (connection == null)
            PromiseSend(request, cmd);
        else
            request.Start(cmd);

        request.mSubject.subscribe(listener::onSuccessful, err -> listener.onFailed(err.getMessage()));
    }

    public TGapConnection makeConnection(INotification mNotification)
    {
        if (connection != null && connection.isConnected())
            return connection;
        else
        {
            connection = new TGapConnection(address, mSevice, mNotification);
            return connection;
        }
    }


    public void refreshConnectionTimeout()
    {
    }

    public void clearConnectTimeOut()
    {
    }

    public boolean bindBluetoothSevice(Context mContext)
    {
        this.context = mContext;

        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(context, "ble not support", Toast.LENGTH_SHORT).show();
            System.exit(0);
        }

        Intent gattServiceIntent = new Intent(context, TService.class);
        isBindService = context.bindService(gattServiceIntent, mServiceConnection, context.BIND_AUTO_CREATE);
        KLog.i(TAG, "bindBluetoothSevice.isBindService:" + isBindService);
        return isBindService;
    }

    public void unBindBluetoothSevice()
    {
        KLog.i(TAG, "bindBluetoothSevice.isBindService:" + isBindService);
        if (isBindService)
            context.unbindService(mServiceConnection);
    }


    /* TShellSimpleRequest */

    /**
     * the request narrow to 1 ack 1 answer simple request, most cases toPromise
     */

    class TShellSimpleRequest extends TShellRequest
    {
        public CallBack callBack;
        public String cmd;


        public TShellSimpleRequest(CallBack callBack, int Timeout, String cmd)
        {
            super(Timeout);
            this.cmd = cmd;
            this.callBack = callBack;
        }

        @Override
        void Start(String cmd, Object[]... args)
        {
            connection.write(cmd);
        }

        @Override
        void Notification(String line)
        {
            if (callBack.onCall(line))
            {
                next(line);
                complete();
            }
        }
    }
}
