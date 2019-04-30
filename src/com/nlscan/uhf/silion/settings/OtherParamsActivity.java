package com.nlscan.uhf.silion.settings;

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

import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.uhf.silion.BaseActivity;
import com.nlscan.uhf.silion.R;
import com.nlscan.uhf.silion.UHFSilionParams;

/**
 * 其他参数
 */
public class OtherParamsActivity extends BaseActivity {

	private Context mContext;
	private UHFManager mUHFMgr = UHFManager.getInstance();
	
	private Button button_oantuqget,button_oantuqset,button_odatauqget,button_odatauqset,button_hrssiget,button_hrssiset,button_gettempture;
	private CheckBox cb_oant,cb_odata,cb_hrssi;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uhf_settings_tablelayout_others);
		mContext = getApplicationContext();
		
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
		button_oantuqget.performClick();
		button_odatauqget.performClick();
		button_hrssiget.performClick();
		button_gettempture.performClick();
	}
	
	private void initView()
	{
		button_oantuqget = (Button) findViewById(R.id.button_oantuqget);
		button_oantuqset = (Button) findViewById(R.id.button_oantuqset);
		
		cb_oant = (CheckBox) findViewById(R.id.checkBox_oantuq);
		//获取,对于同一个标签，如果被不同的天线读到是否将做为多条标签数据
		button_oantuqget.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				int[] val = null;
				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
				Object objUQ = settingsMap.get(UHFSilionParams.TAGDATA_UNIQUEBYANT.KEY);
				if(objUQ != null)
					val = (int[])objUQ;

				if (val != null && val.length > 0 ) {
					cb_oant.setChecked(val[0] == 1 ? true : false);
				} else
					Toast.makeText(mContext ,R.string.no_data, Toast.LENGTH_SHORT).show();
			}

		});
		
		//设置,对于同一个标签，如果被不同的天线读到是否将做为多条标签数据
		button_oantuqset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				int[] val = new int[] { -1 };
				val[0] = cb_oant.isChecked() ? 1 : 0;
				
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.TAGDATA_UNIQUEBYANT.KEY, UHFSilionParams.TAGDATA_UNIQUEBYANT.PARAM_TAGDATA_UNIQUEBYANT, String.valueOf(val[0] ));
				if (er == UHFReader.READER_STATE.OK_ERR) {
					Toast.makeText(mContext, R.string.setting_success, Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(mContext,mContext.getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}

		});
		
		button_oantuqset = (Button) findViewById(R.id.button_oantuqset);
		button_odatauqget = (Button) findViewById(R.id.button_odatauqget);
		cb_odata = (CheckBox) findViewById(R.id.checkBox_odatauq);
		//获取: Epc相同的标签如果在使用嵌入盘存读功能时，读出的其它bank数据不同，是否作为多条标签数据
		button_odatauqget.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				int[] val = null;
				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
				Object objUQ = settingsMap.get(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY);
				if(objUQ != null)
					val = (int[])objUQ;

				if (val != null && val.length > 0) {
					cb_odata.setChecked(val[0] == 1 ? true : false);
				} else
					Toast.makeText(mContext,R.string.no_data, Toast.LENGTH_SHORT).show();
			}

		});
		
		//设置: Epc相同的标签如果在使用嵌入盘存读功能时，读出的其它bank数据不同，是否作为多条标签数据
		button_odatauqset = (Button) findViewById(R.id.button_odatauqset);
		button_odatauqset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				int[] val = new int[] { -1 };
				val[0] = cb_odata.isChecked() ? 1 : 0;
				String sValue = converToString(val);
				
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY, UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.PARAM_TAGDATA_UNIQUEBYEMDDATA, sValue);
				
				if (er == UHFReader.READER_STATE.OK_ERR) {
					Toast.makeText(mContext, R.string.setting_success, Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(mContext,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}

		});
		
		button_hrssiget = (Button) findViewById(R.id.button_hrssiget);
		button_hrssiset = (Button) findViewById(R.id.button_hrssiset);
		cb_hrssi = (CheckBox) findViewById(R.id.checkBox_hrssi);
		//获取:　是否只记录最大rssi
		button_hrssiget.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				int[] val = null;
				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
				Object obj = settingsMap.get(UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.KEY);
				if(obj != null)
					val = (int[])obj;

				if (val != null && val.length > 0) {
					cb_hrssi.setChecked(val[0] == 1 ? true : false);
				} else
					Toast.makeText(mContext,R.string.no_data, Toast.LENGTH_SHORT).show();
			}

		});
		
		//设置:　是否只记录最大rssi
		button_hrssiset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				int[] val = new int[] { -1 };
				val[0] = cb_hrssi.isChecked() ? 1 : 0;
				String sValue = converToString(val);
				
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.KEY, UHFSilionParams.TAGDATA_RECORDHIGHESTRSSI.PARAM_TAGDATA_RECORDHIGHESTRSSI, sValue);
				
				if (er == UHFReader.READER_STATE.OK_ERR) {
					Toast.makeText(mContext, R.string.setting_success, Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(mContext,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}

		});
		
		//温度
		button_gettempture = (Button) findViewById(R.id.button_tempure);
		button_gettempture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				EditText et = (EditText) findViewById(R.id.editText_tempure);
				try {
					String sValue = mUHFMgr.getParam(UHFSilionParams.TEMPTURE.KEY, UHFSilionParams.TEMPTURE.PARAM_TEMPTURE, null);
					if( sValue != null && TextUtils.isDigitsOnly(sValue))
						et.setText(sValue);
				} catch (Exception e) {
					Toast.makeText(mContext,	"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)	.show();
					e.printStackTrace();
				}
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
}
