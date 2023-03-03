package com.example.bluetoothsampleproject;

public class GlobalStaticData {
    static private GlobalStaticData mPrivateData = null;
    private BleDeviceInfo mCurrentConnectDevInfo = null;

    static public GlobalStaticData getInstance() {
        if (mPrivateData == null) {
            mPrivateData = new GlobalStaticData();
        }
        return mPrivateData;
    }

    public void setCurrentConnectDevInfo(BleDeviceInfo info) {
        mCurrentConnectDevInfo = info;
    }

    public BleDeviceInfo getCurrentConnectDevInfo() {
        return mCurrentConnectDevInfo;
    }


}
