package com.nlscan.uhf.silion.settings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.uhf.silion.BaseActivity;
import com.nlscan.uhf.silion.MyAdapter2;
import com.nlscan.uhf.silion.R;
import com.nlscan.uhf.silion.UHFSilionParams;

/**
 * 区域频率
 */
public class RegionFreActivity extends BaseActivity {

	private Context mContext;
	private UHFManager mUHFMgr = UHFManager.getInstance();
	
	private Button button_getreg,	button_setreg,button_getfre, button_setfre;
	private Spinner spinner_region;
	private ArrayAdapter<String> arradp_reg;
	private ListView elist;
	private CheckBox cb_allsel;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uhf_settings_tablelayout_invfre);
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
		button_getreg.performClick();
		button_getfre.performClick();
	}
	
	private void initView()
	{
		elist = (ListView) this.findViewById(R.id.listView_frequency);
		// 绑定listView的监听器
		elist.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position,
					long id) {
				// TODO Auto-generated method stub
				// 取得ViewHolder对象，这样就省去了通过层层的findViewById去实例化我们需要的cb实例的步骤
				MyAdapter2.ViewHolder holder = (MyAdapter2.ViewHolder) view.getTag();
				// 改变CheckBox的状态
				holder.cb.toggle();
				// 将CheckBox的选中状况记录下来
				MyAdapter2.getIsSelected().put(position, holder.cb.isChecked());
			}

		});
		
		button_getreg = (Button) findViewById(R.id.button_getregion);
		button_setreg = (Button) findViewById(R.id.button_setregion);
		spinner_region = (Spinner) findViewById(R.id.spinner_region);
		
		String[] spireg = { "中国", "北美", "日本", "韩国", "欧洲", "欧洲2", "欧洲3", "印度",
				"加拿大", "全频段", "中国2" };//读写器工作区域
		spireg = getResources().getStringArray(R.array.region_items);
		arradp_reg = new ArrayAdapter<String>(mContext,	R.layout.spinner_item, spireg);
		arradp_reg.setDropDownViewResource(R.layout.spinner_drop_item);
