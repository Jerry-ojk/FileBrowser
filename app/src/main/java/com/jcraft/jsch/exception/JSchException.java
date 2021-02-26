package com.jcraft.jsch.exception;

public class JSchException extends Exception {
    //private static final long serialVersionUID=-1319309923966731989L;
    public static final int CODE_ERROR_NONE = 0;
    public static final int CODE_ERROR_UNKNOWN = 1;

    public static final int CODE_ERROR_MISS_PARAMETER = 2;

    public static final int CODE_ERROR_AUTH = 3;
    public static final int CODE_ERROR_AUTH_PASSWORD_CHANGE = 4;

    public static final int CODE_ERROR_CONNECT = 4;
    public static final int CODE_ERROR_SOCKET = 5;
    public static final int CODE_ERROR_NULL = 6;
    public static final int CODE_ERROR_IO = 7;
    public static final int CODE_ERROR_CONFIG = 8;
    public static final int CODE_ERROR_PROCESS = 9;

    public int code;

    public JSchException(String message) {
        super(message);
        code = CODE_ERROR_NONE;
    }

    public JSchException(String message, Throwable throwable) {
        super(message, throwable);
        code = CODE_ERROR_NONE;
    }

    public JSchException(Throwable throwable) {
        this(JSchException.CODE_ERROR_UNKNOWN, "", throwable);
    }

    public JSchException(int code, String message) {
        super(message);
        this.code = code;
    }

    public JSchException(int code, Throwable throwable) {
        super("", throwable);
        this.code = code;
    }

    public JSchException(int code, String message, Throwable e) {
        super(message, e);
        this.code = code;
    }
}
