package com.jcraft.jsch.request;

import com.jcraft.jsch.Buffer;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Packet;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Util;

public class RequestPtyReq extends Request {
    private String terminalType = "vt100";
    private int tcol = 60;
    private int trow = 24;
    private int twp = 1080;
    private int thp = 480;

    private byte[] terminal_mode = Util.EMPTY_BYTE;

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public void setTerminalMode(byte[] terminal_mode) {
        this.terminal_mode = terminal_mode;
    }

    public void setTSize(int tcol, int trow, int twp, int thp) {
        this.tcol = tcol;
        this.trow = trow;
        this.twp = twp;
        this.thp = thp;
    }

    public void request(Session session, Channel channel) throws Exception {
        super.request(session, channel);

        Buffer buf = new Buffer();
        Packet packet = new Packet(buf);

        packet.reset();
        buf.putByte((byte) Channel.SSH_MSG_CHANNEL_REQUEST);
        buf.putInt(channel.getRecipientId());
        buf.putString("pty-req");
        buf.putByte((byte) (waitForReply() ? 1 : 0));
        buf.putString(terminalType);// vt100/dumb
        buf.putInt(tcol);
        buf.putInt(trow);
        buf.putInt(twp);
        buf.putInt(thp);
        buf.putString(terminal_mode);
        write(packet);
    }
}