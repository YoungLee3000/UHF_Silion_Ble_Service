package com.nlscan.uhf.silionBle;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.nlscan.android.uhf.TagInfo;
import com.nlscan.android.uhf.UHFReader;
import com.uhf.api.cls.Reader;
import com.uhf.api.cls.Reader.Mtr_Param;
import com.uhf.api.cls.Reader.READER_ERR;

public class UHFSilionSettingService {

	private final static String TAG = "UHFSilionBleSetting";

	private final static String SP_NAME_COMMON = "common";
	private final static String SP_KEY_FISRT_LOADING = "fisrt_loading";
	
	private Context mContext;
	private BleReader mReader;
	private Map<String,Object> mSettingsMap;
	
	public UHFSilionSettingService(Context context, BleReader reader)
	{
		this.mContext = context;
		this.mReader = reader;
		mSettingsMap = new HashMap<String,Object>();
	}

	public void setReader(BleReader mReader){
		this.mReader = mReader;
	}
	
	/**
	 * 初始化配置操作
	 */
	public void effectParams(BleReader reader)
	{
		DLog.d(TAG, "Effect params...");
		this.mReader = reader;
		
		SharedPreferences sp = mContext.getSharedPreferences(SP_NAME_COMMON, Context.MODE_PRIVATE);
		boolean isFirstLoading = sp.getBoolean(SP_KEY_FISRT_LOADING, true);
		
		if(isFirstLoading)
		{

			Log.d(TAG,"first load");
			//第一次加载,从UHF模块中加载配置
			loadParamsFromUHFModule();
			//初始化默认配置(第一次上电时使用)
			initDefaultParams();
			
		}else{
			//加载保存的配置参数
			loadSavedParams();
			//初始化保存配置数据
			initParams();
			//加载模块中的配置参数
//			loadParamsFromUHFModule();
			
		}
		
		//首次加载状态关闭
		sp.edit().putBoolean(SP_KEY_FISRT_LOADING, false).commit();
	}
	
	/**
	 * 加载保存的配置参数
	 */
	private void loadSavedParams()
	{
		//加载"Gen2协议参数"
		int[] datas = getSavedIntArraySettings(UHFSilionParams.POTL_GEN2_SESSION.KEY, UHFSilionParams.POTL_GEN2_SESSION.PARAM_POTL_GEN2_SESSION);
		mSettingsMap.put(UHFSilionParams.POTL_GEN2_SESSION.KEY, datas);
		
		//加载"Gen2协议参数Q值"
		datas = getSavedIntArraySettings(UHFSilionParams.POTL_GEN2_Q.KEY, UHFSilionParams.POTL_GEN2_Q.PARAM_POTL_GEN2_Q);
		mSettingsMap.put(UHFSilionParams.POTL_GEN2_Q.KEY, datas);
		
		//加载"Gen2协议基带编码方式"
		datas = getSavedIntArraySettings(UHFSilionParams.POTL_GEN2_TAGENCODING.KEY, UHFSilionParams.POTL_GEN2_TAGENCODING.PARAM_POTL_GEN2_TAGENCODING);
		mSettingsMap.put(UHFSilionParams.POTL_GEN2_TAGENCODING.KEY, datas);
		
		//加载"支持的最大EPC长度，单位为bit"
		datas = getSavedIntArraySettings(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY,UHFSilionParams.POTL_GEN2_MAXEPCLEN.PARAM_POTL_GEN2_MAXEPCLEN);
		mSettingsMap.put(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY, datas);
		
		//加载"读写器发射功率"Map<String,String[]{"天线ID,读功率,写功率",...}>格式
		load_PARAM_RF_ANTPOWER_Saved();
		
		//加载"读写器最大输出功率"
		datas = getSavedIntArraySettings(UHFSilionParams.RF_MAXPOWER.KEY,UHFSilionParams.RF_MAXPOWER.PARAM_RF_MAXPOWER);
		mSettingsMap.put(UHFSilionParams.RF_MAXPOWER.KEY, datas);
		
		//加载"读写器最小输出功率"
		datas = getSavedIntArraySettings(UHFSilionParams.RF_MINPOWER.KEY,UHFSilionParams.RF_MINPOWER.PARAM_RF_MINPOWER);
		mSettingsMap.put(UHFSilionParams.RF_MINPOWER.KEY, datas);
		
		//标签过滤器，可在对标签进行读，写，锁，盘存操作的时候指定过滤条件
		load_PARAM_TAG_FILTER_Saved();
		
		//在进行gen2标签的盘存操作的同时可以读某个bank的数据。
		load_PARAM_TAG_EMBEDEDDATA_Saved();
		
		//所有被检测到的天线（不是所有的天线都能被检测）
		load_PARAM_READER_CONN_ANTS_Saved();
		
		//加载"天线检测配置"
		datas = getSavedIntArraySettings(UHFSilionParams.READER_IS_CHK_ANT.KEY,UHFSilionParams.READER_IS_CHK_ANT.PARAM_READER_IS_CHK_ANT);
		mSettingsMap.put(UHFSilionParams.READER_IS_CHK_ANT.KEY, datas);
		
		//加载保存的"读写器ip"
		load_PARAM_READER_IP_Saved();
		
		//读写器工作区域
		load_PARAM_FREQUENCY_REGION_Saved();
		
		//加载保存的"读写器跳频表设置"
		load_PARAM_FREQUENCY_HOPTABLE_Saved();
		
		//Gen2协议后向链路速率
		datas = getSavedIntArraySettings(UHFSilionParams.POTL_GEN2_BLF.KEY,UHFSilionParams.POTL_GEN2_BLF.PARAM_POTL_GEN2_BLF);
		mSettingsMap.put(UHFSilionParams.POTL_GEN2_BLF.KEY, datas);
		
		//Gen2协议写模式
		datas = getSavedIntArraySettings(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY,UHFSilionParams.POTL_GEN2_WRITEMODE.PARAM_POTL_GEN2_WRITEMODE);
		mSettingsMap.put(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY, datas);
		
		//Gen2协议目标
		datas = getSavedIntArraySettings(UHFSilionParams.POTL_GEN2_TARGET.KEY,UHFSilionParams.POTL_GEN2_TARGET.PARAM_POTL_GEN2_TARGET);
		mSettingsMap.put(UHFSilionParams.POTL_GEN2_TARGET.KEY, datas);
		
		//对于同一个标签，如果被不同的天线读到是否将做为多条标签数据
		datas = getSavedIntArraySettings(UHFSilionParams.TAGDATA_UNIQUEBYANT.KEY,UHFSilionParams.TAGDATA_UNIQUEBYANT.PARAM_TAGDATA_UNIQUEBYANT);
		mSettingsMap.put(UHFSilionParams.TAGDATA_UNIQUEBYANT.KEY, datas);
		
		//Epc相同的标签如果在使用嵌入盘存读功能时，读出的其它bank数据不同，是否作为多条标签数据
		datas = getSavedIntArraySettings(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY,UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.PARAM_TAGDATA_UNIQUEBYEMDDATA);
		mSettingsMap.put(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY, datas);
		
		//是否只记录最大rssi
		datas = getSavedIntArraySettings(UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.KEY,UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.PARAM_TAGDATA_RECORDHIGHESTRSSI);
		mSettingsMap.put(UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.KEY, datas);
		
		//跳频时间
		datas = getSavedIntArraySettings(UHFSilionParams.RF_HOPTIME.KEY,UHFSilionParams.RF_HOPTIME.PARAM_RF_HOPTIME);
		mSettingsMap.put(UHFSilionParams.RF_HOPTIME.KEY, datas);
		
		//是否启用lbt
		datas = getSavedIntArraySettings(UHFSilionParams.RF_LBT_ENABLE.KEY,UHFSilionParams.RF_LBT_ENABLE.PARAM_RF_LBT_ENABLE);
		mSettingsMap.put(UHFSilionParams.RF_LBT_ENABLE.KEY, datas);
		
		//180006b协议后向链路速率
		datas = getSavedIntArraySettings(UHFSilionParams.POTL_ISO180006B_BLF.KEY,UHFSilionParams.POTL_ISO180006B_BLF.PARAM_POTL_ISO180006B_BLF);
		mSettingsMap.put(UHFSilionParams.POTL_ISO180006B_BLF.KEY, datas);
		
		//Gen2协议Tari
		datas = getSavedIntArraySettings(UHFSilionParams.POTL_GEN2_TARI.KEY,UHFSilionParams.POTL_GEN2_TARI.PARAM_POTL_GEN2_TARI);
		mSettingsMap.put(UHFSilionParams.POTL_GEN2_TARI.KEY, datas);
		
		//天线组
		datas = getSavedIntArraySettings(UHFSilionParams.ANTS.KEY,UHFSilionParams.ANTS.PARAM_ANTS_GROUP);
		if(datas == null || datas.length == 0)
			datas = new int[]{1};
		mSettingsMap.put(UHFSilionParams.ANTS.PARAM_ANTS_GROUP, datas);
		
		//操作的天线
		datas = getSavedIntArraySettings(UHFSilionParams.ANTS.KEY,UHFSilionParams.ANTS.PARAM_OPERATE_ANTS);
		if(datas == null || datas.length == 0)
			datas = new int[]{1};
		mSettingsMap.put(UHFSilionParams.ANTS.PARAM_OPERATE_ANTS, datas[0]);
		
		//盘点超时时间
		long lTimeout = getLongSavedSettings(UHFSilionParams.INV_TIME_OUT.KEY,UHFSilionParams.INV_TIME_OUT.PARAM_INV_TIME_OUT, 50l);
		mSettingsMap.put(UHFSilionParams.INV_TIME_OUT.KEY, lTimeout);
		
		//盘点间隔时间
		long lIntervalTime = getLongSavedSettings(UHFSilionParams.INV_INTERVAL.KEY,UHFSilionParams.INV_INTERVAL.PARAM_INV_INTERVAL_TIME, 0l);
		mSettingsMap.put(UHFSilionParams.INV_INTERVAL.KEY, lIntervalTime);
		
		//盘点快速模式
		int quickMode = (int)getLongSavedSettings(UHFSilionParams.INV_QUICK_MODE.KEY,UHFSilionParams.INV_QUICK_MODE.PARAM_INV_QUICK_MODE, 0l);
		mSettingsMap.put(UHFSilionParams.INV_QUICK_MODE.KEY, quickMode);
			
		//支持的天线最大数
		mSettingsMap.put(UHFSilionParams.ANTS.PARAM_MAX_ANTS_COUNT, 1);
		
		//是否开启低电量时,功率自动调节
		int lowPowerDMEnable = (int)getLongSavedSettings(UHFSilionParams.LOWER_POWER.KEY,UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE, 0L);
		mSettingsMap.put(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE, lowPowerDMEnable);
				
		//低电量值,默认20(20%)
		int lowpowerLevel = (int)getLongSavedSettings(UHFSilionParams.LOWER_POWER.KEY,UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_LEVEL, 20l);
		mSettingsMap.put(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_LEVEL, lowpowerLevel);
				
		//低电量(<20%)时,读功率
		int lowpowerReadDBM = (int)getLongSavedSettings(UHFSilionParams.LOWER_POWER.KEY,UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_READ_DBM, 2000l);
		mSettingsMap.put(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_READ_DBM, lowpowerReadDBM);
	}
	
