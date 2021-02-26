package com.jcraft.jsch.jce;

import java.security.MessageDigest;

public class SHA512 implements com.jcraft.jsch.HASH {
    private MessageDigest messageDigest;

    @Override
    public int getBlockSize() {
        return 64;
    }

    @Override
    public void init() throws Exception {
        try {
            messageDigest = MessageDigest.getInstance("SHA-512");
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public void update(byte[] foo, int start, int len) throws Exception {
        messageDigest.update(foo, start, len);
    }

    @Override
    public byte[] digest() throws Exception {
        return messageDigest.digest();
    }
}
