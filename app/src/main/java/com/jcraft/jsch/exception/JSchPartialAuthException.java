package com.jcraft.jsch.exception;

public class JSchPartialAuthException extends JSchException {
    //private static final long serialVersionUID=-378849862323360367L;
    private String otherServerAuthMethods;

    public JSchPartialAuthException(String otherServerAuthMethods) {
        super(otherServerAuthMethods);
        this.otherServerAuthMethods = otherServerAuthMethods;
    }

    public String getOtherServerAuthMethods() {
        return otherServerAuthMethods;
    }
}
