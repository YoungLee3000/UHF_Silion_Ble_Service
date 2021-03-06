package com.nlscan.uhf.silionBle;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.Vibrator;
import android.util.Log;
import android.view.WindowManager;

import com.nlscan.android.uhf.IUHFTagInventoryListener;
import com.nlscan.android.uhf.TagInfo;
import com.nlscan.android.uhf.UHFCommonParams;
import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFModuleInfo;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.blecommservice.IBleInterface;
import com.nlscan.uhf.silion.ISilionUHFService;
import com.pow.api.cls.RfidPower.PDATYPE;
import com.uhf.api.cls.Reader;
import com.uhf.api.cls.Reader.HardwareDetails;
import com.uhf.api.cls.Reader.Mtr_Param;
import com.uhf.api.cls.Reader.READER_ERR;
import com.uhf.api.cls.Reader.TAGINFO;

public class UHFSilionService extends Service {


	private final static String TAG = "UHFSilionBleService";
	private final static String EXTRA_IMU = "extra_imu_data";

	
	private Context mContext;
	private IBinder mBinder;
//	private RfidPower mRfidPower;
	private BleReader mReader;
	private boolean mPowerOn = false;
	/**
	 * 屏幕灭屏前,是否处在上电状态
	 */
	private boolean mPowerOnBeforeScreenOff = false;
	
	private boolean mScreenOn = true;
	
	private SoundPool soundPool;
	private Vibrator mVibrator;
	
	private enum ReadingState{
		IDLE,
		READING,
	}
	
	private ReadingState mReadingState = ReadingState.IDLE;
	private boolean mForceStoped = true;
	
	private UHFManager mUHFMgr ; 
	private Map<String,Object> mSettingsMap; 
	private UHFSilionSettingService mSettingsService;
	
	private PDATYPE PT = PDATYPE.valueOf(0);
	
	/**UHF模块型号*/
	private String mUHFDeviceModel ;
	private UHFModuleInfo mUHFModuleInfo;
	
	private OperateHandler mOperHandler;
	private HandlerThread mReadingHandlerThread;
	
	private SenderHandler mSenderHandler;
	private HandlerThread mSenderHandlerThread;
	
	final RemoteCallbackList<IUHFTagInventoryListener> mClientCallbackList = new RemoteCallbackList<IUHFTagInventoryListener>();
	
	/**开始读取的时间*/
	private long mStartReadTime = 0;


	/**电量监控范围**/
	private boolean mBatteryMonitorOn = false;//是否开启电量监控
	private int mBatteryWarn1 = 20;//电量警戒线1
	private int mBatteryWarn2 = 15;//电量警戒线2
	private int mCurCharge = -1; //当前电量
	private boolean mPowerAllow = true; //是否允许上电

	private final static String BROAD_BATTERY_MONITOR = "com.nlscan.uhf.silion.action.BATTERY_MONITOR";//电量监控参数
	private final static String EXTRA_STRING_MONITOR = "if monitor";
	private final static String EXTRA_STRING_WARN_ONE = "warn value 1";
	private final static String EXTRA_STRING_WARN_TWO = "warn value 2";


	//设备信息
	private final static String DEFAULT_MODULE = "MODULE_SLRB1200";
	//蓝牙相关
	private  IBleInterface mBleInterface;
	private boolean mIfQuickReading = false;
	private BleServiceConnection mBleServiceConnection;
	BluetoothAdapter mBluetooth;


	@Override
	public void onCreate() {
		super.onCreate();
		
		mContext = getApplicationContext();
		mUHFMgr = UHFManager.getInstance();
		mSettingsService = new UHFSilionSettingService(mContext, mReader);
		mSettingsMap = mSettingsService.getAllSettings();
		mBluetooth =  BluetoothAdapter.getDefaultAdapter();

		bindBleService();





//		mRfidPower = new RfidPower(PT);
		

		
		mReadingHandlerThread = new HandlerThread("ReadingHandlerThread",android.os.Process.THREAD_PRIORITY_FOREGROUND);
		mReadingHandlerThread.start();
		mOperHandler = new OperateHandler(mReadingHandlerThread.getLooper());
		
		mSenderHandlerThread = new HandlerThread("SenderHandlerThread",android.os.Process.THREAD_PRIORITY_FOREGROUND);
		mSenderHandlerThread.start();
		mSenderHandler = new SenderHandler(mReadingHandlerThread.getLooper());
		
		
		soundPool = new SoundPool(10, AudioManager.STREAM_RING, 5);
		soundPool.load(this, R.raw.beep51, 1);
		mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
		
		mUHFModuleInfo = getModuleInfo(getPackageName());
				
		if(mBinder == null)
			mBinder = new MyBinder();

		registerReceiver();
		
		Log.d(TAG, "Silion UHF Service onCreated.");
	}


	/**
	 * 绑定BLE蓝牙服务
	 */
	private void bindBleService(){
		Log.d(TAG,"begin bind ble service");

//		boolean ifConnect = false;
//		try {
//			if (mBleInterface!=null)
//				ifConnect = mBleInterface.isBleAccess();
//			Log.d(TAG,"blue is access 222 " + ifConnect);
//		} catch (RemoteException e) {
//			e.printStackTrace();
//		}
//		if (ifConnect) return;

		Intent service = new Intent("android.nlscan.intent.action.START_BLE_SERVICE");
		service.setPackage("com.nlscan.blecommservice");
		mBleServiceConnection = new BleServiceConnection();
		mContext.bindService(service,mBleServiceConnection, Context.BIND_AUTO_CREATE);
	}


	private boolean mConnectState = false;
	/**
	 * 绑定服务状态
	 */
	private class  BleServiceConnection implements ServiceConnection {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBleInterface = IBleInterface.Stub.asInterface(service);

			mReader = new BleReader(mBleInterface);
			mSettingsService.setReader(mReader);

//			mSettingsService = new UHFSilionSettingService(mContext, mReader);
//			mSettingsMap = mSettingsService.getAllSettings();
			Log.d(TAG, "onServiceConnected");
			mConnectState = true;
            mSenderHandler.removeMessages(SenderHandler.MSG_CHECK_CONN_ALIVE);

		}
		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBleInterface = null;
			mConnectState = false;

