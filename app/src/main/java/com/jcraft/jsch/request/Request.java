package com.jcraft.jsch.request;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.exception.JSchException;
import com.jcraft.jsch.Packet;
import com.jcraft.jsch.Session;

public abstract class Request {
    private boolean reply = false;
    private Session session = null;
    private Channel channel = null;

    public void request(Session session, Channel channel) throws Exception {
        this.session = session;
        this.channel = channel;
        if (channel.connectTimeout > 0) {
            setReply(true);
        }
    }

    public boolean waitForReply() {
        return reply;
    }

    public void setReply(boolean reply) {
        this.reply = reply;
    }

    public void write(Packet packet) throws Exception {
        if (reply) {
            channel.reply = -1;
        }
        session.write(packet);
        if (reply) {
            long start = System.currentTimeMillis();
            long timeout = channel.connectTimeout;
            while (channel.isConnected() && channel.reply == -1) {
                try {
                    Thread.sleep(10);
                } catch (Exception ee) {
                }
                if (timeout > 0L &&
                        (System.currentTimeMillis() - start) > timeout) {
                    channel.reply = 0;
                    throw new JSchException("channel request: timeout");
                }
            }

            if (channel.reply == 0) {
                throw new JSchException("failed to send channel request");
            }
        }
    }
}
