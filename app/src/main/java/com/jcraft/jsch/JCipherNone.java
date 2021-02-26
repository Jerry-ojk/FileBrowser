package com.jcraft.jsch;

public class JCipherNone implements JCipher {
    private static final int ivsize = 8;
    private static final int bsize = 16;

    @Override
    public int getIVSize() {
        return ivsize;
    }

    @Override
    public int getBlockSize() {
        return bsize;
    }

    @Override
    public void init(int mode, byte[] key, byte[] iv) throws Exception {
    }

    @Override
    public void update(byte[] foo, int s1, int len, byte[] bar, int s2) throws Exception {
    }

    @Override
    public boolean isCBC() {
        return false;
    }
}