//		arradp_reg
//				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_region.setAdapter(arradp_reg);
		
		//获取,读写器工作区域
		button_getreg.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				spinner_region.setSelection(-1);
				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
				Object obj = settingsMap.get(UHFSilionParams.FREQUENCY_REGION.KEY);
				int region = -1;
				if(obj != null){
					region = (int)obj;
				}
					
				if(region !=  -1)
				{
					UHFReader.Region_Conf regionEnum = UHFReader.Region_Conf.valueOf(region);
					switch (regionEnum)
					{
					case RG_NA: //北美:
						spinner_region.setSelection(2);
						break;
					case RG_EU3: //欧洲3
						spinner_region.setSelection(3);
						break;
					case RG_PRC: //中国
						spinner_region.setSelection(0);
						break;
					case RG_PRC2: //中国２
						spinner_region.setSelection(1);
						break;
					case RG_OPEN://全屏段
						spinner_region.setSelection(4);
						break;
					}
				}else
					Toast.makeText(mContext,R.string.no_data, Toast.LENGTH_SHORT).show();
			}
		});
		
		//设置,读写器工作区域
		button_setreg.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View view) {
				UHFReader.Region_Conf rre;
			       
				switch (spinner_region.getSelectedItemPosition()) {
				case 0:
					rre = UHFReader.Region_Conf.RG_PRC;
					break;
				case 1:
					rre = UHFReader.Region_Conf.RG_PRC2;//中国2
					break;
				case 2:
					rre = UHFReader.Region_Conf.RG_NA;//北美
					break;
				case 3:
					rre = UHFReader.Region_Conf.RG_EU3;//欧洲3
					break;
				case 4:
					rre = UHFReader.Region_Conf.RG_OPEN;//全频段
					break;
				default:
					rre = UHFReader.Region_Conf.RG_NONE;
					break;
				}
				if (rre == UHFReader.Region_Conf.RG_NONE) {
					Toast.makeText(mContext, R.string.unsupport_region,Toast.LENGTH_SHORT).show();
					return;
				}

				int iRegion = rre.value();
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.FREQUENCY_REGION.KEY, UHFSilionParams.FREQUENCY_REGION.PARAM_FREQUENCY_REGION, String.valueOf(iRegion));
				if (er == UHFReader.READER_STATE.OK_ERR) {
					button_getfre.performClick();
					Toast.makeText(mContext,R.string.setting_success,Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(mContext,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}

		});
		
		button_getfre = (Button) findViewById(R.id.button_getfre);
		button_setfre = (Button) findViewById(R.id.button_setfre);
		//获取,读写器跳频表设置
		button_getfre.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
				Object obj = settingsMap.get(UHFSilionParams.FREQUENCY_HOPTABLE.KEY);
				int[] htb = null;
				if(obj != null){
					htb = (int[])obj;
				}
				
				int[] tablefre = null;
				if (htb != null) {

					tablefre = Sort(htb, htb.length);
					String[] ssf = new String[htb.length];
					for (int i = 0; i < htb.length; i++) {
						ssf[i] = String.valueOf(tablefre[i]);
					}
					showlist(ssf);

				} else
					Toast.makeText(mContext,R.string.no_data, Toast.LENGTH_SHORT).show();

			}

		});

		//设置,读写器跳频表设置
		button_setfre.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				ArrayList<Integer> lit = new ArrayList<Integer>();
				for (int i = 0; i < elist.getCount(); i++) {
					String temp = (String) elist.getItemAtPosition(i);
					if (MyAdapter2.getIsSelected().get(i)) {
						lit.add(Integer.valueOf(temp));
					}

				}
				if (lit.size() > 0) {
					int[] vls = CollectionTointArray(lit);
					int[] htb = vls;
					String sValue = converToString(htb);
					UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.FREQUENCY_HOPTABLE.KEY, UHFSilionParams.FREQUENCY_HOPTABLE.PARAM_HTB, sValue);
					if (er == UHFReader.READER_STATE.OK_ERR) {
						Toast.makeText(mContext, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(mContext,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
				}

			}

		});
		
		cb_allsel = (CheckBox) findViewById(R.id.checkBox_allselect);
		cb_allsel.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@SuppressWarnings("static-access")
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				// TODO Auto-generated method stub
				MyAdapter2 m2 = (MyAdapter2) elist.getAdapter();
				if (arg1 == true) {
					HashMap<Integer, Boolean> isSelected = new HashMap<Integer, Boolean>();

					for (int m = 0; m < m2.getCount(); m++)
						isSelected.put(m, true);
					m2.setIsSelected(isSelected);

				} else {
					HashMap<Integer, Boolean> isSelected = new HashMap<Integer, Boolean>();

					for (int m = 0; m < m2.getCount(); m++)
						isSelected.put(m, false);
					m2.setIsSelected(isSelected);
				}
				elist.setAdapter(m2);
			}

		});
	}//end initView
	
	private void showlist(String[] fres) {
		ArrayList<String> list = new ArrayList<String>();
		list.addAll(Arrays.asList(fres));
		MyAdapter2 mAdapter = new MyAdapter2(list, this);
		// 绑定Adapter

		elist.setAdapter(mAdapter);
	}
	
	public int[] Sort(int[] array, int len) {
		int tmpIntValue = 0;
		for (int xIndex = 0; xIndex < len; xIndex++) {
			for (int yIndex = 0; yIndex < len; yIndex++) {
				if (array[xIndex] < array[yIndex]) {
					tmpIntValue = (Integer) array[xIndex];
					array[xIndex] = array[yIndex];
					array[yIndex] = tmpIntValue;
				}
			}
		}

		return array;
	}
	
	public static int[] CollectionTointArray(
			@SuppressWarnings("rawtypes") List list) {
		@SuppressWarnings("rawtypes")
		Iterator itor = list.iterator();
		int[] backdata = new int[list.size()];
		int i = 0;
		while (itor.hasNext()) {
			backdata[i++] = (int) (Integer) itor.next();
		}
		return backdata;
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
}
