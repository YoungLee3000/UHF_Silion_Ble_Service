/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.nlscan.uhf.silionBle.upgrade;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;


import com.nlscan.blecommservice.IScanConfigCallback;
import com.nlscan.uhf.silionBle.R;
import com.nlscan.uhf.silionBle.upgrade.driver.BleSerialPort;
import com.nlscan.uhf.silionBle.upgrade.util.HexDump;


import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialConsoleActivity extends AppCompatActivity implements Handler.Callback{

    private final String TAG = SerialConsoleActivity.class.getSimpleName();


    private static BleSerialPort sPort = null;

    private TextView mTitleTextView;
    private TextView mDumpTextView;
    private ScrollView mScrollView;
    private int READDATA_MAXLEN = 1024;
    private Handler mHandler = null;
    private Native mNative = null;
    private static String mstrFilePath = null;
    private SimpleDateFormat mSimpleDateFormat;

    private String GetRevData(int nTimeOut)
    {
        Log.d(TAG, " GetRevData:   start ... "+nTimeOut);
        if (nTimeOut < 10)
        {
            nTimeOut = 10;
        }
        int nTimeAll = 0;
        while (nTimeAll < nTimeOut)
        {
            SystemClock.sleep(1);
            nTimeAll += 1;
            if (mRevData != null){
                Log.d(TAG, " GetRevData:  "+ mRevData+"  use time:  "+nTimeAll);
                return mRevData;
            }
        }
        Log.d(TAG, " GetRevData:  timeout ");
        return null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, " onCreate");
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.serial_console);
        //mTitleTextView = (TextView) findViewById(R.id.demoTitle);
        mDumpTextView = findViewById(R.id.consoleText);
        mScrollView = findViewById(R.id.demoScroller);
        
        //Button bt = (Button)findViewById(R.id.btnUpgrade);
        //bt.setOnClickListener(btnUpgradeonClick);
        
        mHandler = new Handler(this);
        
        mNative = new Native();

        //mNative.SetFrameSize(getIntent().getIntExtra("FrameSize",230));
        mDestroy = false;
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                UpgradeThread upgradeThread = new UpgradeThread(mHandler, mstrFilePath);
                upgradeThread.start();
            }
        },300);
        mSimpleDateFormat = new SimpleDateFormat("mm:ss");
    }


    private static final int CMD_JNI_Upgrade = 0xff02;
    private class MSG_TYPE
    {
        public static final int SHOW           = 0x00;
        public static final int SEND_DATA      = 0x01;
        public static final int RCV_DATA       = 0x02;
        public static final int SHOW_PERCENT   = 0x04;
        public static final int UPDATE_UI      = 0x05;
        
        public MSG_TYPE() {
        }
    }
    @Override
    public boolean handleMessage(Message msg) {
        // TODO Auto-generated method stub
        switch (msg.what)
        {
        case CMD_JNI_Upgrade:
            switch (msg.arg1)
            {
            case MSG_TYPE.SHOW:
                mDumpTextView.append("MSG:" + (String)msg.obj + "\n\n");
                break;
             case MSG_TYPE.UPDATE_UI:
                 hasClick = true;
                 findViewById(R.id.upgrade_exit).setVisibility(View.VISIBLE);
                 break;
            case MSG_TYPE.SHOW_PERCENT:
                String content = mDumpTextView.getText().toString();
                //Log.i(TAG, "SHOW_PERCENT ["+content+"]");
                String upgradingMsg = getString(R.string.upgrading_msg);
                if (content != null && content.contains(upgradingMsg) && content.endsWith(" \n\n")){
                    int index = content.lastIndexOf(upgradingMsg);
                    if (index >0){
                        content = content.substring(0,index);
                        mDumpTextView.setText(content + upgradingMsg+"\n " + (String)msg.obj);
                    }
                }else {
                    mDumpTextView.append(upgradingMsg+"\n " + (String)msg.obj);
                }

                break;
            case MSG_TYPE.SEND_DATA:
                byte[] senddata = (byte[]) msg.obj;
                mDumpTextView.append("SEND:" + senddata.length + "\n\n");
                break;
            case MSG_TYPE.RCV_DATA:
                mDumpTextView.append("RCV:");
                updateReceivedData((String) msg.obj);
                break;
            }

            mScrollView.smoothScrollTo(0, 2400);
            break;
            case 101:
                hasClick =false;
                break;
        }
        return false;
    }

    private String mRevData;
    IScanConfigCallback.Stub mRevDataCallback = new IScanConfigCallback.Stub() {
        @Override
        public void onConfigCallback(String str) throws RemoteException {
            Log.d(TAG, " Receive Data: ["+ str+"]");
            mRevData = str;
        }
    };

    class UpgradeThread extends Thread {
        private final Handler mhandler;
        private String path;

        public UpgradeThread(Handler handler, String filename) {
            mhandler = handler;
            path = filename;
        }

        private void ShowMsg(int nMsg, Object strObj) {
            Message msg = new Message();
            msg.what = CMD_JNI_Upgrade;
            msg.arg1 = nMsg;
            msg.obj = strObj;
            mhandler.sendMessage(msg);
        }

        private int SendDataAndReceive(byte[] sendbuf, String hopeRcvData, String hopeRcvData1) {
            mRevData = null;//reset
            try {
                //ShowMsg(MSG_TYPE.SHOW, "to write: ["+HexDump.dumpHexString(sendbuf)+"]");
                Log.i(TAG, " write: [" + (sendbuf.length < 50 ? HexDump.dumpHexString(sendbuf) : sendbuf.length) + "]");
                sPort.write(sendbuf, 50, mRevDataCallback);
            } catch (Exception ex) {
                Log.i(TAG, " write: Exception[" + (sendbuf.length < 50 ? HexDump.dumpHexString(sendbuf) : sendbuf.length) + "]");
                ShowMsg(MSG_TYPE.SHOW, "write Data Exception " + HexDump.toHexString(sendbuf));
                return -2;
            }
            int stat = readData(hopeRcvData);
            if (stat == 0) {
                mRevData = null;
                sPort.addReadCallback(mRevDataCallback);
                stat = readData(hopeRcvData1);
            }
            return stat;
        }

        private int readData(String hopeRcvData) {
            return readData(hopeRcvData, 1000);//默认超时
        }

        private int readData(String hopeRcvData, int timeout) {
            if (hopeRcvData.equals("NOACK")){
                Log.i(TAG, " readbuf noack  return direct");
                return 0;
            }

            String readbuf = GetRevData(timeout);

            if (null != readbuf) {
                //Log.i(TAG, " readbuf : [" + readbuf + "]");
                if (readbuf.equals(hopeRcvData)) {
                    return 0;
                } else {
                    ShowMsg(MSG_TYPE.RCV_DATA, readbuf);
                }
            }
            //Log.i(TAG, " readbuf : [" + (readbuf != null) + "]");

            return -4;
        }


        private int SendDataAndReceive(byte[] sendbuf, String hopeRcvData) {

            mRevData = null;//reset
            try {
                //ShowMsg(MSG_TYPE.SHOW, "to write: ["+HexDump.dumpHexString(sendbuf)+"]");
                Log.i(TAG, " to write: [" + (sendbuf.length < 50 ? HexDump.dumpHexString(sendbuf) : sendbuf.length) + "]");
                sPort.write(sendbuf, 50, mRevDataCallback);
            } catch (Exception ex) {
                Log.i(TAG, " to write: Exception[" + (sendbuf.length < 50 ? HexDump.dumpHexString(sendbuf) : sendbuf.length) + "]");
                ShowMsg(MSG_TYPE.SHOW, "write Data Exception " + HexDump.toHexString(sendbuf));
                return -2;
            }
            return readData(hopeRcvData, 2000);//default 2000 ms
        }
        //是否升级成功
        boolean upgradeSucceed = false;

        @Override
        public void run() {
            String[] strMsgList = {
                    getResources().getString(R.string.Import_file_succeed),
                    "OutOfMemoryError or file path is null",
                    "file open false or file length is 0",
                    "File length is not enough",
                    "File is not bin file",
                    "File crc check failed",
                    "Can't find upgrade information",
            };
            //----------------Step 1: Import Bin File ----------------
            int nRet = mNative.ImportBinFile(path);
            int nShowIdx = Math.abs(nRet);
            if (nShowIdx >= strMsgList.length) {
                ShowMsg(MSG_TYPE.SHOW, "unknow return: " + nRet);
            } else {
                ShowMsg(MSG_TYPE.SHOW, strMsgList[nShowIdx]);
            }
            if (0 != nRet) {
                return;
            }

            //----------------Step 2: Judge whether the BIN file is EM2037 ----------------
            String strDevNameFromImportFile = mNative.GetDevNameFromImportFile();
            if (null == strDevNameFromImportFile) {
                ShowMsg(MSG_TYPE.SHOW, "Dev Name From Import File is null");
                return;
            }

            //if (!strDevNameFromImportFile.equals("EM2037"))
            if (strDevNameFromImportFile.equals("Badge") || strDevNameFromImportFile.equals("BADGE")) {
                //ShowMsg(MSG_TYPE.SHOW, "Dev Name From Import File is:" + strDevNameFromImportFile);
            } else {
                ShowMsg(MSG_TYPE.SHOW, "Dev Name From Import File is other:" + strDevNameFromImportFile);
                return;
            }

            //----------------Step 3.0:  sure device is can work ----------------
            int nSendDataRet = 0;
            byte[] devCheckbuf = {'?'};
            String devCheckRet = "21"; // !
            int nTryTtime = 2;
            do {
                nSendDataRet = SendDataAndReceive(devCheckbuf, devCheckRet);
            } while ((nSendDataRet != 0) && (--nTryTtime) > 0);

            if (0 != nSendDataRet) {
                //dev is currently unable to do upgrade task !!!
                ShowMsg(MSG_TYPE.SHOW, "当前无法进行升级，请尝试重新连接工牌！ " );
                return;
            }

            //----------------Step 3: Setup device in upgrade mode ----------------
            //                  .....upGrade..
            byte[] upgradebuf = {0x7e, 0x00, 0x00, 0x09, 0x7e, 0x75, 0x70, 0x47, 0x72, 0x61, 0x64, 0x65, 0x7e, (byte) 0xa6};
            String upgradeRet = "06";// 06

            nTryTtime = 2;
            do {
                nSendDataRet = SendDataAndReceive(upgradebuf, upgradeRet);
            } while ((nSendDataRet != 0) && (--nTryTtime) > 0);

            if (0 != nSendDataRet) {
                ShowMsg(MSG_TYPE.SHOW, "upgrade err:" + nSendDataRet);
                return;
            }

            //----------------Step 4: upgrade 4 Type Pack(EM2037 only include boot and kernel) ----------------
            int[] nUpgradeTypeList = {
                    Native.UPGRADE_TYPE_BOOT,
                    Native.UPGRADE_TYPE_KERNEL,
                    Native.UPGRADE_TYPE_FLASH,
                    Native.UPGRADE_TYPE_APPL
            };
            String[] nUpgradeTypeListStr = {
                    "BOOT",
                    "KERNEL",
                    "FLASH",
                    "APPLICATION"
            };

            String[] nUpgradeTypeListMsg = {
                    "=== 开始升级 BOOT ===",
                    "=== 开始升级 KERNEL ===",
                    "=== 开始升级 FLASH ===",
                    "=== 开始升级 APPLICATION ==="
            };
            String[] nUpgradeTypeCompleteListMsg = {
                    "=== 升级 BOOT 成功 ===",
                    "=== 升级 KERNEL 成功 ===",
                    "=== 升级 FLASH 成功 ===",
                    "=== 升级 APPLICATION 成功 ==="
            };

            Boolean bFirstUpgradeType = true;
            Boolean bNeedBreak = false;
            for (int iUpgradeType = 0; iUpgradeType < 4; iUpgradeType++) {
                Integer pImportFileStatus = new Integer(0);
                Integer pFrameSize = new Integer(0);
                Integer pCmdPackCnt = new Integer(0);
                Integer pDataPackCnt = new Integer(0);
                nRet = mNative.GetParam(nUpgradeTypeList[iUpgradeType], pImportFileStatus, pFrameSize, pCmdPackCnt, pDataPackCnt);
                if (0 != nRet) {
                    if (-2 == nRet) {
                        //ShowMsg(MSG_TYPE.SHOW, "GetParam false, have no Data");
                    } else {
                        ShowMsg(MSG_TYPE.SHOW, "GetParam false :" + nRet);
                    }
                    if (iUpgradeType == 3){
                        ShowMsg(MSG_TYPE.SHOW, "所有升级包已检查完成，等待退出升级");
                    }
                    continue;
                }
                if (0 != pImportFileStatus) {
                    nShowIdx = Math.abs(nRet);
                    if (nShowIdx >= strMsgList.length) {
                        ShowMsg(MSG_TYPE.SHOW, "ERROR: " + nRet);
                    } else {
                        ShowMsg(MSG_TYPE.SHOW, strMsgList[nShowIdx]);
                    }
                    break;
                }
 //               ShowMsg(MSG_TYPE.SHOW, "FrameSize:" + pFrameSize);
 //               ShowMsg(MSG_TYPE.SHOW, "CmdPackCnt:" + pCmdPackCnt);
 //               ShowMsg(MSG_TYPE.SHOW, "DataPackCnt:" + pDataPackCnt);

                if ((0 == pFrameSize) &&
                        (0 == pCmdPackCnt) &&
                        (0 == pDataPackCnt)) {
                    continue;
                }

                if (!bFirstUpgradeType) {
                    //----------------Step 4.4:  if have next pack, need send "NextDown" cmd  ----------------
                    byte[] nextDownbuf = {0x02, 0x05, 0x00, 0x09, 0x40, 0x4e, 0x65, 0x78, 0x74, 0x44, 0x6f, 0x77, 0x6e, 0x46, 0x3f, 0x36, 0x33};
                    //byte[] nextDown = {0x02,0x05,0x00,0x02,0x30,0x00,(byte)0xE8,0x28,0x6D,0x45};
                    String nextDown = "0205000130B4FE55A2";
                    nSendDataRet = SendDataAndReceive(nextDownbuf, nextDown);
                    if (0 != nSendDataRet) {
                        ShowMsg(MSG_TYPE.SHOW, "NextDown err:" + nSendDataRet);
                    } else {
                        //ShowMsg(MSG_TYPE.SHOW, "NextDown Upgrade Type: " + nUpgradeTypeListStr[iUpgradeType]);
                    }
                }
                bFirstUpgradeType = false;

                //----------------Step 4.1:  in upgrade mode ----------------
                byte[] checkbuf = {'?', '?', '?'};
                String checkRet = "3C"; //  <
                nTryTtime = 10;
                do {
                    nSendDataRet = SendDataAndReceive(checkbuf, checkRet);
                }
                while (nSendDataRet != 0 && nSendDataRet != -2 && nSendDataRet != -4 && (--nTryTtime) > 0);//-2 write exception

                if (0 != nSendDataRet) {
                    ShowMsg(MSG_TYPE.SHOW, "devCheck err:" + nSendDataRet);
                    break;// modified by cms
                }


                upgradeSucceed = false;

                ShowMsg(MSG_TYPE.SHOW, nUpgradeTypeListMsg[iUpgradeType]);

                //findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
                //----------------Step 4.2:  Sending cmd (include infomation) ----------------
                bNeedBreak = false;
                for (int iCmdIdx = 0; iCmdIdx < pCmdPackCnt; iCmdIdx++) {
                    Log.d(TAG, "getPackCmd=" + iCmdIdx);
                    byte[] sendbuf = mNative.GetPackCmd(nUpgradeTypeList[iUpgradeType], iCmdIdx);
                    if (null == sendbuf) {
                        ShowMsg(MSG_TYPE.SHOW, "GetPackCmd is null");
                        bNeedBreak = true;
                        break;
                    }
                    String hopeRcvData = "0205000130B4FE55A2";
                    nTryTtime = 10;
                    do {
                        nSendDataRet = SendDataAndReceive(sendbuf, hopeRcvData);
                    } while ((nSendDataRet != 0) && (--nTryTtime) > 0);
                    if (nSendDataRet != 0) {
                        ShowMsg(MSG_TYPE.SHOW, "SendDataAndReceive err:" + nSendDataRet);
                        bNeedBreak = true;
                        break;
                    }
                }
                if (bNeedBreak) {
                    break;
                }

                //----------------Step 4.2:  Sending Pack data ----------------
                bNeedBreak = false;
                int nPercentage = -1;
                long startCurrentUpgradePackTime = 0;
                long startUpgradePackTime = 0;
                long singlePackDuration = 120;
                for (int iDataIdx = 0; iDataIdx < pDataPackCnt; iDataIdx++) {
                    if (mDestroy){
                        ShowMsg(MSG_TYPE.SHOW, "Upgrade Drop out !!!  , Now To exit ");
                        ExitUpgradeTask();
                        return;
                    }
                    if (iDataIdx == 0) {
                        startUpgradePackTime = System.currentTimeMillis();
                    }
                    if (iDataIdx == 0|| iDataIdx%10 == 0){
                        startCurrentUpgradePackTime = System.currentTimeMillis();
                    }
                    byte[] sendbuf = mNative.GetPackData(nUpgradeTypeList[iUpgradeType], iDataIdx);
                    Log.d(TAG, " GetPackData index = " + (iDataIdx + 1) + "/" + pDataPackCnt + " sendbuf: " + sendbuf + " len: " + (sendbuf != null ? sendbuf.length : 0));
                    if (null == sendbuf) {
                        ShowMsg(MSG_TYPE.SHOW, "GetPackData is null");
                        bNeedBreak = true;
                        break;
                    }

                    String hopeRcvData = "2A";
                    String hopeRcvDataLastPack = "2A";
                    if ((iDataIdx+1)%4 != 0 && iDataIdx != (pDataPackCnt - 1)){
                        //hopeRcvData = "NOACK";
                        //Log.d(TAG, " GetPackData index = " + (iDataIdx + 1)+"  NO hope ACK ");
                    }else {
                        //Log.d(TAG, " GetPackData index = " + (iDataIdx + 1)+"  hope Rcv ============= "+hopeRcvData);
                    }
                    nTryTtime = 10;
                    do {
                        if (iDataIdx == (pDataPackCnt - 1)) {
                            Log.d(TAG, "SendDataAndReceive last packet ");
                            nSendDataRet = SendDataAndReceive(sendbuf, hopeRcvDataLastPack, hopeRcvDataLastPack);
                        } else {
                            nSendDataRet = SendDataAndReceive(sendbuf, hopeRcvData);
                        }
                    } while ((nSendDataRet != 0) && (--nTryTtime) > 0);

                    if (nSendDataRet != 0) {
                        ShowMsg(MSG_TYPE.SHOW, "SendDataAndReceive err:" + nSendDataRet);
                        bNeedBreak = true;
                        break;
                    }

                    if (iDataIdx == 0 || iDataIdx%100 == 0){
                        singlePackDuration = System.currentTimeMillis() - startCurrentUpgradePackTime;
                    }
                    //update progress
                    nPercentage = (iDataIdx + 1) * 100 / pDataPackCnt;
                    ShowMsg(MSG_TYPE.SHOW_PERCENT, " " + (iDataIdx + 1) + " / " + pDataPackCnt + " (" + nPercentage + "%)     "+
                            mSimpleDateFormat.format(new Date(System.currentTimeMillis()-startUpgradePackTime))+" / "+
                            mSimpleDateFormat.format(new Date(singlePackDuration*(pDataPackCnt-iDataIdx-1)))+" \n\n");

                    if (iDataIdx == (pDataPackCnt - 1)) {
                        nPercentage = 100;
                        //ShowMsg(MSG_TYPE.SHOW, "All upgrade packages have been sent!");
                    }
                   // Log.i("TAG","upgrade current pack==use time =========== "+(System.currentTimeMillis()-startUpgradePackTime1));
                }
                if (bNeedBreak) {
                    break;
                }

                if (100 != nPercentage) {
                    break;
                }
                ShowMsg(MSG_TYPE.SHOW, getString(R.string.wait_for_complete));

                //----------------Step 4.3:  Wait Update to complete  ----------------
                try {
                    mRevData = null;
                    sPort.addReadCallback(mRevDataCallback);
                    String readbuf = GetRevData(pDataPackCnt * 10 + 100000);// wait 3 min

                    if (null != readbuf) {
                        if ("5E".equals(readbuf))// ^
                        {
                            upgradeSucceed = true;
                            //ShowMsg(MSG_TYPE.SHOW, "Upgrade completed, Check next Pack");
                            ShowMsg(MSG_TYPE.SHOW, nUpgradeTypeCompleteListMsg[iUpgradeType]);
                            //ShowMsg(MSG_TYPE.SHOW, getString(R.string.upgrade_success));
                            //ShowMsg(MSG_TYPE.SHOW, getString(R.string.check_nextpack));
                        } else if ("21".equals(readbuf)) // !
                        {
                            ShowMsg(MSG_TYPE.SHOW, getString(R.string.upgrade_fail));
                            break;
                        } else {
                            ShowMsg(MSG_TYPE.SHOW, "UPGRADE OTHER ERROR!!!");
                            break;
                        }
                    }else {
                        ShowMsg(MSG_TYPE.SHOW, "Wait TimeOut !!!  ");
                        break;
                    }
                } catch (Exception ex) {
                    ShowMsg(MSG_TYPE.SHOW, "read Data Exception");
                    break;
                }
            }

            if (nSendDataRet == -2) {
                ShowMsg(MSG_TYPE.SHOW, "write exception , direct exit  :" + nSendDataRet);
                return;
            }

            //----------------Step 5: upgrade completion, and exit ----------------
            ExitUpgradeTask();
        }

        private void ExitUpgradeTask(){
            //findViewById(R.id.progressBar).setVisibility(View.GONE);

            byte[] exitbuf = {0x02, 0x05, 0x00, 0x05, 0x40, 0x45, 0x78, 0x69, 0x74, (byte) 0x9d, 0x5e, 0x0f, (byte) 0x9a};// ....@Exit.^..
            String exitRet = "0205000130B4FE55A2";   //  ....0..U.
            int nSendDataRet = SendDataAndReceive(exitbuf, exitRet);
            if (0 != nSendDataRet) {
                ShowMsg(MSG_TYPE.SHOW, "exit err: " + nSendDataRet);
            } else {
                ShowMsg(MSG_TYPE.SHOW, "已成功退出升级");
                if (upgradeSucceed){
                    ShowMsg(MSG_TYPE.SHOW, getString(R.string.upgrade_success));
                    ShowMsg(MSG_TYPE.UPDATE_UI, "");
                }
            }
        }
    }

    private void updateReceivedData(String data) {
        final String message = "Read " + data + "\n\n";
        mDumpTextView.append(message);
        mScrollView.smoothScrollTo(0, 2400);
    }

    /**
     * Starts the activity, using the supplied driver instance.
     *
     * @param context
     */
    static void show(Context context, BleSerialPort port, String strFilePath) {
        sPort = port;
        mstrFilePath = strFilePath;
        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        //intent.putExtra("FrameSize",frameSize>128 ?frameSize:128);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, " onDestroy");
        mDestroy = true;
    }
    boolean mDestroy = false;

    private boolean hasClick = false;
    @Override
    public void onBackPressed() {
        if (!hasClick){
            Toast toast = Toast.makeText(this, "在按一次退出升级", Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER,0,0);
            toast.show();
            mHandler.sendEmptyMessageDelayed(101,1000);
            hasClick = true;
        }else {
            mHandler.removeMessages(101);
            super.onBackPressed();
        }
    }
}
