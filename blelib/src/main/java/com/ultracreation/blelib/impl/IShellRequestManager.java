package com.ultracreation.blelib.impl;

import com.ultracreation.blelib.tools.TShellRequest;

/**
 * Created by you on 2016/12/31.
 */

public interface IShellRequestManager
{
    void addRequest(TShellRequest request);
    void removeRequest(TShellRequest request);
    void execute();
    void onError(String message);
    void onNotification(String message);
    void clearRequest();
}
