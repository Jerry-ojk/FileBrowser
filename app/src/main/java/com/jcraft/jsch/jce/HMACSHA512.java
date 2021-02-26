package com.jcraft.jsch.jce;

public class HMACSHA512 extends HMAC {
    public HMACSHA512() {
        name = "hmac-sha2-512";
        bsize = 64;
        algorithm = "HmacSHA512";
    }
}
