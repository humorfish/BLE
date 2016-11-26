/**
 * BLTDeviceCommandConstants.java
 * polarstork ( http://www.polarstork.com )
 * Author: khalil KHALIL ( http://portfolio.khalilof.com )
 * Copyright: Copyright 2014 / 2015 http://bluetens.com 
 */

package com.ultracreation.blelib.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;
import com.ultracreation.blelib.bean.GattDataSource;
import com.ultracreation.blelib.bean.SampleGattAttributes;

import java.util.UUID;

public class BLTDeviceCommandConstants {

	/**
     * 命令格式：<br>
     * 0x55,length,commandtype,commanddata,chksum;<br>
     *   0x55        数据包头;<br>
     *   Length      数据长度用1byte表示，sizeof(commandtype+ commanddata);<br>
     *   Commandtype 命令类型，代表不同功能用1byte表示;<br>
     *   Commanddata 命令所带数据，根据需要用Nbyte表示;<br>
     *   Chksum       校验和用1byte表示，~(length + commandtype + commanddata)+1;
     */
    public static final byte kPackHeader = 0x55;

    /**
     * 获取版本号GetVersion(0x01)：<br>
     * 0x55,Length,GetVersion,Chksum
     */
    public static final byte kCommandSendVersionAsk = 0x01;

    /**
     * 返回版本号 ReturnVersion(0x02):<br>
     * 0x55, Length, ReturnVersion ,VersionData,…, Chksum<br>
     * 注：<br>
     * Version, 日期加两位序号如2012110901，所以用5BYTE来表示，字节0存储20字节1 存储12，字节2存储11，字节3存储09，字节4存储01.<br>
     */
    public static final byte kCommandResponseVersion = 0x02;

    /**
     * 功能选择命令 FunctionSelect(0x03):<br>
     * 0x55, Length, FunctionSelect ,FunMode, Chksum<br>
     * 注：FunMode = 0, 跳转到应用程序<br>
     * FunMode = 1, 开始升级
     */
    public static final byte kCommandSendChooseDeviceMode = 0x03;

    /**
     * 返回功能选择命令 ReturnFunctionSelect(0x04):<br>
     * 0x55, Length, ReturnFunctionSelect,State, Chksum<br>
     * 注：State = 0, 成功跳转到应用程序，可进行功能性操作<br>
     * State = 1, 准备好升级<br>
     */
    public static final byte kCommandResponseChooseDeviceSuccess = 0x04;

    /**
     * 开始升级命令（大小，分多少段发送）BeginUpgrade(0x05)<br>
     * 0x55, Length, BeginUpgrade,VERSION, TOTAL LEN, SECTION, Chksum<br>
     * 注：<br>
     * TOTAL LEN 表示固件的大小，分4个字节表示，字节0保存低8位，字节1保存9~16位，字节2保存17~24位，字节3保存25~32位数据。 <br>
     * SECTION表示升级文件需要分多少次传送完，例如一次传送128byte，那么SECTION = TOTAL LEN/128<br>
     * VERSION, 日期加两位序号如2012110901，所以用5BYTE来表示，字节0存储20,字节1存储12，字节2存储11，字节3存储09，字节4存储01.<br>
     */
    public static final byte kCommandSendUpgradeTotalInfo = 0x05;

    /**
     * 升级数据包 UpgradeData(0x06)<br>
     * 0x55, Length, UpgradeData ,datalength,Section, NData, Chksum <br>
     * 注:<br>
     *   datalength表示数据长度（Section+ NData），目前暂定一次传送128字节，所以用1BYTE表示长度。<br>
     *   SECTION为当前发送的是第多少段，从0开始，依次递加，直到发送完毕。<br>
     *   NDATA表示真实的升级数据，本协议里为128BYTE。<br>
     */
    public static final byte kCommandSendUpgradeDataPackage = 0x06;

