package com.nlscan.uhf.silion.settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nlscan.android.uhf.TagInfo;
import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.uhf.silion.BaseActivity;
import com.nlscan.uhf.silion.R;
import com.nlscan.uhf.silion.UHFSilionParams;

/**
 * 盘点参数
 */
public class InventoryParamsActivity extends BaseActivity {

	private Context mContext ;
	
	private Button button_invproset;
	private Button button_opproget,button_opproset,button_invantsset,button_getusl,
	button_setusl;
	private CheckBox cb_gen2,cb_6b,cb_ipx64,cb_ipx256,cb_ant1, cb_ant2, cb_ant3, cb_ant4;

	private UHFManager mUHFMgr;
	private Map<String,Object> settingsMap;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uhf_settings_tablelayout_invusl);
		mContext = getApplicationContext();
		mUHFMgr = UHFManager.getInstance();
		settingsMap = mUHFMgr.getAllParams();
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
	
	
	@Override
	protected void onResume() {
		super.onResume();
	}

	private void updateViewData()
	{
		button_getusl.performClick();
		button_opproget.performClick();
	}
	
	private void initView()
	{
		button_invproset = (Button) findViewById(R.id.button_invproset);
		cb_gen2 = (CheckBox) findViewById(R.id.checkBox_invgen2);
		cb_6b = (CheckBox) findViewById(R.id.checkBox_inv6b);
		cb_ipx64 = (CheckBox) findViewById(R.id.checkBox_invipx64);
		cb_ipx256 = (CheckBox) findViewById(R.id.checkBox_invipx256);
		
		button_opproget = (Button) findViewById(R.id.button_opproget);
		button_opproset = (Button) findViewById(R.id.button_opproset);
		
		
		//设置盘存操作的协议（仅仅M6e架构的读写器支持的参数）
		button_invproset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				List<TagInfo.SL_TagProtocol> ltp = new ArrayList<TagInfo.SL_TagProtocol>();
				List<String> ls = new ArrayList<String>();
				
				if (cb_gen2.isChecked()) {
					ltp.add(TagInfo.SL_TagProtocol.SL_TAG_PROTOCOL_GEN2);
					ls.add("GEN2");

				}
				if (cb_6b.isChecked()) {
					ltp.add(TagInfo.SL_TagProtocol.SL_TAG_PROTOCOL_ISO180006B);
					ls.add("6B");
				}
				if (cb_ipx64.isChecked()) {
					ltp.add(TagInfo.SL_TagProtocol.SL_TAG_PROTOCOL_IPX64);
					ls.add("IPX64");
				}
				if (cb_ipx256.isChecked()) {
					ltp.add(TagInfo.SL_TagProtocol.SL_TAG_PROTOCOL_IPX256);
					ls.add("IPX256");
				}

				if (ltp.size() < 1) {
					Toast.makeText(mContext, R.string.select_protocol,Toast.LENGTH_SHORT).show();
					return;
				}
				TagInfo.SL_TagProtocol[] tagProtocolArray = ltp.toArray(new TagInfo.SL_TagProtocol[ltp.size()]);
				
				int[] iProtocols = new int[tagProtocolArray.length];
				for (int i = 0; i <tagProtocolArray.length; i++) {
					iProtocols[i] = tagProtocolArray[i].value();
				}
				String sValue = converToString(iProtocols);
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.TAG_INVPOTL.KEY, UHFSilionParams.TAG_INVPOTL.PARAM_TAG_INVPOTL, sValue);
				if (er == UHFReader.READER_STATE.OK_ERR) {
					Toast.makeText(mContext, R.string.setting_success, Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(mContext,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}

		});
		
		button_opproget.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

			}

		});
		button_opproset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

			}

		});

		button_invantsset = (Button) findViewById(R.id.button_invantsset);
		cb_ant1 = (CheckBox) findViewById(R.id.checkBox_ant1);
		cb_ant2 = (CheckBox) findViewById(R.id.checkBox_ant2);
		cb_ant3 = (CheckBox) findViewById(R.id.checkBox_ant3);
		cb_ant4 = (CheckBox) findViewById(R.id.checkBox_ant4);
		
		int[] ants = (int[]) settingsMap.get(UHFSilionParams.ANTS.PARAM_ANTS_GROUP);
		if(ants == null || ants.length == 0)
		{
			ants = new int[1];
			ants[0] = 1;
		}
		cb_ant1.setVisibility(ants.length > 0?View.VISIBLE:View.INVISIBLE);
		cb_ant2.setVisibility(ants.length > 1?View.VISIBLE:View.INVISIBLE);
		cb_ant3.setVisibility(ants.length > 2?View.VISIBLE:View.INVISIBLE);
		cb_ant4.setVisibility(ants.length > 3?View.VISIBLE:View.INVISIBLE);
		
		button_invantsset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				List<Integer> ltp = new ArrayList<Integer>();

				if (cb_ant1.isChecked())
					ltp.add(1);
				if (cb_ant2.isChecked())
					ltp.add(2);
				if (cb_ant3.isChecked())
					ltp.add(3);
				if (cb_ant4.isChecked())
					ltp.add(4);

				if (ltp.size() == 0) {
					Toast.makeText(mContext,R.string.select_inv_ants, Toast.LENGTH_SHORT).show();
					return;
				}

				Integer[] ants = ltp.toArray(new Integer[ltp.size()]);
				int[] uants = new int[ants.length];
				for (int i = 0; i < ants.length; i++)
					uants[i] = ants[i];

				String sValue = converToString(uants);
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.ANTS.KEY, UHFSilionParams.ANTS.PARAM_ANTS_GROUP, sValue);
				if(er == UHFReader.READER_STATE.OK_ERR)
					Toast.makeText(mContext, R.string.setting_success, Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(mContext,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}

		});
		
		button_getusl = (Button) findViewById(R.id.button_invuslget);
		button_setusl = (Button) findViewById(R.id.button_invuslset);
		
		//获取,盘点超时,盘点间隔
		button_getusl.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				EditText ettime = (EditText) findViewById(R.id.editText_invtime);
				EditText etsleep = (EditText) findViewById(R.id.editText_invsleep);
				
				long invTimeout = UHFSilionParams.INV_TIME_OUT.DEFAULT_INV_TIMEOUT;
				long invInterval = UHFSilionParams.INV_INTERVAL.DEFAULT_INV_INTERVAL_TIME;
				settingsMap = mUHFMgr.getAllParams();
				Object objTimeout = settingsMap.get(UHFSilionParams.INV_TIME_OUT.KEY);
				Object objInterval = settingsMap.get(UHFSilionParams.INV_INTERVAL.KEY);
				
				if(objTimeout != null )
					invTimeout = (long)objTimeout;
				
				if(objInterval != null)
					invInterval = (long)objInterval;
				
				ettime.setText(String.valueOf(invTimeout));
				etsleep.setText(String.valueOf(invInterval));
			}

		});
		
		//设置,盘点超时,盘点间隔
		button_setusl.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				try {
					EditText ettime = (EditText) findViewById(R.id.editText_invtime);
					EditText etsleep = (EditText) findViewById(R.id.editText_invsleep);
					long readTimeout = -1;
					long intervalTime = -1;
					String sTimeout = ettime.getText().toString();
					String sInterval = etsleep.getText().toString();
					if(sTimeout != null && TextUtils.isDigitsOnly(sTimeout))
						readTimeout = Long.parseLong(sTimeout);
					
					if(sInterval != null && TextUtils.isDigitsOnly(sInterval))
						intervalTime = Long.parseLong(sInterval);
					
					UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR ;
					if(readTimeout > -1)
						er = mUHFMgr.setParam(UHFSilionParams.INV_TIME_OUT.KEY, UHFSilionParams.INV_TIME_OUT.PARAM_INV_TIME_OUT, String.valueOf(readTimeout));
					if(intervalTime > -1)
						er = mUHFMgr.setParam(UHFSilionParams.INV_INTERVAL.KEY, UHFSilionParams.INV_INTERVAL.PARAM_INV_INTERVAL_TIME, String.valueOf(intervalTime));

					if(er == UHFReader.READER_STATE.OK_ERR)
						Toast.makeText(mContext, R.string.setting_success, Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(mContext,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
					
				} catch (Exception e) {
					Toast.makeText(mContext,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					e.printStackTrace();
					return;
				}
				
			}

		});
		
		
	}//initView
	
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
	
	private Object getUHFSetting(String key)
	{
		settingsMap = mUHFMgr.getAllParams();
		if(settingsMap != null)
			return  settingsMap.get(key);
		
		return null;
	}
	
	private int getUHFIntSetting(String key,int defaultValue)
	{
		settingsMap = mUHFMgr.getAllParams();
		int result = defaultValue;
		if(settingsMap != null && settingsMap.get(key) != null)
			result = (int) settingsMap.get(key);
		return result;
	}
	
	private int[] getUHFIntArraySetting(String key)
	{
		settingsMap = mUHFMgr.getAllParams();
		int[] result = null;
		if(settingsMap != null && settingsMap.get(key) != null)
			result = (int[]) settingsMap.get(key);
		return result;
	}
}
