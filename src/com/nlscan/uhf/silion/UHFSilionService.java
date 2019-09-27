package com.nlscan.uhf.silion;

import java.io.FileWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.nlscan.android.uhf.IUHFTagInventoryListener;
import com.nlscan.android.uhf.TagInfo;
import com.nlscan.android.uhf.UHFCommonParams;
import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFModuleInfo;
import com.nlscan.android.uhf.UHFReader;
import com.pow.api.cls.RfidPower;
import com.pow.api.cls.RfidPower.PDATYPE;
import com.uhf.api.cls.Reader;
import com.uhf.api.cls.Reader.HardwareDetails;
import com.uhf.api.cls.Reader.Mtr_Param;
import com.uhf.api.cls.Reader.READER_ERR;
import com.uhf.api.cls.Reader.TAGINFO;

public class UHFSilionService extends Service {

	private final static String TAG = Constants.TAG_PREFIX+"UHFSilionService";
	
	private Context mContext;
	private IBinder mBinder;
	private RfidPower mRfidPower;
	private Reader mReader;
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
	
	@Override
	public void onCreate() {
		super.onCreate();
		
		mContext = getApplicationContext();
		mUHFMgr = UHFManager.getInstance();
		
		mReader = new Reader();
		mRfidPower = new RfidPower(PT);
		
		mSettingsService = new UHFSilionSettingService(mContext, mReader);
		mSettingsMap = mSettingsService.getAllSettings();
		
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

	@Override
	public void onDestroy() {
		super.onDestroy();
		unRegisterReceiver();
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
		try {
        	//上电
            FileWriter fw58 = new FileWriter(mUHFModuleInfo.power_driver);//写文件
            fw58.write(powerOn?"1":"0");
            fw58.close();
            Log.d(TAG, "Power driver, Power state: "+(powerOn?"POWER ON.":"POWER DOWN."));
            Thread.sleep(10);
           } catch (Exception e) {
        	   Log.w(TAG, "Power driver, write device data error.",e);
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
        	   Log.w(TAG, "Power driver, write device data error.",e);
           }
		
		return true;
	}
	
	private int doPowerRetryCount = 0;
	
	/**
	 * UHF模块上电
	 * @param reTryState 上一次的上电状态
	 * @return
	 */
	private UHFReader.READER_STATE doPowerOn(UHFReader.READER_STATE lastState)
	{
		if(doPowerRetryCount == 0)
			sendUHFState(UHFManager.UHF_STATE_POWER_ONING);
		
		//驱动上电
		boolean powOn = powerDriver(true);
		if(!powOn)
			return  UHFReader.READER_STATE.HARDWARE_ALERT_ERR_BY_UNKNOWN_ERR;
		
        boolean blen=mRfidPower.PowerUp();
        UHFReader.READER_STATE state =  blen ? UHFReader.READER_STATE.OK_ERR : UHFReader.READER_STATE.CMD_FAILED_ERR;
        
        if(state == UHFReader.READER_STATE.OK_ERR)
			state = initReader();//连接读写器(初始创建读写器)
		if(state == UHFReader.READER_STATE.OK_ERR)
			state = doInitReaderParams();//初始化读写器参数
		mPowerOn = (state == UHFReader.READER_STATE.OK_ERR);
		if(state != UHFReader.READER_STATE.OK_ERR){ //初始化读写器失败,下电以备下一次的重新上电
			powerDriver(false);//驱动下电
		}
		
		//失败重试
//		if(state != UHFReader.READER_STATE.OK_ERR && doPowerRetryCount < 1)
//		{
//			try {
//				Log.d(TAG, "Do power failed,state: "+state.toString()+", Retry doPowerOn.");
//				Thread.sleep(200);
//				doPowerRetryCount ++ ;
//				state = doPowerOn(state);
//			} catch (Exception e) {
//			}
//		}
//		
//		if(doPowerRetryCount == 0)
//			sendUHFState(mPowerOn?UHFManager.UHF_STATE_POWER_ON : UHFManager.UHF_STATE_POWER_OFF);
//		else
//			doPowerRetryCount --;
		sendUHFState(mPowerOn?UHFManager.UHF_STATE_POWER_ON : UHFManager.UHF_STATE_POWER_OFF);
        return state;
	}
	
	/**
	 * 连接读写器(初始创建读写器)
	 * @return
	 */
	private UHFReader.READER_STATE initReader()
	{
		Log.d(TAG, "Start connect to device...");
		
		//初始化读写器
		int ant = 1;
		 READER_ERR er = Reader.READER_ERR.MT_CMD_FAILED_ERR; 
		try{
			er=mReader.InitReader_Notype(mUHFModuleInfo.serial_path, ant);
			Log.d(TAG, "Connect to device state : "+er+","+(er == READER_ERR.MT_OK_ERR));
			return UHFReader.READER_STATE.valueOf(er.value());
		}catch(Exception e){
			Log.w(TAG, "Connect to reader faild.",e);
		} 
		
		return UHFReader.READER_STATE.CMD_FAILED_ERR;
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
        	Reader.READER_ERR er = Reader.READER_ERR.MT_CMD_FAILED_ERR;
        	er = mReader.ParamSet(Mtr_Param.MTR_PARAM_TAG_SEARCH_MODE, new int[] { 0 });
        	
			HardwareDetails val = mReader.new HardwareDetails();
			er = mReader.GetHardwareDetails(val);//获取硬件信息
			
			if(er == READER_ERR.MT_OK_ERR)
			{
				//UHF模块型号
				mUHFDeviceModel = val.module.toString();
				
				Log.d(TAG, "Module_Type : "+val.module.toString());
				Log.d(TAG, "MaindBoard_Type : "+val.board.toString());
				Log.d(TAG, "Reader_Type : "+val.logictype.toString());
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
		
		//停止盘点
		doStopReading();
		
		synchronized (this) {
			
			if (mReader != null)
				mReader.CloseReader();

			boolean oldPowerOn = mPowerOn;
			boolean blen = mRfidPower.PowerDown();
			try {
				//驱动下电
				powerDriver(false);
				Log.d(TAG, "UHF disconnect complete,state : "+blen);
				UHFReader.READER_STATE state =  blen ? UHFReader.READER_STATE.OK_ERR : UHFReader.READER_STATE.CMD_FAILED_ERR;
				if(state == UHFReader.READER_STATE.OK_ERR ){
					mPowerOn = false;
				}
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
			er = mReader.AsyncStartReading(uants, uants.length, 0);
			if (er != READER_ERR.MT_OK_ERR) {
				DLog.w(TAG, "不停顿盘点启动失败 : "+er.toString());
				return;
			}else
				mOperHandler.sendEmptyMessage(OperateHandler.MSG_START_READING);
		}
	}
	
	private void doStartReading()
	{
		//Log.d(TAG, "Enter doStartReading..");
		
		if(mForceStoped)
		{
			Log.d(TAG, "Reading is stoped.");
			mReadingState = ReadingState.IDLE;
			return ;
		}
		
		try {
			if(mStartReadTime == 0)
				mStartReadTime = System.currentTimeMillis();
			
			READER_ERR er;
			TAGINFO[] tagInfos = null;
			String[] epcIds = null;
			int[] tagcnt = new int[1]; //本
			tagcnt[0] = 0;
			
			
			boolean quickMode = isQuickMode();
			int[] uants = getAnts();
			long readTimeout = mSettingsService.getLongParamValue(UHFSilionParams.INV_TIME_OUT.KEY, UHFSilionParams.INV_TIME_OUT.PARAM_INV_TIME_OUT,UHFSilionParams.INV_TIME_OUT.DEFAULT_INV_TIMEOUT);
			
			if (quickMode) {
				er =mReader.AsyncGetTagCount(tagcnt);
			} else {
				//Log.d(TAG, "Start TagInventory_Raw...");
				//long begin = System.currentTimeMillis();
				er = mReader.TagInventory_Raw(uants,uants.length, (short) readTimeout, tagcnt);
				//long end = System.currentTimeMillis();
				//Log.d(TAG, "End TagInventory_Raw.., span time : "+(end - begin));
			}
			
			if (er == READER_ERR.MT_OK_ERR) 
			{
				mReadingState = ReadingState.READING;
				
				if (tagcnt[0] > 0) 
				{
					int tagCount = tagcnt[0];
					epcIds = new String[tagCount];
					tagInfos = new TAGINFO[tagCount];
					
					//Log.d(TAG, "============================================================");
					for (int i = 0; i < tagCount; i++) 
					{
						TAGINFO tfs = mReader.new TAGINFO();
						if (mRfidPower.GetType() == PDATYPE.SCAN_ALPS_ANDROID_CUIUS2) 
						{
							try {
								Thread.sleep(10);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						
						if (quickMode)
							er = mReader.AsyncGetNextTag(tfs);
						else
							er = mReader.GetNextTag(tfs);

						// Log.d("MYINFO","get tag index:" +
						// String.valueOf(i)+ " er:" + er.toString());
						if (er == READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) 
						{
							doStopReading();
							doPowerOff();
							break;
						}

						// Log.d("MYINFO","debug gettag:"+er.toString());
						// Log.d("MYINFO","debug tag:"+Reader.bytes_Hexstr(tfs.EpcId));
						String epcstr = tfs.EpcId == null ? null :  Reader.bytes_Hexstr(tfs.EpcId);
						if(epcstr == null)
							continue;
						
						if (epcstr.length() < 24)
							epcstr = String.format("%-24s", epcstr);
						if (er == READER_ERR.MT_OK_ERR) 
						{
							epcIds[i] = epcstr;
							tagInfos[i] = tfs;
						} 
						
						//Log.d(TAG, "EpcId : "+epcstr);
						
					}//end for
					
					//Log.d(TAG, "============================================================");
					
					if(tagInfos != null && tagInfos.length > 0)
					{
						//成功提示
						performPrompt();
						//发送给客户
						sendResult(tagInfos);
					}
					
					
				}//end if(tagcnt[0] > 0) 
				
			}else{
				
				Log.w(TAG, "Reading error : er = "+er.toString());
				if (er == READER_ERR.MT_HARDWARE_ALERT_ERR_BY_TOO_MANY_RESET) 
				{
					doPowerOff();
					Thread.sleep(500);
					doPowerOn(null);
				}
				
			}//end if
			
		} catch (Exception e) {
			Log.w(TAG, "Start reading failed.", e);
		}
		
		//进入下一扫描周期
		long intevalTime =mSettingsService.getLongParamValue(UHFSilionParams.INV_INTERVAL.KEY, UHFSilionParams.INV_INTERVAL.PARAM_INV_INTERVAL_TIME,UHFSilionParams.INV_INTERVAL.DEFAULT_INV_INTERVAL_TIME);
		mOperHandler.sendEmptyMessageDelayed(OperateHandler.MSG_START_READING, intevalTime);
		
	}//end doStartReading
	
	/**
	 * 停止盘点
	 */
	private void doStopReading()
	{
		synchronized (this) {
			
			Log.d(TAG, "Enter doStopReading...");
			
			mStartReadTime = 0;
			mForceStoped = true;
			mOperHandler.removeMessages(OperateHandler.MSG_START_READING);
			
			READER_ERR er = null;
			if (isQuickMode()) {
				Log.d(TAG, "stop---");
				er = mReader.AsyncStopReading();
				mReadingState = ReadingState.IDLE;
				if (er != READER_ERR.MT_OK_ERR) {
					Log.d(TAG, "不停顿盘点停止失败 : "+er.toString());
					return;
				}
			}else{
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
		int ant = (int)mSettingsMap.get(UHFSilionParams.ANTS.PARAM_OPERATE_ANTS);
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
			iQuick = (int)obj;
		
		return iQuick == 1;
	}
	
	/**
	 * 发送盘点结果
	 * @param tags
	 */
	private void sendResult(TAGINFO[] tags)
	{
		if(tags == null || tags.length == 0)
			return ;
		
		TAGINFO[]  newTags = Arrays.copyOf(tags, tags.length);
		TagInfo[] nls_TagInfos = new TagInfo[newTags.length];
		for(int i = 0 ;i< newTags.length;i++)
		{
			TAGINFO tag = newTags[i];
			try {
				tag.protocol = tag.protocol == null? Reader.SL_TagProtocol.SL_TAG_PROTOCOL_NONE:tag.protocol;
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
			mVibrator.vibrate(new long[] { 0, 80, 80 }, -1); // 震动
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
					er =doPowerOn(null);
				}
				Log.d(TAG, "Reset default settings complete.");
				return er == UHFReader.READER_STATE.OK_ERR;
			}
		} catch (Exception e) {
			Log.w(TAG, "Do reset default settings failed.", e);
		}
		
		return false;
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
				doStopReading();
				try {
					Thread.sleep(500);
				} catch (Exception e) {
					// TODO: handle exception
				}
				doPowerOff();
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
	 * 电池电量监控
	 */
	private BroadcastReceiver mBatteryStateReceiver=new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			
			String action=intent.getAction();
			if(Intent.ACTION_BATTERY_CHANGED.equals(action) && mPowerOn)
			{
				Map<String, Object> settingsMap = mUHFMgr.getAllParams();
				int iLowerpowerEnable =settingsMap.containsKey(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE)? (int)settingsMap.get(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE) : 0;;
				if(iLowerpowerEnable == 0)
					return ;
				
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
				
				
				final int LOW_LEVEL = (int) settingsMap.get(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_LEVEL);
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
	
	//===================================================================

	private class MyBinder extends ISilionUHFService.Stub
	{
		@Override
		public int  powerOn() throws RemoteException
		{
			
			synchronized (this) {
				
				long id = Binder.clearCallingIdentity();
				
				//灭屏状态
				//if( !mScreenOn )
					//return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
				
				if(mPowerOn)
					return UHFReader.READER_STATE.OK_ERR.value();
				
				//UHF模块上电
				doPowerRetryCount = 0;
				UHFReader.READER_STATE state =  doPowerOn(null);
				
				Binder.restoreCallingIdentity(id);
				return state.value();
			}
			
			
			
		}

		@Override
		public int powerOff() throws RemoteException {
			
			long id = Binder.clearCallingIdentity();
			UHFReader.READER_STATE state = doPowerOff();
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
				if(mReadingState == ReadingState.READING)
					return UHFReader.READER_STATE.OK_ERR.value();
				
				//触发扫描
				mForceStoped = false;
				boolean isQuickMode = isQuickMode();
				if(isQuickMode)
					nonStopStartReading();
				else
					mOperHandler.sendEmptyMessage(OperateHandler.MSG_START_READING);
				
				Binder.restoreCallingIdentity(id);
				return UHFReader.READER_STATE.OK_ERR.value();
			}
			
		}

		@Override
		public int stopTagInventory() throws RemoteException {
			
			long id = Binder.clearCallingIdentity();
			mOperHandler.sendEmptyMessage(OperateHandler.MSG_STOP_READING);
			Binder.restoreCallingIdentity(id);
			return UHFReader.READER_STATE.OK_ERR.value();
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
			//灭屏状态
			if( !mScreenOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			//未上电
			if( !mPowerOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			
	      int targetLlockObj =0;
	      if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD.value() ) > 0 )
	    	  targetLlockObj |= Reader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD.value();
	      if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD.value() ) > 0 )
	    	  targetLlockObj |= Reader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD.value();
	      if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK1.value() ) > 0 )
	    	  targetLlockObj |= Reader.Lock_Obj.LOCK_OBJECT_BANK1.value();
	      if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK2.value() ) > 0 )
	    	  targetLlockObj |= Reader.Lock_Obj.LOCK_OBJECT_BANK2.value();
	      if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK3.value() ) > 0 )
	    	  targetLlockObj |= Reader.Lock_Obj.LOCK_OBJECT_BANK3.value();
	      
	      	int tartgetLockType = 0;
	      	if( (lockType & UHFReader.Lock_Type.LOCK.value()) > 0) //暂时锁定
	      	{
	      		if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD.value() ) > 0 )
	      			tartgetLockType |= Reader.Lock_Type.ACCESS_PASSWD_LOCK.value();
	      		
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD.value() ) > 0 )
	      			tartgetLockType |= Reader.Lock_Type.KILL_PASSWORD_LOCK.value();
	      		 
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK1.value() ) > 0 )
	      			tartgetLockType |= Reader.Lock_Type.BANK1_LOCK.value();
	      		 
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK2.value() ) > 0 )
		      			tartgetLockType |= Reader.Lock_Type.BANK2_LOCK.value();
	      		 
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK3.value() ) > 0 )
		      			tartgetLockType |= Reader.Lock_Type.BANK3_LOCK.value();
	      		 
	      	}else if( (lockType & UHFReader.Lock_Type.UNLOCK.value()) > 0){//解锁
	      		
	      		if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD.value() ) > 0 )
	      			tartgetLockType |= Reader.Lock_Type.ACCESS_PASSWD_UNLOCK.value();
	      		
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD.value() ) > 0 )
	      			tartgetLockType |= Reader.Lock_Type.KILL_PASSWORD_UNLOCK.value();
	      		 
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK1.value() ) > 0 )
	      			tartgetLockType |= Reader.Lock_Type.BANK1_UNLOCK.value();
	      		 
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK2.value() ) > 0 )
		      			tartgetLockType |= Reader.Lock_Type.BANK2_UNLOCK.value();
	      		 
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK3.value() ) > 0 )
		      			tartgetLockType |= Reader.Lock_Type.BANK3_UNLOCK.value();
	      		 
	      	}else if( (lockType & UHFReader.Lock_Type.PERM_LOCK.value()) > 0){//永久锁定(锁定之后必须使用密码才可以操作)
	      		
	      		if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD.value() ) > 0 )
	      			tartgetLockType |= Reader.Lock_Type.ACCESS_PASSWD_PERM_LOCK.value();
	      		
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_KILL_PASSWORD.value() ) > 0 )
	      			tartgetLockType |= Reader.Lock_Type.KILL_PASSWORD_PERM_LOCK.value();
	      		 
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK1.value() ) > 0 )
	      			tartgetLockType |= Reader.Lock_Type.BANK1_PERM_LOCK.value();
	      		 
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK2.value() ) > 0 )
		      			tartgetLockType |= Reader.Lock_Type.BANK2_PERM_LOCK.value();
	      		 
	      		 if((lockObject & UHFReader.Lock_Obj.LOCK_OBJECT_BANK3.value() ) > 0 )
		      			tartgetLockType |= Reader.Lock_Type.BANK3_PERM_LOCK.value();
	      	}
	      	
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
			//灭屏状态
			if( !mScreenOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			//未上电
			if( !mPowerOn )
				return UHFReader.READER_STATE.INVALID_READER_HANDLE.value();
			
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
				powerDriver(true);
		        boolean blen=mRfidPower.PowerUp();
		        state =  blen ? UHFReader.READER_STATE.OK_ERR : UHFReader.READER_STATE.CMD_FAILED_ERR;
		        
		        if(state == UHFReader.READER_STATE.OK_ERR)
					state = initReader();//连接读写器(初始创建读写器)
			}
			
			boolean available = state == UHFReader.READER_STATE.OK_ERR;
			Log.d(TAG, "Get UHFDeviceModel available: "+available);	
			 if(available)
	        {
				HardwareDetails val = mReader.new HardwareDetails();
				Reader.READER_ERR er = mReader.GetHardwareDetails(val);//获取硬件信息
				
				if(er == READER_ERR.MT_OK_ERR)
				{
					//UHF模块型号
					mUHFDeviceModel = val.module.toString();
					Log.d(TAG, "Get Module_Type : "+val.module.toString());
				}
	        }
			
			if(!oldPowerOn)
			{
				//驱动下电
				powerDriver(false);
			}
			
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
				powerDriver(true);
				
		        boolean blen=mRfidPower.PowerUp();
		        UHFReader.READER_STATE state =  blen ? UHFReader.READER_STATE.OK_ERR : UHFReader.READER_STATE.CMD_FAILED_ERR;
		        
		        if(state == UHFReader.READER_STATE.OK_ERR)
					state = initReader();//连接读写器(初始创建读写器)
		        
		        available = state == UHFReader.READER_STATE.OK_ERR;
		        Log.d(TAG, "isDeviceAvailable available: "+available);	
		        if(available)
		        {
		        	HardwareDetails val = mReader.new HardwareDetails();
		        	Reader.READER_ERR  er = mReader.GetHardwareDetails(val);//获取硬件信息
					
					if(er == READER_ERR.MT_OK_ERR)
					{
						//UHF模块型号
						mUHFDeviceModel = val.module.toString();
						Log.d(TAG, "Get Module_Type : "+val.module.toString());
					}
					
					if (mReader != null)
						mReader.CloseReader();
		        }
				
				//驱动下电
				powerDriver(false);
			}
			Binder.restoreCallingIdentity(id);
			return available;
		}

		@Override
		public String getParam(String paramKey, String paramName) throws RemoteException {
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
