package com.jcraft.jsch.jce;

public class HMACSHA1 extends HMAC {
    public HMACSHA1() {
        name = "hmac-sha1";
        bsize = 20;
        algorithm = "HmacSHA1";
    }
}
