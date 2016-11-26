package com.ultracreation.blelib.utils;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;

public class XByteFormat {
	/**
     * true - do the show byte work.
     * false- do not.
     */
    final static boolean DO_SHOW_BYTES = false;

    /**
     * Show the byte data in the hex format.
     * @param src the source of the byte data.
     * @param offset the offset of the data to show start at.
     * @param length the length of the data to show.
     */
    public static void showBytes(byte[] src, int offset, int length) {
        if (DO_SHOW_BYTES) {
            StringBuilder builder = new StringBuilder();
            for (int i=0; i<length; ++i) {
                builder.append(String.format("%02X ", src[offset+i]));
            }
            XLog.v("UtilityKit", ""+builder.toString());
        }
    }
    
    /**
     * bytes to String.
     * @param src the source of the byte data.
     * @param offset the offset of the data to show start at.
     * @param length the length of the data to show.
     */
    public static final String bytesToStr(byte[] src, int offset, int length) {
        StringBuilder builder = new StringBuilder();
        for (int i=0; i<length; ++i) {
            builder.append(String.format("%02X ", src[offset+i]));
        }
        return builder.toString();
    }
    
    /**
     * Bytes to set to the byte buffer.
     * @param src the byte buffer.
     * @param offset the offset to begin fill.
     * @param length the length to fill.
     * @param value the value to value.
     */
    public static final void memset(byte[] src, int offset, int length, byte value) {
    	int n = offset;
    	for (; n<length; ++n) {
    		src[n] = value;
    	}
    }
    
	/**
	 * 判断assets文件是否存在
	 */
	public static boolean isAssetsFileExist(Context cxt, String pt) {
		AssetManager am = cxt.getAssets();
		try {
			String[] names = am.list("");
			for (int i = 0; i < names.length; i++) {
				if (names[i].equals(pt.trim())) {
					// System.out.println(pt+"文件存在！！！！");
					return true;
				} else {
					// System.out.println(pt+"不存在啦！！！！");
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public static String bytes2HexString(byte[] b) {
		String ret = "";
		try {
			for (int i = 0; i < b.length; i++) {
				String hex = Integer.toHexString(b[i] & 0xFF);
				if (hex.length() == 1) {
					hex = '0' + hex;
				}
				ret += hex.toUpperCase();
			}
			return ret;
		} catch (Exception e){
			e.printStackTrace();
		}

		return ret;
	}
}
