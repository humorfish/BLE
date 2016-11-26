package com.ultracreation.blelib.bean;

import com.ultracreation.ble_lib.utils.BluetoothUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author liuww
 * @data 20150514
 * @info Bluetens新的数据通讯，采用shell模式
 */
public class BluetensShell extends TBluetensFormat {

    /**
     * shell 命令的开始字符
     */
    private final static byte CMD_HEADER = '>';

    /**
     * shell 命令的第一个结束字符 \r
     */
    private final static byte CMD_TAIL_1 = '\r';//0X0D;

    /**
     * shell 命令的第二个结束字符\n
     */
    private final static byte CMD_TAIL_2 = '\n';//0X0A;


    //命令的扩展长度，1 个字节头 + 2个字节的尾
    private final static int LEN_CMD_EXT = 3;

    /**
     * shell 的字符命令
     */
    private final static String[] SHELL_CMD_STR = {
            "help",                    //帮助信息
            "id",                        //显示或设置EPK
            "ver",                        //显示固件版本号
            "hv",                        //
            "bat",                        //得到电量
            "chng",                    //显示冲电状态， yes/no
            "ls",                            //列表文件系统， 列给格式 ：文件名 + 文件大小
            //参数 ： * 或文件名，由于嵌入式系统能力，暂时不支持通配符
            "dump",                //读取文件， [-p=文件位置]  [-l=长度]
            //判断dump结束，如读取到\r\n时，判断后面的字符为："2:" + " end of dump"
            "cat",                        //传递文件到设备上  <file> [-p=pos] [-l=len]
            "fmt BBFS",                            //格式化文件系统
            "cs",
            "crc",                        //计算文件crc16；参数  文件名 -p=指定要计算的位置， -l=指定要计算的长度
            //返回crc16
            "md5",                    //计算文件md5 ；参数 文件名 -p=指定要计算的位置， -l=指定要计算的长度，不指定时读到文件尾
            //返回 md5
            "ptab",                    //显示脉冲校准表
            "ftab",                    //显示频率校准表
            "stab",                    //显示强度校准表 ? 无参数返回
            "freq",                    //显示设置频率 ，无参数：返回当前频率
            // <频率> 1~1200 Hz, 超过范围系统使用54 Hz
            //设置频率 ： 返回值： 当前或设置的频率值
            "puls",                    //显示或设置脉冲, 无参数 ： 返回当前脉冲
            //<微秒> 4~150 Us, 超过范围系统使用 100 us
            //设置脉冲: 返回值， 当前或设置的脉冲值
            "str",                        //显示或设置强度, 无参数 ：返回当前强度
            //<强度值> 1~60, 0 等同于关闭, 超过范围系统设置 0
            //设置脉冲  返回值： 当前或设置的强度值
            "osta",                    //开始输出 返回值 ： ok
            "osto",                    //结束输出  返回值  :  ok
            "ssta",                    //执行脚本  参数 [文件名]
            "ota",                  //ota 升级
            "intv",                        //执行的间隔时间 ms
            "isp",                        //跳转到升级模式
            "?",                            //询问是否升级的问号
            "btnm",                    //修改bluetens名字
            "sdef",                    //设置默认模式
            "tick",                        //获取当前程序运行状态
            "rm",                        //删除文件
            "stat",                    //得到当前设备运行的状态
            "rst bt",                    //复位蓝牙
            "shdn"                        //关机
    };


