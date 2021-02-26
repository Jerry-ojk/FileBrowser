package com.jcraft.jsch;

import com.jcraft.jsch.exception.JSchAuthCancelException;
import com.jcraft.jsch.exception.JSchException;
import com.jcraft.jsch.exception.JSchPartialAuthException;
import com.jcraft.jsch.jce.RandomImpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Vector;

import javax.crypto.Cipher;

import jerry.filebrowser.ssh.SSHConnectConfig;
import jerry.filebrowser.ftp.callback.SessionListener;

public class Session implements Runnable {

    // http://ietf.org/internet-drafts/draft-ietf-secsh-assignednumbers-01.txt
    public static final int SSH_MSG_DISCONNECT = 1;
    public static final int SSH_MSG_IGNORE = 2;
    public static final int SSH_MSG_UNIMPLEMENTED = 3;
    public static final int SSH_MSG_DEBUG = 4;
    public static final int SSH_MSG_SERVICE_REQUEST = 5;
    public static final int SSH_MSG_SERVICE_ACCEPT = 6;
    public static final int SSH_MSG_KEXINIT = 20;
    public static final int SSH_MSG_NEWKEYS = 21;
    public static final int SSH_MSG_KEXDH_INIT = 30;
    public static final int SSH_MSG_KEXDH_REPLY = 31;
    public static final int SSH_MSG_KEX_DH_GEX_GROUP = 31;
    public static final int SSH_MSG_KEX_DH_GEX_INIT = 32;
    public static final int SSH_MSG_KEX_DH_GEX_REPLY = 33;
    public static final int SSH_MSG_KEX_DH_GEX_REQUEST = 34;
    public static final int SSH_MSG_GLOBAL_REQUEST = 80;
    public static final int SSH_MSG_REQUEST_SUCCESS = 81;
    public static final int SSH_MSG_REQUEST_FAILURE = 82;


    private static final int PACKET_MAX_SIZE = 256 * 1024;
    public static final int DEFAULT_TIMEOUT = 5000;


    final int max_auth_tries = 6;

    private volatile byte[] serveSSHVersionByte;// server version
    private volatile String serveSSHVersion;// server version
    private static final byte[] CLIENT_VERSION = Util.str2byte("SSH-2.0-Jerry_" + SSHClient.VERSION); // client version

    private volatile byte[] I_C; // the payload of the client's SSH_MSG_KEXINIT
    private volatile byte[] I_S; // the payload of the server's SSH_MSG_KEXINIT
    private volatile byte[] K_S; // the host key

    private volatile byte[] session_id;

    private volatile byte[] IVc2s;
    private volatile byte[] IVs2c;
    private volatile byte[] Ec2s;
    private volatile byte[] Es2c;
    private volatile byte[] MACc2s;
    private volatile byte[] MACs2c;

    private volatile int seqi = 0;
    private volatile int seqo = 0;

    private volatile String[] guess = null;
    private volatile JCipher cipherStoC;
    private volatile JCipher CipherCtoS;
    private volatile MAC macStoC;
    private volatile MAC macCtoS;
    //private byte[] mac_buf;
    private byte[] s2cmac_result1;
    private byte[] s2cmac_result2;

    private Compression deflater;// 压缩
    private Compression inflater;

    private volatile IO networkIO;
    private volatile Socket socket;
    private volatile int timeout = DEFAULT_TIMEOUT;

    private volatile boolean isConnected = false;

    private volatile boolean isAuthed = false;

    private volatile Thread connectThread = null;
    private final Object lock = new Object();

    public volatile boolean x11_forwarding = false;
    public volatile boolean agent_forwarding = false;

    volatile InputStream inputStream = null;
    volatile OutputStream outputStream = null;

    static Random random;

    volatile Buffer buffer;
    volatile Packet packet;

    private volatile SocketFactory socketFactory = null;

    static final int buffer_margin = 32 + // maximum padding length
            64 + // maximum mac length
            32;  // margin for deflater; deflater may inflate data

    private java.util.Hashtable<String, String> config = null;

    private volatile Proxy proxy = null;
    private volatile String hostKeyAlias = null;
    private volatile int serverAliveInterval = 0;
    private volatile int serverAliveCountMax = 1;

    private volatile IdentityRepository identityRepository = null;
    private volatile HostKeyRepository hostkeyRepository = null;

    volatile boolean daemon_thread = false;

    private volatile long kex_start_time = 0L;

    int auth_failures = 0;

    private SSHClient client;
    private String host = null;
    private volatile String org_host = "127.0.0.1";
    private int port = 22;

    private String username = null;
    String password = null;
    volatile byte[] passwordByte = null;

    private volatile SessionListener listener;
    private volatile UserInfo userinfo;

    private volatile SSHConnectConfig connectConfig;

    public Session(SSHClient client, SSHConnectConfig connectConfig) {
        this.client = client;
        buffer = new Buffer();
        packet = new Packet(buffer);
        this.username = connectConfig.user;
        this.org_host = this.host = connectConfig.host;
        this.port = connectConfig.port;
        this.password = connectConfig.pwd;
        this.passwordByte = Util.str2byte(connectConfig.pwd);
        this.connectConfig = connectConfig;

//        applyConfig();
//
//        if (host == null) {
//            throw new JSchException(JSchException.CODE_ERROR_MISS_PARAMETER, "缺少参数：主机");
//        }
//
//        if (username == null) {
////            try {
////                this.username = (String) (System.getProperties().get("user.name"));
////            } catch (SecurityException e) {
////
////            }
//            throw new JSchException(JSchException.CODE_ERROR_MISS_PARAMETER, "缺少参数：用户名");
//        }
    }

    public void connect() throws JSchException {
        connect(timeout);
    }

    public void connect(int timeout) throws JSchException {
        if (isConnected) {
            return;
        }
        if (timeout <= 0) {
            timeout = DEFAULT_TIMEOUT;
        }
        networkIO = new IO();
        random = new RandomImpl();
        Packet.setRANDOM(random);
        try {
            if (proxy == null) {
                InputStream in;
                OutputStream out;
                if (socketFactory == null) {
                    socket = new Socket();
                    socket.connect(new InetSocketAddress(host, port), timeout);
                    in = socket.getInputStream();
                    out = socket.getOutputStream();
                } else {
                    socket = socketFactory.createSocket(host, port);
                    in = socketFactory.getInputStream(socket);
                    out = socketFactory.getOutputStream(socket);
                }
                socket.setSoTimeout(DEFAULT_TIMEOUT);
                socket.setTcpNoDelay(true);
                networkIO.setInputStream(in);
                networkIO.setOutputStream(out);
            } else {
                synchronized (proxy) {
                    proxy.connect(socketFactory, host, port, timeout);
                    networkIO.setInputStream(proxy.getInputStream());
                    networkIO.setOutputStream(proxy.getOutputStream());
                    socket = proxy.getSocket();
                }
            }

            if (socket == null) {
                throw new JSchException(JSchException.CODE_ERROR_NULL, "socket 创建失败");
            }

            socket.setSoTimeout(timeout);
            isConnected = true;
            client.addSession(this);
            if (listener != null) {
                listener.onSocketConnect();
            }

            {
                // Some Cisco devices will miss to read '\n' if it is sent separately.
                byte[] foo = new byte[CLIENT_VERSION.length + 1];
                System.arraycopy(CLIENT_VERSION, 0, foo, 0, CLIENT_VERSION.length);
                foo[foo.length - 1] = (byte) '\n';
                networkIO.send(foo, 0, foo.length);
            }
            int i = 0, j = 0;
            while (true) {
                i = 0;
                j = 0;
                while (i < buffer.data.length) {
                    j = networkIO.getByte();
                    if (j < 0) break;
                    buffer.data[i] = (byte) j;
                    i++;
                    if (j == 10) break;// "/n"
                }
                if (j < 0) {
                    throw new JSchException(JSchException.CODE_ERROR_CONNECT, "connection is closed by foreign host");
                }

                if (buffer.data[i - 1] == 10) {    // 0x0a
                    i--;
                    if (i > 0 && buffer.data[i - 1] == 13) {  // 0x0d
                        i--;
                    }
                }

                if (i <= 3 ||
                        ((i != buffer.data.length) &&
                                (buffer.data[0] != 'S' || buffer.data[1] != 'S' ||
                                        buffer.data[2] != 'H' || buffer.data[3] != '-'))) {
                    // It must not start with 'SSH-'
                    //System.err.println(new String(buf.data, 0, i);
                    continue;
                }

                if (i == buffer.data.length ||
                        i < 7 ||                                      // SSH-1.99 or SSH-2.0
                        (buffer.data[4] == '1' && buffer.data[6] != '9')  // SSH-1.5
                ) {
                    throw new JSchException(JSchException.CODE_ERROR_CONNECT, "服务器SFTP版本号无效");
                }
                break;
            }

            // 获取到服务器SSH_Version
            serveSSHVersionByte = new byte[i];
            System.arraycopy(buffer.data, 0, serveSSHVersionByte, 0, i);

            serveSSHVersion = new String(serveSSHVersionByte, StandardCharsets.UTF_8);
            if (listener != null) listener.onReceiveServerSSHVersion(serveSSHVersion);

            sendKexInit();
            read(buffer);
            if (buffer.getCommand() != SSH_MSG_KEXINIT) {
                in_kex = false;
                throw new JSchException(JSchException.CODE_ERROR_CONNECT, "无效的服务器协议: " + buffer.getCommand());
            }
            // SSH_MSG_KEXINIT
            if (listener != null) listener.onReceiveKeyExchange();

            KeyExchange kex = readKexInit(buffer);
            while (true) {
                read(buffer);
                if (kex.getState() == buffer.getCommand()) {
                    kex_start_time = System.currentTimeMillis();
                    boolean result = kex.next(buffer);
                    if (!result) {
                        //System.err.println("verify: "+result);
                        in_kex = false;
                        throw new JSchException(JSchException.CODE_ERROR_PROCESS, "verify: " + result);
                    }
                } else {
                    in_kex = false;
                    throw new JSchException(JSchException.CODE_ERROR_PROCESS, "无效的服务器协议(kex): " + buffer.getCommand());
                }
                if (kex.getState() == KeyExchange.STATE_END) {
                    break;
                }
            }

            try {
                long tmp = System.currentTimeMillis();
                in_prompt = true;
                checkHost(host, port, kex);
                in_prompt = false;
                kex_start_time += (System.currentTimeMillis() - tmp);
            } catch (JSchException e) {
                in_kex = false;
                in_prompt = false;
                throw e;
            }

            send_newkeys();
            // receive SSH_MSG_NEWKEYS(21)
            read(buffer);
            //System.err.println("read: 21 ? "+buf.getCommand());

            if (buffer.getCommand() == SSH_MSG_NEWKEYS) {
                if (listener != null) listener.onReceiveNewKey();
                receiveNewKeys(kex);
            } else {
                in_kex = false;
                throw new JSchException(JSchException.CODE_ERROR_PROCESS, "无效的服务器协议(new keys): " + buffer.getCommand());
            }
            boolean auth = startUserAuth();
            if (!auth) {
                throw new JSchException(JSchException.CODE_ERROR_AUTH, "Auth fail");
            } else {
                startConnectThread();
            }
        } catch (Exception e) {
            in_kex = false;
            try {
                if (isConnected) {
                    String message = e.toString();
                    packet.reset();
                    buffer.checkFreeSize(1 + 4 * 3 + message.length() + 2 + buffer_margin);
                    buffer.putByte((byte) SSH_MSG_DISCONNECT);
                    buffer.putInt(3);
                    buffer.putString(Util.str2byte(message));
                    buffer.putString(Util.str2byte("en"));
                    write(packet);
                }
            } catch (Exception ignored) {
            }
            disconnect();
            isConnected = false;
            //e.printStackTrace();
//            if (e instanceof RuntimeException) throw (RuntimeException) e;
            if (e instanceof JSchException) {
                throw (JSchException) e;
            } else {
                throw new JSchException(JSchException.CODE_ERROR_CONNECT, "Session.connect():", e);
            }
        } finally {
            Util.clearZero(this.passwordByte);
            this.password = null;
            this.passwordByte = null;
        }
    }

