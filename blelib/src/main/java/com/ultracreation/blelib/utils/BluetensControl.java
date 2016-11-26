package com.ultracreation.blelib.utils;

import android.os.SystemClock;
import android.util.Log;

import com.ultracreation.blelib.bean.BluetensShell;
import com.ultracreation.blelib.bean.BufferCache;
import com.ultracreation.blelib.bean.ControlDeviceCallBack;
import com.ultracreation.blelib.bean.EnumCmd;
import com.ultracreation.blelib.bean.TBluetensFormat;
import com.ultracreation.blelib.bean.WriteFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Created by you on 2016/9/29.
 */
public class BluetensControl {
    public final static byte MAX_PACK_LEN = 20;
    public final static short PACK_WAIT_TIME = 30;
    public final static byte ISP_STATE_NOT_SEND_FILE_BEGIN = 9; //改为传递文件的进度 开始
    public final static byte ISP_STATE_NOT_SEND_FILE_ING = 10; //改为传递文件的进度 进度中
    public final static byte ISP_STATE_NOT_SEND_FILE_END = 11; //改为传递文件的进度 结束
    //新生产的bluetens 用的是不同的芯片 升级文件不同
    public final static int mLocalFileVer = 10000; //自定义的 只选择bluetensX 的固件升级的版本
    public final static int mLocalFileVer_one = 10011;//旧版bluetens
    public final static int mLocalFileVer_divide = 20000;//新版的BluetensQ 与 旧版BluetensX的分隔线
    public final static int mLocalFileVer_two = 20027;//新版的BluetensQ
    // 连上Bluetens得到状态
    public final static byte BLE_WORK_STATE_NORMAL = 0; // 正常状态，等待接收命令
    public final static byte BLE_WORK_STATE_RUNING = 1; // 已经处理按摩程序工作状态
    public final static byte BLE_WORK_STATE_UPDATE_ISP = 2; // 已经处于等待升级状态 只选择bluetensX 的固件升级

    public final static byte BLE_WORK_STATE_UPDATE_CAN = 3; // 处于可以升级的状态
    public final static byte BLE_WORK_STATE_POWER_ON = 4; // 设备刚开机状态
    public final static byte BLE_WORK_STATE_AT_RESET = 5; // 重启了蓝牙设备
    public final static byte BLE_WORK_STATE_IS_CHNG = 6;    //处理充电状态,
    public final static byte BLE_ASK_STATE_NORMAL = 0; // 正常命令
    public final static byte BLE_ASK_GET_STATE = 1; // 开始询问设备状态
    public final static byte BLE_ASK_GET_STATE_OVER = 2; // 结束询问设备状态
    public final static byte BLE_ASK_SEND_FILE_SSTA = 3; // 设置执行文件
    public final static byte BLE_ASK_SEND_FILE_SSTA_OVER = 4; // 上传文件并执行
    public final static byte BLE_ASK_SEND_FILE_SDEF = 5; // 设置默认模式开始
    public final static byte BLE_ASK_SEND_FILE_SDEF_OVER = 6; // 设置默认模式结束
    public final static byte BLE_ASK_CHECK_DEF_MD5_YES = 7; // 检测默认md5 OK
    public final static byte BLE_ASK_FINAL = 99; // 执行出错
    public final static byte BLE_NOTIFY_NONE = 0;
    public final static byte BLE_NOTIFY_STRENGTH = 1;
    public final static byte BLE_NOTIFY_SHUTDOWN = 2;
    public final static byte BLE_NOTIFY_NOLOAD = 3;
    public final static byte BLE_NOTIFY_LOW_BATTERY = 4;//低电
    public final static byte BLE_NOTIFY_BAD_EQUIPMENT = 5;// 设备损坏
    public final static byte BLE_NOTIFY_STOP = 5;// 设备停止
    public final static String BLE_STATE_TICK = "tick"; // 设备状态的关键字 时间
    public final static String BLE_STATE_MD5_LAST = "lmd5";    //最后一次 运行文件的md5
    public final static String BLE_STATE_MD5_DEF = "dmd5";    //默认运行文件的md5
    public final static String BLE_STATE_STR = "str"; // 运行的强度
    private final static String TAG = "BluetensControl";
    private final static byte SEND_CMD_NONE = 0;
    private final static byte SEND_CMD_SEND_OVER = 1;
    private final static String BEGIN_STR_AT = "AT+";// NAMEB";
    private final static int BLE_SEND_LEN = 20;
    /**
     * The singlton object.
     */
    private static BluetensControl mObj = null;
    // 判断是否是通知消息
    private final byte[] NOTIFY_SHUTDOWN_BYTE = "NOTIFY shutdown".getBytes();
    private final byte[] NOTIFY_STRENGTH_BYTE = "NOTIFY strength".getBytes();
    private final byte[] NOTIFY_NOLOAD_BYTE = "NOTIFY noload".getBytes();
    private final byte[] NOTIFY_LOW_BATTERY = "NOTIFY low battery".getBytes(); // 低电
    private final byte[] NOTIFY_ERROR_STOP = "NOTIFY error stop".getBytes(); // 设备损坏
    private final byte[] NOTIFY_AUTO_STOP = "NOTIFY stop".getBytes(); // 设备自行停止
    public TBluetensFormat mBluetensFormat;
    public boolean isExitControl = false; // 是否要退出当前的操作
    private boolean mIsSetMode = false;
    private boolean fileSysFull = false;
    private ControlDeviceCallBack mControlDeviceCallBack;
    private long lastSendCmdTime;
    private RestData mRestData = new RestData(); // 自己读取的数据状态信息
    private BufferCache mByteBuffer = new BufferCache(1024);

    // 1: end of ls
    // 2: end of dump
    // 3: end of cat
    private WriteFile mWriteFile = null;

    /**
     * The Constructor.
     */

