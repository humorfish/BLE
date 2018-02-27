package com.ultracreation.blelib.impl;

import android.support.annotation.NonNull;

import com.ultracreation.blelib.tools.TGapConnection;

/**
 * Created by you on 2017/1/19.
 */

public interface IConnectionManager
{
    TGapConnection getConnection(@NonNull String deviceId);
    void disconnect(@NonNull String deviceId);
    boolean isConnected(@NonNull String deviceId);
    TGapConnection connect(@NonNull String deviceId);
    void clear();
    void addDisconnectListener();
    void start(String deviceId);
}