    public boolean startUserAuth() throws Exception {
        // 启动用户鉴权服务
        packet.reset();
        buffer.putByte((byte) Session.SSH_MSG_SERVICE_REQUEST);
        buffer.putString("ssh-userauth");
        write(packet);
        read(buffer);
        int command = buffer.getCommand();

        // SSH_MSG_DISCONNECT
        if (command != SSH_MSG_SERVICE_ACCEPT) {
            throw new JSchException("不支持用户鉴权服务");
        }

        boolean auth = false;
        boolean auth_cancel = false;

        UserAuth userAuth = new UserAuthPassword();
        auth = userAuth.start(this);
        if (auth) {
            return true;
        }
        auth_failures++;

        // 寻找最优先的鉴权方法
        String preferredAuthentications = getConfig("PreferredAuthentications");
        String[] localAuthMethods = Util.split(preferredAuthentications, ",");
        String otherServerAuthMethodString = ((UserAuthPassword) userAuth).getOtherAuthMethods();
        if (listener != null) {
            listener.onReceiveUserAuth(otherServerAuthMethodString);
        }
        if (otherServerAuthMethodString != null) {
            otherServerAuthMethodString = otherServerAuthMethodString.toLowerCase();
        } else {
            otherServerAuthMethodString = preferredAuthentications;
        }
        String[] otherServerAuthMethods = Util.split(otherServerAuthMethodString, ",");

        int methodIndex = 0;

        loop:
        while (true) {
            auth_failures++;
            if (auth_failures > max_auth_tries) {
                return false;
            }
            while (methodIndex < localAuthMethods.length) {
                String method = localAuthMethods[methodIndex++];
                if ("password".equals(method)) continue;
                boolean acceptable = false;
                for (int k = 0; k < otherServerAuthMethods.length; k++) {
                    if (otherServerAuthMethods[k].equals(method)) {
                        acceptable = true;
                        break;
                    }
                }
                if (!acceptable) {
                    continue;
                }

                if (listener != null) {
                    listener.onNextUserAuth(method);
                }

                userAuth = null;
                try {
                    String name = getConfig("userauth." + method);
                    if (name == null) continue;
                    Class c = Class.forName(name);
                    userAuth = (UserAuth) (c.newInstance());

                    auth = userAuth.start(this);

                    if (auth) {
                        SSHClient.log("用户鉴权方法：" + method + " 鉴权成功");
                        return true;
                    }
                } catch (JSchAuthCancelException ee) {
                    auth_cancel = true;
                    auth_failures++;
                } catch (JSchPartialAuthException ee) {
                    String oldMethod = otherServerAuthMethodString;
                    otherServerAuthMethodString = ee.getOtherServerAuthMethods();
                    otherServerAuthMethods = Util.split(otherServerAuthMethodString, ",");
                    if (!oldMethod.equals(otherServerAuthMethodString)) {
                        methodIndex = 0;
                    }
//                    auth_cancel = false;
                    auth_failures++;
                    continue loop;
                } catch (JSchException ee) {
                    // auth_failures++;
                    // throw ee;
                } catch (Exception ee) {
                    if (userAuth == null) {
                        SSHClient.log("用户鉴权方法：" + method + " 加载失败");
                    } else {
                        SSHClient.log("用户鉴权时发生异常");
                        break loop;
                    }
                    ee.printStackTrace();
                }
                auth_failures++;
            }
            break;
        }
        return auth;
    }

    private void startConnectThread() throws JSchException {
        isAuthed = true;
        synchronized (lock) {
//                if (isConnected) {
            connectThread = new Thread(this);
            connectThread.setName("Connect thread " + host + " session");
            if (daemon_thread) {
                connectThread.setDaemon(true);
            }
            connectThread.start();
            requestPortForwarding();
//                } else {
//                    // The session has been already down and
//                    // we don't have to start new thread.
//                }
        }
    }

    private KeyExchange readKexInit(Buffer buffer) throws Exception {
        int j = buffer.readInt();
        if (j != buffer.getLength()) {    // packet was compressed and
            buffer.readByte();           // j is the size of deflated packet.
            I_S = new byte[buffer.indexWrite - 5];
        } else {
            I_S = new byte[j - 1 - buffer.readByte()];
        }
        System.arraycopy(buffer.data, buffer.indexRead, I_S, 0, I_S.length);

        if (!in_kex) {     // We are inputStream rekeying activated by the remote!
            sendKexInit();
        }

        guess = KeyExchange.guess(I_S, I_C);
        if (guess == null) {
            throw new JSchException(JSchException.CODE_ERROR_PROCESS, "Algorithm negotiation fail");
        }

        if (!isAuthed &&
                (guess[KeyExchange.PROPOSAL_ENC_ALGS_CTOS].equals("none") ||
                        (guess[KeyExchange.PROPOSAL_ENC_ALGS_STOC].equals("none")))) {
            throw new JSchException(JSchException.CODE_ERROR_PROCESS, "NONE Cipher should not be chosen before authentification is successed.");
        }

        KeyExchange kex;
        try {
            Class c = Class.forName(getConfig(guess[KeyExchange.PROPOSAL_KEX_ALGS]));
            kex = (KeyExchange) (c.newInstance());
        } catch (Exception e) {
            throw new JSchException(JSchException.CODE_ERROR_CONFIG, e);
        }

        kex.init(this, serveSSHVersionByte, CLIENT_VERSION, I_S, I_C);
        return kex;
    }

    private volatile boolean in_kex = false;
    private volatile boolean in_prompt = false;

    public void rekey() throws Exception {
        sendKexInit();
    }

    private void sendKexInit() throws Exception {
        if (in_kex) return;

        String cipherC2S_available = checkAvailableCiphers();
        String cipherS2C_available = cipherC2S_available;
        if (cipherC2S_available == null) {
            throw new JSchException(JSchException.CODE_ERROR_PROCESS, "没有可用的Ciphers");
        }

        String kex_algorithms_available = checkAvailableKexes();
        if (kex_algorithms_available == null) {
            throw new JSchException(JSchException.CODE_ERROR_PROCESS, "没有可用的kex_algorithms");
        }


        String server_host_key_algorithms_available = checkAvailableSignatures();
        if (server_host_key_algorithms_available == null) {
            throw new JSchException(JSchException.CODE_ERROR_PROCESS, "没有可用的server_host_key_algorithms");
        }

        in_kex = true;
        kex_start_time = System.currentTimeMillis();

        // byte      SSH_MSG_KEXINIT(20)
        // byte[16]  cookie (random bytes)
        // string    kex_algorithms
        // string    server_host_key_algorithms
        // string    encryption_algorithms_client_to_server
        // string    encryption_algorithms_server_to_client
        // string    mac_algorithms_client_to_server
        // string    mac_algorithms_server_to_client
        // string    compression_algorithms_client_to_server
        // string    compression_algorithms_server_to_client
        // string    languages_client_to_server
        // string    languages_server_to_client
        Buffer buf = new Buffer();                // send_kexinit may be invoked
        Packet packet = new Packet(buf);          // by user thread.
        packet.reset();
        buf.putByte((byte) SSH_MSG_KEXINIT);

        synchronized (random) {
            random.fill(buf.data, buf.indexWrite, 16);
            buf.skipWrite(16);
        }

        buf.putString(kex_algorithms_available);
        buf.putString(server_host_key_algorithms_available);
        buf.putString(cipherC2S_available);
        buf.putString(cipherS2C_available);
        buf.putString(getConfig("mac.c2s"));
        buf.putString(getConfig("mac.s2c"));
        buf.putString(getConfig("compression.c2s"));// none
        buf.putString(getConfig("compression.s2c"));// none
        buf.putString(getConfig("lang.c2s"));
        buf.putString(getConfig("lang.s2c"));
        buf.putByte((byte) 0);
        buf.putInt(0);

        buf.setReadOffSet(5);
        I_C = new byte[buf.getLength()];
        buf.readByte(I_C);

        write(packet);
        SSHClient.log("发送 SSH_MSG_KEXINIT");
    }

    private void send_newkeys() throws Exception {
        // send SSH_MSG_NEWKEYS(21)
        packet.reset();
        buffer.putByte((byte) SSH_MSG_NEWKEYS);
        write(packet);
        SSHClient.log("发送 SSH_MSG_NEWKEYS");
    }