    protected BluetensControl() {
        mBluetensFormat = new BluetensShell();
    }

    /**
     * Get the object of the FWDataSource.
     *
     * @return the singleton object.
     */
    public static BluetensControl getInstance() {
        if (null == mObj) {
            mObj = new BluetensControl();
        }
        return mObj;
    }

    public void setModel(boolean setModel) {
        mIsSetMode = setModel;
    }

    /**
     * @param callBack 设置操控的回调
     */
    public void setControlDeviceCallBack(ControlDeviceCallBack callBack) {
        mControlDeviceCallBack = callBack;
    }

    // 简单打印
    private void logPrintf(String msg) {
        System.out.println(TAG + ":" + msg);
    }

    private void checkSendCmdCount() {

        if (mRestData.sendCmdState != SEND_CMD_NONE) {
            if (System.currentTimeMillis() - lastSendCmdTime >= 1000 * 3) {
                mRestData.sendCmdState = SEND_CMD_NONE;
            }
        }
    }

    // 新的发送数据
    public boolean sendSerialCmd(EnumCmd cmdEnum, String value) {
        KLog.i(TAG, "sendSerialCmd 1：" + "cmdEnum: " + cmdEnum + " value: "
                + value + " sendCmdState:" + mRestData.sendCmdState);
        checkSendCmdCount();
        if (mControlDeviceCallBack == null
                || mRestData.sendCmdState != SEND_CMD_NONE)
            return false;

        byte[] sendData = null;
        switch (cmdEnum) {
            case COMMAND_VER: // 显示固件版本号
                sendData = mBluetensFormat.formatSendBluetensVerData();
                break;
            case COMMAND_BAT: // 得到电量
                sendData = mBluetensFormat.getFormatData(cmdEnum, null);
                break;
            case COMMAND_CHNG: // 显示充电状态， yes/no
                sendData = mBluetensFormat.formatSendBluetensChng();
                break;
            case COMMAND_LS:
                sendData = mBluetensFormat.formatSendBluetensLs(value);
                mRestData.sendCmdState = SEND_CMD_SEND_OVER;
                break;
            case COMMAND_RM:
                sendData = mBluetensFormat.getFormatData(cmdEnum, value);
                mRestData.sendCmdState = SEND_CMD_SEND_OVER;
                break;
            case COMMAND_CRC:
                sendData = mBluetensFormat.formatSendBluetensCRC(value, -1, -1);
                break;
            case COMMAND_MD5:
                sendData = mBluetensFormat.formatSendBluetensMD5(value, -1, -1);
                break;
            case COMMAND_STR: // 显示或设置强度
                sendData = mBluetensFormat.formatSendBluetensSTR(Integer
                        .valueOf(value));
                mRestData.sendCmdState = SEND_CMD_SEND_OVER;
                break;
            case COMMAND_FMT:
                sendData = mBluetensFormat.getFormatData(cmdEnum, null);
                break;
            case COMMAND_OSTA: // 开始输出 返回值 ： ok
                sendData = mBluetensFormat.formatSendBluetensWorkData(true);
                break;
            case COMMAND_OSTO: // 结束输出 返回值 : ok
                sendData = mBluetensFormat.formatSendBluetensWorkData(false);
                break;
            case COMMAND_SSTA: // 开始执行指定的文件
                sendData = mBluetensFormat.formatSendBluetensSsta(value);
                break;
            case COMMAND_OFF: // 直接关机
                sendData = mBluetensFormat.formatSendBluetensSwitchData(false);
                break;
            case COMMAND_SETDEF: // 设置默认升级文件
                sendData = mBluetensFormat.getFormatData(cmdEnum, value);
                mRestData.sendCmdState = SEND_CMD_SEND_OVER;
                break;
            case COMMAND_TICK: // 得到当前运行程序的时间
                sendData = mBluetensFormat.getFormatData(cmdEnum, null);
                break;
            case COMMAND_STATE: // 得到当前的状态
                sendData = mBluetensFormat.getFormatData(cmdEnum, null);
                break;
        }
        if (sendData != null) {
            logPrintf("sendSerialCmd:" + new String(sendData));
            // printForTest(sendData);
            if (mControlDeviceCallBack != null && mControlDeviceCallBack.writeData(sendData)) {
                mRestData.lastSendCmd = cmdEnum;
                lastSendCmdTime = System.currentTimeMillis();
                // readCount = 0;
                return true;
            }
        }
        return false;
    }

    public boolean sendSerialCmd(EnumCmd cmdEnum, String fileName,
                                 String parameter1, String parameter2) {

        logPrintf("sendSerialCmd 2：" + "cmdEnum: " + cmdEnum + " fileName: "
                + fileName + " parameter1:" + parameter1 + " parameter2:"
                + parameter2 + " sendCmdState:" + mRestData.sendCmdState);
        checkSendCmdCount();
        if (mControlDeviceCallBack == null
                || mRestData.sendCmdState != SEND_CMD_NONE)
            return false;

        mRestData.lastSendCmd = cmdEnum;
        byte[] sendData = null;
        switch (cmdEnum) {
            case COMMAND_CAT: {
                sendData = mBluetensFormat.formatSendBluetensCat(fileName,
                        Integer.valueOf(parameter1), Integer.valueOf(parameter2));
                mRestData.sendCmdState = SEND_CMD_SEND_OVER;
                break;
            }
            case COMMAND_CRC:
                sendData = mBluetensFormat.formatSendBluetensCRC(fileName,
                        Integer.valueOf(parameter1), Integer.valueOf(parameter2));
                break;
            case COMMAND_MD5:
                sendData = mBluetensFormat.formatSendBluetensMD5(fileName,
                        Integer.valueOf(parameter1), Integer.valueOf(parameter2));
                break;
        }
        if (sendData != null) {
            logPrintf("sendSerialCmd:" + new String(sendData));
            // printForTest(sendData);
            if (mControlDeviceCallBack.writeData(sendData)) {
                mRestData.lastSendCmd = cmdEnum;
                lastSendCmdTime = System.currentTimeMillis();
                return true;
            }
        }
        return false;
    }

