package com.jcraft.jsch.request;

import com.jcraft.jsch.Buffer;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Packet;
import com.jcraft.jsch.Session;

public class RequestShell extends Request {
    public void request(Session session, Channel channel) throws Exception {
        super.request(session, channel);

        Buffer buf = new Buffer();
        Packet packet = new Packet(buf);
        packet.reset();
        /**
         * byte      SSH_MSG_CHANNEL_REQUEST
         * uint32    recipient channel
         * string    "shell"
         *  boolean   want reply
         */
        buf.putByte((byte) Channel.SSH_MSG_CHANNEL_REQUEST);
        buf.putInt(channel.getRecipientId());
        buf.putString("shell");
        buf.putByte((byte) (waitForReply() ? 1 : 0));
        write(packet);
    }
}