    private void checkHost(String chost, int port, KeyExchange kex) throws JSchException {
        String shkc = getConfig("StrictHostKeyChecking");
        SSHClient.log("checkHost：StrictHostKeyChecking：" + shkc);
        if (hostKeyAlias != null) {
            chost = hostKeyAlias;
        }

        //System.err.println("shkc: "+shkc);

        byte[] K_S = kex.getHostKey();
        String key_type = kex.getKeyType();
        String key_fprint = kex.getFingerPrint();

        if (hostKeyAlias == null && port != 22) {
            chost = ("[" + chost + "]:" + port);
        }

        HostKeyRepository hkr = getHostKeyRepository();

        String hkh = getConfig("HashKnownHosts");
        if (hkh.equals("yes") && (hkr instanceof KnownHosts)) {
            hostkey = ((KnownHosts) hkr).createHashedHostKey(chost, K_S);
        } else {
            hostkey = new HostKey(chost, K_S);
        }

        int i = 0;
        synchronized (hkr) {
            i = hkr.check(chost, K_S);
        }

        boolean insert = false;
        if ((shkc.equals("ask") || shkc.equals("yes")) &&
                i == HostKeyRepository.CHANGED) {
            String file = null;
            synchronized (hkr) {
                file = hkr.getKnownHostsRepositoryID();
            }
            if (file == null) {
                file = "known_hosts";
            }

            boolean b = false;

            if (userinfo != null) {
                String message =
                        "WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!\n" +
                                "IT IS POSSIBLE THAT SOMEONE IS DOING SOMETHING NASTY!\n" +
                                "Someone could be eavesdropping on you right now (man-inputStream-the-middle attack)!\n" +
                                "It is also possible that the " + key_type + " host key has just been changed.\n" +
                                "The fingerprint for the " + key_type + " key sent by the remote host " + chost + " is\n" +
                                key_fprint + ".\n" +
                                "Please contact your system administrator.\n" +
                                "Add correct host key inputStream " + file + " to get rid of this message.";

                if (shkc.equals("ask")) {
                    b = userinfo.promptYesNo(message +
                            "\nDo you want to delete the old key and insert the new key?");
                } else {  // shkc.equals("yes")
                    userinfo.showMessage(message);
                }
            }

            if (!b) {
                throw new JSchException(JSchException.CODE_ERROR_PROCESS, "HostKey has been changed: " + chost);
            }

            synchronized (hkr) {
                hkr.remove(chost,
                        kex.getKeyAlgorithmName(),
                        null);
                insert = true;
            }
        }

        if ((shkc.equals("ask") || shkc.equals("yes")) &&
                (i != HostKeyRepository.OK) && !insert) {
            if (shkc.equals("yes")) {
                throw new JSchException(JSchException.CODE_ERROR_PROCESS, "reject HostKey: " + host);
            }
            //System.err.println("finger-print: "+key_fprint);
            if (userinfo != null) {
                boolean foo = userinfo.promptYesNo(
                        "The authenticity of host '" + host + "' can't be established.\n" +
                                key_type + " key fingerprint is " + key_fprint + ".\n" +
                                "Are you sure you want to continue connecting?"
                );
                if (!foo) {
                    throw new JSchException(JSchException.CODE_ERROR_PROCESS, "reject HostKey: " + host);
                }
                insert = true;
            } else {
                if (i == HostKeyRepository.NOT_INCLUDED)
                    throw new JSchException(JSchException.CODE_ERROR_PROCESS, "UnknownHostKey: " + host + ". " + key_type + " key fingerprint is " + key_fprint);
                else
                    throw new JSchException(JSchException.CODE_ERROR_PROCESS, "HostKey has been changed: " + host);
            }
        }

        if (shkc.equals("no") && HostKeyRepository.NOT_INCLUDED == i) {
            insert = true;
        }

        if (i == HostKeyRepository.OK) {
            HostKey[] keys =
                    hkr.getHostKey(chost, kex.getKeyAlgorithmName());
            String _key = Util.byte2str(Util.toBase64(K_S, 0, K_S.length));
            for (int j = 0; j < keys.length; j++) {
                if (keys[i].getKey().equals(_key) &&
                        keys[j].getMarker().equals("@revoked")) {
                    if (userinfo != null) {
                        userinfo.showMessage(
                                "The " + key_type + " host key for " + host + " is marked as revoked.\n" +
                                        "This could mean that a stolen key is being used to " +
                                        "impersonate this host.");
                    }
                    if (SSHClient.getLogger().isEnabled(Logger.INFO)) {
                        SSHClient.getLogger().log(Logger.INFO,
                                "Host '" + host + "' has provided revoked key.");
                    }
                    throw new JSchException(JSchException.CODE_ERROR_PROCESS, "revoked HostKey: " + host);
                }
            }
        }

        if (i == HostKeyRepository.OK) {
            SSHClient.log("Host '" + host + "' is known and matches the " + key_type + " host key");
        }

        if (insert) {
            SSHClient.log("Permanently added '" + host + "' (" + key_type + ") to the list of known hosts.");
        }

        if (insert) {
            synchronized (hkr) {
                hkr.add(hostkey, userinfo);
            }
        }
    }

    public <T extends Channel> T openChannel(String type) throws JSchException {
        if (!isConnected) {
            throw new JSchException(JSchException.CODE_ERROR_CONNECT, "session is down");
        }
        Channel channel = Channel.getChannel(type);
        if (channel != null) {
            addChannel(channel);
            channel.init();
        }
        if (channel instanceof ChannelSession) {
            applyConfigChannel((ChannelSession) channel);
        }
        return (T) channel;
    }

    // encode will bin invoked inputStream write with synchronization.
    public void encode(Packet packet) throws Exception {
        if (deflater != null) {
            compress_len[0] = packet.buffer.indexWrite;
            packet.buffer.data = deflater.compress(packet.buffer.data, 5, compress_len);
            packet.buffer.indexWrite = compress_len[0];
        }
        if (CipherCtoS != null) {
            //packet.padding(c2scipher.getIVSize());
            packet.padding(c2sCipherSize);
            int pad = packet.buffer.data[4];
            synchronized (random) {
                random.fill(packet.buffer.data, packet.buffer.indexWrite - pad, pad);
            }
        } else {
            packet.padding(8);
        }

        if (macCtoS != null) {
            macCtoS.update(seqo);
            macCtoS.update(packet.buffer.data, 0, packet.buffer.indexWrite);
            macCtoS.doFinal(packet.buffer.data, packet.buffer.indexWrite);
        }
        if (CipherCtoS != null) {
            byte[] buf = packet.buffer.data;
            CipherCtoS.update(buf, 0, packet.buffer.indexWrite, buf, 0);
        }
        if (macCtoS != null) {
            packet.buffer.skipWrite(macCtoS.getBlockSize());
        }
    }

    int[] uncompress_len = new int[1];
    int[] compress_len = new int[1];

    private int s2cCipherSize = 8;
    private int c2sCipherSize = 8;

    public synchronized void read(Buffer buffer) throws Exception {
        int j = 0;
        while (true) {
            buffer.reset();
            networkIO.getByte(buffer.data, buffer.indexWrite, s2cCipherSize);
            buffer.indexWrite += s2cCipherSize;

            if (cipherStoC != null) {
                cipherStoC.update(buffer.data, 0, s2cCipherSize, buffer.data, 0);
            }
            j = ((buffer.data[0] << 24) & 0xff000000) |
                    ((buffer.data[1] << 16) & 0x00ff0000) |
                    ((buffer.data[2] << 8) & 0x0000ff00) |
                    ((buffer.data[3]) & 0x000000ff);
            // RFC 4253 6.1. Maximum Packet Length
            if (j < 5 || j > PACKET_MAX_SIZE) {
                start_discard(buffer, cipherStoC, macStoC, j, PACKET_MAX_SIZE);
            }
            int need = j + 4 - s2cCipherSize;
            //if(need<0){
            //  throw new IOException("invalid data");
            //}
            if ((buffer.indexWrite + need) > buffer.data.length) {
                byte[] foo = new byte[buffer.indexWrite + need];
                System.arraycopy(buffer.data, 0, foo, 0, buffer.indexWrite);
                buffer.data = foo;
            }

            if ((need % s2cCipherSize) != 0) {
                String message = "Bad packet length " + need;
                if (SSHClient.getLogger().isEnabled(Logger.FATAL)) {
                    SSHClient.getLogger().log(Logger.FATAL, message);
                }
                start_discard(buffer, cipherStoC, macStoC, j, PACKET_MAX_SIZE - s2cCipherSize);
            }

            if (need > 0) {
                networkIO.getByte(buffer.data, buffer.indexWrite, need);
                buffer.indexWrite += need;
                if (cipherStoC != null) {
                    cipherStoC.update(buffer.data, s2cCipherSize, need, buffer.data, s2cCipherSize);
                }
            }

            if (macStoC != null) {
                macStoC.update(seqi);
                macStoC.update(buffer.data, 0, buffer.indexWrite);

                macStoC.doFinal(s2cmac_result1, 0);
                networkIO.getByte(s2cmac_result2, 0, s2cmac_result2.length);
                if (!java.util.Arrays.equals(s2cmac_result1, s2cmac_result2)) {
                    if (need > PACKET_MAX_SIZE) {
                        throw new IOException("MAC Error");
                    }
                    start_discard(buffer, cipherStoC, macStoC, j, PACKET_MAX_SIZE - need);
                    continue;
                }
            }

            seqi++;

            if (inflater != null) {
                //inflater.uncompress(buf);
                int pad = buffer.data[4];
                uncompress_len[0] = buffer.indexWrite - 5 - pad;
                byte[] foo = inflater.decompress(buffer.data, 5, uncompress_len);
                if (foo != null) {
                    buffer.data = foo;
                    buffer.indexWrite = 5 + uncompress_len[0];
                } else {
                    System.err.println("fail inputStream inflater");
                    break;
                }
            }

            final int type = buffer.getCommand() & 0xff;
            if (type == SSH_MSG_DISCONNECT) {
                buffer.rewind();
                buffer.readInt();
                buffer.readShort();
                int reason_code = buffer.readInt();
                byte[] description = buffer.getStringByte();
                byte[] language_tag = buffer.getStringByte();
                throw new JSchException(JSchException.CODE_ERROR_CONNECT, "SSH_MSG_DISCONNECT: " +
                        reason_code +
                        " " + Util.byte2str(description) +
                        " " + Util.byte2str(language_tag));
                //break;
            } else if (type == SSH_MSG_IGNORE) {
            } else if (type == SSH_MSG_UNIMPLEMENTED) {
                buffer.rewind();
                buffer.readInt();
                buffer.readShort();
                int reason_id = buffer.readInt();
                if (SSHClient.getLogger().isEnabled(Logger.INFO)) {
                    SSHClient.getLogger().log(Logger.INFO,
                            "Received SSH_MSG_UNIMPLEMENTED for " + reason_id);
                }
            } else if (type == SSH_MSG_DEBUG) {
                buffer.rewind();
                buffer.readInt();
                buffer.readShort();
/*
	byte always_display=(byte)buf.getByte();
	byte[] message=buf.getString();
	byte[] language_tag=buf.getString();
	System.err.println("SSH_MSG_DEBUG:"+
			   " "+Util.byte2str(message)+
			   " "+Util.byte2str(language_tag));
*/
            } else if (type == Channel.SSH_MSG_CHANNEL_WINDOW_ADJUST) {
                buffer.rewind();
                buffer.readInt();
                buffer.readShort();
                Channel c = Channel.getChannel(buffer.readInt(), this);
                if (c != null) {
                    c.addRemoteWindowSize(buffer.readUInt());
                }
            } else if (type == UserAuth.SSH_MSG_USERAUTH_SUCCESS) {
                isAuthed = true;
                if (inflater == null && deflater == null) {
                    String method;
                    method = guess[KeyExchange.PROPOSAL_COMP_ALGS_CTOS];
                    initDeflater(method);
                    method = guess[KeyExchange.PROPOSAL_COMP_ALGS_STOC];
                    initInflater(method);
                }
                break;
            } else {
                break;
            }
        }
        buffer.rewind();
    }

    private void start_discard(Buffer buf, JCipher cipher, MAC mac,
                               int packet_length, int discard) throws JSchException, IOException {
        MAC discard_mac = null;

        if (!cipher.isCBC()) {
            throw new JSchException(JSchException.CODE_ERROR_PROCESS, "Packet corrupt");
        }

        if (packet_length != PACKET_MAX_SIZE && mac != null) {
            discard_mac = mac;
        }

        discard -= buf.indexWrite;

        while (discard > 0) {
            buf.reset();
            int len = discard > buf.data.length ? buf.data.length : discard;
            networkIO.getByte(buf.data, 0, len);
            if (discard_mac != null) {
                discard_mac.update(buf.data, 0, len);
            }
            discard -= len;
        }

        if (discard_mac != null) {
            discard_mac.doFinal(buf.data, 0);
        }
        throw new JSchException(JSchException.CODE_ERROR_PROCESS, "Packet corrupt");
    }

