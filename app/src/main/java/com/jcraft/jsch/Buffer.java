package com.jcraft.jsch;

import com.jcraft.jsch.exception.JSchException;

import java.nio.charset.StandardCharsets;

public class Buffer {
    byte[] data;
    int indexWrite;
    int indexRead;

    public Buffer(int size) {
        data = new byte[size];
        indexWrite = 0;
        indexRead = 0;
    }

    public Buffer(byte[] data) {
        this.data = data;
        indexWrite = 0;
        indexRead = 0;
    }

    public Buffer() {
        this(1024 * 10 * 2);
    }

    public void putByte(byte b) {
        data[indexWrite++] = b;
    }

    public void putByte(byte[] bytes) {
        putByte(bytes, 0, bytes.length);
    }

    public void putByte(byte[] bytes, int begin, int length) {
        System.arraycopy(bytes, begin, data, indexWrite, length);
        indexWrite += length;
    }

    public void putString(String value) {
        if (value == null) return;
        putString(value.getBytes(StandardCharsets.UTF_8));
    }

    public void putString(byte[] bytes) {
        putString(bytes, 0, bytes.length);
    }

    public void putString(byte[] bytes, int begin, int length) {
        putInt(length);
        putByte(bytes, begin, length);
    }

    public void putInt(int value) {
        data[indexWrite++] = (byte) (value >>> 24);
        data[indexWrite++] = (byte) (value >>> 16);
        data[indexWrite++] = (byte) (value >>> 8);
        data[indexWrite++] = (byte) (value);
    }

    public void putLong(long value) {
        data[indexWrite++] = (byte) (value >>> 56);
        data[indexWrite++] = (byte) (value >>> 48);
        data[indexWrite++] = (byte) (value >>> 40);
        data[indexWrite++] = (byte) (value >>> 32);
        data[indexWrite++] = (byte) (value >>> 24);
        data[indexWrite++] = (byte) (value >>> 16);
        data[indexWrite++] = (byte) (value >>> 8);
        data[indexWrite++] = (byte) (value);
    }

    public void skipWrite(int n) {
        indexWrite += n;
    }

    public void putPad(int n) {
        while (n > 0) {
            data[indexWrite++] = (byte) 0;
            n--;
        }
    }

    public void putMPInt(byte[] foo) {
        int i = foo.length;
        if ((foo[0] & 0x80) != 0) {
            i++;
            putInt(i);
            putByte((byte) 0);
        } else {
            putInt(i);
        }
        putByte(foo);
    }

    void skipRead(int n) {
        indexRead += n;
    }

    public void setReadOffSet(int offset) {
        this.indexRead = offset;
    }

    public int getReadOffSet() {
        return indexRead;
    }

    public int getLength() {
        return indexWrite - indexRead;
    }

    public int readByte() {
        return (data[indexRead++] & 0xff);
    }

    public int readShort() {
        final int high = data[indexRead++] & 0xff;
        final int low = data[indexRead++] & 0xff;
        return (high << 8) | low;
    }

    public int readInt() {
        final int a = data[indexRead++] & 0xff;
        final int b = data[indexRead++] & 0xff;
        final int c = data[indexRead++] & 0xff;
        final int d = data[indexRead++] & 0xff;
        return (a << 24) | (b << 16) | (c << 8) | d;
    }

    public long readLong() {
        final long high = readInt() & 0xffffffffL;
        final long low = readInt() & 0xffffffffL;
        return (high << 32) | (low);
    }

    public long readUInt() {
        final long a = data[indexRead++] & 0xff;
        final long b = data[indexRead++] & 0xff;
        final long c = data[indexRead++] & 0xff;
        final long d = data[indexRead++] & 0xff;
        return (a << 24) | (b << 16) | (c << 8) | d;
    }


    public void readByte(byte[] bytes) {
        readByte(bytes, 0, bytes.length);
    }

    public void readByte(byte[] bytes, int start, int len) {
        System.arraycopy(data, indexRead, bytes, start, len);
        indexRead += len;
    }


