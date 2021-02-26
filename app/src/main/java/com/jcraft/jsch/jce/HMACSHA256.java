package com.jcraft.jsch.jce;

public class HMACSHA256 extends HMAC {
    public HMACSHA256() {
        name = "hmac-sha2-256";
        bsize = 32;
        algorithm = "HmacSHA256";
    }
}
