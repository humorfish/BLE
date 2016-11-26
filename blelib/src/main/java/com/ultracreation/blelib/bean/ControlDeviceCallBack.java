package com.ultracreation.blelib.bean;


import com.ultracreation.blelib.utils.BluetensControl;

/**
 * @author liuww
 * @info 针对不同设备的操作回调接口
 */
public interface ControlDeviceCallBack {
	//写数据接口
	public boolean writeData(byte[] data);
	
	//读取数据接口
	public byte[] readData(int len);
	
	//返回升级进度和状态信息
	public void sendUpdateInfo(byte state, int progress, String msgInfo);
	
	//响应当前的工作状态,根据询问的状态结束一
	public void responseAskWorkStateOver(BluetensControl.RestData restData);
}
