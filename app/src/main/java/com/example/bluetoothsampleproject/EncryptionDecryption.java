package com.example.bluetoothsampleproject;

import android.util.Log;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class EncryptionDecryption {


    public static byte[] AESEncrypt(byte[] key, byte[] iv, byte[] data, String mode) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/" + mode + "/NoPadding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher)) {
            cipherOutputStream.write(data);
        }
        return outputStream.toByteArray();
    }

    public static byte[] encrypt(byte[] key, byte[] data, String mode) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/" + mode + "/NoPadding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher)) {
            cipherOutputStream.write(data);
        }
        return outputStream.toByteArray();
    }

    public static byte[] decrypt(byte[] key, byte[] data) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, IOException {
        Cipher cipher = Cipher.getInstance("AES/" + "ECB" + "/NoPadding");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, "AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (CipherOutputStream cipherOutputStream = new CipherOutputStream(outputStream, cipher)) {
            cipherOutputStream.write(data);
        }
        return outputStream.toByteArray();
    }

    public static byte[] decryptData(byte[] secretKey, byte[] encryptedData) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(secretKey, "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);
        return (cipher.doFinal(encryptedData));
    }

    public static byte[] Rol(byte[] b) {
        byte[] r = new byte[b.length];
        byte carry = 0;

        for (int i = b.length - 1; i >= 0; i--) {
            int u = (b[i] & 0xff) << 1;
            r[i] = (byte) ((u & 0xff) + carry);
            carry = (byte) ((u & 0xff00) >>> 8);
        }

        return r;
    }


    public static byte[] KEYA(String mobileUIDA, String masterKey) {
        byte[] k0, k1, k2;
        k0 = DefaultKey(hexStringToByteArray(masterKey));
        k1 = FirstSubkey(k0);
        k2 = SecondSubKey(k1);
        String M = "01" + mobileUIDA;
        String padding = "80000000000000000000000000000000000000000000";
        byte[] D = hexStringToByteArray(M + padding);
        for (int j = 0; j < k2.length; j++)
            D[D.length - 16 + j] ^= k2[j];
        byte[] DK12;
        try {
            DK12 = AESEncrypt(hexStringToByteArray(masterKey), new byte[16], D, "CBC");
            Log.d("log_w", "DK12:   " + Utils.byteArrayToHexString(DK12));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        byte[] Diversified = Arrays.copyOfRange(DK12, DK12.length - 16, DK12.length);
        Log.d("log_w", "Master diverse:" + Utils.byteArrayToHexString(Diversified));
        return Diversified;
    }

    public static byte[] DefaultKey(byte[] masterKey) {
        try {
            return AESEncrypt(masterKey, new byte[16], new byte[16], "CBC");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] FirstSubkey(byte[] key0) {
        byte[] key = Rol(key0); //If the most significant bit of L is equal to 0, K1 is the left-shift of L by 1 bit.
        if ((key0[0] & 0x80) == 0x80)
            key[15] ^= 0x87; // Otherwise, K1 is the exclusive-OR of const_Rb and the left-shift of L by 1 bit.

        return key;
    }

    public static byte[] SecondSubKey(byte[] key1) {
        byte[] key = Rol(key1); // If the most significant bit of K1 is equal to 0, K2 is the left-shift of K1 by 1 bit.
        if ((key1[0] & 0x80) == 0x80)
            key[15] ^= 0x87; // Otherwise, K2 is the exclusive-OR of const_Rb and the left-shift of K1 by 1 bit.

        return key;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }


}

