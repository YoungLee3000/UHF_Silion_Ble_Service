package com.nlscan.uhf.silionBle.upgrade;


public class Native
{



  public native String  getCrcStr(byte[] buf);
  
  static
  {
    System.loadLibrary("nlsCrc");
  }
}