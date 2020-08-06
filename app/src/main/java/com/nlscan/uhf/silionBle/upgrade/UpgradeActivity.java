package com.nlscan.uhf.silionBle.upgrade;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;


import com.nlscan.blecommservice.IBleInterface;
import com.nlscan.blecommservice.IScanConfigCallback;
import com.nlscan.blelib.BleInterface;
import com.nlscan.blelib.ConnectStatusChangeListener;
import com.nlscan.blelib.IBleManager;
import com.nlscan.uhf.silionBle.BaseActivity;
import com.nlscan.uhf.silionBle.R;
import com.nlscan.uhf.silionBle.upgrade.dfu.DfuActivity;
import com.nlscan.uhf.silionBle.upgrade.driver.BleSerialDriver;
import com.nlscan.uhf.silionBle.upgrade.driver.BleSerialPort;


public class UpgradeActivity extends AppCompatActivity implements View.OnClickListener, ConnectStatusChangeListener {

    private TextView mUpgradeFileName, mUpgradeFileInfo;
    private String mFilePath;

    private ProgressBar mProgressBar;

    private BleSerialPort mPort = null;
    private BleServiceConnection mConnection = null;
    private IBleManager mBleManager;
    private TextView mBluetoothState,mBTCpuVer,mBTVer;
    private View selectFile,startUpgrade, upgradeTips;
    private boolean mIsConnected;
    private Native mNative = null;
    private String mBTCpuVersion;

    private static final String TAG = "BleUpdate";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upgrade);

        mUpgradeFileName = findViewById(R.id.upgrade_file_path);
        mUpgradeFileInfo = findViewById(R.id.upgrade_file_info);
        mProgressBar = findViewById(R.id.progressBar);
        mBluetoothState = findViewById(R.id.blue_state);
        mBTCpuVer = findViewById(R.id.cpu_ver);
        mBTVer = findViewById(R.id.bt_ver);
        selectFile = findViewById(R.id.select_file);
        startUpgrade = findViewById(R.id.upgrade);
        upgradeTips = findViewById(R.id.upgrade_tips);

        selectFile.setOnClickListener(this);

        Intent service = new Intent("android.nlscan.intent.action.START_BLE_SERVICE");
        service.setPackage("com.nlscan.blecommservice");
        mConnection = new BleServiceConnection();
        bindService(service, mConnection, Context.BIND_AUTO_CREATE);

        mBleManager = BleInterface.getBleManager(this);
        mBleManager.addBadgeConnectStatusChangeListener(this);

        requestPermission();
        mNative = new Native();



        mIsConnected = true;

