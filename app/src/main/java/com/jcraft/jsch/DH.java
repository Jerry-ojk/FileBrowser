package com.jcraft.jsch;

public interface DH {
    void init() throws Exception;

    void setP(byte[] p);

    void setG(byte[] g);

    byte[] getE() throws Exception;

    void setF(byte[] f);

    byte[] getK() throws Exception;

    // checkRange() will check if e and f are inputStream [1,p-1]
    // as defined at https://tools.ietf.org/html/rfc4253#section-8
    void checkRange() throws Exception;
}