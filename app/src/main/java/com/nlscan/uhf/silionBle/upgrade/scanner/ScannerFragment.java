
package com.nlscan.uhf.silionBle.upgrade.scanner;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.UUID;

/**
 * ScannerFragment class scan required BLE devices and shows them in a list. This class scans and filter
 * devices with standard BLE Service UUID and devices with custom BLE Service UUID. It contains a
 * list and a button to scan/cancel. There is a interface {@link OnDeviceSelectedListener} which is
 * implemented by activity in order to receive selected device. The scanning will continue to scan
 * for 5 seconds and then stop.
 */
public class ScannerFragment {
	private final static String TAG = "ScannerFragment";


	private final static long SCAN_DURATION = 10000;

	private final static int REQUEST_PERMISSION_REQ_CODE = 34; // any 8-bit number

	private OnDeviceSelectedListener mListener;

	private final Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			startScan();
		}
	};
	//private Button mScanButton;

	//private View mPermissionRationale;
	private ParcelUuid mUuid;

	private boolean mIsScanning = false;
	private String mDfuAddress;

	public ScannerFragment(UUID uuid, String address) {
		this.mUuid = new ParcelUuid(uuid);
		if (address != null && address.length() == 17) {
			try {
				String preAddress = address.substring(0, 15);
				String lastAddress = address.substring(15, 17);

				int value = Integer.parseInt(lastAddress, 16) + 1;
				if (value > 255) value = 0;
				mDfuAddress = preAddress + String.format("%02x",value).toUpperCase();
			}catch (Exception e){}
		}
	}



	/**
	 * Interface required to be implemented by activity.
	 */
	public interface OnDeviceSelectedListener {
		/**
		 * Fired when user selected the device.
		 * 
		 * @param device
		 *            the device to connect to
		 * @param name
		 *            the device name. Unfortunately on some devices {@link BluetoothDevice#getName()}
		 *            always returns <code>null</code>, i.e. Sony Xperia Z1 (C6903) with Android 4.3.
		 *            The name has to be parsed manually form the Advertisement packet.
		 */
		void onDeviceSelected(final BluetoothDevice device, final String name);

		/**
		 * Fired when scanner dialog has been cancelled without selecting a device.
		 */
		void onDialogCanceled();
	}

	/**
	 * This will make sure that {@link OnDeviceSelectedListener} interface is implemented by activity.
	 */
	public void onAttach(final Context context) {
		try {
			this.mListener = (OnDeviceSelectedListener) context;
		} catch (final ClassCastException e) {
			throw new ClassCastException(context.toString() + " must implement OnDeviceSelectedListener");
		}
		mHandler.sendEmptyMessageDelayed(0, 1000);
	}



	public void onDestroyView() {
		stopScan();
	}



	/**
	 * Scan for 5 seconds and then stop scanning when a BluetoothLE device is found then mLEScanCallback
	 * is activated This will perform regular scan for custom BLE Service UUID and then filter out.
	 * using class ScannerServiceParser
	 */
	private void startScan() {
		Log.i(TAG, "startScan");
		// Hide the rationale message, we don't need it anymore.
		//if (mPermissionRationale != null)
		//	mPermissionRationale.setVisibility(View.GONE);

		//mAdapter.clearDevices();
		//mScanButton.setText(R.string.scanner_action_cancel);

		BluetoothAdapter.getDefaultAdapter().startLeScan(leScanCallback);

		/*final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
		final ScanSettings settings = new ScanSettings.Builder()
				.setLegacy(false)
				.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(1000).setUseHardwareBatchingIfSupported(false).build();
		final List<ScanFilter> filters = new ArrayList<>();
		filters.add(new ScanFilter.Builder().setServiceUuid(mUuid).build());
		scanner.startScan(filters, settings, scanCallback);*/

		mIsScanning = true;
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				if (mIsScanning) {
					stopScan();
				}
			}
		}, SCAN_DURATION);
	}
	/**
	 * Stop scan if user tap Cancel button
	 */
	private void stopScan() {
		Log.i(TAG, "stopScan "+mIsScanning);
		if (mIsScanning) {
			//mScanButton.setText(R.string.scanner_action_scan);

			//final BluetoothLeScannerCompat scanner = BluetoothLeScannerCompat.getScanner();
			//scanner.stopScan(scanCallback);

			BluetoothAdapter.getDefaultAdapter().stopLeScan(leScanCallback);
			mIsScanning = false;
		}
	}

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {

            String name = device != null ? device.getName() : null;
            if ("DfuTarg".equals(name)) { // D1:1D:60:C1:E7:0C  D1:1D:60:C1:E7:0B
                Log.i(TAG, "onLeScan "+device.getAddress()+" "+mDfuAddress);
                if (mDfuAddress != null && device.getAddress().equals(mDfuAddress)){
					stopScan();
					mListener.onDeviceSelected(device, name);
					return;
                }
            }
        }
    };

	/*private ScanCallback scanCallback = new ScanCallback() {
		@Override
		public void onScanResult(final int callbackType, final ScanResult result) {
			// do nothing
			Log.i(TAG, "onScanResult "+result.getDevice().getName());
		}

		@Override
		public void onBatchScanResults(final List<ScanResult> results) {
			//mAdapter.update(results);
			for (final ScanResult result : results) {
                Log.i(TAG, "onBatchScanResults "+result.getDevice().getName());
				String name = result.getScanRecord() != null ? result.getScanRecord().getDeviceName() : result.getDevice().getName();
				if ("DfuTarg".equals(name)) {
					stopScan();
					mListener.onDeviceSelected(result.getDevice(), name);
					return;
				}
			}

		}

		@Override
		public void onScanFailed(final int errorCode) {
			// should never be called
		}
	};*/

}
