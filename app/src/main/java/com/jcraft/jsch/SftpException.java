package com.jcraft.jsch;

public class SftpException extends Exception {
    //private static final long serialVersionUID=-5616888495583253811L;
    public int code;

    public SftpException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SftpException(int code, String message, Throwable throwable) {
        super(message, throwable);
        this.code = code;
    }

    public String toString() {
        return code + ": " + getMessage();
    }
}
