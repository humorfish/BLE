/*
 * XBluetoothGatt.java
 *
 * The powerful class to handle the bluetoothgatt on android.
 * 
 * Created by:
 * yikui@e-tek.cn
 *
 * 2014-07-10 PM.
 */

package com.ultracreation.blelib.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.SystemClock;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

public class XBluetoothGatt {

	/**
	 * Debug tag.
	 */
	private final static String TAG = "XBluetoothGatt";//.class.getName();

	/**
	 * The timeout value for the sending.
	 */
	private final static long SENDING_TIMEOUT = 30000;

	/**
	 * The max size to write to the characteristic.
	 */
	private final static int INIT_WRITE_SIZE = 20;
	
	/**
	 * The high speed.
	 */
	private final static int HIGH_SPEED_WRITE_SIZE = 20;
	
	/**
	 * The low speed.
	 */
	private final static int LOW_SPEED_WRITE_SIZE = 80;//20;
	
	/**
	 * 
	 */
	private final static int LIMIT_WRITE_SPEED = 68;
	
	/**
	 * The Max count to check the write speed.
	 */
	private final static int MAX_CHECK_SPEED_COUNT = 16;
	
	/**
	 * The best write size.
	 */
	private int BEST_WRITE_SIZE = INIT_WRITE_SIZE;

	/**
	 * The BluetoothGatt this object holding.
	 */
	private BluetoothGatt gattMain = null;

	/**
	 * The state of the connection.
	 */
	private int nConnectionState = BluetoothProfile.STATE_DISCONNECTED;
	
	/**
	 * true - on action, can not read/write.
	 * false- not on action.
	 */
	private static boolean bOnAction = false;
	
	// The time out record.
	private static long sendingTimeoutRecord = System.currentTimeMillis();
	
	/**
	 * true - on action, can not read/write.
	 * false- not on action.
	 */
	private boolean bSelfAction = false;
	
	
	/**
	 * 直接操作蓝牙发送数据模式，不通过消息
	 */
	private boolean isDirectModel = false;
	
	
	
	/**
	 * @return 返回当前写入数据模式
	 */
	public boolean isDirectModel() {
		return isDirectModel;
	}

	/**
	 * @param isDirectModel 设置数据发送模式
	 */
	public void setDirectModel(boolean isDirectModel) {
		this.isDirectModel = isDirectModel;
	}


	/**
	 * Speed test.
	 */
	private long mWriteBeginAt=0, mWriteResponseAt=0;
	private long mSpeed = 0;
	private long mSpeedCheckCount = 0;
	private boolean mSpeedAlreadyFixed = false;
	
	/**
	 * The limit send data byte - force sleep a bit.
	 */
	private final static long LIMIT_SEND_BYTES_FORCE_SLEEP = 60;
	private long mSendBytesCount = 0L;
	
	
	/**
	 * The Read/Writer task queue.
	 */
	private Queue<BluetoothGattCharacteristic> readerQueue = null;
	private Queue<BluetoothGattCharacteristic> writerQueue = null;
	private Queue<XData> writerDataQueue = null;
	
	/**
	 * The queue for write the descriptor. 
	 */
	//private Queue<String> mDescriptionWriterMacAddrQueue = new LinkedList<String>();
	private Queue<BluetoothGattDescriptor> mDescriptionWriterQueue = null;//new LinkedList<BluetoothGattDescriptor>();
	
	/**
	 * Constructor with given BluetoothGatt object.
	 * @param gatt
	 *        The Associated BluetoothGatt object.
	 */
	public XBluetoothGatt(final BluetoothGatt gatt) {
		gattMain = gatt;
		
		// Build the queue buffers.
		readerQueue = new LinkedList<BluetoothGattCharacteristic>();
		writerQueue = new LinkedList<BluetoothGattCharacteristic>();
		writerDataQueue = new LinkedList<XData>();
		mDescriptionWriterQueue = new LinkedList<BluetoothGattDescriptor>();
	}
	
	/**
	 * Get the BluetoothGatt object.
	 */
	public final BluetoothGatt getGatt() {
		return gattMain;
	}
	