            //启动服务连接状态监控,如果断开,自动重连
            mSenderHandler.removeMessages(SenderHandler.MSG_CHECK_CONN_ALIVE);
            mSenderHandler.sendEmptyMessage(SenderHandler.MSG_CHECK_CONN_ALIVE);


		}
	}




	@Override
	public void onDestroy() {
		super.onDestroy();
		mBleInterface = null;
		unRegisterReceiver();
		mContext.unbindService(mBleServiceConnection);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}


	@Override
	public IBinder onBind(Intent intent) {
		
		if(mBinder == null)
			mBinder = new MyBinder();
		
		return mBinder;
	}
	
	//============================================================================
	
	private boolean powerDriver(boolean powerOn)
	{
		Log.d(TAG, "Enter Power driver, : "+powerOn);
		try {
        	//上电
            FileWriter fw58 = new FileWriter(mUHFModuleInfo.power_driver);//写文件
            fw58.write(powerOn?"1":"0");
            fw58.close();
            Log.d(TAG, "Power driver, Power state: "+(powerOn?"POWER ON.":"POWER DOWN."));
            Thread.sleep(10);
           } catch (Exception e) {
        	   Log.w(TAG, "Power driver, write power on data error.",e);
        	   return false;
           }
		
		try {
            ///sys/bus/platform/devices/sys_switch.0/nls_serial 文件。写入 "txrx=gpio" 表示设置为GPIO脚。写入 "txrx=serial"表示设置为串口功能脚
            FileWriter fw58 = new FileWriter("/sys/bus/platform/devices/sys_switch.0/nls_serial");
            if(powerOn){
            	fw58.write("txrx=serial");
            	Log.d(TAG, "Power driver, nls_serial : "+(powerOn?"txrx=serial":"txrx=gpio"));
            }
            //fw58.write(powerOn?"txrx=serial":"txrx=gpio");
            fw58.close();
            
            Thread.sleep(50);
           } catch (Exception e) {
        	   Log.w(TAG, "Power driver, write gpio to serial error.",e);
           }
		
		return true;
	}
	
	private int doPowerRetryCount = 0;
	
	/**
	 * UHF模块上电
	 * @param lastState 上一次的上电状态
	 * @return
	 */
	private UHFReader.READER_STATE doPowerOn(UHFReader.READER_STATE lastState)
	{

//		if (!mPowerAllow){
//			new Thread(){
//				@Override
//				public  void run(){
//					Looper.prepare();
//					showBatteryDialog3();
//					Looper.loop();
//				}
//			}.start();
//
//			Log.d("BatteryMonitor","not allow power on");
//			return UHFReader.READER_STATE.CMD_FAILED_ERR;
//		}

//		if(doPowerRetryCount == 0)
//			sendUHFState(UHFManager.UHF_STATE_POWER_ONING);
		
		//驱动上电
//		boolean powOn = powerDriver(true);
//		if(!powOn)
//			return  UHFReader.READER_STATE.HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR;



//		mReader.setmBleInterface(mBleInterface);

//		try {
//			if (mBleInterface == null || !mBleInterface.isBleAccess()){
//				bindBleService();
//				return UHFReader.READER_STATE.CMD_FAILED_ERR;
//			}
//		} catch (RemoteException e) {
//			e.printStackTrace();
//		}

		UHFReader.READER_STATE state =   initReader();
//		UHFReader.READER_STATE state = UHFReader.READER_STATE.OK_ERR;
		if(state == UHFReader.READER_STATE.OK_ERR)
			state = doInitReaderParams();//初始化读写器参数
		mPowerOn = (state == UHFReader.READER_STATE.OK_ERR);
//		if(state != UHFReader.READER_STATE.OK_ERR){ //初始化读写器失败,下电以备下一次的重新上电
//			powerDriver(false);//驱动下电
//		}

//		sendUHFState(mPowerOn?UHFManager.UHF_STATE_POWER_ON : UHFManager.UHF_STATE_POWER_OFF);
        return state;
	}
	
	/**
	 * 连接读写器(初始创建读写器)
	 * @return
	 */
	private UHFReader.READER_STATE initReader()
	{
		Log.d(TAG, "Start connect to device...");

		if (mBleInterface == null ){
			bindBleService();
			return UHFReader.READER_STATE.CMD_FAILED_ERR;
		}

		boolean ifAccess = false;
		try {
			ifAccess = mBleInterface.isBleAccess();
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		if (!ifAccess) return UHFReader.READER_STATE.CMD_FAILED_ERR;


		//初始化读写器
		int ant = 1;
		 READER_ERR er = READER_ERR.MT_CMD_FAILED_ERR;
		try{
			er=mReader.InitReader_Notype(mUHFModuleInfo.serial_path, ant);
			Log.d(TAG, "Connect to device state : "+er+","+(er == READER_ERR.MT_OK_ERR));


//			if (er == READER_ERR.MT_CMD_FAILED_ERR){

//				Thread.sleep(2000);
//
//				READER_ERR er2 = mReader.InitReader_Notype(mUHFModuleInfo.serial_path, ant);
//				Log.d(TAG, "the second Connect to device state : "+er2+","+(er2 == READER_ERR.MT_OK_ERR));
//
//				if (er2 == READER_ERR.MT_CMD_FAILED_ERR){
//					try {
//						if (mBleServiceConnection !=null)
//							mContext.unbindService(mBleServiceConnection);
//					} catch (Exception e) {
//						e.printStackTrace();
//					}
//					bindBleService();
//				}
//				else {
//					return UHFReader.READER_STATE.valueOf(er2.value());
//				}

//
//			}
//			else{
				return UHFReader.READER_STATE.valueOf(er.value());
//			}
		}catch(Exception e){
			Log.w(TAG, "Connect to reader faild.",e);

		}
		
		return UHFReader.READER_STATE.CMD_FAILED_ERR;
//		return UHFReader.READER_STATE.OK_ERR;
	}
	
	/**
	 * 初始化读写器参数
	 * @return
	 */
	private UHFReader.READER_STATE doInitReaderParams()
	{
		Log.d(TAG, "Do init reader params...");
		try {
			
			//执行模块参数配置
        	mSettingsService.effectParams(mReader);
//        	mSettingsMap = mSettingsService.getAllSettings();
        	READER_ERR er ;
//        	er = mReader.ParamSet(Mtr_Param.MTR_PARAM_TAG_SEARCH_MODE, new int[] { 0 });
        	
//			HardwareDetails val = mReader.new HardwareDetails();
//			er = mReader.GetHardwareDetails(val);//获取硬件信息
			er = mBleInterface.isBleAccess() ?  READER_ERR.MT_OK_ERR : READER_ERR.MT_CMD_FAILED_ERR;
			if(er == READER_ERR.MT_OK_ERR)
			{
				//UHF模块型号
				mUHFDeviceModel = DEFAULT_MODULE;
				
//				Log.d(TAG, "Module_Type : "+val.module.toString());
//				Log.d(TAG, "MaindBoard_Type : "+val.board.toString());
//				Log.d(TAG, "Reader_Type : "+val.logictype.toString());
			}
			
			return UHFReader.READER_STATE.valueOf(er.value()); 
			
		} catch (Exception ex) {
			Log.d(TAG, "Init uhf reader params failed.",ex);
		}
		
		return UHFReader.READER_STATE.CMD_FAILED_ERR;
		
	}//doInitReadParams
	
	/**
	 * 断开设备(并下电)
	 * @return
	 */
	private UHFReader.READER_STATE doPowerOff()
	{
		if(!mPowerOn)
			return UHFReader.READER_STATE.OK_ERR;

		if (mBleInterface == null) return UHFReader.READER_STATE.CMD_FAILED_ERR;

		try {
			if (mBleInterface !=null && !mBleInterface.isBleAccess())
				return UHFReader.READER_STATE.CMD_FAILED_ERR;
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		//停止盘点
		doStopReading();
		
		synchronized (this) {
			
			if (mReader != null)
				mReader.CloseReader();

			boolean oldPowerOn = mPowerOn;
			boolean blen = true;
			try {
				//驱动下电
//				powerDriver(false);
//				Log.d(TAG, "UHF disconnect complete,state : "+blen);
				UHFReader.READER_STATE state =   mReader.doPowerOff() ;
				mPowerOn = false;

//				mContext.unbindService(mBleServiceConnection);
				Log.d(TAG, "UHF pown off , state : "+state.toString());
				return state;
			} catch (Exception e) {
				Log.d(TAG, "Do disconnect failed.", e);
			} finally{
				if(oldPowerOn && !mPowerOn)
					sendUHFState(UHFManager.UHF_STATE_POWER_OFF);
			}
			
			return UHFReader.READER_STATE.CMD_FAILED_ERR;
		}
		
		
	}//doPowerOff
	
	private void nonStopStartReading()
	{
		READER_ERR er;
		int[] uants = getAnts();
		boolean quickMode = isQuickMode();
		if (quickMode) {
		    er = mReader.StartReadingCommon();
//			er = mReader.AsyncStartReading(uants, uants.length, 0);
			if (er != READER_ERR.MT_OK_ERR) {
				DLog.w(TAG, "不停顿盘点启动失败 : "+er.toString());
				return;
			}else
				mOperHandler.sendEmptyMessage(OperateHandler.MSG_START_READING);
		}
	}

	private int mReadCount = 0;
	
	private void doStartReading()
	{
		//Log.d(TAG, "Enter doStartReading..");
		long pre = System.currentTimeMillis();
//		if(mForceStoped)
//		{
//			Log.d(TAG, "Reading is stoped.");
//			mReadingState = ReadingState.IDLE;
//			return ;
//		}
		mReadCount++;
//		Log.d(TAG,"uhf change 2 ");
		try {
			if(mStartReadTime == 0)
				mStartReadTime = System.currentTimeMillis();
			
			READER_ERR er;
			TAGINFO[] tagInfos ;
//			String[] epcIds = null;
			int[] tagcnt = new int[1]; //本
			
			
			boolean quickMode = isQuickMode();
//			Log.d(TAG,"if quick mode " + quickMode);
//			int[] uants = getAnts();
//			long readTimeout = mSettingsService.getLongParamValue(UHFSilionParams.INV_TIME_OUT.KEY, UHFSilionParams.INV_TIME_OUT.PARAM_INV_TIME_OUT,
//                    UHFSilionParams.INV_TIME_OUT.DEFAULT_INV_TIMEOUT);
//
//			Log.d(TAG,"the read time out is " + readTimeout);
			
//			if (quickMode) {
////
//				er =mReader.AsyncGetTagCount(tagcnt);
//			} else {
//
//				er = mReader.TagInventory_Raw(uants,uants.length, (short) readTimeout, tagcnt);
//
//			}

			er = mReader.getInvTagCount(tagcnt);
			
			if (er == READER_ERR.MT_OK_ERR) 
			{
				mReadingState = ReadingState.READING;
				
				if (tagcnt[0] > 0) 
				{
					int tagCount = tagcnt[0];
					Log.d(TAG,"the tag count is " + tagCount);
//					epcIds = new String[tagCount];
					tagInfos = new TAGINFO[tagCount];

					
					Log.d(TAG, "============================================================");
					for (int i = 0; i < tagCount; i++)
					{
						TAGINFO tfs = mReader.new TAGINFO();


						if (quickMode)
							er = mReader.AsyncGetNextTag(tfs);
						else
							er = mReader.GetNextTag(tfs);

//						if (er == READER_ERR.MT_OK_ERR)
//							performPrompt();
						tagInfos[i] = tfs;
						Log.d(TAG,"the tfs is null " + (tfs == null));
//
					}//end for



					Log.d(TAG,"tag info size " + tagInfos.length);
//					if(tagInfos != null && tagInfos.length > 0)
//					{
						//成功提示
						performPrompt();
						//发送给客户
						sendResult(tagInfos);
//					}


				}
				
			}//else{
////				if ( !mBleInterface.isBleAccess()) doPowerOff();
////				Log.w(TAG, "Reading error : er = "+er.toString());
//
//			}//end if



			
		} catch (Exception e) {
			Log.w(TAG, "Start reading failed.", e);
		}




		String imuRel = mReader.getImuData();

        if (mReadCount % 4 == 1)
		    sendIMURel(imuRel);


//		Log.d(TAG,"the uhf parse cause " + (System.currentTimeMillis() - pre) + " ms");
		//进入下一扫描周期

//        long intevalTime = 10;
		long intevalTime =mSettingsService.getLongParamValue(UHFSilionParams.INV_INTERVAL.KEY, UHFSilionParams.INV_INTERVAL.PARAM_INV_INTERVAL_TIME,UHFSilionParams.INV_INTERVAL.DEFAULT_INV_INTERVAL_TIME);
//		Log.d(TAG,"inventory time " + intevalTime);
		mOperHandler.sendEmptyMessageDelayed(OperateHandler.MSG_START_READING, 0);
		
	}//end doStartReading



	private void sendIMURel(String imuData)
	{
		if(imuData != null && !"".equals(imuData))
		{
			Intent sendIntent  = new Intent(Constants.ACTION_IMU_RESULT_SEND);
			sendIntent.putExtra(EXTRA_IMU, imuData);
			sendBroadcast(sendIntent);
		}
	}
	
	/**
	 * 停止盘点
	 */
	private void doStopReading()
	{
		synchronized (this) {
			
			Log.d(TAG, "Enter doStopReading...");
			

			mReadCount =0;
			READER_ERR er = null;
			if (isQuickMode()) {
				Log.d(TAG, "stop---");
				er = mReader.StopReading();
				mReadingState = ReadingState.IDLE;
				if (er != READER_ERR.MT_OK_ERR) {
					Log.d(TAG, "不停顿盘点停止失败 : "+er.toString());
					return;
				}
				else {
					mStartReadTime = 0;
					mForceStoped = true;
//					mOperHandler.removeMessages(OperateHandler.MSG_START_READING);
					mIfQuickReading = false;
				}
			}else{
				mStartReadTime = 0;
				mForceStoped = true;
//				mOperHandler.removeMessages(OperateHandler.MSG_START_READING);
				er = mReader.StopReading();
				mReadingState = ReadingState.IDLE;
			}
			
			Log.d(TAG,"Do stop reading complete,state : "+er+","+(er == READER_ERR.MT_OK_ERR));
		}
	}
	
	private int[] getAnts()
	{
		int[] ants = (int[]) mSettingsMap.get(UHFSilionParams.ANTS.PARAM_ANTS_GROUP);
		if(ants == null || ants.length == 0)
		{
			ants = new int[1];
			ants[0] = 1;
		}
		
		return ants;
	}
	
	private int getOperateAnt()
	{
		int ant = (Integer)mSettingsMap.get(UHFSilionParams.ANTS.PARAM_OPERATE_ANTS);
		return ant;
	}
	
	/**
	 * 是否快速盘点模式
	 * @return
	 */
	private boolean isQuickMode()
	{
		Object obj = mSettingsMap.get(UHFSilionParams.INV_QUICK_MODE.KEY);
		int iQuick = 0;
		if(obj != null)
			iQuick = (Integer)obj;

		return iQuick == 1;
//        return true;
	}
	
	/**
	 * 发送盘点结果
	 * @param tags
	 */
	private void sendResult(TAGINFO[] tags)
	{
		if(tags == null || tags.length == 0)
			return ;
		Log.d(TAG,"the len of tag " + tags.length);

		int tagCount = 0;
		for (TAGINFO taginfo : tags){
			if (taginfo !=null) tagCount++;
		}
		Log.d(TAG,"the final tag count " +tagCount);
		TAGINFO[]  newTags = Arrays.copyOf(tags, tagCount);
		TagInfo[] nls_TagInfos = new TagInfo[newTags.length];
		for(int i = 0 ;i< tagCount;i++)
		{
			TAGINFO tag = newTags[i];
			if (tag == null) continue;
			try {
				tag.protocol = Reader.SL_TagProtocol.SL_TAG_PROTOCOL_GEN2;
				TagInfo.SL_TagProtocol nls_TagProtocol = TagInfo.SL_TagProtocol.valueOf(tag.protocol.value());
				
				TagInfo nls_Tag = new TagInfo(
																			tag.AntennaID,
																			tag.Frequency,
																			tag.TimeStamp,
																			tag.EmbededDatalen,
																			tag.EmbededData,
																			tag.Res,
																			tag.Epclen,
																			tag.PC,
																			tag.CRC,
																			tag.EpcId,
																			tag.Phase,
																			nls_TagProtocol,
																			tag.ReadCnt,
																			tag.RSSI);
				nls_TagInfos[i] = nls_Tag;
			} catch (Exception e) {
				Log.w(TAG, "Parse taginfo failed.",e);
			}
			
		}//end for
		
		//发送结果
		Message msg = Message.obtain(mSenderHandler, SenderHandler.MSG_SEND_RESULT, nls_TagInfos);
		mSenderHandler.sendMessageDelayed(msg, 0);
	}
	
	/**
	 * 盘点提示
	 */
	private void performPrompt()
	{
		boolean soundEnable = mUHFMgr.isPromptSoundEnable();
		boolean vibrateEnable = mUHFMgr.isPromptVibrateEnable();
		boolean ledEnable = mUHFMgr.isPromptLEDEnable();
		
		if(soundEnable){
			AudioManager am = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
			// 获取最大音量值
			float audioMaxVolumn = am.getStreamMaxVolume(AudioManager.STREAM_RING);
			// 不断获取当前的音量值
			float audioCurrentVolumn = am.getStreamVolume(AudioManager.STREAM_RING);
			//最终影响音量
			float volumnRatio = audioCurrentVolumn/audioMaxVolumn;
			soundPool.play(1, volumnRatio, volumnRatio, 0, 0, 1);
		}
		if(vibrateEnable)
			mVibrator.vibrate(80); // 震动
	}
	
	private void sendResultByBroadcast(TagInfo[] tagInfos)
	{
		if(tagInfos != null && tagInfos.length >0)
		{
			Intent sendIntent  = new Intent(Constants.ACTION_UHF_RESULT_SEND);
			sendIntent.putExtra(Constants.EXTRA_TAG_INFO, tagInfos);
			sendIntent.putExtra("extra_start_reading_time", mStartReadTime);
			sendBroadcast(sendIntent);
		}
	}
	
	private void sendResultByAPI(TagInfo[] tagInfos)
	{
		if(tagInfos != null && tagInfos.length >0)
		{
			if(mClientCallbackList != null)
			{
				int count = mClientCallbackList.beginBroadcast();
				for(int i =0 ;i<count; i++)
				{
					IUHFTagInventoryListener listener = mClientCallbackList.getBroadcastItem(i);
					try {
						listener.onReadingResult(tagInfos);
					} catch (Exception e) {
					}
				}
				mClientCallbackList.finishBroadcast();
			}
		}
	}
	
	private UHFModuleInfo getModuleInfo(String packageName)
	{
		if(packageName == null)
			return null;
		
		final String moduleConfigFilePath = "/system/usr/uhf/uhf_module_config.xml";
		List<UHFModuleInfo> infoList = UHFModuleInfo.parseUHFModuleInfo(moduleConfigFilePath);
		if(infoList != null)
		{
			for(UHFModuleInfo info : infoList)
			{
				if(packageName.equals(info.packageName))
					return info;
			}
		}
		return null;
	}
	
	private void registerReceiver()
	{
		IntentFilter inFilter = new IntentFilter();
		inFilter.addAction(Intent.ACTION_SCREEN_ON);
		inFilter.addAction(Intent.ACTION_SCREEN_OFF);
		try {
			mContext.registerReceiver(mScreenReceiver, inFilter);
		} catch (Exception e) {
		}

		//电量监控参数
		IntentFilter batteryMonitorFilter = new IntentFilter();
		batteryMonitorFilter.addAction(BROAD_BATTERY_MONITOR);
		try {
			mContext.registerReceiver(mBatteryMonitorReceiver, batteryMonitorFilter);
		} catch (Exception e) {
		}
		
		//电池电量
		IntentFilter batteryIntentFilter=new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(mBatteryStateReceiver, batteryIntentFilter);
	}
	
	private void unRegisterReceiver()
	{
		try {
			mContext.unregisterReceiver(mScreenReceiver);
		} catch (Exception e) {
		}

		try {
			mContext.unregisterReceiver(mBatteryMonitorReceiver);
		} catch (Exception e) {
		}
		
		try {
			unregisterReceiver(mBatteryStateReceiver);
		} catch (Exception e) {
		}
	}
	
	private void sendUHFState(int uhf_state)
	{
		Intent stateIntent = new Intent(UHFManager.ACTOIN_UHF_STATE_CHANGE);
		stateIntent.putExtra(UHFManager.EXTRA_UHF_STATE, uhf_state);
		mContext.sendBroadcast(stateIntent);
	}	
	
	private boolean doResetDefaultSettings()
	{
		boolean suc = false;
		try {
			boolean mOldPowerOn = mPowerOn;
			UHFReader.READER_STATE er = mPowerOn?doPowerOff() : UHFReader.READER_STATE.OK_ERR;//下电
			if(er == UHFReader.READER_STATE.OK_ERR){
				suc = mSettingsService.clearSavedUHFSettings();//清空所有保存的配置
				if(suc && mOldPowerOn)
				{
					Thread.sleep(500);
//					er =doPowerOn(null);
					int state = powerOnThread();
					if (state != UHFReader.READER_STATE.OK_ERR.value()) er = UHFReader.READER_STATE.CMD_FAILED_ERR;
				}
				Log.d(TAG, "Reset default settings complete.");
				return er == UHFReader.READER_STATE.OK_ERR;
			}
		} catch (Exception e) {
			Log.w(TAG, "Do reset default settings failed.", e);
		}
		
		return false;
	}


	//检查蓝牙服务是否断开
    private void checkServiceConn()
    {
        if(!mConnectState  || mBleInterface  == null )
        {
            Log.d(TAG, "Ble service reconnecting ...");
            bindBleService();
        }
    }



	//===================================================================
	
	private class OperateHandler extends Handler{

		public final static int MSG_START_READING = 0X01;
		public final static int MSG_STOP_READING = 0X02;

		
		public OperateHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what)
			{
			case MSG_START_READING:
				doStartReading();
				break;
			case MSG_STOP_READING:
				doStopReading();
				break;
			}
		}
		
	}//end class ScanHandler
	
	private class SenderHandler extends Handler
	{
		public final static int MSG_SEND_RESULT = 0X01;
		public final static int MSG_DO_SCREEN_ON = 0X02;
		public final static int MSG_DO_SCREEN_OFF = 0X03;
        public final static int MSG_CHECK_CONN_ALIVE = 0X04;

        //检测时间间隔(毫秒)
        private final static int CHECK_INTERVAL_TIME_MS = 1000*2;

		public SenderHandler(Looper looper) {
			super(looper);
		}

		@Override
		public void handleMessage(Message msg) 
		{
			if(msg.what == MSG_SEND_RESULT)
			{
				TagInfo[] infos = (TagInfo[])msg.obj;
				int outputMode = UHFManager.getInstance().getOutputMode();
				if(outputMode == UHFCommonParams.OUTPUT_MODE.VALUE_OUT_PUT_MODE_BROADCAST)
					sendResultByBroadcast(infos);
				if(outputMode == UHFCommonParams.OUTPUT_MODE.VALUE_OUT_PUT_MODE_API)
					sendResultByAPI(infos);
			}
			
			if(msg.what == MSG_DO_SCREEN_ON)
			{
				if(mPowerOnBeforeScreenOff)//屏幕灭屏前,是否处在上电状态,如果是,就自动上电
					doPowerOn(null);
			}
			
			if(msg.what == MSG_DO_SCREEN_OFF)
			{
				mPowerOnBeforeScreenOff = mPowerOn;
				if(mUHFDeviceModel == null)
					return ;
				
				doStopReading();
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					// TODO: handle exception
				}
				doPowerOff();
			}

			if (msg.what == MSG_CHECK_CONN_ALIVE){
                checkServiceConn();
                sendEmptyMessageDelayed(MSG_CHECK_CONN_ALIVE, CHECK_INTERVAL_TIME_MS);
            }
			
		}
		
	}//SenderHandler;
	
	/**
	 * 广播监听<p>
	 * 屏幕亮灭事件<p>
	 */
	private BroadcastReceiver mScreenReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(Intent.ACTION_SCREEN_ON.equals(action))
			{
				mScreenOn = true;
				Message.obtain(mSenderHandler, SenderHandler.MSG_DO_SCREEN_ON).sendToTarget();
				
			}else if(Intent.ACTION_SCREEN_OFF.equals(action)){
				mScreenOn = false;
				Message.obtain(mSenderHandler, SenderHandler.MSG_DO_SCREEN_OFF).sendToTarget();//下电
			}
			
		}//end onReceiver
		
	};//end mReceiver


	/**
	 * 上电操作监控
	 */
	private BroadcastReceiver mBatteryMonitorReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BROAD_BATTERY_MONITOR.equals(action)){
				mBatteryMonitorOn = intent.getBooleanExtra(EXTRA_STRING_MONITOR,false);
				mBatteryWarn1 = intent.getIntExtra(EXTRA_STRING_WARN_ONE,mBatteryWarn1);
				mBatteryWarn2 = intent.getIntExtra(EXTRA_STRING_WARN_TWO,mBatteryWarn2);
			}
		}
	};

	
	/**
	 * 电池电量监控
	 */
	private BroadcastReceiver mBatteryStateReceiver=new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action=intent.getAction();
			//未上电状态下也要更新当前电量
			if (Intent.ACTION_BATTERY_CHANGED.equals(action) && !mPowerOn && mBatteryMonitorOn){
				//当前电量比
				int level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
				//总电量单位
				int scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100);
				int mCurLevel=(int) (((float) level/scale)*100);
				mPowerAllow = mCurLevel > mBatteryWarn2;
				mCurCharge = mCurLevel;
			}

			if(Intent.ACTION_BATTERY_CHANGED.equals(action) && mPowerOn && mBatteryMonitorOn)
			{
				
				long id = Binder.clearCallingIdentity();
				//状态
				int status=intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
				//外接电源类型
				int plugType=intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
				//当前电量比
				int level=intent.getIntExtra(BatteryManager.EXTRA_LEVEL,0);
				//总电量单位
				int scale=intent.getIntExtra(BatteryManager.EXTRA_SCALE,100);
				int mCurLevel=(int) (((float) level/scale)*100);
				mPowerAllow = mCurLevel > mBatteryWarn2;

				if (  mBatteryMonitorOn &&
						mCurLevel <= mBatteryWarn1
						){
					if ( (mCurCharge > 0 && mCurLevel - mCurCharge == -1) || mCurCharge == -1)
						showBatteryDialog1();
				}


				if (    mBatteryMonitorOn &&
						mCurLevel <= mBatteryWarn2
				       ){

					if ( (mCurCharge > 0 && mCurLevel - mCurCharge == -1) || mCurCharge == -1)
						showBatteryDialog2();
				}

				mCurCharge = mCurLevel;

				Log.d( "BatteryMonitor","current level: " + mCurCharge + " warn 1: " + mBatteryWarn1
						+ " warn 2: " + mBatteryWarn2);


				Map<String, Object> settingsMap = mUHFMgr.getAllParams();
				if (settingsMap == null) return;
				int iLowerpowerEnable =settingsMap.containsKey(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE)? (Integer)settingsMap.get(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE) : 0;;
				if(iLowerpowerEnable == 0)
					return ;

				final int LOW_LEVEL = (Integer) settingsMap.get(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_LEVEL);
				//DLog.d(TAG, "Current Level : "+mCurLevel+", LOW_LEVEL: "+LOW_LEVEL);
				
				String sValue = (String) settingsMap.get(UHFSilionParams.RF_ANTPOWER.KEY);
				int curReadAntPower = -1;//当前天线功率
				int[] curWriteAntPowers = new int[]{2700};
				JSONArray jsArray = null;
				if (sValue != null ) 
				{
					try {
						jsArray = new JSONArray(sValue);
						int len = jsArray.length();
						if(len > 0)
						{
							curWriteAntPowers = new int[len];
							for(int i =0 ;i < len;i++)
							{
								JSONObject jobj = jsArray.optJSONObject(i);
								int tempAntPower = jobj.optInt("readPower");//读功率
								curWriteAntPowers[i] = jobj.optInt("writePower");//写功率
								curReadAntPower = tempAntPower > curReadAntPower?tempAntPower:curReadAntPower;
							}
							
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				
				//低电量读功率DBM
				String  sLowAntPower = mUHFMgr.getParam(UHFSilionParams.LOWER_POWER.KEY, UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_READ_DBM, "2000");
				if(mCurLevel < LOW_LEVEL && curReadAntPower > Integer.parseInt(sLowAntPower)) //电量低于指定值(如:20%)
				{
					
					try {
						if(jsArray != null && jsArray.length() > 0)
						{
							JSONArray jsItemArray = new JSONArray();
							for (int i = 0; i < jsArray.length(); i++) 
							{
								int antid = i + 1;
								int readPower =  Integer.parseInt(sLowAntPower);
								int writePower =  curWriteAntPowers[i];
								JSONObject jsItem = new JSONObject();
								jsItem.put("antid", antid);
								jsItem.put("readPower", readPower);
								jsItem.put("writePower", writePower);
								
								jsItemArray.put(jsItem);
							}
							
							UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.RF_ANTPOWER.KEY, UHFSilionParams.RF_ANTPOWER.PARAM_RF_ANTPOWER, jsItemArray.toString());
							if(er == UHFReader.READER_STATE.OK_ERR
									&& mReadingState == ReadingState.READING)
							{
								long oldStartTime = mStartReadTime;
								mUHFMgr.stopTagInventory();
								Thread.sleep(100);
								mStartReadTime = oldStartTime;
								mUHFMgr.startTagInventory();
							}
								
							DLog.d(TAG, "Lower power , set ant_power : "+jsArray.length()+",current power level : "+mCurLevel+",state : "+er);
						}
						
						
					} catch (Exception e) {
					}
					
				}//end if(mCurLevel < 20)
				
				Binder.restoreCallingIdentity(id);
			}//end if
			
		}//end onReceiver
	};


	/**
	 * 当电量低于警戒线1时，弹出对话框提示充电
	 */
	private void showBatteryDialog1() {

		AlertDialog dialog = new AlertDialog.Builder(getApplicationContext()).
						setTitle(R.string.dialog_low_power_title)
						.setMessage(R.string.dialog_notice_charge)
						.setCancelable(false)
						.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog, int id) {
							}
						})
						.create();
		dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		dialog.show();

	}


	/**
	 * 当电量低于警戒线2时，弹出对话框，提示下电
	 */
	private void showBatteryDialog2() {

		AlertDialog dialog = new AlertDialog.Builder(getApplicationContext()).
				setTitle(R.string.dialog_low_power_title)
				.setMessage(R.string.dialog_notice_down)
				.setCancelable(false)
				.setPositiveButton(R.string.dialog_power_down, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						mUHFMgr.powerOff();
					}
				})
				.setNegativeButton(R.string.dialog_go_on, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {

					}
				})
				.create();
		dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		dialog.show();
	}


	/**
	 * 当电量低于警戒线2时，弹出对话框提示无法上电
	 */
	private void showBatteryDialog3() {

		AlertDialog dialog = new AlertDialog.Builder(getApplicationContext()).
				setTitle(R.string.dialog_low_power_title)
				.setMessage(R.string.dialog_notice_allow)
				.setCancelable(false)
				.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
					}
				})
				.create();
		dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
		dialog.show();

	}


	private void enableBlue(int count){
		if (mBluetooth == null){
			mBluetooth = BluetoothAdapter.getDefaultAdapter();

		}

		if (mBluetooth.isEnabled() ){
			if (count == 0)
				mBluetooth.disable();
		}
		else{
			mBluetooth.enable();
		}

	}


	
	//===================================================================


	private int powerOnThread(){
		//UHF模块上电
		doPowerRetryCount = 0;
		sendUHFState(UHFManager.UHF_STATE_POWER_ONING);

		long startTime = System.currentTimeMillis();

		UHFReader.READER_STATE state =  doPowerOn(null);

//				int bindTry = 0;

		while (System.currentTimeMillis() - startTime <= 6000){
			Log.d(TAG,"retry power up");
			if (state == UHFReader.READER_STATE.OK_ERR) break;

//					try {
//						Thread.sleep(2000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}

//					if (System.currentTimeMillis() - startTime >= 12000 && bindTry <= 0){
//						enableBlue(bindTry);
//
//						bindTry++;
//
//					}

			state = doPowerOn(null);
		}

		sendUHFState(mPowerOn?UHFManager.UHF_STATE_POWER_ON : UHFManager.UHF_STATE_POWER_OFF);



		long readTimeout = mSettingsService.getLongParamValue(UHFSilionParams.INV_TIME_OUT.KEY, UHFSilionParams.INV_TIME_OUT.PARAM_INV_TIME_OUT,
				UHFSilionParams.INV_TIME_OUT.DEFAULT_INV_TIMEOUT);
		long intevalTime =mSettingsService.getLongParamValue(UHFSilionParams.INV_INTERVAL.KEY,
				UHFSilionParams.INV_INTERVAL.PARAM_INV_INTERVAL_TIME,
				UHFSilionParams.INV_INTERVAL.DEFAULT_INV_INTERVAL_TIME);

		if (state.value() == UHFReader.READER_STATE.OK_ERR.value()){
			if (!isQuickMode()){
				mReader.StartReading((int)readTimeout,(int) intevalTime);
			}
			else {
				mReader.AsyncStartReading();
			}
		}

		return state.value();


	}




	private class MyBinder extends ISilionUHFService.Stub
	{
		@Override
		public int  powerOn() throws RemoteException
		{
			
			synchronized (this) {

				mOperHandler.removeMessages(OperateHandler.MSG_START_READING);

				mOperHandler.sendEmptyMessage(OperateHandler.MSG_START_READING);
				
				long id = Binder.clearCallingIdentity();
				
//				灭屏状态
				if( !mScreenOn )
					return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
				
//				if(mPowerOn)
//					return UHFReader.READER_STATE.OK_ERR.value();



				int stateVal = powerOnThread();
				
				Binder.restoreCallingIdentity(id);

				return stateVal;
			}
			
			
			
		}

		@Override
		public int powerOff() throws RemoteException {

			Log.d(TAG,"the device power off");
			mOperHandler.removeMessages(OperateHandler.MSG_START_READING);

			
			long id = Binder.clearCallingIdentity();
			UHFReader.READER_STATE state = doPowerOff();
			mPowerOn = false;
			Binder.restoreCallingIdentity(id);
			return state.value();
		}

		@Override
		public int startTagInventory() throws RemoteException {
			
			synchronized (this) {
				
				long id = Binder.clearCallingIdentity();
				
				//灭屏状态
				if( !mScreenOn )
					return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
				
				if( !mPowerOn)
					return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
				
				//扫描中,返回
//				if(mReadingState == ReadingState.READING)
//					return UHFReader.READER_STATE.OK_ERR.value();
				
				//触发扫描
				mIfQuickReading = true;
				mForceStoped = false;
				if (mBleInterface!=null)
					mBleInterface.clearUhfTagData();
				mReader.clearTagData();



				mOperHandler.removeMessages(OperateHandler.MSG_START_READING);

				mOperHandler.sendEmptyMessage(OperateHandler.MSG_START_READING);

//				if(isQuickMode)
//					nonStopStartReading();
//				else{
//					mReader.StartReadingCommon();
//
//				}

				
				Binder.restoreCallingIdentity(id);
				return mReader.StartReadingCommon().value();
			}
			
		}

		@Override
		public int stopTagInventory() throws RemoteException {
			Log.d(TAG,"stop the tag inventory");
			
			long id = Binder.clearCallingIdentity();
			mOperHandler.sendEmptyMessage(OperateHandler.MSG_STOP_READING);
			Binder.restoreCallingIdentity(id);

//			if (isQuickMode())
//				while (mIfQuickReading){}
//			if (!mIfQuickReading)
				return UHFReader.READER_STATE.OK_ERR.value();
//			else
//				return UHFReader.READER_STATE.CMD_FAILED_ERR.value();
		}

		@Override
		public boolean isPowerOn() throws RemoteException {
			return mPowerOn;
		}

		@Override
		public boolean isInInventory() throws RemoteException {
			return mReadingState == ReadingState.READING;
		}

		@Override
		public int writeTagData(int bank, int address, byte[] data, String hexAccesspasswd) throws RemoteException {
			long id = Binder.clearCallingIdentity();
//			mReader.StopReading();
			//灭屏状态
			if( !mScreenOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			//未上电
			if( !mPowerOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			
			int ant = getOperateAnt();
			byte[] bytePasswd = UHFReader.Str2Hex(hexAccesspasswd);
			bytePasswd = bytePasswd == null?new byte[4]:bytePasswd;
			int datalen = data.length;
			READER_ERR er = mReader.WriteTagData(ant,  (char)bank, address, data, datalen, bytePasswd, (short)1000);
			Binder.restoreCallingIdentity(id);
			return UHFReader.READER_STATE.valueOf(er.value()).value();
		}

		@Override
		public int writeTagEpcEx(byte[] epcData, String hexAccesspasswd) throws RemoteException {
			long id = Binder.clearCallingIdentity();
//			mReader.StopReading();
			//灭屏状态
			if( !mScreenOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			//未上电
			if( !mPowerOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			
			int ant = getOperateAnt();
			int epclen = epcData.length;
			byte[] bytePasswd = UHFReader.Str2Hex(hexAccesspasswd);
			READER_ERR er = mReader.WriteTagEpcEx(ant, epcData, epclen, bytePasswd, (short)1000);
			Binder.restoreCallingIdentity(id);
			return UHFReader.READER_STATE.valueOf(er.value()).value();
		}

		@Override
		public byte[] GetTagData(int bank, int address, int blkcnt, String hexAccesspasswd) throws RemoteException {
			long id = Binder.clearCallingIdentity();
//			mReader.StopReading();
			//灭屏状态
			if( !mScreenOn )
				return null;
			//未上电
			if( !mPowerOn )
				return null;
			
			int ant = getOperateAnt();
			byte[] data=new byte[blkcnt*2];
			byte[] bytePasswd = UHFReader.Str2Hex(hexAccesspasswd);
			READER_ERR er = mReader.GetTagData(ant, (char)bank, address, blkcnt, data, bytePasswd, (short)1000) ;
			Binder.restoreCallingIdentity(id);
			return er == READER_ERR.MT_OK_ERR? data : null;
		}

		@Override
		public int LockTag(int lockObject, int lockType, String hexAccesspasswd) throws RemoteException {
			long id = Binder.clearCallingIdentity();
//			mReader.StopReading();
			//灭屏状态
			if( !mScreenOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			//未上电
			if( !mPowerOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			
	      int targetLlockObj =lockObject;
//	      if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD.value() ) > 0 )
//	    	  targetLlockObj |= Reader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD.value();
//	      if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD.value() ) > 0 )
//	    	  targetLlockObj |= Reader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD.value();
//	      if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK1.value() ) > 0 )
//	    	  targetLlockObj |= Reader.Lock_Obj.LOCK_OBJECT_BANK1.value();
//	      if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK2.value() ) > 0 )
//	    	  targetLlockObj |= Reader.Lock_Obj.LOCK_OBJECT_BANK2.value();
//	      if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK3.value() ) > 0 )
//	    	  targetLlockObj |= Reader.Lock_Obj.LOCK_OBJECT_BANK3.value();
	      

	      	int tartgetLockType = lockType;
//	      	if( (lockType & UHFReader.Lock_Type.LOCK.value()) > 0) //暂时锁定
//	      	{
//	      		if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD.value() ) > 0 )
//	      			tartgetLockType |= Reader.Lock_Type.ACCESS_PASSWD_LOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD.value() ) > 0 )
//	      			tartgetLockType |= Reader.Lock_Type.KILL_PASSWORD_LOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK1.value() ) > 0 )
//	      			tartgetLockType |= Reader.Lock_Type.BANK1_LOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK2.value() ) > 0 )
//		      			tartgetLockType |= Reader.Lock_Type.BANK2_LOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK3.value() ) > 0 )
//		      			tartgetLockType |= Reader.Lock_Type.BANK3_LOCK.value();
//
//	      	}else if( (lockType & UHFReader.Lock_Type.UNLOCK.value()) > 0){//解锁
//
//	      		if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD.value() ) > 0 )
//	      			tartgetLockType |= Reader.Lock_Type.ACCESS_PASSWD_UNLOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD.value() ) > 0 )
//	      			tartgetLockType |= Reader.Lock_Type.KILL_PASSWORD_UNLOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK1.value() ) > 0 )
//	      			tartgetLockType |= Reader.Lock_Type.BANK1_UNLOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK2.value() ) > 0 )
//		      			tartgetLockType |= Reader.Lock_Type.BANK2_UNLOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK3.value() ) > 0 )
//		      			tartgetLockType |= Reader.Lock_Type.BANK3_UNLOCK.value();
//
//	      	}else if( (lockType & UHFReader.Lock_Type.PERM_LOCK.value()) > 0){//永久锁定(锁定之后必须使用密码才可以操作)
//
//	      		if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD.value() ) > 0 )
//	      			tartgetLockType |= Reader.Lock_Type.ACCESS_PASSWD_PERM_LOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD.value() ) > 0 )
//	      			tartgetLockType |= Reader.Lock_Type.KILL_PASSWORD_PERM_LOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK1.value() ) > 0 )
//	      			tartgetLockType |= Reader.Lock_Type.BANK1_PERM_LOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK2.value() ) > 0 )
//		      			tartgetLockType |= Reader.Lock_Type.BANK2_PERM_LOCK.value();
//
//	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK3.value() ) > 0 )
//		      			tartgetLockType |= Reader.Lock_Type.BANK3_PERM_LOCK.value();
//	      	}
	      	
	      	int ant = getOperateAnt();
	      	byte[] pwdb = null;
	      	pwdb=UHFReader.Str2Hex(hexAccesspasswd);
	      	pwdb = pwdb == null ? new byte[4]:pwdb;
	      	READER_ERR er = mReader.LockTag(ant,(byte)targetLlockObj,(short)tartgetLockType,pwdb,(short)1000);
			Binder.restoreCallingIdentity(id);
			return UHFReader.READER_STATE.valueOf(er.value()).value();
		}

		@Override
		public int KillTag(String hexAccesspasswd) throws RemoteException {
			long id = Binder.clearCallingIdentity();
//			mReader.StopReading();
			//灭屏状态
			if( !mScreenOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			//未上电
			if( !mPowerOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			
			int ant = getOperateAnt();
			 byte[] kpwdb=UHFReader.Str2Hex(hexAccesspasswd);
			 kpwdb = kpwdb == null ? new byte[4]:kpwdb;
			 READER_ERR er = mReader.KillTag(ant, kpwdb, (short)1000);
			Binder.restoreCallingIdentity(id);
			return UHFReader.READER_STATE.valueOf(er.value()).value();
		}

		@Override
		public int setParams(String paramKey,String paramName,String sValue) throws RemoteException {
			long id = Binder.clearCallingIdentity();
//			mReader.StopReading();
			//灭屏状态
			if( !mScreenOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			//未上电
			if( !mPowerOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();

			if ( UHFSilionParams.RESTORE_FACTORY.PARAM_RESTORE_FACTORY.equals(paramName))
				doPowerOff();
			
			UHFReader.READER_STATE state =  mSettingsService.setParam(paramKey, paramName, sValue);
			
			Binder.restoreCallingIdentity(id);
			return state.value();
		}

		@Override
		public Map<String,Object> getAllParams() throws RemoteException {
			
			return mSettingsMap;
		}

		@Override
		public void registerTagInventoryListener(IUHFTagInventoryListener listener) throws RemoteException {
			if(listener != null)
				mClientCallbackList.register(listener);
		}

		@Override
		public void unRegisterTagInventoryListener(IUHFTagInventoryListener listener) throws RemoteException {
			if(listener != null)
				mClientCallbackList.unregister(listener);
		}

		@Override
		public String getUHFDeviceModel() throws RemoteException {
			
			if(mUHFDeviceModel != null)
				return mUHFDeviceModel;
			
			boolean oldPowerOn = mPowerOn;
			 UHFReader.READER_STATE state = UHFReader.READER_STATE.CMD_FAILED_ERR;
			if(!mPowerOn) //未上电,上先电获取
			{
				//驱动上电
//				boolean power = powerDriver(true);
//				if(power)
					state =  initReader();
			}
			
			boolean available = state == UHFReader.READER_STATE.OK_ERR;
			Log.d(TAG, "Get UHFDeviceModel available: "+available);	
			 if(available)
	        {
				mUHFDeviceModel = DEFAULT_MODULE;
				Log.d(TAG, "Get Module_Type : "+mUHFDeviceModel);

				if (mReader != null)
					mReader.CloseReader();
	        }
			
//			if(!oldPowerOn)
//			{
//				//驱动下电
//				powerDriver(false);
//			}
			
			return mUHFDeviceModel;
			
		}//end getUHFDeviceModel

		@Override
		public boolean isDeviceAvailable() throws RemoteException {
			
			long id = Binder.clearCallingIdentity();
			boolean available = false;
			
			if(mPowerOn) //已上电状态,表示设备正常装载
				available = true;
			else //未上电,上先电获取
			{
				//驱动上电
//				boolean power = powerDriver(true);
				
				
				UHFReader.READER_STATE state = initReader();


		        available = state == UHFReader.READER_STATE.OK_ERR;
		        Log.d(TAG, "isDeviceAvailable available: "+available);	
		        if(available)
		        {
//		        	HardwareDetails val = mReader.new HardwareDetails();
//		        	READER_ERR  er = mReader.GetHardwareDetails(val);//获取硬件信息
//
//					if(er == READER_ERR.MT_OK_ERR)
//					{
						//UHF模块型号
						mUHFDeviceModel = DEFAULT_MODULE;
						Log.d(TAG, "Get Module_Type : "+mUHFDeviceModel);
//					}
					
					if (mReader != null)
						mReader.CloseReader();
					
					
		        }
				
		      //驱动下电
//				powerDriver(false);
			}
			Binder.restoreCallingIdentity(id);
			return available;
		}

		@Override
		public String getParam(String paramKey, String paramName) throws RemoteException {
//			mReader.StopReading();

			if (UHFSilionParams.GET_BLE_STATE.KEY.equals(paramKey) &&
				UHFSilionParams.GET_BLE_STATE.PARAM_BLE_STATE.equals(paramName)	){
				return  "" + mBleInterface.isBleAccess()  ;
			}

			return mSettingsService.getParam(paramKey, paramName);
		}
		
		/**
	     * 还原默认设置
	     * @return true-是,false-否
	     */
		@Override
		public boolean restoreDefaultSettings() throws RemoteException {
			long id = Binder.clearCallingIdentity();
			boolean suc = doResetDefaultSettings();
			Binder.clearCallingIdentity();
			return suc;
		}
	}//end Binder
	
}
