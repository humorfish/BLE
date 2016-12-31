package com.ultracreation.blelib.tools;

/**
 * Created by you on 2016/12/31.
 */

public interface IShellRequestManager
{
    void addRequest(TShell.TShellSimpleRequest request);
    void removeRequest(TShell.TShellSimpleRequest request);
    void execute();
}
