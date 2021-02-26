package com.jcraft.jsch.request;

import com.jcraft.jsch.Buffer;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Packet;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Util;

public class RequestAgentForwarding extends Request {
    public void request(Session session, Channel channel) throws Exception {
        super.request(session, channel);

        setReply(false);

        Buffer buf = new Buffer();
        Packet packet = new Packet(buf);

        // byte      SSH_MSG_CHANNEL_REQUEST(98)
        // uint32 recipient channel
        // string request type        // "auth-agent-req@openssh.com"
        // boolean want reply         // 0
        packet.reset();
        buf.putByte((byte) Channel.SSH_MSG_CHANNEL_REQUEST);
        buf.putInt(channel.getRecipientId());
        buf.putString(Util.str2byte("auth-agent-req@openssh.com"));
        buf.putByte((byte) (waitForReply() ? 1 : 0));
        write(packet);
        session.agent_forwarding = true;
    }
}
