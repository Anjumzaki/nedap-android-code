package com.example.bluetoothsampleproject;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import java.util.ArrayList;

public class AddDeviceDialog extends AlertDialog {
    String TAG = "AddDeviceDialog";
    Context context;
    private ProgressBar addDeviceProgress;
    private ArrayList<BleDeviceInfo> mBleDevices;
    private BleDeviceInfo mCurrentInfo;
    private OnConfirmInterface mOnConfirmInterface;
    ListView mDeviceListView;
    private DeviceInfoAdapter mDeviceInfoAdapter;
    private boolean mAutoSel = true;

    public AddDeviceDialog(Context context, ArrayList<BleDeviceInfo> deviceInfo) {
        super(context);
        // TODO Auto-generated constructor stub
        this.context = context;
        mBleDevices = new ArrayList<>();
        if (deviceInfo != null)
            mBleDevices.addAll(deviceInfo);
        if (mBleDevices.size() > 0)
            mCurrentInfo = deviceInfo.get(0);
    }

    public void SetOnConfirmInterface(OnConfirmInterface onConfirmInterface) {
        this.mOnConfirmInterface = onConfirmInterface;
    }

    public interface OnConfirmInterface {
        void doConfirm(BleDeviceInfo info);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        Log.d(TAG, "Create...");
        super.onCreate(savedInstanceState);
        LayoutInflater inflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams") View view = inflater.inflate(R.layout.dialog_add_device, null);
        setContentView(view);
        mDeviceInfoAdapter = new DeviceInfoAdapter(context, mBleDevices);
        mDeviceListView = view.findViewById(R.id.lvDeviceInfo);
        mDeviceListView.setAdapter(mDeviceInfoAdapter);
        mDeviceListView.setOnItemClickListener(listOnItemClickListener);

        view.findViewById(R.id.btnCancel).setOnClickListener(clickListener);
        view.findViewById(R.id.btnConfirm).setOnClickListener(clickListener);
        addDeviceProgress = findViewById(R.id.addDeviceProg);

        Window dialogWindow = getWindow();
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        DisplayMetrics d = context.getResources().getDisplayMetrics();
        lp.width = (int) (d.widthPixels * 0.9);

        dialogWindow.setAttributes(lp);

        if (mBleDevices != null && mBleDevices.size() > 0) {
            addDeviceProgress.setVisibility(View.GONE);
        } else {
            addDeviceProgress.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAutoSel = true;
        UpdateDevicesList(mBleDevices);
    }

    public void UpdateDevicesList(ArrayList<BleDeviceInfo> deviceInfo) {
        if (addDeviceProgress != null) {
            if (mBleDevices != null && mBleDevices.size() > 0) {
                addDeviceProgress.setVisibility(View.GONE);
            } else {
                addDeviceProgress.setVisibility(View.VISIBLE);
            }
        }

        if (mAutoSel) {
            if (mBleDevices == null) {
                mBleDevices = new ArrayList<>();
            } else {
                mBleDevices.clear();
            }
            if (deviceInfo != null)
                mBleDevices.addAll(deviceInfo);
            int index = -1;
            if (GlobalStaticData.getInstance().getCurrentConnectDevInfo() != null) {
                for (int i = 0; i < mBleDevices.size(); i++) {
                    if (mBleDevices.get(i).GetMacAddress().equals(
                            GlobalStaticData.getInstance().getCurrentConnectDevInfo().GetMacAddress())) {

                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    mBleDevices.add(0, GlobalStaticData.getInstance().getCurrentConnectDevInfo());
                    index = 0;
                }
            }

            if (mBleDevices != null && mBleDevices.size() > 0) {
                if (index == -1) {
                    mCurrentInfo = mBleDevices.get(0);
                    if (mDeviceInfoAdapter != null)
                        mDeviceInfoAdapter.setSelectIndex(0);
                } else {
                    mCurrentInfo = mBleDevices.get(index);
                    if (mDeviceInfoAdapter != null)
                        mDeviceInfoAdapter.setSelectIndex(index);
                }
            }
        } else {
            if (mBleDevices == null) {
                mBleDevices = new ArrayList<>();
            }
            if (deviceInfo != null) {
                for (int i = 0; i < deviceInfo.size(); i++) {
                    boolean find = false;
                    for (int j = 0; j < mBleDevices.size(); j++) {
                        if (mBleDevices.get(j).GetMacAddress().equals(
                                deviceInfo.get(i).GetMacAddress())) {
                            find = true;
                            break;
                        }
                    }
                    if (!find)
                        mBleDevices.add(deviceInfo.get(i));
                }

            }
        }
        if (mDeviceInfoAdapter != null)
            mDeviceInfoAdapter.notifyDataSetChanged();
    }

    AdapterView.OnItemClickListener listOnItemClickListener = new AdapterView.OnItemClickListener() {

        @Override
        public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
                                long arg3) {
            // TODO Auto-generated method stub
            mAutoSel = false;

            mDeviceInfoAdapter.setSelectIndex(arg2);
            mCurrentInfo = (BleDeviceInfo) mDeviceInfoAdapter.getItem(arg2);
            mDeviceInfoAdapter.notifyDataSetChanged();

        }
    };
    View.OnClickListener clickListener = new View.OnClickListener() {


        @SuppressLint("NonConstantResourceId")
        @Override
        public void onClick(View v) {
            // TODO Auto-generated method stub
            switch (v.getId()) {
                case R.id.btnCancel:
                    dismiss();
                    break;
                case R.id.btnConfirm:
                    mOnConfirmInterface.doConfirm(mCurrentInfo);
                    dismiss();
                    break;
                default:
                    break;
            }
        }

    };
}