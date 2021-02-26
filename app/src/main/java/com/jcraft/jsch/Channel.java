package com.jcraft.jsch;

import com.jcraft.jsch.exception.JSchException;
import com.jcraft.jsch.request.RequestSignal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;


public abstract class Channel implements Runnable {

    public static final int SSH_MSG_CHANNEL_OPEN = 90;
    public static final int SSH_MSG_CHANNEL_OPEN_CONFIRMATION = 91;
    public static final int SSH_MSG_CHANNEL_OPEN_FAILURE = 92;
    public static final int SSH_MSG_CHANNEL_WINDOW_ADJUST = 93;
    public static final int SSH_MSG_CHANNEL_DATA = 94;
    public static final int SSH_MSG_CHANNEL_EXTENDED_DATA = 95;
    public static final int SSH_MSG_CHANNEL_EOF = 96;
    public static final int SSH_MSG_CHANNEL_CLOSE = 97;
    public static final int SSH_MSG_CHANNEL_REQUEST = 98;
    public static final int SSH_MSG_CHANNEL_SUCCESS = 99;
    public static final int SSH_MSG_CHANNEL_FAILURE = 100;

    // SSH_MSG_CHANNEL_OPEN_FAILURE 'reason code'
    static final int SSH_OPEN_ADMINISTRATIVELY_PROHIBITED = 1;
    static final int SSH_OPEN_CONNECT_FAILED = 2;
    static final int SSH_OPEN_UNKNOWN_CHANNEL_TYPE = 3;
    static final int SSH_OPEN_RESOURCE_SHORTAGE = 4;

    private static volatile int localChannelIdIndex = 0;
    private static final java.util.Vector<Channel> CHANNEL_POOL = new java.util.Vector<>();

    protected volatile byte[] type = Util.str2byte("foo");

    final int localChannelId;
    volatile int recipientId = -1;


    volatile int localMaxWindowSize = 0x100000;
    volatile int localWindowSize = localMaxWindowSize; // local initial window size
    volatile int localMaxPacketSize = 0x4000;     // local maximum packet size

    volatile long remoteWindowSize = 0;         // remote initial window size
    volatile int remoteMaxPacketSize = 0;        // remote maximum packet size

    volatile IO io_local = null;
    volatile Thread thread = null;

    volatile boolean eof_local = false;
    volatile boolean eof_remote = false;

    volatile boolean close = false;
    volatile boolean connected = false;
    volatile boolean open_confirmation = false;

    volatile int reasonCode = -1;

    public volatile int reply = 0;
    public volatile int connectTimeout = 5000;

    private volatile Session session;

    volatile int notifyMe = 0;

    static Channel getChannel(String type) {
        if (type.equals("sftp")) {
            return new ChannelSftp();
        }
        if (type.equals("shell")) {
            return new ChannelShell();
        }
//        if (type.equals("session")) {
//            return new ChannelSession();
//        }
//        if (type.equals("exec")) {
//            return new ChannelExec();
//        }
//        if (type.equals("x11")) {
//            return new ChannelX11();
//        }
//        if (type.equals("auth-agent@openssh.com")) {
//            return new ChannelAgentForwarding();
//        }
//        if (type.equals("direct-tcpip")) {
//            return new ChannelDirectTCPIP();
//        }
//        if (type.equals("forwarded-tcpip")) {
//            return new ChannelForwardedTCPIP();
//        }
//        if (type.equals("subsystem")) {
//            return new ChannelSubsystem();
//        }
        return null;
    }

    static Channel getChannel(int localId, Session session) {
        synchronized (CHANNEL_POOL) {
            for (int i = 0; i < CHANNEL_POOL.size(); i++) {
                Channel channel = CHANNEL_POOL.elementAt(i);
                if (channel.localChannelId == localId && channel.session == session)
                    return channel;
            }
        }
        return null;
    }

    static void del(Channel c) {
        synchronized (CHANNEL_POOL) {
            CHANNEL_POOL.removeElement(c);
        }
    }

    Channel() {
        synchronized (CHANNEL_POOL) {
            localChannelId = localChannelIdIndex++;
            CHANNEL_POOL.addElement(this);
        }
    }

    void init() throws JSchException {
    }


    public final void connect() throws JSchException {
        connect(5000);
    }

