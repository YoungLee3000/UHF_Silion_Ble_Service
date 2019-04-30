package com.nlscan.uhf.silion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.opengl.Visibility;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.nlscan.android.uhf.TagInfo;
import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;

public class UHFSilionSettingsActivity extends BaseActivity {

	String[] spireg = { "中国", "北美", "日本", "韩国", "欧洲", "欧洲2", "欧洲3", "印度",
			"加拿大", "全频段", "中国2" };//读写器工作区域
	String[] spibank = { "保留区", "EPC区", "TID区", "用户区" };//在进行gen2标签的盘存操作的同时可以读某个bank的数据
	String[] spifbank = { "EPC区", "TID区", "用户区" };//盘点过滤:过滤bank
	String[] spises = { "S0", "S1", "S2", "S3" };
	String[] spipow = { "500", "600", "700", "800", "900", "1000", "1100",
			"1200", "1300", "1400", "1500", "1600", "1700", "1800", "1900",
			"2000", "2100", "2200", "2300", "2400", "2500", "2600", "2700",
			"2800", "2900", "3000" };// 运行时添加
	String[] spiq = { "自动", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
			"10", "11", "12", "13", "14", "15" };
	String[] spinvmo = { "普通模式", "高速模式" };//快速模式
	String[] spiblf = { "40", "250", "400", "640" };
	String[] spimlen = { "96", "496" };
	String[] spitget = { "A", "B", "A-B", "B-A" };
	String[] spigcod = { "FM0", "M2", "M4", "M8" };
	String[] spitari = { "25微妙", "12.5微妙", "6.25微妙" };//Gen2协议Tari
	String[] spiwmod = { "字写", "块写" };
	String[] spi6btzsd = { "99percent", "11percent" };
	String[] spidelm = { "Delimiter1", "Delimiter4" };
	String[] spiperst = { "0%", "5%", "10%", "15%", "20%", "25%", "30%", "35%",
			"40%", "45%", "50%" };

	CheckBox cb_gpo1, cb_gpo2, cb_gpo3, cb_gpo4, cb_gpi1, cb_gpi2, cb_gpi3,
			cb_gpi4, cb_oant, cb_odata, cb_hrssi, cb_gen2, cb_6b, cb_ipx64,
			cb_ipx256, cb_ant1, cb_ant2, cb_ant3, cb_ant4, cb_allsel,
			cb_nostop, cbmf_readcount, cbmf_rssi, cbmf_ant, cbmf_fre,
			cbmf_time, cbmf_rfu, cbmf_pro, cbmf_dl, cb_fre;
	RadioGroup rg_emdenable, rg_antcheckenable, rg_invfilenable,
			rg_invfilmatch;

	private ArrayAdapter<String> arrdp_bank, arrdp_fbank, arrdp_ses,
			arradp_pow, arrdp_q, arrdp_invmo, arrdp_blf, arrdp_mlen,
			arrdp_tget, arrdp_g2cod, arrdp_tari, arrdp_wmod, arrdp_6btzsd,
			arrdp_delm, arradp_reg, arrdp_per;
	Spinner spinner_ant1rpow, spinner_ant1wpow, spinner_ant2rpow,
			spinner_ant2wpow, spinner_ant3rpow, spinner_ant3wpow,
			spinner_ant4rpow, spinner_ant4wpow, spinner_sesion, spinner_q,
			spinner_wmode, spinner_blf, spinner_maxlen, spinner_target,
			spinner_g2code, spinner_tari, spinner_emdbank, spinner_filbank,
			spinner_region, spinner_invmode, spinner_6btzsd, spinner_delmi,
			spinner_persen;
	TabHost tabHost_set;

	Button button_getantpower, button_setantpower, button_getantcheck,
			button_setantcheck, button_getgen2ses, button_setgen2ses,
			button_getgen2q, button_setgen2q, button_getwmode, button_setwmode,
			button_getgen2blf, button_setgenblf, button_getgen2maxl,
			button_setgen2maxl, button_getgen2targ, button_setgen2targ,
			button_getgen2code, button_setgen2code, button_getgen2tari,
			button_setgen2tari, button_setgpo, button_getgpi, button_getemd,
			button_setemd, button_getfil, button_setfil, button_getreg,
			button_setreg, button_getfre, button_setfre, button_getusl,
			button_setusl, button_invproset, button_opproget, button_opproset,
			button_invantsset,

			button_oantuqget, button_oantuqset, button_odatauqget,
			button_odatauqset, button_hrssiget, button_hrssiset,
			button_invmodeget, button_invmodeset, button_6bdpget,
			button_6bdpset, button_6bdltget, button_6bdltset, button_6bblfget,
			button_6bblfset, button_gettempture, button_nostop;
	
	private CheckBox checkbox_q1enable1200,
										checkbox_q2enable1200;
	
	private ListView elist;

	private UHFManager mUHFMgr = UHFManager.getInstance();
	
