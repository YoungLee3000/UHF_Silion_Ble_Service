package com.nlscan.uhf.silion;

import android.util.Log;

public class DLog {

	private static boolean debug = true;
	
	public static void d(String tag, String msg)
	{
		if(debug)
			Log.d(tag, msg);
	}
	
	public static void i(String tag, String msg)
	{
		if(debug)
			Log.i(tag, msg);
	}
	
	public static void w(String tag, String msg)
	{
		if(debug)
			Log.w(tag, msg);
	}
	
	public static void w(String tag, String msg, Throwable e)
	{
		if(debug)
			Log.w(tag, msg,e);
	}
	
	public static void e(String tag, String msg)
	{
		if(debug)
			Log.e(tag, msg);
	}
	
	public static void e(String tag, String msg, Throwable e)
	{
		if(debug)
			Log.e(tag, msg,e);
	}
}
