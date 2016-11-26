package com.ultracreation.blelib.bean;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;

public class NewDeviceDataSource {
	
	// The address list.
	private ArrayList<String> mDevicesAddressArrList = new ArrayList<>();
	
	/**
	 * Get the count of the devices.
	 * @return
	 */
	public int count() {
		return mDevicesAddressArrList.size();
	}
	
	/**
	 * Get the item at the given index.
	 * @param nIndex
	 * @return the BluetoothDevice.
	 */
	public String itemAtIndex(int nIndex) {
		return mDevicesAddressArrList.get(nIndex);
	}
	
	/**
	 * Check if contain the device.
	 * @param newItem
	 * @return
	 */
	public boolean contain(String newItem) {
		return mDevicesAddressArrList.contains(newItem);
	}
	
	/**
	 * Add item.
	 * @param newItem
	 */
	public void addItem(String newItem) {
		if (!mDevicesAddressArrList.contains(newItem)) {
			mDevicesAddressArrList.add(newItem);
		}
	}
	
	/**
	 * Remove item.
	 * @param item
	 */
	public void removeItem(BluetoothDevice item) {
		if (!mDevicesAddressArrList.contains(item)) {
			mDevicesAddressArrList.remove(item);
		}
	}
	
	/**
	 * Remove item.
	 */
	public void removeItem(String macAddr) {
		String device = null;
		for (String d : mDevicesAddressArrList) {
			if (macAddr.equalsIgnoreCase(d)) {
				device = d;
			}
		}
		if (null != device) {
			mDevicesAddressArrList.remove(device);
		}
	}
	
	/**
	 * Clear items.
	 */
	public void clear() {
		mDevicesAddressArrList.clear();		
	}

	/**
	 * The default source.
	 * @return
	 * the source object.
	 */
	public static NewDeviceDataSource defaultSrc() {
		if (null == mSrcObj) {
			mSrcObj = new NewDeviceDataSource();
		}
		return mSrcObj;
	}
	
	private NewDeviceDataSource() {
	}
	
	// The singleton object.
	private static NewDeviceDataSource mSrcObj = null;
}
