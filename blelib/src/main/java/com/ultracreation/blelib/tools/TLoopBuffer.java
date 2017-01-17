package com.ultracreation.blelib.tools;

import android.support.annotation.NonNull;

import java.nio.ByteBuffer;

/**
 * Created by you on 2016/12/3.
 */
public class TLoopBuffer
{
    private ByteBuffer mMemory;
    private int readIndex;
    private int writeIndex;

    public TLoopBuffer(int size)
    {
        readIndex = writeIndex = 0;
        if (size < 1024)
            size = 1024;

        mMemory = ByteBuffer.allocate(size);
    }

    public int size()
    {
        return mMemory.capacity();
    }

    public int count()
    {
        return (writeIndex + mMemory.capacity() - readIndex) % mMemory.capacity();
    }

    public int avail()
    {
        return mMemory.capacity() - count();
    }

    public boolean isEmpty()
    {
        return readIndex == writeIndex;
    }

    public boolean isFull()
    {
        return (writeIndex + 1) % mMemory.capacity() == readIndex;
    }

    public byte[] memory()
    {
        return mMemory.array();
    }

    public void clear()
    {
        readIndex = writeIndex = 0;
    }

    public boolean push(@NonNull final byte[] datas, int count)
    {
        if (count > datas.length)
            count = datas.length;

        if (count > avail())
            return false;

        for (int i = 0; i < count; i++)
        {
            mMemory.put(writeIndex, datas[i]);
            writeIndex = (writeIndex + 1) % mMemory.capacity();
        }
        
        return true;
    }

    public byte[] extractTo(int extractSize)
    {
        if (extractSize > count())
            extractSize = count();

        byte[] datas = new byte[extractSize];

        for (int i = 0; i < extractSize; i++)
        {
            datas[i] = mMemory.get(readIndex);
            readIndex = (readIndex + 1) % mMemory.capacity();
        }
        
        return datas;
    }
}