    byte[] getSessionId() {
        return session_id;
    }

    private void receiveNewKeys(KeyExchange kex) throws Exception {
        updateKeys(kex);
        in_kex = false;
    }

    private void updateKeys(KeyExchange kex) throws Exception {
        byte[] K = kex.getK();
        byte[] H = kex.getH();
        HASH hash = kex.getHash();

        if (session_id == null) {
            session_id = new byte[H.length];
            System.arraycopy(H, 0, session_id, 0, H.length);
        }

    /*
      Initial IV client to server:     HASH (K || H || "A" || session_id)
      Initial IV server to client:     HASH (K || H || "B" || session_id)
      Encryption key client to server: HASH (K || H || "C" || session_id)
      Encryption key server to client: HASH (K || H || "D" || session_id)
      Integrity key client to server:  HASH (K || H || "E" || session_id)
      Integrity key server to client:  HASH (K || H || "F" || session_id)
    */

        buffer.reset();
        buffer.putMPInt(K);
        buffer.putByte(H);
        buffer.putByte((byte) 0x41);
        buffer.putByte(session_id);
        hash.update(buffer.data, 0, buffer.indexWrite);
        IVc2s = hash.digest();

        int j = buffer.indexWrite - session_id.length - 1;

        buffer.data[j]++;
        hash.update(buffer.data, 0, buffer.indexWrite);
        IVs2c = hash.digest();

        buffer.data[j]++;
        hash.update(buffer.data, 0, buffer.indexWrite);
        Ec2s = hash.digest();

        buffer.data[j]++;
        hash.update(buffer.data, 0, buffer.indexWrite);
        Es2c = hash.digest();

        buffer.data[j]++;
        hash.update(buffer.data, 0, buffer.indexWrite);
        MACc2s = hash.digest();

        buffer.data[j]++;
        hash.update(buffer.data, 0, buffer.indexWrite);
        MACs2c = hash.digest();


        String method;
        method = guess[KeyExchange.PROPOSAL_ENC_ALGS_STOC];
        Class c = Class.forName(getConfig(method));
        cipherStoC = (JCipher) (c.newInstance());
        while (cipherStoC.getBlockSize() > Es2c.length) {
            buffer.reset();
            buffer.putMPInt(K);
            buffer.putByte(H);
            buffer.putByte(Es2c);
            hash.update(buffer.data, 0, buffer.indexWrite);
            byte[] foo = hash.digest();
            byte[] bar = new byte[Es2c.length + foo.length];
            System.arraycopy(Es2c, 0, bar, 0, Es2c.length);
            System.arraycopy(foo, 0, bar, Es2c.length, foo.length);
            Es2c = bar;
        }
        cipherStoC.init(Cipher.DECRYPT_MODE, Es2c, IVs2c);
        s2cCipherSize = cipherStoC.getIVSize();

        method = guess[KeyExchange.PROPOSAL_MAC_ALGS_STOC];
        c = Class.forName(getConfig(method));
        macStoC = (MAC) (c.newInstance());
        MACs2c = expandKey(buffer, K, H, MACs2c, hash, macStoC.getBlockSize());
        macStoC.init(MACs2c);
        //mac_buf=new byte[s2cmac.getBlockSize()];
        s2cmac_result1 = new byte[macStoC.getBlockSize()];
        s2cmac_result2 = new byte[macStoC.getBlockSize()];

        method = guess[KeyExchange.PROPOSAL_ENC_ALGS_CTOS];
        c = Class.forName(getConfig(method));
        CipherCtoS = (JCipher) (c.newInstance());
        while (CipherCtoS.getBlockSize() > Ec2s.length) {
            buffer.reset();
            buffer.putMPInt(K);
            buffer.putByte(H);
            buffer.putByte(Ec2s);
            hash.update(buffer.data, 0, buffer.indexWrite);
            byte[] foo = hash.digest();
            byte[] bar = new byte[Ec2s.length + foo.length];
            System.arraycopy(Ec2s, 0, bar, 0, Ec2s.length);
            System.arraycopy(foo, 0, bar, Ec2s.length, foo.length);
            Ec2s = bar;
        }
        CipherCtoS.init(Cipher.ENCRYPT_MODE, Ec2s, IVc2s);
        c2sCipherSize = CipherCtoS.getIVSize();

        method = guess[KeyExchange.PROPOSAL_MAC_ALGS_CTOS];
        c = Class.forName(getConfig(method));
        macCtoS = (MAC) (c.newInstance());
        MACc2s = expandKey(buffer, K, H, MACc2s, hash, macCtoS.getBlockSize());
        macCtoS.init(MACc2s);

        method = guess[KeyExchange.PROPOSAL_COMP_ALGS_CTOS];
        initDeflater(method);

        method = guess[KeyExchange.PROPOSAL_COMP_ALGS_STOC];
        initInflater(method);
    }


    /*
     * RFC 4253  7.2. Output from Key Exchange
     * If the key length needed is longer than the output of the HASH, the
     * key is extended by computing HASH of the concatenation of K and H and
     * the entire key so far, and appending the resulting bytes (as many as
     * HASH generates) to the key.  This process is repeated until enough
     * key material is available; the key is taken from the beginning of
     * this value.  In other words:
     *   K1 = HASH(K || H || X || session_id)   (X is e.g., "A")
     *   K2 = HASH(K || H || K1)
     *   K3 = HASH(K || H || K1 || K2)
     *   ...
     *   key = K1 || K2 || K3 || ...
     */
    private byte[] expandKey(Buffer buf, byte[] K, byte[] H, byte[] key,
                             HASH hash, int required_length) throws Exception {
        byte[] result = key;
        int size = hash.getBlockSize();
        while (result.length < required_length) {
            buf.reset();
            buf.putMPInt(K);
            buf.putByte(H);
            buf.putByte(result);
            hash.update(buf.data, 0, buf.indexWrite);
            byte[] tmp = new byte[result.length + size];
            System.arraycopy(result, 0, tmp, 0, result.length);
            System.arraycopy(hash.digest(), 0, tmp, result.length, size);
            Util.clearZero(result);
            result = tmp;
        }
        return result;
    }

    /*public*/ /*synchronized*/ void write(Packet packet, Channel c, int length) throws Exception {
        long t = getTimeout();
        while (true) {
            if (in_kex) {
                if (t > 0L && (System.currentTimeMillis() - kex_start_time) > t) {
                    throw new IOException("timeout inputStream waiting for rekeying process.");
                }
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
                continue;
            }
            synchronized (c) {
                if (c.remoteWindowSize < length) {
                    try {
                        c.notifyMe++;
                        c.wait(100);
                    } catch (InterruptedException ignored) {
                    } finally {
                        c.notifyMe--;
                    }
                }

                if (in_kex) {
                    continue;
                }

                if (c.remoteWindowSize >= length) {
                    c.remoteWindowSize -= length;
                    break;
                }
            }
            if (c.close || !c.isConnected()) {
                throw new IOException("channel is broken");
            }

            boolean sendit = false;
            int s = 0;
            byte command = 0;
            int recipient = -1;
            synchronized (c) {
                if (c.remoteWindowSize > 0) {
                    long len = c.remoteWindowSize;
                    if (len > length) {
                        len = length;
                    }
                    if (len != length) {
                        s = packet.shift((int) len,
                                (CipherCtoS != null ? c2sCipherSize : 8),
                                (macCtoS != null ? macCtoS.getBlockSize() : 0));
                    }
                    command = packet.buffer.getCommand();
                    recipient = c.getRecipientId();
                    length -= len;
                    c.remoteWindowSize -= len;
                    sendit = true;
                }
            }
            if (sendit) {
                _write(packet);
                if (length == 0) {
                    return;
                }
                packet.unshift(command, recipient, s, length);
            }

            synchronized (c) {
                if (in_kex) {
                    continue;
                }
                if (c.remoteWindowSize >= length) {
                    c.remoteWindowSize -= length;
                    break;
                }

                //try{
                //System.outputStream.println("1wait: "+c.remoteWindowSize);
                //  c.notifyme++;
                //  c.wait(100);
                //}
                //catch(java.lang.InterruptedException e){
                //}
                //finally{
                //  c.notifyme--;
                //}
            }
        }
        _write(packet);
    }

    public void write(Packet packet) throws Exception {
        long t = getTimeout();
        while (in_kex) {
            if (t > 0L &&
                    (System.currentTimeMillis() - kex_start_time) > t &&
                    !in_prompt) {
                throw new JSchException(JSchException.CODE_ERROR_PROCESS, "timeout inputStream waiting for rekeying process.");
            }
            final byte command = packet.buffer.getCommand();
            //System.err.println("command: "+command);
            if (command == SSH_MSG_KEXINIT ||
                    command == SSH_MSG_NEWKEYS ||
                    command == SSH_MSG_KEXDH_INIT ||
                    command == SSH_MSG_KEXDH_REPLY ||
                    command == SSH_MSG_KEX_DH_GEX_GROUP ||
                    command == SSH_MSG_KEX_DH_GEX_INIT ||
                    command == SSH_MSG_KEX_DH_GEX_REPLY ||
                    command == SSH_MSG_KEX_DH_GEX_REQUEST ||
                    command == SSH_MSG_DISCONNECT) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        }
        _write(packet);
    }

    private void _write(Packet packet) throws Exception {
        synchronized (lock) {
            encode(packet);
            if (networkIO != null) {
                networkIO.send(packet);
                seqo++;
            }
        }
    }

    private Runnable thread;

