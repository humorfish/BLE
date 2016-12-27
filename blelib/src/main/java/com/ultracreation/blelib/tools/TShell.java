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
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by Administrator on 2016/11/28.
 */

public enum TShell
{
    Shell;

    private final static String TAG = TShell.class.getSimpleName();
    private int connectTimeOut = 10000;
    private int requestTimeOut = 3000;

    private boolean isBindService = false;
    private Context context;
    private TService mSevice;
    private String address;
    private TGapConnection connection;
    private TimeOutManager timeOutManager;
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
        requests = new LinkedList<>();
        timeOutManager = new TimeOutManager();
    }

    public void get(String address)
    {
        this.address = address;
    }

    public void versionRequest(TShellRequest.RequestListener listener)
    {
        requestStart(">ver", requestTimeOut, datas -> datas.contains("v."), listener);
    }

    private void connect(TShellSimpleRequest request, String cmd)
    {
        timeOutManager.startConnectTimeOut(connectTimeOut, message -> KLog.i(TAG, "" + message));

        this.makeConnection(new INotification()
        {
            @Override
            public void onConnected()
            {
                timeOutManager.clearTimeOut();
                timeOutManager.startQuestTimeOut(request.TimeoutInterval, message -> KLog.i(TAG, "" + message));
                request.start(cmd);
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
                    requests.peekFirst().onNotification(Line);
                    requests.removeFirst();

                    if (timeOutManager.timeOutList.size() > 0)
                    {
                        timeOutManager.removeTimeOut();
                    }
                }
            }
        });
    }

    private void requestStart(String cmd, int timeOut, CallBack callBack, TShellRequest.RequestListener listener)
    {
        TShellSimpleRequest request = new TShellSimpleRequest(callBack, timeOut, cmd);
        requests.add(request);

        if (connection == null)
            connect(request, cmd);
        else
            request.start(cmd);

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

    private class TShellSimpleRequest extends TShellRequest
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
        void start(String cmd, Object[]... args)
        {
            connection.write(cmd);
        }

        @Override
        void onNotification(String line)
        {
            if (callBack.onCall(line))
            {
                next(line);
                complete();
            }
        }
    }

    class TimeOutManager implements ITimeOutManager
    {
        private Timer timer;
        private TimerTask timerTask;
        private LinkedList<ITimeOutCallBack> timeOutList;

        public TimeOutManager()
        {
            timeOutList = new LinkedList<>();
        }

        @Override
        public void startConnectTimeOut(int timeOut, ITimeOutCallBack callBack)
        {
            setTimeOut(timeOut, callBack);
        }

        @Override
        public void startQuestTimeOut(int timeOut, ITimeOutCallBack callBack)
        {
            setTimeOut(timeOut, callBack);
        }

        @Override
        public void setTimeOut(int timeOut, ITimeOutCallBack callBack)
        {
            timeOutList.add(callBack);
            timer = new Timer();
            timerTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    timeOutList.peekFirst().onCall("time out");
                    timeOutList.removeFirst();
                }
            };

            timer.schedule(timerTask, timeOut);
        }

        @Override
        public void clearTimeOut()
        {
            if (timerTask != null)
            {
                timerTask.cancel();
                timerTask = null;
            }

            if (timer != null)
            {
                timer.cancel();
                timer = null;
            }
        }

        @Override
        public void removeTimeOut()
        {
            if (timeOutList.size() > 0)
                timeOutList.removeFirst();
        }
    }

}
