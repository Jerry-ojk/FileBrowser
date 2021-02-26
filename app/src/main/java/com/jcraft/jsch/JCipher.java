package com.jcraft.jsch;

public interface JCipher {
//    static int ENCRYPT_MODE = Cipher.ENCRYPT_MODE;
//    static int DECRYPT_MODE = Cipher.DECRYPT_MODE;

    int getIVSize();

    int getBlockSize();

    void init(int mode, byte[] key, byte[] iv) throws Exception;

    void update(byte[] input, int inputOffset, int inputLen, byte[] output, int outputOffset) throws Exception;

    boolean isCBC();
}
