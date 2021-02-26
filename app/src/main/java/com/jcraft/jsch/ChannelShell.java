package com.jcraft.jsch;

import com.jcraft.jsch.exception.JSchException;
import com.jcraft.jsch.request.RequestShell;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jerry.filebrowser.shell.ShellListener;

public class ChannelShell extends ChannelSession {
    private ShellListener listener;

    public void setShellListener(ShellListener listener) {
        this.listener = listener;
    }

    public ChannelShell() {
        pty = true;
    }

    @Override
    void init() throws JSchException {
        io_local.setInputStream(getSession().inputStream);
        io_local.setOutputStream(getSession().outputStream);
    }

    @Override
    public void start() throws JSchException {
        Session session = getSession();
        try {
            sendRequests();
            RequestShell request = new RequestShell();
            request.request(session, this);
        } catch (Exception e) {
            if (e instanceof JSchException) throw (JSchException) e;
            throw new JSchException("ChannelShell", e);
        }
//
//        if (io.inputStream != null) {
//            thread = new Thread(this);
//            thread.setName("Shell for " + session.host);
//            if (session.daemon_thread) {
//                thread.setDaemon(session.daemon_thread);
//            }
//            thread.start();
//        }
    }

    public void sendCommand(String command) throws IOException {
        sendCommand(command.getBytes(StandardCharsets.UTF_8));
    }

    public void sendCommand(byte[] command) throws IOException {
        if (close) throw new IOException("通道已关闭");
        if (!isConnected()) throw new IOException("Session已关闭");
        if (command == null || command.length == 0) return;
        int alreadySend = 0;
        // int left = command.length;
        Buffer buffer = new Buffer(remoteMaxPacketSize);
        Packet packet = new Packet(buffer);
        try {
            while (isConnected()) {
                final int maxLen = remoteMaxPacketSize - (5 + 1 + 4 + 4) - Session.buffer_margin;
                final int len = Math.min(command.length - alreadySend, maxLen);
                if (buffer.data.length < remoteMaxPacketSize) {
                    buffer = new Buffer(remoteMaxPacketSize);
                    packet = new Packet(buffer);
                }
                buffer.setReadOffSet(0);
                packet.reset(); // 5
                buffer.putByte((byte) SSH_MSG_CHANNEL_DATA); // 1
                buffer.putInt(recipientId); // 4
                buffer.putInt(len); // 4
                buffer.putByte(command, alreadySend, len);
                getSession().write(packet, this, len);
                alreadySend += len;
                if (alreadySend == command.length) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    void write(byte[] data, int start, int len) {
        if (listener != null) listener.onReceiveText(data, start, len);
    }
}