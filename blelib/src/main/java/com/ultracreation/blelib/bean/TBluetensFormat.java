package com.ultracreation.blelib.bean;

import java.util.Map;


/**
 * @author liuww
 * @info Bluetens 数据格式化 
 */
public abstract class TBluetensFormat {
	
	/**
	 * 根据传递的值，得到对应的格式数据
	 * @param enumCmd
	 * @param cmdValue
	 * @return
	 */
	public abstract byte[] getFormatData(EnumCmd enumCmd, String cmdValue);
	
	
	/**
	 * 根据读取的数据，解析出数据类型
	 * @param data
	 * @return
	 */
	public abstract EnumCmd getReadDataType(byte[] data); 
	
	
	/**
	 * 设置帮助信息
	 * @return
	 */
	public abstract byte[] formatSendBluetensHelpData() ;
	
	
	/**
	 * @return 读取帮助信息
	 */
	public abstract String resolveBluetensHelpData(byte[] data) ;
	
	
	/**
	 * 读取版本信息的数据
	 * @return
	 */
	public abstract byte[] formatSendBluetensVerData() ;
	
	
	/**
	 * @return 解析读取的版本信息
	 */
	public abstract int resolveBluetensVerData(byte[] data) ;
	
	
	/**
	 * 设置 EPK ID 号
	 * @param epkId
	 * @return
	 */
	public abstract byte[] formatSendBluetensEPKData(String epkId) ;
	
	
	/**
	 * @return 读取EPK号
	 */
	public abstract String resolveBluetensEPKData(byte[] data) ;
	
	
	
	/**
	 * 设置设备开关
	 * @param isOn true 打开 false 关闭
	 * @return
	 */
	public abstract byte[] formatSendBluetensSwitchData(boolean isOn);
	
	
	/**
	 * 设置设备开始或结束工作
	 * @param isWork true 开始工作 false 结束工作
	 * @return
	 */
	public abstract byte[] formatSendBluetensWorkData(boolean isWork);
	
	/**
	 * 读取充电状态
	 * @return
	 */
	public abstract byte[] formatSendBluetensChng() ;
	
	
	/**
	 * @return 得到当前的充电状态  true 充中电 false 未充电
	 */
	public abstract boolean resolveBluetensChng(byte[] data);
	
	

	/**
	 *  返回文件状态，通过ls查看
	 * @param fileName
	 * @return
	 */
	public abstract byte[] formatSendBluetensLs(String fileName) ;
	
	
	/**
	 * @return 解析文件查看的数据
	 */
	public abstract String resolveBluetensLs(byte[] data);
	
	
	/**
	 *  读取文件 dump 
	 * @param fileName 读取的文件名字
	 * @param filePos  开始的位置
	 * @param len 读取的长度
	 * @return 读取到 end of dump
	 */
	public abstract byte[] formatSendBluetensDump(String fileName,
			int filePos,  int len ) ;
	
	
	/**
	 * @return 解析读取到的文件信息
	 */
	public abstract byte[] resolveSendBluetensDump(byte[] data);
	
	
	/**
	 *  cat 传递文件到设备上
	 * @param fileName 传递的文件名字
	 * @param filePos  开始的位置
	 * @param len 传递的长度
	 * @return 读取到 end of dump
	 */
	public abstract byte[] formatSendBluetensCat(String fileName,
			int filePos,  int len ) ;
	
	/**
	 * @return 解析读取到的文件信息 结束 3: end of cat
	 */
	public abstract boolean resolveSendBluetensCat(byte[] data);
	
	
	/**
	 *  得到指定文件的crc校验
	 * @param fileName
	 * @param filePos
	 * @param len
	 * @return
	 */
	public abstract byte[] formatSendBluetensCRC(String fileName,
			int filePos,  int len ) ;
	
	/**
	 * @return 读取到的CRC检验数据
	 */
	public abstract byte[] resolveSendBluetensCRC(byte[] data);
	
	
	/**
	 *  得到指定文件的MD5校验
	 * @param fileName
	 * @param filePos
	 * @param len
	 * @return
	 */
	public abstract byte[] formatSendBluetensMD5(String fileName,
			int filePos,  int len ) ;
	
	/**
	 * @return 读取到的MD5检验数据
	 */
	public abstract String resolveSendBluetensMD5(byte[] data);
	
	
	/**
	 * @return 显示脉冲校准表
	 */
	public abstract String resolveBluetensPTab(byte[] data);
	
	
	/**
	 * @return 显示频率校准表
	 */
	public abstract String resolveBluetensFTab(byte[] data);
	
	
	/**
	 * @return 显示强度校准表
	 */
	public abstract int resolveBluetensSTab(byte[] data);
	
	
	/**
	 * @return 显示频率 
	 */
	public abstract int resolveBluetensFREQ(byte[] data);
	
	
	/**
	 * @return 设置频率 
	 */
	public abstract byte[] formatSendBluetensFREQ(int value);
	
	
	/**
	 * @return 显示脉冲
	 */
	public abstract int resolveBluetensPULS(byte[] data);
	
	
	/**
	 * @return 设置脉冲
	 */
	public abstract byte[] formatSendBluetensPULS(int value);
	
	
	
	/**
	 * @return 显示强度
	 */
	public abstract int resolveBluetensSTR(byte[] data);
	
	
	/**
	 * @return 设置强度
	 */
	public abstract byte[] formatSendBluetensSTR(int value);
	
	
	/**
	 * @return 显示间隔时间
	 */
	public abstract int resolveBluetensIntv(byte[] data);
	
	
	/**
	 * @return 设置间隔时间
	 */
	public abstract byte[] formatSendBluetensIntv(int value);
	
	
	/**
	 * @return 设置文件脚本
	 */
	public abstract byte[] formatSendBluetensSsta(String filename);
	
	
	/**
	 * @return 解析回复数据的正确与否
	 */
	public abstract boolean resolveBluetensSsta(byte[] data);
	
	/**
	 * @return 跳转到升级模式命令
	 */
	public abstract byte[] formatSendBluetensIsp();
	
	
	/**
	 * @return 解析回复时间
	 */
	public abstract int resolveBluetensTick(byte[] data);
	
	/**
	 * @return 解析回复的电量信息
	 */
	public abstract int resolveBluetensBat(byte[] data);
	
	
	/**
	 * @param data 
	 * @return 解析获取的设备的当前状态
	 */
	public abstract Map<String, String> resolveBluetensState(byte[] data);
}
