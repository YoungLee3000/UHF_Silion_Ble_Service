package com.nlscan.uhf.silionBle.upgrade.ota;

import android.os.SystemClock;

public class DfuProgressInfo {
    private int progress;
    private int bytesSent;
    private int initialBytesSent;
    private int lastBytesSent;
    private int bytesReceived;
    private int imageSizeInBytes;
    private int maxObjectSizeInBytes;
    private int currentPart;
    private int totalParts;
    private long timeStart, lastProgressTime;

    DfuProgressInfo() {

    }

    DfuProgressInfo init(final int imageSizeInBytes, final int currentPart, final int totalParts) {
        this.imageSizeInBytes = imageSizeInBytes;
        this.maxObjectSizeInBytes = Integer.MAX_VALUE; // by default the whole firmware will be sent as a single object
        this.currentPart = currentPart;
        this.totalParts = totalParts;
        return this;
    }

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    DfuProgressInfo setTotalPart(final int totalParts) {
        this.totalParts = totalParts;
        return this;
    }

    void setProgress(final int progress) {
        this.progress = progress;
    }

    void setBytesSent(final int bytesSent) {
        if (timeStart == 0) {
            timeStart = SystemClock.elapsedRealtime();
            initialBytesSent = bytesSent;
        }
        this.bytesSent = bytesSent;
        this.progress = (int) (100.0f * bytesSent / imageSizeInBytes);
    }

    void addBytesSent(final int increment) {
        setBytesSent(bytesSent + increment);
    }

    void setBytesReceived(final int bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    void setMaxObjectSizeInBytes(final int bytes) {
        this.maxObjectSizeInBytes = bytes;
    }

    boolean isComplete() {
        return bytesSent == imageSizeInBytes;
    }

    boolean isObjectComplete() {
        return (bytesSent % maxObjectSizeInBytes) == 0;
    }

    int getAvailableObjectSizeIsBytes() {
        final int remainingBytes = imageSizeInBytes - bytesSent;
        final int remainingChunk = maxObjectSizeInBytes - (bytesSent % maxObjectSizeInBytes);
        return Math.min(remainingBytes, remainingChunk);
    }

    int getProgress() {
        return progress;
    }

    int getBytesSent() {
        return bytesSent;
    }

    @SuppressWarnings("unused")
    int getBytesReceived() {
        return bytesReceived;
    }

    @SuppressWarnings("unused")
    int getImageSizeInBytes() {
        return imageSizeInBytes;
    }

    float getSpeed() {
        final long now = SystemClock.elapsedRealtime();
        final float speed = now - timeStart != 0 ? (float) (bytesSent - lastBytesSent) / (float) (now - lastProgressTime) : 0.0f;
        lastProgressTime = now;
        lastBytesSent = bytesSent;
        return speed;
    }

    float getAverageSpeed() {
        final long now = SystemClock.elapsedRealtime();
        return now - timeStart != 0 ? (float) (bytesSent - initialBytesSent) / (float) (now - timeStart) : 0.0f;
    }

    int getCurrentPart() {
        return currentPart;
    }

    int getTotalParts() {
        return totalParts;
    }

    boolean isLastPart() {
        return currentPart == totalParts;
    }
}
