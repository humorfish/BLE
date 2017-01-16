package com.ultracreation.blelib.impl;

import com.ultracreation.blelib.bean.TSeekOrigin;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Created by you on 2017/1/16.
 */

public abstract class TStream extends IStream
{
    int Seek(TSeekOrigin oirgin, int position)
    {
        return 0;
    }

    Observable<Integer> ReadBuf(byte[] byteArray, int count)
    {
        if (count > byteArray.length)
            count = byteArray.length;

        Subject<Integer> callBack = PublishSubject.create();
        if (count == 0)
        {
            callBack.onNext(0);
            callBack.onComplete();
        }

        int readSize = this.Read(byteArray, count);
        if (readSize <= 0)
        {
            callBack.error(new IllegalStateException());
        }
        else
            callBack.onNext(readSize);

        while (readed < Count)
        {
            let View = new Uint8Array(ByteArray.buffer, ByteArray.byteOffset + readed);
            let reading = this.Read(View, Count - readed);

            if (reading <= 0)
            {
                callBack.error(new EStreamRead());
                return;
            }

            readed += reading;
            callBack.onNext(readed);
        }

        callBack.onComplete();

        return callBack;
    }
}
