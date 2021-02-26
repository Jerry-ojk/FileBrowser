package com.jcraft.jsch.request;

import com.jcraft.jsch.Buffer;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelX11;
import com.jcraft.jsch.Packet;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Util;

public class RequestX11 extends Request {
    public void setCookie(String cookie) {
        ChannelX11.cookie = Util.str2byte(cookie);
    }

    public void request(Session session, Channel channel) throws Exception {
        super.request(session, channel);

        Buffer buf = new Buffer();
        Packet packet = new Packet(buf);

        // byte      SSH_MSG_CHANNEL_REQUEST(98)
        // uint32 recipient channel
        // string request type        // "x11-req"
        // boolean want reply         // 0
        // boolean   single connection
        // string    x11 authentication protocol // "MIT-MAGIC-COOKIE-1".
        // string    x11 authentication cookie
        // uint32    x11 screen number
        packet.reset();
        buf.putByte((byte) Channel.SSH_MSG_CHANNEL_REQUEST);
        buf.putInt(channel.getRecipientId());
        buf.putString(Util.str2byte("x11-req"));
        buf.putByte((byte) (waitForReply() ? 1 : 0));
        buf.putByte((byte) 0);
        buf.putString(Util.str2byte("MIT-MAGIC-COOKIE-1"));
        buf.putString(ChannelX11.getFakedCookie(session));
        buf.putInt(0);
        write(packet);
        session.x11_forwarding = true;
    }
}
