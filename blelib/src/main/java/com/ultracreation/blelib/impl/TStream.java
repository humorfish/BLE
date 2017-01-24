package com.ultracreation.blelib.impl;

import com.ultracreation.blelib.bean.TSeekOrigin;

import java.nio.ByteBuffer;

import io.reactivex.Observable;

/**
 * Created by you on 2017/1/16.
 */

public abstract class TStream extends IStream
{
    @Override
    Observable<byte[]> readBuf(byte[] byteArray, int count)
    {
        return Observable.create(e ->
        {
            int mCount = count;
            if (mCount > byteArray.length)
                mCount = byteArray.length;

            if (mCount == 0)
            {
                e.onNext(new byte[0]);
                e.onComplete();
            } else
            {
                byte[] bytes = read(count);
                int readedSize = 0;
                if (bytes == null || bytes.length <= 0)
                {
                    e.onError(new IllegalStateException("empty array"));
                    return;
                } else
                {
                    readedSize = bytes.length;
                    e.onNext(bytes);
                }

                while (readedSize < mCount)
                {
                    byte[] readedBytes = read(mCount - readedSize);

                    if (readedBytes == null || readedBytes.length <= 0)
                    {
                        e.onError(new IllegalStateException("empty array"));
                        return;
                    }

                    readedSize += readedBytes.length;
                    e.onNext(readedBytes);
                }

                e.onComplete();
            }
        });
    }

    @Override
    Observable<Integer> writeBuf(byte[] byteArray, int count)
    {
        return Observable.create(e ->
        {
            int mCount = count;
            if (mCount > byteArray.length)
                mCount = byteArray.length;

            if (mCount == 0)
            {
                e.onNext(0);
                e.onComplete();
            } else
            {
                int wroteSize = write(byteArray);
                if (wroteSize <= 0)
                {
                    e.onError(new IllegalStateException("empty array"));
                    return;
                } else
                    e.onNext(wroteSize);

                int arrayOffset = 0;
                ByteBuffer buffer = ByteBuffer.allocate(mCount);
                while (wroteSize < mCount)
                {
                    byte[] tmpArrary = buffer.get(byteArray, arrayOffset, wroteSize).array();
                    arrayOffset = wroteSize;

                    int writting = write(tmpArrary);

                    if (writting <= 0)
                    {
                        e.onError(new IllegalStateException("empty array"));
                        return;
                    }

                    wroteSize += writting;
                    e.onNext(wroteSize);
                }

                e.onComplete();
            }
        });
    }

    @Override
    Observable<String> readLn()
    {
        return Observable.create(e -> e.onError(new IllegalStateException("not implements")));
    }

    @Override
    Observable<Integer> writeLn(final String line)
    {
        final String LINE_BREAK = "\r\n";
        byte[] array = (line + LINE_BREAK).getBytes();
        return writeBuf(array, array.length);
    }

    int size()
    {
        int curr = seek(0, TSeekOrigin.FormCurrent);
        int retval = seek(0, TSeekOrigin.FromEnd) - seek(0, TSeekOrigin.FormBeginning);
        seek(curr, TSeekOrigin.FormBeginning);

        return retval;
    }

    int getPosition()
    {
        return seek(0, TSeekOrigin.FormCurrent);
    }

    void setPosition(int position)
    {
        seek(position, TSeekOrigin.FormBeginning);
    }
}
