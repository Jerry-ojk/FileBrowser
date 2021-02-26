package com.jcraft.jsch.jce;

import com.jcraft.jsch.HASH;

import java.security.MessageDigest;

public class MD5 implements HASH {
    private MessageDigest messageDigest;

    public int getBlockSize() {
        return 16;
    }

    @Override
    public void init() throws Exception {
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(byte[] foo, int start, int len) throws Exception {
        messageDigest.update(foo, start, len);
    }

    @Override
    public byte[] digest() {
        return messageDigest.digest();
    }
}
