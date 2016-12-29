package com.ultracreation.blelib.tools;

/**
 * Created by Administrator on 2016/12/15.
 */

public abstract class IGapConnection
{
    String deviceId;

    abstract void connect();
    abstract void disconnect();
    abstract boolean isConnected();

    abstract void write(String cmd);
    abstract void writeNoResponse(byte[] buf);
}