	private View createIndicatorView(Context context, TabHost tabHost,String title) {
		
		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		View tabIndicator = inflater.inflate(R.layout.tab_indicator_vertical,tabHost.getTabWidget(), false);
		final TextView titleView = (TextView) tabIndicator
				.findViewById(R.id.tv_indicator);
		titleView.setText(title);
		return tabIndicator;
	}

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.uhf_settings_tablelayout);

		spireg = getResources().getStringArray(R.array.region_items);
		spibank = getResources().getStringArray(R.array.bank_items_all);
		spifbank = getResources().getStringArray(R.array.bank_items_small);
		spinvmo = getResources().getStringArray(R.array.inventory_mode);
		spitari = getResources().getStringArray(R.array.gen2_tari);//Gen2协议Tari
		
		// 获取TabHost对象
		// 得到TabActivity中的TabHost对象
		tabHost_set = (TabHost) findViewById(R.id.tabhost4);
		// 如果没有继承TabActivity时，通过该种方法加载启动tabHost
		tabHost_set.setup();
		tabHost_set.getTabWidget().setOrientation(LinearLayout.VERTICAL);
		// tabHost2.addTab(tabHost2.newTabSpec("tab1").setIndicator("初始化",
		// getResources().getDrawable(R.drawable.ic_launcher)).setContent(R.id.tab11));
		// tabHost2.addTab(tabHost2.newTabSpec("tab1").setIndicator(createIndicatorView(this,
		// tabHost2, "1111"))
		// .setContent(R.id.tab11));
		tabHost_set.addTab(tabHost_set.newTabSpec("tab1")
				.setIndicator(createIndicatorView(this, tabHost_set, getString(R.string.inventory_params))) //盘点参数
				.setContent(R.id.uhf_settings_sub1_invusl));
		tabHost_set.addTab(tabHost_set.newTabSpec("tab2")
				.setIndicator(createIndicatorView(this, tabHost_set, getString(R.string.ants_power))) //天线功率
				.setContent(R.id.uhf_settings_sub2_antpow));
		tabHost_set.addTab(tabHost_set.newTabSpec("tab3")
				.setIndicator(createIndicatorView(this, tabHost_set, getString(R.string.region_freq))) //区域频率
				.setContent(R.id.uhf_settings_sub3_invfre));
		tabHost_set.addTab(tabHost_set.newTabSpec("tab4")
				.setIndicator(createIndicatorView(this, tabHost_set, getString(R.string.gen2_item))) //Gen2项
				.setContent(R.id.uhf_settings_sub4_gen2));
		tabHost_set.addTab(tabHost_set.newTabSpec("tab5")
				.setIndicator(createIndicatorView(this, tabHost_set, getString(R.string.inventory_filter))) //盘点过滤
				.setContent(R.id.uhf_settings_sub5_invfil));
		tabHost_set.addTab(tabHost_set.newTabSpec("tab6")
				.setIndicator(createIndicatorView(this, tabHost_set, getString(R.string.addition_data))) //附加数据
				.setContent(R.id.uhf_settings_sub6_emd));
		/*tabHost_set.addTab(tabHost_set.newTabSpec("tab7")
				.setIndicator(createIndicatorView(this, tabHost_set, "GPIO")) //GPIO
				.setContent(R.id.uhf_settings_sub7_gpio));*/
		tabHost_set.addTab(tabHost_set.newTabSpec("tab8")
				.setIndicator(createIndicatorView(this, tabHost_set, getString(R.string.other_params)))//其他参数
				.setContent(R.id.uhf_settings_sub8_others));
		tabHost_set.addTab(tabHost_set.newTabSpec("tab9")
				.setIndicator(createIndicatorView(this, tabHost_set, getString(R.string.quick_mode))) //快速模式
				.setContent(R.id.uhf_settings_sub9_quickly));
		TabWidget tw = tabHost_set.getTabWidget();
		tw.getChildAt(0).setBackgroundColor(Color.BLUE);
		// tabHost2.setCurrentTab(2);

		
		spinner_ant1rpow = (Spinner) findViewById(R.id.spinner_ant1rpow);
		spinner_ant1wpow = (Spinner) findViewById(R.id.spinner_ant1wpow);
		spinner_ant2rpow = (Spinner) findViewById(R.id.spinner_ant2rpow);
		spinner_ant2wpow = (Spinner) findViewById(R.id.spinner_ant2wpow);
		spinner_ant3rpow = (Spinner) findViewById(R.id.spinner_ant3rpow);
		spinner_ant3wpow = (Spinner) findViewById(R.id.spinner_ant3wpow);
		spinner_ant4rpow = (Spinner) findViewById(R.id.spinner_ant4rpow);
		spinner_ant4wpow = (Spinner) findViewById(R.id.spinner_ant4wpow);

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

		spinner_ant2rpow.setEnabled(false);
		spinner_ant2wpow.setEnabled(false);
		spinner_ant3rpow.setEnabled(false);
		spinner_ant3wpow.setEnabled(false);
		spinner_ant4rpow.setEnabled(false);
		spinner_ant4wpow.setEnabled(false);
		// */

		spinner_sesion = (Spinner) findViewById(R.id.spinner_gen2session);
		arrdp_ses = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spises);
		arrdp_ses
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_sesion.setAdapter(arrdp_ses);

		spinner_q = (Spinner) findViewById(R.id.spinner_gen2q);
		arrdp_q = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spiq);
		arrdp_q.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_q.setAdapter(arrdp_q);

		spinner_wmode = (Spinner) findViewById(R.id.spinner_gen2wmode);
		arrdp_wmod = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spiwmod);
		arrdp_wmod
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_wmode.setAdapter(arrdp_wmod);

		spinner_blf = (Spinner) findViewById(R.id.spinner_gen2blf);
		arrdp_blf = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spiblf);
		arrdp_blf
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_blf.setAdapter(arrdp_blf);

		spinner_maxlen = (Spinner) findViewById(R.id.spinner_gen2maxl);
		arrdp_mlen = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spimlen);
		arrdp_mlen
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_maxlen.setAdapter(arrdp_mlen);

		spinner_target = (Spinner) findViewById(R.id.spinner_target);
		arrdp_tget = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spitget);
		arrdp_tget
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_target.setAdapter(arrdp_tget);

		spinner_g2code = (Spinner) findViewById(R.id.spinner_gen2code);
		arrdp_g2cod = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spigcod);
		arrdp_g2cod
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_g2code.setAdapter(arrdp_g2cod);

		spinner_tari = (Spinner) findViewById(R.id.spinner_gen2tari);
		arrdp_tari = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spitari);
		arrdp_tari
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_tari.setAdapter(arrdp_tari);

		spinner_emdbank = (Spinner) findViewById(R.id.spinner_emdbank);
		arrdp_bank = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spibank);
		arrdp_bank
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_emdbank.setAdapter(arrdp_bank);

		spinner_filbank = (Spinner) findViewById(R.id.spinner_invfbank);
		arrdp_fbank = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spifbank);
		arrdp_fbank
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_filbank.setAdapter(arrdp_fbank);

		spinner_invmode = (Spinner) findViewById(R.id.spinner_invmode);
		arrdp_invmo = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spinvmo);
		arrdp_invmo
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_invmode.setAdapter(arrdp_invmo);

		spinner_6btzsd = (Spinner) findViewById(R.id.spinner_6bdlt);
		arrdp_6btzsd = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spi6btzsd);
		arrdp_6btzsd
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_6btzsd.setAdapter(arrdp_6btzsd);

		spinner_delmi = (Spinner) findViewById(R.id.spinner_6bdp);
		arrdp_delm = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spidelm);
		arrdp_delm
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_delmi.setAdapter(arrdp_delm);

		spinner_region = (Spinner) findViewById(R.id.spinner_region);
		arradp_reg = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spireg);
		arradp_reg
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_region.setAdapter(arradp_reg);

		spinner_persen = (Spinner) findViewById(R.id.spinner_per);
		arrdp_per = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, spiperst);
		arrdp_per
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_persen.setAdapter(arrdp_per);

		rg_antcheckenable = (RadioGroup) findViewById(R.id.radioGroup_antcheck);
		button_getantpower = (Button) findViewById(R.id.button_antpowget);
		button_setantpower = (Button) findViewById(R.id.button_antpowset);
		button_getantcheck = (Button) findViewById(R.id.button_checkantget);
		button_setantcheck = (Button) findViewById(R.id.button_checkantset);

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

		button_setgpo = (Button) findViewById(R.id.button_gposet);
		button_getgpi = (Button) findViewById(R.id.button_gpiget);

		button_gettempture = (Button) findViewById(R.id.button_tempure);
		button_nostop = (Button) findViewById(R.id.button_nostop);
		checkbox_q1enable1200 = (CheckBox) findViewById(R.id.checkbox_q1enable1200);
		checkbox_q2enable1200 = (CheckBox) findViewById(R.id.checkbox_q2enable1200);

		cb_gpo1 = (CheckBox) findViewById(R.id.checkBox_gpo1);
		cb_gpo2 = (CheckBox) findViewById(R.id.checkBox_gpo2);
		cb_gpo3 = (CheckBox) findViewById(R.id.checkBox_gpo3);
		cb_gpo4 = (CheckBox) findViewById(R.id.checkBox_gpo4);
		cb_gpi1 = (CheckBox) findViewById(R.id.checkBox_gpi1);
		cb_gpi2 = (CheckBox) findViewById(R.id.checkBox_gpi2);
		cb_gpi3 = (CheckBox) findViewById(R.id.checkBox_gpi3);
		cb_gpi4 = (CheckBox) findViewById(R.id.checkBox_gpi4);

		cb_gen2 = (CheckBox) findViewById(R.id.checkBox_invgen2);
		cb_6b = (CheckBox) findViewById(R.id.checkBox_inv6b);
		cb_ipx64 = (CheckBox) findViewById(R.id.checkBox_invipx64);
		cb_ipx256 = (CheckBox) findViewById(R.id.checkBox_invipx256);

		cb_ant1 = (CheckBox) findViewById(R.id.checkBox_ant1);
		cb_ant2 = (CheckBox) findViewById(R.id.checkBox_ant2);
		cb_ant3 = (CheckBox) findViewById(R.id.checkBox_ant3);
		cb_ant4 = (CheckBox) findViewById(R.id.checkBox_ant4);
		//天线
		Object obj = getUHFSetting(UHFSilionParams.ANTS.PARAM_MAX_ANTS_COUNT);
		int maxAntCount = 1;
		if(obj != null)
			maxAntCount = (int) obj;
		cb_ant1.setVisibility(maxAntCount >= 1 ? View.VISIBLE:View.GONE);
		cb_ant2.setVisibility(maxAntCount >= 2 ? View.VISIBLE:View.GONE);
		cb_ant3.setVisibility(maxAntCount >= 3? View.VISIBLE:View.GONE);
		cb_ant4.setVisibility(maxAntCount >= 4 ? View.VISIBLE:View.GONE);
		
		
		
		cb_allsel = (CheckBox) findViewById(R.id.checkBox_allselect);

		cbmf_readcount = (CheckBox) this.findViewById(R.id.checkBox_readcount);
		cbmf_rssi = (CheckBox) this.findViewById(R.id.checkBox_rssi);
		cbmf_ant = (CheckBox) this.findViewById(R.id.checkBox_ant);
		cbmf_fre = (CheckBox) this.findViewById(R.id.checkBox_frequency);
		cbmf_time = (CheckBox) this.findViewById(R.id.checkBox_time);
		cbmf_rfu = (CheckBox) this.findViewById(R.id.checkBox_rfu);
		cbmf_pro = (CheckBox) this.findViewById(R.id.checkBox_pro);
		cbmf_dl = (CheckBox) this.findViewById(R.id.checkBox_tagdatalen);
		cb_nostop = (CheckBox) findViewById(R.id.checkBox_nostop);

		button_getemd = (Button) findViewById(R.id.button_getemd);
		button_setemd = (Button) findViewById(R.id.button_setemd);
		rg_emdenable = (RadioGroup) findViewById(R.id.radioGroup_emdenable);
		button_getfil = (Button) findViewById(R.id.button_getfil);
		button_setfil = (Button) findViewById(R.id.button_setfil);
		button_getreg = (Button) findViewById(R.id.button_getregion);
		button_setreg = (Button) findViewById(R.id.button_setregion);
		button_getfre = (Button) findViewById(R.id.button_getfre);
		button_setfre = (Button) findViewById(R.id.button_setfre);
		button_getusl = (Button) findViewById(R.id.button_invuslget);
		button_setusl = (Button) findViewById(R.id.button_invuslset);

		button_invproset = (Button) findViewById(R.id.button_invproset);
		button_opproget = (Button) findViewById(R.id.button_opproget);
		button_opproset = (Button) findViewById(R.id.button_opproset);

		button_invantsset = (Button) findViewById(R.id.button_invantsset);

		rg_invfilenable = (RadioGroup) findViewById(R.id.radioGroup_enablefil);
		rg_invfilmatch = (RadioGroup) findViewById(R.id.radioGroup_invmatch);
		elist = (ListView) this.findViewById(R.id.listView_frequency);

		button_oantuqget = (Button) findViewById(R.id.button_oantuqget);
		button_oantuqset = (Button) findViewById(R.id.button_oantuqset);
		button_odatauqget = (Button) findViewById(R.id.button_odatauqget);
		button_odatauqset = (Button) findViewById(R.id.button_odatauqset);
		button_hrssiget = (Button) findViewById(R.id.button_hrssiget);
		button_hrssiset = (Button) findViewById(R.id.button_hrssiset);
		button_invmodeget = (Button) findViewById(R.id.button_invmodeget);
		button_invmodeset = (Button) findViewById(R.id.button_invmodeset);
		button_6bdpget = (Button) findViewById(R.id.button_6bdpget);
		button_6bdpset = (Button) findViewById(R.id.button_6bdpset);
		button_6bdltget = (Button) findViewById(R.id.button_6bdltget);
		button_6bdltset = (Button) findViewById(R.id.button_6bdltset);
		button_6bblfget = (Button) findViewById(R.id.button_6bblfget);
		button_6bblfset = (Button) findViewById(R.id.button_6bblfset);
		cb_oant = (CheckBox) findViewById(R.id.checkBox_oantuq);
		cb_odata = (CheckBox) findViewById(R.id.checkBox_odatauq);
		cb_hrssi = (CheckBox) findViewById(R.id.checkBox_hrssi);
		
		//读写器天线端口数
		int  antportc = getAntportCount();
		
		if (antportc >= 2) {
			spinner_ant2rpow.setEnabled(true);
			spinner_ant2wpow.setEnabled(true);
		}
		if (antportc >= 3) {
			spinner_ant3rpow.setEnabled(true);
			spinner_ant3wpow.setEnabled(true);
		}
		if (antportc >= 4) {
			spinner_ant4rpow.setEnabled(true);
			spinner_ant4wpow.setEnabled(true);
		}
		
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
					Toast.makeText(UHFSilionSettingsActivity.this,
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
						Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,
								Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(UHFSilionSettingsActivity.this,
								getString(R.string.setting_fail)+" : "+ er.toString(), Toast.LENGTH_SHORT)
								.show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(UHFSilionSettingsActivity.this,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					return;
				}

			}

		});
		
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
						Toast.makeText(UHFSilionSettingsActivity.this,	R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(UHFSilionSettingsActivity.this,
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
						Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail) + " : "+er.toString(), Toast.LENGTH_SHORT).show();
					
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(UHFSilionSettingsActivity.this,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
					return;
				}

			}

		});

		//获取:gen2 session
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
						Toast.makeText(UHFSilionSettingsActivity.this,R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(UHFSilionSettingsActivity.this,"Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
						Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
						
						int iQuickMode= getUHFIntSetting(UHFSilionParams.INV_QUICK_MODE.KEY,0);
						int[] iGenSessions = getUHFIntArraySetting(UHFSilionParams.POTL_GEN2_SESSION.KEY);
						iGenSessions = iGenSessions == null ? new int[]{-1}:iGenSessions;
						boolean q1enable1200 =  ( iQuickMode == 1 && iGenSessions[0] > 0 );
						boolean q2enable1200 =  ( iQuickMode == 1 && iGenSessions[0] ==  0 );
						checkbox_q1enable1200.setChecked(q1enable1200);
						checkbox_q2enable1200.setChecked(q2enable1200);
					} else
						Toast.makeText(UHFSilionSettingsActivity.this, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(UHFSilionSettingsActivity.this,"Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
					return;
				}

			}

		});
		
		//获取Gen2协议参数Q值
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
						Toast.makeText(UHFSilionSettingsActivity.this,R.string.no_data, Toast.LENGTH_SHORT)	.show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(UHFSilionSettingsActivity.this,
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
						Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(UHFSilionSettingsActivity.this, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(UHFSilionSettingsActivity.this,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					return;
				}

			}

		});

		//获取: Gen2协议写模式
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
						Toast.makeText(UHFSilionSettingsActivity.this,R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					e.printStackTrace();
					Toast.makeText(UHFSilionSettingsActivity.this,"Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
						Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(UHFSilionSettingsActivity.this, getString(R.string.setting_fail)+" : "+er.toString(),Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(UHFSilionSettingsActivity.this,	"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)	.show();
					e.printStackTrace();
					return;
				}

			}

		});
		
		//获取: Gen2协议后向链路速率
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
						Toast.makeText(UHFSilionSettingsActivity.this,R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					Toast.makeText(UHFSilionSettingsActivity.this,"Exception:" + e.getMessage(), Toast.LENGTH_SHORT).show();
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
						Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(UHFSilionSettingsActivity.this, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					Toast.makeText(UHFSilionSettingsActivity.this,
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
				Object obj = settingsMap.get(UHFSilionParams.POTL_GEN2_WRITEMODE.KEY);
				int[] val =(int[])obj;
				if ( val != null && val.length > 0) {
					if (val[0] == 96)
						spinner_maxlen.setSelection(0);
					else
						spinner_maxlen.setSelection(1);
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,R.string.no_data, Toast.LENGTH_SHORT).show();
			}

		});
		
		//设置: 支持的最大EPC长度，单位为bit
		button_setgen2maxl.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				int[] val = new int[] { spinner_maxlen.getSelectedItemPosition() == 0 ? 96 : 496 };
				String sValue = converToString(val);
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.POTL_GEN2_MAXEPCLEN.KEY, UHFSilionParams.POTL_GEN2_MAXEPCLEN.PARAM_POTL_GEN2_MAXEPCLEN, sValue);
				
				if (er == UHFReader.READER_STATE.OK_ERR) {
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(UHFSilionSettingsActivity.this, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
			}

		});
		
		//获取: Gen2协议目标
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
						Toast.makeText(UHFSilionSettingsActivity.this,R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					Toast.makeText(UHFSilionSettingsActivity.this,
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
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

			}

		});
		
		//获取: Gen2协议基带编码方式
		button_getgen2code.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
				Object obj = settingsMap.get(UHFSilionParams.POTL_GEN2_TARGET.KEY);
				int[] val =(int[])obj;
				
				if (val != null && val.length > 0 ) {
					spinner_g2code.setSelection(val[0]);
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,	R.string.no_data, Toast.LENGTH_SHORT).show();
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
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

			}
		});
		
		//获取: Gen2协议Tari
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
						Toast.makeText(UHFSilionSettingsActivity.this,	R.string.no_data, Toast.LENGTH_SHORT).show();

				} catch (Exception e) {
					// TODO Auto-generated catch block
					Toast.makeText(UHFSilionSettingsActivity.this,
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
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(UHFSilionSettingsActivity.this, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
			}

		});

		button_getgpi.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				/*int antportc = getAntportCount();
				int[][] gpi = new int[antportc][1];

				for (int i = 0; i < gpi.length; i++) {
					READER_ERR er = myapp.Mreader.GetGPI(i + 1, gpi[i]);

					if (er == READER_ERR.MT_OK_ERR) {
						if (i == 1) {
							if (gpi[i][0] == 1)
								cb_gpi1.setChecked(true);
							else
								cb_gpi1.setChecked(false);
						} else if (i == 2) {
							if (gpi[i][0] == 1)
								cb_gpi2.setChecked(true);
							else
								cb_gpi2.setChecked(false);
						} else if (i == 3) {
							if (gpi[i][0] == 1)
								cb_gpi3.setChecked(true);
							else
								cb_gpi3.setChecked(false);
						} else if (i == 4) {
							if (gpi[i][0] == 1)
								cb_gpi4.setChecked(true);
							else
								cb_gpi4.setChecked(false);
						}
					} else
						Toast.makeText(UHFSettingsActivity.this,
								"获取失败:" + er.toString(), Toast.LENGTH_SHORT)
								.show();
				}*/

			}

		});
		button_setgpo.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub

				/*int[][] gpo = new int[myapp.antportc][1];
				for (int i = 0; i < gpo.length; i++) {
					if (i == 0)
						gpo[0][0] = cb_gpo1.isChecked() ? 1 : 0;
					else if (i == 1)
						gpo[1][0] = cb_gpo2.isChecked() ? 1 : 0;
					else if (i == 2)
						gpo[2][0] = cb_gpo3.isChecked() ? 1 : 0;
					else if (i == 3)
						gpo[3][0] = cb_gpo4.isChecked() ? 1 : 0;
				}
				for (int i = 0; i < gpo.length; i++) {
					READER_ERR er = myapp.Mreader.SetGPO(i + 1, gpo[i][0]);
					if (er == READER_ERR.MT_OK_ERR)
						Toast.makeText(UHFSettingsActivity.this, "设置成功",
								Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(UHFSettingsActivity.this,
								"设置失败:" + er.toString(), Toast.LENGTH_SHORT)
								.show();
				}*/

			}

		});
		
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
						//Toast.makeText(UHFSilionSettingsActivity.this,	R.string.no_data, Toast.LENGTH_SHORT).show();
					
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
							Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
						} else
							Toast.makeText(UHFSilionSettingsActivity.this, getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

				} catch (Exception ex) {
					Toast.makeText(UHFSilionSettingsActivity.this,
							"Exception:" + ex.toString(), Toast.LENGTH_SHORT).show();
					ex.printStackTrace();
				}
			}
		});

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
						Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : "+er.toString(), Toast.LENGTH_SHORT).show();

				} catch (Exception ex) {
					Toast.makeText(UHFSilionSettingsActivity.this,
							"Exception:" + ex.toString(), Toast.LENGTH_SHORT).show();
					ex.printStackTrace();
				}
			}
		});
		
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
					Toast.makeText(UHFSilionSettingsActivity.this,R.string.no_data, Toast.LENGTH_SHORT).show();
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
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.unsupport_region,Toast.LENGTH_SHORT).show();
					return;
				}

				int iRegion = rre.value();
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.FREQUENCY_REGION.KEY, UHFSilionParams.FREQUENCY_REGION.PARAM_FREQUENCY_REGION, String.valueOf(iRegion));
				if (er == UHFReader.READER_STATE.OK_ERR) {
					button_getfre.performClick();
					Toast.makeText(UHFSilionSettingsActivity.this,R.string.setting_success,Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}

		});

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
					Toast.makeText(UHFSilionSettingsActivity.this,R.string.no_data, Toast.LENGTH_SHORT).show();

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
						Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success,Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
				}

			}

		});

		//获取,盘点超时,盘点间隔
		button_getusl.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				EditText ettime = (EditText) findViewById(R.id.editText_invtime);
				EditText etsleep = (EditText) findViewById(R.id.editText_invsleep);
				
				long invTimeout = UHFSilionParams.INV_TIME_OUT.DEFAULT_INV_TIMEOUT;
				long invInterval = UHFSilionParams.INV_INTERVAL.DEFAULT_INV_INTERVAL_TIME;
				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
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
						Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success, Toast.LENGTH_SHORT).show();
					if(er == UHFReader.READER_STATE.OK_ERR)
						Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
					
				} catch (Exception e) {
					Toast.makeText(UHFSilionSettingsActivity.this,
							"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)
							.show();
					e.printStackTrace();
					return;
				}
				
			}

		});

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
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.select_protocol,Toast.LENGTH_SHORT).show();
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
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success, Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
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
				// TODO Auto-generated method stub

			}

		});

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
					Toast.makeText(UHFSilionSettingsActivity.this,R.string.select_inv_ants, Toast.LENGTH_SHORT).show();
					return;
				}

				Integer[] ants = ltp.toArray(new Integer[ltp.size()]);
				int[] uants = new int[ants.length];
				for (int i = 0; i < ants.length; i++)
					uants[i] = ants[i];

				String sValue = converToString(uants);
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.ANTS.KEY, UHFSilionParams.ANTS.PARAM_ANTS_GROUP, sValue);
				if(er == UHFReader.READER_STATE.OK_ERR)
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success, Toast.LENGTH_SHORT).show();
				else
					Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}

		});

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
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.no_data, Toast.LENGTH_SHORT).show();
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
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success, Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}

		});
		
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
					Toast.makeText(UHFSilionSettingsActivity.this,R.string.no_data, Toast.LENGTH_SHORT).show();
			}

		});
		
		//设置: Epc相同的标签如果在使用嵌入盘存读功能时，读出的其它bank数据不同，是否作为多条标签数据
		button_odatauqset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				int[] val = new int[] { -1 };
				val[0] = cb_odata.isChecked() ? 1 : 0;
				String sValue = converToString(val);
				
				UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.KEY, UHFSilionParams.TAGDATA_UNIQUEBYEMDDATA.PARAM_TAGDATA_UNIQUEBYEMDDATA, sValue);
				
				if (er == UHFReader.READER_STATE.OK_ERR) {
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success, Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}

		});

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
					Toast.makeText(UHFSilionSettingsActivity.this,R.string.no_data, Toast.LENGTH_SHORT).show();
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
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success, Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}

		});
		
		//获取:标签盘存模式(只有m6e系列读写器支持此参数；0表示普通盘存模式，1表示高速盘存模式，适用于标签数量较少但移动速度非常快的场合)
		button_invmodeget.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				int[] val = new int[] { -1 }; //不支持该参数
				if (val[0] != -1) {
					spinner_invmode.setSelection(val[0]);
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,	R.string.unsupport,Toast.LENGTH_SHORT).show();
			}

		});
		
		//设置:标签盘存模式(只有m6e系列读写器支持此参数；0表示普通盘存模式，1表示高速盘存模式，适用于标签数量较少但移动速度非常快的场合)
		button_invmodeset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				int[] val = new int[] { spinner_invmode.getSelectedItemPosition() };
				val[0] = -1; //不支持该参数
				if (val[0] != -1) {
					Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success, Toast.LENGTH_SHORT).show();
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,	R.string.unsupport,Toast.LENGTH_SHORT).show();

			}

		});

		button_6bdpget.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Toast.makeText(UHFSilionSettingsActivity.this,	R.string.unsupport,Toast.LENGTH_SHORT).show();
			}

		});
		button_6bdpset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Toast.makeText(UHFSilionSettingsActivity.this,	R.string.unsupport,Toast.LENGTH_SHORT).show();
			}
		});

		button_6bdltget.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Toast.makeText(UHFSilionSettingsActivity.this,	R.string.unsupport,Toast.LENGTH_SHORT).show();

			}

		});
		button_6bdltset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Toast.makeText(UHFSilionSettingsActivity.this,	R.string.unsupport,Toast.LENGTH_SHORT).show();
			}

		});

		//获取,180006b协议后向链路速率(只支持40和160)
		button_6bblfget.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				int[] val = null;
				Map<String,Object> settingsMap = mUHFMgr.getAllParams();
				Object obj = settingsMap.get(UHFSilionParams.POTL_ISO180006B_BLF.KEY);
				if(obj != null)
					val = (int[])obj;

				if (val != null && val.length > 0) {
					EditText et = (EditText) findViewById(R.id.editText_6bblf);
					et.setText(String.valueOf(val[0]));
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,R.string.no_data, Toast.LENGTH_SHORT).show();

			}

		});
		
		//设置,180006b协议后向链路速率(只支持40和160)
		button_6bblfset.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				
				try {
					EditText et = (EditText) findViewById(R.id.editText_6bblf);
					String sValue = et.getText()	.toString();
					int[] val = null;
					if( sValue != null && TextUtils.isDigitsOnly(sValue))
						val = new int[] { Integer.valueOf(sValue) };
					
					sValue = converToString(val);
					UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.POTL_ISO180006B_BLF.KEY, UHFSilionParams.POTL_ISO180006B_BLF.PARAM_POTL_ISO180006B_BLF, sValue);
					if (er == UHFReader.READER_STATE.OK_ERR) {
						Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success, Toast.LENGTH_SHORT).show();
					} else
						Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
					
				} catch (Exception e) {
					Toast.makeText(UHFSilionSettingsActivity.this,	"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)	.show();
					e.printStackTrace();
					return;
				}
			}

		});
		
		button_gettempture.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				EditText et = (EditText) findViewById(R.id.editText_tempure);
				try {
					String sValue = mUHFMgr.getParam(UHFSilionParams.TEMPTURE.KEY, UHFSilionParams.TEMPTURE.KEY, null);
					if( sValue != null && TextUtils.isDigitsOnly(sValue))
						et.setText(sValue);
				} catch (Exception e) {
					Toast.makeText(UHFSilionSettingsActivity.this,	"Exception:" + e.getMessage(), Toast.LENGTH_SHORT)	.show();
					e.printStackTrace();
				}
			}

		});
		
		//1200模块启用快速模式
		button_nostop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {

				int metaflag = 0;
				if (cbmf_readcount.isChecked())
					metaflag |= 0X0001;
				if (cbmf_rssi.isChecked())
					metaflag |= 0X0002;
				if (cbmf_ant.isChecked())
					metaflag |= 0X0004;
				if (cbmf_fre.isChecked())
					metaflag |= 0X0008;
				if (cbmf_time.isChecked())
					metaflag |= 0X0010;
				if (cbmf_rfu.isChecked())
					metaflag |= 0X0020;
				if (cbmf_pro.isChecked())
					metaflag |= 0X0040;
				if (cbmf_dl.isChecked())
					metaflag |= 0X0080;

				int option = (metaflag<<8)
						| spinner_persen.getSelectedItemPosition();
				
				boolean nostop =false;
				if (cb_nostop.isChecked())
					nostop = true;
				else
					nostop = false;
			}
		});
		
		int iQuickMode= getUHFIntSetting(UHFSilionParams.INV_QUICK_MODE.KEY,0);
		int[] iGenSessions = getUHFIntArraySetting(UHFSilionParams.POTL_GEN2_SESSION.KEY);
		iGenSessions = iGenSessions == null ? new int[]{-1}:iGenSessions;
		boolean q1enable1200 =  ( iQuickMode == 1 && iGenSessions[0] > 0 );
		boolean q2enable1200 =  ( iQuickMode == 1 && iGenSessions[0] ==  0 );
		checkbox_q1enable1200.setChecked(q1enable1200);
		checkbox_q2enable1200.setChecked(q2enable1200);
		
		checkbox_q1enable1200.setEnabled(!q2enable1200);
		checkbox_q2enable1200.setEnabled(!q1enable1200);
		
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
					//Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success, Toast.LENGTH_SHORT).show();
					if(isChecked)
						checkbox_q2enable1200.setEnabled(false);
					else
						checkbox_q2enable1200.setEnabled(true);
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
				
				
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
					//Toast.makeText(UHFSilionSettingsActivity.this, R.string.setting_success, Toast.LENGTH_SHORT).show();
					if(isChecked)
						checkbox_q1enable1200.setEnabled(false);
					else
						checkbox_q1enable1200.setEnabled(true);
				} else
					Toast.makeText(UHFSilionSettingsActivity.this,getString(R.string.setting_fail)+" : " + er.toString(), Toast.LENGTH_SHORT).show();
			}
		});
		
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
		tabHost_set.setOnTabChangedListener(new OnTabChangeListener() {

			@Override
			public void onTabChanged(String arg0) {
				// TODO Auto-generated method stub
				int j = tabHost_set.getCurrentTab();
				TabWidget tabIndicator = tabHost_set.getTabWidget();
				View vw = tabIndicator.getChildAt(j);
				vw.setBackgroundColor(Color.BLUE);
				int tc = tabHost_set.getTabContentView().getChildCount();
				for (int i = 0; i < tc; i++) {
					if (i != j) {
						View vw2 = tabIndicator.getChildAt(i);
						if(vw2 != null)
							vw2.setBackgroundColor(Color.TRANSPARENT);
					} else {
						switch (i) {
						case 0:

							button_getusl.performClick();
							button_opproget.performClick();
							break;
						case 1:
							button_getantcheck.performClick();
							button_getantpower.performClick();
							break;
						case 2:
							button_getreg.performClick();
							button_getfre.performClick();
							break;
						case 3:
							button_getgen2ses.performClick();
							button_getgen2q.performClick();
							button_getwmode.performClick();
							// button_getgen2blf.performClick();
							button_getgen2maxl.performClick();
							button_getgen2targ.performClick();
							button_getgen2code.performClick();
							// button_getgen2tari.performClick();
							break;
						case 4:
							button_getfil.performClick();
							break;
						case 5:
							button_getemd.performClick();
							break;
						case 6:
							button_oantuqget.performClick();
							button_odatauqget.performClick();
							button_hrssiget.performClick();
							button_gettempture.performClick();
							break;
						}
					}
				}

			}

		});

		cb_gen2.setChecked(true);
		cb_6b.setChecked(false);
		cb_ipx64.setChecked(false);
		cb_ipx256.setChecked(false);
		
		obj = getUHFSetting(UHFSilionParams.TAG_INVPOTL.KEY);
		int[] invpro = new int[]{TagInfo.SL_TagProtocol.SL_TAG_PROTOCOL_NONE.value()};
		if(obj != null)
			invpro = (int[])obj;
		
		for (int k = 0; k < invpro.length; k++) 
		{
			int iPro = invpro[k];
			TagInfo.SL_TagProtocol protocol = TagInfo.SL_TagProtocol.valueOf(iPro);
			switch (protocol) {
			case SL_TAG_PROTOCOL_IPX256:
				cb_ipx256.setChecked(true);
				break;
			case SL_TAG_PROTOCOL_IPX64:
				cb_ipx64.setChecked(true);
				break;
			case SL_TAG_PROTOCOL_GEN2:
				cb_gen2.setChecked(true);
				break;
			case SL_TAG_PROTOCOL_ISO180006B:
				cb_6b.setChecked(true);
				break;
			case SL_TAG_PROTOCOL_ISO180006B_UCODE:
				
				break;
			case SL_TAG_PROTOCOL_NONE:
			default:
				break;
			}
		}

		cb_ant1.setChecked(false);
		cb_ant2.setChecked(false);
		cb_ant3.setChecked(false);
		cb_ant4.setChecked(false);
		
		obj = getUHFSetting(UHFSilionParams.ANTS.PARAM_ANTS_GROUP);
		int[] uants = new int[]{1};
		if(obj != null)
			uants = (int[])obj;
		
		for (int k = 0; k < uants.length; k++) {
			if (uants[k] == 1) {
				cb_ant1.setChecked(true);
			} else if (uants[k] == 2) {
				cb_ant2.setChecked(true);
			} else if (uants[k] == 3) {
				cb_ant3.setChecked(true);
			} else if (uants[k] == 4) {
				cb_ant4.setChecked(true);
			}
		}
		
		this.button_getusl.performClick();
		
		initActionBar();
		
	}//end onCreate

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
	
	long exittime = 0L;
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		/*if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) 
		{
			if ((System.currentTimeMillis() - exittime) > 2000) {
				Toast.makeText(getApplicationContext(), "再按一次退出程序",
						Toast.LENGTH_SHORT).show();
				exittime = System.currentTimeMillis();
			} else {
				finish();
				// System.exit(0);
			}
			return true;
		}*/
		return super.onKeyDown(keyCode, event);
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
		Map<String,Object> settingsMap = mUHFMgr.getAllParams();
		if(settingsMap != null)
			return  settingsMap.get(key);
		
		return null;
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
