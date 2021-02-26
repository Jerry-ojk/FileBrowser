package com.jcraft.jsch.jce;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.DSAKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;

public class KeyPairGenDSA implements com.jcraft.jsch.KeyPairGenDSA {
    byte[] x;  // private
    byte[] y;  // public
    byte[] p;
    byte[] q;
    byte[] g;

    public void init(int key_size) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA");
        keyGen.initialize(key_size, new SecureRandom());
        KeyPair pair = keyGen.generateKeyPair();
        PublicKey pubKey = pair.getPublic();
        PrivateKey prvKey = pair.getPrivate();

        x = ((DSAPrivateKey) prvKey).getX().toByteArray();
        y = ((DSAPublicKey) pubKey).getY().toByteArray();

        DSAParams params = ((DSAKey) prvKey).getParams();
        p = params.getP().toByteArray();
        q = params.getQ().toByteArray();
        g = params.getG().toByteArray();
    }

    public byte[] getX() {
        return x;
    }

    public byte[] getY() {
        return y;
    }

    public byte[] getP() {
        return p;
    }

    public byte[] getQ() {
        return q;
    }

    public byte[] getG() {
        return g;
    }
}
