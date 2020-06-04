package com.nlscan.uhf.silionBle.settings;

import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.uhf.silionBle.BaseActivity;
import com.nlscan.uhf.silionBle.R;
import com.nlscan.uhf.silionBle.UHFSilionParams;

/**
 * Gen2项
 */
public class Gen2Activity extends BaseActivity {

	private Context mContext;
	private UHFManager mUHFMgr = UHFManager.getInstance();
	
	private Button button_getgen2ses, button_setgen2ses,
	button_getgen2q, button_setgen2q, button_getwmode, button_setwmode,
	button_getgen2blf, button_setgenblf, button_getgen2maxl,
	button_setgen2maxl, button_getgen2targ, button_setgen2targ,
	button_getgen2code, button_setgen2code, button_getgen2tari,
	button_setgen2tari;
	
	String[] spises = { "S0", "S1", "S2", "S3" };
	String[] spiq = { "Auto", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
			"10", "11", "12", "13", "14", "15" };
	String[] spiwmod = { "字写", "块写" };
	String[] spiblf = { "40", "250", "400", "640" };
	String[] spimlen = { "96", "496" };
	String[] spitget = { "A", "B", "A-B", "B-A" };
	String[] spigcod = { "FM0", "M2", "M4", "M8" };
	String[] spitari = { "25微妙", "12.5微妙", "6.25微妙" };//Gen2协议Tari
	private Spinner spinner_sesion,spinner_q,spinner_wmode,spinner_blf,spinner_maxlen,spinner_target,spinner_g2code,spinner_tari;
	private ArrayAdapter<String> arrdp_ses,arrdp_q,arrdp_wmod,arrdp_blf,arrdp_mlen,arrdp_tget,arrdp_g2cod,arrdp_tari;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uhf_settings_tablelayout_gen2);
		mContext = getApplicationContext();
		spiwmod = getResources().getStringArray(R.array.write_mode);
		spitari = getResources().getStringArray(R.array.gen2_tari);//Gen2协议Tari
		
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
		button_getgen2ses.performClick();
		button_getgen2q.performClick();
		button_getwmode.performClick();
		// button_getgen2blf.performClick();
		button_getgen2maxl.performClick();
		button_getgen2targ.performClick();
		button_getgen2code.performClick();
		// button_getgen2tari.performClick();
	}
	
	private void initView()
	{
		button_getgen2ses = (Button) findViewById(R.id.button_gen2sesget);
		button_setgen2ses = (Button) findViewById(R.id.button_gen2sesset);
		button_getgen2q = (Button) findViewById(R.id.button_gen2qget);
		button_setgen2q = (Button) findViewById(R.id.button_gen2qset);
		button_getwmode = (Button) findViewById(R.id.button_gen2wmodeget);
		button_setwmode = (Button) findViewById(R.id.button_gen2wmodeset);
		button_getgen2blf = (Button) findViewById(R.id.button_gen2blfget);
		button_setgenblf = (Button) findViewById(R.id.button_gen2blfset);
		button_getgen2maxl = (Button) findViewById(R.id.button_gen2mlget);
		button_setgen2maxl = (Button) findViewById(R.id.button_gen2mlset);
		button_getgen2targ = (Button) findViewById(R.id.button_target);
		button_setgen2targ = (Button) findViewById(R.id.button_targetset);
		button_getgen2code = (Button) findViewById(R.id.button_codeget);
		button_setgen2code = (Button) findViewById(R.id.button_codeset);
		button_getgen2tari = (Button) findViewById(R.id.button_gen2tariget);
		button_setgen2tari = (Button) findViewById(R.id.button_gen2tariset);
		
		//获取:gen2 session
		spinner_sesion = (Spinner) findViewById(R.id.spinner_gen2session);
		arrdp_ses = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spises);
		arrdp_ses
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_sesion.setAdapter(arrdp_ses);
		button_getgen2ses.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				try {
					
					Map<String,Object> settingsMap = mUHFMgr.getAllParams();
					Object obj = settingsMap.get(UHFSilionParams.POTL_GEN2_SESSION.KEY);
					int[] val2 = obj == null?new int[]{-1}:(int[])obj;

					if (val2 != null && val2.length > 0) {
						spinner_sesion.setSelection(val2[0]);
					} else
						Toast.makeText(mContext,R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(mContext,"Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
				}

			}

		});
		
		button_setgen2ses.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {
					int[] val = new int[] { -1 };
					val[0] = spinner_sesion.getSelectedItemPosition();
					
					UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_SESSION.KEY, UHFSilionParams.POTL_GEN2_SESSION.PARAM_POTL_GEN2_SESSION, String.valueOf(val[0]));
					
					if (er == UHFReader.READER_STATE.OK_ERR) {
						Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(mContext, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(mContext,"Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
					return;
				}

			}

		});
		
		//获取Gen2协议参数Q值
		spinner_q = (Spinner) findViewById(R.id.spinner_gen2q);
		arrdp_q = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spiq);
		arrdp_q.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_q.setAdapter(arrdp_q);
		button_getgen2q.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {
					Map<String,Object> settingsMap = mUHFMgr.getAllParams();
					Object obj = settingsMap.get(UHFSilionParams.POTL_GEN2_Q.KEY);
					int[] val =(int[])obj;

					if ( val != null && val.length > 0) {
						spinner_q.setSelection(val[0] + 1);
					} else
						Toast.makeText(mContext,R.string.no_data, Toast.LENGTH_SHORT)	.show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(mContext,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
				}

			}

		});
		
		//设置:Gen2协议参数Q值
		button_setgen2q.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {
					int[] val = new int[] { -1 };
					val[0] = spinner_q.getSelectedItemPosition() - 1;
					String sValue = converToString(val);
					UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_Q.KEY, UHFSilionParams.POTL_GEN2_Q.PARAM_POTL_GEN2_Q, sValue);
					if (er == UHFReader.READER_STATE.OK_ERR) {
						Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(mContext, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(mContext,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					return;
				}

			}

		});

		//获取: Gen2协议写模式
		spinner_wmode = (Spinner) findViewById(R.id.spinner_gen2wmode);
		arrdp_wmod = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spiwmod);
		arrdp_wmod
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_wmode.setAdapter(arrdp_wmod);
		button_getwmode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				try {
					Map<String,Object> settingsMap = mUHFMgr.getAllParams();
					Object obj = settingsMap.get(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY);
					int[] val =(int[])obj;

					if (val != null && val.length > 0) {
						if (val[0] == 0)
							spinner_wmode.setSelection(0);
						else if (val[0] == 1)
							spinner_wmode.setSelection(1);
					} else
						Toast.makeText(mContext,R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(mContext,"Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
				}

			}

		});
		
		//设置: Gen2协议写模式
		button_setwmode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {
					int[] val = new int[] { spinner_wmode.getSelectedItemPosition() };
					String sValue = converToString(val);
					UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY, UHFSilionParams.POTL_GEN2_WRITEMODE.PARAM_POTL_GEN2_WRITEMODE, sValue);
					
					if (er == UHFReader.READER_STATE.OK_ERR) {
						Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(mContext, getString(R.string.setting_fail)+" : "+er.toString(),Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(mContext,	"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)	.show();
					e.printStackTrace();
					return;
				}

			}

		});
		
		//获取: Gen2协议后向链路速率
		spinner_blf = (Spinner) findViewById(R.id.spinner_gen2blf);
		arrdp_blf = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spiblf);
		arrdp_blf
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_blf.setAdapter(arrdp_blf);
		button_getgen2blf.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {
					
					Map<String,Object> settingsMap = mUHFMgr.getAllParams();
					Object obj = settingsMap.get(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY);
					int[] val =(int[])obj;

					if (val != null && val.length > 0) 
					{
						switch (val[0]) {
						case 40:
							spinner_blf.setSelection(0);
							break;
						case 250:
							spinner_blf.setSelection(1);
							break;
						case 400:
							spinner_blf.setSelection(2);
							break;
						case 640:
							spinner_blf.setSelection(3);
							break;
						}
					} else
						Toast.makeText(mContext,R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					Toast.makeText(mContext,"Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}

			}

		});
		
		//设置: Gen2协议后向链路速率
		button_setgenblf.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				try {
					int[] val = new int[] { -1 };

					switch (spinner_blf.getSelectedItemPosition()) {
					case 0:
						val[0] = 40;
						break;
					case 1:
						val[0] = 250;
						break;
					case 2:
						val[0] = 400;
						break;
					case 3:
						val[0] = 640;
						break;
					}
					
					String sValue = converToString(val);
					UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_BLF.KEY, UHFSilionParams.POTL_GEN2_BLF.PARAM_POTL_GEN2_BLF, sValue);
					
					if (er == UHFReader.READER_STATE.OK_ERR) {
						Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(mContext, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					Toast.makeText(mContext,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					e.printStackTrace();
					return;
				}
			}

		});
		
		//获取: 支持的最大EPC长度，单位为bit
		button_getgen2maxl.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
				Object obj = settingsMap.get(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY);
				int[] val =(int[])obj;
				if ( val != null && val.length > 0) {
					if (val[0] == 96)
						spinner_maxlen.setSelection(0);
					else
						spinner_maxlen.setSelection(1);
				} else
					Toast.makeText(mContext,R.string.no_data, Toast.LENGTH_SHORT).show();
			}

		});
		
		//设置: 支持的最大EPC长度，单位为bit
		spinner_maxlen = (Spinner) findViewById(R.id.spinner_gen2maxl);
		arrdp_mlen = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spimlen);
		arrdp_mlen
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_maxlen.setAdapter(arrdp_mlen);
		button_setgen2maxl.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				int[] val = new int[] { spinner_maxlen.getSelectedItemPosition() == 0 ? 96 : 496 };
				String sValue = converToString(val);
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY, UHFSilionParams.POTL_GEN2_MAXEPCLEN.PARAM_POTL_GEN2_MAXEPCLEN, sValue);
				
				if (er == UHFReader.READER_STATE.OK_ERR) {
					Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(mContext, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
			}

		});
		
		//获取: Gen2协议目标
		spinner_target = (Spinner) findViewById(R.id.spinner_target);
		arrdp_tget = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spitget);
		arrdp_tget
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_target.setAdapter(arrdp_tget);
		button_getgen2targ.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
					
					Map<String,Object> settingsMap = mUHFMgr.getAllParams();
					Object obj = settingsMap.get(UHFSilionParams.POTL_GEN2_TARGET.KEY);
					int[] val =(int[])obj;
					
					if (val != null && val.length > 0) {
						spinner_target.setSelection(val[0]);
					} else
						Toast.makeText(mContext,R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					Toast.makeText(mContext,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					e.printStackTrace();
				}

			}

		});
		
		//设置: Gen2协议目标
		button_setgen2targ.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				int[] val = new int[] { spinner_target.getSelectedItemPosition() };
				
				String sValue = converToString(val);
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_TARGET.KEY, UHFSilionParams.POTL_GEN2_TARGET.PARAM_POTL_GEN2_TARGET, sValue);

				if (er == UHFReader.READER_STATE.OK_ERR ){
					Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(mContext,getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

			}

		});
		
		//获取: Gen2协议基带编码方式
		spinner_g2code = (Spinner) findViewById(R.id.spinner_gen2code);
		arrdp_g2cod = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spigcod);
		arrdp_g2cod
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_g2code.setAdapter(arrdp_g2cod);
		button_getgen2code.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
				Object obj = settingsMap.get(UHFSilionParams.POTL_GEN2_TAGENCODING.KEY);
				int[] val =(int[])obj;
				
				if (val != null && val.length > 0 ) {
					spinner_g2code.setSelection(val[0]);
				} else
					Toast.makeText(mContext,	R.string.no_data, Toast.LENGTH_SHORT).show();
			}

		});
		
		//设置: Gen2协议基带编码方式
		button_setgen2code.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				int[] val = new int[] { spinner_g2code.getSelectedItemPosition() };
				
				String sValue = converToString(val);
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_TAGENCODING.KEY, UHFSilionParams.POTL_GEN2_TAGENCODING.PARAM_POTL_GEN2_TAGENCODING, sValue);

				if (er == UHFReader.READER_STATE.OK_ERR) {
					Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(mContext,getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

			}
		});
		
		//获取: Gen2协议Tari
		spinner_tari = (Spinner) findViewById(R.id.spinner_gen2tari);
		arrdp_tari = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spitari);
		arrdp_tari
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_tari.setAdapter(arrdp_tari);
		button_getgen2tari.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				try {

					Map<String,Object> settingsMap = mUHFMgr.getAllParams();
					Object obj = settingsMap.get(UHFSilionParams.POTL_GEN2_TARI.KEY);
					int[] val =(int[])obj;
					
					if (val != null && val.length > 0 ) {
						spinner_tari.setSelection(val[0]);
					} else
						Toast.makeText(mContext,	R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast.makeText(mContext,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					e.printStackTrace();
				}

			}

		});
		
		//设置:  Gen2协议Tari
		button_setgen2tari.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				int[] val = new int[] { spinner_tari.getSelectedItemPosition() };
				String sValue = converToString(val);
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_TARI.KEY, UHFSilionParams.POTL_GEN2_TARI.PARAM_POTL_GEN2_TARI, sValue);
				
				if (er == UHFReader.READER_STATE.OK_ERR) {
					Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(mContext, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
			}

		});
		
	}//end initView
	
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
	
	private int getUHFIntSetting(String key,int defaultValue)
	{
		Map<String,Object> settingsMap = mUHFMgr.getAllParams();
		int result = defaultValue;
		if(settingsMap != null && settingsMap.get(key) != null)
			result = (Integer) settingsMap.get(key);
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