	/**
	 * 从UHF模块中加载配置
	 */
	private void loadParamsFromUHFModule()
	{
		//从模块中,加载"Gen2协议参数"
		int[] outArrayDatas = new int[]{-1};
		loadIntArrayParamsModule(UHFSilionParams.POTL_GEN2_SESSION.KEY, UHFSilionParams.POTL_GEN2_SESSION.PARAM_POTL_GEN2_SESSION,outArrayDatas);
		
		//从模块中,加载"Gen2协议参数Q值"
		outArrayDatas = new int[]{-1};
		loadIntArrayParamsModule(UHFSilionParams.POTL_GEN2_Q.KEY, UHFSilionParams.POTL_GEN2_Q.PARAM_POTL_GEN2_Q,outArrayDatas);
		
		//从模块中,加载"Gen2协议基带编码方式"
		outArrayDatas = new int[]{-1};
		loadIntArrayParamsModule(UHFSilionParams.POTL_GEN2_TAGENCODING.KEY, UHFSilionParams.POTL_GEN2_TAGENCODING.PARAM_POTL_GEN2_TAGENCODING,outArrayDatas);
		
		//从模块中,加载"支持的最大EPC长度，单位为bit"
		outArrayDatas = new int[]{-1};
		loadIntArrayParamsModule(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY,UHFSilionParams.POTL_GEN2_MAXEPCLEN.PARAM_POTL_GEN2_MAXEPCLEN, outArrayDatas);
		
		//从模块中,加载"读写器发射功率"Map<String,String[]{"天线ID,读功率,写功率",...}>格式
		load_PARAM_RF_ANTPOWER_Module();
		
		//从模块中,加载"读写器最大输出功率"
		outArrayDatas = new int[1];
		loadIntArrayParamsModule(UHFSilionParams.RF_MAXPOWER.KEY,UHFSilionParams.RF_MAXPOWER.PARAM_RF_MAXPOWER, outArrayDatas);
		
		//从模块中,加载"读写器最小输出功率"
		outArrayDatas = new int[1];
		loadIntArrayParamsModule(UHFSilionParams.RF_MINPOWER.KEY,UHFSilionParams.RF_MINPOWER.PARAM_RF_MINPOWER, outArrayDatas);
		
		//从模块中,加载"过滤器"
		load_PARAM_TAG_FILTER_Module();
		
		//从模块中,加载"在进行gen2标签的盘存操作的同时可以读某个bank的数据。"参数值
		load_PARAM_TAG_EMBEDEDDATA_Module();
		
		//从模块中,加载所有被检测到的天线（不是所有的天线都能被检测）
		load_PARAM_READER_CONN_ANTS_Module();
		
		//从模块中,读写器天线端口数
		outArrayDatas = new int[1];
		loadIntArrayParamsModule(UHFSilionParams.READER_AVAILABLE_ANTPORTS.KEY,UHFSilionParams.READER_AVAILABLE_ANTPORTS.PARAM_READER_AVAILABLE_ANTPORTS, outArrayDatas);
		
		//从模块中,加载,读写器ip
		load_PARAM_READER_IP_Module();
		
		//从模块中,加载"读写器工作区域"
		load_PARAM_FREQUENCY_REGION_Module();
		
		//从模块中,加载"读写器跳频表设置"
		load_PARAM_FREQUENCY_HOPTABLE_Module();
		
		//从模块中,加载"Gen2协议后向链路速率"
		outArrayDatas = new int[]{ -1};
		loadIntArrayParamsModule(UHFSilionParams.POTL_GEN2_BLF.KEY,UHFSilionParams.POTL_GEN2_BLF.PARAM_POTL_GEN2_BLF, outArrayDatas);
		
		//从模块中,加载"Gen2协议写模式"
		outArrayDatas = new int[]{ -1};
		loadIntArrayParamsModule(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY,UHFSilionParams.POTL_GEN2_WRITEMODE.PARAM_POTL_GEN2_WRITEMODE, outArrayDatas);
		
		//从模块中,加载"Gen2协议目标"
		outArrayDatas = new int[]{ -1};
		loadIntArrayParamsModule(UHFSilionParams.POTL_GEN2_TARGET.KEY,UHFSilionParams.POTL_GEN2_TARGET.PARAM_POTL_GEN2_TARGET, outArrayDatas);
		
		//从模块中,加载"对于同一个标签，如果被不同的天线读到是否将做为多条标签数据"
		outArrayDatas = new int[]{ -1};
		loadIntArrayParamsModule(UHFSilionParams.TAGDATA_UNIQUEBYANT.KEY,UHFSilionParams.TAGDATA_UNIQUEBYANT.PARAM_TAGDATA_UNIQUEBYANT, outArrayDatas);
		
		//从模块中,加载"Epc相同的标签如果在使用嵌入盘存读功能时，读出的其它bank数据不同，是否作为多条标签数据"
		outArrayDatas = new int[]{ -1};
		loadIntArrayParamsModule(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY,UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.PARAM_TAGDATA_UNIQUEBYEMDDATA, outArrayDatas);
		
		//从模块中,加载"是否只记录最大rssi"
		outArrayDatas = new int[]{ -1};
		loadIntArrayParamsModule(UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.KEY,UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.PARAM_TAGDATA_RECORDHIGHESTRSSI, outArrayDatas);
		
		//从模块中,加载"跳频时间"
		//outArrayDatas = new int[]{ -1};
		//loadIntArrayParamsModule(UHFSilionParams.RF_HOPTIME.KEY,UHFSilionParams.RF_HOPTIME.PARAM_RF_HOPTIME, outArrayDatas);
		
		//从模块中,加载"是否启用lbt"
		//outArrayDatas = new int[]{ -1};
		//loadIntArrayParamsModule(UHFSilionParams.RF_LBT_ENABLE.KEY,UHFSilionParams.RF_LBT_ENABLE.PARAM_RF_LBT_ENABLE, outArrayDatas);
		
		//从模块中,加载"180006b协议后向链路速率"
		outArrayDatas = new int[]{ -1};
		loadIntArrayParamsModule(UHFSilionParams.POTL_ISO180006B_BLF.KEY,UHFSilionParams.POTL_ISO180006B_BLF.PARAM_POTL_ISO180006B_BLF, outArrayDatas);
		
		//从模块中,加载"Gen2协议Tari"
		outArrayDatas = new int[]{ -1};
		loadIntArrayParamsModule(UHFSilionParams.POTL_GEN2_TARI.KEY,UHFSilionParams.POTL_GEN2_TARI.PARAM_POTL_GEN2_TARI, outArrayDatas);
		
		//支持的天线最大数
		mSettingsMap.put(UHFSilionParams.ANTS.PARAM_MAX_ANTS_COUNT, 1);
		
	}//loadParamsFromUHFModule
	
	/**
	 * 初始化默认配置(第一次上电时使用)
	 */
	private void initDefaultParams()
	{
		
		//设置盘存操作的协议------------------------------
		String sValue = ""+TagInfo.SL_TagProtocol.SL_TAG_PROTOCOL_GEN2.value();
		setParam(UHFSilionParams.TAG_INVPOTL.KEY, UHFSilionParams.TAG_INVPOTL.PARAM_TAG_INVPOTL, sValue);
		
		//天线检测配置-------------------------------------
		int checkant=1;
		setParam(UHFSilionParams.READER_IS_CHK_ANT.KEY, UHFSilionParams.READER_IS_CHK_ANT.PARAM_READER_IS_CHK_ANT, String.valueOf(checkant));
		
		//读写器发射功率-------------------------------------
		int[] rpow=new int[]{2700,2000,2000,2000};
		int[] wpow=new int[]{2000,2000,2000,2000};
		int antportc = 1;
		String sAntPowerValue = "";
		JSONArray jsAntArray = new JSONArray();
		for (int i = 0; i < antportc; i++) {
			int antid = i + 1;
			int readPower = rpow[i];
			int writePower = wpow[i];
			try {
				JSONObject jobj = new JSONObject();
				jobj.put("antid", antid);//天线ID
				jobj.put("readPower", readPower);//读功率
				jobj.put("writePower", writePower);//写功率
				jsAntArray.put(jobj);
			} catch (Exception e) {
			}
		}
		
		if(jsAntArray.length() > 0)
			sAntPowerValue = jsAntArray.toString();
		else
			sAntPowerValue = null;
		
		setParam(UHFSilionParams.RF_ANTPOWER.KEY, UHFSilionParams.RF_ANTPOWER.PARAM_RF_ANTPOWER, sAntPowerValue);
		
		//读写器工作区域-------------------------------------
		UHFReader.Region_Conf region = UHFReader.Region_Conf.RG_NA;
		setParam(UHFSilionParams.FREQUENCY_REGION.KEY, UHFSilionParams.FREQUENCY_REGION.PARAM_FREQUENCY_REGION, String.valueOf(region.value()));
		
		//Gen2协议参数Session-------------------------------------
		int session = 0;
		setParam(UHFSilionParams.POTL_GEN2_SESSION.KEY, UHFSilionParams.POTL_GEN2_SESSION.PARAM_POTL_GEN2_SESSION, String.valueOf(session));
		
		//Gen2协议参数Q值-------------------------------------
		int qv = -1;
		setParam(UHFSilionParams.POTL_GEN2_Q.KEY, UHFSilionParams.POTL_GEN2_Q.PARAM_POTL_GEN2_Q, String.valueOf(qv));
		
		//Gen2协议写模式(0表示字写，1表示块写)-------------------------------------
		int wmode = 0;
		setParam(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY, UHFSilionParams.POTL_GEN2_WRITEMODE.PARAM_POTL_GEN2_WRITEMODE, String.valueOf(wmode));
		
		//支持的最大EPC长度，单位为bit-------------------------------------
		int maxlen = 496;
		setParam(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY, UHFSilionParams.POTL_GEN2_MAXEPCLEN.PARAM_POTL_GEN2_MAXEPCLEN, String.valueOf(maxlen));
		
		//Gen2协议目标(0:A; 1:B; 2:A->B; 3:B->A)-------------------------------------
		int target = 0;
		setParam(UHFSilionParams.POTL_GEN2_TARGET.KEY, UHFSilionParams.POTL_GEN2_TARGET.PARAM_POTL_GEN2_TARGET, String.valueOf(target));
		
		//Epc相同的标签如果在使用嵌入盘存读功能时，读出的其它bank数据不同，是否作为多条标签数据-------------------------------------
		//(0:不作为多条标签数据；1：作为多条标签数据)
		int adataq = 0; 
		setParam(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY, UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.PARAM_TAGDATA_UNIQUEBYEMDDATA, String.valueOf(adataq));
		
		//是否只记录最大rssi(0:是 ,1：否)-------------------------------------
		int rhssi = 1;
		setParam(UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.KEY, UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.PARAM_TAGDATA_RECORDHIGHESTRSSI, String.valueOf(rhssi));
		
		//天线组
		setParam(UHFSilionParams.ANTS.KEY, UHFSilionParams.ANTS.PARAM_ANTS_GROUP, "1");
		//操作天线
		setParam(UHFSilionParams.ANTS.KEY, UHFSilionParams.ANTS.PARAM_OPERATE_ANTS, "1");
		
		//盘点超时时间
		setParam(UHFSilionParams.INV_TIME_OUT.KEY, UHFSilionParams.INV_TIME_OUT.PARAM_INV_TIME_OUT, String.valueOf(UHFSilionParams.INV_TIME_OUT.DEFAULT_INV_TIMEOUT));
		//盘点间隔时间
		setParam(UHFSilionParams.INV_INTERVAL.KEY, UHFSilionParams.INV_INTERVAL.PARAM_INV_INTERVAL_TIME, String.valueOf(UHFSilionParams.INV_INTERVAL.DEFAULT_INV_INTERVAL_TIME));
		
		//低电量标准20(20%)
		setParam(UHFSilionParams.LOWER_POWER.KEY, UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_LEVEL, "20");
		//低电量(<20%)时,读功率
		setParam(UHFSilionParams.LOWER_POWER.KEY, UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_READ_DBM, "2000");
		
	}//end initDefaultParams
	
