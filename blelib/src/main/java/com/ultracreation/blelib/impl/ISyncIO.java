package com.ultracreation.blelib.impl;


import io.reactivex.Observable;

/**
 * Created by you on 2017/1/16.
 */

public interface ISyncIO
{
    Observable<Integer> Synchronize(IStream callback);
}
