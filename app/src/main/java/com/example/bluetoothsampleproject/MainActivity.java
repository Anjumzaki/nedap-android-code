package com.example.bluetoothsampleproject;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class MainActivity extends Activity {
    Button bluetoothButton;
    private ArrayList<BleDeviceInfo> mBleDevices;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_LOCATION = 0x88899;
    private static final long SCAN_PERIOD = 10000;
    private boolean scanning;
    String cardNumber = "";
    String logText = "";
    String MASTER_KEY = "630A2FB8AB1615F4736C254D602A394E";
    byte[] randA;
    byte[] randA_;
    byte[] randB;
    boolean isFirst;
    TextView infoText;
    EditText etCardNumber;
    byte[] randB_ = new byte[]{};
    private byte[] KEYA;
    int receiveDeviceId = -1;
    BluetoothGattCharacteristic writeCharacteristic, writeCharacteristicProperty;
    BluetoothGattCharacteristic notificationCharacteristic;
    private int mDeviceIdOld = -1;
    public boolean connected = false;
    private Handler mHandler;
    List<BluetoothGattService> service;
    Timer mTimer;
    private AddDeviceDialog mAddDeviceDialog;
    public BluetoothLeService bluetoothLeService;
    BluetoothAdapter bluetoothAdapter;
    BleDeviceInfo mLastConnectDevInfo;
    Date mToastDisconnectDate;
    Step _step = Step.NONE;


    enum Step {
        GET, NONE
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bluetoothButton = findViewById(R.id.bluetoothButton);
        etCardNumber = findViewById(R.id.cardNumber);
        infoText = findViewById(R.id.infoText);
        if (Build.VERSION.SDK_INT >= 31) {
            this.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 1);
        } else if (Build.VERSION.SDK_INT >= 23) {
            this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        InitBle();
        mAddDeviceDialog = new AddDeviceDialog(MainActivity.this, mBleDevices);
        bluetoothButton.setOnClickListener(view -> AddDevice());
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        checkPermission();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermission() {
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION};
        if (checkSelfPermission(permissions[0]) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(permissions, REQUEST_ENABLE_LOCATION);
        } else {
            startInit();
        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    void startInit() {
        if (!bluetoothAdapter.isEnabled()) {
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        mBleDevices = new ArrayList<>();
        mAddDeviceDialog = new AddDeviceDialog(MainActivity.this, mBleDevices);
        Window window = mAddDeviceDialog.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(wlp);
        mAddDeviceDialog.SetOnConfirmInterface(this::ConnectDevice);
        scanLeDevice(true);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        registerReceiver(mGattUpdateReceiver, intentFilter);
        mTimer = new Timer();
    }

    private void AddDevice() {
        scanLeDevice(true);
        mAddDeviceDialog.show();
    }


    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(() -> {
                scanning = false;
                bluetoothAdapter.stopLeScan(mLeScanCallback);
            }, SCAN_PERIOD);

            scanning = true;
            mBleDevices.clear();
            bluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            scanning = false;
            bluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void InitBle() {
        mHandler = new Handler();
        // Use this check to determine whether BLE is supported on the device.  Then you can
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "your_device_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Initializes a Bluetooth adapter
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "your_device_bluetooth_not_supported", Toast.LENGTH_SHORT).show();
            finish();
        }
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!bluetoothLeService.initialize()) {
                finish();
            }
            if (!connected) {
                bluetoothLeService.close();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            bluetoothLeService = null;
        }
    };

    public void setStatusMessage(String message) {
        infoText.append(message + "\n");
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    void ConnectDevice(BleDeviceInfo info) {
        hideSoftKeyboard(this);
        if (!etCardNumber.getText().toString().matches("")) {
            cardNumber = "08" + etCardNumber.getText().toString();
            KEYA = EncryptionDecryption.KEYA(cardNumber, MASTER_KEY);
        } else {
            Toast.makeText(this, "Please Enter Card Number", Toast.LENGTH_SHORT).show();
        }
        if (info != null && bluetoothLeService != null) {
            if (mLastConnectDevInfo != null && mLastConnectDevInfo.GetMacAddress().equals(info.GetMacAddress()) && connected) {
                return;
            }
            bluetoothLeService.disconnect();
            bluetoothLeService.close();
            mLastConnectDevInfo = info;
            setStatusMessage(" Connecting with: ");
            setStatusMessage(" " + info.GetDeviceName());
            setStatusMessage(" Device Address: " + info.GetMacAddress());
            setStatusMessage(" --------------------------------------- ");
            bluetoothLeService.connect(info.GetMacAddress());
        }
    }

    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) activity.getSystemService(
                        Activity.INPUT_METHOD_SERVICE);
        if (inputMethodManager.isAcceptingText()) {
            inputMethodManager.hideSoftInputFromWindow(
                    activity.getCurrentFocus().getWindowToken(),
                    0
            );
        }
    }

    // Device scan callback.
    BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(() -> {
                if (device == null) return;
                boolean exit = false;
                if (device.getName() != null && !device.getName().equals("")) {
                    for (BleDeviceInfo info : mBleDevices) {
                        if (info.GetDeviceName().equals(device.getName()) && info.GetMacAddress().equals(device.getAddress())) {
                            exit = true;
                        }
                    }
                    if (!exit) {
                        mBleDevices.add(new BleDeviceInfo(device.getName(), device.getAddress()));
                    }
                    if (mAddDeviceDialog != null) mAddDeviceDialog.UpdateDevicesList(mBleDevices);
                }
            });
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothLeService != null && connected) {
            bluetoothLeService.disconnect();
        }
        unbindService(mServiceConnection);
        bluetoothLeService = null;
    }

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.M)
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mDeviceIdOld = -1;
                isFirst = true;
                connected = true;
                logText = "Connected To" + mLastConnectDevInfo.GetDeviceName();
                setStatusMessage("Connected To: " + mLastConnectDevInfo.GetDeviceName());
                Log.d("log_w", "connected" + action);

            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mDeviceIdOld = -1;
                isFirst = true;
                connected = false;
                bluetoothLeService.disconnect();
                _step = Step.NONE;
                GlobalStaticData.getInstance().setCurrentConnectDevInfo(null);
                Log.d("log_w", "disConnected" + action);
                setStatusMessage("Device Disconnected---------------------------------");
                infoText.setText(null);

            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                GetGattServices(bluetoothLeService.getSupportedGattServices());
                connected = true;
                GlobalStaticData.getInstance().setCurrentConnectDevInfo(mLastConnectDevInfo);
                Log.d("log_w", "SERVICES_DISCOVERED" + action);
                setStatusMessage("KEYA: " + Utils.byteArrayToHexString(KEYA));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                setStatusMessage(" Notification Enabled");
                setStatusMessage(" Notification Occur" + notificationCharacteristic.getUuid());
                setStatusMessage(" Subscribed" + notificationCharacteristic.getUuid());
                setStatusMessage(" " + writeCharacteristic.getUuid());
                setStatusMessage(" Data Receiving");
                ReceiveData(intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA));
                byte[] data = intent.getByteArrayExtra(BluetoothLeService.EXTRA_DATA);
                setStatusMessage(" Device Challenge Received-------------------");
                setStatusMessage(" Device Challenge:" + Utils.byteArrayToHexString(data));
                byte[] value = Utils.hexStringToByteArray(cardNumber);
                if (_step == Step.NONE) {
                    Log.d("log_w", "Sending UIDA  " + cardNumber);
                    setStatusMessage("Sending UIDA  " + cardNumber);
                    setStatusMessage("Sending UIDA on UUID_ID " + writeCharacteristicProperty.getUuid());
                    Log.d("log_w", "Sending UIDA on UUID_ID " + writeCharacteristicProperty.getUuid());
                    SetStep(Step.GET);
                    sendStrData(value);
                } else if (_step == Step.GET) {
                    Log.d("log_w", "Response Type :" + String.format("%02X", data[0]));
                    setStatusMessage("Response Type :" + String.format("%02X", data[0]));
                    if (data[0] == 0x41) {
                        generateChallenge(data);
                    } else if (data[0] == 0x43) {
                        authenticateChallenge(data);
                    }
                }
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        private void generateChallenge(byte[] data) {
            Log.d("log_w", "Generating Mobile Challenge---------------------------------");
            setStatusMessage("Generating Mobile Challenge---------------------------------");
            byte[] srcb;
            srcb = data;
            setStatusMessage("data from Device :" + Utils.byteArrayToHexString(data));
            byte[] dstb = new byte[srcb.length - 1];
            System.arraycopy(srcb, 1, dstb, 0, dstb.length);
            randB = dstb;
            randB_ = new byte[dstb.length];
            System.arraycopy(dstb, 1, randB_, 0, dstb.length - 1);
            randB_[dstb.length - 1] = dstb[0];
            String uniqueKey = UniqueId.getUniqueKey(8);
            randA = uniqueKey.getBytes(StandardCharsets.UTF_8);
            byte[] randATemp = Arrays.copyOfRange(randA, 1, randA.length);
            byte[] randAFirst = Arrays.copyOfRange(randA, 0, 1);
            byte[] randA_ = ByteBuffer.allocate(randATemp.length + randAFirst.length).put(randATemp).put(randAFirst).array();
            Log.d("log_w", "randA.length ---------------------------------" + randA_.length);
            byte[] temp;
            Log.d("log_w", "randA.length ---------------------------------" + randA.length);
            Log.d("log_w", "randB_.length ---------------------------------" + randB_.length);
            ByteBuffer arrayBuffer = ByteBuffer.allocate(randA.length + randB_.length);
            arrayBuffer.put(randA);
            arrayBuffer.put(randB_);
            temp = arrayBuffer.array();
            Log.d("log_w", "temp before---------------------------------" + temp.length);
            byte[] encrypted;
            try {
                encrypted = EncryptionDecryption.encrypt(KEYA, temp, "ECB");
                Log.d("log_w", "encrypted---------------------------------" + Arrays.toString(encrypted));
                setStatusMessage("Encrypted Challenge:" + Utils.byteArrayToHexString(encrypted));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            temp = new byte[]{0x42};
            ByteBuffer tempBuffer = ByteBuffer.allocate(temp.length + encrypted.length);
            tempBuffer.put(temp);
            tempBuffer.put(encrypted);
            temp = tempBuffer.array();
            sendByteData(temp);
        }

        public void SetStep(Step newStep) {
            _step = newStep;
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        private void GetGattServices(List<BluetoothGattService> supportedGattServices) {
            if (supportedGattServices == null) return;

            for (BluetoothGattService gattService : supportedGattServices) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                service = supportedGattServices;
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString().equals(GattAttributes.DEVICE_WRITE)) {
                        Log.d("log_w", "Write Characteristic: " + gattCharacteristic.getUuid());
                        setStatusMessage("Write Characteristic: " + gattCharacteristic.getUuid());
                        writeCharacteristic = gattCharacteristic;
                    }
                    if (gattCharacteristic.getUuid().toString().equals(GattAttributes.DEVICE_WRITE_PROPERTY)) {
                        Log.d("log_w", "Write Characteristic: " + gattCharacteristic.getUuid());
                        writeCharacteristicProperty = gattCharacteristic;
                        setStatusMessage("Write Characteristic UUID_ID: " + gattCharacteristic.getUuid());
                    } else if (gattCharacteristic.getUuid().toString().equals(GattAttributes.DEVICE_NOTIFICATION)) {
                        notificationCharacteristic = gattCharacteristic;
                        bluetoothLeService.setCharacteristicNotification(notificationCharacteristic, true);
                        Log.d("log_w", "Notification Characteristic: " + gattCharacteristic.getUuid());
                        setStatusMessage("Notification Characteristic: " + gattCharacteristic.getUuid());
                        Log.d("log_w", "Notification Descriptor: " + gattCharacteristic.getDescriptors().get(0).getUuid());
                    }
                }
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.M)
        private void ReceiveData(byte[] bs) {
            if (bs == null) return;
            if (mDeviceIdOld <= 0 && bs.length > 0) {
                mDeviceIdOld = 1;
                receiveDeviceId = bs[0];
                isFirst = false;
            }
        }
    };

    private void authenticateChallenge(byte[] data) {
        Log.d("log_w", "Response Type :" + String.format("%02X", data[0]));
        setStatusMessage("Response Type :" + String.format("%02X", data[0]));
        setStatusMessage("Authentication---------------------------------");
        setStatusMessage("Authentication passed---------------------------------");
        Log.d("log_w", "Authentication---------------------------------" + data[0]);
        Log.d("log_w", "Authentication---------------------------------" + Utils.byteArrayToHexString(data));
        Log.d("log_w", "data RESP---------------------------------" + Arrays.toString(data));
        byte[] response;
        response = data;
        byte[] new_response = new byte[response.length - 1];
        System.arraycopy(response, 0, new_response, 0, response.length - 1);
        byte[] decrypt;
        try {
            decrypt = EncryptionDecryption.decrypt(KEYA, new_response);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeyException | InvalidAlgorithmParameterException |
                 IllegalBlockSizeException | BadPaddingException | IOException e) {
            throw new RuntimeException(e);
        }
        byte[] A_ = Arrays.copyOfRange(decrypt, 0, 8);
        Log.d("", "" + Arrays.toString(A_));

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void sendStrData(byte[] str) {
        if (writeCharacteristicProperty != null && bluetoothLeService != null && connected) {
            writeCharacteristicProperty.setValue(str);
            bluetoothLeService.writeCharacteristic(writeCharacteristicProperty);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void sendByteData(byte[] bs) {
        if (writeCharacteristic != null && bluetoothLeService != null && connected) {
            writeCharacteristic.setValue(bs);
            bluetoothLeService.writeCharacteristic(writeCharacteristic);
            Log.d("log_w", "sendByteData" + Arrays.toString(writeCharacteristic.getValue()));
            Log.d("log_w", "sendByteData" + writeCharacteristic.getUuid());
        } else if (bluetoothLeService != null) {
            mToastDisconnectDate = new Date();
            long diff = new Date().getTime() - mToastDisconnectDate.getTime();
            if (!scanning && !connected && diff > 35 * 1000) {
                Toast.makeText(this, "No device connection, please add device", Toast.LENGTH_SHORT).show();
                mToastDisconnectDate = new Date();
            }
        }
    }


}

