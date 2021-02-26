package com.jcraft.jsch.exception;

public class JSchAuthCancelException extends JSchException {
    //private static final long serialVersionUID=3204965907117900987L;
    private String method;

    public JSchAuthCancelException(String message) {
        super(JSchException.CODE_ERROR_AUTH, message);
        this.method = message;
    }

    public String getMethod() {
        return method;
    }
}