    @Override
    public byte[] getFormatData(EnumCmd enumCmd,
                                String cmdValue) {
        byte[] cmdStr = SHELL_CMD_STR[enumCmd.ordinal()].getBytes();
        if (cmdStr != null) {
            byte[] data = null;
            int lenValue = 0;
            if (cmdValue != null)
                lenValue = cmdValue.length();

            if (enumCmd.ordinal() <= EnumCmd.COMMAND_OFF.ordinal()) {
                //这些只需要传送一个命令就行了
                //发送的字节
                if (lenValue > 0)
                    data = new byte[LEN_CMD_EXT + lenValue + cmdStr.length + 1];
                else
                    data = new byte[LEN_CMD_EXT + lenValue + cmdStr.length];

                int i = 0;
                //命令的头
                data[i++] = CMD_HEADER;
                //具体的命令
                for (int n = 0; n < cmdStr.length; n++) {
                    data[i++] = cmdStr[n];
                }

                //命令的参数
                if (lenValue > 0) {
                    //命令中间的空格
                    data[i++] = ' ';
                    byte[] _value = cmdValue.getBytes();
                    for (int n = 0; n < _value.length; n++) {
                        data[i++] = _value[n];
                    }
                }

                //命令结束符
                data[i++] = CMD_TAIL_1;
                data[i++] = CMD_TAIL_2;
            } else {

            }
            return data;
        }
        return null;
    }


    public byte[][] getFormatData2(EnumCmd enumCmd,
                                   String cmdValue) {
        byte[] cmdStr = SHELL_CMD_STR[enumCmd.ordinal()].getBytes();
        if (cmdStr != null) {
            byte[] data1 = null;
            byte[] data2 = null;
            int lenValue = 0;
            if (cmdValue != null)
                lenValue = cmdValue.length();

            if (enumCmd.ordinal() <= EnumCmd.COMMAND_OFF.ordinal()) {
                //这些只需要传送一个命令就行了
                //发送的字节
                if (lenValue > 0)
                    data1 = new byte[1 + lenValue + cmdStr.length + 1];
                else
                    data1 = new byte[1 + lenValue + cmdStr.length];

                int i = 0;
                //命令的头
                data1[i++] = CMD_HEADER;
                //具体的命令
                for (int n = 0; n < cmdStr.length; n++) {
                    data1[i++] = cmdStr[n];
                }

                //命令的参数
                if (lenValue > 0) {
                    //命令中间的空格
                    data1[i++] = ' ';
                    byte[] _value = cmdValue.getBytes();
                    for (int n = 0; n < _value.length; n++) {
                        data1[i++] = _value[n];
                    }
                }

                //命令结束符
                data2 = new byte[2];
                data2[0] = CMD_TAIL_1;
                data2[1] = CMD_TAIL_2;

                byte[][] restData = new byte[2][];
                restData[0] = data1;
                restData[1] = data2;
                return restData;
            } else {

            }
            return null;
        }
        return null;
    }


    @Override
    public EnumCmd getReadDataType(byte[] data) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public byte[] formatSendBluetensVerData() {
        return getFormatData(EnumCmd.COMMAND_VER, null);
    }


    @Override
    public int resolveBluetensVerData(byte[] data) {
        /*if(data != null && data.length>2) {
			byte[] _data = new byte[data.length-2];
			System.arraycopy(data, 0, _data, 0, data.length-2);
			String _ver = byte2String(_data);
			if(_ver != null) {
//				_ver = _ver.replace("\r", "").replace("\n", "").replace(".", "").replace("v", "");
				_ver = _ver.replace(".", "").replace("v", "");
				if(_ver.getBytes().length>0 &&
						_ver.getBytes()[0] >='0' && _ver.getBytes()[0]<='9' )
				return string2Int(_ver);
			}
		}*/
        if (data != null && data.length > 2) {
            byte[] _data = new byte[data.length - 2];

            System.arraycopy(data, 0, _data, 0, data.length - 2);
            String _ver = byte2String(_data);
            if (_ver != null) {
               return BluetoothUtils.versinStr2Int(_ver.replace("\r", "").replace("\n", ""));
            }
        }
        return 0;
    }


    @Override
    public byte[] formatSendBluetensEPKData(String epkId) {
        return getFormatData(EnumCmd.COMMAND_ID, epkId);
    }


    @Override
    public String resolveBluetensEPKData(byte[] data) {
        return byte2String(data);
    }