	/**
	 * Check if connected or not.
	 * @return
	 */
	public boolean isConnected() {
		return BluetoothProfile.STATE_CONNECTED == nConnectionState;
	}

	/**
	 * Check if 连接中...
	 * @return
	 */
	public boolean isConnecting() {
		return BluetoothProfile.STATE_CONNECTING == nConnectionState;
	}


	/**
	 * @return 是否断开中...
	 */
	public boolean isDisconnecting() {
		return BluetoothProfile.STATE_DISCONNECTING == nConnectionState;
	}


	/**
	 * @return 是否已经断开
	 */
	public boolean isDisconnected() {
		return BluetoothProfile.STATE_DISCONNECTED == nConnectionState;
	}

	/**
	 * Set the new connection.
	 * @param nNewState
	 */
	public void setConnectionState(int nNewState) {
		nConnectionState = nNewState;
		if (BluetoothProfile.STATE_CONNECTED==nNewState) {
			clearAll();
		}
	}
	
	/**
	 * Get the state of the connection.
	 * @return
	 */
	public int getConnectionState() {
		return nConnectionState;
	}
	
	/**
	 * Submit descriptor write request.
	 * @param desc
	 */
	public void descriptorRequest(final BluetoothGattDescriptor desc) {
		if (isConnected()) {
			mDescriptionWriterQueue.add(desc);
		}
	}
	
	/**
	 * Submit a read request.
	 * @param characteristic
	 *        The characteristic to read.
	 */
	public void readRequest(final BluetoothGattCharacteristic characteristic) {
		if (!readerQueue.contains(characteristic) && isConnected()) {
			readerQueue.add(characteristic);
		}
	}
	
	/**
	 * Submit a write request.
	 * @param characteristic
	 *        The characteristic to write.
	 * @param data
	 *        The data to write.
	 */
	public void writeRequest(final BluetoothGattCharacteristic characteristic, final XData data) {
		if (isConnected()) {
			writerQueue.add(characteristic);
			writerDataQueue.add(data);
		}
	}
	
	/**
	 * On descriptor write result, this will trigger next write(descriptor) session.
	 * @param gatt 
	 * @param descriptor
	 * @param status
	 */
	public void onDescriptorWriteResult(BluetoothGatt gatt,
										BluetoothGattDescriptor descriptor, int status) {
		//mDescriptionWriterQueue.remove();
		/**
		 * Write action is over.
		 */
		bOnAction = false;
		bSelfAction = false;
		
		/**
		 * Read
		 */
		if(!isDirectModel)
			doReadWriteAction(false);
	}

	/**
	 * On read result, this will trigger next read action.
	 * @param characteristic
	 *        The characteristic that read.
	 * @param status
	 *        The status indicate success or not.
	 *        BluetoothGatt.GATT_SUCCESS - success.
	 */
	public void onReadResult(final BluetoothGattCharacteristic characteristic, int status) {
		readerQueue.remove();
		
		// Reset the send bytes.
		mSendBytesCount = 0L;

		/**
		 * Read action is over.
		 */
		bOnAction = false;
		bSelfAction = false;
		
		/**
		 * Write
		 */
		if(!isDirectModel)
			doReadWriteAction(false);
	}
	
	/**
	 * On write result, this will trigger next write action.
	 * @param characteristic
	 *        The characteristic that read.
	 * @param status
	 *        The status indicate success or not.
	 *        BluetoothGatt.GATT_SUCCESS - success.
	 */
	public void onWriteResult(final BluetoothGattCharacteristic characteristic, int status) {
		/**
		 * Write action is over.
		 */
		bOnAction = false;
		bSelfAction = false;
		
		/**
		 * Read
		 */
		if(!isDirectModel)
			doReadWriteAction(BluetoothGatt.GATT_SUCCESS!=status?true:false);
	}
	