    public void connect(int timeout) throws JSchException {
        if (connected) return;
        if (timeout > 0) this.connectTimeout = timeout;
        try {
            // 不能重载
            sendChannelOpen();
            // 可以重载
            start();
        } catch (Exception e) {
            connected = false;
            disconnect();
            throw new JSchException(JSchException.CODE_ERROR_CONNECT, e);
        }
    }


    final void sendChannelOpen() throws Exception {
        Session session = getSession();
        if (!session.isConnected()) {
            throw new IOException("session没有连接");
        }

        Buffer buf = new Buffer(100);
        Packet packet = new Packet(buf);
        // byte   SSH_MSG_CHANNEL_OPEN(90)
        // string channel type         //
        // uint32 sender channel       // 0
        // uint32 initial window size  // 0x100000(65536)
        // uint32 maxmum packet size   // 0x4000(16384)
        packet.reset();
        buf.putByte((byte) SSH_MSG_CHANNEL_OPEN);
        buf.putString(this.type);
        buf.putInt(this.localChannelId);
        buf.putInt(this.localWindowSize);
        buf.putInt(this.localMaxPacketSize);
        session.write(packet);

        final long start = System.currentTimeMillis();
        long wait = 0;
        synchronized (this) {
            while (recipientId == -1 && session.isConnected() && wait < connectTimeout) {
                try {
                    wait(10L);
                    this.notifyMe = 1;
                } catch (InterruptedException ignored) {
                } finally {
                    this.notifyMe = 0;
                }
                wait = System.currentTimeMillis() - start;
            }
        }
        if (!session.isConnected()) {
            throw new JSchException(JSchException.CODE_ERROR_CONNECT, "session已关闭");
        }
        if (!this.open_confirmation || recipientId == -1) {  // timeout
            throw new JSchException(JSchException.CODE_ERROR_CONNECT, "channel打开失败");
        }
        connected = true;
    }


    public void start() throws JSchException {

    }


    protected void sendOpenConfirmation() throws Exception {
        Buffer buf = new Buffer(100);
        Packet packet = new Packet(buf);
        packet.reset();
        buf.putByte((byte) SSH_MSG_CHANNEL_OPEN_CONFIRMATION);
        buf.putInt(getRecipientId());
        buf.putInt(localChannelId);
        buf.putInt(localWindowSize);
        buf.putInt(localMaxPacketSize);
        getSession().write(packet);
    }

    protected void sendOpenFailure(int reasonCode) {
        try {
            Buffer buf = new Buffer(100);
            Packet packet = new Packet(buf);
            packet.reset();
            buf.putByte((byte) SSH_MSG_CHANNEL_OPEN_FAILURE);
            buf.putInt(getRecipientId());
            buf.putInt(reasonCode);
            buf.putString(Util.str2byte("open failed"));
            buf.putString(Util.EMPTY_BYTE);
            getSession().write(packet);
        } catch (Exception ignored) {
        }
    }


    synchronized void setRecipientId(int foo) {
        this.recipientId = foo;
        if (notifyMe > 0)
            notifyAll();
    }

    public int getRecipientId() {
        return recipientId;
    }

    public void setXForwarding(boolean foo) {
    }

    public boolean isEOF() {
        return eof_remote;
    }

    void getData(Buffer buf) {
        setRecipientId(buf.readInt());
        setRemoteWindowSize(buf.readUInt());
        setRemoteMaxPacketSize(buf.readInt());
    }

    public void setInputStream(InputStream in) {
        io_local.setInputStream(in, false);
    }

    public void setInputStream(InputStream in, boolean doNotClose) {
        io_local.setInputStream(in, doNotClose);
    }

    public void setOutputStream(OutputStream out) {
        io_local.setOutputStream(out, false);
    }

    public void setOutputStream(OutputStream out, boolean doNotClose) {
        io_local.setOutputStream(out, doNotClose);
    }

    public void setExtOutputStream(OutputStream out) {
        io_local.setExtOutputStream(out, false);
    }

    public void setExtOutputStream(OutputStream out, boolean doNotClose) {
        io_local.setExtOutputStream(out, doNotClose);
    }

