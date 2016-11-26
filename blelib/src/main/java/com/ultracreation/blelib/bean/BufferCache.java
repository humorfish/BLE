package com.ultracreation.blelib.bean;

/**
 * @author liuww
 * @info 读取数据的缓冲区
 */
public class BufferCache {
	
	private int mCurIdx;
	private int mSize;
	private byte[] mBuffer;
	
	private byte[] mEndByte;
	
	public BufferCache(int size) {
		mBuffer = new byte[size];
		
		mCurIdx = 0;
		mSize = size;
		
		mEndByte = new byte[2];
		//0x0D, 0x0A
		mEndByte[0] = 0X0D;
		mEndByte[1] = 0X0A;
	}
	
	public boolean append(byte bData) {
		if(mCurIdx < mSize) {
			mBuffer[mCurIdx++] = bData;
			return true;
		}
		return false;
	}
	
	public boolean append(byte[] data) {
		if(mCurIdx+data.length < mSize) {
			for(int i=0; i<data.length; i++) {
				mBuffer[mCurIdx++] = data[i];
			}
			return true;
		}
		return false;
	}
	
	
	/**
	 * 往buffer中加入数据，当数据满或者遇到结束符的时候，才会返回对应的数据
	 * @param bData
	 * @return
	 */
	public byte[] appendRestSegment(byte bData) {
		byte[] restData = null;
		if(bData == mEndByte[1] && getLastDataByByte() == mEndByte[0]) {
			//读取到结束字符，可以显示或写入
			if(append(bData)) {
				restData = getDataBuff();
			} else {
				restData = new byte[mSize + 1];
				System.arraycopy(mBuffer, 0, restData, 0, mSize);
				restData[mSize] = bData;				
			}
			clear();
		} else if(isFull()){
			//当数据已经满的时候
			restData = getDataBuff();
			clear();
			append(bData);
		} else {
			append(bData);
		}
		return restData;
	}
	
	
	public boolean isFull() {
		if(mCurIdx < mSize)
			return false;
		return true;
	}
	
	
	public void clear() {
		for(int i=0; i<mCurIdx; i++) {
			mBuffer[i] = 0;
		}
		mCurIdx = 0;
	}
	
	public void clear(int startIdx) {
		for(int i=startIdx; i<mCurIdx; i++) {
			mBuffer[i] = 0;
		}
		mCurIdx = startIdx;
	}
	
	
//	public void clear(int startIdx, int endIdx) {
//		for(int i=startIdx; i<mCurIdx; i++) {
//			mBuffer[i] = 0;
//		}
//		mCurIdx = startIdx;
//	}
	
	public byte[] getDataBuff() {
		byte[] data = new byte[mCurIdx];
		System.arraycopy(mBuffer, 0, data, 0, mCurIdx);
		return data;
	}
	
	public byte getLastDataByByte() {
		if(mCurIdx>0)
			return mBuffer[mCurIdx - 1];
		return 0;
	}
	
}
