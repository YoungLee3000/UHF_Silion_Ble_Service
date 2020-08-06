package com.nlscan.uhf.silionBle.upgrade.ota;

public class SecureDfuError {
    // DFU status values
    // public static final int SUCCESS = 1; // that's not an error
    public static final int OP_CODE_NOT_SUPPORTED = 2;
    public static final int INVALID_PARAM = 3;
    public static final int INSUFFICIENT_RESOURCES = 4;
    public static final int INVALID_OBJECT = 5;
    public static final int UNSUPPORTED_TYPE = 7;
    public static final int OPERATION_NOT_PERMITTED = 8;
    public static final int OPERATION_FAILED = 10; // 0xA
    public static final int EXTENDED_ERROR = 11; // 0xB
}
