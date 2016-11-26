package com.ultracreation.blelib.bean;

import java.util.ArrayList;
import java.util.HashMap;

public class DeviceRuntimeDataSource {
	
	/**
	 * Keys.
	 * @notice if you add new keys here, you should set up a default value for it at
	 * @link {genDefault}
	 */
	public final static String MAC_ADDR = "MAC_ADDR";
	public final static String FW_VERSION  = "FW_VERSION";
	public final static String SYNC_TIME = "SYNC_TIME";
	public final static String BATTERY  = "BATTERY";
	public final static String RSSI     = "RSSI";
	public final static String ON_FINDING = "ON_FINDING";
	public final static String ON_RESERVE_FINDING = "ON_RESERVE_FINDING";
	public final static String ON_MOVEING  = "ON_MOVEING";
	public final static String ON_CHARGING  = "ON_CHARGING";
	
	/**
	 * Main data container.
	 */
	private ArrayList<HashMap<String, String>> mData = new ArrayList<HashMap<String, String>>();
	
	/**
	 * Get the count of the items.
	 * @return
	 */
	public int count() {
		return mData.size();
	}
	
	/**
	 * Get item at index.
	 * @param index
	 * @return the hash map items.
	 */
	public HashMap<String, String> itemAtIndex(int index) {
		if (index < mData.size()) {
			return mData.get(index);
		}
		return null;
	}
	
	/**
	 * Get the string formated value.
	 * @param macAddr
	 * @param keyStr
	 * @return
	 */
	public String getValue(String macAddr, String keyStr) {
		HashMap<String, String> mapItem = findItem(macAddr);
		if (null != mapItem) {
			return mapItem.get(keyStr);
		}
		return "";
	}
	
	/**
	 * Get the boolean formated value.
	 * @param macAddr
	 * @param keyStr
	 * @return
	 */
	public boolean getBooleanValue(String macAddr, String keyStr) {
		HashMap<String, String> mapItem = findItem(macAddr);
		if (null != mapItem) {
			final String str = mapItem.get(keyStr);
			if (null != str) {
				if (str.equals("1")) {
					return true;
				}
			} 
		}
		return false;
	}
	
	/**
	 * Set the value.
	 * @param macAddr
	 * @param keyStr
	 * @param strValue
	 * @return
	 */
	public void setValue(String macAddr, String keyStr, String strValue) {
		HashMap<String, String> mapItem = findItem(macAddr);
		if (null != mapItem) {
			mapItem.put(keyStr, strValue);
		}
	}
	
	/**
	 * Add an new device and gen default values.
	 * @param macAddr
	 */
	public void addNewDevice(String macAddr) {
		if (!existDevice(macAddr)) {
			HashMap<String, String> newMap = new HashMap<String, String>();
			newMap.put(MAC_ADDR, macAddr);
			genDefault(newMap);
			mData.add(newMap);
		}
	}
	
	/**
	 * Remove device.
	 * @param macAddr
	 */
	public void removeDevice(String macAddr) {
		int n = 0;
		int nMax = mData.size();
		boolean bFind = false;
		for (n=0; n<nMax; ++n) {
			HashMap<String, String> m = mData.get(n);
			String macAddrItem = m.get(MAC_ADDR);
			if (macAddrItem.equalsIgnoreCase(macAddr)) {
				bFind = true;
				break;
			}
		}
		if (bFind) {
			mData.remove(n);
		}
	}
	
	/**
	 * Clear the content.
	 */
	public void clear() {
		mData.clear();
	}
	
	/**
	 * Check if exist the device.
	 * @param macAddr
	 * @return true - exist, false - not.
	 */
	public boolean existDevice(String macAddr) {
		return null != findItem(macAddr);
	}
	
	/**
	 * Find the item with given MAC Address.
	 * @param macAddr
	 * @return null - not found.
	 */
	private HashMap<String, String> findItem(String macAddr) {
		HashMap<String, String> mapItem = null;
		for (HashMap<String, String> m : mData) {
			String macAddrItem = m.get(MAC_ADDR);
			if (macAddrItem.equalsIgnoreCase(macAddr)) {
				mapItem = m;
			}
		}
		return mapItem;
	}
	
	private void genDefault(HashMap<String, String> newMap) {
		newMap.put(BATTERY, "100");
		newMap.put(FW_VERSION, "0");
		newMap.put(RSSI, "-90");
		newMap.put(ON_FINDING, "0");
		newMap.put(ON_RESERVE_FINDING, "0");
		newMap.put(ON_MOVEING, "0");
		newMap.put(ON_CHARGING, "0");
	}
	
	/**
	 * Get the singleton object.
	 * @return
	 */
	public static DeviceRuntimeDataSource defaultSrc() {
		if (null == mSrcObj) {
			mSrcObj = new DeviceRuntimeDataSource(); 			
		}
		return mSrcObj;
	}
	
	// The contructor, prevent user instant.
	private DeviceRuntimeDataSource() {
	}
	
	// The source object.
	private static DeviceRuntimeDataSource mSrcObj = null;
}
