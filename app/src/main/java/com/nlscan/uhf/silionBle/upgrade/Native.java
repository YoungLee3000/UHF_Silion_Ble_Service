package com.nlscan.uhf.silionBle.upgrade;


public class Native
{


  public static final int UPGRADE_TYPE_NONE = 0;
  public static final int UPGRADE_TYPE_BOOT = 1;
  public static final int UPGRADE_TYPE_KERNEL = 2;
  public static final int UPGRADE_TYPE_FLASH = 4;
  public static final int UPGRADE_TYPE_APPL = 8;

  public native void SetFrameSize(int size);
  public native int ImportBinFile(String strFileName);
  public native String GetDevNameFromImportFile();
  public native String GetVersionFromImportFile();
  public native int GetParam(int nUpgradeType, Integer pImportFileStatus, Integer pFrameSize, Integer pCmdPackCnt, Integer pDataPackCnt);
  public native byte[] GetPackCmd(int nUpgradeType,int nPackIdx);
  public native byte[] GetPackData(int nUpgradeType,int nPackIdx);
  public native String  getCrcStr(byte[] buf);
  
  static
  {
    System.loadLibrary("nlsEM2037upgrade");
  }
}