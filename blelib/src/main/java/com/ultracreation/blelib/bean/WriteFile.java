package com.ultracreation.blelib.bean;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class WriteFile {
	private FileOutputStream mOutputStream = null;
	
	public WriteFile(String path, String name) {
		try {
			mOutputStream = new FileOutputStream(path+"/" + name);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	public boolean writeData(byte[] data) {
		if(mOutputStream!=null) {
			try {
				mOutputStream.write(data);
				return true;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
	
	public void free() {//test
		if(mOutputStream!=null) {
			try {
				mOutputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
