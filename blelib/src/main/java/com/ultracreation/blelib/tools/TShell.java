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

import com.ultracreation.blelib.impl.INotification;
import com.ultracreation.blelib.impl.IShellRequestManager;
import com.ultracreation.blelib.impl.RequestCallBackFilter;
import com.ultracreation.blelib.utils.KLog;

import java.util.LinkedList;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;


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

    public Subject<byte[]> getVersion()
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = new TShellSimpleRequest(this, ">ver", datas -> new String(datas).contains("0: ok"), requestTimeOut, listener);
        requestStart(request);
        return listener;
    }

    public Subject<byte[]> getStatus()
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = new TShellSimpleRequest(this, ">stat", datas -> new String(datas).contains("0: ok"), requestTimeOut, listener);
        requestStart(request);
        return listener;
    }

    public Subject<byte[]> startOutput()
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = new TShellSimpleRequest(this, ">osta", datas -> new String(datas).contains("0: ok"), requestTimeOut, listener);
        requestStart(request);
        return listener;
    }

    public Subject<byte[]> stopOutput()
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = new TShellSimpleRequest(this, ">osto", datas -> new String(datas).contains("0: ok"), requestTimeOut, listener);
        requestStart(request);
        return listener;
    }

    public Subject<byte[]> fileMd5(String fileName)
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = new TShellSimpleRequest(this, ">md5 " + fileName, datas -> new String(datas).contains("0: ok"), requestTimeOut, listener);
        requestStart(request);
        return listener;
    }

    public Subject<byte[]> catFile(String fileName, byte[] fileData)
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellCatRequest request = new TShellCatRequest(this, ">cat " + fileName + " -l=" + fileData.length, datas -> new String(datas).contains("3:end of cat"), requestTimeOut, fileData, listener);
        requestStart(request);
        return listener;
    }

    public Subject<byte[]> startScriptFile(String fileName)
    {
        Subject<byte[]> listener = PublishSubject.create();
        TShellSimpleRequest request = new TShellSimpleRequest(this, ">ssta " + fileName, datas -> new String(datas).contains("0: ok"), requestTimeOut, listener);
        requestStart(request);
        return listener;
    }

    private void requestStart(TShellRequest request)
    {
        requestManager.addRequest(request);
        connect();
    }

    private void connect()
    {
        if (connection != null && connection.isConnected())
        {
            requestManager.execute();
        } else
        {
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
                        KLog.i(TAG, "-----4--->onConnectedFailed.message" + message);
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
                    public void onReceiveData(byte[] line)
                    {
                        requestManager.onNotification(line);
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
        private LinkedList<TShellRequest> requests;
        private TShellRequest currentRequest;

        public TShellRequestManager()
        {
            requests = new LinkedList<>();
        }

        @Override
        public void addRequest(TShellRequest request)
        {
            requests.add(request);
            KLog.i(TAG, "addRequest.size:" + requests.size());
        }

        @Override
        public void removeRequest(TShellRequest request)
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
        public void onNotification(byte[] datas)
        {
            if (currentRequest != null)
            {
                removeRequest(currentRequest);
                execute();
                currentRequest.onNotification(datas);
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
        private TShell proxy;
        private RequestCallBackFilter callBackFilter;

        public TShellSimpleRequest(@NonNull TShell proxy, @NonNull String cmd, @NonNull RequestCallBackFilter callBackFilter, int timeOut, @NonNull Subject<byte[]> listener)
        {
            super(cmd, timeOut, listener);
            this.callBackFilter = callBackFilter;
            this.proxy = proxy;
        }

        @Override
        void start(Object[]... args)
        {
            if (proxy.connection != null)
                proxy.connection.write(cmd);

            refreshTimeout();
        }

        @Override
        void onNotification(byte[] line)
        {
            if (callBackFilter.onCall(line))
            {
                next(line);
            }
        }
    }

    /* TShellRequest */
    public abstract class TProxyShellRequest extends TShellRequest
    {
        protected TShell proxy;
        public TProxyShellRequest(@NonNull TShell proxy, @NonNull String cmd, int timeOut, @NonNull Subject<byte[]> listener)
        {
            super(cmd, timeOut, listener);
            this.proxy = proxy;
        }

        abstract void catFile();
    }

    public class TShellCatRequest extends TProxyShellRequest
    {
        private RequestCallBackFilter callBackFilter;
        private byte[] fileData;

        public TShellCatRequest(@NonNull TShell proxy, @NonNull String cmd, @NonNull RequestCallBackFilter callBackFilter, int timeOut, @NonNull byte[] fileData, @NonNull Subject<byte[]> listener)
        {
            super(proxy, cmd, timeOut, listener);

            KLog.i(TAG, "---1--->TShellCatRequest");
            this.callBackFilter = callBackFilter;
            this.fileData = fileData;
        }

        @Override
        void catFile()
        {
            proxy.stopOutput().subscribe(s ->
            {
                if (proxy.connection != null)
                {
                    proxy.connection.write(cmd);
                    proxy.connection.writeNoResponse(fileData);
                }
            }, error ->
            {
                error("stop running failed");
            });
        }

        @Override
        void start(Object[]... args)
        {
            KLog.i(TAG, "---2--->TShellCatRequest.start");
            catFile();
        }

        @Override
        void onNotification(byte[] Line)
        {
            if (callBackFilter.onCall(Line))
            {
                next(Line);
            }
        }
    }
}
