package com.ultracreation.blelib.tools;

import android.text.TextUtils;

import com.ultracreation.blelib.utils.KLog;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by you on 2016/12/1.
 */
public enum TDataManager {
    instence;

    private static final String TAG = TDataManager.class.getSimpleName();
    private TManager mTManager;

    TDataManager() {
        mTManager = new TManager();
    }

    public void receiveData(byte[] data, String address) {
        mTManager.receiveData(data, address);
    }

    public Subject<String> getData() {
        return mTManager.mSubject;
    }

    private interface IManager {
        void receiveData(byte[] data, String address);
    }

    private class TManager implements IManager {
        Subject<String> mSubject;
        StringBuilder mStringBuilder;

        TManager() {
            mSubject = PublishSubject.create();
            mStringBuilder = new StringBuilder();
        }

        @Override
        public void receiveData(byte[] datas, String address) {
            String tmp = new String(datas);
            if (! TextUtils.isEmpty(tmp)) {
                int index = tmp.indexOf("\r\n");
                KLog.i(TAG, "data:" + tmp.replace("\r\n", "") + "  index:" + index);

                if (index > 0) {
                    String before = tmp.substring(0, index);
                    String after = tmp.substring(index + 2, tmp.length());
                    mStringBuilder.append(before);

                    mSubject.onNext(mStringBuilder.toString());
                    mStringBuilder.setLength(0);
                    mStringBuilder.append(after);
                } else
                    mStringBuilder.append(tmp);
            }

        }
    }
}
