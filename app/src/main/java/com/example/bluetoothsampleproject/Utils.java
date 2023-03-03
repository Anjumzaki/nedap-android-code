package com.example.bluetoothsampleproject;

import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Utils {
    public static void showMessageDebug(String message) {
        showMessage(message);
    }

    public static void showMessage(String message) {
        //
    }

    public static String byteArrayToHexString(byte[] buf) {
        if (buf == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : buf) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim().replace(" ", "");
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }

        return data;
    }

    public static byte[] concat(byte[] array1, byte[] array2) {
        byte[] concatenated = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, concatenated, 0, array1.length);
        System.arraycopy(array2, 0, concatenated, array1.length, array2.length);
        return concatenated;
    }

    public static byte[] rotateLeft(byte[] arr) {
        if (arr == null || arr.length < 2) {
            return arr;
        }
        byte[] head = Arrays.copyOfRange(arr, 0, 1);
        byte[] tail = Arrays.copyOfRange(arr, 1, arr.length);
        return concat(tail, head);
    }

    public static byte[] prepend(byte[] a, byte el) {
        byte[] c = new byte[a.length + 1];
        c[0] = el;
        System.arraycopy(a, 0, c, 1, a.length);
        return c;
    }

    public static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding ");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        return cipher.doFinal(clear);
    }

    public static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding ");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        return cipher.doFinal(encrypted);
    }

}
