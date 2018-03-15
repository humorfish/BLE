package com.ultracreation.blelib.tools;

import android.bluetooth.BluetoothProfile;
import android.support.annotation.NonNull;

import com.ultracreation.blelib.impl.IShellRequestManager;
import com.ultracreation.blelib.impl.RequestCallBackFilter;
import com.ultracreation.blelib.service.BLEService;
import com.ultracreation.blelib.utils.KLog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import io.reactivex.Observable;
import io.reactivex.subjects.Subject;

/**
 * Created by Administrator on 2016/12/19.
 */

public class TGapConnection extends IGapConnection
{
    private final BLEService mService;
    private String TAG = TGapConnection.class.getSimpleName();
    private TShellRequestManager requestManager;

    public TGapConnection(@NonNull String deviceId, @NonNull BLEService mService)
    {
        super(deviceId);
        this.mService = mService;
        requestManager = new TShellRequestManager();
    }

    @Override
    void start()
    {
        if (mConnectionState == BluetoothProfile.STATE_CONNECTED)
            requestManager.execute();
    }

    @Override
    void addRequest(TShellRequest request)
    {
        requestManager.addRequest(request);
    }

    @Override
    void writeCmd(String cmd)
    {
        if (mConnectionState == BluetoothProfile.STATE_CONNECTED)
        {
            cmd = cmd + "\r\n";
            mService.write(deviceId, cmd.getBytes(), null);
        }
    }

    @Override
    Observable<Integer> writeBuf(byte[] buf)
    {
        return Observable.create(e ->
        {
            if (mConnectionState == BluetoothProfile.STATE_CONNECTED)
            {
                mService.write(deviceId, buf, e);

            } else
            {
                e.onError(new IllegalStateException("not connected"));
            }
        });
    }

    @Override
    void receivedData(byte[] line)
    {
        KLog.i(TAG, "onReceiveData.line:" + new String(line));
        requestManager.onNotification(line);
    }

    @Override
    void destory()
    {
        requestManager.clear();
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
                currentRequest.onError(message);
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
        public void clear()
        {
            if (currentRequest != null)
            {
                currentRequest.clearTimeout();
                currentRequest = null;
            }

            requests.clear();
        }
    }

    /**
     * the request narrow to 1 ack 1 answer simple request
     */

    class TShellSimpleRequest extends TShellRequest
    {
        private RequestCallBackFilter callBackFilter;

        public TShellSimpleRequest(@NonNull String cmd, @NonNull RequestCallBackFilter callBackFilter, int timeOut, @NonNull Subject<byte[]> listener)
        {
            super(cmd, timeOut, listener);
            this.callBackFilter = callBackFilter;
        }

        @Override
        void start()
        {
            refreshTimeout();
            writeCmd(cmd);
        }

        @Override
        void onNotification(byte[] line)
        {
            if (callBackFilter.onCall(line))
            {
                onNext(line);
                onComplete();
            }
        }
    }

    public class TShellCatRequest<T> extends TShellRequest
    {
        private long lastCallBackTime = 0;
        private long CAT_TIMEOUT = 3000;

        private RequestCallBackFilter callBackFilter;
        private byte[] fileData;

        public TShellCatRequest(@NonNull String cmd, @NonNull RequestCallBackFilter callBackFilter, int timeOut, @NonNull byte[] fileData, @NonNull Subject<T> listener)
        {
            super(cmd, timeOut, listener);

            this.callBackFilter = callBackFilter;
            this.fileData = fileData;
            long flushTime = (long) (2.4 * fileData.length);
            if (flushTime > CAT_TIMEOUT)
                CAT_TIMEOUT = flushTime;
        }

        void catFile()
        {
            lastCallBackTime = System.currentTimeMillis();
            refreshTimeout();

            writeCmd(cmd);
            writeBuf(fileData).subscribe(
                    progress ->
                    {
                        KLog.i(TAG, "progress:" + progress);
                        SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss ");
                        Date lastDate = new Date(lastCallBackTime);//获取当前时间
                        String lastTime = formatter.format(lastDate);
                        KLog.i(TAG, "lastTime:" + lastTime + "  waitTime:" + (System.currentTimeMillis() - lastCallBackTime));
                        lastCallBackTime = System.currentTimeMillis();

                        if (progress == 100)
                            onNext(99);
                        else
                            onNext(progress);
                    },
                    err -> onError(err.getMessage()));
        }

        @Override
        void start()
        {
            catFile();
        }

        @Override
        void onNotification(byte[] line)
        {
            if (callBackFilter.onCall(line))
            {
                onNext(100);
                onComplete();
            }
        }

        @Override
        void refreshTimeout()
        {
            clearTimeout();
            setTimeout();
        }

        @Override
        void setTimeout()
        {
            timeOutTask = new TimerTask()
            {
                @Override
                public void run()
                {
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss ");
                    Date lastDate = new Date(lastCallBackTime);//获取当前时间
                    String lastTime = formatter.format(lastDate);
                    KLog.i(TAG, "lastTime:" + lastTime + "  waitTime:" + (System.currentTimeMillis() - lastCallBackTime));

                    if (System.currentTimeMillis() - lastCallBackTime > CAT_TIMEOUT)
                        onError(cmd + " request time out");
                    else
                    {
                        lastCallBackTime = System.currentTimeMillis();
                        refreshTimeout();
                    }
                }
            };

            timer = new Timer();
            timer.schedule(timeOutTask, CAT_TIMEOUT);
        }
    }
}
