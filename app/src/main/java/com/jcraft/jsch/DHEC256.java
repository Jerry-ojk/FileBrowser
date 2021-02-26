package com.jcraft.jsch;

public class DHEC256 extends DHECN {
    public DHEC256() {
        sha_name = "sha-256";
        key_size = 256;
    }
}