    /**
     * 升级状态命令，UpgradeState(0x07).在收到开始升级命令或者升级数据包时返回此命令，另外在擦写FLASH过程中出错时，或者在数据没有传送完时，长时间没有继续收到数据，发送此命令。<br>
     * 0x55, Length, UpgradeState , ACK TYPE, ERROE TYPE, Chksum<br>
     * 注:<br>
     *    ACK TYPE    = 0, 表示无错误，继续发送数据。<br>
     *    ACK TYPE    = 1, 出错，错误见 ERROR TYPE。<br>
     *    ERROE TYPE = 0 （长时间没收到数据，上位机收到后重新发送数据）<br>
     *    ERROE TYPE = 1（擦写出错，提示升级失败，需掉电重来）<br>
     *    测试部分代码
	 *    0x06发送数据，0x07返回接收到的数据（跟之前的指令一样，把数据放到两个状态位后面）。
     */
    public static final byte kCommandResponseUpgradeError = 0x07;

    /**
     * 升级成功命令 UpgradeSuccess(0x08).下位机升级成功后，发送此命令，上位机收到后提示升级成功，并发送功能选择命令跳转到应用程序。<br>
     * 0x55, Length, UpgradeSuccess, 0x00, Chksum
     */
    public static final byte kCommandResponseUpgradeSucess = 0x08;

    /**
     * 停止命令 FunctionStop(0x09)，下位机收到停止命令后停止按摩波形的输出。<br>
     * 0x55, Length, FunctionStop, state, Chksum<br>
     * state = 0, 表示下位机停止按摩功能。
     * state = 1, 表示下位机关机。
     */
    public static final byte kCommandOnOff = 0x09;

    /**
     * 强度等级命令FunctionIntension(0x0A)，下位机收到后调节强度。<br>
     * 0x55, Length, FunctionIntension, IntensionLevel, Chksum<br>
     */
	public static final byte kCommandSendIntension = 0x0A;

	/**
	 * 开始发送参数命令BeginSendParaData(0x0B)，下位机收到此命令后停止当前的工作模式，并返回准备好命令，然后清空BUF准备接收详细参数。<br>
	 * 0x55, Length, BeginSendParaData, TOTALLEN , TOTALSECTION ,Chksum<br>
	 * 
	 * ----------------------------------------------------------------------<br>
	 * 0x55, Length, BeginSendParaData, TOTALLEN(1) , TOTALSECTION(1) ,TYPE,Chksum<br>
	 * ----------------------------------------------------------------------<br>
	 * 0x55, Length, BeginSendParaData, TOTALLEN , TOTALSECTION ,TYPE,Chksum<br>
	 * 
	 * 注：<br>
	 *    TOTALLEN为详细参数的大小,用2个字节表示，TOTALSECTION表示参数分多少次传送完，例如一次传送240BYTE, TOTALSECTION = TOTALLEN/240,目前默认为一次传送240个字节。<br>
	 */
	public static final byte kCommandBeginSendParaData = 0x0B;

	/**
	 * 返回开始发送参数命令 ReturnBeginSendParaData(0x0C)。收到上位机的BeginSendParaData命令后，下位机返回准备好或者重发。<br>
	 * 0x55, Length, ReturnBeginSendParaData, STATUS ,Chksum<br>
	 * <b>注：</b><br>
	 * STATUS = 0,表示准备好，可以发送详细参数。<br>
	 * STATUS = 1，表示未准备好，需重发。<br>
	 */
	public static final byte kCommandBeginSendParaDataResponse = 0x0C;

	/**
	 * 发送按摩参数命令SendParaData(0x0D)，下位机收到数据后返回数据是否正确，正确的话继续发下一段，错误则重发当前数据。所有数据接收完后按摩器开始输出波形工作。<br>
	 * 0x55, Length, SendParaData, DataLength, SECTION, NDATA,Chksum<br>
	 * 注：<br>
	 *   DataLength表示数据长度，（SECTION+NDATA），本协议里暂定一次传送240BYTE,LEN用一个字节表示。<br>
	 *   SECTION表示当前发送的是第多少段，从0开始，依次递加，直到发送完毕。<br>
	 *   NDATA表示真实的按摩参数，一次240BYTE<br>
	 */
	public static final byte kCommandSendParaData = 0x0D;

