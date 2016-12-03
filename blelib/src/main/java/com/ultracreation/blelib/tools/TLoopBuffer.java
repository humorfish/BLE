package com.ultracreation.blelib.tools;

import java.nio.ByteBuffer;

/**
 * Created by you on 2016/12/3.
 */
public class TLoopBuffer {

    private ByteBuffer _Memory;
    private int bufferSize;
    private int ReadIndex;
    private int WriteIndex;

    TLoopBuffer(int Size)
    {
        this.ReadIndex = this.WriteIndex = 0;
        this._Memory = ByteBuffer.allocate(Size);
    }

    int size()
    {
        return bufferSize;
    }

    int count()
    {
        return (this.WriteIndex + bufferSize - this.ReadIndex) % bufferSize;
    }

    int avail()
    {
        return bufferSize - count();
    }

    boolean isEmpty()
    {
        return (this.ReadIndex == this.WriteIndex);
    }

    boolean isFull()
    {
        return (this.WriteIndex + 1) % bufferSize == this.ReadIndex;
    }

    byte[] memory()
    {
        return this._Memory.array();
    }

    void clear()
    {
        this.ReadIndex = this.WriteIndex = 0;
    }

    boolean push(final byte[] datas, int count)
    {
        if (count > datas.length)
            count = datas.length;

        if (count > avail())
            return false;

        for (int i = 0; i < count; i++)
        {
            _Memory.put(WriteIndex, datas[i]);
            WriteIndex = (this.WriteIndex + 1) % bufferSize;
        }
        return true;
    }

    int ExtractTo(byte[] datas, int count)
    {
        if (count > datas.length)
            count = datas.length;

        if (count > count())
            count = count();

        if (count > 0)
        {
            for (int i = 0; i < count; i ++)
            {
                datas[i] = _Memory.get(ReadIndex);
                ReadIndex = (ReadIndex + 1) % bufferSize;
            }
        }
        return count;
    }

    byte[] Extract(int count)
    {
        if (count > this.count())
            count = this.count();
        if (count == 0)
            return null;

        byte[] mBytes = new byte[count];
        this.ExtractTo(mBytes, count);
        return mBytes;
    }

}