    public InputStream getInputStream() throws IOException {
        int max_input_buffer_size = 32 * 1024;
        try {
            max_input_buffer_size =
                    Integer.parseInt(getSession().getConfig("max_input_buffer_size"));
        } catch (Exception ignored) {
        }
        PipedInputStream in =
                new MyPipedInputStream(
                        32 * 1024,  // this value should be customizable.
                        max_input_buffer_size
                );
        boolean resizable = 32 * 1024 < max_input_buffer_size;
        io_local.setOutputStream(new PassiveOutputStream(in, resizable), false);
        return in;
    }

    public InputStream getExtInputStream() throws IOException {
        int max_input_buffer_size = 32 * 1024;
        try {
            String size = getSession().getConfig("max_input_buffer_size");
            if (size != null) {
                max_input_buffer_size = Integer.parseInt(size);
            }
        } catch (Exception ignored) {
        }
        MyPipedInputStream in =
                new MyPipedInputStream(
                        32 * 1024,  // this value should be customizable.
                        max_input_buffer_size
                );
        boolean resizable = 32 * 1024 < max_input_buffer_size;
        io_local.setExtOutputStream(new PassiveOutputStream(in, resizable), false);
        return in;
    }
//
//    public OutputStream getOutputStream() {
//        final Channel channel = this;
//        OutputStream out = new OutputStream() {
//            private int dataLen = 0;
//            private Buffer buffer = null;
//            private Packet packet = null;
//            private boolean closed = false;
//
//            private synchronized void init() throws IOException {
//                buffer = new Buffer(remoteMaxPacketSize);
//                packet = new Packet(buffer);
//
//                byte[] _buf = buffer.data;
//                if (_buf.length - (14 + 0) - Session.buffer_margin <= 0) {
//                    buffer = null;
//                    packet = null;
//                    throw new IOException("failed to initialize the channel.");
//                }
//            }
//
//            byte[] b = new byte[1];
//
//            @Override
//            public void write(int w) throws IOException {
//                b[0] = (byte) w;
//                write(b, 0, 1);
//            }
//
//            @Override
//            public void write(byte[] buf, int s, int l) throws IOException {
//                if (packet == null) {
//                    init();
//                }
//
//                if (closed) {
//                    throw new IOException("Already closed");
//                }
//
//                byte[] _buf = buffer.data;
//                int _bufl = _buf.length;
//                while (l > 0) {
//                    int _l = l;
//                    if (l > _bufl - (14 + dataLen) - Session.buffer_margin) {
//                        _l = _bufl - (14 + dataLen) - Session.buffer_margin;
//                    }
//
//                    if (_l <= 0) {
//                        flush();
//                        continue;
//                    }
//
//                    System.arraycopy(buf, s, _buf, 14 + dataLen, _l);
//                    dataLen += _l;
//                    s += _l;
//                    l -= _l;
//                }
//            }
//
//            @Override
//            public void flush() throws IOException {
//                if (closed) {
//                    throw new IOException("Already closed");
//                }
//                if (dataLen == 0)
//                    return;
//                packet.reset();
//                buffer.putByte((byte) Session.SSH_MSG_CHANNEL_DATA);
//                buffer.putInt(recipient);
//                buffer.putInt(dataLen);
//                buffer.skip(dataLen);
//                try {
//                    int foo = dataLen;
//                    dataLen = 0;
//                    synchronized (channel) {
//                        if (!channel.close)
//                            getSession().write(packet, channel, foo);
//                    }
//                } catch (Exception e) {
//                    close();
//                    throw new IOException(e.toString());
//                }
//
//            }
//
//            @Override
//            public void close() throws IOException {
//                if (packet == null) {
//                    try {
//                        init();
//                    } catch (IOException e) {
//                        // close should be finished silently.
//                        return;
//                    }
//                }
//                if (closed) {
//                    return;
//                }
//                if (dataLen > 0) {
//                    flush();
//                }
//                channel.eof();
//                closed = true;
//            }
//        };
//        return out;
//    }

    static class MyPipedInputStream extends PipedInputStream {
        private int BUFFER_SIZE = 1024;
        private int max_buffer_size = BUFFER_SIZE;

        MyPipedInputStream(int size) {
            super();
            buffer = new byte[size];
            BUFFER_SIZE = size;
            max_buffer_size = size;
        }

        MyPipedInputStream(int size, int max_buffer_size) {
            this(size);
            this.max_buffer_size = max_buffer_size;
        }

