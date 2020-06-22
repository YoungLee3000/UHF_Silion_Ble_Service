package com.nlscan.uhf.silionBle;

public class CrcModel {



    public native String  getCrcStr(byte[] buf);

    static {
        System.loadLibrary("crcGet");
    }
}
