// IBleInterface.aidl
package com.nlscan.blecommservice;

// Declare any non-default types here with import statements
import com.nlscan.blecommservice.IBleScanCallback;
import com.nlscan.blecommservice.IBatteryChangeListener;
import com.nlscan.blecommservice.IScanConfigCallback;

interface IBleInterface {
    void setScanCallback(IBleScanCallback callback);
    boolean setScanConfig(IScanConfigCallback callback, String str);
    //battery about
    void addBatteryLevelChangeListener(IBatteryChangeListener callback);
    void removeBatteryLevelChangeListener(IBatteryChangeListener callback);
    //find badge ; set badge voice/ enable/dsiable
    void sendCommand(int cmd);

    //send uhf command
    String sendUhfCommand(String command);

    //get uhf tag data
    String getUhfTagData();

    //clear the uhf tag data
    void clearUhfTagData();

    //is the ble avilable
    boolean isBleAccess();
}
