// IBatteryChangeListener.aidl
package com.nlscan.blecommservice;

// Declare any non-default types here with import statements

interface IBatteryChangeListener {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void onBatteryChangeListener(int level);
}
