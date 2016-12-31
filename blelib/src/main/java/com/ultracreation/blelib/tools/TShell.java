package com.ultracreation.blelib.tools;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.NonNull;
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
        requestManager = new TShellRequestManager();
    }

    public void get(String address)
    {
        this.address = address;
    }

    public void versionRequest(TShellRequest.RequestListener listener)
    {
        requestStart(">ver", requestTimeOut, datas -> datas.contains("v."), listener);
    }

    private void requestStart(String cmd, int timeOut, RequestCallBackFilter callBackFilter, TShellRequest.RequestListener listener)
    {
        TShellSimpleRequest request = new TShellSimpleRequest(cmd, callBackFilter, timeOut, listener);
        requestManager.addRequest(request);

        if (connection == null)
            connect(request);
        else
            requestManager.execute();
    }

    private void connect(TShellSimpleRequest request)
    {
        if (connection != null)
            return;

        connection = new TGapConnection(address, mSevice, new INotification()
        {
            @Override
            public void onConnected()
            {
                requestManager.execute();
            }

            @Override
            public void onConnectedFailed(String message)
            {
                connection = null;
                request.error(message);
            }

            @Override
            public void onDisconnected()
            {
                connection = null;
            }

            @Override
            public void onReceiveData(String Line)
            {
                request.onNotification(Line);
            }
        });
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

        public TShellRequestManager()
        {
            requests = new LinkedList<>();
        }

        @Override
        public void addRequest(TShellSimpleRequest request)
        {
            requests.add(request);
        }

        @Override
        public void removeRequest(TShellSimpleRequest request)
        {
            requests.remove(request);
        }

        @Override
        public void execute()
        {
            if (requests.size() > 0)
            {
                TShellSimpleRequest request = requests.peekFirst();
                request.start();
                requests.remove(request);
            }
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
