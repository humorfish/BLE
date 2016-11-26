/*
 * XData.java
 *
 * Data like NSData on iOS.
 * 
 * Created by:
 * yikui@e-tek.cn
 *
 * 2012-07-10 PM.
 */
package com.ultracreation.blelib.utils;

public class XData {
	
	/**
	 * Construct.
	 */
	public XData() {
		raw = null;
	}
	
	/**
	 * Init with the bytes.
	 * @param src
	 *        raw data.
	 * @param offset
	 *        the offset of the byte to start.
	 * @param length
	 *        the length of the bytes.
	 */
	public XData(byte[] src, long offset, long length) {
		if (length > 0 && src.length > 0) {
			if (offset < src.length) {
				long len = length;
				if (offset+length > src.length) {
					len = src.length - offset;
				}
				raw = new byte[(int)len];
				System.arraycopy(src, (int)offset, raw, 0, (int)len);
			}
		}
	}
	
	/**
	 * Length of the data in bytes.
	 * @return
	 */
	public long length() {
		long len = 0L;
		if (null != raw) {
			len = raw.length;
		}
		return len;
	}
	
	/**
	 * Get the bytes.
	 * @return
	 */
	public final byte[] data() {
		return raw;
	}
	
	/**
	 * Get sub data.
	 * @param location offset from start.
	 * @param length length of the data.
	 * @return the sub data.
	 */
	public final byte[] subData(int location, int length) {
		if (null != raw && length>0 && location>=0) {
			if (raw.length>location) {
				int len = length;
				if (location+length > raw.length) {
					len = raw.length - location;
				}
				if (len > 0) {
					byte[] dest = new byte[len];
					System.arraycopy(raw, location, dest, 0, len);
					return dest;
				}
			}
		}
		return null;
	}
	
	/**
	 * Append bytes.
	 * @param data
	 * @param location
	 * @param length
	 */
	public void append(byte[] data, int location, int length) {
		if (null==data) {
			return;
		}
		if (0==data.length || location<0 || location >= data.length) {
			return;
		}
		int len = length;
		if (location+length > data.length) {
			len = data.length - location;
		}
		if (null==raw) {
			raw = new byte[len];
			System.arraycopy(data, location, raw, 0, len);
		}
		else {
			byte[] dataFinal = new byte[raw.length+len];
			if (raw.length>0) {
				System.arraycopy(raw, 0, dataFinal, 0, raw.length);
			}
			System.arraycopy(data, location, dataFinal, raw.length, len);

			raw = null;
			raw = dataFinal;
		}
	}
	
	/**
	 * Replace the the given data.
	 * @param location
	 * @param length
	 * @param data
	 */
	public void replace(int location, int length, byte[] data) {
		if (null != raw && length>0 && location>=0) {
			if (location < raw.length) {
				int lenFinal = location;
				if (null != data) {
					lenFinal += data.length;
				}
//				Log.d("BodyTonerCmdFormater", "1  location+length: " + (location+length) + " raw.length:" + raw.length);
				if (location+length < raw.length) {
					lenFinal += raw.length - location - length;
				}
				int nCopy = 0;
				byte[] dataFinal = null;
				if (lenFinal>0) {
					dataFinal = new byte[lenFinal];
				}
				if (location>0) {
					System.arraycopy(raw, 0, dataFinal, 0, location);
				}
				nCopy += location;
				if (null!=data) {
					System.arraycopy(data, 0, dataFinal, nCopy, data.length);
					nCopy += data.length;
				}
//				Log.d("BodyTonerCmdFormater", "2  location+length: " + (location+length) + " raw.length:" + raw.length);
				if (location+length < raw.length) {
					System.arraycopy(raw, location+length, dataFinal, nCopy, raw.length-location-length);
				}
				
				raw = null;
				raw = dataFinal;
			}
		}
		else if (null == raw && 0==location && length>0 && null!=data) {
			if (data.length>0) {
				byte[] dataFinal = new byte[data.length];
				System.arraycopy(data, 0, dataFinal, 0, data.length);
				
				raw = null;
				raw = dataFinal;
			}
		}
	}
	
	/**
	 * Clear the buffer.
	 */
	public void clear() {
		raw = null;
	}

	/**
	 * Raw data.
	 */
	private byte[] raw = null;
}
