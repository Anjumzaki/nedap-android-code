package com.example.bluetoothsampleproject;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


@RequiresApi(api = Build.VERSION_CODES.M)
public class BluetoothLeService extends Service {
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.nedAP.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_CONNECTED = "com.nedAP.ble.ACTION_GATT_CONNECTED";
    public final static String ACTION_DATA_AVAILABLE = "com.nedAP.ble.ACTION_DATA_AVAILABLE";
    public final static String ACTION_GATT_DISCONNECTED = "com.nedAP.ble.ACTION_GATT_DISCONNECTED";
    private static final UUID UUID_DEVICE_READ_DATA_DEVICE = UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb");
    public final static String EXTRA_DATA = "com.nedAP.ble.EXTRA_DATA";
    public static boolean isReceiveData = false;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    int mConnectionState = STATE_DISCONNECTED;
    ReadWriteLock myLock = null;
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // successfully connected to the GATT Server
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                // Attempts to discover services after successful connection.
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w("TAG", "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d("log_w", "onCharacteristicRead" + Arrays.toString(characteristic.getValue()));
                //call check
                broadcastUpdate(characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d("log_w", "onCharacteristicChanged" + Arrays.toString(characteristic.getValue()));
            //call check
            broadcastUpdate(characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        }
    };

    private void broadcastUpdate(final BluetoothGattCharacteristic characteristic) {
        isReceiveData = true;
        final Intent intent = new Intent(BluetoothLeService.ACTION_DATA_AVAILABLE);

        if (GattAttributes.DEVICE_NOTIFICATION.equals(String.valueOf(characteristic.getUuid()))) {
            myLock.readLock().lock();
            final byte[] data = characteristic.getValue();
            myLock.readLock().unlock();
            if (data != null && data.length > 0) {
                intent.putExtra(EXTRA_DATA, data);
            }
        }
        sendBroadcast(intent);
    }

    Binder binder = new LocalBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    public boolean initialize() {
        myLock = new ReentrantReadWriteLock(false);
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e("TAG", "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Log.e("TAG", "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            Log.w("TAG", "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d("TAG", "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w("TAG", "Device not found.  Unable to connect.");
            return false;
        }
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d("TAG", "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("TAG", "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGattDescriptor descriptor =
                characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        Log.d("log_w", "descriptor" + Arrays.toString(descriptor.getValue()));

    }

    public void setCharacteristicNotificationFalse(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (bluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("TAG", "BluetoothAdapter not initialized");
            return;
        }
        BluetoothGattDescriptor descriptor =
                characteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        Log.d("log_w", "value  23" + Arrays.toString(descriptor.getValue()));

    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("TAG", "BluetoothAdapter not initialized");
            return;
        }

        myLock = new ReentrantReadWriteLock(false);
        isReceiveData = false;
        myLock.writeLock().lock();
        mBluetoothGatt.writeCharacteristic(characteristic);
        myLock.writeLock().unlock();
    }

    public void disconnect() {
        if (bluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w("TAG", "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

}