	/**
	 * Set Action to false before this object destroy.
	 */
	public void onDestroyActionCheck() {
		if (bSelfAction) {
			bOnAction = false;
		}
	}
	
	
	/**
	 * 直接调用发送数据接口 不用等待
	 * @param characteristic
	 * @param data
	 * @return
	 */
	private long totalPack = 0;
	private long sendingTimeoutRecord2 = 0;
	public boolean writeDataDirect(BluetoothGattCharacteristic characteristic, byte[] data, int packWaitTime) {
		int n = 0;
		int _minLen = 0;
		boolean b = false;
		int _countSleep = 0;
		
		//当以直接模式发送数据的时候，不响应其它信息键
		if(!isDirectModel)
			isDirectModel = true;
		
		//增加临时变量，以后最后更改 test
		if(data.length==1 && data[0] == '?') {
			BodyTonerCmdFormater.isShellAskSync = true;
		}
		
		while(n<data.length) {
			Log.d(TAG, "writeDataDirect .................waitTime:" + packWaitTime +" dectime:" + (System.currentTimeMillis()-sendingTimeoutRecord2));
			
			if (System.currentTimeMillis()-sendingTimeoutRecord2 < packWaitTime) {
				Log.d(TAG, "writeDataDirect ................._countSleep:" + _countSleep++);
				SystemClock.sleep(5);
				continue;
			}
			
			if( (data.length-n)>INIT_WRITE_SIZE) {
				_minLen = INIT_WRITE_SIZE;
			} else {
				_minLen = data.length - n;
			}
			
			byte[] writeData = new byte[_minLen];
			System.arraycopy(data, n, writeData, 0, _minLen);
			Log.d(TAG, "writeDataDirect .................n:" + n + " _minLen:" + _minLen + " totalPack:" + totalPack++);
			characteristic.setValue(writeData);
			characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE);
			b = gattMain.writeCharacteristic(characteristic);
			int _count = 0;
			while (!b) {
				XLog.e(TAG, "Write fail!");
				SystemClock.sleep(15);
				b = gattMain.writeCharacteristic(characteristic);
				_count++;
				//只重发三次
				if(_count>=20)
					break;
			}
			n +=_minLen;
			_countSleep = 0;
			sendingTimeoutRecord2 = System.currentTimeMillis();
		} 
//		if( doRead()) {
//			
//		}
		
