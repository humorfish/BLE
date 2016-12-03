package com.ultracreation.blelib.tools;

import android.text.TextUtils;

import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by you on 2016/12/1.
 */
public enum TDataManager {
    instence;

    private TManager mTManager;

    TDataManager(){
        mTManager = new TManager();
    }

    public void receiveData(byte[] data){
        mTManager.receiveData(data);
    }

    public Subject<String> getData(){
        return mTManager.mSubject;
    }

    private class TManager implements IManager{
        Subject<String> mSubject;
        StringBuilder mStringBuilder;

        TManager(){
            mSubject = PublishSubject.create();
            mStringBuilder = new StringBuilder();
        }

        @Override
        public void receiveData(byte[] data) {
            String tmp = new String(data);
            if (!TextUtils.isEmpty(tmp))
            {
                int index = tmp.indexOf("\r\n");
                if (index > 0)
                {
                    String before = mStringBuilder.substring(0, index);
                    String after = mStringBuilder.substring(index + 1, mStringBuilder.length());
                    mSubject.onNext(before);
                    mStringBuilder.setLength(0);
                    mStringBuilder.append(after);
                } else
                    mStringBuilder.append(tmp);
            }
        }
    }


    private interface IManager{
        void receiveData(byte[] data);
    }
}