        MyPipedInputStream(PipedOutputStream out) throws IOException {
            super(out);
        }

        MyPipedInputStream(PipedOutputStream out, int size) throws IOException {
            super(out);
            buffer = new byte[size];
            BUFFER_SIZE = size;
        }

        /*
         * TODO: We should have our own Piped[I/O]Stream implementation.
         * Before accepting data, JDK's PipedInputStream will check the existence of
         * reader thread, and if it is not alive, the stream will be closed.
         * That behavior may cause the problem if multiple threads make access to it.
         */
        public synchronized void updateReadSide() throws IOException {
            if (available() != 0) { // not empty
                return;
            }
            in = 0;
            out = 0;
            buffer[in++] = 0;
            int a = read();
            //System.out.println("updateReadSide:" + a);
            //Log.i("666", "updateReadSide:" + a);
        }

        private int freeSpace() {
            int size = 0;
            if (out < in) {
                size = buffer.length - in;
            } else if (in < out) {
                if (in == -1) size = buffer.length;
                else size = out - in;
            }
            return size;
        }

        synchronized void checkSpace(int len) {
            int size = freeSpace();
            if (size < len) {
                int dataSize = buffer.length - size;
                int foo = buffer.length;
                while ((foo - dataSize) < len) {
                    foo *= 2;
                }

                if (foo > max_buffer_size) {
                    foo = max_buffer_size;
                }
                if ((foo - dataSize) < len) return;

                byte[] tmp = new byte[foo];
                if (out < in) {
                    System.arraycopy(buffer, 0, tmp, 0, buffer.length);
                } else if (in < out) {
                    if (in == -1) {
                    } else {
                        System.arraycopy(buffer, 0, tmp, 0, in);
                        System.arraycopy(buffer, out,
                                tmp, tmp.length - (buffer.length - out),
                                (buffer.length - out));
                        out = tmp.length - (buffer.length - out);
                    }
                } else {
                    System.arraycopy(buffer, 0, tmp, 0, buffer.length);
                    in = buffer.length;
                }
                buffer = tmp;
            } else if (buffer.length == size && size > BUFFER_SIZE) {
                int i = size / 2;
                if (i < BUFFER_SIZE) i = BUFFER_SIZE;
                buffer = new byte[i];
            }
        }
    }

    void setLocalWindowSizeMax(int size) {
        this.localMaxWindowSize = size;
    }

    void setLocalWindowSize(int size) {
        this.localWindowSize = size;
    }

    void setLocalPacketSize(int size) {
        this.localMaxPacketSize = size;
    }

    synchronized void setRemoteWindowSize(long size) {
        this.remoteWindowSize = size;
    }

    synchronized void addRemoteWindowSize(long size) {
        remoteWindowSize += size;
        if (notifyMe > 0)
            notifyAll();
    }

    void setRemoteMaxPacketSize(int size) {
        this.remoteMaxPacketSize = size;
    }

    @Override
    public void run() {
    }

//    void write(byte[] bytes) throws IOException {
//        write(bytes, 0, bytes.length);
//    }

    void write(byte[] bytes, int start, int len) throws IOException {
        if (io_local != null) {
            io_local.send(bytes, start, len);
        }
    }

    void write_ext(byte[] bytes, int start, int len) throws IOException {
        if (io_local != null) {
            io_local.sendExt(bytes, start, len);
        }
    }

    void eof_remote() {
        eof_remote = true;
        if (io_local != null) {
            io_local.closeOut();
        }
    }

    void eof() {
        if (eof_local) return;
        eof_local = true;

        int i = getRecipientId();
        if (i == -1) return;

        try {
            Buffer buf = new Buffer(100);
            Packet packet = new Packet(buf);
            packet.reset();
            buf.putByte((byte) SSH_MSG_CHANNEL_EOF);
            buf.putInt(i);
            synchronized (this) {
                if (!close) getSession().write(packet);
            }
        } catch (Exception e) {
            //System.err.println("Channel.eof");
            //e.printStackTrace();
        }
    /*
    if(!isConnected()){ disconnect(); }
    */
    }

