package com.nlscan.uhf.silion;

import java.util.Map;

import org.json.JSONObject;

import android.app.Application;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.nlscan.android.uhf.UHFManager;
import com.nlscan.android.uhf.UHFReader;
import com.nlscan.android.uhf.UHFReader.Lock_Obj;
import com.nlscan.android.uhf.UHFReader.Lock_Type;

public class TagReadLockActivity extends BaseActivity {

	String[] spibank;//={"保留区","EPC区","TID区","用户区"}; 
	String[] spifbank;//={"EPC区","TID区","用户区"}; 
	String[] spilockbank;//={"访问密码","销毁密码","EPCbank","TIDbank","USERbank"}; 
	String[] spilocktype;//={"解锁定","暂时锁定","永久锁定"};
	
	Button button_readop,
					button_writeop,
					button_writepc,
					button_lockop,
					button_kill;
	
	Spinner spinner_opbank,
					spinner_bankr,
					spinner_bankw,
					spinner_lockbank,
					spinner_locktype; 
	TabHost tabHost_op;
	 RadioGroup gr_opant,gr_match,gr_enablefil,gr_wdatatype;
	 CheckBox cb_pwd;
	// Switch swt_fil;
	@SuppressWarnings("rawtypes")
	private ArrayAdapter arradp_bank,arradp_fbank,arradp_lockbank,arradp_locktype;
	public static EditText EditText_sub3fildata,EditText_sub3wdata;
	
	private int opant = 1;
	private int antCount = 1;
	private UHFManager mUHFMgr;
	private String sHexPasswd;
	
	private View createIndicatorView(Context context, TabHost tabHost, String title) {
		  LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
 
		  View tabIndicator = inflater.inflate(R.layout.tab_indicator_vertical, tabHost.getTabWidget(), false);
		  final TextView titleView = (TextView) tabIndicator.findViewById(R.id.tv_indicator);
		  titleView.setText(title);
		  return tabIndicator;
		 }

	 private int SortGroup(RadioGroup rg)
		{
			 int check1=rg.getCheckedRadioButtonId();
			    if(check1!=-1)
			    {
			    	for(int i=0;i<rg.getChildCount();i++)
			    	{ 
			    	  View vi=rg.getChildAt(i);
			    	  int vv=vi.getId();
			    	  if(check1==vv)
			    	  {
			    		  return i;
			    	  }
			    	}
			    	
			    	return -1;
			    }
			    else
			    	return check1;
		}
	 
	 private void SetOpant() 
	 {
		 	 opant=SortGroup(gr_opant)+1;
	 }
	 
