package com.example.bluetoothsampleproject;


import java.security.SecureRandom;

public class UniqueId {
    public static String getUniqueKey(int length) {
        int maxSize;
        maxSize = length;
        char[] chars;
        String a = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        chars = a.toCharArray();
        int size;
        byte[] data = new byte[1];
        SecureRandom crypto = new SecureRandom();
        crypto.nextBytes(data);
        size = maxSize;
        data = new byte[size];
        crypto.nextBytes(data);
        StringBuilder result = new StringBuilder(size);
        for (byte b : data) {
            result.append(chars[Math.abs(b) % (chars.length - 1)]);
        }
        return result.toString();
    }
}