		return b;
	}
	
	/**
	 * Do the read write action.
	 *         true - do read first if possible, false - do write first if possible.
	 * @param bRewrite
	 *         true - rewrite the data.
	 */
	private void doReadWriteAction(boolean bRewrite) {
		boolean b = false;
		
		if (BluetoothProfile.STATE_CONNECTED != nConnectionState) {
			return;
		}
		
		if (bOnAction) {
			System.out.println("doReadWriteAction bOnAction:" + bOnAction);
			if (System.currentTimeMillis()-sendingTimeoutRecord < SENDING_TIMEOUT) {
				return;
			}
		}
		
		if (!(b=doWrite(bRewrite))) {
			b = doRead();
		}

		bOnAction = b;
		bSelfAction = bOnAction;
		sendingTimeoutRecord = System.currentTimeMillis();
	}

	/**
	 * Do the read action.
	 * @return
	 *      true - success. otherwise - fail.
	 */
	private boolean doRead() {
		if (readerQueue.size()<=0) {
			//gattMain.readRemoteRssi();
			return false;
		}
		XLog.v(TAG, "doRead()");
		BluetoothGattCharacteristic charac = readerQueue.peek();
		if (null != charac) {
			return gattMain.readCharacteristic(charac);
		}
		else {
			XLog.v(TAG, "null ");
		}
		readerQueue.remove();
		return false;
	}
	
	//private boolean mOnNotification = true;

	/**
	 * Do the write action.
	 * @param bRewrite true - rewrite the latest data.
	 * @return
	 *      true - success. otherwise - fail.
	 */
	private boolean doWrite(boolean bRewrite) {
		boolean b = false;

		if (writerQueue.size()<=0 || writerDataQueue.size()<=0) {
			return false;
		}
		//XLog.v(TAG, "doWrite()");

		BluetoothGattCharacteristic charac = writerQueue.peek();
		XData data = writerDataQueue.peek();
		
		
		if (null != charac && null != data) {

			if (data.length()>0) {								
				int nLeftSize = (int)data.length();
				XLog.i(TAG, "nLeftSize="+nLeftSize);
				if (nLeftSize > BEST_WRITE_SIZE) {
					nLeftSize = BEST_WRITE_SIZE;
				}
				byte[] writeData = data.subData(0, nLeftSize);
				charac.setValue(writeData);
				//b = XTcpClient.defaultTcpClient().write(writeData, 0, writeData.length);
				//mCharacLatestWrtingData = charac;
				mWriteBeginAt = System.currentTimeMillis();
				b = gattMain.writeCharacteristic(charac);
//				mSendBytesCount += nLeftSize;
				int _count = 0;
				while (!b) {
					XLog.e(TAG, "Write fail!");
					SystemClock.sleep(15);
					b = gattMain.writeCharacteristic(charac);
					_count++;
					//只重发三次
					if(_count>=20)
						break;
				}
				if (b) {
					data.replace(0, nLeftSize, null);
				}
				return b;
			}
			else {
				// Last write request is finished.
//				System.out.println(" Sleep 20"); 
//				SystemClock.sleep(20);
				
				//XLog.v(TAG, "Remove");
				writerQueue.remove();
				writerDataQueue.remove();
				charac = writerQueue.peek();
				data = writerDataQueue.peek();
				if (null != charac && null != data) {

					if (data.length()>0) {
						int nLeftSize = (int)data.length();
//						XLog.i(TAG, "nLeftSize="+nLeftSize);
						if (nLeftSize > BEST_WRITE_SIZE) {
							nLeftSize = BEST_WRITE_SIZE;
						}
						byte[] writeData = data.subData(0, nLeftSize);
						charac.setValue(writeData);
						//b = XTcpClient.defaultTcpClient().write(writeData, 0, writeData.length);
						mWriteBeginAt = System.currentTimeMillis();
						b = gattMain.writeCharacteristic(charac);
//						mSendBytesCount += nLeftSize;
						int _count = 0;
						while (!b) {
							XLog.e(TAG, "Write fail!");
							SystemClock.sleep(15);
							b = gattMain.writeCharacteristic(charac);
							_count++;
							//只重发三次
							if(_count>=20)
								break;
						}
						if (b) {
							data.replace(0, nLeftSize, null);
						}
						return b;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Do the descriptor write.
	 * @return
	 *        true - success. otherwise - fail.
	 */
	private boolean doDescriptorWrite() {
		XLog.v(TAG, "doDescriptorWrite{");
		if (mDescriptionWriterQueue.size()<=0) {
			return false;
		}
		XLog.v(TAG, "doDescriptorWrite}");
		
		final BluetoothGattDescriptor desc = mDescriptionWriterQueue.peek();
		if (null != desc) {
			return gattMain.writeDescriptor(desc);
		}
		
		mDescriptionWriterQueue.remove();
		return false;
	}
	
	/**
	 * Clear all content.
	 */
	public void clearAll() {
		if (null != readerQueue) {
			readerQueue.clear();
		}
		if (null != writerQueue) {
			writerQueue.clear();
		}
		if (null != writerDataQueue) {
			writerDataQueue.clear();
		}
		if (null != mDescriptionWriterQueue) {
			mDescriptionWriterQueue.clear();
		}
		
		bSelfAction = true;
		bOnAction = false;
	}

	/**
	 * On speed write over.
	 */
	public void doSpeedTestOnWriteOver() {

		if (mSpeedAlreadyFixed) {
			return;
		}
		
		mWriteResponseAt = System.currentTimeMillis();
		long curSpeed =  mWriteResponseAt-mWriteBeginAt;
		mSpeed = (curSpeed+mSpeed) / 2;
		mSpeedCheckCount++;
		if (mSpeedCheckCount >= MAX_CHECK_SPEED_COUNT) {
			if (mSpeed < LIMIT_WRITE_SPEED) {
				// High speed.
				BEST_WRITE_SIZE = HIGH_SPEED_WRITE_SIZE;
			}
			else {
				// Low speed.
				BEST_WRITE_SIZE = LOW_SPEED_WRITE_SIZE;
			}
			XLog.e(TAG, "Do the Write Speed check:" + BEST_WRITE_SIZE);
			
			// Check over.
			mSpeedAlreadyFixed = true;
		}
		
		XLog.e(TAG, "Speed=" + curSpeed + "," + mSpeed);
	}
}