	 private void SetFiler()
	 { 
		 if(SortGroup(gr_enablefil)==1)
		 {  
			 EditText et_fdata=(EditText)findViewById(R.id.editText_opfilterdata);
			 EditText etadr=(EditText)findViewById(R.id.editText_opfilsadr);
			 
			 int bank=spinner_opbank.getSelectedItemPosition()+1;
			 String sAddr = etadr.getText().toString();
			 int startaddr = 0;
			 if(sAddr != null && TextUtils.isDigitsOnly(sAddr))
				 startaddr =  Integer.valueOf(sAddr);
			 
			 String sHexData =et_fdata.getText().toString();
			 
			 int isInvert = 0;
			 int ma=SortGroup(gr_match);
			 if(ma==1)
				 isInvert=1;
			 else
				 isInvert=0;
			 
			 try {
				//,{"bank":1,"fdata":0A0B,"flen:"8,"startaddr":2,"isInvert":1}
				 JSONObject jsFilter = new JSONObject();
				 jsFilter.put("bank", bank);
				 jsFilter.put("startaddr", startaddr);
				 jsFilter.put("fdata", sHexData);
				 jsFilter.put("isInvert", isInvert);
				 
				 String sValue = jsFilter.toString();
				 UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.TAG_FILTER.KEY, UHFSilionParams.TAG_FILTER.PARAM_TAG_FILTER, sValue);
				 if(er == UHFReader.READER_STATE.OK_ERR)
					 Toast.makeText(TagReadLockActivity.this, R.string.success,Toast.LENGTH_SHORT).show();
				 else
					 Toast.makeText(TagReadLockActivity.this, R.string.failed,Toast.LENGTH_SHORT).show();
				 
			} catch (Exception e) {
			}
			 
		 } else{ 
			 UHFReader.READER_STATE er = mUHFMgr.setParam(UHFSilionParams.TAG_FILTER.KEY, UHFSilionParams.TAG_FILTER.PARAM_CLEAR, "1");
		 }
	 }
	 
	 private void SetPassword() 
	 {
		sHexPasswd = "";
		if (cb_pwd.isChecked()) 
		{
			EditText et = (EditText) findViewById(R.id.editText_password);
			sHexPasswd = et.getText().toString();
		}
	 }
	 
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.read_lock_tablelayout);
		Application app=getApplication();
		mUHFMgr = UHFManager.getInstance();
		spibank=new String[]{getString(R.string.reserve_bank),
													getString(R.string.epc_bank),
													getString(R.string.tid_bank) , 
													getString(R.string.user_bank) };//{"保留区","EPC区","TID区","用户区"}; 
		spifbank=new String[]{
									getString(R.string.epc_bank),
									getString(R.string.tid_bank) , 
									getString(R.string.user_bank) };//{"EPC区","TID区","用户区"}; 
		
		spilockbank=new String[]{
									getString(R.string.access_pwd),
									getString(R.string.destroy_pwd) , 
									"EPCbank",
									"TIDbank",
									"USERbank"};//{"访问密码","销毁密码","EPCbank","TIDbank","USERbank"}; 
		
		spilocktype=new String[]{
								getString(R.string.release_lock),
								getString(R.string.temp_lock) , 
								getString(R.string.fix_lock) };//{"解锁定","暂时锁定","永久锁定"};
		
		//操作天线
		opant = getIntSettings(UHFSilionParams.ANTS.PARAM_OPERATE_ANTS, 1);
		int[] antArrays = getIntArraySettings(UHFSilionParams.READER_CONN_ANTS.KEY, new int[]{1});
		if(antArrays != null)
			antCount = antArrays.length;
		
		initActionBar();
		
		// 获取TabHost对象          
		 // 得到TabActivity中的TabHost对象
		tabHost_op = (TabHost) findViewById(R.id.tabhost3); 
		// 如果没有继承TabActivity时，通过该种方法加载启动tabHost 
		tabHost_op.setup(); 
		tabHost_op.getTabWidget().setOrientation(LinearLayout.VERTICAL);
		//tabHost2.addTab(tabHost2.newTabSpec("tab1").setIndicator("初始化",  
		//getResources().getDrawable(R.drawable.ic_launcher)).setContent(R.id.tab11)); 
	    //tabHost2.addTab(tabHost2.newTabSpec("tab1").setIndicator(createIndicatorView(this, tabHost2, "1111"))
	    	//	.setContent(R.id.tab11)); 
	   
		//读标签
		tabHost_op.addTab(tabHost_op.newTabSpec("tab1").setIndicator(createIndicatorView(this, tabHost_op, getString(R.string.read_tag)))
				.setContent(R.id.tab3_sub_read)); 
		//写标签
		tabHost_op.addTab(tabHost_op.newTabSpec("tab2").setIndicator(createIndicatorView(this, tabHost_op, getString(R.string.write_tag))) 
				.setContent(R.id.tab3_sub_write)); 
		//锁与销毁
		tabHost_op.addTab(tabHost_op.newTabSpec("tab3").setIndicator(createIndicatorView(this, tabHost_op, getString(R.string.lock_destroy)))
				.setContent(R.id.tab3_sub_lockkill)); 
				
				TabWidget tw=tabHost_op.getTabWidget();
				tw.getChildAt(0).setBackgroundColor(Color.BLUE);
				 
				spinner_bankr = (Spinner) findViewById(R.id.spinner_bankr); 
				// View layout = getLayoutInflater().inflate(R.layout.tab3_tablelayout_write, null);
				spinner_bankw= (Spinner)findViewById(R.id.spinner_bankw);
				spinner_opbank= (Spinner)findViewById(R.id.spinner_opfbank);	 
				arradp_bank = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,spibank); 
				arradp_bank.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);  
				arradp_fbank = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,spifbank); 
				arradp_fbank.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);  
				spinner_bankr.setAdapter(arradp_bank);
				spinner_bankw.setAdapter(arradp_bank);
				spinner_opbank.setAdapter(arradp_fbank);
				
				spinner_lockbank = (Spinner) findViewById(R.id.spinner_lockbank); 
				 //将可选内容与ArrayAdapter连接起来         	 
				arradp_lockbank = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,spilockbank); 
				 //设置下拉列表的风格       
				arradp_lockbank.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				 //将adapter 添加到spinner中      
				spinner_lockbank.setAdapter(arradp_lockbank);
				
				spinner_locktype = (Spinner) findViewById(R.id.spinner_locktype);   	 
				arradp_locktype = new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,spilocktype); 
				arradp_locktype.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				spinner_locktype.setAdapter(arradp_locktype);
			 
				button_readop = (Button) findViewById(R.id.button_read);
				button_writeop = (Button) findViewById(R.id.button_write);
				button_writepc = (Button) findViewById(R.id.button_wepc);
				button_lockop=(Button)findViewById(R.id.button_lock);
				button_kill=(Button)findViewById(R.id.button_kill);
				gr_opant=(RadioGroup)findViewById(R.id.radioGroup_opant);
				int antChildCount = gr_opant.getChildCount();
				if(antChildCount > antCount)
					gr_opant.removeViews(antCount, antChildCount - antCount);
				
				gr_match=(RadioGroup)findViewById(R.id.radioGroup_opmatch);
				gr_enablefil=(RadioGroup)findViewById(R.id.radioGroup_enableopfil);
				gr_wdatatype=(RadioGroup)findViewById(R.id.radioGroup_datatype);
				cb_pwd=(CheckBox)findViewById(R.id.checkBox_opacepwd);
				 EditText_sub3fildata=(EditText)findViewById(R.id.editText_opfilterdata);
				 EditText_sub3wdata=(EditText)findViewById(R.id.editText_dataw);
				 
				 spinner_opbank.setSelection(0);
				 spinner_bankr.setSelection(1);
				 spinner_bankw.setSelection(1);
				 spinner_lockbank.setSelection(2);
				 spinner_locktype.setSelection(1);
				 gr_opant.check(gr_opant.getChildAt(opant-1).getId());
				
				button_readop.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View arg0) {
						// TODO Auto-generated method stub
						try {
							SetOpant();
							SetPassword();
							SetFiler();
							
							EditText etcount=(EditText)findViewById(R.id.editText_opcountr);
							EditText etadr=(EditText)findViewById(R.id.editText_startaddr);
							EditText etdr=(EditText)findViewById(R.id.editText_datar);
							EditText etdrw=(EditText)findViewById(R.id.editText_startaddrw);
							 
							UHFReader.READER_STATE er=UHFReader.READER_STATE.OK_ERR;
							int trycount=3;
							byte[] rdata = null;
							do{
								int bank = spinner_bankr.getSelectedItemPosition();
								String sAddr = etadr.getText().toString();
								int startAddr = 0;
								if(sAddr != null && TextUtils.isDigitsOnly(sAddr))
									startAddr = Integer.parseInt(sAddr);
								String sblckcnt = etcount.getText().toString();
								int blkcnt  = 0;
								 if(sblckcnt != null && TextUtils.isDigitsOnly(sblckcnt))
									 blkcnt = Integer.parseInt(sblckcnt);
								 
								 
								rdata = mUHFMgr.GetTagData(bank, startAddr, blkcnt, sHexPasswd);
								trycount--;
								if(trycount<1)
									break;
								
							}while(rdata == null);
							
							 if(rdata != null)
							 {
								 String val="";
								 char[] out=null;
								 if(SortGroup(gr_wdatatype)==0)
								 { 
									val=UHFReader.Hex2Str(rdata);
								 } else if(SortGroup(gr_wdatatype)==1) {
									 out=new char[rdata.length];
									 for(int i=0;i<rdata.length;i++)
										 out[i]=(char) rdata[i];
									 val= new String(rdata);
								 } else if(SortGroup(gr_wdatatype)==2)	{
									 val=new String(rdata,"gbk");
								 }
									etdr.setText(val);
									EditText_sub3wdata.setText(val);
								 Toast.makeText(TagReadLockActivity.this, R.string.success,Toast.LENGTH_SHORT).show();
							 } else
								 Toast.makeText(TagReadLockActivity.this, R.string.failed,Toast.LENGTH_SHORT).show();
							
							
						} catch (Exception e) {
							Toast.makeText(TagReadLockActivity.this, "Exception:"+e.getMessage(),Toast.LENGTH_SHORT).show();
						}
						
					}
					
				});
				
				button_writeop.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View arg0) {
						// TODO Auto-generated method stub
						try {
							SetOpant();
							SetPassword();
							SetFiler();
							
							//EditText etcount=(EditText)findViewById(R.id.editText_opcountw);
							byte[] data=null;
							if(SortGroup(gr_wdatatype)==0)
							{
								String sHexData = EditText_sub3wdata.getText().toString();
								data = UHFReader.Str2Hex(sHexData);
							}else if(SortGroup(gr_wdatatype)==1){
								String ascstr=EditText_sub3wdata.getText().toString();
								if(ascstr.length()%2!=0)
									ascstr+="0";
								data=ascstr.getBytes();
								 
							}else if(SortGroup(gr_wdatatype)==2){
								String ascstr=EditText_sub3wdata.getText().toString();
								 data=ascstr.getBytes("gbk");  
							}
							int bank = spinner_bankw.getSelectedItemPosition();
							EditText etadr=(EditText)findViewById(R.id.editText_startaddrw);
							String sAddr = etadr.getText().toString();
							int startAddr = 0;
							if( sAddr != null && TextUtils.isDigitsOnly(sAddr))
								startAddr = Integer.parseInt(sAddr);
							
							UHFReader.READER_STATE er= UHFReader.READER_STATE.OK_ERR;
							int trycount=3;
							do{
								er = mUHFMgr.writeTagData(bank, startAddr, data, sHexPasswd);
								trycount--;
								if(trycount<1)
									break;
							}while(er != UHFReader.READER_STATE.OK_ERR);
							
									 
							 if( er == UHFReader.READER_STATE.OK_ERR)
							 {
								 Toast.makeText(TagReadLockActivity.this, getString(R.string.success)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
							 }
							 else
								 Toast.makeText(TagReadLockActivity.this, getString(R.string.failed)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
							
						}catch (Exception e) {
							Toast.makeText(TagReadLockActivity.this, "Exception :"+e.getMessage(), Toast.LENGTH_SHORT).show();
						}
					}
					
				});
				
				button_writepc.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View arg0) {
						
						try {
							SetOpant();
							SetPassword();
							SetFiler();
							
							//EditText etadr=(EditText)findViewById(R.id.editText_startaddrw);
							 
							byte[] data=null;
							if(SortGroup(gr_wdatatype)==0)
							{
								String sHexData = EditText_sub3wdata.getText().toString();
								data = UHFReader.Str2Hex(sHexData);
							}else if(SortGroup(gr_wdatatype)==1){
								String ascstr=EditText_sub3wdata.getText().toString();
								if(ascstr.length()/2!=0)
									ascstr+="0";
								data=new byte[EditText_sub3wdata.getText().toString().length()];
								
								for(int i=0;i<data.length;i++)
									data[i]=Byte.parseByte(ascstr.substring(i,1),16);
							}else if(SortGroup(gr_wdatatype)==2){
								String ascstr=EditText_sub3wdata.getText().toString();
								 data=ascstr.getBytes("gbk");  
							}
							 
							UHFReader.READER_STATE er=UHFReader.READER_STATE.OK_ERR;
							int trycount=3;
							do{

								er = mUHFMgr.writeTagEpcEx(data, sHexPasswd);
								trycount--;
								if(trycount<1)
									break;
							}while(er!=UHFReader.READER_STATE.OK_ERR);
							
							if( er == UHFReader.READER_STATE.OK_ERR)
							 {
								 Toast.makeText(TagReadLockActivity.this, getString(R.string.success)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
							 }
							 else
								 Toast.makeText(TagReadLockActivity.this, getString(R.string.failed)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
							
							 
						} catch (Exception e) {
							Toast.makeText(TagReadLockActivity.this, "Exception :"+e.getMessage(), Toast.LENGTH_SHORT).show();
						}
					}
					
				});
				
				button_lockop.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View arg0) {
						try {
							SetOpant();
							SetPassword();
							SetFiler();
							
							UHFReader.Lock_Obj lobj = null;
							UHFReader.Lock_Type ltyp=null;
							int lbank=spinner_lockbank.getSelectedItemPosition();
							int ltype=spinner_locktype.getSelectedItemPosition();
							if(lbank==0)
							{
								lobj=UHFReader.Lock_Obj.LOCK_OBJECT_ACCESS_PASSWD;
								if(ltype==0)
									ltyp=UHFReader.Lock_Type.UNLOCK;//解锁定
								else if(ltype==1)
									ltyp=Lock_Type.LOCK; //暂时锁定
								else if(ltype==2)
									ltyp=Lock_Type.PERM_LOCK;//永久锁定
									 
							}
							else if(lbank==1)
							{
								lobj=Lock_Obj.LOCK_OBJECT_KILL_PASSWORD;
								if(ltype==0)
									ltyp=Lock_Type.UNLOCK;
								else if(ltype==1)
									ltyp=Lock_Type.LOCK;
								else if(ltype==2)
									ltyp=Lock_Type.PERM_LOCK;
							}
							else if(lbank==2)
							{
								lobj=Lock_Obj.LOCK_OBJECT_BANK1; //EPC分区
								if(ltype==0)
									ltyp=Lock_Type.UNLOCK;
								else if(ltype==1)
									ltyp=Lock_Type.LOCK;
								else if(ltype==2)
									ltyp=Lock_Type.PERM_LOCK;
							}
							else if(lbank==3)
							{
								lobj=Lock_Obj.LOCK_OBJECT_BANK2;//TID分区
								if(ltype==0)
									ltyp=Lock_Type.UNLOCK;
								else if(ltype==1)
									ltyp=Lock_Type.LOCK;
								else if(ltype==2)
									ltyp=Lock_Type.PERM_LOCK;
							}
							else if(lbank==4)
							{
								lobj=Lock_Obj.LOCK_OBJECT_BANK3;//USER分区
								if(ltype==0)
									ltyp=Lock_Type.UNLOCK;
								else if(ltype==1)
									ltyp=Lock_Type.LOCK;
								else if(ltype==2)
									ltyp=Lock_Type.PERM_LOCK;
							}
							
							UHFReader.READER_STATE er = UHFReader.READER_STATE.OK_ERR;
							er = mUHFMgr.LockTag(lobj.value(), ltyp.value(), sHexPasswd);
							
							if( er == UHFReader.READER_STATE.OK_ERR)
							 {
								 Toast.makeText(TagReadLockActivity.this, getString(R.string.success)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
							 }
							 else
								 Toast.makeText(TagReadLockActivity.this, getString(R.string.failed)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
							
						} catch (Exception e) {
							Toast.makeText(TagReadLockActivity.this, "Exception :"+e.getMessage(), Toast.LENGTH_SHORT).show();
						}
					
					}
					
				});
				
				button_kill.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View arg0) {
						// TODO Auto-generated method stub
						
						try {
							 SetOpant();
							 SetFiler();
							 SetPassword();
							 EditText et=(EditText)findViewById(R.id.editText_password);
							 
							 UHFReader.READER_STATE er = mUHFMgr.destroyTag(sHexPasswd);
							 
							 if( er == UHFReader.READER_STATE.OK_ERR)
							 {
								 Toast.makeText(TagReadLockActivity.this, getString(R.string.success)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
							 }
							 else
								 Toast.makeText(TagReadLockActivity.this, getString(R.string.failed)+" : "+er.toString(), Toast.LENGTH_SHORT).show();
							 
						} catch(Exception e) {
							Toast.makeText(TagReadLockActivity.this, "Exception :"+e.getMessage(), Toast.LENGTH_SHORT).show();
						}
					 
					}
					
				});
				
				tabHost_op.setOnTabChangedListener(new OnTabChangeListener(){

					@Override
					public void onTabChanged(String arg0) {
						// TODO Auto-generated method stub
						int j=tabHost_op.getCurrentTab();
						TabWidget tabIndicator=tabHost_op.getTabWidget();
						View vw=tabIndicator.getChildAt(j);
						vw.setBackgroundColor(Color.BLUE);
						int tc=tabHost_op.getTabContentView().getChildCount();
						for(int i=0;i<tc;i++)
						{
							if(i!=j)
							{
								View vw2=tabIndicator.getChildAt(i);
								vw2.setBackgroundColor(Color.TRANSPARENT);
							}
						}
						 
					}
					
				});
				
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
	
	
	@Override
	protected void onResume() {
		super.onResume();
		updateFilterData();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    /*if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){   
	        if((System.currentTimeMillis()-myapp.exittime) > 2000){  
	            Toast.makeText(getApplicationContext(), "�ٰ�һ���˳�����", Toast.LENGTH_SHORT).show();                                
	            myapp.exittime = System.currentTimeMillis();   
	        } else {
	            finish();
	           // System.exit(0);
	        }
	        return true;   
	    }*/
	    return super.onKeyDown(keyCode, event);
	}
	
	private void updateFilterData()
	{
		EditText et_fdata=(EditText)findViewById(R.id.editText_opfilterdata);
		 EditText etadr=(EditText)findViewById(R.id.editText_opfilsadr);
		 Map<String,Object> settingsMap = mUHFMgr.getAllParams();
		Object obj = settingsMap.get(UHFSilionParams.TAG_FILTER.KEY);
		String sJson = (String)obj;
		if(sJson != null)
		{
			try {
				JSONObject jobj = new JSONObject(sJson);
				int bank = jobj.optInt("bank");
				int startaddr = jobj.optInt("startaddr");
				String sHexData = jobj.optString("fdata");
				int isInvert = jobj.optInt("isInvert");
				et_fdata.setText(sHexData);
				etadr.setText(String.valueOf(startaddr));
				gr_match.check(isInvert == 0?R.id.rb_match:R.id.rb_non_match);
				gr_enablefil.check(R.id.rb_enable);
				spinner_opbank.setSelection(bank > 0 ?(bank - 1):0);
			} catch (Exception e) {
			}
		}else{
			gr_enablefil.check(R.id.rb_not_enable);
		}
	}
	
	private int getIntSettings(String settingKey,int defValue)
	{
		Map<String,Object> settingsMap = mUHFMgr.getAllParams();
		Object obj = settingsMap.get(settingKey);
		
		if(obj != null)
			return (Integer)obj;
		
		return defValue;
	}
	 
	private int[] getIntArraySettings(String settingKey,int[] defValue)
	{
		Map<String,Object> settingsMap = mUHFMgr.getAllParams();
		Object obj = settingsMap.get(settingKey);
		
		if(obj != null)
			return (int[])obj;
		
		return defValue;
	}
}
