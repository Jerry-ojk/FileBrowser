package com.jcraft.jsch;

import jerry.filebrowser.ftp.callback.AuthListener;
import jerry.filebrowser.ftp.callback.SessionListener;

public abstract class UserAuth {
    protected static final int SSH_MSG_USERAUTH_REQUEST = 50;
    protected static final int SSH_MSG_USERAUTH_FAILURE = 51;
    protected static final int SSH_MSG_USERAUTH_SUCCESS = 52;
    protected static final int SSH_MSG_USERAUTH_BANNER = 53;
    protected static final int SSH_MSG_USERAUTH_INFO_REQUEST = 60;
    protected static final int SSH_MSG_USERAUTH_INFO_RESPONSE = 61;

    protected Packet packet;
    protected Buffer buffer;
    protected String username;
    protected AuthListener listener;
    protected UserInfo userInfo;

    public boolean start(Session session) throws Exception {
        this.packet = session.packet;
        this.buffer = packet.getBuffer();
        this.username = session.getUserName();
        SessionListener listener = session.getSessionListener();
        if (listener instanceof AuthListener) {
            this.listener = (AuthListener) listener;
        }
        return true;
    }
}