    public void run() {
        thread = this;

        byte[] foo;
        final Buffer buffer = new Buffer();
        final Packet packet = new Packet(buffer);
        int i = 0;
        Channel channel;
        int[] start = new int[1];
        int[] length = new int[1];
        KeyExchange kex = null;

        int stimeout = 0;
        try {
            while (isConnected && thread != null) {
                try {
                    read(buffer);
                    stimeout = 0;
                } catch (InterruptedIOException/*SocketTimeoutException*/ ee) {
                    if (!in_kex && stimeout < serverAliveCountMax) {
                        sendKeepAliveMsg();
                        stimeout++;
                        continue;
                    } else if (in_kex && stimeout < serverAliveCountMax) {
                        stimeout++;
                        continue;
                    }
                    throw ee;
                }

                final int msgType = buffer.getCommand() & 0xff;
//                System.out.println("msgType=" + msgType);
                if (kex != null && kex.getState() == msgType) {
                    kex_start_time = System.currentTimeMillis();
                    boolean result = kex.next(buffer);
                    if (!result) {
                        throw new JSchException(JSchException.CODE_ERROR_PROCESS, "verify:false");
                    }
                    continue;
                }


                switch (msgType) {
                    case SSH_MSG_KEXINIT:
                        kex = readKexInit(buffer);
                        break;

                    case SSH_MSG_NEWKEYS:
                        send_newkeys();
                        receiveNewKeys(kex);
                        kex = null;
                        break;

                    case Channel.SSH_MSG_CHANNEL_DATA:
                        buffer.readInt();
                        buffer.readByte();
                        buffer.readByte();
                        i = buffer.readInt();
                        channel = Channel.getChannel(i, this);
                        foo = buffer.getStringByte(start, length);
                        if (channel == null) {
                            break;
                        }

                        if (length[0] == 0) {
                            break;
                        }

                        try {
                            channel.write(foo, start[0], length[0]);
                        } catch (Exception e) {
                            channel.disconnect();
                            break;
                        }

                        int len = length[0];
                        channel.setLocalWindowSize(channel.localWindowSize - len);
                        if (channel.localWindowSize < channel.localMaxWindowSize / 2) {
                            packet.reset();
                            buffer.putByte((byte) Channel.SSH_MSG_CHANNEL_WINDOW_ADJUST);
                            buffer.putInt(channel.getRecipientId());
                            buffer.putInt(channel.localMaxWindowSize - channel.localWindowSize);
                            if (!channel.close) write(packet);
                            channel.setLocalWindowSize(channel.localMaxWindowSize);
                        }
                        break;

                    case Channel.SSH_MSG_CHANNEL_EXTENDED_DATA:
                        buffer.readInt();
                        buffer.readShort();
                        i = buffer.readInt();
                        channel = Channel.getChannel(i, this);
                        buffer.readInt();// data_type_code == 1
                        foo = buffer.getStringByte(start, length);
                        //System.err.println("stderr: "+new String(foo,start[0],length[0]));
                        if (channel == null) {
                            break;
                        }

                        if (length[0] == 0) {
                            break;
                        }

                        channel.write_ext(foo, start[0], length[0]);

                        len = length[0];
                        channel.setLocalWindowSize(channel.localWindowSize - len);
                        if (channel.localWindowSize < channel.localMaxWindowSize / 2) {
                            packet.reset();
                            buffer.putByte((byte) Channel.SSH_MSG_CHANNEL_WINDOW_ADJUST);
                            buffer.putInt(channel.getRecipientId());
                            buffer.putInt(channel.localMaxWindowSize - channel.localWindowSize);
                            if (!channel.close) write(packet);
                            channel.setLocalWindowSize(channel.localMaxWindowSize);
                        }
                        break;

                    case Channel.SSH_MSG_CHANNEL_WINDOW_ADJUST:
                        buffer.readInt();
                        buffer.readShort();
                        i = buffer.readInt();
                        channel = Channel.getChannel(i, this);
                        if (channel == null) {
                            break;
                        }
                        channel.addRemoteWindowSize(buffer.readUInt());
                        break;
                    case Channel.SSH_MSG_CHANNEL_EOF:
                        buffer.readInt();
                        buffer.readShort();
                        i = buffer.readInt();
                        channel = Channel.getChannel(i, this);
                        if (channel != null) {
                            //channel.eof_remote=true;
                            //channel.eof();
                            channel.eof_remote();
                        }
	  /*
	  packet.reset();
	  buf.putByte((byte)SSH_MSG_CHANNEL_EOF);
	  buf.putInt(channel.getRecipient());
	  write(packet);
	  */
                        break;
                    case Channel.SSH_MSG_CHANNEL_CLOSE:
                        buffer.readInt();
                        buffer.readShort();
                        i = buffer.readInt();
                        channel = Channel.getChannel(i, this);
                        if (channel != null) {
                            channel.disconnect();
                        }
                        break;

                    /**
                     *       byte      SSH_MSG_CHANNEL_OPEN_CONFIRMATION
                     *       uint32    recipient channel
                     *       uint32    sender channel
                     *       uint32    initial window size
                     *       uint32    maximum packet size
                     *       ....      channel type specific data follows
                     */
                    case Channel.SSH_MSG_CHANNEL_OPEN_CONFIRMATION:
                        buffer.readInt();
                        buffer.readShort();
                        i = buffer.readInt();// recipient channel
                        channel = Channel.getChannel(i, this);
                        int recipientChannel = buffer.readInt();// sender channel
                        long rws = buffer.readUInt();
                        int rps = buffer.readInt();
                        if (channel != null) {
                            channel.setRemoteWindowSize(rws);
                            channel.setRemoteMaxPacketSize(rps);
                            channel.open_confirmation = true;
                            channel.setRecipientId(recipientChannel);
                        }
                        break;
                    /**
                     *       byte      SSH_MSG_CHANNEL_OPEN_FAILURE
                     *       uint32    recipient channel
                     *       uint32    reason code
                     *       string    description in ISO-10646 UTF-8 encoding [RFC3629]
                     *       string    language tag [RFC3066]
                     */
                    case Channel.SSH_MSG_CHANNEL_OPEN_FAILURE:
                        buffer.readInt();
                        buffer.readShort();
                        i = buffer.readInt();
                        channel = Channel.getChannel(i, this);
                        if (channel != null) {
                            int reason_code = buffer.readInt();
                            //description=buf.getString();
                            channel.setFailureReasonCode(reason_code);
                            channel.close = true;
                            channel.eof_remote = true;
                            channel.setRecipientId(0);
                        }
                        break;
                    case Channel.SSH_MSG_CHANNEL_REQUEST:
                        buffer.readInt();
                        buffer.readShort();
                        i = buffer.readInt();
                        foo = buffer.getStringByte();
                        boolean reply = (buffer.readByte() != 0);
                        channel = Channel.getChannel(i, this);
                        if (channel != null) {
                            byte reply_type = (byte) Channel.SSH_MSG_CHANNEL_FAILURE;
                            if ((Util.byte2str(foo)).equals("exit-status")) {
                                i = buffer.readInt();             // exit-status
                                channel.setFailureReasonCode(i);
                                reply_type = (byte) Channel.SSH_MSG_CHANNEL_SUCCESS;
                            }
                            if (reply) {
                                packet.reset();
                                buffer.putByte(reply_type);
                                buffer.putInt(channel.getRecipientId());
                                write(packet);
                            }
                        } else {
                        }
                        break;
                    case Channel.SSH_MSG_CHANNEL_OPEN:
                        buffer.readInt();
                        buffer.readShort();
                        foo = buffer.getStringByte();
                        String ctyp = Util.byte2str(foo);
                        if (!"forwarded-tcpip".equals(ctyp) &&
                                !("x11".equals(ctyp) && x11_forwarding) &&
                                !("auth-agent@openssh.com".equals(ctyp) && agent_forwarding)) {
                            //System.err.println("Session.run: CHANNEL OPEN "+ctyp);
                            //throw new IOException("Session.run: CHANNEL OPEN "+ctyp);
                            packet.reset();
                            buffer.putByte((byte) Channel.SSH_MSG_CHANNEL_OPEN_FAILURE);
                            buffer.putInt(buffer.readInt());
                            buffer.putInt(Channel.SSH_OPEN_ADMINISTRATIVELY_PROHIBITED);
                            buffer.putString(Util.EMPTY_BYTE);
                            buffer.putString(Util.EMPTY_BYTE);
                            write(packet);
                        } else {
                            channel = Channel.getChannel(ctyp);
                            addChannel(channel);
                            channel.getData(buffer);
                            channel.init();

                            Thread tmp = new Thread(channel);
                            tmp.setName("Channel " + ctyp + " " + host);
                            if (daemon_thread) {
                                tmp.setDaemon(true);
                            }
                            tmp.start();
                        }
                        break;
                    case Channel.SSH_MSG_CHANNEL_SUCCESS:
                        buffer.readInt();
                        buffer.readShort();
                        i = buffer.readInt();
                        channel = Channel.getChannel(i, this);
                        if (channel == null) {
                            break;
                        }
                        channel.reply = 1;
                        break;
                    case Channel.SSH_MSG_CHANNEL_FAILURE:
                        buffer.readInt();
                        buffer.readShort();
                        i = buffer.readInt();
                        channel = Channel.getChannel(i, this);
                        if (channel == null) {
                            break;
                        }
                        channel.reply = 0;
                        break;
                    case SSH_MSG_GLOBAL_REQUEST:
                        buffer.readInt();
                        buffer.readShort();
                        foo = buffer.getStringByte();       // request name
                        reply = (buffer.readByte() != 0);
                        if (reply) {
                            packet.reset();
                            buffer.putByte((byte) SSH_MSG_REQUEST_FAILURE);
                            write(packet);
                        }
                        break;
                    case SSH_MSG_REQUEST_FAILURE:
                    case SSH_MSG_REQUEST_SUCCESS:
                        Thread t = grr.getThread();
                        if (t != null) {
                            grr.setReply(msgType == SSH_MSG_REQUEST_SUCCESS ? 1 : 0);
                            if (msgType == SSH_MSG_REQUEST_SUCCESS && grr.getPort() == 0) {
                                buffer.readInt();
                                buffer.readShort();
                                grr.setPort(buffer.readInt());
                            }
                            t.interrupt();
                        }
                        break;
                    default:
                        throw new IOException("未知的SSH消息类型：" + msgType);
                }
            }
        } catch (Exception e) {
            in_kex = false;
            SSHClient.log("后台Session连接线程捕捉到异常，程序退出：" + e.getMessage());
            e.printStackTrace();
        }
        disconnect();
        isConnected = false;
//        try {
//
//        } catch (Exception e) {
//            //System.err.println("@2");
//            //e.printStackTrace();
//        }
    }

    public void disconnect() {
        if (!isConnected) return;
        isConnected = false;
        SSHClient.log("从 " + host + ":" + port + "断开");

        Channel.disconnect(this);
        PortWatcher.delPort(this);
        ChannelForwardedTCPIP.delPort(this);
        ChannelX11.removeFakedCookie(this);
        try {
            synchronized (lock) {
                if (connectThread != null) {
                    // Thread.yield();
                    connectThread.interrupt();
                    connectThread = null;
                }
            }
            if (socket != null) socket.close();

            if (proxy != null) {
                synchronized (proxy) {
                    proxy.close();
                }

            }
            if (networkIO != null) {
                if (networkIO.inputStream != null) networkIO.inputStream.close();
                if (networkIO.outputStream != null) networkIO.outputStream.close();
                if (networkIO.out_ext != null) networkIO.out_ext.close();

            }
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();

        } catch (Exception e) {

        } finally {
            connectThread = null;
            thread = null;
            socket = null;
            proxy = null;
            networkIO = null;
            outputStream = null;
            inputStream = null;
            client.removeSession(this);
        }
    }

    /**
     * Registers the local port forwarding for loop-back interface.
     * If <code>lport</code> is <code>0</code>, the tcp port will be allocated.
     *
     * @param lport local port for local port forwarding
     * @param host  host address for local port forwarding
     * @param rport remote port number for local port forwarding
     * @return an allocated local TCP port number
     * @see #setPortForwardingL(String bind_address, int lport, String host, int rport, ServerSocketFactory ssf, int connectTimeout)
     */
    public int setPortForwardingL(int lport, String host, int rport) throws JSchException {
        return setPortForwardingL("127.0.0.1", lport, host, rport);
    }

