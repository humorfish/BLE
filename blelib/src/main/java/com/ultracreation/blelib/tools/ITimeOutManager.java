package com.ultracreation.blelib.tools;

/**
 * Created by Administrator on 2016/12/27.
 */
public interface ITimeOutManager
{
    void startConnectTimeOut(int timeOut, ITimeOutCallBack callBack );
    void startQuestTimeOut(int timeOut, ITimeOutCallBack callBack);
    void setTimeOut(int timeOut, ITimeOutCallBack callBack);
    void clearTimeOut();
    void removeTimeOut();
}
