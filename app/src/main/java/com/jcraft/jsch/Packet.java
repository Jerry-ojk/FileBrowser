package com.jcraft.jsch;

public class Packet {

    private static Random RANDOM = null;
    Buffer buffer;

    public Packet(Buffer buffer) {
        this.buffer = buffer;
    }

    public void reset() {
        buffer.indexWrite = 5;
    }

    //      uint32    packet_length = 1+n1+n2l
    //      byte      padding_length
    //      byte[n1]  payload; n1 = packet_length - padding_length - 1
    //      byte[n2]  random padding; n2 = padding_length
    //      byte[m]   mac (Message Authentication Code - MAC); m = mac_length

    void padding(int bsize) {
        int len = buffer.indexWrite;
        int pad = (-len) & (bsize - 1);
        if (pad < bsize) {
            pad += bsize;
        }
        len = len + pad - 4;
        // packet_length
        //         The length of the packet in bytes, not including 'mac' or the
        //         'packet_length' field itself.
        buffer.data[0] = (byte) (len >>> 24);
        buffer.data[1] = (byte) (len >>> 16);
        buffer.data[2] = (byte) (len >>> 8);
        buffer.data[3] = (byte) (len);
        buffer.data[4] = (byte) pad;
        synchronized (RANDOM) {
            RANDOM.fill(buffer.data, buffer.indexWrite, pad);
        }
        buffer.skipWrite(pad);
    }

    int shift(int len, int bsize, int mac) {
        int s = len + 5 + 9;
        int pad = (-s) & (bsize - 1);
        if (pad < bsize) pad += bsize;
        s += pad;
        s += mac;
        s += 32; // margin for deflater; deflater may inflate data

        /**/
        if (buffer.data.length < s + buffer.indexWrite - 5 - 9 - len) {
            byte[] foo = new byte[s + buffer.indexWrite - 5 - 9 - len];
            System.arraycopy(buffer.data, 0, foo, 0, buffer.data.length);
            buffer.data = foo;
        }
        /**/

//if(data.data.length<len+5+9)
//  System.err.println("data.data.length="+data.data.length+" len+5+9="+(len+5+9));

//if(data.data.length<s)
//  System.err.println("data.data.length="+data.data.length+" s="+(s));

        System.arraycopy(buffer.data,
                len + 5 + 9,
                buffer.data, s, buffer.indexWrite - 5 - 9 - len);

        buffer.indexWrite = 10;
        buffer.putInt(len);
        buffer.indexWrite = len + 5 + 9;
        return s;
    }

    void unshift(byte command, int recipient, int s, int len) {
        System.arraycopy(buffer.data, s, buffer.data, 5 + 9, len);
        buffer.data[5] = command;
        buffer.indexWrite = 6;
        buffer.putInt(recipient);
        buffer.putInt(len);
        buffer.indexWrite = len + 5 + 9;
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public static void setRANDOM(Random random) {
        Packet.RANDOM = random;
    }
}