    /**
     * Registers the local port forwarding.  If <code>bind_address</code> is an empty string
     * or '*', the port should be available from all interfaces.
     * If <code>bind_address</code> is <code>"localhost"</code> or
     * <code>null</code>, the listening port will be bound for local use only.
     * If <code>lport</code> is <code>0</code>, the tcp port will be allocated.
     *
     * @param bind_address bind address for local port forwarding
     * @param lport        local port for local port forwarding
     * @param host         host address for local port forwarding
     * @param rport        remote port number for local port forwarding
     * @return an allocated local TCP port number
     * @see #setPortForwardingL(String bind_address, int lport, String host, int rport, ServerSocketFactory ssf, int connectTimeout)
     */
    public int setPortForwardingL(String bind_address, int lport, String host, int rport) throws JSchException {
        return setPortForwardingL(bind_address, lport, host, rport, null);
    }

    /**
     * Registers the local port forwarding.
     * If <code>bind_address</code> is an empty string or <code>"*"</code>,
     * the port should be available from all interfaces.
     * If <code>bind_address</code> is <code>"localhost"</code> or
     * <code>null</code>, the listening port will be bound for local use only.
     * If <code>lport</code> is <code>0</code>, the tcp port will be allocated.
     *
     * @param bind_address bind address for local port forwarding
     * @param lport        local port for local port forwarding
     * @param host         host address for local port forwarding
     * @param rport        remote port number for local port forwarding
     * @param ssf          socket factory
     * @return an allocated local TCP port number
     * @see #setPortForwardingL(String bind_address, int lport, String host, int rport, ServerSocketFactory ssf, int connectTimeout)
     */
    public int setPortForwardingL(String bind_address, int lport, String host, int rport, ServerSocketFactory ssf) throws JSchException {
        return setPortForwardingL(bind_address, lport, host, rport, ssf, 0);
    }

    /**
     * Registers the local port forwarding.
     * If <code>bind_address</code> is an empty string
     * or <code>"*"</code>, the port should be available from all interfaces.
     * If <code>bind_address</code> is <code>"localhost"</code> or
     * <code>null</code>, the listening port will be bound for local use only.
     * If <code>lport</code> is <code>0</code>, the tcp port will be allocated.
     *
     * @param bind_address   bind address for local port forwarding
     * @param lport          local port for local port forwarding
     * @param host           host address for local port forwarding
     * @param rport          remote port number for local port forwarding
     * @param ssf            socket factory
     * @param connectTimeout timeout for establishing port connection
     * @return an allocated local TCP port number
     */
    public int setPortForwardingL(String bind_address, int lport, String host, int rport, ServerSocketFactory ssf, int connectTimeout) throws JSchException {
        PortWatcher pw = PortWatcher.addPort(this, bind_address, lport, host, rport, ssf);
        pw.setConnectTimeout(connectTimeout);
        Thread tmp = new Thread(pw);
        tmp.setName("PortWatcher Thread for " + host);
        if (daemon_thread) {
            tmp.setDaemon(daemon_thread);
        }
        tmp.start();
        return pw.lport;
    }

    /**
     * Cancels the local port forwarding assigned
     * at local TCP port <code>lport</code> on loopback interface.
     *
     * @param lport local TCP port
     */
    public void delPortForwardingL(int lport) throws JSchException {
        delPortForwardingL("127.0.0.1", lport);
    }

    /**
     * Cancels the local port forwarding assigned
     * at local TCP port <code>lport</code> on <code>bind_address</code> interface.
     *
     * @param bind_address bind_address of network interfaces
     * @param lport        local TCP port
     */
    public void delPortForwardingL(String bind_address, int lport) throws JSchException {
        PortWatcher.delPort(this, bind_address, lport);
    }

    /**
     * Lists the registered local port forwarding.
     *
     * @return a list of "lport:host:hostport"
     */
    public String[] getPortForwardingL() throws JSchException {
        return PortWatcher.getPortForwarding(this);
    }

    /**
     * Registers the remote port forwarding for the loopback interface
     * of the remote.
     *
     * @param rport remote port
     * @param host  host address
     * @param lport local port
     * @see #setPortForwardingR(String bind_address, int rport, String host, int lport, SocketFactory sf)
     */
    public void setPortForwardingR(int rport, String host, int lport) throws JSchException {
        setPortForwardingR(null, rport, host, lport, (SocketFactory) null);
    }

    /**
     * Registers the remote port forwarding.
     * If <code>bind_address</code> is an empty string or <code>"*"</code>,
     * the port should be available from all interfaces.
     * If <code>bind_address</code> is <code>"localhost"</code> or is not given,
     * the listening port will be bound for local use only.
     * Note that if <code>GatewayPorts</code> is <code>"no"</code> on the
     * remote, <code>"localhost"</code> is always used as a bind_address.
     *
     * @param bind_address bind address
     * @param rport        remote port
     * @param host         host address
     * @param lport        local port
     * @see #setPortForwardingR(String bind_address, int rport, String host, int lport, SocketFactory sf)
     */
    public void setPortForwardingR(String bind_address, int rport, String host, int lport) throws JSchException {
        setPortForwardingR(bind_address, rport, host, lport, (SocketFactory) null);
    }

    /**
     * Registers the remote port forwarding for the loopback interface
     * of the remote.
     *
     * @param rport remote port
     * @param host  host address
     * @param lport local port
     * @param sf    socket factory
     * @see #setPortForwardingR(String bind_address, int rport, String host, int lport, SocketFactory sf)
     */
    public void setPortForwardingR(int rport, String host, int lport, SocketFactory sf) throws JSchException {
        setPortForwardingR(null, rport, host, lport, sf);
    }

    // TODO: This method should return the integer value as the assigned port.

    /**
     * Registers the remote port forwarding.
     * If <code>bind_address</code> is an empty string or <code>"*"</code>,
     * the port should be available from all interfaces.
     * If <code>bind_address</code> is <code>"localhost"</code> or is not given,
     * the listening port will be bound for local use only.
     * Note that if <code>GatewayPorts</code> is <code>"no"</code> on the
     * remote, <code>"localhost"</code> is always used as a bind_address.
     * If <code>rport</code> is <code>0</code>, the TCP port will be allocated on the remote.
     *
     * @param bind_address bind address
     * @param rport        remote port
     * @param host         host address
     * @param lport        local port
     * @param sf           socket factory
     */
    public void setPortForwardingR(String bind_address, int rport, String host, int lport, SocketFactory sf) throws JSchException {
        int allocated = _setPortForwardingR(bind_address, rport);
        ChannelForwardedTCPIP.addPort(this, bind_address,
                rport, allocated, host, lport, sf);
    }

    /**
     * Registers the remote port forwarding for the loopback interface
     * of the remote.
     * The TCP connection to <code>rport</code> on the remote will be
     * forwarded to an instance of the class <code>daemon</code>.
     * The class specified by <code>daemon</code> must implement
     * <code>ForwardedTCPIPDaemon</code>.
     *
     * @param rport  remote port
     * @param daemon class name, which implements "ForwardedTCPIPDaemon"
     * @see #setPortForwardingR(String bind_address, int rport, String daemon, Object[] arg)
     */
    public void setPortForwardingR(int rport, String daemon) throws JSchException {
        setPortForwardingR(null, rport, daemon, null);
    }

    /**
     * Registers the remote port forwarding for the loopback interface
     * of the remote.
     * The TCP connection to <code>rport</code> on the remote will be
     * forwarded to an instance of the class <code>daemon</code> with
     * the argument <code>arg</code>.
     * The class specified by <code>daemon</code> must implement <code>ForwardedTCPIPDaemon</code>.
     *
     * @param rport  remote port
     * @param daemon class name, which implements "ForwardedTCPIPDaemon"
     * @param arg    arguments for "daemon"
     * @see #setPortForwardingR(String bind_address, int rport, String daemon, Object[] arg)
     */
    public void setPortForwardingR(int rport, String daemon, Object[] arg) throws JSchException {
        setPortForwardingR(null, rport, daemon, arg);
    }

    /**
     * Registers the remote port forwarding.
     * If <code>bind_address</code> is an empty string
     * or <code>"*"</code>, the port should be available from all interfaces.
     * If <code>bind_address</code> is <code>"localhost"</code> or is not given,
     * the listening port will be bound for local use only.
     * Note that if <code>GatewayPorts</code> is <code>"no"</code> on the
     * remote, <code>"localhost"</code> is always used as a bind_address.
     * The TCP connection to <code>rport</code> on the remote will be
     * forwarded to an instance of the class <code>daemon</code> with the
     * argument <code>arg</code>.
     * The class specified by <code>daemon</code> must implement <code>ForwardedTCPIPDaemon</code>.
     *
     * @param bind_address bind address
     * @param rport        remote port
     * @param daemon       class name, which implements "ForwardedTCPIPDaemon"
     * @param arg          arguments for "daemon"
     * @see #setPortForwardingR(String bind_address, int rport, String daemon, Object[] arg)
     */
    public void setPortForwardingR(String bind_address, int rport, String daemon, Object[] arg) throws JSchException {
        int allocated = _setPortForwardingR(bind_address, rport);
        ChannelForwardedTCPIP.addPort(this, bind_address,
                rport, allocated, daemon, arg);
    }

    /**
     * Lists the registered remote port forwarding.
     *
     * @return a list of "rport:host:hostport"
     */
    public String[] getPortForwardingR() throws JSchException {
        return ChannelForwardedTCPIP.getPortForwarding(this);
    }

    public SSHConnectConfig getConnectConfig() {
        return connectConfig;
    }

    private class Forwarding {
        String bind_address = null;
        int port = -1;
        String host = null;
        int hostport = -1;
    }

    /**
     * The given argument may be "[bind_address:]port:host:hostport" or
     * "[bind_address:]port host:hostport", which is from LocalForward command of
     * ~/.ssh/config .
     */
    private Forwarding parseForwarding(String conf) throws JSchException {
        String[] tmp = conf.split(" ");
        if (tmp.length > 1) {   // "[bind_address:]port host:hostport"
            Vector foo = new Vector();
            for (int i = 0; i < tmp.length; i++) {
                if (tmp[i].length() == 0) continue;
                foo.addElement(tmp[i].trim());
            }
            StringBuffer sb = new StringBuffer(); // join
            for (int i = 0; i < foo.size(); i++) {
                sb.append((String) (foo.elementAt(i)));
                if (i + 1 < foo.size())
                    sb.append(":");
            }
            conf = sb.toString();
        }

        String org = conf;
        Forwarding f = new Forwarding();
        try {
            if (conf.lastIndexOf(":") == -1)
                throw new JSchException(JSchException.CODE_ERROR_PROCESS, "parseForwarding: " + org);
            f.hostport = Integer.parseInt(conf.substring(conf.lastIndexOf(":") + 1));
            conf = conf.substring(0, conf.lastIndexOf(":"));
            if (conf.lastIndexOf(":") == -1)
                throw new JSchException(JSchException.CODE_ERROR_PROCESS, "parseForwarding: " + org);
            f.host = conf.substring(conf.lastIndexOf(":") + 1);
            conf = conf.substring(0, conf.lastIndexOf(":"));
            if (conf.lastIndexOf(":") != -1) {
                f.port = Integer.parseInt(conf.substring(conf.lastIndexOf(":") + 1));
                conf = conf.substring(0, conf.lastIndexOf(":"));
                if (conf.length() == 0 || conf.equals("*")) conf = "0.0.0.0";
                if (conf.equals("localhost")) conf = "127.0.0.1";
                f.bind_address = conf;
            } else {
                f.port = Integer.parseInt(conf);
                f.bind_address = "127.0.0.1";
            }
        } catch (NumberFormatException e) {
            throw new JSchException(JSchException.CODE_ERROR_PROCESS, "parseForwarding: " + e.toString());
        }
        return f;
    }