//        getCurrentBTVersion();

        showUpgradeButton();



    }

    private void requestPermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH},0);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADMIN},0);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},0);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    private IBleInterface mBleInterface;

    @Override
    public void onBadgeConnectStatusChange(final String deviceName, final String address,final boolean isConnected) {
        Log.i("TAG", "onBadgeConnectStatusChange "+isConnected);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mIsConnected = isConnected;
                mBluetoothState.setText(isConnected ?" 已连接 "+deviceName+" "+address :"工牌未连接，请先连接工牌");
                if (isConnected){
                    getCurrentBTVersion();
                }
                showUpgradeButton();
            }
        });

    }

    private class  BleServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.i("TAG", "onServiceConnected");
            mBleInterface = IBleInterface.Stub.asInterface(service);
            getCurrentBTVersion();

            mPort = new BleSerialDriver(mBleInterface);
            showUpgradeButton();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBleInterface = null;
        }
    }

    private void getCurrentBTVersion(){

        if (mBleInterface == null)return;
        mHandler.removeMessages(0);
        mHandler.removeMessages(1);
        mHandler.sendEmptyMessageDelayed(0, 50);
        //mHandler.sendEmptyMessageDelayed(1, 200);
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (mBleInterface != null){
                try {
                    if (msg.what == 0){
                        Log.d(TAG,"query the version ");
                        mBleInterface.setScanConfig(iScanConfigCallback,"@QRYFWV;QRYBFW");//CUP版本号
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    IScanConfigCallback.Stub iScanConfigCallback = new IScanConfigCallback.Stub() {
        @Override
        public void onConfigCallback(final String str) throws RemoteException {
            Log.i("TAG","onConfigCallback:  "+str);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (str!= null ){
                        if (str.contains("QRYBFW") && str.contains("@QRYFWV")){
                            String[] split = str.split(";");
                            if (split != null)
                            {
                                for (String s : split) {
                                    if (s != null) {
                                        if (s.startsWith("@QRYFWVFirmware Version:")) {
                                            String version = s.replace("@QRYFWVFirmware Version:", "");
                                            mBTCpuVer.setText("当前CPU版本: " + version);
                                            mBTCpuVer.setVisibility(View.VISIBLE);
                                            mBTCpuVersion = version.replace("\n", "");
                                        }
                                        if (s.startsWith("QRYBFWBT Firmware Version:")) {
                                            String version = s.replace("QRYBFWBT Firmware Version:", "");
                                            mBTVer.setText("当前蓝牙版本: " + version);
                                            mBTVer.setVisibility(View.VISIBLE);
                                        }

                                    }
                                }
                            }
                        }
                    }
                }
            });
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK){
            if (data != null)
            {
                String pathstring = data.getStringExtra("selected_file_path");
                if (!TextUtils.isEmpty(pathstring))
                {
                    mFilePath = pathstring;
                    String temp = getResources().getString(R.string.SensorUpgradeFileText)+mFilePath;
                    mUpgradeFileName.setText(temp);
                    mUpgradeFileName.setVisibility(View.VISIBLE);

                    if (pathstring.endsWith(".zip")){
                        String replace = pathstring.replace(".zip", "");
                        String name = replace.replace("/storage/emulated/0/", "");
                        mUpgradeFileInfo.setText(name);
                        mUpgradeFileInfo.setVisibility(View.VISIBLE);
                    }else {
                        String importFileInfo = getImportFileInfo(pathstring);
                        mUpgradeFileInfo.setText("");
                        mUpgradeFileInfo.setVisibility(View.INVISIBLE);
                        if (!TextUtils.isEmpty(importFileInfo)){
                            mUpgradeFileInfo.setText(importFileInfo);
                            mUpgradeFileInfo.setVisibility(View.VISIBLE);
                        }
                    }
                    showUpgradeButton();
                }
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},0);
        }
        Intent intent = new Intent(UpgradeActivity.this, FileBrowserActivity.class);
        startActivityForResult(intent, 100);
    }

    public void onStartUpgrade(View view) {
        if (mFilePath != null && mFilePath.endsWith(".zip") ){
            if (mBleInterface != null){
                try {
                    mBleInterface.setScanConfig(new IScanConfigCallback.Stub() {
                        @Override
                        public void onConfigCallback(String str) throws RemoteException {
                            Log.i("TAG","onConfigCallback1:  "+str);
                            if (!TextUtils.isEmpty(str) && str.length() == 17) {
                                mBleInterface.setScanConfig(null, "@WLSOTA");

                                Intent intent = new Intent(UpgradeActivity.this, DfuActivity.class);
                                intent.putExtra("FILEPATH", mFilePath);
                                intent.putExtra("ADDRESS", str);
                                startActivity(intent);
                                finish();
                            }
                        }
                    }, "GETADDRESS");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }


        }else {
            if (mFilePath.length() >= 5){
                SerialConsoleActivity.show(UpgradeActivity.this, mPort, mFilePath);
                finish();
            }
        }
    }

    private String getImportFileInfo(String path){
        String[] strMsgList = {
                getResources().getString(R.string.Import_file_succeed),
                "OutOfMemoryError or file path is null",
                "file open false or file length is 0",
                "File length is not enough",
                "File is not bin file",
                "File crc check failed",
                "Can't find upgrade information",
        };

        int nRet = mNative.ImportBinFile(path);
        //Log.i("TAG","nRet"+nRet);
        int nShowIdx = Math.abs(nRet);
        if (nShowIdx >= strMsgList.length) {
            return "unknow return: " + nRet;
        }else if (nShowIdx == 0){
            String version = mNative.GetVersionFromImportFile();
            //Log.i("TAG",mBTCpuVersion+" -> "+version);
            if (mBTCpuVersion != null){
                return mBTCpuVersion+"升级到 "+version;
            }else {
                return "版本: "+version;
            }

        }else {
            return strMsgList[nShowIdx];
        }
    }

    private void showUpgradeButton(){
        selectFile.setVisibility(mIsConnected ? View.VISIBLE:View.INVISIBLE);
        upgradeTips.setVisibility(mIsConnected ? View.VISIBLE:View.GONE);
        if (mPort != null && !TextUtils.isEmpty(mFilePath) && mIsConnected){
            startUpgrade.setVisibility(View.VISIBLE);
            upgradeTips.setVisibility(View.GONE);
        }else {
            startUpgrade.setVisibility(View.GONE);
        }
    }

    public void onBackClick(View view) {
        onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnection != null)
            unbindService(mConnection);
        mBleManager.removeBadgeConnectStatusChangeListener(this);
        mBleManager.release();
        if (mHandler != null){
            mHandler.removeMessages(0);
            mHandler.removeMessages(1);
        }
    }
}

