package com.jcraft.jsch;

public interface ProgressCallback {
    public static final int ACTION_PUT = 0;
    public static final int ACTION_GET = 1;
    public static final long UNKNOWN_SIZE = -1L;

    void onStart(int operation, String src, String dest, long totalSize);

    boolean onSizeIncrease(long size);

    void onFinish();
}