package com.nlscan.uhf.silionBle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Parcelable;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.nlscan.android.uhf.TagInfo;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.uhf.silion.ISilionUHFService;
import com.nlscan.uhf.silionBle.upgrade.Native;

public class MainActivity extends Activity {

	
	/**读码结果发送的广播action*/
	public final static String ACTION_UHF_RESULT_SEND = "nlscan.intent.action.uhf.ACTION_RESULT";
	/**读码结果发送的广播Extra*/
	public final static String EXTRA_TAG_INFO = "tag_info";
	
	private Button btn_power_on,
								btn_power_off,
								btn_start_read,
								btn_stop_read,
								btn_clear,
								btn_charge;
	
	private TextView tv_once,
										tv_state,
										tv_tags, 
										tv_costt,
										tv_all_time,
										tv_charge;
	
	private Context mContext;
	private ISilionUHFService mIUHFService;
	private ListView listView;
	private MyAdapter Adapter;
	
	Map<String, TagInfo> TagsMap = new LinkedHashMap<String, TagInfo>();// 有序
	private int gTagsTimesCount = 0;
	private List<Map<String, ?>> ListMs = new ArrayList<Map<String, ?>>();
	private String[] Coname = new String[] { "序号", "EPC ID", "次数", "天线", "协议", "RSSI", "频率", "附加数据" };
	
	private Map<String, String> listHeader = new HashMap<String, String>(); 
	
	private long exittime;

	private Native crcModel = new Native();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mContext = getApplicationContext();
		initView();
		bindUHFService();

