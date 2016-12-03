package com.ultracreation.ble.shell;

/**
 * Created by you on 2016/11/29.
 */
public enum  TShell {
    instance;

    TShell(){}

    private class TBLEManager implements IBLEManager{
        @Override
        public void write() {

        }

        @Override
        public void stopScan() {

        }

        @Override
        public void startScan() {

        }
    }

    interface IBLEManager{
        void write();
        void stopScan();
        void startScan();
    }
}