    @Override
    public byte[] formatSendBluetensSwitchData(boolean isOn) {
        if (isOn)
            return null;
        else
            return getFormatData(EnumCmd.COMMAND_OFF, null);
    }


    @Override
    public byte[] formatSendBluetensWorkData(boolean isWork) {
        if (isWork)
            return getFormatData(EnumCmd.COMMAND_OSTA, null);
        else
            return getFormatData(EnumCmd.COMMAND_OSTO, null);
    }


    @Override
    public boolean resolveBluetensChng(byte[] data) {
        String _str = byte2String(data);
        if (_str != null && _str.contains("yes"))
            return true;
        return false;
    }

    @Override
    public byte[] formatSendBluetensChng() {
        return getFormatData(EnumCmd.COMMAND_CHNG, null);
    }


    @Override
    public byte[] formatSendBluetensLs(String fileName) {
        return getFormatData(EnumCmd.COMMAND_LS, fileName);
    }


    @Override
    public String resolveBluetensLs(byte[] data) {
        return byte2String(data);
    }


    @Override
    public byte[] formatSendBluetensDump(String fileName, int filePos, int len) {
        String _value = fileName;
        if (filePos >= 0) {
            _value += " -p=" + filePos;
        }
        if (len >= 0) {
            _value += " -l=" + len;
        }
        return getFormatData(EnumCmd.COMMAND_DUMP, _value);
    }


    @Override
    public byte[] resolveSendBluetensDump(byte[] data) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public byte[] formatSendBluetensCat(String fileName, int filePos, int len) {
        String _value = fileName;
        if (filePos >= 0) {
            _value += " -p=" + filePos;
        }
        if (len >= 0) {
            _value += " -l=" + len;
        }
        return getFormatData(EnumCmd.COMMAND_CAT, _value);
    }


    @Override
    public boolean resolveSendBluetensCat(byte[] data) {
        // TODO Auto-generated method stub
        return true;
    }


    @Override
    public byte[] formatSendBluetensCRC(String fileName, int filePos, int len) {
        String _value = fileName;
        if (filePos >= 0) {
            _value += " -p=" + filePos;
        }
        if (len >= 0) {
            _value += " -l=" + len;
        }
        return getFormatData(EnumCmd.COMMAND_CRC, _value);
    }


    @Override
    public byte[] resolveSendBluetensCRC(byte[] data) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public byte[] formatSendBluetensMD5(String fileName, int filePos, int len) {
        String _value = fileName;
        if (filePos >= 0) {
            _value += " -p=" + filePos;
        }
        if (len >= 0) {
            _value += " -l=" + len;
        }
        return getFormatData(EnumCmd.COMMAND_MD5, _value);
    }


    @Override
    public String resolveSendBluetensMD5(byte[] data) {
        return byte2String(data);
    }


    @Override
    public String resolveBluetensPTab(byte[] data) {
        // TODO Auto-generated method stub
        return byte2String(data);
    }


    @Override
    public String resolveBluetensFTab(byte[] data) {
        // TODO Auto-generated method stub
        return byte2String(data);
    }


    @Override
    public int resolveBluetensSTab(byte[] data) {
        return byteString2Int(data);
    }


    @Override
    public int resolveBluetensFREQ(byte[] data) {
        return byteString2Int(data);
    }


    @Override
    public byte[] formatSendBluetensFREQ(int value) {
        String _data = null;
        if (value >= 0)
            _data = String.valueOf(value);

        return getFormatData(EnumCmd.COMMAND_FREQ, _data);
    }


    @Override
    public int resolveBluetensPULS(byte[] data) {
        return byteString2Int(data);
    }


    @Override
    public byte[] formatSendBluetensPULS(int value) {
        String _data = null;
        if (value >= 0)
            _data = String.valueOf(value);

        return getFormatData(EnumCmd.COMMAND_PULS, _data);
    }