    public byte[] getMPInt() {
        int i = readInt();  // uint32
        if (i < 0 ||  // bigger than 0x7fffffff
                i > 8 * 1024) {
            // TODO: an exception should be thrown.
            i = 8 * 1024; // the session will be broken, but working around OOME.
        }
        byte[] foo = new byte[i];
        readByte(foo, 0, i);
        return foo;
    }

    public byte[] getMPIntBits() {
        int bits = readInt();
        int bytes = (bits + 7) / 8;
        byte[] foo = new byte[bytes];
        readByte(foo, 0, bytes);
        if ((foo[0] & 0x80) != 0) {
            byte[] bar = new byte[foo.length + 1];
            bar[0] = 0; // ??
            System.arraycopy(foo, 0, bar, 1, foo.length);
            foo = bar;
        }
        return foo;
    }

    public byte[] getStringByte() {
        int len = readInt();  // uint32
        if (len < 0 || len > 256 * 1024) {
            // TODO: an exception should be thrown.
            len = 256 * 1024; // the session will be broken, but working around OOME.
        }
        byte[] bytes = new byte[len];
        readByte(bytes, 0, len);
        return bytes;
    }

    public void skipStringByte() {
        int len = readInt();  // uint32
        if (len < 0 || len > 256 * 1024) { // bigger than 0x7fffffff
            // TODO: an exception should be thrown.
            len = 256 * 1024; // the session will be broken, but working around OOME.
        }
        skipRead(len);
    }


    public byte[] getStringByte(int[] start, int[] len) {
        int i = readInt();
        start[0] = indexRead;
        indexRead += i;
        len[0] = i;
        return data;
    }

    public void reset() {
        indexWrite = 0;
        indexRead = 0;
    }

    public void shift() {
        if (indexRead == 0) return;
        System.arraycopy(data, indexRead, data, 0, indexWrite - indexRead);
        indexWrite = indexWrite - indexRead;
        indexRead = 0;
    }

    public void rewind() {
        indexRead = 0;
    }

    public byte getCommand() {
        return data[5];
    }

    public void checkFreeSize(int n) {
        int size = indexWrite + n + Session.buffer_margin;
        if (data.length < size) {
            int i = data.length * 2;
            if (i < size) i = size;
            byte[] tmp = new byte[i];
            System.arraycopy(data, 0, tmp, 0, indexWrite);
            data = tmp;
        }
    }

    public byte[][] getBytes(int n, String msg) throws JSchException {
        byte[][] tmp = new byte[n][];
        for (int i = 0; i < n; i++) {
            int j = readInt();
            if (getLength() < j) {
                throw new JSchException(msg);
            }
            tmp[i] = new byte[j];
            readByte(tmp[i]);
        }
        return tmp;
    }

  /*
  static Buffer fromBytes(byte[]... args){
    int length = args.length*4;
    for(int i = 0; i < args.length; i++){
      length += args[i].length;
    }
    Buffer buf = new Buffer(length);
    for(int i = 0; i < args.length; i++){
      buf.putString(args[i]);
    }
    return buf;
  }
  */

    static Buffer fromBytes(byte[][] args) {
        int length = args.length * 4;
        for (int i = 0; i < args.length; i++) {
            length += args[i].length;
        }
        Buffer buf = new Buffer(length);
        for (int i = 0; i < args.length; i++) {
            buf.putString(args[i]);
        }
        return buf;
    }


/*
  static String[] chars={
    "0","1","2","3","4","5","6","7","8","9", "a","b","c","d","e","f"
  };
  static void dump_buffer(){
    int foo;
    for(int i=0; i<tmp_buffer_index; i++){
        foo=tmp_buffer[i]&0xff;
	System.err.print(chars[(foo>>>4)&0xf]);
	System.err.print(chars[foo&0xf]);
        if(i%16==15){
          System.err.println("");
	  continue;
	}
        if(i>0 && i%2==1){
          System.err.print(" ");
	}
    }
    System.err.println("");
  }
  static void dump(byte[] b){
    dump(b, 0, b.length);
  }
  static void dump(byte[] b, int s, int l){
    for(int i=s; i<s+l; i++){
      System.err.print(Integer.toHexString(b[i]&0xff)+":");
    }
    System.err.println("");
  }
*/

}
