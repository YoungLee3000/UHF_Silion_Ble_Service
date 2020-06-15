// IScanCallback.aidl
package com.nlscan.blecommservice;

// Declare any non-default types here with import statements

interface IBleScanCallback {
    void onReceiveResult(String result,int codeType, String rawHexString);
}
