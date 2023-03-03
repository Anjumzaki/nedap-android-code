package com.example.bluetoothsampleproject;

import java.io.Serializable;

public class BleDeviceInfo implements Serializable {
    private static final long serialVersionUID = -7060210544600464481L;
    String mDeviceName;
    String mMacAddress;

    public BleDeviceInfo(String name, String address) {
        mDeviceName = name;
        mMacAddress = address;
    }

    public String GetDeviceName() {
        return mDeviceName;
    }

    public String GetMacAddress() {
        return mMacAddress;
    }

}

