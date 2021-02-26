package com.jcraft.jsch;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IO {
    InputStream inputStream;
    OutputStream outputStream;
    OutputStream out_ext;

    private boolean in_doNotClose = false;
    private boolean out_doNotClose = false;
    private boolean out_ext_doNotClose = false;

    void setOutputStream(OutputStream out) {
        this.outputStream = out;
    }

    void setOutputStream(OutputStream out, boolean doNotClose) {
        this.out_doNotClose = doNotClose;
        setOutputStream(out);
    }

    void setExtOutputStream(OutputStream out) {
        this.out_ext = out;
    }

    void setExtOutputStream(OutputStream out, boolean doNotClose) {
        this.out_ext_doNotClose = doNotClose;
        setExtOutputStream(out);
    }

    void setInputStream(InputStream in) {
        this.inputStream = in;
    }

    void setInputStream(InputStream in, boolean doNotClose) {
        this.in_doNotClose = doNotClose;
        setInputStream(in);
    }

    public void send(Packet packet) throws IOException {
        outputStream.write(packet.buffer.data, 0, packet.buffer.indexWrite);
        outputStream.flush();
    }

    void send(byte[] array, int begin, int length) throws IOException {
        outputStream.write(array, begin, length);
        outputStream.flush();
    }

    void sendExt(byte[] array, int begin, int length) throws IOException {
        out_ext.write(array, begin, length);
        out_ext.flush();
    }

    int getByte() throws IOException {
        return inputStream.read();
    }

    void getByte(byte[] array) throws IOException {
        getByte(array, 0, array.length);
    }

    void getByte(byte[] array, int begin, int length) throws IOException {
        do {
            int completed = inputStream.read(array, begin, length);
            if (completed < 0) {
                throw new IOException("End of IO Stream Read");
            }
            begin += completed;
            length -= completed;
        }
        while (length > 0);
    }

    void closeOut() {
        try {
            if (outputStream != null && !out_doNotClose) outputStream.close();
            outputStream = null;
        } catch (Exception ignored) {
        }
    }

    public void close() {
        try {
            if (inputStream != null && !in_doNotClose) inputStream.close();
            inputStream = null;
            if (outputStream != null && !out_doNotClose) outputStream.close();
            outputStream = null;
            if (out_ext != null && !out_ext_doNotClose) out_ext.close();
            out_ext = null;
        } catch (IOException ignored) {
        }
    }
}
