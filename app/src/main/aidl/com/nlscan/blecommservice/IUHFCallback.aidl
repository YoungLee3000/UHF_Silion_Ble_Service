// IUHFCallback.aidl
package com.nlscan.blecommservice;

// Declare any non-default types here with import statements

interface IUHFCallback {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void onReceiveUhf(String data);
}
