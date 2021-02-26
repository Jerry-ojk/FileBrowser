package com.jcraft.jsch.jce;

import com.jcraft.jsch.HASH;

import java.security.MessageDigest;

public class SHA1 implements HASH {
    MessageDigest messageDigest;

    public int getBlockSize() {
        return 20;
    }

    @Override
    public void init() throws Exception {
        try {
            messageDigest = MessageDigest.getInstance("SHA-1");
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
    public byte[] digest() {
        return messageDigest.digest();
    }
}