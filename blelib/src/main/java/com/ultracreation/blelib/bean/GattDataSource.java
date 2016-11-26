package com.ultracreation.blelib.bean;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.ultracreation.blelib.utils.XBluetoothGatt;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GattDataSource {
	private final String TAG = "GattDataSource";
	//���ʱ��ms
	private final static int RUNTIME_BLUETOOTH_TICK_TIME = 500;
	//����ͬʱ���ӵ��豸����
	public final static byte RUNTIME_BLUETOOTH_MAX_CONNECT_COUNT = 3;

	//Ĭ�ϵ���������ʱ�� 5s
	public final static int RUNTIME_BLUETOOTH_CONNECT_TICK_DEF = 1000 / RUNTIME_BLUETOOTH_TICK_TIME * 30;

	//��ǰ�����Ľ�������ʱ��
//	public final static int RUNTIME_BLUETOOTH_CONNECT_TICK_CUR_PAGE = 30;

	//���ӳ�ʱ��ʱ 3s
	private final static int RUNTIME_BLUETOOTH_CONNECT_ING_TIME_OUT = 1000 / RUNTIME_BLUETOOTH_TICK_TIME * 3;

	//�Ͽ����ӳ�ʱ��ʱ 2s
	private final static int RUNTIME_BLUETOOTH_DISCONNECT_ING_TIME_OUT = 1000 / RUNTIME_BLUETOOTH_TICK_TIME * 2;

	//����Ҫ�Զ��Ͽ��ļ�ʱ
	public final static int RUNTIME_BLUETOOTH_CONNECT_TICK_NOT_RUN = -1;


	// The address list.
	private ConcurrentHashMap<String, XBluetoothGatt> mGattMap = new ConcurrentHashMap<String, XBluetoothGatt>();
	
	/**
	 * Get the count of the gatt.
	 * @return
	 */
	public int count() {
		return mGattMap.size();
	}

	/**
	 * Get the item by the mac address.
	 * @param macAddr
	 * @return XBluetoothGatt.
	 */
	public XBluetoothGatt itemFor(String macAddr) {
		XBluetoothGatt xBluetoothGatt = null;
		if (mGattMap != null)
			try {
				xBluetoothGatt = mGattMap.get(macAddr);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return xBluetoothGatt;
	}

	/**
	 * Get the core BluetoothGatt address.
	 * @param macAddr
	 * @return
	 */
	public BluetoothGatt itemForCore(String macAddr) {
		final XBluetoothGatt gat;
		try {
			gat = mGattMap.get(macAddr);
			if (null != gat) {
				return gat.getGatt();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Add item.
	 * @param newItem
	 */
	public void addItem(String macAddr, XBluetoothGatt newItem) {
		mGattMap.put(macAddr, newItem);
	}


	/**
	 * Remove item.
	 */
	public void removeItem(String macAddr) {
		final XBluetoothGatt gat = mGattMap.get(macAddr);
		if (null != gat) {
			gat.onDestroyActionCheck();
		}
		mGattMap.remove(macAddr);
	}
	
	/**
	 * Clear items.
	 */
	public void clear() {
		mGattMap.clear();	
	}
	
	/**
	 * Return all items.
	 * @return
	 */
	public Collection<XBluetoothGatt> allItems() {
		return mGattMap.values();
	}

	/**
	 * The default source.
	 * @return
	 * the source object.
	 */
	public static GattDataSource defaultSrc() {
		if (null == mSrcObj) {
			mSrcObj = new GattDataSource();
		}
		return mSrcObj;
	}

	// The singleton object.
	private static GattDataSource mSrcObj = null;

	/**
	 * �Ͽ����е�����
	 */
	public void disconnectAll () {
		Iterator iter = mGattMap.entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			Log.d(TAG, "disconnectAll addr:" + entry.getKey());
			XBluetoothGatt gatt = (XBluetoothGatt) entry.getValue();
			gatt.getGatt().disconnect();

			try {
				Thread.sleep(30);

				if (null != gatt.getGatt()) {
					gatt.getGatt().close();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		clear();
	}


	public boolean isHaveConnectingGatt() {
		Iterator iter = mGattMap.entrySet().iterator();
		int count = 0;

		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			XBluetoothGatt gatt = (XBluetoothGatt) entry.getValue();

			if (gatt.isConnecting()){
				count++;
				Log.d(TAG, "^^^^^^^^^^^^^^^^^^^ isHaveConnectingGatt  mac:" + entry.getKey() + " count:" + count);
			}
		}

		if (count > 0) {
			return true;
		}
		return false;
	}


	/**
	 * @return �Ͽ����������е��豸
	 */
	public boolean disconnectConnectingGatt () {
		Iterator iter = mGattMap.entrySet().iterator();
		XBluetoothGatt maxGatt = null;

		while (iter.hasNext()) {
			Map.Entry entry = (Map.Entry) iter.next();
			XBluetoothGatt gatt = (XBluetoothGatt) entry.getValue();

			if (gatt.isConnecting()){
				maxGatt = gatt;
			}
		}

		Log.d(TAG, "disconnectConnectingGatt maxGatt:" + maxGatt);
		if (maxGatt != null) {
			Log.d(TAG, "disconnectConnectingGatt ******** disconnect mac:" + maxGatt.getGatt().getDevice().getAddress() );
			maxGatt.setConnectionState(BluetoothProfile.STATE_DISCONNECTING);
			maxGatt.getGatt().disconnect();
			return true;
		}
		return false;
	}

	/**
	 * @return �ﵽ���������
	 */
	public boolean isConnectExceedMax() {
		Log.d(TAG, " *** isConnectExceedMax:" + count() + " RUNTIME_BLUETOOTH_MAX_CONNECT_COUNT:" + RUNTIME_BLUETOOTH_MAX_CONNECT_COUNT);
		if (count() >= RUNTIME_BLUETOOTH_MAX_CONNECT_COUNT)
			return true;
		return false;
	}

}
