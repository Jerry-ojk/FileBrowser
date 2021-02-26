package com.jcraft.jsch;

import com.jcraft.jsch.request.RequestAgentForwarding;
import com.jcraft.jsch.request.RequestEnv;
import com.jcraft.jsch.request.RequestPtyReq;
import com.jcraft.jsch.request.RequestWindowChange;
import com.jcraft.jsch.request.RequestX11;

import java.util.Enumeration;
import java.util.Hashtable;

public class ChannelSession extends Channel {
    protected volatile boolean agent_forwarding = false;
    protected volatile boolean xforwading = false;
    protected volatile Hashtable<String, String> env = null;

    protected volatile boolean pty = false;

    // protected volatile String ttype = "vt100";
    protected volatile String ttype = "dumb";

    protected volatile int tCol = 80;
    protected volatile int tRow = 24;
    // 行数覆盖像素
    protected volatile int twp = 0;
    protected volatile int thp = 0;
    protected volatile byte[] terminal_mode = null;

    ChannelSession() {
        type = Util.str2byte("session");
        io_local = new IO();
    }

    /**
     * Enable the agent forwarding.
     *
     * @param enable
     */
    public void setAgentForwarding(boolean enable) {
        agent_forwarding = enable;
    }

    /**
     * Enable the X11 forwarding.
     * Refer to RFC4254 6.3.1. Requesting X11 Forwarding.
     *
     * @param enable
     */
    public void setXForwarding(boolean enable) {
        xforwading = enable;
    }


    /**
     * Set the environment variable.
     * Refer to RFC4254 6.4 Environment Variable Passing.
     *
     * @param name  A name of environment variable.
     * @param value A value of environment variable.
     * @see #setEnv(String, String)
     */
    public void setEnv(String name, String value) {
        synchronized (this) {
            getEnv().put(name, value);
        }
    }

    private Hashtable<String, String> getEnv() {
        if (env == null) env = new Hashtable<>();
        return env;
    }

    /**
     * Allocate a Pseudo-Terminal.
     * Refer to RFC4254 6.2. Requesting a Pseudo-Terminal.
     *
     * @param enable
     */
    public void setPty(boolean enable) {
        pty = enable;
    }

    /**
     * Set the terminal mode.
     *
     * @param terminal_mode
     */
    public void setTerminalMode(byte[] terminal_mode) {
        this.terminal_mode = terminal_mode;
    }

    /**
     * Change the window dimension interactively.
     * Refer to RFC4254 6.7. Window Dimension Change Message.
     *
     * @param col terminal width, columns
     * @param row terminal height, rows
     * @param wp  terminal width, pixels
     * @param hp  terminal height, pixels
     */
    public void setPtySize(int col, int row, int wp, int hp) {
        setPtyType(this.ttype, col, row, wp, hp);
        if (!pty || !isConnected()) {
            return;
        }
        try {
            RequestWindowChange request = new RequestWindowChange();
            request.setSize(col, row, wp, hp);
            request.request(getSession(), this);
        } catch (Exception e) {
            //System.err.println("ChannelSessio.setPtySize: "+e);
        }
    }

    /**
     * Set the terminal type.
     * This method is not effective after Channel#connect().
     *
     * @param ttype terminal type(for example, "vt100")
     * @see #setPtyType(String, int, int, int, int)
     */
    public void setPtyType(String ttype) {
        setPtyType(ttype, 80, 24, 640, 480);
    }

    /**
     * Set the terminal type.
     * This method is not effective after Channel#connect().
     *
     * @param ttype terminal type(for example, "vt100")
     * @param col   terminal width, columns
     * @param row   terminal height, rows
     * @param wp    terminal width, pixels
     * @param hp    terminal height, pixels
     */
    public void setPtyType(String ttype, int col, int row, int wp, int hp) {
        this.ttype = ttype;
        this.tCol = col;
        this.tRow = row;
        this.twp = wp;
        this.thp = hp;
    }

    protected void sendRequests() throws Exception {
        final Session session = getSession();
        if (agent_forwarding) {
            new RequestAgentForwarding().request(session, this);
        }

        if (xforwading) {
            new RequestX11().request(session, this);
        }

        if (pty) {
            RequestPtyReq request = new RequestPtyReq();
            request.setTerminalType(ttype);
            request.setTSize(tCol, tRow, twp, thp);
            if (terminal_mode != null) {
                request.setTerminalMode(terminal_mode);
            }
            request.request(session, this);
        }

        if (env != null) {
            for (Enumeration<String> _env = env.keys(); _env.hasMoreElements(); ) {
                String name = _env.nextElement();
                String value = env.get(name);
                RequestEnv request = new RequestEnv();
                request.setEnv(name, value);
                request.request(session, this);
            }
        }
    }

    public void run() {
        Buffer buffer = new Buffer(remoteMaxPacketSize);
        Packet packet = new Packet(buffer);
        int i = -1;
        try {
            while (isConnected() &&
                    thread != null &&
                    io_local != null &&
                    io_local.inputStream != null) {
                i = io_local.inputStream.read(buffer.data,
                        14,
                        buffer.data.length - 14
                                - Session.buffer_margin
                );
                if (i == 0) continue;
                if (i == -1) {
                    eof();
                    break;
                }
                if (close) break;
                //System.outputStream.println("write: "+i);
                packet.reset();
                buffer.putByte((byte) SSH_MSG_CHANNEL_DATA);
                buffer.putInt(recipientId);
                buffer.putInt(i);
                buffer.skipWrite(i);
                getSession().write(packet, this, i);
            }
        } catch (Exception e) {
            //System.err.println("# ChannelExec.run");
            //e.printStackTrace();
        }
        Thread _thread = thread;
        if (_thread != null) {
            synchronized (_thread) {
                _thread.notifyAll();
            }
        }
        thread = null;
        //System.err.println(this+":run <");
    }
}