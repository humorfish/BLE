package com.ultracreation.blelib.utils;

import android.util.Log;
import com.ultracreation.blelib.impl.BodyTonerCmdFormaterListener;


public class BodyTonerCmdFormater {

    /**
     * The min length of command.
     */
    public static final int kBtCmdMinLength = 3;
    private static final String TAG = BodyTonerCmdFormater.class.getSimpleName();
    private final static byte shellEndCh1 = '\r';

    private final static byte shellEndCh2 = '\n';
    //临时变量，有时间需要更正这种模式
    public static boolean isShellAskSync = false;            //shell模式下访问同步的？号
    private static int pushReceivedZeroCount = 0;
    /**
     * 当前的数据回复模式 true 为新的shll 命令模式
     */
    private boolean isShellCmdMode = true;
    private boolean isShellWork = true;            //当前是发送>命令的工作模式
    /**
     * Listener
     */
    private BodyTonerCmdFormaterListener _listener = null;
    /**
     * Received data.
     */
    private XData _rcvBuf = null;

    /**
     * The constructor.
     */
    public BodyTonerCmdFormater() {
        _rcvBuf = new XData();
    }

    /**
     * @return 得到读取到很多0的计数
     */
    public static int getPushReceivedZeroCount() {
        return pushReceivedZeroCount;
    }

    public static void setPushReceivedZeroCountZero() {
        pushReceivedZeroCount = 0;
    }

    /**
     * Register listener.
     *
     * @param listener
     */
    public void registerListener(BodyTonerCmdFormaterListener listener) {
        _listener = listener;
    }

    /**
     * Clear the content.
     */
    public void clear() {
        _rcvBuf.clear();
    }

    /**
     * Push the received data together and parse it.
     *
     * @param data
     */
    public void pushReceivedData(final byte[] data, String macAddr) {

        if (null != data) {
            if (data.length > 0) {
                KLog.i(TAG, "*************** pushReceivedData src :" + XByteFormat.bytesToStr(data, 0, data.length));
                if (isShellCmdMode)
                    KLog.i(TAG, "pushReceivedData isShellCmdMode  data:" + new String(data));
                _rcvBuf.append(data, 0, data.length);

                if (_rcvBuf.length() > 512 && _rcvBuf.data() != null) {
                    boolean isAllZero = true;
                    byte[] c = _rcvBuf.data();
                    for (int i = 0; i < 512; i++) {
                        if (c[i] != 0) {
                            isAllZero = false;
                            break;
                        }
                    }
                    if (isAllZero) {
                        _rcvBuf.replace(0, 512, null);
                        pushReceivedZeroCount++;
                    }
                }
            }
        }

        XRange r;
        KLog.i(TAG, "pushReceivedData _rcvBuf.length():" + _rcvBuf.length());

        //新的命令模式，数据以\r\n结束 当为？的时候 不要\r\n
        while (_rcvBuf.length() >= 1) {

            final byte[] c = _rcvBuf.data();
            if (null != c) {
                XLog.i(TAG, "pushReceivedData shell 2:" + XByteFormat.bytesToStr(c, 0, c.length));
            }

            r = fetchShell(c);

            XLog.i(TAG, "pushReceivedData shell 3 r.location:" + r.location + "  r.length:" + r.length);
            if (r.length > 0) {
                // Notify.
                XLog.v(TAG, "FETCHED: shell range:" + r.location + "," + r.length);
                notifyReceivedCommand(_rcvBuf.subData(r.location, r.length), macAddr);

                // Remove.
                r.location = 0;
                _rcvBuf.replace(r.location, r.length, null);
            } else {
                break;
            }
        }
    }

