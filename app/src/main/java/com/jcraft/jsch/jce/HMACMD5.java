package com.jcraft.jsch.jce;

public class HMACMD5 extends HMAC {
    public HMACMD5() {
        name = "hmac-md5";
        bsize = 16;
        algorithm = "HmacMD5";
    }
}
