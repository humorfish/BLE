package com.ultracreation.blelib.impl;

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
    abstract int Read(byte[] byteArray, int number);
    abstract int Write(byte[] byteArray, int number);
    abstract int Seek(int offset, TSeekOrigin oirgin);

    /**
     *  Stream Guarantee IO
     */
    abstract Observable<Integer> ReadBuf(byte[] byteArray, int number);
    abstract Observable<Integer> WriteBuf(byte[] byteArray, int number);

    /**
     *  Stream Line IO
     */
    abstract Observable<String> ReadLn();
    abstract Observable<Integer>  WriteLn(String line);

    /**
     *  Stream Properties, implements with getter /setter
     */
    int Size;
    int Position;
    IllegalStateException Endianness;
}