    /**
     * Registers the local port forwarding.  The argument should be
     * inputStream the format like "[bind_address:]port:host:hostport".
     * If <code>bind_address</code> is an empty string or <code>"*"</code>,
     * the port should be available from all interfaces.
     * If <code>bind_address</code> is <code>"localhost"</code> or is not given,
     * the listening port will be bound for local use only.
     *
     * @param conf configuration of local port forwarding
     * @return an assigned port number
     * @see #setPortForwardingL(String bind_address, int lport, String host, int rport)
     */
    public int setPortForwardingL(String conf) throws JSchException {
        Forwarding f = parseForwarding(conf);
        return setPortForwardingL(f.bind_address, f.port, f.host, f.hostport);
    }

    /**
     * Registers the remote port forwarding.  The argument should be
     * inputStream the format like "[bind_address:]port:host:hostport".  If the
     * bind_address is not given, the default is to only bind to loopback
     * addresses.  If the bind_address is <code>"*"</code> or an empty string,
     * then the forwarding is requested to listen on all interfaces.
     * Note that if <code>GatewayPorts</code> is <code>"no"</code> on the remote,
     * <code>"localhost"</code> is always used for bind_address.
     * If the specified remote is <code>"0"</code>,
     * the TCP port will be allocated on the remote.
     *
     * @param conf configuration of remote port forwarding
     * @return an allocated TCP port on the remote.
     * @see #setPortForwardingR(String bind_address, int rport, String host, int rport)
     */
    public int setPortForwardingR(String conf) throws JSchException {
        Forwarding f = parseForwarding(conf);
        int allocated = _setPortForwardingR(f.bind_address, f.port);
        ChannelForwardedTCPIP.addPort(this, f.bind_address,
                f.port, allocated, f.host, f.hostport, null);
        return allocated;
    }

    /**
     * Instantiates an instance of stream-forwarder to <code>host</code>:<code>port</code>.
     * Set I/O stream to the given channel, and then invoke Channel#connect() method.
     *
     * @param host remote host, which the given stream will be plugged to.
     * @param port remote port, which the given stream will be plugged to.
     */
    public Channel getStreamForwarder(String host, int port) throws JSchException {
        ChannelDirectTCPIP channel = new ChannelDirectTCPIP();
        channel.init();
        this.addChannel(channel);
        channel.setHost(host);
        channel.setPort(port);
        return channel;
    }

    private class GlobalRequestReply {
        private Thread thread = null;
        private int reply = -1;
        private int port = 0;

        void setThread(Thread thread) {
            this.thread = thread;
            this.reply = -1;
        }

        Thread getThread() {
            return thread;
        }

        void setReply(int reply) {
            this.reply = reply;
        }

        int getReply() {
            return this.reply;
        }

        int getPort() {
            return this.port;
        }

        void setPort(int port) {
            this.port = port;
        }
    }

    private GlobalRequestReply grr = new GlobalRequestReply();

    private int _setPortForwardingR(String bind_address, int rport) throws JSchException {
        synchronized (grr) {
            Buffer buf = new Buffer(100); // ??
            Packet packet = new Packet(buf);

            String address_to_bind = ChannelForwardedTCPIP.normalize(bind_address);

            grr.setThread(Thread.currentThread());
            grr.setPort(rport);

            try {
                // byte SSH_MSG_GLOBAL_REQUEST 80
                // string "tcpip-forward"
                // boolean want_reply
                // string  address_to_bind
                // uint32  port number to bind
                packet.reset();
                buf.putByte((byte) SSH_MSG_GLOBAL_REQUEST);
                buf.putString(Util.str2byte("tcpip-forward"));
                buf.putByte((byte) 1);
                buf.putString(Util.str2byte(address_to_bind));
                buf.putInt(rport);
                write(packet);
            } catch (Exception e) {
                grr.setThread(null);
                throw new JSchException(e);
            }

            int count = 0;
            int reply = grr.getReply();
            while (count < 10 && reply == -1) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                count++;
                reply = grr.getReply();
            }
            grr.setThread(null);
            if (reply != 1) {
                throw new JSchException(JSchException.CODE_ERROR_PROCESS, "remote port forwarding failed for listen port " + rport);
            }
            rport = grr.getPort();
        }
        return rport;
    }

    /**
     * Cancels the remote port forwarding assigned at remote TCP port <code>rport</code>.
     *
     * @param rport remote TCP port
     */
    public void delPortForwardingR(int rport) throws JSchException {
        this.delPortForwardingR(null, rport);
    }

    /**
     * Cancels the remote port forwarding assigned at
     * remote TCP port <code>rport</code> bound on the interface at
     * <code>bind_address</code>.
     *
     * @param bind_address bind address of the interface on the remote
     * @param rport        remote TCP port
     */
    public void delPortForwardingR(String bind_address, int rport) throws JSchException {
        ChannelForwardedTCPIP.delPort(this, bind_address, rport);
    }

    private void initDeflater(String method) throws JSchException {
        if (method.equals("none")) {
            deflater = null;
            return;
        }
        String foo = getConfig(method);
        if (foo != null) {
            if (method.equals("zlib") || (isAuthed && method.equals("zlib@openssh.com"))) {
                try {
                    Class c = Class.forName(foo);
                    deflater = (Compression) (c.newInstance());
                    int level = 6;
                    try {
                        level = Integer.parseInt(getConfig("compression_level"));
                    } catch (Exception ignored) {
                    }
                    deflater.init(Compression.DEFLATER, level);
                } catch (Exception e) {
                    throw new JSchException(JSchException.CODE_ERROR_CONFIG, "compression_level", e);
                }
//                catch (NoClassDefFoundError ee) {
//                    throw new JSchException(ee.toString(), ee);
//                } catch (Exception ee) {
//                    throw new JSchException(ee.toString(), ee);
//                    //System.err.println(foo+" isn't accessible.");
//                }
            }
        }
    }

    private void initInflater(String method) throws JSchException {
        if (method.equals("none")) {
            inflater = null;
            return;
        }
        String foo = getConfig(method);
        if (foo != null) {
            if (method.equals("zlib") ||
                    (isAuthed && method.equals("zlib@openssh.com"))) {
                try {
                    Class c = Class.forName(foo);
                    inflater = (Compression) (c.newInstance());
                    inflater.init(Compression.INFLATER, 0);
                } catch (Exception ee) {
                    throw new JSchException(JSchException.CODE_ERROR_CONFIG, ee);
                    //System.err.println(foo+" isn't accessible.");
                }
            }
        }
    }

    void addChannel(Channel channel) {
        channel.setSession(this);
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }


    public void setUserInfo(UserInfo userinfo) {
        this.userinfo = userinfo;
    }

    public UserInfo getUserInfo() {
        return this.userinfo;
    }


//    public void setInputStream(InputStream in) {
//        this.in = in;
//    }

//    public void setOutputStream(OutputStream out) {
//        this.outputStream = out;
//    }

    public void setX11Host(String host) {
        ChannelX11.setHost(host);
    }

    public void setX11Port(int port) {
        ChannelX11.setPort(port);
    }

    public void setX11Cookie(String cookie) {
        ChannelX11.setCookie(cookie);
    }

    public void setPassword(String password) {
        this.password = password;
        this.passwordByte = Util.str2byte(password);
    }

//    public void setPassword(byte[] password) {
//        if (password != null) {
//            this.password = new byte[password.length];
//            System.arraycopy(password, 0, this.password, 0, password.length);
//        }
//    }

