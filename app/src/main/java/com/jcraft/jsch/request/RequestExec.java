package com.jcraft.jsch.request;

import com.jcraft.jsch.Buffer;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Packet;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Util;

public class RequestExec extends Request {
    private byte[] command = new byte[0];

    public RequestExec(byte[] command) {
        this.command = command;
    }

    @Override
    public void request(Session session, Channel channel) throws Exception {
        super.request(session, channel);

        Buffer buf = new Buffer();
        Packet packet = new Packet(buf);

        // send
        // byte     SSH_MSG_CHANNEL_REQUEST(98)
        // uint32 recipient channel
        // string request type       // "exec"
        // boolean want reply        // 0
        // string command
        packet.reset();
        buf.putByte((byte) Channel.SSH_MSG_CHANNEL_REQUEST);
        buf.putInt(channel.getRecipientId());
        buf.putString(Util.str2byte("exec"));
        buf.putByte((byte) (waitForReply() ? 1 : 0));
        buf.checkFreeSize(4 + command.length);
        buf.putString(command);
        write(packet);
    }
}