	/**
	 * <b>返回按摩参数命令ReturnSendParaData(0x0E)，下位机在收到按摩详细参数命令后发送此命令，以返回收到命令的状态，上位机根据此状态来确定发送下一段数据或者重发。</b><br>
	 * 0x55, Length, ReturnSendParaData, SECTION ,STATUS ,Chksum<br>
	 * <b>注:</b><br>
	 * SECTION 为按摩详细参数命令里传下来的SECTION。<br>
	 * STATUS = 0，表示参数正确，可继续下一段。<br>
	 * STATUS = 1，表示参数出错，需要重发。<br>
	 */
	public static final byte kCommandSendParaDataResponse = 0x0E;

	/**
	 * <b>按摩参数发送完成 EndSendParaData(0x0F)，CHECKSUM不正确，下位机向上位机发送此命令表示数据被破坏。</b><br>
	 * 0x55, Length, EndSendParaData, 0x00 ,Chksum<br>
	 */
	public static final byte kCommandEndSendParaDataResponse = 0x0F;

	/**
	 * <b>上传强度 ChangeIntensionLevel(0x10)，下位机向上位机发送强度级别。</b><br>
	 * 0x55, Length, ChangeIntensionLevel,IntensionLevel,Chksum<br>
	 * 注：IntensionLevel分60个等级，数据为1-60.<br>
	 */
	public static final byte kCommandDeviceUploadIntensionLevel = 0x10;

	/**
	 * <b>上位机发送点模式 FunctionPoint(0x11)。</b><br>
	 * 0x55, Length, FunctionPoint , State,Chksum<br>
	 * 注：State = 0 面，State = 1 点 .上位机发送命令后收到ACK代表发送成功。
	 */
	public static final byte kCommandSendPointPlane = 0x11;

	/**
	 * <b>命令的ack ，代表收到此命令, ReturnFunctionAck(0x12)</b><br>
	 * 0x55, Length, ReturnFunctionAck, FunctionType,Chksum<br>
	 * 注：FunctionType 为ACK所对应的命令，如回复FunctionPoint ，则：<br>
	 * FunctionType = FunctionPoint<br>
	 */
	public static final byte kCommandAck = 0x12;

	/**
	 * <b>上位机发送反复模式FunctionRepeat (0x13)</b><br>
	 * 0x55, Length, FunctionRepeat, State,Chksum<br>
	 * 注：State = 0 无反复，State = 1 反复 . 上位机发送命令后收到ACK代表发送成功。<br>
	 */
	public static final byte kCommandSendRepeat = 0x13;

	/**
	 * <b>上位机获取当前默认模式GetDefaultMode (0x14)</b><br>
	 * 0x55, Length, GetDefaultMode,Chksum<br>
	 */
	public static final byte kCommandSendGetDefaultMode = 0x14;

	/**
	 * <b>下位机返回当前默认模式 ReturnDefaultMode(0x15)</b><br>
	 * 0x55, Length, ReturnDefaultMode, mode ,Chksum<br>
	 * 注：mode 从1-12代表12种按摩模式<br>
	 */
	public static final byte kCommandSendGetDefaultModeResponse = 0x15;

	/**
	 * <b>上位机设置默认模式SetDefaultMode(0x16)</b><br>
	 * 0x55, Length, SetDefaultMode, mode ,Chksum<br>
	 * 注：mode 从1-12代表12种按摩模式<br>
	 */
	public static final byte kCommandSendSetDefaultMode = 0x16;

	/**
	 * <b>下位机返回设置默认模式状态 ReturnSetDefaultModeState(0x17)</b><br>
	 * 0x55, Length, ReturnSetDefaultModeState, State ,Chksum<br>
	 * 注: State = 0，表示设置成功。<br>
	 * State = 1，表示设置失败，需要重发。<br>
	 */
	public static final byte kCommandSendSetDefaultModeResponse = 0x17;

	/**
	 * 询问是否有蓝牙连接上命令,上位机收到此命令后回复ReturnFunctionAck命令代表握手连接成功，
	 * 可以进行下一步了。IsBlueToothConnected(0x18)
     * 0x55, Length, IsBlueToothConnected,Chksum
	 */
	//public static final byte kCommandSendBluetoothConnectAsk = 0x18;
	
