package com.jcraft.jsch.request;

import com.jcraft.jsch.Buffer;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Packet;
import com.jcraft.jsch.Session;

public class RequestWindowChange extends Request {
    int width_columns = 80;
    int height_rows = 24;
    int width_pixels = 640;
    int height_pixels = 480;

    public void setSize(int col, int row, int wp, int hp) {
        this.width_columns = col;
        this.height_rows = row;
        this.width_pixels = wp;
        this.height_pixels = hp;
    }

    public void request(Session session, Channel channel) throws Exception {
        super.request(session, channel);

        Buffer buf = new Buffer();
        Packet packet = new Packet(buf);

        //byte      SSH_MSG_CHANNEL_REQUEST
        //uint32    recipient_channel
        //string    "window-change"
        //boolean   FALSE
        //uint32    terminal width, columns
        //uint32    terminal height, rows
        //uint32    terminal width, pixels
        //uint32    terminal height, pixels
        packet.reset();
        buf.putByte((byte) Channel.SSH_MSG_CHANNEL_REQUEST);
        buf.putInt(channel.getRecipientId());
        buf.putString("window-change");
        buf.putByte((byte) (waitForReply() ? 1 : 0));
        buf.putInt(width_columns);
        buf.putInt(height_rows);
        buf.putInt(width_pixels);
        buf.putInt(height_pixels);
        write(packet);
    }
}