    @Override
    public int resolveBluetensSTR(byte[] data) {
        int strengthInt = 0;
        String strength = new String(data);
        try {
            if (strength != null && strength.startsWith("str="))
                strength = strength.replace("str=", "");

            if (strength != null) {
                strength = strength.replace("\r\n", "");
                strengthInt = Integer.valueOf(strength);
                if (strengthInt < 0 || strengthInt > 60)
                    strengthInt = 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return strengthInt;
    }


    @Override
    public byte[] formatSendBluetensSTR(int value) {
        String _data = null;
        if (value >= 0)
            _data = String.valueOf(value);

        return getFormatData(EnumCmd.COMMAND_STR, _data);
    }


    @Override
    public int resolveBluetensIntv(byte[] data) {
        return byteString2Int(data);
    }


    @Override
    public byte[] formatSendBluetensIntv(int value) {
        String _data = null;
        if (value >= 0)
            _data = String.valueOf(value);

        return getFormatData(EnumCmd.COMMAND_INTV, _data);
    }


    @Override
    public byte[] formatSendBluetensHelpData() {
        return getFormatData(EnumCmd.COMMAND_HELP, null);
    }


    @Override
    public String resolveBluetensHelpData(byte[] data) {
        return byte2String(data);
    }


    @Override
    public byte[] formatSendBluetensSsta(String filename) {
        return getFormatData(EnumCmd.COMMAND_SSTA, filename);
    }

    @Override
    public boolean resolveBluetensSsta(byte[] data) {
        String _rest = byte2String(data);
//		if(_rest.startsWith("ok"))
        if (_rest.contains("ok"))
            return true;
        else
            return false;
    }

    @Override
    public byte[] formatSendBluetensIsp() {
        return getFormatData(EnumCmd.COMMAND_ISP, null);
    }

    //如果返回的都是字符信息，则直接转为String
    private String byte2String(byte[] data) {
        if (data != null && data.length > 0) {
            return new String(data).replace("\r\n", "");
        }
        return null;
    }


    @Override
    public int resolveBluetensTick(byte[] data) {
        if (data != null) {
            int idx = 0;
            for (int i = 0; i < data.length; i++) {
                if (data[i] == 0X20) {
                    idx = i;
                }
            }
            if (idx > 0) {
                byte[] _data = new byte[idx];
                System.arraycopy(data, 0, _data, 0, idx);
                return byteString2Int(_data);
            } else {
                return byteString2Int(data);
            }
        }
        return 0;
    }

    @Override
    public int resolveBluetensBat(byte[] data) {
        if (data[0] >= '0' && data[0] <= '9') {
            String _str = byte2String(data);
            if (_str != null) {
                int _bat = 0;
                try {
                    _bat = Integer.valueOf(_str.replace("\r\n", "").replace(" mv", ""));
                } catch (Exception e) {
                    e.printStackTrace();
                }
//				return _bat/100;
                return _bat;
            }
        }
        return 0;
    }

    @Override
    public Map<String, String> resolveBluetensState(byte[] data) {
        try {
            String _str = new String(data);
            if (_str != null && !_str.isEmpty()) {
                HashMap<String, String> m = new HashMap<String, String>();
                String[] strs = _str.split(",");
                for (String str : strs) {
                    if (str.split("=").length > 1) {
                        m.put(str.split("=")[0], str.split("=")[1]);
                    }
                }
                return m;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //把byte的string数据转成int
    private int byteString2Int(byte[] data) {
        if (data[0] >= '0' && data[0] <= '9') {
            String _str = byte2String(data);
            if (_str != null) {
                int _data = 0;
                try {
                    _data = Integer.valueOf(_str.replace("\r\n", ""));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return _data;
            }
        }
        return 0;
    }


    //转数据异常处理
    private int string2Int(String value) {
        int _value = 0;
        try {
            _value = Integer.valueOf(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return _value;
    }


}
