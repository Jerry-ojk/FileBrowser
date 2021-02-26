package com.jcraft.jsch.jce;

public class HMACSHA196 extends HMACSHA1 {

    public HMACSHA196() {
        name = "hmac-sha1-96";
    }

    @Override
    public int getBlockSize() {
        return 12;
    }


    private final byte[] _buf20 = new byte[20];

    @Override
    public void doFinal(byte[] buf, int offset) {
        super.doFinal(_buf20, 0);
        System.arraycopy(_buf20, 0, buf, offset, 12);
    }
}