    /**
     * Fetch one command.
     *
     * @param c
     * @return the range of the command.
     */
    private final XRange fetch(byte[] c) {
        int n = 0;
        int nMax = c.length;
        int len = 0;
        byte crc = 0;
        XRange r = new XRange();

        int _maxLen = 40;

        while (n < nMax) {
            if (BLTDeviceCommandConstants.kPackHeader == c[n] && n + 1 < nMax) {
                len = c[n + 1] & 0XFF;
                if (BluetoothUtils.isCheckUpdateData && n + 2 < nMax) {
                    if (c[n + 2] > BLTDeviceCommandConstants.kCommandResponseValidFlashCompleted) {
                        //不是需要的命令，直接退出
                        n += 2;
                        Log.d(TAG, "fetch continue len" + len + " n:" + n);
                        continue;
//						r.length = n +2;
//						break;
                    }
                    if (c[n + 2] == BLTDeviceCommandConstants.kCommandResponseUpgradeError ||
                            c[n + 2] == BLTDeviceCommandConstants.kCommandDevicePackError)
                        _maxLen = 132;
                }

//				Log.d(TAG, "fetch n:" +n + " len:" + len + " nMax:" + nMax + " maxLen:" + _maxLen);
//				Log.d(TAG, "fetch n+len+3" + (n+len+3) + " cheng:" + (c[n+1] & 0XFF) );
                if (n + len + 3 > nMax && (len < _maxLen)) {//|| len==0x45
                    r.length = 0;
                    break;
                } else if (n + len + 3 <= nMax) {
                    crc = BLTDeviceCommandConstants.btCrc(c, n + 1, len + 1);
                    Log.d(TAG, "fetch crc:" + crc + " = " + c[n + len + 2]);
                    if (crc == c[n + len + 2]) {
                        r.location = n + 2;
                        r.length = len;
                        break;
                    }
                }
            }
            n++;
//			Log.d(TAG, "fetch n:" + n );
        }

        return r;
    }

    /**
     * 针对shell命令参数的解析方法
     *
     * @param c
     * @return the range of the command.
     */
    private final XRange fetchShell(byte[] c) {
        int n = 0;
        int nMax = c.length;
        XRange r = new XRange();

        int _maxLen = 512;
        r.location = 0;
        while (n < nMax) {
            if (0 != c[n]) {
                r.location = n;
                break;
            }
            n++;
        }

        Log.d(TAG, "fetch isShellAskSync:" + isShellAskSync + " isShellWork:" + isShellWork);

        while (n < nMax) {
            if (shellEndCh1 == c[n]) {
                if (n + 1 < nMax && shellEndCh2 == c[n + 1]) {
//					r.location = 0;
                    r.length = n + 2;
                } else if (!(isShellWork && !isShellAskSync)) {
                    r.length = n + 1;
                }
                break;
            } else if (shellEndCh2 == c[n] &&
                    (n > 0 && c[n - 1] == '?')) {
                if (n + 1 < nMax && shellEndCh2 == c[n + 1]) {
                    r.location = n - 1;
                    r.length = n + 2;
                }
                break;
            } else if (isShellAskSync && c[n] == '?') {
                isShellAskSync = false;
                r.location = n;
                r.length = n + 1;
                break;
            } else if (n >= _maxLen - 1) {
//				r.location = 0;
                r.length = n + 1;
                break;
            }
            n++;
        }

        if (isShellAskSync && r.length > 0) {
            isShellAskSync = false;
        }
        return r;
    }

    /**
     * Notify the received command.
     *
     * @param data
     */
    private void notifyReceivedCommand(byte[] data, String macAddr) {
        KLog.v(TAG, "  notifyReceivedCommand _listener:" + _listener);
        if (null != _listener) {
            _listener.OnCommandReady(data, macAddr);
        }
    }

    //解析很多的命令数据
    private byte[][] parseDataToArray(byte[] value) {
        int i = 0, n = 0;
        if (value.length <= 5) {
            byte[][] dataArray = new byte[1][];
            dataArray[0] = value;
            return dataArray;
        }
        for (i = 0; i < value.length; i++) {
            if (value[i] == BLTDeviceCommandConstants.kPackHeader)
                n++;
        }
        if (n > 0) {
            byte[][] dataArray = new byte[n][];
            n = 0;

            for (i = 0; i < value.length; i++) {
                if (value[i] == BLTDeviceCommandConstants.kPackHeader) {
                    int j;
                    for (j = i + 1; j < value.length; j++) {
                        if (value[j] == BLTDeviceCommandConstants.kPackHeader) {
                            break;
                        }
                    }
                    // copy数据
                    Log.d(TAG, " parseDataToArray i:" + i + " j:" + j + " dec:"
                            + (j - i) + " n:" + n);
                    if (i != j) {
                        dataArray[n] = new byte[j - i];
                        Log.d(TAG, " parseDataToArray  dec:" + (j - i) + " n:"
                                + n);
                        System.arraycopy(value, i, dataArray[n], 0, j - i);
                    } else {
                        dataArray[n] = null;
                    }
                    n++;
                }
            }
            return dataArray;
        }

        return null;
    }
}
