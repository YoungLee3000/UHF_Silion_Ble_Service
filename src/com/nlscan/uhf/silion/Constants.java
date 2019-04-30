package com.nlscan.uhf.silion;


public class Constants {

	public final static String TAG_PREFIX = "UHF_Silion_Service/";
	
	/** 上电串口路径 */
	//public final static String UHF_POWER_DEVICE_PATH = "/sys/bus/platform/devices/sys_switch.0/power1";
	
	/** 读写器设备路径 */
	//public final static String UHF_READER_DEVICE_PATH = "/dev/ttyMT0";
	
	/**读码结果发送的广播action*/
	public final static String ACTION_UHF_RESULT_SEND = "nlscan.intent.action.uhf.ACTION_RESULT";
	
	/**读码结果发送的广播Extra*/
	public final static String EXTRA_TAG_INFO = "tag_info";
	
	//==============================
}
