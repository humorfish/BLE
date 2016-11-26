package com.ultracreation.blelib.utils;

import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.ultracreation.blelib.service.BluetoothLeService;


/**
 * Created by you on 2016/9/26.
 */
public class BluetoothUtils {
    private static final String TAG = BluetoothUtils.class.getSimpleName();
    public static boolean isCheckUpdateData = false;

    public static final String MINITENS = "MiniQ";
    //蓝牙名字强制增加的后缀名
    private static final String BLUETENS_NAME_EXT = ".BLT";
    private final static String NEW_BLUETENS_NAME = "BluetensX";
    private final static String OLD_BLUETENS_NAME = "BLUETENS";

    // 记录上一次bluetens 的使用的信息
    public static final String LAST_UNIQUE_ID = "lastuniqueid";
    public static final String LAST_BODYPART = "lastbodypart";
    public static final String LAST_ACTIVITY = "lastactivity";
    public static final String LAST_BLEMAC = "lastconnectmac";


    public static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.ACTION_RSSI_UPDATE);
        intentFilter.addAction(BluetoothLeService.ACTION_DEVICES_UPDATE);
        return intentFilter;
    }

    /**
     * @param srcName
     * @return 返回是否为Bluetens设备名
     */
    public static boolean isBluetensDeviceName(String srcName) {
        if (!TextUtils.isEmpty(srcName)) {

            if (NEW_BLUETENS_NAME.equals(srcName) ||
                    OLD_BLUETENS_NAME.equals(srcName))
                return true;

            int idx = srcName.lastIndexOf(BLUETENS_NAME_EXT);
            Log.d(TAG, "isBluetensDeviceName idx:" + idx);
            if (idx >= 0 && idx == srcName.length() - BLUETENS_NAME_EXT.length()) {
                return true;
            } else {
                return false;
            }
        }
        return false;
    }

    /**
     * @param verStr 数字版本号
     * @return
     */
    public static int versinStr2Int(String verStr) {
        String[] rs = verStr.split("\\.");
        try {
            if (rs.length == 4) {
                return Integer.valueOf(String.format("%02d%02d%02d", Integer.valueOf(rs[1]), Integer.valueOf(rs[2]), Integer.valueOf(rs[3])));
            } else {
                return Integer.valueOf(verStr.replaceAll(".", "").replaceAll("V", "").replaceAll("v", ""));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void setLastConnectedDevice(Context context, String lastUnique_id, String lastBodypart, String lastActivity, String lastConnectedDeviceMac) {
        //Log.i(TAG, "SET lastUnique_id="+lastUnique_id + "  lastBodypart="+lastBodypart+"  lastActivity="+ lastActivity +"  lastConnectedDeviceMac="+lastConnectedDeviceMac);
        SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", 0);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.remove(LAST_BODYPART);
        editor.remove(LAST_ACTIVITY);
        editor.remove(LAST_BLEMAC);
        editor.remove(LAST_UNIQUE_ID);

        editor.putString(LAST_BODYPART, lastBodypart);
        editor.putString(LAST_ACTIVITY, lastActivity);
        editor.putString(LAST_BLEMAC, lastConnectedDeviceMac);
        editor.putString(LAST_UNIQUE_ID, lastUnique_id);
        editor.commit();
    }

    public static String[] getLastConnectedDevice(Context context) {
        SharedPreferences userPrefs = context.getSharedPreferences("UserPrefs", 0);

        String lastUniqueId = userPrefs.getString(LAST_UNIQUE_ID, "");
        String lastBodypart = userPrefs.getString(LAST_BODYPART, "");
        String lastActivity = userPrefs.getString(LAST_ACTIVITY, "");
        String lastBleMac = userPrefs.getString(LAST_BLEMAC, "");
        String[] message = new String[4];
        message[0] = lastBodypart;
        message[1] = lastActivity;
        message[2] = lastBleMac;
        message[3] = lastUniqueId;

        //Log.i(TAG, "GET lastUnique_id="+lastUniqueId + "  lastBodypart="+lastBodypart+"  lastActivity="+ lastActivity +"  lastConnectedDeviceMac="+lastBleMac);
        return message;
    }

    private boolean isSameWithLastUse(Context context, int unique_id, String mBoodypart, String mActivity, String bleMac) {
        String _unique_id = String.valueOf(unique_id);

        String[] lastUserMessage = BluetoothUtils.getLastConnectedDevice(context);
        String lastUseBodypart = lastUserMessage[0];
        String lastUseActivity = lastUserMessage[1];
        String lastUseBleMac = lastUserMessage[2];
        String lastUniqueId = lastUserMessage[3];
        //Log.i(TAG, "lastUseBodypart = "+ lastUseBodypart + "  lastUseActivity="+ lastUseActivity+ "  lastUseBleMac="+ lastUseBleMac+ " lastUniqueId="+lastUniqueId);
        if ((lastUniqueId != null && !lastUniqueId.isEmpty() && lastUniqueId.equals(_unique_id)) && (lastUseBodypart != null && lastUseBodypart.equals(mBoodypart)) && (lastUseActivity != null && lastUseActivity.equals(mActivity)) && (lastUseBleMac != null && lastUseBleMac.equals(bleMac)))
            return true;
        return false;
    }

}
