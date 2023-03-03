package com.example.bluetoothsampleproject;

import java.util.HashMap;

public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String DEVICE_NOTIFICATION = "540810c2-d573-11e5-ab30-625662870761";
    public static String DEVICE_WRITE = "54080bd6-d573-11e5-ab30-625662870761";
    public static String DEVICE_WRITE_PROPERTY = "e68a5c09-aef8-4447-8f10-f3339898dee9";


    static {
        // Services.


    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}