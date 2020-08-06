/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
 */

package com.nlscan.uhf.silionBle.upgrade.driver;

import android.os.RemoteException;
import android.util.Log;

import com.nlscan.blecommservice.IBleInterface;
import com.nlscan.blecommservice.IScanConfigCallback;
import com.nlscan.uhf.silionBle.upgrade.util.HexDump;

import java.io.IOException;

/**
 *
 */
public class BleSerialDriver implements BleSerialPort {

    private final String TAG = BleSerialDriver.class.getSimpleName();

    public static final int DEFAULT_WRITE_BUFFER_SIZE = 235;

    private static final String UPDATE_HEAD = "5DCB00005DCB";

    protected IBleInterface mInterface = null;

    /** Internal write buffer.  Guarded by {@link #mWriteBufferLock}. */
    protected byte[] mWriteBuffer;

    public BleSerialDriver(IBleInterface iBleInterface) {
        mInterface = iBleInterface;
        mWriteBuffer = new byte[DEFAULT_WRITE_BUFFER_SIZE];
    }

    protected final Object mWriteBufferLock = new Object();


    @Override
    public void addReadCallback(IScanConfigCallback callback){
        try {
            if (mInterface != null)
                mInterface.setScanConfig(callback,UPDATE_HEAD);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int read(byte[] dest, int timeoutMillis) throws IOException {
        return 0;
    }

    @Override
    public int write(byte[] src, int timeoutMillis, IScanConfigCallback callback) throws IOException {
        Log.i("BleSerialDriver","srcLen: "+src.length+" write: "+ HexDump.toHexString(src));
        int offset = 0;
        int count = 30;
        while (offset < src.length && count >= 0) {
            int writeLength;
            int amtWritten = 0;
            synchronized (mWriteBufferLock) {
                final byte[] writeBuffer;

                writeLength = Math.min(src.length - offset, mWriteBuffer.length);
                if (offset == 0 && src.length <= mWriteBuffer.length) {
                    writeBuffer = src;
                } else {
                    // bulkTransfer does not support offsets, make a copy.
                    System.arraycopy(src, offset, mWriteBuffer, 0, writeLength);
                    writeBuffer = mWriteBuffer;
                }

                try {
                    amtWritten = mInterface.setScanConfig(((src.length - offset) <= mWriteBuffer.length) ? callback: null,
                            UPDATE_HEAD+ HexDump.toHexString(writeBuffer,0,writeLength)) ? writeLength : 0;
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
            if (amtWritten < 0) {
                throw new IOException("Error writing " + writeLength
                        + " bytes at offset " + offset + " length=" + src.length);
            }

            Log.d(TAG, "Wrote amt=" + amtWritten + " attempted=" + writeLength+" count: "+count);
            offset += amtWritten;
            if (amtWritten == 0)//only write fail to count--
                count--;
        }
        return offset;
    }

    @Override
    public void close() throws IOException {
        mInterface = null;
    }
}