	/**
	 * 初始化读写配置参数
	 */
	private void initParams()
	{
		//从模块中,加载"Gen2协议参数"
		int[] intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.POTL_GEN2_SESSION.KEY);
		String sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.POTL_GEN2_SESSION.KEY, UHFSilionParams.POTL_GEN2_SESSION.PARAM_POTL_GEN2_SESSION, sValue);
		
		//加载"Gen2协议参数Q值"
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.POTL_GEN2_Q.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.POTL_GEN2_Q.KEY, UHFSilionParams.POTL_GEN2_Q.PARAM_POTL_GEN2_Q, sValue);
		
		//加载"Gen2协议基带编码方式"
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.POTL_GEN2_TAGENCODING.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.POTL_GEN2_TAGENCODING.KEY, UHFSilionParams.POTL_GEN2_TAGENCODING.PARAM_POTL_GEN2_TAGENCODING, sValue);
		
		//加载"支持的最大EPC长度，单位为bit"
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY, UHFSilionParams.POTL_GEN2_MAXEPCLEN.PARAM_POTL_GEN2_MAXEPCLEN, sValue);
		
		//加载"读写器发射功率[{"antid":1,"readPower":2600,"writePower":2700},...]格式
		String antPowerItems = (String) mSettingsMap.get(UHFSilionParams.RF_ANTPOWER.KEY);
		if(antPowerItems != null)
			setParam(UHFSilionParams.RF_ANTPOWER.KEY, UHFSilionParams.RF_ANTPOWER.PARAM_RF_ANTPOWER, antPowerItems);
		
		//加载"读写器最大输出功率"
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.RF_MAXPOWER.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.RF_MAXPOWER.KEY, UHFSilionParams.RF_MAXPOWER.PARAM_RF_MAXPOWER, sValue);
		
		//加载"读写器最小输出功率"
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.RF_MINPOWER.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.RF_MINPOWER.KEY, UHFSilionParams.RF_MINPOWER.PARAM_RF_MINPOWER, sValue);
		
		//标签过滤器，可在对标签进行读，写，锁，盘存操作的时候指定过滤条件
		String sFilter = (String) mSettingsMap.get(UHFSilionParams.TAG_FILTER.KEY);
		setParam(UHFSilionParams.TAG_FILTER.KEY, UHFSilionParams.TAG_FILTER.PARAM_TAG_FILTER, sFilter);
		
		//在进行gen2标签的盘存操作的同时可以读某个bank的数据。
		String sEmbedData = (String) mSettingsMap.get(UHFSilionParams.TAG_EMBEDEDDATA.KEY);
		setParam(UHFSilionParams.TAG_EMBEDEDDATA.KEY, UHFSilionParams.TAG_EMBEDEDDATA.PARAM_TAG_EMBEDEDDATA, sEmbedData);
				
		//加载"天线检测配置"
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.READER_IS_CHK_ANT.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.READER_IS_CHK_ANT.KEY, UHFSilionParams.READER_IS_CHK_ANT.PARAM_READER_IS_CHK_ANT, sValue);
		
		//读写器ip地址设置
		String sReaderIp = (String) mSettingsMap.get(UHFSilionParams.READER_IP.KEY);
		if(!TextUtils.isEmpty(sReaderIp))
			setParam(UHFSilionParams.READER_IP.KEY, UHFSilionParams.READER_IP.PARAM_READER_IP, sReaderIp);
		
		//加载"读写器工作区域"
		int[] htb =(int[])mSettingsMap.get(UHFSilionParams.FREQUENCY_HOPTABLE.KEY);//先把保存的区域频率获取出来,不能等到区域设置完成再取
		Object oRegionId =  mSettingsMap.get(UHFSilionParams.FREQUENCY_REGION.KEY);
		if(oRegionId != null){
			int iRegionId = (Integer)oRegionId;
			setParam(UHFSilionParams.FREQUENCY_REGION.KEY, UHFSilionParams.FREQUENCY_REGION.PARAM_FREQUENCY_REGION, String.valueOf(iRegionId));
		}
		
		//读写器跳频表设置
		 String sHtb = converToString(htb);
		 setParam(UHFSilionParams.FREQUENCY_HOPTABLE.KEY, UHFSilionParams.FREQUENCY_HOPTABLE.PARAM_HTB, sHtb);
		 
		 //Gen2协议后向链路速率
		 intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.POTL_GEN2_BLF.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.POTL_GEN2_BLF.KEY, UHFSilionParams.POTL_GEN2_BLF.PARAM_POTL_GEN2_BLF, sValue);
		
		//Gen2协议写模式
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY, UHFSilionParams.POTL_GEN2_WRITEMODE.PARAM_POTL_GEN2_WRITEMODE, sValue);
		
		//Gen2协议目标
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.POTL_GEN2_TARGET.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.POTL_GEN2_TARGET.KEY, UHFSilionParams.POTL_GEN2_TARGET.PARAM_POTL_GEN2_TARGET, sValue);
		
		//对于同一个标签，如果被不同的天线读到是否将做为多条标签数据
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.TAGDATA_UNIQUEBYANT.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.TAGDATA_UNIQUEBYANT.KEY, UHFSilionParams.TAGDATA_UNIQUEBYANT.PARAM_TAGDATA_UNIQUEBYANT, sValue);
		
		//Epc相同的标签如果在使用嵌入盘存读功能时，读出的其它bank数据不同，是否作为多条标签数据
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY, UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.PARAM_TAGDATA_UNIQUEBYEMDDATA, sValue);
		
		//是否只记录最大rssi
		int[] iValue = (int[]) mSettingsMap.get(UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.KEY);
		sValue = converToString(iValue);
		if(sValue != null)
			setParam(UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.KEY, UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.PARAM_TAGDATA_RECORDHIGHESTRSSI, sValue);
		
		//跳频时间
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.RF_HOPTIME.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.RF_HOPTIME.KEY, UHFSilionParams.RF_HOPTIME.PARAM_RF_HOPTIME, sValue);
		
		//是否启用lbt
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.RF_LBT_ENABLE.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.RF_LBT_ENABLE.KEY, UHFSilionParams.RF_LBT_ENABLE.PARAM_RF_LBT_ENABLE, sValue);
		
		//180006b协议后向链路速率
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.POTL_ISO180006B_BLF.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.POTL_ISO180006B_BLF.KEY, UHFSilionParams.POTL_ISO180006B_BLF.PARAM_POTL_ISO180006B_BLF, sValue);
		
		//Gen2协议Tari
		intArrayDatas = (int[]) mSettingsMap.get(UHFSilionParams.POTL_GEN2_TARI.KEY);
		sValue = converToString(intArrayDatas);
		if(sValue != null)
			setParam(UHFSilionParams.POTL_GEN2_TARI.KEY, UHFSilionParams.POTL_GEN2_TARI.PARAM_POTL_GEN2_TARI, sValue);
		
	}//initParams()
	
	
	/**
	 * 设置参数
	 * @param paramKey 参数组的唯一标识
	 * @param subParamName 参数属性　
	 * @param sValue 参数值
	 * @return ({@link UHFReader.READER_STATE})
	 */
	public UHFReader.READER_STATE setParam(String paramKey,String subParamName,String sValue)
	{
		DLog.d(TAG, "Enter set param.Param key : "+paramKey+", Sub param name : "+subParamName+",Str value : "+sValue);
		
		UHFReader.READER_STATE state = UHFReader.READER_STATE.INVALID_PARA ;
		
		try {
			
			//Gen2协议参数Session
			if(paramKey.equals(UHFSilionParams.POTL_GEN2_SESSION.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION, iValueArray);
			}
			
			//Gen2协议参数Q值
			if(paramKey.equals(UHFSilionParams.POTL_GEN2_Q.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_POTL_GEN2_Q, iValueArray);
			}
			
			//Gen2协议基带编码方式
			if(paramKey.equals(UHFSilionParams.POTL_GEN2_TAGENCODING.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_POTL_GEN2_TAGENCODING, iValueArray);
			}
			
			//支持的最大EPC长度，单位为bit
			if(paramKey.equals(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_POTL_GEN2_MAXEPCLEN, iValueArray);
			}
			
			//读写器发射功率
			if(paramKey.equals(UHFSilionParams.RF_ANTPOWER.KEY)){
				state = set_PARAM_RF_ANTPOWER(sValue);
			}
			
			//标签过滤器，可在对标签进行读，写，锁，盘存操作的时候指定过滤条件
			if(paramKey.equals(UHFSilionParams.TAG_FILTER.KEY)){
				state = set_PARAM_TAG_FILTER(subParamName, sValue);
			}
			
			//在进行gen2标签的盘存操作的同时可以读某个bank的数据(附加数据)
			if(paramKey.equals(UHFSilionParams.TAG_EMBEDEDDATA.KEY)){
				state = set_PARAM_TAG_EMBEDEDDATA(subParamName, sValue);
			}
			
			//设置盘存操作的协议（仅仅M6e架构的读写器支持的参数）
			if(paramKey.equals(UHFSilionParams.TAG_INVPOTL.KEY)){
				state = set_PARAM_TAG_INVPOTL(subParamName, sValue);
			}
			
			//天线检测配置
			if(paramKey.equals(UHFSilionParams.READER_IS_CHK_ANT.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_READER_IS_CHK_ANT, iValueArray);
			}
			
			//读写器ip地址设置
			if(paramKey.equals(UHFSilionParams.READER_IP.KEY)){
				state = set_PARAM_READER_IP(subParamName, sValue);
			}
			
			//读写器工作区域
			if(paramKey.equals(UHFSilionParams.FREQUENCY_REGION.KEY)){
				state = set_FREQUENCY_REGION(subParamName, sValue);
			}
			
			//读写器跳频表设置
			if(paramKey.equals(UHFSilionParams.FREQUENCY_HOPTABLE.KEY)){
				state = set_FREQUENCY_HOPTABLE(subParamName, sValue);
			}
			
			//Gen2协议后向链路速率
			if(paramKey.equals(UHFSilionParams.POTL_GEN2_BLF.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_POTL_GEN2_BLF, iValueArray);
			}
			
			//Gen2协议写模式
			if(paramKey.equals(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_POTL_GEN2_WRITEMODE, iValueArray);
			}
			
			//Gen2协议目标
			if(paramKey.equals(UHFSilionParams.POTL_GEN2_TARGET.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_POTL_GEN2_TARGET, iValueArray);
			}
			
			//对于同一个标签，如果被不同的天线读到是否将做为多条标签数据
			if(paramKey.equals(UHFSilionParams.TAGDATA_UNIQUEBYANT.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_TAGDATA_UNIQUEBYANT, iValueArray);
			}
			
			//Epc相同的标签如果在使用嵌入盘存读功能时，读出的其它bank数据不同，是否作为多条标签数据
			if(paramKey.equals(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_TAGDATA_UNIQUEBYEMDDATA, iValueArray);
			}
			
			//是否只记录最大rssi
			if(paramKey.equals(UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_TAGDATA_RECORDHIGHESTRSSI, iValueArray);
			}
			
			//跳频时间
			if(paramKey.equals(UHFSilionParams.RF_HOPTIME.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_RF_HOPTIME, iValueArray);
			}
			
			//是否启用lbt
			if(paramKey.equals(UHFSilionParams.RF_LBT_ENABLE.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_RF_LBT_ENABLE, iValueArray);
			}
			
			//Gen2协议Tari
			if(paramKey.equals(UHFSilionParams.POTL_GEN2_TARI.KEY)){
				int[] iValueArray = convertToIntArray(sValue, ",");
				state = setIntArrayParam(paramKey,Mtr_Param.MTR_PARAM_POTL_GEN2_TARI, iValueArray);
			}
			
			//天线
			if(paramKey.equals(UHFSilionParams.ANTS.KEY))
			{
				int[]  iValueArray = convertToIntArray(sValue, ",");
				//天线集合
				if(subParamName.equals(UHFSilionParams.ANTS.PARAM_ANTS_GROUP))
				{
					mSettingsMap.put(UHFSilionParams.ANTS.PARAM_ANTS_GROUP, iValueArray);
					state = UHFReader.READER_STATE.OK_ERR;
				}
				
				//操作的天线
				if(subParamName.equals(UHFSilionParams.ANTS.PARAM_OPERATE_ANTS))
				{
					if(iValueArray == null || iValueArray.length == 0)
						iValueArray = new int[]{1};
					mSettingsMap.put(UHFSilionParams.ANTS.PARAM_OPERATE_ANTS, iValueArray[0]);
					state = UHFReader.READER_STATE.OK_ERR;
				}
				
			}
			
			//盘点超时时间
			if(paramKey.equals(UHFSilionParams.INV_TIME_OUT.KEY))
			{
				if(TextUtils.isDigitsOnly(sValue))
				{

					long intevalTime =getLongParamValue(UHFSilionParams.INV_INTERVAL.KEY,
							UHFSilionParams.INV_INTERVAL.PARAM_INV_INTERVAL_TIME,
							UHFSilionParams.INV_INTERVAL.DEFAULT_INV_INTERVAL_TIME);

					mReader.StartReading((short) Long.parseLong(sValue),(short) intevalTime);
					mSettingsMap.put(UHFSilionParams.INV_TIME_OUT.KEY, Long.parseLong(sValue));
					state = UHFReader.READER_STATE.OK_ERR;
				}
			}
			
			//盘点间隔时间
			if(paramKey.equals(UHFSilionParams.INV_INTERVAL.KEY))
			{
				if(TextUtils.isDigitsOnly(sValue))
				{
					long readTimeout = getLongParamValue(UHFSilionParams.INV_TIME_OUT.KEY, UHFSilionParams.INV_TIME_OUT.PARAM_INV_TIME_OUT,
							UHFSilionParams.INV_TIME_OUT.DEFAULT_INV_TIMEOUT);


					mReader.StartReading((short) readTimeout,(short) Long.parseLong(sValue));
					mSettingsMap.put(UHFSilionParams.INV_INTERVAL.KEY, Long.parseLong(sValue));
					state = UHFReader.READER_STATE.OK_ERR;
				}
			}
			
			//快速盘点模式,1:开启,0关闭
			if(paramKey.equals(UHFSilionParams.INV_QUICK_MODE.KEY))
			{
				state = set_INV_QUICK_MODE(subParamName, sValue);
			}
			
			//是否开启低电量时,功率自动调节
			if(subParamName.equals(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE))
			{
				mSettingsMap.put(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE, Integer.parseInt(sValue));
				state = UHFReader.READER_STATE.OK_ERR;
			}
			
			//低电量值标准,默认20(20%)
			if(subParamName.equals(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_LEVEL))
			{
				mSettingsMap.put(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_LEVEL, Integer.parseInt(sValue));
				state = UHFReader.READER_STATE.OK_ERR;
			}
			
			//低电量(<20%)的读功率
			if(subParamName.equals(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_READ_DBM))
			{
				mSettingsMap.put(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_READ_DBM, Integer.parseInt(sValue));
				state = UHFReader.READER_STATE.OK_ERR;
			}
			
			//保存配置,且更新内存中的配置
			if(state == UHFReader.READER_STATE.OK_ERR)
				saveSettings(paramKey, subParamName, sValue);
			
			DLog.d(TAG, "Set param completed.state : "+state.toString()+"' , ok : "+(state == UHFReader.READER_STATE.OK_ERR));
			
		} catch (Exception e) {
			Log.w(TAG, "Set param failed.Param key : "+paramKey+", Sub param name : "+subParamName+",Str value : "+sValue, e);
		}
		
		return state;
	}
	
	/**
	 * 获取提定的参数值
	 * @param paramKey
	 * @param paramName
	 * @return
	 */
	public String getParam(String paramKey, String paramName)
	{
		if(UHFSilionParams.TEMPTURE.KEY.equals(paramKey))
		{
			try {
				
				int[] val = new int[1];
				val[0] = 0;
				READER_ERR er = mReader.ParamGet(Mtr_Param.MTR_PARAM_RF_TEMPERATURE, val);
				if (er == READER_ERR.MT_OK_ERR) {
					mSettingsMap.put(UHFSilionParams.TEMPTURE.KEY, val[0]);
					return String.valueOf(val[0]);
				}else
					DLog.d(TAG, "Get tempture failed : "+er.toString());
				return null;
			} catch (Exception e) {
			}
		}


		if(UHFSilionParams.CHARGE_VALUE.KEY.equals(paramKey))
		{
			try {

				int[] val = new int[1];
				val[0] = 0;
				READER_ERR er = mReader.GetCharge( val);
				if (er == READER_ERR.MT_OK_ERR) {
					mSettingsMap.put(UHFSilionParams.CHARGE_VALUE.KEY, val[0]);
					return String.valueOf(val[0]);
				}else
					DLog.d(TAG, "Get charge failed : "+er.toString());
				return null;
			} catch (Exception e) {
			}
		}




		
		if(mSettingsMap != null)
		{
			Object obj = mSettingsMap.get(paramKey);
			if(obj == null)
				obj = mSettingsMap.get(paramName);
			if(obj != null)
			{
				if((obj instanceof Integer) || (obj instanceof Long))
				{
					return String.valueOf(obj);
				}
				if(obj instanceof int[])
				{
					int[] iArray = (int[])obj;
					String sArray = converToString(iArray);
					return sArray;
				}
				if(obj instanceof String)
				{
					return (String)obj;
				}
				
			}//end if;
			
		}
		return null;
	}//end getParam
	
	/**
	 * 获取所有设置
	 * @return Map<String,Object>(Map<参数ID,值>)
	 */
	public Map<String,Object> getAllSettings()
	{
		return mSettingsMap;
	}
	
	/**
	 * 获取指定的属性值
	 * @param paramKey
	 * @param paramName
	 * @return
	 */
	public Object getParamValue(String paramKey,String paramName)
	{
		Object obj = mSettingsMap.get(paramKey);
		if(obj instanceof Map)
		{
			Map<String,String> sMap = (Map<String,String>)obj;
			return sMap.get(paramName);
		}
		
		return obj;
	}
	
	public long getLongParamValue(String paramKey,String paramName,long defaultValue)
	{
		Object obj = mSettingsMap.get(paramKey);
		if(obj == null)
			return defaultValue;
		try {
			return (Long)obj;
		} catch (Exception e) {
		}
		return defaultValue;
	}
	
	/**
	 * 清空所有保存的数据
	 * @return
	 */
	public boolean clearSavedUHFSettings()
	{
				//清空:"Gen2协议参数"
				clearSavedSettings(UHFSilionParams.POTL_GEN2_SESSION.KEY);
				
				//清空:"Gen2协议参数Q值"
				clearSavedSettings(UHFSilionParams.POTL_GEN2_Q.KEY);
				
				//清空:"Gen2协议基带编码方式"
				clearSavedSettings(UHFSilionParams.POTL_GEN2_TAGENCODING.KEY);
				
				//清空:"支持的最大EPC长度，单位为bit"
				clearSavedSettings(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY);
				
				//加载"读写器发射功率"Map<String,String[]{"天线ID,读功率,写功率",...}>格式
				clearSavedSettings(UHFSilionParams.RF_ANTPOWER.KEY);
				
				//清空:"读写器最大输出功率"
				clearSavedSettings(UHFSilionParams.RF_MAXPOWER.KEY);
				
				//清空:"读写器最小输出功率"
				clearSavedSettings(UHFSilionParams.RF_MINPOWER.KEY);
				
				//清空:标签过滤器，可在对标签进行读，写，锁，盘存操作的时候指定过滤条件
				clearSavedSettings(UHFSilionParams.TAG_FILTER.KEY);
				
				//清空:在进行gen2标签的盘存操作的同时可以读某个bank的数据。
				clearSavedSettings(UHFSilionParams.TAG_EMBEDEDDATA.KEY);
				
				//清空:所有被检测到的天线（不是所有的天线都能被检测）
				clearSavedSettings(UHFSilionParams.READER_CONN_ANTS.KEY);
				
				//清空:"天线检测配置"
				clearSavedSettings(UHFSilionParams.READER_IS_CHK_ANT.KEY);
				
				//清空:保存的"读写器ip"
				clearSavedSettings(UHFSilionParams.READER_IP.KEY);
				
				//清空:读写器工作区域
				clearSavedSettings(UHFSilionParams.FREQUENCY_REGION.KEY);
				
				//清空:保存的"读写器跳频表设置"
				clearSavedSettings(UHFSilionParams.FREQUENCY_HOPTABLE.KEY);
				
				//清空:Gen2协议后向链路速率
				clearSavedSettings(UHFSilionParams.POTL_GEN2_BLF.KEY);
				
				//清空:Gen2协议写模式
				clearSavedSettings(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY);
				
				//清空:Gen2协议目标
				clearSavedSettings(UHFSilionParams.POTL_GEN2_TARGET.KEY);
				
				//清空:对于同一个标签，如果被不同的天线读到是否将做为多条标签数据
				clearSavedSettings(UHFSilionParams.TAGDATA_UNIQUEBYANT.KEY);
				
				//清空:Epc相同的标签如果在使用嵌入盘存读功能时，读出的其它bank数据不同，是否作为多条标签数据
				clearSavedSettings(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY);
				
				//清空:是否只记录最大rssi
				clearSavedSettings(UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.KEY);
				
				//清空:跳频时间
				clearSavedSettings(UHFSilionParams.RF_HOPTIME.KEY);
				
				//清空:是否启用lbt
				clearSavedSettings(UHFSilionParams.RF_LBT_ENABLE.KEY);
				
				//清空:180006b协议后向链路速率
				clearSavedSettings(UHFSilionParams.POTL_ISO180006B_BLF.KEY);
				
				//清空:Gen2协议Tari
				clearSavedSettings(UHFSilionParams.POTL_GEN2_TARI.KEY);
				
				//清空:天线组
				clearSavedSettings(UHFSilionParams.ANTS.KEY);
				
				//清空:操作的天线
				clearSavedSettings(UHFSilionParams.ANTS.KEY);
				
				//清空:盘点超时时间
				clearSavedSettings(UHFSilionParams.INV_TIME_OUT.KEY);
				
				//清空:盘点间隔时间
				clearSavedSettings(UHFSilionParams.INV_INTERVAL.KEY);
				
				//清空:盘点快速模式
				clearSavedSettings(UHFSilionParams.INV_QUICK_MODE.KEY);
				
				//清空:是否开启低电量时,功率自动调节
				clearSavedSettings(UHFSilionParams.LOWER_POWER.KEY);
				
				//清空:其他信息
				clearSavedSettings(SP_NAME_COMMON);
				
				//清空:所有设置的缓存
				mSettingsMap.clear();
				
				return true;
	}
	
	/**
	 * 设置int[]类型的参数
	 * @param mtrParam 参数名
	 * @param iValueArray　参数值int[]
	 * @return ({@link UHFReader.READER_STATE})
	 */
	private UHFReader.READER_STATE setIntArrayParam(String paramKey,Mtr_Param mtrParam,int[] iValueArray)
	{
		try {
			if(iValueArray != null && iValueArray.length > 0)
			{
				Reader.READER_ERR er = mReader.ParamSet(mtrParam, iValueArray);
				if(er == Reader.READER_ERR.MT_OK_ERR)
					mSettingsMap.put(paramKey, iValueArray);
				
				return UHFReader.READER_STATE.valueOf(er.value());
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return UHFReader.READER_STATE.CMD_FAILED_ERR;
	}
	
	/**
	 * 设置读写器发射功率
	 * @param sValue JSON数组[{"antid":1,"readPower":2600,"writePower":2700},...]
	 * @return ({@link UHFReader.READER_STATE})
	 */
	private UHFReader.READER_STATE set_PARAM_RF_ANTPOWER(String sValue)
	{
		DLog.d(TAG, "Enter set_PARAM_RF_ANTPOWER...");
		if(TextUtils.isEmpty(sValue))
			return  UHFReader.READER_STATE.INVALID_PARA;
		
		try {
			
			
			JSONArray antPowerJSArray = new JSONArray(sValue);
			int len = antPowerJSArray.length();
			Reader.AntPower[] antPowers = new Reader.AntPower[len];
			for(int i =0 ;i<len;i++)
			{
				JSONObject jsItem = antPowerJSArray.getJSONObject(i);
				
				Reader.AntPower antPower = mReader.new AntPower();
				antPower.antid = jsItem.optInt("antid"); //天线ID
				antPower.readPower = (short) jsItem.optInt("readPower");//读功率
				antPower.writePower = (short) jsItem.optInt("writePower");//写功率
				antPowers[i] = antPower;
			}
			
			Reader.AntPowerConf powerConf = mReader.new AntPowerConf();
			powerConf.antcnt = antPowers.length;
			powerConf.Powers = antPowers;
			
			Reader.READER_ERR er= mReader.ParamSet(Mtr_Param.MTR_PARAM_RF_ANTPOWER, powerConf);
			
			if(er == Reader.READER_ERR.MT_OK_ERR)
				mSettingsMap.put(UHFSilionParams.RF_ANTPOWER.KEY, sValue);
			
			return UHFReader.READER_STATE.valueOf(er.value());
			
		} catch (Exception e) {
			Log.w(TAG, "set_PARAM_RF_ANTPOWER failed.", e);
		}
		
		return UHFReader.READER_STATE.CMD_FAILED_ERR;
	}
	
	/**
	 * 设置过滤器
	 * @param paramName
	 * @param sValue json格式存放,{"bank":1,"fdata":0A0B,"flen:"8,"startaddr":2,"isInvert":1}
	 * @return ({@link UHFReader.READER_STATE})
	 */
	private UHFReader.READER_STATE set_PARAM_TAG_FILTER(String paramName,String sValue)
	{
		DLog.d(TAG, "Enter set_PARAM_TAG_FILTER...");
	    
	    try {
	    	
	    	Reader.TagFilter_ST tagFilter = null;
	    	//移除过滤内容
	    	if(sValue == null || (paramName.equals(UHFSilionParams.TAG_FILTER.PARAM_CLEAR) && "1".equals(sValue)))
	    	{
	    		tagFilter = null;
	    	}else{
	    		JSONObject jsFilter = new JSONObject(sValue);
				tagFilter = mReader.new TagFilter_ST();
				tagFilter.bank = jsFilter.optInt("bank");
				tagFilter.startaddr = jsFilter.optInt("startaddr");
				 String sHexData = jsFilter.optString("fdata");
				if(sHexData != null)
				{
					byte[] fdata = UHFReader.Str2Hex(sHexData);
					tagFilter.fdata = fdata;
					tagFilter.flen = fdata.length*8;
				}
				
				tagFilter.isInvert = jsFilter.getInt("isInvert");
	    	}
			
			Reader.READER_ERR er = mReader.ParamSet(Mtr_Param.MTR_PARAM_TAG_FILTER, tagFilter);
			if(er == Reader.READER_ERR.MT_OK_ERR)
			{
				if(tagFilter != null)
				{
					mSettingsMap.put(UHFSilionParams.TAG_FILTER.KEY, sValue);
				}else{
					mSettingsMap.remove(UHFSilionParams.TAG_FILTER.KEY);
				}
			}
			
			return UHFReader.READER_STATE.valueOf(er.value());
			
		} catch (Exception e) {
			Log.w(TAG, "set_PARAM_TAG_FILTER failed.", e);
		}
	    
	    return UHFReader.READER_STATE.INVALID_PARA;
	}
	
	/**
	 * 设置在进行gen2标签的盘存操作的同时可以读某个bank的数据
	 * @param paramName
	 * @param sValue  JSON格式,{"bank":1,"startaddr":2,"bytecnt":8,"accesspwd":"0A0B"}
	 * @return ({@link UHFReader.READER_STATE})
	 */
	private UHFReader.READER_STATE set_PARAM_TAG_EMBEDEDDATA(String paramName,String sValue)
	{
			DLog.d(TAG, "Enter set_PARAM_TAG_EMBEDEDDATA...");
		    
		    try {
		    	Reader.EmbededData_ST embededData = null;
		    	Reader.READER_ERR er ;
		    	if(sValue == null || (paramName.equals(UHFSilionParams.TAG_EMBEDEDDATA.PARAM_CLEAR) && "1".equals(sValue)))
		    	{
		    		embededData = null;
		    	}else{
		    		embededData = mReader.new EmbededData_ST();
		    		JSONObject jsItem = new JSONObject(sValue);
			    	embededData.bank = jsItem.optInt("bank");
			    	embededData.startaddr = jsItem.optInt("startaddr");
			    	embededData.bytecnt = jsItem.optInt("bytecnt");
			    	String sHexAccessPasswd = jsItem.optString("accesspwd");
					if(sHexAccessPasswd != null)
					{
						byte[] fdata = UHFReader.Str2Hex(sHexAccessPasswd);
						embededData.accesspwd = fdata;
					}
					
		    	}
		    	
		    	er = mReader.ParamSet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, embededData);
		    	
				if(er == Reader.READER_ERR.MT_OK_ERR)
				{
					if( embededData != null )
					{
						mSettingsMap.put(UHFSilionParams.TAG_EMBEDEDDATA.KEY, sValue);
					}else
						mSettingsMap.remove(UHFSilionParams.TAG_EMBEDEDDATA.KEY);
				}
				return UHFReader.READER_STATE.valueOf(er.value());
				
			} catch (Exception e) {
				Log.w(TAG, "set_PARAM_TAG_EMBEDEDDATA failed.", e);
			}

		    return UHFReader.READER_STATE.INVALID_PARA;
		    
	}//end set_PARAM_TAG_EMBEDEDDATA
	
	/**
	 * 设置盘存操作的协议（仅仅M6e架构的读写器支持的参数）
	 * @param paramName
	 * @param sValue 1,2,3形式串
	 * @return ({@link UHFReader.READER_STATE})
	 */
	private UHFReader.READER_STATE set_PARAM_TAG_INVPOTL(String paramName,String sValue)
	{
		DLog.d(TAG, "Enter set_PARAM_TAG_INVPOTL...");
		try {
			
			if(!TextUtils.isEmpty(sValue))
			{
				int [] invPotlItems = convertToIntArray(sValue, ",");
				Reader.Inv_Potls_ST ipst = mReader.new Inv_Potls_ST();
				Reader.Inv_Potl[] ipts = new Reader.Inv_Potl[ invPotlItems == null ? 0 : invPotlItems.length];
				for(int i =0 ; i < ipts.length; i++)
				{
					Reader.Inv_Potl ipt = mReader.new Inv_Potl();
					ipt.potl = Reader.SL_TagProtocol.valueOf(invPotlItems[i]);
					ipt.weight = 30;
					ipts[0] = ipt;
				}
				ipst.potls = ipts;
				ipst.potlcnt = ipts.length;
				
				Reader.READER_ERR er = mReader.ParamSet(Mtr_Param.MTR_PARAM_TAG_INVPOTL, ipst);
				
				if(er == Reader.READER_ERR.MT_OK_ERR)
					mSettingsMap.put(UHFSilionParams.TAG_INVPOTL.KEY, invPotlItems);//int[]
				
				return UHFReader.READER_STATE.valueOf(er.value());
				
			}//end if
			
		} catch (Exception e) {
			Log.w(TAG, "set_PARAM_TAG_INVPOTL failed.", e);
		}
		
		return UHFReader.READER_STATE.INVALID_PARA;
	}
	
	/**
	 * 设置读写器ip地址
	 * @param paramName 参数名
	 * @param sValue　参数值 ,json格式保存{"ip":"0A0B","mask":"0B0C","gateway":"0C0D"}
	 * @return ({@link UHFReader.READER_STATE})　
	 */
	private UHFReader.READER_STATE set_PARAM_READER_IP(String paramName,String sValue)
	{
		DLog.d(TAG, "Enter set_PARAM_READER_IP...");
		if(TextUtils.isEmpty(sValue))
			return UHFReader.READER_STATE.INVALID_PARA;
	    
	    try {
	    	
	    	JSONObject jsItem = new JSONObject(sValue);
	    	Reader.Reader_Ip readerIp = mReader.new Reader_Ip();
	    	String sHexIp = jsItem.optString("ip");
	    	String sHexMask = jsItem.optString("mask");
	    	String sHexGateway = jsItem.optString("gateway");
			if(sHexIp != null)
			{
				byte[] byteIP = UHFReader.Str2Hex(sHexIp);
				readerIp.ip = byteIP;
			}
			
			if(sHexMask != null)
			{
				byte[] byteMask = UHFReader.Str2Hex(sHexMask);
				readerIp.mask = byteMask;
			}
			
			if(sHexGateway != null)
			{
				byte[] byteGateWay = UHFReader.Str2Hex(sHexGateway);
				readerIp.gateway = byteGateWay;
			}
			Reader.READER_ERR er = mReader.ParamSet(Mtr_Param.MTR_PARAM_READER_IP, readerIp);
			
			return UHFReader.READER_STATE.valueOf(er.value());
			
		} catch (Exception e) {
			Log.w(TAG, "set_PARAM_READER_IP failed.", e);
		}

	    return UHFReader.READER_STATE.INVALID_PARA;
	    
	}//end set_PARAM_READER_IP
	
	/**
	 * 设置读写器工作区域
	 * @param paramName 参数名
	 * @param sValue　参数值
	 * @return ({@link UHFReader.READER_STATE})　
	 */
	private UHFReader.READER_STATE set_FREQUENCY_REGION(String paramName,String sValue)
	{
		DLog.d(TAG, "Enter set_FREQUENCY_REGION...");
		try {
			if(sValue != null && TextUtils.isDigitsOnly(sValue))
			{
				Reader.Region_Conf region_conf = Reader.Region_Conf.RG_NONE;
				
				int iValue = Integer.parseInt(sValue);
				UHFReader.Region_Conf uRegionConf = UHFReader.Region_Conf.valueOf(iValue);
				DLog.d(TAG, "Region_Conf: "+uRegionConf.toString());
				if(uRegionConf == UHFReader.Region_Conf.RG_EU)
					region_conf = Reader.Region_Conf .RG_EU;
				else if(uRegionConf == UHFReader.Region_Conf.RG_EU2)
					region_conf = Reader.Region_Conf .RG_EU2;
				else if(uRegionConf == UHFReader.Region_Conf.RG_EU3)
					region_conf = Reader.Region_Conf .RG_EU3;
				else if(uRegionConf == UHFReader.Region_Conf.RG_KR)
					region_conf = Reader.Region_Conf .RG_KR;
				else if(uRegionConf == UHFReader.Region_Conf.RG_NA)
					region_conf = Reader.Region_Conf .RG_NA;
				else if(uRegionConf == UHFReader.Region_Conf.RG_NONE)
					region_conf = Reader.Region_Conf .RG_NONE;
				else if(uRegionConf == UHFReader.Region_Conf.RG_OPEN)
					region_conf = Reader.Region_Conf .RG_OPEN;
				else if(uRegionConf == UHFReader.Region_Conf.RG_PRC)
					region_conf = Reader.Region_Conf .RG_PRC;
				else if(uRegionConf == UHFReader.Region_Conf.RG_PRC2)
					region_conf = Reader.Region_Conf .RG_PRC2;
				
				DLog.d(TAG, "Real Region_Conf: "+region_conf.toString());
				
				Reader.READER_ERR er = mReader.ParamSet(Mtr_Param.MTR_PARAM_FREQUENCY_REGION, region_conf);
				if(er == Reader.READER_ERR.MT_OK_ERR){
					mSettingsMap.put(UHFSilionParams.FREQUENCY_REGION.KEY, iValue);
					load_PARAM_FREQUENCY_HOPTABLE_Module();
				}
				return UHFReader.READER_STATE.valueOf(er.value());
			}
		} catch (Exception e) {
			Log.w(TAG, "set_FREQUENCY_REGION failed.", e);
		}
		 return UHFReader.READER_STATE.INVALID_PARA;
		 
	}//set_FREQUENCY_REGION
	
	/**
	 * 设置读写器跳频表设置
	 * @param paramName 参数名
	 * @param sValue　参数值
	 * @return ({@link UHFReader.READER_STATE})　
	 */
	private UHFReader.READER_STATE set_FREQUENCY_HOPTABLE(String paramName,String sValue)
	{
		DLog.d(TAG, "Enter set_FREQUENCY_HOPTABLE...");
			
		try {
			Reader.HoptableData_ST hoptableData = mReader.new HoptableData_ST();
			int[] htbs = convertToIntArray(sValue, ",");
			if(htbs != null)
			{
				int lenhtb = htbs.length;
				hoptableData.htb = htbs;
				hoptableData.lenhtb = lenhtb;
			}
			Reader.READER_ERR er = mReader.ParamSet(Mtr_Param.MTR_PARAM_FREQUENCY_HOPTABLE, hoptableData);
			if(er == Reader.READER_ERR.MT_OK_ERR){
				mSettingsMap.put(UHFSilionParams.FREQUENCY_HOPTABLE.KEY, htbs);
				load_PARAM_FREQUENCY_HOPTABLE_Module();
			}
			return UHFReader.READER_STATE.valueOf(er.value());
		} catch (Exception e) {
			Log.w(TAG, "set_FREQUENCY_HOPTABLE failed.", e);
		}
		
		return UHFReader.READER_STATE.INVALID_PARA;
		
	}//end set_FREQUENCY_HOPTABLE
	
	/**
	 * 设置快速盘点模式
	 * @param paramName 参数名
	 * @param sValue　参数值
	 * @return ({@link UHFReader.READER_STATE})　
	 */
	private UHFReader.READER_STATE set_INV_QUICK_MODE(String paramName,String sValue)
	{
		DLog.d(TAG, "Enter set_INV_QUICK_MODE...");
		
		int iValue = 0;
		if(sValue != null && TextUtils.isDigitsOnly(sValue))
			iValue = Integer.parseInt(sValue);


		mSettingsMap.put(UHFSilionParams.INV_QUICK_MODE.KEY, iValue);
		if(iValue == 0)
		{


			long readTimeout = getLongParamValue(UHFSilionParams.INV_TIME_OUT.KEY, UHFSilionParams.INV_TIME_OUT.PARAM_INV_TIME_OUT,
					UHFSilionParams.INV_TIME_OUT.DEFAULT_INV_TIMEOUT);
			long intevalTime =getLongParamValue(UHFSilionParams.INV_INTERVAL.KEY,
					UHFSilionParams.INV_INTERVAL.PARAM_INV_INTERVAL_TIME,
					UHFSilionParams.INV_INTERVAL.DEFAULT_INV_INTERVAL_TIME);

			mReader.StartReading((short) readTimeout,(short) intevalTime);
			return UHFReader.READER_STATE.OK_ERR;
		}
		else{
			mReader.AsyncStartReading();
			return UHFReader.READER_STATE.OK_ERR;
		}






		
//		int[] mp = null;
//		Object obj = mSettingsMap.get(UHFSilionParams.RF_MAXPOWER.KEY);
//		if(obj != null)
//			mp = (int[])obj;
		
//		if(mp != null && mp.length > 0)
//		{
//			int maxMP = mp[0];//最大读写功率
//			obj = mSettingsMap.get(UHFSilionParams.ANTS.PARAM_ANTS_GROUP);
//			int[] ants = new int[]{1};
//			if(obj != null)
//				ants = (int[])obj;
//			int antCount = ants.length; //天线数
//
//			try {
//				String sJSONArray = (String) mSettingsMap.get(UHFSilionParams.RF_ANTPOWER.KEY);
//				JSONArray jsArray = new JSONArray();
//				int readPower = maxMP;//maxMP;
//				int writePower = maxMP;//maxMP;
//				if(sJSONArray != null)
//				{
//					jsArray = new JSONArray(sJSONArray);
//					int count = jsArray.length();
//					for(int i =0; i < count ;i++)
//					{
//						JSONObject jobj = jsArray.optJSONObject(i);
//						jobj.put("readPower", readPower);
//					}
//				}else{
//					for (int i = 0; i < antCount; i++) {
//						JSONObject jobj = new JSONObject();
//						int antid =  i + 1;
//						jobj.put("antid", antid);
//						jobj.put("readPower", readPower);
//						jobj.put("writePower", writePower);
//						jsArray.put(jobj);
//					}
//				}
//
//				//设置天线为最大功率
//				String sRFPowerValue =jsArray.toString();
//				UHFReader.READER_STATE er = setParam(UHFSilionParams.RF_ANTPOWER.KEY, UHFSilionParams.RF_ANTPOWER.PARAM_RF_ANTPOWER, sRFPowerValue);
//
//				if(er==UHFReader.READER_STATE.OK_ERR)
//				{
//					DLog.d(TAG, "set RF Power : "+maxMP);
//					//设置盘点间隔为0
//					er = setParam(UHFSilionParams.INV_INTERVAL.KEY, UHFSilionParams.INV_INTERVAL.PARAM_INV_INTERVAL_TIME, "0");
//					if(er==UHFReader.READER_STATE.OK_ERR)
//						DLog.d(TAG, "set inventory interval time : 0 ms ");
//				}
//
//				if(er == UHFReader.READER_STATE.OK_ERR)
//					mSettingsMap.put(UHFSilionParams.INV_QUICK_MODE.KEY, iValue);
//
//				return er;
//			} catch (Exception e) {
//				DLog.w(TAG, "set_INV_QUICK_MODE failed.",e);
//			}
//
//		}//end if
		

		
	}//end set_INV_QUICK_MODE
	
	private Map<String,String> getSpecSettingMap(String paramKey)
	{
		Map<String,String> settingMap = (Map<String,String>)mSettingsMap.get(paramKey);
		if(settingMap == null){
			settingMap =new HashMap<String,String>();
			mSettingsMap.put(paramKey, settingMap);
		}
		
		return settingMap;
	}
	
	/**
	 * 将"数字,数字..."的字符串转成int[]
	 * @return
	 */
	private int[] convertToIntArray(String targetStr , String splite)
	{
		try {
			if(targetStr == null)
				return null;
			
			String[] sArray  = targetStr.split(splite);
			int[] iArray = new int[sArray.length];
			for(int i = 0 ;i<sArray.length;i++)
			{
				iArray[i] = Integer.parseInt(sArray[i]);
			}
			return iArray;
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * 将Int[]转换成"数字,数字..."的字符串
	 * @param intArray
	 * @return
	 */
	private String converToString(int[] intArray)
	{
		if(intArray != null && intArray.length > 0)
		{
			String line = "";
			for(int i = 0;i < intArray.length;i++)
			{
				line += String.valueOf(intArray[i]);
				if(i < intArray.length -1)
					line += ",";
			}
			return line;
		}
		
		return null;
	}
	
	/**
	 * 加载Int[]型的参数数据
	 * @param paramKey
	 * @param outArrayDatas
	 */
	private void loadIntArrayParamsModule(String paramKey,String paramName,int[] outArrayDatas)
	{
		DLog.d(TAG, "Enter loadIntArrayParams...");
		DLog.d(TAG, "Load int array params, paramKey : "+paramKey+",arrayDataLength:"+outArrayDatas.length);
		try {
			
			Mtr_Param mtr_paramKey = getMtrParamKey(paramKey);
			if(mtr_paramKey != null)
			{
				Reader.READER_ERR er = mReader.ParamGet(mtr_paramKey, outArrayDatas);
				if(er == Reader.READER_ERR.MT_OK_ERR)
				{
					mSettingsMap.put(paramKey, outArrayDatas);
					//String sData = converToString(outArrayDatas);
					//if(sData != null)
						//saveSettings(paramKey, paramName, sData);//保存起来
					DLog.d(TAG, "loadIntArrayParamsModule success value : "+Arrays.toString(outArrayDatas));
				}else
					DLog.d(TAG, "loadIntArrayParamsModule failed.paramKey : "+paramKey+", mtr_paramKey : "+mtr_paramKey+", er : "+er.toString());
			}
			
		} catch (Exception e) {
			Log.w(TAG, "set_FREQUENCY_HOPTABLE failed.", e);
		}
		
	}//end loadIntArrayParamsModule
	
	/**
	 * 加截本地保存的"读写器发射功率"(JSONArray的字符串形式[{"antid":1,"readPower":2600,"writePower":2700},...])
	 */
	private void load_PARAM_RF_ANTPOWER_Saved()
	{
		DLog.d(TAG, "Enter load_PARAM_RF_ANTPOWER_Saved...");
		SharedPreferences sp = mContext.getSharedPreferences(UHFSilionParams.RF_ANTPOWER.KEY, Context.MODE_PRIVATE);
		String sValue = sp.getString(UHFSilionParams.RF_ANTPOWER.PARAM_RF_ANTPOWER, null);
		Log.d(TAG,"load power value " + sValue);
		mSettingsMap.put(UHFSilionParams.RF_ANTPOWER.KEY, sValue);
		
	}//load_PARAM_RF_ANTPOWER_Saved
	
	/**
	 * 加载"读写器发射功率"参数值(JSONArray的字符串形式[{"antid":1,"readPower":2600,"writePower":2700},...])
	 */
	private void load_PARAM_RF_ANTPOWER_Module()
	{
		DLog.d(TAG, "Enter load_PARAM_RF_ANTPOWER_Module...");
		Reader.AntPowerConf antPowerConf = mReader.new AntPowerConf();
		Reader.READER_ERR er = mReader.ParamGet(Mtr_Param.MTR_PARAM_RF_ANTPOWER, antPowerConf);
		
		if(er == Reader.READER_ERR.MT_OK_ERR)
		{
			Reader.AntPower[] powers = antPowerConf.Powers;
			int len = antPowerConf.antcnt;
			if(powers != null && len > 0)
			{
				JSONArray antPowerJSArray = new JSONArray();
				for(int i = 0;i<len;i++)
				{
					Reader.AntPower antPower = powers[i];
					try {
						JSONObject antPowerJS = new JSONObject();
						antPowerJS.put("antid", antPower.antid);//天线ID
						antPowerJS.put("readPower", antPower.readPower);//读功率
						antPowerJS.put("writePower", antPower.writePower);//写功率
						antPowerJSArray.put(antPowerJS);
					} catch (Exception e) {
					}
				}
				mSettingsMap.put(UHFSilionParams.RF_ANTPOWER.KEY, antPowerJSArray.toString());
			}else{
				mSettingsMap.put(UHFSilionParams.RF_ANTPOWER.KEY, null);
			}
			
			//saveAllLine: 天线ID,读功率,写功率|天线ID,读功率,写功率|...
			//saveSettings(UHFSilionParams.RF_ANTPOWER.KEY, UHFSilionParams.RF_ANTPOWER.PARAM_RF_ANTPOWER, saveAllLine);
			
		}//end if
		
	}//end load_RF_ANTPOWER
	
	/**
	 * 从模块中, 加载"过滤器",json格式存放,{"bank":1,"fdata":0A0B,"flen:"8,"startaddr":2,"isInvert":1}
	 */
	private void load_PARAM_TAG_FILTER_Module()
	{
		DLog.d(TAG, "Enter load_PARAM_TAG_FILTER...");
		Reader.TagFilter_ST filter = mReader.new TagFilter_ST();
		Reader.READER_ERR er = mReader.ParamGet(Mtr_Param.MTR_PARAM_TAG_FILTER, filter);
		
		if(er == Reader.READER_ERR.MT_OK_ERR)
		{
			int bank = filter.bank;
			byte[] fdata = filter.fdata;
			int len = filter.flen;
			if(len > 0)
			{
				fdata = Arrays.copyOf(fdata, len/8);
				String sHexFdata = UHFReader.bytes_Hexstr(fdata);
				int startaddr = filter.startaddr;
				int isInvert = filter.isInvert;
				try {
					JSONObject jsItem = new JSONObject();
					jsItem.put("bank", bank);
					jsItem.put("fdata", sHexFdata);
					jsItem.put("flen", len);
					jsItem.put("startaddr",startaddr);
					jsItem.put("isInvert", isInvert);
					mSettingsMap.put(UHFSilionParams.TAG_FILTER.KEY, jsItem.toString());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		
	}//end load_PARAM_TAG_FILTER_Module
	
	/**
	 * 加截本地保存的"标签过滤器，可在对标签进行读，写，锁，盘存操作的时候指定过滤条件",json格式存放,{"bank":1,"fdata":0A0B,"flen:"8,"startaddr":2,"isInvert":1}
	 */
	private void load_PARAM_TAG_FILTER_Saved()
	{
		DLog.d(TAG, "Enter load_PARAM_TAG_FILTER_Saved...");
		SharedPreferences sp = mContext.getSharedPreferences(UHFSilionParams.TAG_FILTER.KEY, Context.MODE_PRIVATE);
		String sValue = sp.getString(UHFSilionParams.TAG_FILTER.PARAM_TAG_FILTER, null);
		mSettingsMap.put(UHFSilionParams.TAG_FILTER.KEY, sValue);
	}
	
	/**
	 * 从模块中, 加载"在进行gen2标签的盘存操作的同时可以读某个bank的数据。"参数值<p>
	 * JSON格式,{"bank":1,"startaddr":2,"bytecnt":8,"accesspwd":"0A0B"}
	 */
	private void load_PARAM_TAG_EMBEDEDDATA_Module()
	{
		DLog.d(TAG, "Enter load_PARAM_TAG_EMBEDEDDATA...");
		Reader.READER_ERR er = Reader.READER_ERR.MT_CMD_FAILED_ERR;
		Reader.EmbededData_ST embededData = mReader.new EmbededData_ST();
		try {
			er = mReader.ParamGet(Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA, embededData);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
		if(er == Reader.READER_ERR.MT_OK_ERR)
		{
			
			int bank = embededData.bank;
			byte[] accesspwd = embededData.accesspwd;
			int bytecnt = embededData.bytecnt;
			
			String sHexAccesspwd = (accesspwd == null? null : UHFReader.bytes_Hexstr(accesspwd));
			int startaddr = embededData.startaddr;
			
			//saveSettings(UHFSilionParams.TAG_EMBEDEDDATA.KEY, UHFSilionParams.TAG_EMBEDEDDATA.PARAM_BANK,  String.valueOf(bank));
			//saveSettings(UHFSilionParams.TAG_EMBEDEDDATA.KEY, UHFSilionParams.TAG_EMBEDEDDATA.PARAM_ACCESS_PASSWD,  sHexAccesspwd);
			//saveSettings(UHFSilionParams.TAG_EMBEDEDDATA.KEY, UHFSilionParams.TAG_EMBEDEDDATA.PARAM_BYTE_COUNT,  String.valueOf(bytecnt));
			//saveSettings(UHFSilionParams.TAG_EMBEDEDDATA.KEY, UHFSilionParams.TAG_EMBEDEDDATA.PARAM_START_ADDRESS,  String.valueOf(startaddr));
			try {
				JSONObject jsItem = new JSONObject();
				jsItem.put("bank", bank);
				jsItem.put("accesspwd", sHexAccesspwd);
				jsItem.put("bytecnt", bytecnt);
				jsItem.put("startaddr", startaddr);
				mSettingsMap.put(UHFSilionParams.TAG_EMBEDEDDATA.KEY, jsItem.toString());
			} catch (Exception e) {
				DLog.w(TAG, "load_PARAM_TAG_EMBEDEDDATA failed.",e);
			}
		}
		
	}//end load_PARAM_TAG_EMBEDEDDATA
	
	/**
	 * 加截本地保存的"在进行gen2标签的盘存操作的同时可以读某个bank的数据。"<p>
	 *  JSON格式,{"bank":1,"startaddr":2,"bytecnt":8,"accesspwd":"0A0B"}
	 */
	private void load_PARAM_TAG_EMBEDEDDATA_Saved()
	{
		DLog.d(TAG, "Enter load_PARAM_TAG_EMBEDEDDATA_Saved...");
		SharedPreferences sp = mContext.getSharedPreferences(UHFSilionParams.TAG_EMBEDEDDATA.KEY, Context.MODE_PRIVATE);
		String sValue = sp.getString(UHFSilionParams.TAG_EMBEDEDDATA.PARAM_TAG_EMBEDEDDATA, null);
		mSettingsMap.put(UHFSilionParams.TAG_EMBEDEDDATA.KEY, sValue);
		
	}//end load_PARAM_TAG_EMBEDEDDATA_Saved
	
	/**
	 * 从模块中,所有被检测到的天线（不是所有的天线都能被检测）
	 */
	private void load_PARAM_READER_CONN_ANTS_Module()
	{
		DLog.d(TAG, "Enter load_PARAM_READER_CONN_ANTS_Module...");
		Reader.ConnAnts_ST connAnts = mReader.new ConnAnts_ST();
		Reader.READER_ERR er = mReader.ParamGet(Mtr_Param.MTR_PARAM_READER_CONN_ANTS, connAnts);
		
		if(er == Reader.READER_ERR.MT_OK_ERR)
		{
			int[] connectedants = connAnts.connectedants;
			int antcnt = connAnts.antcnt;
			if(antcnt > 0 && connectedants != null)
			{
				connectedants = Arrays.copyOf(connectedants, antcnt);
				String sConnectedants = converToString(connectedants);
				
				//保存设置
				//saveSettings(UHFSilionParams.READER_CONN_ANTS.KEY, UHFSilionParams.READER_CONN_ANTS.PARAM_CONNECT_ANTS,  sConnectedants);
				DLog.d(TAG, "Available ants : "+Arrays.toString(connectedants) );
				mSettingsMap.put(UHFSilionParams.READER_CONN_ANTS.KEY, connectedants);//int[]
			}
		}
		
	}//end load_PARAM_READER_CONN_ANTS_Module
	
	/**
	 * 加载保存的"所有被检测到的天线（不是所有的天线都能被检测）"
	 */
	private void load_PARAM_READER_CONN_ANTS_Saved()
	{
		DLog.d(TAG, "Enter load_PARAM_READER_CONN_ANTS_Saved...");
		SharedPreferences sp = mContext.getSharedPreferences(UHFSilionParams.READER_CONN_ANTS.KEY, Context.MODE_PRIVATE);
		String sConnectedants = sp.getString(UHFSilionParams.READER_CONN_ANTS.PARAM_CONNECT_ANTS, null);
		int[] iArrayData = convertToIntArray(sConnectedants, ",");//将"数字,数字..."的字符串转成int[]
		
		mSettingsMap.put(UHFSilionParams.READER_CONN_ANTS.KEY, iArrayData);
		
	}//end load_PARAM_READER_CONN_ANTS_Saved
	
	/**
	 * 从模块中,加载"读写器工作区域"
	 */
	private void load_PARAM_FREQUENCY_REGION_Module()
	{
		DLog.d(TAG, "Enter load_PARAM_FREQUENCY_REGION_Module...");
		Reader.Region_Conf[] regionConfs = new Reader.Region_Conf[1];
		Reader.READER_ERR er = mReader.ParamGet(Mtr_Param.MTR_PARAM_FREQUENCY_REGION, regionConfs);
		if(er == Reader.READER_ERR.MT_OK_ERR)
		{
			Reader.Region_Conf conf = regionConfs[0];
			if(conf != null)
			{
				int regionId = conf.value();
				//保存设置
				//saveSettings(UHFSilionParams.FREQUENCY_REGION.KEY, UHFSilionParams.FREQUENCY_REGION.PARAM_FREQUENCY_REGION,  String.valueOf(regionId));
				
				mSettingsMap.put(UHFSilionParams.FREQUENCY_REGION.KEY, regionId);
			}
		}
		
	}//load_PARAM_FREQUENCY_REGION_Module
	
	/**
	 * 加载保存的"读写器工作区域"
	 */
	private void load_PARAM_FREQUENCY_REGION_Saved()
	{
		DLog.d(TAG, "Enter load_PARAM_FREQUENCY_REGION_Saved...");
		SharedPreferences sp = mContext.getSharedPreferences(UHFSilionParams.FREQUENCY_REGION.KEY, Context.MODE_PRIVATE);
		String sRegionId =  sp.getString(UHFSilionParams.FREQUENCY_REGION.PARAM_FREQUENCY_REGION, null);
		if(sRegionId != null  && TextUtils.isDigitsOnly(sRegionId))
		{
			int regionId = Integer.parseInt(sRegionId);
			mSettingsMap.put(UHFSilionParams.FREQUENCY_REGION.KEY, regionId);
		}
		
	}//load_PARAM_FREQUENCY_REGION_Saved
	
	/**
	 * 从模块中,加载"读写器跳频表设置"
	 */
	private void load_PARAM_FREQUENCY_HOPTABLE_Module()
	{
		DLog.d(TAG, "Enter load_PARAM_FREQUENCY_HOPTABLE_Module...");
		Reader.HoptableData_ST hdst = mReader.new HoptableData_ST();
		Reader.READER_ERR er = mReader.ParamGet(Mtr_Param.MTR_PARAM_FREQUENCY_HOPTABLE, hdst);
		
		if(er == Reader.READER_ERR.MT_OK_ERR)
		{
			int[]   htb = hdst.htb;
			int lenhtb = hdst.lenhtb;
			if(lenhtb > 0)
			{
				htb = Arrays.copyOf(htb, lenhtb);
				String shtb = converToString(htb);
				
				//保存设置
				saveSettings(UHFSilionParams.FREQUENCY_HOPTABLE.KEY, UHFSilionParams.FREQUENCY_HOPTABLE.PARAM_HTB,  String.valueOf(shtb));
				
				mSettingsMap.put(UHFSilionParams.FREQUENCY_HOPTABLE.KEY, htb);
				DLog.d(TAG, "hoptable : "+Arrays.toString(htb));
			}
			
		}else
			DLog.d(TAG, "load_PARAM_FREQUENCY_HOPTABLE_Module failed.er : "+er.toString());
		
	}//load_PARAM_FREQUENCY_HOPTABLE_Module
	
	/**
	 * 加载保存的"读写器跳频表设置"
	 */
	private void load_PARAM_FREQUENCY_HOPTABLE_Saved()
	{
		DLog.d(TAG, "Enter load_PARAM_FREQUENCY_HOPTABLE_Saved...");
		SharedPreferences sp = mContext.getSharedPreferences(UHFSilionParams.FREQUENCY_HOPTABLE.KEY, Context.MODE_PRIVATE);
		String shtb =  sp.getString(UHFSilionParams.FREQUENCY_HOPTABLE.PARAM_HTB, null);
		int[]   htb = convertToIntArray(shtb, ",");
		mSettingsMap.put(UHFSilionParams.FREQUENCY_HOPTABLE.KEY, htb);
		
	}//end load_PARAM_FREQUENCY_HOPTABLE_Saved
	
	/**
	 * 从模块中,加载,读写器ip<p>
	 * json格式保存{"ip":"0A0B","mask":"0B0C","gateway":"0C0D"}
	 */
	private void load_PARAM_READER_IP_Module()
	{
		DLog.d(TAG, "Enter load_PARAM_READER_IP_Module...");
		Reader.Reader_Ip readerIp = mReader.new Reader_Ip();
		Reader.READER_ERR er = mReader.ParamGet(Mtr_Param.MTR_PARAM_READER_IP, readerIp);
		if(er == Reader.READER_ERR.MT_OK_ERR)
		{
			String sHexIP = HexUtil.bytesToHexString(readerIp.ip);
			String sHexMask = HexUtil.bytesToHexString(readerIp.mask);
			String sHexGateway = HexUtil.bytesToHexString(readerIp.gateway);
			
			try {
				JSONObject jsItem = new JSONObject();
				jsItem.put("ip", sHexIP);
				jsItem.put("mask", sHexMask);
				jsItem.put("gateway", sHexGateway);
				mSettingsMap.put(UHFSilionParams.READER_IP.KEY, jsItem.toString());
				DLog.d(TAG, "load_PARAM_READER_IP_Module success. result : "+jsItem.toString());
			} catch (Exception e) {
				DLog.w(TAG, "load_PARAM_READER_IP_Module failed.", e);
			}
		}else
			DLog.d(TAG, "load_PARAM_READER_IP_Module failed. er : "+er.toString());
		
	}//load_PARAM_READER_IP_Module
	
	/**
	 * 加载保存的"读写器ip"<p>
	 * json格式保存{"ip":"0A0B","mask":"0B0C","gateway":"0C0D"}
	 */
	private void load_PARAM_READER_IP_Saved()
	{
		SharedPreferences sp = mContext.getSharedPreferences(UHFSilionParams.READER_IP.KEY, Context.MODE_PRIVATE);
		String sValue =  sp.getString(UHFSilionParams.READER_IP.PARAM_READER_IP, null);
		if(sValue != null)
			mSettingsMap.put(UHFSilionParams.READER_IP.KEY, sValue);
		
	}//load_PARAM_READER_IP_Saved
	
	/**
	 * 从模块中,加载,读写器省电模式<p>
	 * 省电级别为0-3,数字越大表示越省电，0为完全不省电<p>
	 * json格式保存{"ip":"0A0B","mask":"0B0C","gateway":"0C0D"}
	 */
	private void load_POWERSAVE_MODE_Module()
	{
		DLog.d(TAG, "Enter load_POWERSAVE_MODE_Module...");
		Reader.Reader_Ip readerIp = mReader.new Reader_Ip();
		Reader.READER_ERR er = mReader.ParamGet(Mtr_Param.MTR_PARAM_POWERSAVE_MODE, readerIp);
		if(er == Reader.READER_ERR.MT_OK_ERR)
		{
			String sHexIP = HexUtil.bytesToHexString(readerIp.ip);
			String sHexMask = HexUtil.bytesToHexString(readerIp.mask);
			String sHexGateway = HexUtil.bytesToHexString(readerIp.gateway);
			
			try {
				JSONObject jsItem = new JSONObject();
				jsItem.put("ip", sHexIP);
				jsItem.put("mask", sHexMask);
				jsItem.put("gateway", sHexGateway);
				mSettingsMap.put(UHFSilionParams.READER_IP.KEY, jsItem.toString());
				DLog.d(TAG, "load_POWERSAVE_MODE_Module success. result : "+jsItem.toString());
			} catch (Exception e) {
				DLog.w(TAG, "load_POWERSAVE_MODE_Module failed.", e);
			}
		}else
			DLog.d(TAG, "load_PARAM_READER_IP_Module failed. er : "+er.toString());
		
	}//load_PARAM_READER_IP_Module
	
	/**
	 * Newland的Key转UHF提供商的Key
	 * @param uhfParamKey Newland的Key
	 * @return UHF提供商的Key
	 */
	private Mtr_Param getMtrParamKey(String uhfParamKey)
	{
		if(UHFSilionParams.POTL_GEN2_SESSION.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_POTL_GEN2_SESSION;
		
		if(UHFSilionParams.POTL_GEN2_Q.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_POTL_GEN2_Q;
		
		if(UHFSilionParams.POTL_GEN2_TAGENCODING.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_POTL_GEN2_TAGENCODING;
		
		if(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_POTL_GEN2_MAXEPCLEN;
		
		if(UHFSilionParams.RF_ANTPOWER.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_RF_ANTPOWER;
		
		if(UHFSilionParams.RF_MAXPOWER.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_RF_MAXPOWER;
		
		if(UHFSilionParams.RF_MINPOWER.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_RF_MINPOWER;
		
		if(UHFSilionParams.TAG_FILTER.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_TAG_FILTER;
		
		if(UHFSilionParams.TAG_EMBEDEDDATA.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_TAG_EMBEDEDDATA;
		
		if(UHFSilionParams.READER_CONN_ANTS.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_READER_CONN_ANTS;
		
		if(UHFSilionParams.READER_AVAILABLE_ANTPORTS.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_READER_AVAILABLE_ANTPORTS;
		
		if(UHFSilionParams.READER_IS_CHK_ANT.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_READER_IS_CHK_ANT;
		
		if(UHFSilionParams.READER_IP.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_READER_IP;
		
		if(UHFSilionParams.FREQUENCY_REGION.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_FREQUENCY_REGION;
		
		if(UHFSilionParams.FREQUENCY_HOPTABLE.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_FREQUENCY_HOPTABLE;
		
		if(UHFSilionParams.POTL_GEN2_BLF.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_POTL_GEN2_BLF;
		
		if(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_POTL_GEN2_WRITEMODE;
		
		if(UHFSilionParams.POTL_GEN2_TARGET.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_POTL_GEN2_TARGET;
		
		if(UHFSilionParams.TAGDATA_UNIQUEBYANT.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_TAGDATA_UNIQUEBYANT;
		
		if(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_TAGDATA_UNIQUEBYEMDDATA;
		
		if(UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_TAGDATA_RECORDHIGHESTRSSI;
		
		if(UHFSilionParams.RF_HOPTIME.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_RF_HOPTIME;
		
		if(UHFSilionParams.RF_LBT_ENABLE.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_RF_LBT_ENABLE;
		
		if(UHFSilionParams.POTL_ISO180006B_BLF.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_POTL_ISO180006B_BLF;
		
		if(UHFSilionParams.POTL_GEN2_TARI.KEY.equals(uhfParamKey))
			return Mtr_Param.MTR_PARAM_POTL_GEN2_TARI;
		
		return null;
	}
	
	
	/**
	 * 保存配置参数
	 * @param paramKey
	 * @param paramName
	 */
	private void saveSettings(String paramKey, String paramName, String sValue)
	{
		//保存到文件
		SharedPreferences sp = mContext.getSharedPreferences(paramKey, Context.MODE_PRIVATE);
		SharedPreferences.Editor sEditor = sp.edit();
		sEditor.putString(paramName, sValue);
		sEditor.apply();
		sEditor.commit();
	}
	
	/**
	 * 获取保存配置参数
	 * @param paramKey
	 * @param paramName
	 * @param defValue 默认值
	 */
	private String getSaveSettings(String paramKey, String paramName,String defValue)
	{
		//保存到文件
		SharedPreferences sp = mContext.getSharedPreferences(paramKey, Context.MODE_PRIVATE);
		return sp.getString(paramName, defValue);
	}
	
	private long getLongSavedSettings(String paramKey, String paramName,long defValue)
	{
		String sValue =getSaveSettings(paramKey, paramName,null);
		try {
			
			if(!TextUtils.isEmpty(sValue) && TextUtils.isDigitsOnly(sValue))
			{
				return Long.parseLong(sValue);
			}
		} catch (Exception e) {
		}
		
		return defValue;
	}
	
	/**
	 * 将保存的"int,int,int"形式的字符串转成int[]
	 * @param paramKey 配置项Key
	 * @param paramName 属性名称
	 * @return int[]
	 */
	private int[] getSavedIntArraySettings(String paramKey, String paramName)
	{
		String sValue =getSaveSettings(paramKey, paramName,null);
		int[] intArray = sValue == null ? null : convertToIntArray(sValue, ",");
		return intArray;
	}
	
	private boolean clearSavedSettings(String paramKey)
	{
		SharedPreferences sp = mContext.getSharedPreferences(paramKey, Context.MODE_PRIVATE);
		boolean suc =  sp.edit().clear().commit();
		Log.d(TAG, "----Clear data : "+paramKey+", "+suc);
		return suc;
	}
}
