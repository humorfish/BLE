package com.ultracreation.blelib.tools;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * Created by you on 2016/12/3.
 */
public class TLoopBuffer
{
    private ByteBuffer _Memory;
    private int ReadIndex;
    private int WriteIndex;

    public TLoopBuffer(int size)
    {
        this.ReadIndex = this.WriteIndex = 0;
        if (size < 1024)
            size = 1024;

        this._Memory = ByteBuffer.allocate(size);
    }

    public int size()
    {
        return _Memory.capacity();
    }

    public int count()
    {
        return (this.WriteIndex + _Memory.capacity() - this.ReadIndex) % _Memory.capacity();
    }

    public int avail()
    {
        return _Memory.capacity() - count();
    }

    public boolean isEmpty()
    {
        return this.ReadIndex == this.WriteIndex;
    }

    public boolean isFull()
    {
        return (this.WriteIndex + 1) % _Memory.capacity() == this.ReadIndex;
    }

    public byte[] memory()
    {
        return this._Memory.array();
    }

    public void clear()
    {
        this.ReadIndex = this.WriteIndex = 0;
    }

    public boolean push(@NonNull final byte[] datas, int count)
    {
        if (count > datas.length)
            count = datas.length;

        if (count > avail())
            return false;

        for (int i = 0; i < count; i++)
        {
            _Memory.put(WriteIndex, datas[i]);
            WriteIndex = (this.WriteIndex + 1) % _Memory.capacity();
        }
        return true;
    }

    public byte[] extractTo(int extractSize)
    {
        if (extractSize > this.count())
            extractSize = this.count();

        byte[] datas = new byte[extractSize];

        for (int i = 0; i < extractSize; i++)
        {
            datas[i] = _Memory.get(ReadIndex);
            ReadIndex = (ReadIndex + 1) % _Memory.capacity();
        }
        return datas;
    }
}