		Log.d("bletest","the crc " +
				crcModel.getCrcStr(HexUtil.toByteArray("FF0003")));

	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN)
		{
			if ((System.currentTimeMillis() - exittime) > 2000) {
				Toast.makeText(getApplicationContext(), R.string.press_again_to_exit, Toast.LENGTH_SHORT).show();
				exittime = System.currentTimeMillis();
			}else
				return super.onKeyDown(keyCode, event);
		}

		return true;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		unRegisterResultReceiver();
	}


	@Override
	protected void onResume() {
		super.onResume();
		registerResultReceiver();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		stopReading();
		powerOff();
		mContext.unbindService(mServiceConnection);
	}


	private void initView()
	{
		btn_power_on = (Button) findViewById(R.id.btn_power_on);
		btn_power_off =  (Button)  findViewById(R.id.btn_power_off);
		btn_start_read = (Button)  findViewById(R.id.btn_start_read);
		btn_stop_read =  (Button) findViewById(R.id.btn_stop_read);
		btn_clear =  (Button) findViewById(R.id.btn_clear);
		btn_charge = (Button) findViewById(R.id.btn_charge);
		listView = (ListView) findViewById(R.id.listView_epclist);
		
		tv_once = (TextView) findViewById(R.id.textView_readoncecnt);
		tv_tags = (TextView) findViewById(R.id.textView_readallcnt);
		tv_all_time = (TextView) findViewById(R.id.textView_readalltime);
		tv_charge = (TextView) findViewById(R.id.textView_charge_val);


		for (int i = 0; i < Coname.length; i++)
			listHeader.put(Coname[i], Coname[i]);
		
		ListMs.add(listHeader);
		Adapter = new MyAdapter(mContext, ListMs, R.layout.listitemview_inv, Coname, new int[] { R.id.textView_readsort, R.id.textView_readepc, R.id.textView_readcnt,
			R.id.textView_readant, R.id.textView_readpro, R.id.textView_readrssi, R.id.textView_readfre, R.id.textView_reademd });
		
		listView.setAdapter(Adapter);
		
		btn_power_on.setOnClickListener(mClick);
		btn_power_off.setOnClickListener(mClick);
		btn_start_read.setOnClickListener(mClick);
		btn_stop_read.setOnClickListener(mClick);
		btn_clear.setOnClickListener(mClick);
	}
	
	/**
	 * 绑定UHF服务
	 */
	private void bindUHFService()
	{
		Intent intent = new Intent("nlscan.intent.action.uhf.UFH_SERVICE");
		intent.setPackage("com.nlscan.uhf.silionBle");
		mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
	}
	
	private void registerResultReceiver()
	{
		try {
			IntentFilter iFilter = new IntentFilter(ACTION_UHF_RESULT_SEND);
			mContext.registerReceiver(mResultReceiver, iFilter);
		} catch (Exception e) {
		}
		
	}
	
	private void unRegisterResultReceiver()
	{
		try {
			mContext.unregisterReceiver(mResultReceiver);
		} catch (Exception e) {
		}
		
	}
	
	/**
	 * 上电(连接)
	 */
	private void powerOn()
	{
		UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR;
		try {
			int state = mIUHFService.powerOn();
			er = UHFReader.READER_STATE.valueOf(state);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
//		Toast.makeText(getApplicationContext(), "Power on :"+er.toString(), Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * 下电(断开)
	 */
	private void powerOff()
	{
		UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR;
		try {
			int state = mIUHFService.powerOff();
			er = UHFReader.READER_STATE.valueOf(state);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
//		Toast.makeText(getApplicationContext(), "Power off :"+er.toString(), Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * 开始扫描
	 */
	private void startReading()
	{
		powerOn();
		UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR;
		try {
			int state = mIUHFService.startTagInventory();
			er = UHFReader.READER_STATE.valueOf(state);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		Toast.makeText(getApplicationContext(), "Start reading :"+er.toString(), Toast.LENGTH_SHORT).show();
	}
	
	/**
	 * 停止扫描
	 */
	private void stopReading()
	{
		UHFReader.READER_STATE er = UHFReader.READER_STATE.CMD_FAILED_ERR;
		try {
			int state = mIUHFService.stopTagInventory();
			er = UHFReader.READER_STATE.valueOf(state);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (er == UHFReader.READER_STATE.OK_ERR)
			btn_start_read.setEnabled(true);
		Toast.makeText(getApplicationContext(), "Stop reading :"+er.toString(), Toast.LENGTH_SHORT).show();
		powerOff();
	}
	
	/**
	 * 清除数据
	 */
	private void clearData()
	{
		
		tv_once.setText(String.valueOf(0));
		tv_tags.setText(String.valueOf(0));
		tv_all_time.setText(String.valueOf(0));

		gTagsTimesCount = 0;
		
		if(TagsMap != null)
			TagsMap.clear();
		
		if(ListMs != null){
			ListMs.clear();
			ListMs.add(listHeader);
		}
		
		Adapter.notifyDataSetChanged();
		
	}


	/**
	 * 获取电量
	 */
	private void getCharge(){
		try {
			String chargeVal = mIUHFService.getParam("CHARGE_VALUE",
					"PARAM_CHARGE_VALUE");
			tv_charge.setText(chargeVal + "%");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	private View.OnClickListener mClick = new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			
			switch (v.getId()) {
			case R.id.btn_power_on:
				powerOn();
				break;
			case R.id.btn_power_off:
				powerOff();
				break;
			case R.id.btn_start_read:
				btn_start_read.setEnabled(false);
				startReading();
				break;
			case R.id.btn_stop_read:

				stopReading();
				break;
			case R.id.btn_clear:
				clearData();
				break;
			case R.id.btn_charge:
				getCharge();
				break;

			}
		}
	};
	
	private ServiceConnection mServiceConnection = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName name) {
			
		}
		
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mIUHFService = ISilionUHFService.Stub.asInterface(service);
			Toast.makeText(getApplicationContext(), "Binder service success.", Toast.LENGTH_SHORT).show();
		}
	};
	
	public class MyAdapter extends SimpleAdapter
	{
		 private int cr;
		 
		public MyAdapter(Context context, List<? extends Map<String, ?>> data,
				int resource, String[] from, int[] to)
		{
			super(context, data, resource, from, to);
			cr=Color.WHITE;
		}
	    public void setColor(int color)
	    {
	    	cr=color;
	    }
	   
		@Override      
		public View getView(final int position, View convertView, ViewGroup parent)
		{           
			// listview每次得到一个item，都要view去绘制，通过getView方法得到view           
			// position为item的序号           
			View view = null;           
			if (convertView != null) {
				view = convertView;
				// 使用缓存的view,节约内存
				// 当listview的item过多时，拖动会遮住一部分item，被遮住的item的view就是convertView保存着。
				// 当滚动条回到之前被遮住的item时，直接使用convertView，而不必再去new view()
				} else {
					view = super.getView(position, convertView, parent);
					}
			int[] colors = {cr, Color.rgb(219, 238, 244) };//RGB颜色 
			 view.setBackgroundColor(colors[position % 2]);// 每隔item之间颜色不同 
			 //Log.d("MYINFO", "getview:"+String.valueOf(position));
			 return super.getView(position, view, parent); 
		}
	}//end MyAdapter
	
	private final static int MSG_REFRESH_RESULT_LIST = 0x01;
	private Handler mUIHandler = new Handler(){

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MSG_REFRESH_RESULT_LIST:
				int curTagCount = msg.arg1;
				int totalTagCount = msg.arg2;
				int totalTimes = (int) msg.obj;
				
				tv_once.setText(String.valueOf(curTagCount));
				tv_tags.setText(String.valueOf(totalTagCount));
				tv_all_time.setText(String.valueOf(totalTimes));
				Adapter.notifyDataSetChanged();
				break;
			}
		}
		
	};
	private BroadcastReceiver mResultReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if(!ACTION_UHF_RESULT_SEND.equals(action))
				return ;
			
			Parcelable[] tagInfos =  intent.getParcelableArrayExtra(EXTRA_TAG_INFO);
			if(tagInfos != null && tagInfos.length > 0)
			{
				
				for(int i =0 ;i < tagInfos.length; i++)
				{
					TagInfo tag = (TagInfo) tagInfos[i];
					if (tag == null) continue;
					String epcId = HexUtil.bytesToHexString(tag.EpcId);
					
					if (!TagsMap.containsKey(epcId)) {
						TagsMap.put(epcId, tag);

						// show
						Map<String, String> m = new HashMap<String, String>();
						m.put(Coname[0], String.valueOf(TagsMap.size()));

						String epcstr =epcId;
						if (epcstr.length() < 24)
							epcstr = String.format("%-24s", epcstr);

						m.put(Coname[1], epcstr);
						String cs = m.get("次数");
						if (cs == null)
							cs = "0";
						int isc = Integer.parseInt(cs) + tag.ReadCnt;

						m.put(Coname[2], String.valueOf(isc));
						m.put(Coname[3], String.valueOf(tag.AntennaID));
						m.put(Coname[4], "");
						m.put(Coname[5], String.valueOf(tag.RSSI));
						m.put(Coname[6], String.valueOf(tag.Frequency));

						if (tag.EmbededDatalen > 0) {
							String out = HexUtil.bytesToHexString(tag.EmbededData);
							m.put(Coname[7], String.valueOf(out));

						} else
							m.put(Coname[7], "                 ");

						ListMs.add(m);
					} else {
						TagInfo tf = TagsMap.get(epcId);

						String epcstr = epcId;
						if (epcstr.length() < 24)
							epcstr = String.format("%-24s", epcstr);

						for (int k = 0; k < ListMs.size(); k++) {
							Map<String, String> m = (Map<String, String>) ListMs.get(k);
							if (m.get(Coname[1]).equals(epcstr)) {
								tf.ReadCnt += tag.ReadCnt;
								tf.RSSI = tag.RSSI;
								tf.Frequency = tag.Frequency;

								m.put(Coname[2], String.valueOf(tf.ReadCnt));
								m.put(Coname[5], String.valueOf(tf.RSSI));
								m.put(Coname[6], String.valueOf(tf.Frequency));
								break;
							}
						}
					}
				}//end for
				
				int cll = TagsMap.size();
				if (cll < 0)
					cll = 0;
				gTagsTimesCount += tagInfos.length;
				Message msg = Message.obtain(mUIHandler, MSG_REFRESH_RESULT_LIST, tagInfos.length, cll,gTagsTimesCount);
				mUIHandler.sendMessageDelayed(msg, 50);
			}//end if
			
		}//end onReceiver
	};
	
}
