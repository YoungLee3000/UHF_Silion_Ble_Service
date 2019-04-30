package com.nlscan.uhf.silion.settings;

import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.uhf.silion.BaseActivity;
import com.nlscan.uhf.silion.R;
import com.nlscan.uhf.silion.UHFSilionParams;

/**
 * 天线功率
 */
public class AntsPowerActivity extends BaseActivity {

	private Context mContext;
	
	private Button  button_getantcheck,	button_setantcheck,button_getantpower, button_setantpower;
	private RadioGroup rg_antcheckenable;
	private Spinner  spinner_ant1rpow, spinner_ant1wpow, spinner_ant2rpow,
	spinner_ant2wpow, spinner_ant3rpow, spinner_ant3wpow,
	spinner_ant4rpow, spinner_ant4wpow;
	
	/**低电量,功率*/
	private Spinner spinner_low_battery_level, spinner_low_ant_power;
	private Button button_lower_battery_get, button_lower_battery_set;
	
	private ArrayAdapter<String> arradp_pow;
	String[] spipow = { "500", "600", "700", "800", "900", "1000", "1100",
			"1200", "1300", "1400", "1500", "1600", "1700", "1800", "1900",
			"2000", "2100", "2200", "2300", "2400", "2500", "2600", "2700",
			"2800", "2900", "3000" };
	
	/**电量百分比*/
	Integer[] lowBattey = new Integer[]{ 0,10, 15, 20, 25, 30, 35, 40,45, 50,60 };
	private ArrayAdapter<Integer> lowerBatteryAdapter;
	private CheckBox cb_lowpoer_enable;
	
	private UHFManager mUHFMgr = UHFManager.getInstance();
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = getApplicationContext();
		setContentView(R.layout.uhf_settings_tablelayout_antpow);
		
