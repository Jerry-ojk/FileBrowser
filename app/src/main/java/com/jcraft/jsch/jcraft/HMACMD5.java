package com.jcraft.jsch.jcraft;

import com.jcraft.jsch.MAC;

import java.security.MessageDigest;

public class HMACMD5 extends HMAC implements MAC {
    private static final String name = "hmac-md5";

    public HMACMD5() {
        super();
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (Exception e) {
            e.printStackTrace();
        }
        setH(md);
    }

    public String getName() {
        return name;
    }
}