	/**
	 * 询问按摩器设备现在状态情况
	 * 0x55, Length, 0x18 Chksum.
	 */
	public static final byte kCommandSendAskDeviceCurrentStateInfo = 0x18;
	
	/**
	 * 按摩器设备答复APP当前的状态信息。
	 * 0x15, Length, 0x19, runState, modeName, leftTime, intension, repeat, ChkSum.
	 * runState: 1 - running, 0 - not running. (1bytes)
	 * modeName: Command 0x0B set.(8bytes)
	 * leftTime: left time (unit:seconds)。 (2bytes)
	 * intension: current intension. (1byte)
	 * repeat: 1 - repeat one cycle, 0 - not repeat. (1byte)
	 */
	public static final byte kCommandResponseAppCurrentStateInfo = 0x19;

	/**
	 * 上位机蓝牙连接上命令,下位机收到此命令后reboot,并发送IsBlueToothConnected命令。
	 * BlueToothConnected(0x19)
     * 0x55, Length, BlueToothConnected,Chksum
	 */
	//public static final byte kCommandResponseBluetoothConnectAsk = 0x19;

	/**
	 * 红贴白贴SelectRedWhite(0x1A)
	 * 0x55, Length, SelectRedWhite,State,Chksum
	 * 注: State = 0，表示不单独贴工作。
     *     State = 1，设置红贴。 
     *     State = 2，设置白贴。
	 */
	public static final byte kCommandSendRedWhitePad = 0x1A;

	/**
	 * 下位机跳转成功后发送同步命令SyncFunction(0x1B),上位机收到后回复功能
     * ACK命令(0x12)， 下位机根据是否收到回复来判断是单机运行还是通过蓝牙工作。
     * 0x55, Length, SyncFunction, battery, Chksum.
     * battery (1BYTE) 0 - 100
	 */
	public static final byte kCommandResponseSyncFunction = 0x1B;

	/**
	 * 下位机停止工作后发送给上位机的命令(0x1C)，上位机收到停止命令后，回复功能ACK命令(0x12)，同时
	 * 按摩器状态调整到关闭模式。
	 * 0x55, Length, DeviceFunctionStop,state, ChkSum.
	 * state = 0, 通知上位机，下位机即将停止按摩功能，上位机工作界面停止。
	 * state = 1, 通知上位机，下位机将关机，上位机蓝牙断开。
	 */
	public static final byte kCommandDeviceSendStopWorkingToApp = 0x1C;
	
	/**
	 * Error for the parameter data. 整个参数数据错误。
	 * 0x55, Length, 1E, checksum
	 * 验证数据测试意思
	 * 0x1e后面跟sector（0-127根据升级包的大小，每个sector 128B），返回命令0x1f后跟flash中读出的数据！
	 */
	public static final byte kCommandDeviceParamDataTotalError = 0x1E;
	
	/**
	 * Error for the pack data. 单个数据包校验码错误。
	 * 0x55, Length, 1E, checksum
	 * 验证数据测试意思
	 *   0x1e后面跟sector（0-127根据升级包的大小，每个sector 128B），返回命令0x1f后跟flash中读出的数据！
	 */
	public static final byte kCommandDevicePackError = 0x1F;
	
	/**
	 * 下位机发送给APP用于打印调试。
	 * 0x55, Length, 0x20, Debug info, checksum.
	 */
	public static final byte kCommandDeviceDebugInfo = 0x20;
	
	//新增加测试事件响应
	
	/**
	 * 0x55, Length, 0x30, idx, checksum.
	 * 验证数据测试意思， 发送请求取包
	 * 0x30后面跟sector（0-127根据升级包的大小，每个sector 128B），返回命令0x31后跟flash中读出的数据！
	 */
	public static final byte kCommandReadFlashSection = 0x30;
	
	/**
	 * 0x55, Length, 0x31, data, checksum.
	 * 验证数据测试意思， 返回的包数据
	 * 0x30后面跟sector（0-127根据升级包的大小，每个sector 128B），返回命令0x31后跟flash中读出的数据！
	 */
	public static final byte kCommandResponseReadFlashSection = 0x31;
	
   


