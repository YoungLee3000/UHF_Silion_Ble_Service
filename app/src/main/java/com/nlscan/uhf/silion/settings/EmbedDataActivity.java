package com.nlscan.uhf.silion.settings;

import java.util.Map;

import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.uhf.silion.BaseActivity;
import com.nlscan.uhf.silion.DLog;
import com.nlscan.uhf.silion.R;
import com.nlscan.uhf.silion.UHFSilionParams;

/**
 * 附加数据
 */
public class EmbedDataActivity extends BaseActivity {

	private Context mContext;
	private UHFManager mUHFMgr = UHFManager.getInstance();
	
	private Button button_getemd,button_setemd;
	private RadioGroup rg_emdenable;
	private Spinner spinner_emdbank;
	private ArrayAdapter arrdp_bank;
	
	String[] spibank = { "保留区", "EPC区", "TID区", "用户区" };//在进行gen2标签的盘存操作的同时可以读某个bank的数据
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uhf_settings_tablelayout_invemd);
		mContext = getApplicationContext();
		spibank = getResources().getStringArray(R.array.bank_items_all);
		
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
		button_getemd.performClick();
	}
	
	private void initView()
	{
		button_getemd = (Button) findViewById(R.id.button_getemd);
		button_setemd = (Button) findViewById(R.id.button_setemd);
		rg_emdenable = (RadioGroup) findViewById(R.id.radioGroup_emdenable);
		
		spinner_emdbank = (Spinner) findViewById(R.id.spinner_emdbank);
		arrdp_bank = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spibank);
		arrdp_bank
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_emdbank.setAdapter(arrdp_bank);
		//获取:在进行gen2标签的盘存操作的同时可以读某个bank的数据。
		button_getemd.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				EditText etst = (EditText) findViewById(R.id.editText_emdsadr);
				EditText etapwd = (EditText) findViewById(R.id.editText_emdacspwd);
				EditText etct = (EditText) findViewById(R.id.editText_emdcount);

				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
				
				String sValue = (String) settingsMap.get(UHFSilionParams.TAG_EMBEDEDDATA.KEY);
				try {
					if(sValue != null)
					{
						JSONObject jsItem = new JSONObject(sValue);
						int bank = jsItem.optInt("bank");
						String sHexAccesspwd = jsItem.optString("accesspwd");
						int bytecnt = jsItem.optInt("bytecnt");
						int startaddr = jsItem.optInt("startaddr");
						
						if (bytecnt == 0) {
							etst.setText("");
							etapwd.setText("");
							etct.setText("");
							rg_emdenable.check(rg_emdenable.getChildAt(0).getId());
							spinner_emdbank.setSelection(0);
						} else {
							byte[] byteAccesspwd = UHFReader.Str2Hex(sHexAccesspwd);
							if (sHexAccesspwd != null) 
							{
								etapwd.setText(sHexAccesspwd);
							}
							etst.setText(String.valueOf(startaddr));
							etct.setText(String.valueOf(bytecnt));
							rg_emdenable.check(rg_emdenable.getChildAt(1).getId());
							spinner_emdbank.setSelection(bank);

						}
						
					}else
						DLog.d("MYINFO", "No embeded data.");
						//Toast.makeText(mContext,	R.string.no_data, Toast.LENGTH_SHORT).show();
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}

		});
		
		//设置: 在进行gen2标签的盘存操作的同时可以读某个bank的数据。
		button_setemd.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				try {
					
					int bank = 0;
					String sHexAccesspwd = null;
					int bytecnt =  0;
					int startaddr =  0;
			    	
					if (SortGroup(rg_emdenable) == 1) {
						EditText etst = (EditText) findViewById(R.id.editText_emdsadr);
						EditText etapwd = (EditText) findViewById(R.id.editText_emdacspwd);
						EditText etct = (EditText) findViewById(R.id.editText_emdcount);
						
						bank = spinner_emdbank.getSelectedItemPosition();
						 sHexAccesspwd = etapwd.getText().toString();
						String sBytecnt = etct.getText().toString();
						if(sBytecnt != null && TextUtils.isDigitsOnly(sBytecnt))
							bytecnt = Integer.parseInt(sBytecnt);
						
						String sStartAddr = etst.getText()	.toString();
						if(sStartAddr != null && TextUtils.isDigitsOnly(sStartAddr))
							startaddr = Integer.parseInt(sStartAddr);
						
					} else
						bytecnt = 0;
					
					JSONObject jsItem = new JSONObject();
					jsItem.put("bank", bank);
					jsItem.put("startaddr", startaddr);
					jsItem.put("bytecnt", bytecnt);
					jsItem.put("accesspwd", sHexAccesspwd);
					
					UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.TAG_EMBEDEDDATA.KEY, UHFSilionParams.TAG_EMBEDEDDATA.PARAM_TAG_EMBEDEDDATA, jsItem.toString());
					
					 if (er == UHFReader.READER_STATE.OK_ERR) {
							Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
						} else
							Toast.makeText(mContext, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

				} catch (Exception ex) {
					Toast.makeText(mContext,
							"Exception:" + ex.toString(), Toast.LENGTH_SHORT).show();
					ex.printStackTrace();
				}
			}
		});

	}
	
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
	
}