    // 判断是否一段数据结束
    private boolean isReadBufferEnd(byte[] readBuffer) {
        printForTest(readBuffer);
        logPrintf(" read buffer length:" + readBuffer.length);
        if (readBuffer.length >= 2) {
            logPrintf(" read buffer:" + readBuffer[readBuffer.length - 2] + " "
                    + readBuffer[readBuffer.length - 1]);
            if (readBuffer[readBuffer.length - 2] == 0X0D
                    && readBuffer[readBuffer.length - 1] == 0X0A) {
                return true;
            }
        }
        return false;
    }

    private boolean isReadBufferEnd(byte[] readBuffer, String endStr) {
        byte[] _strByte = endStr.getBytes();
        printForTest(readBuffer);
        if (readBuffer.length >= 2 + _strByte.length) {
            if (readBuffer[readBuffer.length - 2] == 0X0D
                    && readBuffer[readBuffer.length - 1] == 0X0A) {
                int n = 0;
                for (int i = (readBuffer.length - 2 - _strByte.length); i < readBuffer.length - 2; i++) {
                    if (readBuffer[i] != _strByte[n++]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    // 读取数据保存和判断
    private void opterReadData(byte[] readBuffer) {
        // 0x0D, 0x0A
        logPrintf("opterReadData  lastSendCmd:" + mRestData.lastSendCmd
                + " mAskState:" + mRestData.mAskState);
        switch (mRestData.lastSendCmd) {
            case COMMAND_LS:
                if (isReadBufferEnd(readBuffer, "1: end of ls")) {
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;
                }
                break;
            case COMMAND_DUMP:
                if (isReadBufferEnd(readBuffer, "2: end of dump")) {
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;
                    if (mWriteFile != null) {
                        mWriteFile.free();
                        mWriteFile = null;
                    }
                } else {
                    // 保存文件
                    if (mWriteFile != null) {
                        mWriteFile.writeData(readBuffer);
                    }
                }
                break;
            case COMMAND_CAT:
                if (isReadBufferEnd(readBuffer, "3: end of cat")) {
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;
                    if (mRestData.mAskState == BLE_ASK_SEND_FILE_SSTA
                            || mRestData.mAskState == BLE_ASK_SEND_FILE_SDEF) {
                        sendSerialCmd(EnumCmd.COMMAND_MD5, mRestData.mSendName);
                        // if(mRestData.mAskState == BLE_ASK_SEND_FILE_SSTA) {
                        // //叫程序去执行开始
                        // mRestData.mAskState++;
                        // if(mControlDeviceCallBack != null) {
                        // mControlDeviceCallBack.responseAskWorkStateOver(mRestData);
                        // }
                        // } else {
                        // //设置保存
                        // sendSerialCmd(EnumCmd.COMMAND_SETDEF,
                        // mRestData.mSendName);
                        // }
                    }
                }else if(isReadBufferEnd(readBuffer)&&(new String(readBuffer).contains("insufficient space"))){//, "insufficient space"
                    Log.e(TAG, "readend.string="+new String(readBuffer));
                    fileSysFull = true;
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;
                    Log.e(TAG, "tmpfileName="+mRestData.mSendName+"  tmpData.length="+mRestData.mSendData.length);
                    sendSerialCmd(EnumCmd.COMMAND_FMT, null);
                }
                break;
            case COMMAND_FMT:
                if (isReadBufferEnd(readBuffer, "0: ok")){
                    fileSysFull = false;
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;
                    sendFile2Bluetens(mRestData.mSendName, mRestData.mSendData);
                }
                break;
            case COMMAND_MD5: {
                if (isReadBufferEnd(readBuffer)) {
                    // 一段数据结束
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;
                    if (mRestData.mAskState == BLE_ASK_SEND_FILE_SSTA
                            || mRestData.mAskState == BLE_ASK_SEND_FILE_SDEF) {
                        String _md5 = mBluetensFormat
                                .resolveSendBluetensMD5(readBuffer);
                        if (_md5 != null) {
                            String _localMd5 = MD5Util
                                    .getMD5String(mRestData.mSendData);
                            logPrintf(" md5:" + _md5 + " localMd5:" + _localMd5);
                            if (_md5.equalsIgnoreCase(_localMd5)) {
                                // 说明文件上传正确，可以执行
                                if (mRestData.mAskState == BLE_ASK_SEND_FILE_SSTA) {
                                    // 叫程序去执行开始
                                    mRestData.mAskState++;
                                    if (mControlDeviceCallBack != null) {
                                        mControlDeviceCallBack
                                                .responseAskWorkStateOver(mRestData);
                                    }
                                } else {
                                    // 设置保存
                                    sendSerialCmd(EnumCmd.COMMAND_SETDEF,
                                            mRestData.mSendName);
                                }
                            } else {
                                mRestData.mAskState = BLE_ASK_FINAL;
//							mRestData.mWorkState = BLE_ASK_CHECK_DEF_MD5_NO;
                                // 执行callback
                                if (mControlDeviceCallBack != null) {
                                    mControlDeviceCallBack
                                            .responseAskWorkStateOver(mRestData);
                                }
                                // //测试去执行
                                // mRestData.mAskState++;
                                // if(mControlDeviceCallBack != null) {
                                // mControlDeviceCallBack.responseAskWorkStateOver(mRestData);
                                // }
                            }
                        }
                    }
                }
                break;
            }
            case COMMAND_SETDEF: {
                if (isReadBufferEnd(readBuffer)) {
                    // 一段数据结束
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;
                    if (mRestData.mAskState == BLE_ASK_SEND_FILE_SDEF) {
                        mRestData.mAskState++;
                        if (mControlDeviceCallBack != null) {
                            mControlDeviceCallBack
                                    .responseAskWorkStateOver(mRestData);
                        }
                    }
                }
                break;
            }
            case COMMAND_ISP:
                mRestData.sendCmdState = SEND_CMD_NONE;
                mRestData.lastSendCmd = null;
                break;
            case COMMAND_ASK_UP: {
                if (isReadBufferEnd(readBuffer, "Synchronized")) {
                    // 当前为升级状态ISP
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;
                    mRestData.mWorkState = BLE_WORK_STATE_UPDATE_ISP;
                    mRestData.mAskState = BLE_ASK_GET_STATE_OVER;
                    // 开始升级 提示跳到升级界面
                    if (mControlDeviceCallBack != null) {
                        mControlDeviceCallBack.responseAskWorkStateOver(mRestData);
                    }
                }
                break;
            }
            case COMMAND_VER: {
                if (isReadBufferEnd(readBuffer)) {
                    // 一段数据结束
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;

                    if (mRestData.mAskState == BLE_ASK_GET_STATE) {
                        int ver = mBluetensFormat
                                .resolveBluetensVerData(readBuffer);
                        logPrintf("dispatchReceivedData ver:" + ver
                                + " mLocalFileVer:" + mLocalFileVer);
                        if(ver<100) {
                            //说明版本取的有问题，再获取
                            mRestData.sendCmdState = SEND_CMD_NONE;
                            sendSerialCmd(EnumCmd.COMMAND_VER, null);
                        } else if (ver < mLocalFileVer) {
                            // 进入升级状态,需要跳转到升级中去
                            mRestData.sendCmdState = SEND_CMD_NONE;

                            //增加一个请求电量的
                            sendSerialCmd(EnumCmd.COMMAND_BAT, null);
                            mRestData.sendCmdState = SEND_CMD_NONE;

                            mRestData.lastSendCmd = null;
                            mRestData.mWorkState = BLE_WORK_STATE_UPDATE_CAN;
                            mRestData.mAskState = BLE_ASK_GET_STATE_OVER;
                            // 开始升级 提示跳到升级界面
                            if (mControlDeviceCallBack != null) {
                                mControlDeviceCallBack
                                        .responseAskWorkStateOver(mRestData);
                            }
                        } else {
                            if(false) {
                                //改为新的直接得到设备状态
                                sendSerialCmd(EnumCmd.COMMAND_STATE, null);
                            } else  {
                                //还是改为请求是否在充电状态
                                sendSerialCmd(EnumCmd.COMMAND_CHNG, null);
                            }
                        }
                    }
                }
                break;
            }
            case COMMAND_CHNG: {
                if (isReadBufferEnd(readBuffer)) {
                    // 一段数据结束
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;

                    if (mRestData.mAskState == BLE_ASK_GET_STATE) {
                        boolean isChngr = mBluetensFormat.resolveBluetensChng(readBuffer);
                        logPrintf("dispatchReceivedData isChngr:" + isChngr);
                        if (isChngr) {
                            // 提示已经关机了
                            mRestData.sendCmdState = SEND_CMD_NONE;

                            mRestData.lastSendCmd = null;
                            mRestData.mWorkState = BLE_WORK_STATE_IS_CHNG;
                            mRestData.mAskState = BLE_ASK_GET_STATE_OVER;
                            // 开始升级 提示跳到升级界面
                            if (mControlDeviceCallBack != null) {
                                mControlDeviceCallBack
                                        .responseAskWorkStateOver(mRestData);
                            }
                        } else {
                            //改为新的直接得到设备状态
                            sendSerialCmd(EnumCmd.COMMAND_STATE, null);
                        }
                    }
                }
                break;
            }
            case COMMAND_TICK: {
                if (isReadBufferEnd(readBuffer)) {
                    // 一段数据结束
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;
                    if (mRestData.mAskState == BLE_ASK_GET_STATE) {
                        int _tick = mBluetensFormat.resolveBluetensTick(readBuffer);
                        if (_tick > 0) {
                            mRestData.mTickTime = _tick;
                            mRestData.mWorkState = BLE_WORK_STATE_RUNING;
                            sendSerialCmd(EnumCmd.COMMAND_STAB, null);
                        } else {
                            // 直接启动正常工作模式
                            mRestData.mTickTime = 0;
                            mRestData.mWorkState = BLE_WORK_STATE_NORMAL;
                            mRestData.mAskState = BLE_ASK_GET_STATE_OVER;
                            // 开始正常工作模式
                            if (mControlDeviceCallBack != null) {
                                mControlDeviceCallBack
                                        .responseAskWorkStateOver(mRestData);
                            }
                        }
                    }
                }
                break;
            }
            case COMMAND_STAB: {
                if (isReadBufferEnd(readBuffer)) {
                    // 得到当前强度后
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;
                    if (mRestData.mAskState == BLE_ASK_GET_STATE) {
                        int str = mBluetensFormat.resolveBluetensSTab(readBuffer);
                        logPrintf(" COMMAND_STAB str:" + str + " newstr:"
                                + new String(readBuffer));
                        mRestData.mStr = str;
                        // 开始继续工作模式
                        mRestData.mAskState = BLE_ASK_GET_STATE_OVER;
                        if (mControlDeviceCallBack != null) {
                            mControlDeviceCallBack
                                    .responseAskWorkStateOver(mRestData);
                        }
                    }
                }
                break;
            }
            case COMMAND_STATE: {
                if (isReadBufferEnd(readBuffer) ) {
                    // 一段数据结束
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;

                    if (mRestData.mAskState == BLE_ASK_GET_STATE) {
                        Map<String, String> _dataMap = mBluetensFormat
                                .resolveBluetensState(readBuffer);

                        if (_dataMap != null && _dataMap.size() > 0) {
                            try {
                                int _tick = Integer.valueOf(_dataMap
                                        .get(BLE_STATE_TICK));
                                String _md5 = _dataMap.get(BLE_STATE_MD5_LAST);
                                String _localMd5 = MD5Util
                                        .getMD5String(mRestData.mSendData);

                                logPrintf(" BLE_ASK_CHECK_MD5 md5:" + _md5
                                        + " localMd5:" + _localMd5 + " tick:" + _tick);

                                if (_md5.equalsIgnoreCase(_localMd5)) {
                                    try {
                                        mRestData.mStr = Integer.valueOf(_dataMap.get(BLE_STATE_STR).replace("\r\n", ""));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        mRestData.mStr = 0;//3;
                                    }

                                    //设备上的文件与本地要传的相同
                                    if (_tick > 0 && mRestData.mStr>0) {
                                        Log.e(TAG, "mIsSetMode="+mIsSetMode);
                                        if (!mIsSetMode) {
                                            // 表示有文件在运行
                                            mRestData.mTickTime = _tick;
                                            mRestData.mWorkState = BLE_WORK_STATE_RUNING;

                                            logPrintf(" COMMAND_STAB str:"
                                                    + mRestData.mStr
                                                    + " mTickTime:" + _tick);
                                            // 开始继续工作模式
                                            mRestData.mAskState = BLE_ASK_GET_STATE_OVER;
                                            if (mControlDeviceCallBack != null) {
                                                mControlDeviceCallBack
                                                        .responseAskWorkStateOver(mRestData);
                                            }
                                        }else {
                                            mIsSetMode = false;
                                            // 无运行
                                            mRestData.mTickTime = 0;
                                            mRestData.mAskState = BLE_ASK_CHECK_DEF_MD5_YES;
                                            mRestData.mWorkState = BLE_WORK_STATE_NORMAL;
                                            if (mControlDeviceCallBack != null) {
                                                mControlDeviceCallBack
                                                        .responseAskWorkStateOver(mRestData);
                                            }
										/*//停止访问状态
										mRestData.mAskState = BLE_ASK_FINAL;
//										mRestData.mWorkState = BLE_ASK_CHECK_DEF_MD5_NO;
										// 执行callback
										if (mControlDeviceCallBack != null) {
											mControlDeviceCallBack
													.responseAskWorkStateOver(mRestData);
										}

										//直接显示默认模式设置完成界面
										mRestData.sendCmdState = SEND_CMD_NONE;
										mRestData.lastSendCmd = null;
										if (mRestData.mAskState == BLE_ASK_SEND_FILE_SDEF) {
											mRestData.mAskState++;
											if (mControlDeviceCallBack != null) {
												mControlDeviceCallBack
														.responseAskWorkStateOver(mRestData);
											}
										}*/
                                        }
                                    } else {
                                        // 无运行
                                        mRestData.mTickTime = 0;
                                        mRestData.mAskState = BLE_ASK_CHECK_DEF_MD5_YES;
                                        mRestData.mWorkState = BLE_WORK_STATE_NORMAL;
                                        if (mControlDeviceCallBack != null) {
                                            mControlDeviceCallBack
                                                    .responseAskWorkStateOver(mRestData);
                                        }
                                    }

                                } else {
                                    // 直接启动正常工作模式
                                    mRestData.mTickTime = 0;
                                    mRestData.mWorkState = BLE_WORK_STATE_NORMAL;
                                    mRestData.mAskState = BLE_ASK_GET_STATE_OVER;
                                    // 开始正常工作模式
                                    if (mControlDeviceCallBack != null) {
                                        mControlDeviceCallBack
                                                .responseAskWorkStateOver(mRestData);
                                    }
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                break;
            }
            case COMMAND_RENAME: {//修改名字

                if (isReadBufferEnd(readBuffer)) {
                    logPrintf(" COMMAND_RENAME sendCmdState:" + mRestData.sendCmdState);
                    // 一段数据结束
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.mAskState = BLE_ASK_STATE_NORMAL;
                    mRestData.lastSendCmd = null;
//				sendSerialCmd(EnumCmd.COMMAND_OFF, mRestData.mSendName);
                }
                break;
            }
            case COMMAND_RST_BT: //复位蓝牙
                logPrintf(" COMMAND_RST_BT sendCmdState:" + mRestData.sendCmdState + " readBuffer:" + new String(readBuffer));
                mRestData.sendCmdState = SEND_CMD_NONE;
                mRestData.mAskState = BLE_ASK_STATE_NORMAL;
                mRestData.lastSendCmd = null;
                break;
            default:
                if (isReadBufferEnd(readBuffer)) {
                    // 一段数据结束
                    mRestData.sendCmdState = SEND_CMD_NONE;
                    mRestData.lastSendCmd = null;
                }
                break;
        }
    }

    private boolean isBeginStrAt(byte[] readBuffer) {
        if (new String(readBuffer).contains(BEGIN_STR_AT))
            return true;
        return false;
    }

    private boolean isNotifyData(byte[] readBuffer) {
        logPrintf("isNotifyData readbuffer:" + new String(readBuffer) + " len:"
                + readBuffer.length + " bytelen:" + NOTIFY_STRENGTH_BYTE.length);
        if (readBuffer.length < NOTIFY_NOLOAD_BYTE.length)
            return false;

        int i;
        for (i = 0; i < NOTIFY_STRENGTH_BYTE.length; i++) {
            if (readBuffer[i] != NOTIFY_STRENGTH_BYTE[i])
                break;
        }
        logPrintf("isNotifyData readbuffer i:" + i);
        if (i >= NOTIFY_STRENGTH_BYTE.length) {
            mRestData.mNotifyMsg = BLE_NOTIFY_STRENGTH;
            int _len = readBuffer.length - NOTIFY_STRENGTH_BYTE.length - 3;
            if (_len > 0) {
                int _mineLen = Math.min(_len, 2);
                byte[] _data = new byte[_mineLen];
                System.arraycopy(readBuffer, NOTIFY_STRENGTH_BYTE.length + 1,
                        _data, 0, _mineLen);

                logPrintf("isNotifyData NOTIFY_STRENGTH_BYTE :"
                        + new String(_data));
                if (_data[0] >= '0' && _data[0] <= '9'
                        && _data[_data.length - 1] >= '0'
                        && _data[_data.length - 1] <= '9') {
                    // try{
                    mRestData.mNotifyValue = Integer.valueOf(new String(_data));
                    // }catch (Exception e) {
                    // e.printStackTrace();
                    // mRestData.mNotifyValue = 0;
                    // }
                } else
                    mRestData.mNotifyValue = 0;
                return true;
            }
            return false;
        }

        for (i = 0; i < NOTIFY_SHUTDOWN_BYTE.length; i++) {
            if (readBuffer[i] != NOTIFY_SHUTDOWN_BYTE[i])
                break;
        }
        logPrintf("isNotifyData readbuffer NOTIFY_SHUTDOWN_BYTE i:" + i);
        if (i >= NOTIFY_SHUTDOWN_BYTE.length) {
            mRestData.mNotifyMsg = BLE_NOTIFY_SHUTDOWN;
            return true;
        }

        for (i = 0; i < NOTIFY_NOLOAD_BYTE.length; i++) {
            if (readBuffer[i] != NOTIFY_NOLOAD_BYTE[i])
                break;
        }
        logPrintf("isNotifyData readbuffer NOTIFY_NOLOAD_BYTE i:" + i);
        if (i >= NOTIFY_NOLOAD_BYTE.length - 1) {
            mRestData.mNotifyMsg = BLE_NOTIFY_NOLOAD;
            return true;
        }


        //add by you 低电
        for (i = 0; i < NOTIFY_LOW_BATTERY.length; i++) {
            if (readBuffer[i] != NOTIFY_LOW_BATTERY[i])
                break;
        }
        logPrintf("isNotifyData readbuffer NOTIFY_LOW_BATTERY i:" + i);
        if (i >= NOTIFY_LOW_BATTERY.length - 1) {
            mRestData.mNotifyMsg = BLE_NOTIFY_LOW_BATTERY;
            return true;
        }

        //add by you 设备损坏的消息
        for (i = 0; i < NOTIFY_ERROR_STOP.length; i++) {
            if (readBuffer[i] != NOTIFY_ERROR_STOP[i])
                break;
        }
        logPrintf("isNotifyData readbuffer NOTIFY_ERROR_STOP i:" + i);
        if (i >= NOTIFY_ERROR_STOP.length - 1) {
            mRestData.mNotifyMsg = BLE_NOTIFY_BAD_EQUIPMENT;
            return true;
        }

        // add by you 设备自行停止
        for (i = 0; i < NOTIFY_AUTO_STOP.length; i++) {
            if (readBuffer[i] != NOTIFY_AUTO_STOP[i])
                break;
        }
        logPrintf("isNotifyData readbuffer NOTIFY_AUTO_STOP i:" + i);
        if (i >= NOTIFY_AUTO_STOP.length - 1) {
            mRestData.mNotifyMsg = BLE_NOTIFY_STOP;
            return true;
        }

        return false;
    }


    /**
     * @param readBuffer
     * @return 从串口读取的数据，直接通过这个进行处理
     */
    public synchronized RestData opterReplyData(byte[] readBuffer) {
        mRestData.mSendCmd = mRestData.lastSendCmd;
        //判断是否是改名回复
        if (mRestData.lastSendCmd == EnumCmd.COMMAND_RENAME && (
                new String(readBuffer).startsWith("AT+NAME") || new String(readBuffer).startsWith("btnm="))) {
            //不用退出
            mRestData.mSendName = new String(readBuffer).replace("AT+NAME", "").replace("btnm=", "").replace("\r\n", "");
            KLog.i(TAG, "opterReplyData COMMAND_RENAME :" + mRestData.mSendName);
        } else if (isBeginStrAt(readBuffer)) {
            //当不是请求状态的时候，才去清楚
            if (mRestData.mAskState != BLE_ASK_GET_STATE) {
                clearCmdState();

                mRestData.mWorkState = BLE_WORK_STATE_POWER_ON;
                if (mControlDeviceCallBack != null) {
                    mControlDeviceCallBack.responseAskWorkStateOver(mRestData);
                }
            }
            return null;
        }

        if (isNotifyData(readBuffer)) {
            mRestData.sendCmdState = SEND_CMD_NONE;
            return mRestData;
        } else if (isReadDataByTick(readBuffer)) {
            //增加一个，如果收到了tick直接在这里执行
            mRestData.mAskState = BLE_ASK_GET_STATE;
            mRestData.lastSendCmd = EnumCmd.COMMAND_STATE;

            opterReadData(readBuffer);
            return null;
        } else if (mRestData.lastSendCmd != null) {
            if (readBuffer != null) {
                mRestData.mSendCmd = mRestData.lastSendCmd;
                mRestData.mData = readBuffer;
                opterReadData(readBuffer);

                if (mRestData.mAskState == BLE_ASK_GET_STATE
                        || mRestData.mAskState == BLE_ASK_SEND_FILE_SSTA
                        || mRestData.mAskState == BLE_ASK_SEND_FILE_SDEF)
                    return null;
                else if (mRestData.mAskState == BLE_ASK_GET_STATE_OVER
                        || mRestData.mAskState == BLE_ASK_SEND_FILE_SSTA_OVER
                        || mRestData.mAskState == BLE_ASK_SEND_FILE_SDEF_OVER
                        || mRestData.mAskState == BLE_ASK_FINAL) {
                    mRestData.mAskState = BLE_ASK_STATE_NORMAL;
                    return null;
                }
            }
        } else {

            KLog.i(TAG, "----3---->");
            // 加入到文本框中
            mRestData.mData = readBuffer;
            // 一段数据结束
            mRestData.sendCmdState = SEND_CMD_NONE;
        }
        return mRestData;
    }

    //判断是否收到SSTA的请求
    private boolean isReadDataByTick(byte[] readBuff) {
        if (readBuff != null && mRestData.lastSendCmd != EnumCmd.COMMAND_STATE) {
            String _readStr = new String(readBuff);
            if (_readStr.startsWith("tick=") && _readStr.contains("dmd5=")) {
                Log.d(TAG, "isReadDataByTick ...");
                return true;
            }
        }
        return false;
    }

    // 写文件信息
    public boolean sendFile2Serial(String fileName) {
        if (mControlDeviceCallBack == null)
            return false;

        File file = new File(fileName);
        InputStream in = null;
        try {
            logPrintf("sendFile2Serial begin...：");
            int idx = fileName.lastIndexOf('\\');

            String _fileName;
            if (idx >= 0)
                _fileName = fileName.substring(idx + 1);
            else {
                idx = fileName.lastIndexOf('/');
                if (idx >= 0)
                    _fileName = fileName.substring(idx + 1);
                else
                    _fileName = fileName;
            }
            if (sendSerialCmd(EnumCmd.COMMAND_CAT, _fileName,
                    String.valueOf(-1), String.valueOf(file.length()))) {

                // 一次读一个字节
                // int count = 0;
                logPrintf("sendFile2Serial send begin...：");
                in = new FileInputStream(file);
                // int tempbyte;

                int length = in.available();
                logPrintf(" load length: " + length);
                byte[] _buffer = new byte[length];
                in.read(_buffer);

                mControlDeviceCallBack.writeData(_buffer);
                in.close();
                logPrintf("sendFile2Serial send end...count：" + length);
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();

        }
        return false;
    }

    // 往bluetens写文件数据
    public boolean sendFile2Bluetens(String fileName, byte[] data) {
        if (mControlDeviceCallBack == null)
            return false;

        logPrintf("sendFile2Bluetens begin...：");
        int _sendBlockLen = (data.length / BLE_SEND_LEN) + 1;

        if (mControlDeviceCallBack != null && _sendBlockLen >= 5)
            mControlDeviceCallBack.sendUpdateInfo(ISP_STATE_NOT_SEND_FILE_BEGIN,
                    _sendBlockLen, null);

        if (sendSerialCmd(EnumCmd.COMMAND_CAT, fileName, String.valueOf(-1),
                String.valueOf(data.length))) {
            int n = 0;
            int _leftLen = data.length;
            int _len = 0;
            byte[] _buffer = new byte[BLE_SEND_LEN]; // 蓝牙发送20个字节
            while (_leftLen > 0 && !fileSysFull) {

                if (isExitControl) {
                    isExitControl = false;
                    if (mControlDeviceCallBack != null && _sendBlockLen >= 5)
                        mControlDeviceCallBack.sendUpdateInfo(ISP_STATE_NOT_SEND_FILE_END,
                                0, null);
                    return false;
                }

                _len = Math.min(_leftLen, BLE_SEND_LEN);
                if (_len < BLE_SEND_LEN) {
                    _buffer = new byte[_len];
                }

                System.arraycopy(data, (data.length - _leftLen), _buffer, 0,
                        _len);
                if (fileSysFull) {
                    Log.e(TAG, "---1-->space full!!!");
                    return false;
                }
                if (mControlDeviceCallBack != null) {
                    mControlDeviceCallBack.writeData(_buffer);
                    SystemClock.sleep(25);
                }

                if (mControlDeviceCallBack != null && _sendBlockLen >= 5)
                    mControlDeviceCallBack.sendUpdateInfo(ISP_STATE_NOT_SEND_FILE_ING,
                            ++n, null);
                _leftLen -= _len;
            }
            if (fileSysFull) {
                Log.e(TAG, "---2-->space full!!!");
                return false;
            }
            if (mControlDeviceCallBack != null && _sendBlockLen >= 5)
                mControlDeviceCallBack.sendUpdateInfo(ISP_STATE_NOT_SEND_FILE_END,
                        100, null);
            logPrintf("sendFile2Bluetens send end...：");
            return true;
        }
        return false;
    }

    // 发送接收文件信息
    public boolean getFileFromSerial(String filePath, String fileName) {
        if (mControlDeviceCallBack == null || filePath.isEmpty()
                || fileName.isEmpty())
            return false;

        if (sendSerialCmd(EnumCmd.COMMAND_DUMP, fileName, String.valueOf(-1),
                String.valueOf(-1))) {
            mWriteFile = new WriteFile(filePath, fileName);
            return true;
        }
        return false;
    }

    /**
     * 请求设备的当前状态
     *
     * @return
     */
    public synchronized boolean askBluetensState() {
        String _cmd = "?";
        mRestData.mAskState = BLE_ASK_GET_STATE;

        if (mControlDeviceCallBack != null && mControlDeviceCallBack.writeData(_cmd.getBytes())) {
            mRestData.lastSendCmd = EnumCmd.COMMAND_ASK_UP;

            new Thread() {
                public void run() {
                    synchronized (this) {
                        try {
                            Thread.sleep(1000);
                            if (BodyTonerCmdFormater.getPushReceivedZeroCount() >= 2) {
                                sendCmd2ATResetWithSync(false);
                                SystemClock.sleep(500 * 1);
                                while (BodyTonerCmdFormater.getPushReceivedZeroCount() > 0) {
                                    if (mControlDeviceCallBack != null) {
                                        mControlDeviceCallBack.writeData("AT+RESET\r\nAT+RESET\r\n".getBytes());
                                        byte[] _cmd = mBluetensFormat.formatSendBluetensSwitchData(false);
                                        if (_cmd != null)
                                            mControlDeviceCallBack.writeData(_cmd);
                                    }

                                    BodyTonerCmdFormater.setPushReceivedZeroCountZero();
                                    SystemClock.sleep(500 * 1);

                                    if (isExitControl)
                                        return;
                                }

                                // 当前为升级状态ISP
                                mRestData.sendCmdState = SEND_CMD_NONE;
                                mRestData.lastSendCmd = null;
                                mRestData.mWorkState = BLE_WORK_STATE_AT_RESET;
                                mRestData.mAskState = BLE_ASK_GET_STATE_OVER;
                                // 开始升级 提示跳到升级界面
                                if (mControlDeviceCallBack != null) {
                                    mControlDeviceCallBack.responseAskWorkStateOver(mRestData);
                                }
                            } else {
                                logPrintf("askBluetensState mRestData.mAskState:"
                                        + mRestData.mAskState);
                                if (mRestData.mAskState == BLE_ASK_GET_STATE) {
                                    sendSerialCmd(EnumCmd.COMMAND_VER, null);
                                }
                            }
                        } catch (InterruptedException e) {

                        }
                    }
                }
            }.start();
            return true;
        }
        return false;
    }

    /**
     * 当需要传递模式的时候，需要先初始化要传送的数据，以便前期和设备上的文件进行MD5比较
     *
     * @param fileName
     * @param data
     */
    public void setSendData(String fileName, byte[] data) {
        mRestData.mSendName = fileName;
        mRestData.mSendData = data;
    }

    public boolean sendBluetensModelFile(byte[] data, String fileName,
                                         byte askState) {
        mRestData.mAskState = askState;
        mRestData.mSendName = fileName;
        mRestData.mSendData = data;
        return sendFile2Bluetens(fileName, data);
    }

    // 清除命令发送状态
    public void clearCmdState() {
        mRestData.clean();
        if (mByteBuffer != null)
            mByteBuffer.clear();
        isExitControl = false;
    }

    //发送蓝牙复位命令
    public boolean sendCmd2ATReset() {

        if (mControlDeviceCallBack != null) {
            Log.d(TAG, "------------------------- send A 1 ..---------------:");
            mControlDeviceCallBack.writeData("\r\nA 1\r\nA 1\r\n".getBytes());
            SystemClock.sleep(1000 * 1);

            Log.d(TAG, "------------------------- send AT+RESET ..---------------:");
            if (mControlDeviceCallBack != null)
                mControlDeviceCallBack.writeData("AT+RESET\r\nAT+RESET\r\n".getBytes());
            SystemClock.sleep(500 * 1);
            Log.d(TAG, "------------------------- send A 0 ..---------------:");
            if (mControlDeviceCallBack != null)
                return mControlDeviceCallBack.writeData("A 0\r\n".getBytes());
        }
        return false;
    }

    //需要同步才能发送蓝牙
    public boolean sendCmd2ATResetWithSync(boolean isAskQ) {

        if (mControlDeviceCallBack != null) {
            if (isAskQ) {
                Log.d(TAG, "------------------------- send ? ..---------------:");
                mControlDeviceCallBack.writeData("?".getBytes());
                SystemClock.sleep(500 * 1);
            }
            Log.d(TAG, "------------------------- send Synchronized ..---------------:");
            if (mControlDeviceCallBack != null)
                mControlDeviceCallBack.writeData("Synchronized\r\n".getBytes());
            SystemClock.sleep(500 * 1);

            Log.d(TAG, "------------------------- send AT+RESET ..---------------:");
            if (mControlDeviceCallBack != null)
                return mControlDeviceCallBack.writeData("AT+RESET\r\nAT+RESET\r\n".getBytes());
        }
        return false;
    }

    //简单发送一个\r\n结束以前的命令
    public boolean sendCmd2EndCmd() {

        if (mControlDeviceCallBack != null) {
            Log.d(TAG, "------------------------- send end ..---------------:");
            mControlDeviceCallBack.writeData("\r\n".getBytes());
            SystemClock.sleep(1000 * 1);
            return true;
        }
        return false;
    }

    // 打印参数
    public void printForTest(byte[] input) {
        StringBuffer sb = new StringBuffer();
        sb.append("printForTest:");
        for (byte b : input) {
            sb.append(String.format("%02X ", b));
        }
        logPrintf(sb.toString());
    }

    // 返回给操作者，知道当前的数据
    public static class RestData {
        public EnumCmd mSendCmd;
        public byte[] mData;

        public byte mAskState; // 状态访问是否完成
        public byte mWorkState; // 状态访问完成后，得到服务器的当前的工作状态
        public int mVerCode; // 版本信息
        public int mTickTime; // 如果已经运行，这里为运行时间
        public int mStr; // 如果已经运行，为自己的强度
        public String mSendName;
        public byte mNotifyMsg; // 回传消息类型
        public int mNotifyValue; // 回传消息的值
        // 用来标记当前发送状态的
        private EnumCmd lastSendCmd; // 最后一次发送的命令
        private byte sendCmdState = 0; // 命令的状态，0 无状态， 1 已经发送完成， 2 收到回复中
        private byte[] mSendData;

        /**
         * 清除所有的数据
         */
        public void clean() {
            sendCmdState = SEND_CMD_NONE;
            lastSendCmd = null;
            mData = null;
            mAskState = BLE_ASK_STATE_NORMAL;
            mWorkState = BLE_WORK_STATE_NORMAL;
            mVerCode = 0;
            mStr = 0;
            mTickTime = 0;
//			mSendName = null;
//			mSendData = null;
            mNotifyMsg = BLE_NOTIFY_NONE;
            mNotifyValue = 0;
        }

        /**
         * 当服务操作后，要清除对应的值
         */
        public void clearNotify() {
            mNotifyMsg = BLE_NOTIFY_NONE;
            mNotifyValue = 0;
        }
    }

}
