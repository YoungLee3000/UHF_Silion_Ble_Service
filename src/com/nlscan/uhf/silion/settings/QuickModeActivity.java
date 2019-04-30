package com.nlscan.uhf.silion.settings;

import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.uhf.silion.BaseActivity;
import com.nlscan.uhf.silion.R;
import com.nlscan.uhf.silion.UHFSilionParams;

/**
 * 快速模式
 */
public class QuickModeActivity extends BaseActivity {

	private Context mContext;
	private UHFManager mUHFMgr = UHFManager.getInstance();
	
	private CheckBox checkbox_q1enable1200,
										checkbox_q2enable1200,
										cb_nostop;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uhf_settings_tablelayout_quickly);
		mContext = getApplicationContext();
		
		initActionBar();
		initView();
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
	
	private void initView()
	{
		checkbox_q1enable1200 = (CheckBox) findViewById(R.id.checkbox_q1enable1200);
		checkbox_q2enable1200 = (CheckBox) findViewById(R.id.checkbox_q2enable1200);
		
		int iQuickMode= getUHFIntSetting(UHFSilionParams.INV_QUICK_MODE.KEY,0);
		int[] iGenSessions = getUHFIntArraySetting(UHFSilionParams.POTL_GEN2_SESSION.KEY);
		iGenSessions = iGenSessions == null ? new int[]{-1}:iGenSessions;
		boolean q1enable1200 =  ( iQuickMode == 1 && iGenSessions[0] > 0 );
		boolean q0enable1200 =  ( iQuickMode == 1 && iGenSessions[0] ==  0 );
		checkbox_q1enable1200.setChecked(q1enable1200);
		checkbox_q2enable1200.setChecked(q0enable1200);
		
		checkbox_q1enable1200.setEnabled(!q0enable1200);
		checkbox_q2enable1200.setEnabled(!q1enable1200);
		cb_nostop = (CheckBox) findViewById(R.id.checkBox_nostop);
		//开启(最大功率,S1,间隔0)
		checkbox_q1enable1200.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.INV_QUICK_MODE.KEY, UHFSilionParams.INV_QUICK_MODE.PARAM_INV_QUICK_MODE, isChecked?"1":"0");
				if (er == UHFReader.READER_STATE.OK_ERR && isChecked) {
					er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_SESSION.KEY, UHFSilionParams.POTL_GEN2_SESSION.PARAM_POTL_GEN2_SESSION, "1");
				} 
				
				if (er == UHFReader.READER_STATE.OK_ERR) {
					cb_nostop.setChecked(true);
					//Toast.makeText(mContext, R.string.setting_success, Toast.LENGTH_SHORT).show();
					if(isChecked)
						checkbox_q2enable1200.setEnabled(false);
					else
						checkbox_q2enable1200.setEnabled(true);
				} else
					Toast.makeText(mContext,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
				
				
			}
		});
		
		
		//开启(最大功率,S0,间隔0)
		checkbox_q2enable1200.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.INV_QUICK_MODE.KEY, UHFSilionParams.INV_QUICK_MODE.PARAM_INV_QUICK_MODE, isChecked?"1":"0");
				if (er == UHFReader.READER_STATE.OK_ERR && isChecked) {
					er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_SESSION.KEY, UHFSilionParams.POTL_GEN2_SESSION.PARAM_POTL_GEN2_SESSION, "0");
				} 
				
				if (er == UHFReader.READER_STATE.OK_ERR) {
					cb_nostop.setChecked(true);
					//Toast.makeText(mContext, R.string.setting_success, Toast.LENGTH_SHORT).show();
					if(isChecked)
						checkbox_q1enable1200.setEnabled(false);
					else
						checkbox_q1enable1200.setEnabled(true);
				} else
					Toast.makeText(mContext,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}
		});
	}
	
	private int getUHFIntSetting(String key,int defaultValue)
	{
		Map<String,Object> settingsMap = mUHFMgr.getAllParams();
		int result = defaultValue;
		if(settingsMap != null && settingsMap.get(key) != null)
			result = (int) settingsMap.get(key);
		return result;
	}
	
	private int[] getUHFIntArraySetting(String key)
	{
		Map<String,Object> settingsMap = mUHFMgr.getAllParams();
		int[] result = null;
		if(settingsMap != null && settingsMap.get(key) != null)
			result = (int[]) settingsMap.get(key);
		return result;
	}
}
