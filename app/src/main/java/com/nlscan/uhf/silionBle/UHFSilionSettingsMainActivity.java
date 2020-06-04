package com.nlscan.uhf.silionBle;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.nlscan.uhf.silionBle.settings.AntsPowerActivity;
import com.nlscan.uhf.silionBle.settings.EmbedDataActivity;
import com.nlscan.uhf.silionBle.settings.Gen2Activity;
import com.nlscan.uhf.silionBle.settings.InventoryFilterActivity;
import com.nlscan.uhf.silionBle.settings.InventoryParamsActivity;
import com.nlscan.uhf.silionBle.settings.OtherParamsActivity;
import com.nlscan.uhf.silionBle.settings.QuickModeActivity;
import com.nlscan.uhf.silionBle.settings.RegionFreActivity;

public class UHFSilionSettingsMainActivity extends BaseActivity {

	private Context mContext;
	
	private ListView lv_main;
	private ArrayAdapter<String> mAdapter;
	
	private String[] mLabels;
	private Class[] mActivitys;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings_main);
		mContext = getApplicationContext();
		mLabels = new String[]{
				getString(R.string.inventory_params),
				getString(R.string.ants_power),
				getString(R.string.region_freq),
				getString(R.string.gen2_item),
				getString(R.string.inventory_filter),
				getString(R.string.addition_data),
				getString(R.string.other_params),
				getString(R.string.quick_mode)
		};
		
		mActivitys = new Class[]{
				InventoryParamsActivity.class,
				AntsPowerActivity.class,
				RegionFreActivity.class,
				Gen2Activity.class,
				InventoryFilterActivity.class,
				EmbedDataActivity.class,
				OtherParamsActivity.class,
				QuickModeActivity.class
		};
		initActionBar();
		initView();
	}
	
	private void initActionBar() 
	{
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
		lv_main = (ListView) findViewById(R.id.lv_main);
		mAdapter = new ArrayAdapter<String>(mContext, R.layout.listitemview_main_settings, R.id.tv_title, mLabels);
		lv_main.setAdapter(mAdapter);
		
		lv_main.setOnItemClickListener(new AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if(position < mActivitys.length)
				{
					Class<?> cls = mActivitys[position];
					Intent intent = new Intent(mContext,cls);
					startActivity(intent);
				}
			}
		});
	}
	
}
