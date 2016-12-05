package com.ultracreation.blelib.tools;

/**
 * Created by Administrator on 2016/12/4.
 */

public interface INotification{
    void onConnected();
    void onConnectedFailed();
    void onDisconnected();
}