	/**
	 *  升级完成后发送验证完成命令，后跟一个参数0表示验证OK，其他参数表示验证fail。
	 * 0x55, Length, 0x32, data, checksum.
	 * 0 验证OK, 1 验证失败
	 */
	public static final byte kCommandValidFlashCompleted = 0x32;
	
	/**
	 *  返回发送升级验证后的回复消息
	 * 0x55, Length, 0x32, data, checksum.
	 * 0 验证OK, 1 验证失败
	 */
	public static final byte kCommandResponseValidFlashCompleted = 0x33;


	/**
	 * 直接写原始数据，不加包之类的, 不等待，直接写了.
	 * @param macAddr 
	 * @param data 原始数据
	 * @return
	 */
	private static long sendingTimeoutRecord = 0;
	public static boolean writeSrcDataDirect(final String macAddr, byte[] data, int waitTime)
	{
		if(data != null && data.length>0) {
			if (System.currentTimeMillis()-sendingTimeoutRecord < waitTime) {
				return false;
			}

			GattDataSource mGattSrc = GattDataSource.defaultSrc();
			XBluetoothGatt gat = mGattSrc.itemFor(macAddr);
			BluetoothGatt gatt = mGattSrc.itemForCore(macAddr);
			try {
				final BluetoothGattCharacteristic findCharacteristic = getCharacteristicByUuid(
						gatt, SampleGattAttributes.SERVICE_BOLUTEK,
						SampleGattAttributes.BODY_TONER_WRITE);
				if (null != findCharacteristic) {
					gat.writeDataDirect(findCharacteristic, data, 0);
				}
			} catch (Exception e) {
				Log.e("BLT", e.toString());
			}
		}
		
		sendingTimeoutRecord = System.currentTimeMillis();
		return true;
	}
	
	//增加一个直接写更新数据的
	public static boolean writeSrcDataDirectUpdateData(final String macAddr, byte[] data,int waitTime)
	{
		if(data != null && data.length>0) {		
			GattDataSource mGattSrc = GattDataSource.defaultSrc();
			XBluetoothGatt gat = mGattSrc.itemFor((String) macAddr);
			BluetoothGatt gatt = mGattSrc.itemForCore((String) macAddr);
			
			try {
				final BluetoothGattCharacteristic findCharacteristic = getCharacteristicByUuid(
						gatt, SampleGattAttributes.SERVICE_BOLUTEK,
						SampleGattAttributes.BODY_TONER_WRITE);
				if (null != findCharacteristic) {
						gat.writeDataDirect(findCharacteristic, data, waitTime);
				}
			} catch (Exception e) {
				Log.e("BLT", e.toString());
			}
			
		}
		return true;
	}
	
	/**
	 * Get the spec characteristic by the given Gatt and UUID of the service and
	 * characterisc.
	 * 
	 * @param gatt
	 *            Bluetooth Gatt.
	 * @param serviceUuid
	 *            the uuid of the service.
	 * @param characteristicUuid
	 *            the uuid of the characteristic.
	 * @return If null, not find. not null find one.
	 */
	private final static BluetoothGattCharacteristic getCharacteristicByUuid(
			BluetoothGatt gatt, UUID serviceUuid, UUID characteristicUuid) {
		BluetoothGattCharacteristic characteristic = null;
		final BluetoothGattService service = gatt.getService(serviceUuid);
		if (null != service) {
			characteristic = service.getCharacteristic(characteristicUuid);
		}
		return characteristic;
	}
	
	public static void pushReceived() {
	}
	
	public static byte btCrc(byte[] src, int pos, int len) {
		int n = pos;
		int nMax = pos+len;
		byte crc = 0;
		for (; n<nMax; ++n) {
			crc += src[n];
		}
		return (byte)((~crc)+1);
	}
	
	private static void formatCommand(byte[] src, int location, int length, byte[] out) {
		out[0] = kPackHeader;
		out[1] = (byte)(length);
		out[2] = (byte)(length >> 8);
		System.arraycopy(src, location, out, 3, length);
		out[length+3] = btCrc(out, 1, length+2);
	}
}