  /*
  http://www1.ietf.org/internet-drafts/draft-ietf-secsh-connect-24.txt

5.3  Closing a Channel
  When a party will no longer send more data to a channel, it SHOULD
   send SSH_MSG_CHANNEL_EOF.

            byte      SSH_MSG_CHANNEL_EOF
            uint32    recipient_channel

  No explicit response is sent to this message.  However, the
   application may send EOF to whatever is at the other end of the
  channel.  Note that the channel remains open after this message, and
   more data may still be sent inputStream the other direction.  This message
   does not consume window space and can be sent even if no window space
   is available.

     When either party wishes to terminate the channel, it sends
     SSH_MSG_CHANNEL_CLOSE.  Upon receiving this message, a party MUST
   send back a SSH_MSG_CHANNEL_CLOSE unless it has already sent this
   message for the channel.  The channel is considered closed for a
     party when it has both sent and received SSH_MSG_CHANNEL_CLOSE, and
   the party may then reuse the channel number.  A party MAY send
   SSH_MSG_CHANNEL_CLOSE without having sent or received
   SSH_MSG_CHANNEL_EOF.

            byte      SSH_MSG_CHANNEL_CLOSE
            uint32    recipient_channel

   This message does not consume window space and can be sent even if no
   window space is available.

   It is recommended that any data sent before this message is delivered
     to the actual destination, if possible.
  */

    void close() {
        if (close) return;
        close = true;
        eof_local = eof_remote = true;

        final int i = getRecipientId();
        if (i == -1) return;

        try {
            Buffer buf = new Buffer(100);
            Packet packet = new Packet(buf);
            packet.reset();
            buf.putByte((byte) SSH_MSG_CHANNEL_CLOSE);
            buf.putInt(i);
            synchronized (this) {
                getSession().write(packet);
            }
        } catch (Exception e) {
            //e.printStackTrace();
        }
    }

    public boolean isClosed() {
        return close;
    }

    static void disconnect(Session session) {
        synchronized (CHANNEL_POOL) {
            for (int i = 0; i < CHANNEL_POOL.size(); i++) {
                Channel channel = CHANNEL_POOL.elementAt(i);
                if (channel.session == session) {
                    channel.disconnect();
                }
            }
        }
    }

    public void disconnect() {
        if (!connected) return;
        try {
            synchronized (this) {
                connected = false;
            }
            close();
            eof_remote = eof_local = true;
            thread = null;
            if (io_local != null) {
                io_local.close();
            }
        } finally {
            Channel.del(this);
        }
    }

    public boolean isConnected() {
        Session _session = this.session;
        if (_session != null) {
            return _session.isConnected() && connected;
        }
        return false;
    }


    public void sendSignal(String signal) throws Exception {
        RequestSignal request = new RequestSignal();
        request.setSignal(signal);
        request.request(getSession(), this);
    }

//  public String toString(){
//      return "Channel: type="+new String(type)+",code="+code+",recipient="+recipient+",window_size="+window_size+",packet_size="+packet_size;
//  }

    class PassiveInputStream extends MyPipedInputStream {
        PipedOutputStream out;

        PassiveInputStream(PipedOutputStream out, int size) throws IOException {
            super(out, size);
            this.out = out;
        }

        PassiveInputStream(PipedOutputStream out) throws IOException {
            super(out);
            this.out = out;
        }

        public void close() throws IOException {
            if (out != null) {
                this.out.close();
            }
            out = null;
        }
    }

    class PassiveOutputStream extends PipedOutputStream {
        private MyPipedInputStream _sink = null;

        PassiveOutputStream(PipedInputStream in,
                            boolean resizable_buffer) throws IOException {
            super(in);
            if (resizable_buffer && (in instanceof MyPipedInputStream)) {
                this._sink = (MyPipedInputStream) in;
            }
        }

        public void write(int b) throws IOException {
            if (_sink != null) {
                _sink.checkSpace(1);
            }
            super.write(b);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            if (_sink != null) {
                _sink.checkSpace(len);
            }
            super.write(b, off, len);
        }
    }

    void setFailureReasonCode(int reasonCode) {
        this.reasonCode = reasonCode;
    }

    public int getFailureReasonCode() {
        return reasonCode;
    }

    void setSession(Session session) {
        this.session = session;
    }

    public Session getSession() throws JSchException {
        if (session == null) throw new JSchException("session为null");
        return session;
    }

    public int getLocalChannelId() {
        return localChannelId;
    }
}
