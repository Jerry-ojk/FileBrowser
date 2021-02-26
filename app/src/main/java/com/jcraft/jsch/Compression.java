package com.jcraft.jsch;

public interface Compression {
    static public final int INFLATER = 0;
    static public final int DEFLATER = 1;

    void init(int type, int level);

    byte[] compress(byte[] buf, int start, int[] len);

    byte[] decompress(byte[] buf, int start, int[] len);
}
