package com.nlscan.uhf.silionBle.settings;

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
import com.nlscan.uhf.silionBle.BaseActivity;
import com.nlscan.uhf.silionBle.R;
import com.nlscan.uhf.silionBle.UHFSilionParams;

/**
 * 盘点过滤
 */
public class InventoryFilterActivity extends BaseActivity {

	private Context mContext;
	private UHFManager mUHFMgr = UHFManager.getInstance();
	
	private Button button_getfil, button_setfil;
	private RadioGroup rg_invfilenable,rg_invfilmatch;
	private Spinner spinner_filbank;
	private ArrayAdapter<String> arrdp_fbank;
	
	String[] spifbank = { "EPC区", "TID区", "用户区" };//盘点过滤:过滤bank
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uhf_settings_tablelayout_invfil);
		mContext = getApplicationContext();
		spifbank = getResources().getStringArray(R.array.bank_items_small);
		
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
		button_getfil.performClick();
	}
	
	private void initView()
	{
		button_getfil = (Button) findViewById(R.id.button_getfil);
		button_setfil = (Button) findViewById(R.id.button_setfil);
		rg_invfilenable = (RadioGroup) findViewById(R.id.radioGroup_enablefil);
		rg_invfilmatch = (RadioGroup) findViewById(R.id.radioGroup_invmatch);
		
		spinner_filbank = (Spinner) findViewById(R.id.spinner_invfbank);
		arrdp_fbank = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spifbank);
		arrdp_fbank
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_filbank.setAdapter(arrdp_fbank);
		
		//获取: 标签过滤器，可在对标签进行读，写，锁，盘存操作的时候指定过滤条件
		button_getfil.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
				String sValue = (String) settingsMap.get(UHFSilionParams.TAG_FILTER.KEY);
				try {
					
					if(!TextUtils.isEmpty(sValue))
					{
						JSONObject jsItem = new JSONObject(sValue);
						int bank = jsItem.optInt("bank");
						String sHexFdata = jsItem.optString("fdata");
						int flen = jsItem.optInt("flen");
						int startaddr = jsItem.optInt("startaddr");
						int isInvert = jsItem.optInt("isInvert");
						
						EditText et = (EditText) findViewById(R.id.editText_filterdata);
						EditText etadr = (EditText) findViewById(R.id.editText_invfilsadr);
						byte[] bData = UHFReader.Str2Hex(sHexFdata);
						
						if (bData == null || bData.length == 0) {
							rg_invfilenable.check(rg_invfilenable.getChildAt(0).getId());
							et.setText("");
							etadr.setText("");
							spinner_filbank.setSelection(0);
						} else {
							rg_invfilenable.check(rg_invfilenable.getChildAt(1).getId());
							et.setText(sHexFdata);
							etadr.setText(String.valueOf(startaddr));
							int selection = bank > 1?(bank - 1) : 0;
							spinner_filbank.setSelection(selection);
							
							if (isInvert == 1)
								rg_invfilmatch.check(R.id.radio_non_match);
							else
								rg_invfilmatch.check(R.id.radio_match);
						}
						
					}else{
						rg_invfilenable.check(R.id.rb_not_enable);
					}
					
				} catch (Exception e) {
				}
			}
		});

		//设置盘点过滤器
		button_setfil.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				try {
				    
				    JSONObject jsItem = new JSONObject();
					if (SortGroup(rg_invfilenable) == 1) 
					{
						int bank = 0;
					     int startaddr = 0;
					    int flen = 0;
					    String sHexFdata;
					    int isInvert = 0;
					    
						EditText et_filterData = (EditText) findViewById(R.id.editText_filterdata);
						EditText et_startAddr = (EditText) findViewById(R.id.editText_invfilsadr);
						
						sHexFdata =  et_filterData.getText().toString();
						int ln= 0;
						if(!TextUtils.isEmpty(sHexFdata))
							ln=sHexFdata.length();
						 if(ln==1||ln%2==1)
							 ln++;
						bank = spinner_filbank.getSelectedItemPosition() + 1;
						flen = (ln/2)*8;
						
						String sAddr = et_startAddr.getText().toString();
						if(sAddr != null && TextUtils.isDigitsOnly(sAddr) )
							startaddr = Integer.valueOf(sAddr);
						int ma = SortGroup(rg_invfilmatch);
						if (ma == 1)
							isInvert = 1;
						else
							isInvert = 0;
						
						jsItem.put("bank", bank);
						jsItem.put("fdata", sHexFdata);
						jsItem.put("flen", flen);
						jsItem.put("startaddr",startaddr);
						jsItem.put("isInvert", isInvert);
						
					} else
						jsItem = null;
					
					UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.TAG_FILTER.KEY, UHFSilionParams.TAG_FILTER.PARAM_TAG_FILTER,jsItem == null?null: jsItem.toString());;
				
					if (er == UHFReader.READER_STATE.OK_ERR) {
						Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(mContext,getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

				} catch (Exception ex) {
					Toast.makeText(mContext,
							"Exception:" + ex.toString(), Toast.LENGTH_SHORT).show();
					ex.printStackTrace();
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
}
