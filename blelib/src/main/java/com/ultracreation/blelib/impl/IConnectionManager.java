package com.ultracreation.blelib.impl;

import android.support.annotation.NonNull;

/**
 * Created by you on 2017/1/19.
 */

public interface IConnectionManager
{
    void disconnect(@NonNull String deviceId);
    boolean isConnected(@NonNull String deviceId);
    void connect(@NonNull String deviceId);
    void clear();
}
