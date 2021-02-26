package com.jcraft.jsch.jce;

import com.jcraft.jsch.Random;

import java.security.SecureRandom;

public class RandomImpl implements Random {
    private byte[] tmp = new byte[16];
    private SecureRandom random;

    public RandomImpl() {

        // We hope that 'new SecureRandom()' will use NativePRNG algorithm
        // on Sun's Java5 for GNU/Linux and Solaris.
        // It seems NativePRNG refers to /dev/urandom and it must not be blocked,
        // but NativePRNG is slower than SHA1PRNG ;-<
        // TIPS: By adding option '-Djava.security.egd=file:/dev/./urandom'
        //       SHA1PRNG will be used instead of NativePRNG.
        // On MacOSX, 'new SecureRandom()' will use NativePRNG algorithm and
        // it is also slower than SHA1PRNG.
        // On Windows, 'new SecureRandom()' will use SHA1PRNG algorithm.
        random = new SecureRandom();
//        System.out.println(random.getAlgorithm());
//        System.out.println(random.getProvider().toString());

    /*
    try{ 
      random=SecureRandom.getInstance("SHA1PRNG"); 
      return;
    }
    catch(java.security.NoSuchAlgorithmException e){ 
      // System.err.println(e); 
    }

    // The following code is for IBM's JCE
    try{ 
      random=SecureRandom.getInstance("IBMSecureRandom"); 
      return;
    }
    catch(java.security.NoSuchAlgorithmException ee){ 
      //System.err.println(ee); 
    }
    */
    }

    public void fill(byte[] foo, int start, int len) {
    /*
    // This case will not become true in our usage.
    if(start==0 && foo.length==len){
      random.nextBytes(foo);
      return;
    }
    */
        if (len > tmp.length) {
            tmp = new byte[len];
        }
        random.nextBytes(tmp);
        System.arraycopy(tmp, 0, foo, start, len);
    }
}
