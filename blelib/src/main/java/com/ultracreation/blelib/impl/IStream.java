package com.ultracreation.blelib.impl;

import android.support.annotation.NonNull;

import com.ultracreation.blelib.bean.TSeekOrigin;

import io.reactivex.Observable;

/**
 * Created by you on 2017/1/16.
 */

public abstract class IStream
{
    /**
     *  Stream Basic Operations Read/Write/Seek
     */
    public abstract byte[] read(int count);
    public abstract int write(@NonNull byte[] byteArray);
    public abstract int seek(int offset, TSeekOrigin oirgin);

    /**
     *  Stream Guarantee IO
     */
    abstract Observable<byte[]> readBuf(byte[] byteArray, int count);
    abstract Observable<Integer> writeBuf(byte[] byteArray, int count);

    /**
     *  Stream Line IO
     */
    abstract Observable<String> readLn();
    abstract Observable<Integer>  writeLn(String line);

    /**
     *  Stream Properties, implements with getter /setter
     */
    int size;
    int position;
    IllegalStateException endianness;
}
