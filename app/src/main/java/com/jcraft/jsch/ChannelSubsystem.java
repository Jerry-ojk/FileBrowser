package com.jcraft.jsch;

import com.jcraft.jsch.exception.JSchException;
import com.jcraft.jsch.request.Request;
import com.jcraft.jsch.request.RequestPtyReq;
import com.jcraft.jsch.request.RequestSubsystem;
import com.jcraft.jsch.request.RequestX11;

public class ChannelSubsystem extends ChannelSession {
    boolean xForwading = false;
    boolean pty = false;
    boolean want_reply = true;
    String subsystem = "";

    public void setXForwarding(boolean foo) {
        xForwading = foo;
    }

    public void setPty(boolean foo) {
        pty = foo;
    }

    public void setWantReply(boolean foo) {
        want_reply = foo;
    }

    public void setSubsystem(String foo) {
        subsystem = foo;
    }

    @Override
    public void start() throws JSchException {
        Session _session = getSession();
        try {
            Request request;
            if (xForwading) {
                request = new RequestX11();
                request.request(_session, this);
            }
            if (pty) {
                request = new RequestPtyReq();
                request.request(_session, this);
            }
            request = new RequestSubsystem();
            ((RequestSubsystem) request).request(_session, this, subsystem, want_reply);
        } catch (Exception e) {
            if (e instanceof JSchException) {
                throw (JSchException) e;
            }
            throw new JSchException("ChannelSubsystem", e);
        }
        if (io_local.inputStream != null) {
            thread = new Thread(this);
            thread.setName("Subsystem for " + _session.getHost());
            if (_session.daemon_thread) {
                thread.setDaemon(_session.daemon_thread);
            }
            thread.start();
        }
    }

    void init() throws JSchException {
        io_local.setInputStream(getSession().inputStream);
        io_local.setOutputStream(getSession().outputStream);
    }

    public void setErrStream(java.io.OutputStream out) {
        setExtOutputStream(out);
    }

    public java.io.InputStream getErrStream() throws java.io.IOException {
        return getExtInputStream();
    }
}