		initActionBar();
		initView();
		updateViewData();
	}
	
	private void initActionBar() {
		getActionBar().setDisplayShowHomeEnabled(false);
		getActionBar().setDisplayShowCustomEnabled(true);
		getActionBar().setCustomView(R.layout.action_bar);
		((TextView) findViewById(R.id.tv_title)).setText(getTitle());

		ImageView leftHome = (ImageView) findViewById(R.id.img_home);
		leftHome.setVisibility(View.VISIBLE);
		leftHome.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	private void updateViewData()
	{
		button_getantcheck.performClick();
		button_getantpower.performClick();
		
		button_lower_battery_get.performClick();
	}
	
	private void initView()
	{
		button_getantcheck = (Button) findViewById(R.id.button_checkantget);
		button_setantcheck = (Button) findViewById(R.id.button_checkantset);
		rg_antcheckenable = (RadioGroup) findViewById(R.id.radioGroup_antcheck);
		button_getantcheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {
					int[] val2 = new int[] { -1 };
					Map<String,Object> settingsMap = mUHFMgr.getAllParams();
					val2 = (int[]) settingsMap.get(UHFSilionParams.READER_IS_CHK_ANT.KEY);

					if (val2 != null) {
						if (val2[0] == 0)
							rg_antcheckenable.check(rg_antcheckenable.getChildAt(0).getId());
						else
							rg_antcheckenable.check(rg_antcheckenable.getChildAt(1).getId());
					} 

				} catch (Exception e) {
					Toast.makeText(mContext,
							"Exception : " + e.getMessage(), Toast.LENGTH_SHORT).show();
				}

			}

		});
		
		button_setantcheck.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {
					
					UHFReader.READER_STATE er;
					if (SortGroup(rg_antcheckenable) == 0)
						er = mUHFMgr.setParam(UHFSilionParams.READER_IS_CHK_ANT.KEY, UHFSilionParams.READER_IS_CHK_ANT.PARAM_READER_IS_CHK_ANT, String.valueOf(0));
					else
						er = mUHFMgr.setParam(UHFSilionParams.READER_IS_CHK_ANT.KEY, UHFSilionParams.READER_IS_CHK_ANT.PARAM_READER_IS_CHK_ANT, String.valueOf(1));
					if (er == UHFReader.READER_STATE.OK_ERR) {
						Toast.makeText(mContext, R.string.setting_success,
								Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(mContext,
								getString(R.string.setting_fail)+" : "+ er.toString(), Toast.LENGTH_SHORT)
								.show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(mContext,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					return;
				}

			}

		});
		
		button_getantpower = (Button) findViewById(R.id.button_antpowget);
		button_setantpower = (Button) findViewById(R.id.button_antpowset);
		spinner_ant1rpow = (Spinner) findViewById(R.id.spinner_ant1rpow);
		spinner_ant1wpow = (Spinner) findViewById(R.id.spinner_ant1wpow);
		spinner_ant2rpow = (Spinner) findViewById(R.id.spinner_ant2rpow);
		spinner_ant2wpow = (Spinner) findViewById(R.id.spinner_ant2wpow);
		spinner_ant3rpow = (Spinner) findViewById(R.id.spinner_ant3rpow);
		spinner_ant3wpow = (Spinner) findViewById(R.id.spinner_ant3wpow);
		spinner_ant4rpow = (Spinner) findViewById(R.id.spinner_ant4rpow);
		spinner_ant4wpow = (Spinner) findViewById(R.id.spinner_ant4wpow);

		button_lower_battery_get = (Button) findViewById(R.id.button_lower_battery_get);
		button_lower_battery_set = (Button) findViewById(R.id.button_lower_battery_set);
		spinner_low_battery_level = (Spinner) findViewById(R.id.spinner_low_power);
		spinner_low_ant_power = (Spinner) findViewById(R.id.spinner_low_ant_power);
		
		// /*
		arradp_pow = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spipow);
		arradp_pow
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_ant1rpow.setAdapter(arradp_pow);
		spinner_ant1wpow.setAdapter(arradp_pow);
		spinner_ant2rpow.setAdapter(arradp_pow);
		spinner_ant2wpow.setAdapter(arradp_pow);
		spinner_ant3rpow.setAdapter(arradp_pow);
		spinner_ant3wpow.setAdapter(arradp_pow);
		spinner_ant4rpow.setAdapter(arradp_pow);
		spinner_ant4wpow.setAdapter(arradp_pow);
		
		//低电量时,调节功率
		lowerBatteryAdapter = new ArrayAdapter<Integer>(this,android.R.layout.simple_spinner_item, lowBattey); 
		lowerBatteryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_low_ant_power.setAdapter(arradp_pow);
		spinner_low_battery_level.setAdapter(lowerBatteryAdapter);

		spinner_ant2rpow.setEnabled(false);
		spinner_ant2wpow.setEnabled(false);
		spinner_ant3rpow.setEnabled(false);
		spinner_ant3wpow.setEnabled(false);
		spinner_ant4rpow.setEnabled(false);
		spinner_ant4wpow.setEnabled(false);
		
		
		// */
		//获取:天线功率
		button_getantpower.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {
					
					//加载"读写器发射功率JSONArray的字符串形式[{"antid":1,"readPower":2600,"writePower":2700},...]格式
					Map<String,Object> settingsMap = mUHFMgr.getAllParams();
					String sValue = (String) settingsMap.get(UHFSilionParams.RF_ANTPOWER.KEY);
					

					if (sValue != null ) {
						JSONArray jsArray = new JSONArray(sValue);
						int len = jsArray.length();
						if(len > 0)
						{
							for(int i =0 ;i < len;i++)
							{
								JSONObject jobj = jsArray.optJSONObject(i);
								int antid = jobj.optInt("antid");//天线ID
								short readPower = (short)jobj.optInt("readPower");//读功率
								short writePower = (short)jobj.optInt("writePower");//写功率
								
								if (i == 0) {
									spinner_ant1rpow
											.setSelection((readPower - 500) / 100);
									spinner_ant1wpow
											.setSelection((writePower - 500) / 100);
								} else if (i == 1) {
									spinner_ant2rpow
											.setSelection((readPower - 500) / 100);
									spinner_ant2wpow
											.setSelection((writePower - 500) / 100);
								} else if (i == 2) {
									spinner_ant3rpow
											.setSelection((readPower - 500) / 100);
									spinner_ant3wpow
											.setSelection((writePower - 500) / 100);
								} else if (i == 3) {
									spinner_ant4rpow
											.setSelection((readPower - 500) / 100);
									spinner_ant4wpow
											.setSelection((writePower - 500) / 100);
								}
							}
						}

					} else
						Toast.makeText(mContext,	R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(mContext,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
				}

			}

		});
		
		button_setantpower.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				int[] rp = new int[4];
				int[] wp = new int[4];

				rp[0] = spinner_ant1rpow.getSelectedItemPosition();
				rp[1] = spinner_ant2rpow.getSelectedItemPosition();
				rp[2] = spinner_ant3rpow.getSelectedItemPosition();
				rp[3] = spinner_ant4rpow.getSelectedItemPosition();

				wp[0] = spinner_ant1wpow.getSelectedItemPosition();
				wp[1] = spinner_ant2wpow.getSelectedItemPosition();
				wp[2] = spinner_ant3wpow.getSelectedItemPosition();
				wp[3] = spinner_ant4wpow.getSelectedItemPosition();
				

				try {
					
					//读写器发射功率-------------------------------------
					int antportc  = getAntportCount();
					JSONArray jsItemArray = new JSONArray();
					
					for (int i = 0; i < antportc; i++) {
						int antid = i + 1;
						int readPower =  (short) (500 + 100 * rp[i]);
						int writePower = (short) (500 + 100 * wp[i]);
						JSONObject jsItem = new JSONObject();
						jsItem.put("antid", antid);
						jsItem.put("readPower", readPower);
						jsItem.put("writePower", writePower);
						
						jsItemArray.put(jsItem);
					}
					
					UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.RF_ANTPOWER.KEY, UHFSilionParams.RF_ANTPOWER.PARAM_RF_ANTPOWER, jsItemArray.toString());
					
					if (er == UHFReader.READER_STATE.OK_ERR) {
						Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(mContext,getString(R.string.setting_fail) + " : "+er.toString(), Toast.LENGTH_SHORT).show();
					
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(mContext,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
					return;
				}

			}

		});

		////低电量时,功率调节使能
		
		cb_lowpoer_enable = (CheckBox) findViewById(R.id.cb_lowpoer_enable);
		Map<String,Object> settings = mUHFMgr.getAllParams();
		int iLowerpowerEnable =settings.containsKey(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE)? (int)settings.get(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE) : 0;;
		cb_lowpoer_enable.setChecked(iLowerpowerEnable == 1);
		spinner_low_battery_level.setEnabled(cb_lowpoer_enable.isChecked());
		spinner_low_ant_power.setEnabled(cb_lowpoer_enable.isChecked());
		cb_lowpoer_enable.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				spinner_low_battery_level.setEnabled(isChecked);
				spinner_low_ant_power.setEnabled(isChecked);
			}
		});
		
		
		
		/**获取: 低电量,功率*/
		button_lower_battery_get.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Map<String,Object> settings = mUHFMgr.getAllParams();
				Object obj = settings.get(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_LEVEL);
				int powerLevel = 15;
				if(obj != null)
					powerLevel = (int)obj;
				
				for(int i = 0 ; i <  lowBattey.length; i++)
				{
					if(lowBattey[i] == powerLevel)
					{
						spinner_low_battery_level.setSelection(i);
						break;
					}
				}
				
				obj = settings.get(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_READ_DBM);
				int dbm = 2700;
				if(obj != null)
					dbm = (int)obj;
				
				spinner_low_ant_power.setSelection((dbm-500)/100);
				
				int iLowerpowerEnable =settings.containsKey(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE)? (int)settings.get(UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE) : 0;;
				cb_lowpoer_enable.setChecked(iLowerpowerEnable == 1);
			}
		});
		
		/**获取: 低电量,功率*/
		button_lower_battery_set.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				int level = lowBattey[spinner_low_battery_level.getSelectedItemPosition()];
				String sPower = spipow[spinner_low_ant_power.getSelectedItemPosition()];
				
				try {
					UHFReader.READER_STATE er =  mUHFMgr.setParam(UHFSilionParams.LOWER_POWER.KEY, UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_DM_ENABLE, String.valueOf(cb_lowpoer_enable.isChecked()?1:0));
					if(er == UHFReader.READER_STATE.OK_ERR)
						er = mUHFMgr.setParam(UHFSilionParams.LOWER_POWER.KEY, UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_LEVEL, String.valueOf(level));
					if(er == UHFReader.READER_STATE.OK_ERR)
						er = mUHFMgr.setParam(UHFSilionParams.LOWER_POWER.KEY, UHFSilionParams.LOWER_POWER.PARAM_LOWER_POWER_READ_DBM, String.valueOf(sPower));
					
					if (er == UHFReader.READER_STATE.OK_ERR) {
						Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(mContext,getString(R.string.setting_fail) + " : "+er.toString(), Toast.LENGTH_SHORT).show();
					
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(mContext,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
					return;
				}
			}
		});
	}//end initView
	
	private int SortGroup(RadioGroup rg) {
		int check1 = rg.getCheckedRadioButtonId();
		if (check1 != -1) {
			for (int i = 0; i < rg.getChildCount(); i++) {
				View vi = rg.getChildAt(i);
				int vv = vi.getId();
				if (check1 == vv) {
					return i;
				}
			}

			return -1;
		} else
			return check1;
	}
	
	/**
	 * 获取天线数
	 * @return
	 */
	private int getAntportCount()
	{
		Map<String,Object> settingsMap  = mUHFMgr.getAllParams();
		Object antportObj = settingsMap.get(UHFSilionParams.READER_AVAILABLE_ANTPORTS.KEY) ;
		int[] antportArr = (int[])antportObj;
		int  antportc = (antportArr == null || antportArr.length == 0 ) ? 1 : antportArr[0];
		return antportc;
	}
}
