package com.jcraft.jsch.jce;

import com.jcraft.jsch.JCipher;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;

public class TripleDESCTR implements JCipher {
    private static final int ivsize = 8;
    private static final int bsize = 24;
    private javax.crypto.Cipher cipher;

    public int getIVSize() {
        return ivsize;
    }

    public int getBlockSize() {
        return bsize;
    }

    public void init(int mode, byte[] key, byte[] iv) throws Exception {
        byte[] tmp;
        if (iv.length > ivsize) {
            tmp = new byte[ivsize];
            System.arraycopy(iv, 0, tmp, 0, tmp.length);
            iv = tmp;
        }
        if (key.length > bsize) {
            tmp = new byte[bsize];
            System.arraycopy(key, 0, tmp, 0, tmp.length);
            key = tmp;
        }

        try {

/*
      // The following code does not work on IBM's JDK 1.4.1
      SecretKeySpec skeySpec = new SecretKeySpec(key, "DESede");
      cipher.init((mode==ENCRYPT_MODE?
		   javax.crypto.Cipher.ENCRYPT_MODE:
		   javax.crypto.Cipher.DECRYPT_MODE),
		  skeySpec, new IvParameterSpec(iv));
*/
            DESedeKeySpec deSedeKeySpec = new DESedeKeySpec(key, 0);
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey secretKey = keyFactory.generateSecret(deSedeKeySpec);
            cipher = Cipher.getInstance("DESede/CTR/NoPadding");
            synchronized (JCipher.class) {
                cipher.init((mode == Cipher.ENCRYPT_MODE ?
                                Cipher.ENCRYPT_MODE :
                                Cipher.DECRYPT_MODE),
                        secretKey, new IvParameterSpec(iv));
            }
        } finally {
            cipher = null;
        }
    }

    public void update(byte[] foo, int s1, int len, byte[] bar, int s2) throws Exception {
        cipher.update(foo, s1, len, bar, s2);
    }

    public boolean isCBC() {
        return false;
    }
}