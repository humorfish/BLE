package com.ultracreation.blelib.bean;

/**
 * @author liuww
 * @info 蓝牙操作对应的数字命令
 * @data 2015-05-15
 */
public enum EnumCmd {
	COMMAND_HELP,					//帮助信息
	COMMAND_ID,							//显示或设置EPK
	COMMAND_VER,						//显示固件版本号
	COMMAND_HV,							//
	COMMAND_BAT,						//得到电量
	COMMAND_CHNG,					//显示充电状态， yes/no 
	COMMAND_LS,							//显示文件名
	COMMAND_DUMP,					//读取文件
	COMMAND_CAT,						//传送文件
	COMMAND_FMT,						//格式化文件系统
	COMMAND_CS,
	COMMAND_CRC,						//计算文件的crc16
	COMMAND_MD5,						//计算文件md5
	COMMAND_PTAB,						//显示脉冲校准表
	COMMAND_FTAB,						//显示频率校准表
	COMMAND_STAB,						//显示强度校准表
	COMMAND_FREQ,					//显示或设置频率 
	COMMAND_PULS,					//显示或设置脉冲
	COMMAND_STR,						//显示或设置强度
	COMMAND_OSTA,					//开始输出 返回值 ： ok
	COMMAND_OSTO,					//结束输出  返回值  :  ok
	COMMAND_SSTA,						//执行脚本 参数文件名
	COMMAND_OTA,						//OTA升级
	COMMAND_INTV,						//发送的间隔时间
	COMMAND_ISP,						//跳转到升级模式
	COMMAND_ASK_UP,			// 询问是否升级的?号
	COMMAND_RENAME,				//改名
	COMMAND_SETDEF,				//设置默认模式
	COMMAND_TICK,						//得到当前运行时间，如果为0，则无
	COMMAND_RM,							//删除文件
	COMMAND_STATE,					//得到当前运行的时间
	COMMAND_RST_BT,						//重启蓝牙
	COMMAND_OFF						//直接关机
	
//	COMMAND_ISP_CMD_AT_RESET,					//在运行中出现问题，重启蓝牙模块
//	COMMANDISP_CMD_AT_RESET_WITH_SYNC		//复位健后，先要同步才能RESET蓝牙
}
