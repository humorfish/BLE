package com.ultracreation.blelib.tools;

import android.support.annotation.NonNull;

import com.ultracreation.blelib.bean.TSeekOrigin;
import com.ultracreation.blelib.impl.TStream;

import java.nio.ByteBuffer;

/**
 * Created by you on 2017/1/17.
 */

public class TMemoryStream extends TStream
{
    public TMemoryStream(Object args)
    {
        if (args instanceof Number)
        {
            int number = (int)args;
            if (number < 512)
                number = 512;

            mMemory = new byte[number];
            this.mSize = this.mPosition = 0;

        } else if (args instanceof byte[])
        {
            byte[] buf = (byte[]) args;
            mMemory = buf;
            this.mPosition = 0;
            this.mSize = buf.length;

        } else
            throw new IllegalStateException("unable handle type");
    }

    @Override
    public byte[] read(@NonNull byte[] byteArray, int count)
    {
        if (count > byteArray.length)
            count = byteArray.length;

        if (count > this.mSize - this.mPosition)
            count = this.mSize - this.mPosition;

        if (count > 0)
        {
            ByteBuffer buffer = ByteBuffer.allocate(count);
            byte[] des = buffer.get(mMemory, mPosition, count).array();
            mPosition += count;
            return des;
        }else
            return new byte[0];
    }

    @Override
    public int write(byte[] byteArray, int count)
    {
        if (count > byteArray.length)
            count = byteArray.length;

        if (count > 0)
        {
            this.Expansion(count);
            ByteBuffer buffer = ByteBuffer.allocate(count);
            byte[] des = buffer.get(mMemory, mPosition, count).array();
            System.arraycopy(des, 0, mMemory, mPosition, des.length);

            this.mPosition += count;
            if (this.mPosition > mSize)
                mSize = this.mPosition;
        }
        return count;
    }

    @Override
    public int seek(int offset, TSeekOrigin oirgin)
    {
        switch(oirgin)
        {
            case FormBeginning:
                mPosition = offset;
                break;
            case FormCurrent:
                mPosition += offset;
                break;
            case FromEnd:
                mPosition = mSize + offset;
                break;
            default:
                break;
        }

        if (mPosition < 0)
            mPosition = 0;
        else if (mPosition > mSize)
            mPosition = mSize;

        return mPosition;
    }

    void Expansion(int count)
    {
        if (mMemory.length - this.mPosition >= count)
            return;

        if (mMemory.length < count)
            count *= 2;
        else
            count = mMemory.length * 2;

        setCapacity(count);
    }

    void setCapacity(int value)
    {
        byte[] old = mMemory.clone();
        mMemory = new byte[value];
        System.arraycopy(old, 0, mMemory, 0, old.length);
    }

    int getCapacity()
    {
        return mMemory.length;
    }


    void clear()
    {
        mSize = mPosition = 0;
    }

    /**
     *  Memory Property is violatile
     */
    byte[] getMemory()
    {
        return mMemory;
    }

    /**
     *  MemoryView Property is violatile
     */
    byte[] getMemoryView()
    {
        ByteBuffer buffer = ByteBuffer.allocate(mSize);
        return buffer.get(mMemory, 0, mSize).array();
    }

    int getSize()
    {
        return mSize;
    }

    protected byte[] mMemory;
    protected int mSize;
    protected int mPosition;
}