//    public void setConfig(java.util.Properties properties) {
//        setConfig((java.util.Hashtable) properties);
//    }

    public void setConfig(java.util.Hashtable hashTable) {
        synchronized (lock) {
            if (config == null) {
                config = new java.util.Hashtable<String, String>();
            }
            for (java.util.Enumeration e = hashTable.keys(); e.hasMoreElements(); ) {
                String key = (String) (e.nextElement());
                config.put(key, (String) (hashTable.get(key)));
            }
        }
    }

    public void setConfig(String key, String value) {
        synchronized (lock) {
            if (config == null) {
                config = new java.util.Hashtable<>();
            }
            config.put(key, value);
        }
    }

    public String getConfig(String key) {
        String value = null;
        if (config != null) {
            value = config.get(key);
        }
        if (value == null) {
            value = SSHClient.getConfig(key);
        }
        return value;
    }

    public void setSocketFactory(SocketFactory factory) {
        socketFactory = factory;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) throws SocketException {
        if (socket == null) {
            if (timeout < 0) {
                timeout = Session.DEFAULT_TIMEOUT;
            }
            this.timeout = timeout;
            return;
        }
        socket.setSoTimeout(timeout);
        this.timeout = timeout;
    }

    public String getServerVersion() {
        return serveSSHVersion;
    }

    public String getClientVersion() {
        return Util.byte2str(CLIENT_VERSION);
    }

    public void sendIgnore() throws Exception {
        Buffer buf = new Buffer();
        Packet packet = new Packet(buf);
        packet.reset();
        buf.putByte((byte) SSH_MSG_IGNORE);
        write(packet);
    }

    private static final byte[] keepalivemsg = Util.str2byte("keepalive@jcraft.com");

    public void sendKeepAliveMsg() throws Exception {
        Buffer buf = new Buffer();
        Packet packet = new Packet(buf);
        packet.reset();
        buf.putByte((byte) SSH_MSG_GLOBAL_REQUEST);
        buf.putString(keepalivemsg);
        buf.putByte((byte) 1);
        write(packet);
    }

    private static final byte[] nomoresessions = Util.str2byte("no-more-sessions@openssh.com");

    public void noMoreSessionChannels() throws Exception {
        Buffer buf = new Buffer();
        Packet packet = new Packet(buf);
        packet.reset();
        buf.putByte((byte) SSH_MSG_GLOBAL_REQUEST);
        buf.putString(nomoresessions);
        buf.putByte((byte) 0);
        write(packet);
    }

    private HostKey hostkey = null;

    public HostKey getHostKey() {
        return hostkey;
    }

    public String getHost() {
        return host;
    }

    public String getUserName() {
        return username;
    }

    public int getPort() {
        return port;
    }

    public void setHostKeyAlias(String hostKeyAlias) {
        this.hostKeyAlias = hostKeyAlias;
    }

    public String getHostKeyAlias() {
        return hostKeyAlias;
    }

    /**
     * Sets the interval to send a keep-alive message.  If zero is
     * specified, any keep-alive message must not be sent.  The default interval
     * is zero.
     *
     * @param interval the specified interval, inputStream milliseconds.
     * @see #getServerAliveInterval()
     */
    public void setServerAliveInterval(int interval) throws SocketException {
        setTimeout(interval);
        this.serverAliveInterval = interval;
    }

    /**
     * Returns setting for the interval to send a keep-alive message.
     *
     * @see #setServerAliveInterval(int)
     */
    public int getServerAliveInterval() {
        return this.serverAliveInterval;
    }

    /**
     * Sets the number of keep-alive messages which may be sent without
     * receiving any messages back from the server.  If this threshold is
     * reached while keep-alive messages are being sent, the connection will
     * be disconnected.  The default value is one.
     *
     * @param count the specified count
     * @see #getServerAliveCountMax()
     */
    public void setServerAliveCountMax(int count) {
        this.serverAliveCountMax = count;
    }

    /**
     * Returns setting for the threshold to send keep-alive messages.
     *
     * @see #setServerAliveCountMax(int)
     */
    public int getServerAliveCountMax() {
        return this.serverAliveCountMax;
    }

    public void setDaemonThread(boolean enable) {
        this.daemon_thread = enable;
    }

    public static boolean checkCipher(String cipherName) {
        try {
            Class c = Class.forName(cipherName);
            JCipher cipher = (JCipher) (c.newInstance());
            cipher.init(Cipher.ENCRYPT_MODE,
                    new byte[cipher.getBlockSize()],
                    new byte[cipher.getIVSize()]);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public String checkAvailableCiphers() {
        String cipherc2s = getConfig("cipher.c2s");
        String[] ciphers = Util.split(cipherc2s, ",");
        StringBuilder builder = null;
        for (int i = 0; i < ciphers.length; i++) {
            String cipherName = ciphers[i];
            if (Session.checkCipher(getConfig(cipherName))) {
                if (builder == null) {
                    builder = new StringBuilder(cipherc2s.length());
                    builder.append(cipherName);
                } else {
                    builder.append(',');
                    builder.append(cipherName);
                }
            }
        }
        if (builder == null || builder.length() == 0) {
            return null;
        } else {
            return builder.toString();
        }
    }


    public boolean checkKey(String came) {
        try {
            Class c = Class.forName(came);
            KeyExchange keyExchange = (KeyExchange) (c.newInstance());
            keyExchange.init(this, null, null, null, null);
            return true;
        } catch (Exception ignored) {
            ignored.printStackTrace();
            return false;
        }
    }

    // 检测是否有可用的算法
    public String checkAvailableKexes() {
        String kexes = getConfig("kex_algorithms");
        // String kexes = getConfig("NeedCheckKexes");
        String[] _kexes = Util.split(kexes, ",");
        StringBuilder builder = null;
        for (int i = 0; i < _kexes.length; i++) {
            String algorithm = _kexes[i];
            if (checkKey(getConfig(algorithm))) {
                if (builder == null) {
                    builder = new StringBuilder(kexes.length());
                    builder.append(algorithm);
                } else {
                    builder.append(',');
                    builder.append(algorithm);
                }
            }
        }
        if (builder == null || builder.length() == 0) {
            return null;
        } else {
            return builder.toString();
        }
    }


    public static boolean checkSignature(String cname) {
        try {
            final Class c = Class.forName(cname);
            final Signature sig = (Signature) (c.newInstance());
            sig.init();
            return true;
        } catch (Exception ignored) {
            ignored.printStackTrace();
            return false;
        }
    }

    public String checkAvailableSignatures() {
        String server_host_key_algorithms = getConfig("server_host_key_algorithms");
        final String[] algorithms = Util.split(server_host_key_algorithms, ",");
        if (algorithms.length < 3) {
            return null;
        }
        StringBuilder builder = new StringBuilder(server_host_key_algorithms.length());
        builder.append(algorithms[0]).append(',').append(algorithms[1]);
        for (int i = 2; i < algorithms.length; i++) {
            String name = algorithms[i];
            if (Session.checkSignature(getConfig(name))) {
                builder.append(',').append(name);
            }
        }
        return builder.toString();
    }

    /**
     * Sets the identityRepository, which will be referred
     * inputStream the public key authentication.  The default value is <code>null</code>.
     *
     * @param identityRepository
     * @see #getIdentityRepository()
     */
    public void setIdentityRepository(IdentityRepository identityRepository) {
        this.identityRepository = identityRepository;
    }

    /**
     * Gets the identityRepository.
     * If this.identityRepository is <code>null</code>,
     * JSch#getIdentityRepository() will be invoked.
     *
     * @see SSHClient#getIdentityRepository()
     */
    IdentityRepository getIdentityRepository() {
        if (identityRepository == null)
            return client.getIdentityRepository();
        return identityRepository;
    }

    /**
     * Sets the hostkeyRepository, which will be referred inputStream checking host keys.
     *
     * @param hostkeyRepository
     * @see #getHostKeyRepository()
     */
    public void setHostKeyRepository(HostKeyRepository hostkeyRepository) {
        this.hostkeyRepository = hostkeyRepository;
    }

    /**
     * Gets the hostkeyRepository.
     * If this.hostkeyRepository is <code>null</code>,
     * JSch#getHostKeyRepository() will be invoked.
     *
     * @see SSHClient#getHostKeyRepository()
     */
    public HostKeyRepository getHostKeyRepository() {
        if (hostkeyRepository == null)
            return client.getHostKeyRepository();
        return hostkeyRepository;
    }

  /*
  // setProxyCommand("ssh -l user2 host2 -o 'ProxyCommand ssh user1@host1 nc host2 22' nc %h %p")
  public void setProxyCommand(String command){
    setProxy(new ProxyCommand(command));
  }

  class ProxyCommand implements Proxy {
    String command;
    Process p = null;
    InputStream inputStream = null;
    OutputStream outputStream = null;
    ProxyCommand(String command){
      this.command = command;
    }
    public void connect(SocketFactory socket_factory, String host, int port, int timeout) throws Exception {
      String _command = command.replace("%h", host);
      _command = _command.replace("%p", new Integer(port).toString());
      p = Runtime.getRuntime().exec(_command);
      inputStream = p.getInputStream();
      outputStream = p.getOutputStream();
    }
    public Socket getSocket() { return null; }
    public InputStream getInputStream() { return inputStream; }
    public OutputStream getOutputStream() { return outputStream; }
    public void close() {
      try{
        if(p!=null){
          p.getErrorStream().close();
          p.getOutputStream().close();
          p.getInputStream().close();
          p.destroy();
          p=null;
        }
      }
      catch(IOException e){
      }
    }
  }
  */

    private void applyConfig() throws JSchException {
        ConfigRepository configRepository = client.getConfigRepository();
        if (configRepository == null) {
            return;
        }

        ConfigRepository.Config config = configRepository.getConfig(org_host);

        String value = null;

        if (username == null) {
            value = config.getUser();
            if (value != null)
                username = value;
        }

        value = config.getHostname();
        if (value != null)
            host = value;

        int port = config.getPort();
        if (port != -1)
            this.port = port;

        checkConfig(config, "kex_algorithms");
        checkConfig(config, "server_host_key_algorithms");

        checkConfig(config, "cipher.c2s");
        checkConfig(config, "cipher.s2c");
        checkConfig(config, "mac.c2s");
        checkConfig(config, "mac.s2c");
        checkConfig(config, "compression.c2s");
        checkConfig(config, "compression.s2c");
        checkConfig(config, "compression_level");

        checkConfig(config, "StrictHostKeyChecking");
        checkConfig(config, "HashKnownHosts");
        checkConfig(config, "PreferredAuthentications");
        checkConfig(config, "MaxAuthTries");
        checkConfig(config, "ClearAllForwardings");

        value = config.getValue("HostKeyAlias");
        if (value != null)
            this.setHostKeyAlias(value);

        value = config.getValue("UserKnownHostsFile");
        if (value != null) {
            KnownHosts kh = new KnownHosts(client);
            kh.setKnownHosts(value);
            this.setHostKeyRepository(kh);
        }

        String[] values = config.getValues("IdentityFile");
        if (values != null) {
            String[] global =
                    configRepository.getConfig("").getValues("IdentityFile");
            if (global != null) {
                for (int i = 0; i < global.length; i++) {
                    client.addIdentity(global[i]);
                }
            } else {
                global = new String[0];
            }
            if (values.length - global.length > 0) {
                IdentityRepository.Wrapper ir =
                        new IdentityRepository.Wrapper(client.getIdentityRepository(), true);
                for (int i = 0; i < values.length; i++) {
                    String ifile = values[i];
                    for (int j = 0; j < global.length; j++) {
                        if (!ifile.equals(global[j]))
                            continue;
                        ifile = null;
                        break;
                    }
                    if (ifile == null)
                        continue;
                    Identity identity =
                            IdentityFile.newInstance(ifile, null, client);
                    ir.add(identity);
                }
                this.setIdentityRepository(ir);
            }
        }

        value = config.getValue("ServerAliveInterval");
        if (value != null) {
            try {
                setServerAliveInterval(Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
            } catch (SocketException e) {
                throw new JSchException(JSchException.CODE_ERROR_SOCKET, "setServerAliveInterval()", e);
            }
        }

        value = config.getValue("ConnectTimeout");
        if (value != null) {
            try {
                setTimeout(Integer.parseInt(value));
            } catch (SocketException e) {
                throw new JSchException(JSchException.CODE_ERROR_SOCKET, "setTimeout()", e);
            }
        }

        value = config.getValue("MaxAuthTries");
        if (value != null) {
            setConfig("MaxAuthTries", value);
        }

        value = config.getValue("ClearAllForwardings");
        if (value != null) {
            setConfig("ClearAllForwardings", value);
        }

    }

    private void applyConfigChannel(ChannelSession channel) {
        ConfigRepository configRepository = client.getConfigRepository();
        if (configRepository == null) {
            return;
        }

        ConfigRepository.Config config = configRepository.getConfig(org_host);

        String value = config.getValue("ForwardAgent");
        if (value != null) {
            channel.setAgentForwarding(value.equals("yes"));
        }

        value = config.getValue("RequestTTY");
        if (value != null) {
            channel.setPty(value.equals("yes"));
        }
    }

    private void requestPortForwarding() throws JSchException {

        if (getConfig("ClearAllForwardings").equals("yes"))
            return;

        ConfigRepository configRepository = client.getConfigRepository();
        if (configRepository == null) {
            return;
        }

        ConfigRepository.Config config = configRepository.getConfig(org_host);

        String[] values = config.getValues("LocalForward");
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                setPortForwardingL(values[i]);
            }
        }

        values = config.getValues("RemoteForward");
        if (values != null) {
            for (int i = 0; i < values.length; i++) {
                setPortForwardingR(values[i]);
            }
        }
    }

    private void checkConfig(ConfigRepository.Config config, String key) {
        String value = config.getValue(key);
        if (value != null)
            setConfig(key, value);
    }

    public void setSessionListener(SessionListener listener) {
        this.listener = listener;
    }

    public SessionListener getSessionListener() {
        return this.listener;
    }
}
