package com.jcraft.jsch;

import com.jcraft.jsch.exception.JSchException;
import com.jcraft.jsch.request.Request;
import com.jcraft.jsch.request.RequestSftp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import jerry.filebrowser.file.SFTPAttrs;
import jerry.filebrowser.file.SFTPFile;

public class ChannelSftp extends ChannelSession {

    static private final int LOCAL_MAXIMUM_PACKET_SIZE = 32 * 1024;
    static private final int LOCAL_WINDOW_SIZE_MAX = (64 * LOCAL_MAXIMUM_PACKET_SIZE);

    private static final byte SSH_FXP_INIT = 1;
    private static final byte SSH_FXP_VERSION = 2;

    private static final byte SSH_FXP_OPEN = 3;// handle
    private static final byte SSH_FXP_CLOSE = 4;// handle

    private static final byte SSH_FXP_READ = 5;// 读写文件
    private static final byte SSH_FXP_WRITE = 6;

    private static final byte SSH_FXP_LSTAT = 7;
    private static final byte SSH_FXP_FSTAT = 8;
    private static final byte SSH_FXP_SETSTAT = 9;
    private static final byte SSH_FXP_FSETSTAT = 10;

    private static final byte SSH_FXP_OPENDIR = 11;// handle
    private static final byte SSH_FXP_READDIR = 12;

    private static final byte SSH_FXP_REMOVE = 13;
    private static final byte SSH_FXP_MKDIR = 14;
    private static final byte SSH_FXP_RMDIR = 15;

    private static final byte SSH_FXP_REALPATH = 16;

    private static final byte SSH_FXP_STAT = 17;

    private static final byte SSH_FXP_RENAME = 18;
    private static final byte SSH_FXP_READLINK = 19;
    private static final byte SSH_FXP_SYMLINK = 20;

//    private static final byte SSH_FXP_LINK = 21;
//    private static final byte SSH_FXP_BLOCK = 22;
//    private static final byte SSH_FXP_UNBLOCK = 23;


    private static final byte SSH_FXP_STATUS = 101;
    private static final byte SSH_FXP_HANDLE = 102;
    private static final byte SSH_FXP_DATA = 103;
    private static final byte SSH_FXP_NAME = 104;
    private static final byte SSH_FXP_ATTRS = 105;

    private static final byte SSH_FXP_EXTENDED = (byte) 200;
    private static final byte SSH_FXP_EXTENDED_REPLY = (byte) 201;

    // pflags
    private static final int SSH_FXF_READ = 0x00000001;
    private static final int SSH_FXF_WRITE = 0x00000002;
    private static final int SSH_FXF_APPEND = 0x00000004;
    private static final int SSH_FXF_CREAT = 0x00000008;
    // 强制截断文件长度为0，同时必须指定SSH_FXF_CREAT
    private static final int SSH_FXF_TRUNC = 0x00000010;
    // 如果文件已经存在就会错误，同时必须指定SSH_FXF_CREAT
    private static final int SSH_FXF_EXCL = 0x00000020;

    private static final int SSH_FILEXFER_ATTR_SIZE = 0x00000001;
    private static final int SSH_FILEXFER_ATTR_UIDGID = 0x00000002;
    private static final int SSH_FILEXFER_ATTR_PERMISSIONS = 0x00000004;
    private static final int SSH_FILEXFER_ATTR_ACMODTIME = 0x00000008;
    private static final int SSH_FILEXFER_ATTR_EXTENDED = 0x80000000;

    public static final int SSH_FX_OK = 0;
    public static final int SSH_FX_EOF = 1;
    public static final int SSH_FX_NO_SUCH_FILE = 2;
    public static final int SSH_FX_PERMISSION_DENIED = 3;
    public static final int SSH_FX_FAILURE = 4;
    public static final int SSH_FX_BAD_MESSAGE = 5;
    public static final int SSH_FX_NO_CONNECTION = 6;
    public static final int SSH_FX_CONNECTION_LOST = 7;
    public static final int SSH_FX_OP_UNSUPPORTED = 8;

    public static final int SSH_FX_NOT_A_DIRECTORY = 19;
    /*
   SSH_FX_OK
      Indicates successful completion of the operation.
   SSH_FX_EOF
     indicates end-of-file condition; for SSH_FX_READ it means that no
       more data is available inputStream the file, and for SSH_FX_READDIR it
      indicates that no more files are contained inputStream the directory.
   SSH_FX_NO_SUCH_FILE
      is returned when a reference is made to a file which should exist
      but doesn't.
   SSH_FX_PERMISSION_DENIED
      is returned when the authenticated user does not have sufficient
      permissions to perform the operation.
   SSH_FX_FAILURE
      is a generic catch-all error message; it should be returned if an
      error occurs for which there is no more specific error code
      defined.
   SSH_FX_BAD_MESSAGE
      may be returned if a badly formatted packet or protocol
      incompatibility is detected.
   SSH_FX_NO_CONNECTION
      is a pseudo-error which indicates that the client has no
      connection to the server (it can only be generated locally by the
      client, and MUST NOT be returned by servers).
   SSH_FX_CONNECTION_LOST
      is a pseudo-error which indicates that the connection to the
      server has been lost (it can only be generated locally by the
      client, and MUST NOT be returned by servers).
   SSH_FX_OP_UNSUPPORTED
      indicates that an attempt was made to perform an operation which
      is not supported for the server (it may be generated locally by
      the client if e.g.  the version number exchange indicates that a
      required feature is not supported by the server, or it may be
      returned by the server if the server does not implement an
      operation).
*/
    private static final int MAX_MSG_LENGTH = 256 * 1024;

    public static final int MODE_OVERWRITE = 0;
    public static final int MODE_RESUME = 1;
    public static final int MODE_APPEND = 2;
    public static final int MODE_CREATE = 3;

    public static final int CLIENT_VERSION = 3;
    private volatile int serverVersion = 0;

    private volatile boolean interactive = false;
    private volatile int requestSequence = 1;
    private volatile int[] ackid = new int[1];

    private Buffer globalBuffer;
    private Packet packet;

    // 以下将用于inputStream文件的上传。
    private volatile Buffer obuf;
    private volatile Packet opacket;


    private volatile HashMap<String, String> extensions = null;
    private volatile MyPipedInputStream inputStream = null;

    private volatile boolean extension_posix_rename = false;
    private volatile boolean extension_statvfs = false;
    // private boolean extension_fstatvfs = false;
    private volatile boolean extension_hardlink = false;

/*
10. Changes from previous protocol versions
  The SSH File Transfer Protocol has changed over time, before it's
   standardization.  The following is a description of the incompatible
   changes between different versions.
10.1 Changes between versions 3 and 2
   o  The SSH_FXP_READLINK and SSH_FXP_SYMLINK messages were added.
   o  The SSH_FXP_EXTENDED and SSH_FXP_EXTENDED_REPLY messages were added.
   o  The SSH_FXP_STATUS message was changed to include fields `error
      message' and `language tag'.
10.2 Changes between versions 2 and 1
   o  The SSH_FXP_RENAME message was added.
10.3 Changes between versions 1 and 0
   o  Implementation changes, no actual protocol changes.
*/

    private static final String file_separator = File.separator;
    private static final char file_separatorc = File.separatorChar;
    private static boolean fs_is_bs = (byte) File.separatorChar == '\\';

    private volatile String cwd;
    private volatile String home;

    private static final String UTF8 = "UTF-8";
    private String fileEncoding = UTF8;
    private boolean fileEncodingIsUTF8 = true;

    private RequestQueue requestQueue = new RequestQueue(16);

    public ChannelSftp() {
        super();
        super.localMaxWindowSize = LOCAL_WINDOW_SIZE_MAX;
        super.localWindowSize = LOCAL_WINDOW_SIZE_MAX;
        super.localMaxPacketSize = LOCAL_MAXIMUM_PACKET_SIZE;
    }

    /**
     * Specify how many requests may be sent at any one time.
     * Increasing this value may slightly improve file transfer speed but will
     * increase memory usage.  The default is 16 requests.
     *
     * @param bulk_requests how many requests may be outstanding at any one time.
     */
    public void setBulkRequests(int bulk_requests) throws JSchException {
        if (bulk_requests > 0)
            requestQueue = new RequestQueue(bulk_requests);
        else
            throw new JSchException("setBulkRequests: " +
                    bulk_requests + " must be greater than 0.");
    }

    /**
     * This method will return the value how many requests may be
     * sent at any one time.
     *
     * @return how many requests may be sent at any one time.
     */
    public int getBulkRequests() {
        return requestQueue.size();
    }


    @Override
    void init() {
    }

    @Override
    public void start() throws JSchException {
        try {
            PipedOutputStream outputStream = new PipedOutputStream();
            io_local.setOutputStream(outputStream);
            inputStream = new MyPipedInputStream(outputStream, remoteMaxPacketSize);
            io_local.setInputStream(inputStream);

            Request request = new RequestSftp();
            request.request(getSession(), this);

            globalBuffer = new Buffer(localMaxPacketSize);
            packet = new Packet(globalBuffer);

            obuf = new Buffer(remoteMaxPacketSize);
            opacket = new Packet(obuf);

            // send SSH_FXP_INIT
            sendINIT();// 初始化
            // receive SSH_FXP_VERSION
            Header header = new Header();
            readHeader(globalBuffer, header);
            int length = header.length;
            if (length > MAX_MSG_LENGTH) {
                throw new SftpException(SSH_FX_FAILURE, "Received message is too long: " + length);
            }
            int type = header.type;             // 2 -> SSH_FXP_VERSION
            if (type != SSH_FXP_VERSION) {
                throw new SftpException(SSH_FX_FAILURE, "header.type不是SSH_FXP_VERSION");
            }
            serverVersion = header.responseId;
            //  System.out.println("服务器SFTP协议版本：" + serverVersion);
            if (length > 0) {
                extensions = new HashMap<>();
                // extension data
                fill(globalBuffer, length);
                byte[] extension_name = null;
                byte[] extension_data = null;
                while (length > 0) {
                    extension_name = globalBuffer.getStringByte();
                    length -= (4 + extension_name.length);
                    extension_data = globalBuffer.getStringByte();
                    length -= (4 + extension_data.length);
                    extensions.put(Util.byte2str(extension_name), Util.byte2str(extension_data));
                }
                String s = extensions.get("posix-rename@openssh.com");
                if (s != null && s.equals("1")) {
                    extension_posix_rename = true;
                }
                s = extensions.get("statvfs@openssh.com");
                if (s != null && s.equals("2")) {
                    extension_statvfs = true;
                }
                s = extensions.get("hardlink@openssh.com");
                if (s != null && s.equals("1")) {
                    extension_hardlink = true;
                }
            }

      /*
      if(extensions.get("fstatvfs@openssh.com")!=null &&
         extensions.get("fstatvfs@openssh.com").equals("2")){
        extension_fstatvfs = true;
      }
      */

        } catch (Exception e) {
            //System.err.println(e);
            if (e instanceof JSchException) throw (JSchException) e;
            throw new JSchException(e);
        }
    }
//
//    public void lcd(String path) throws SftpException {
//        path = localAbsolutePath(path);
//        if ((new File(path)).isDirectory()) {
//            try {
//                path = (new File(path)).getCanonicalPath();
//            } catch (Exception ignored) {
//            }
//            lcwd = path;
//            return;
//        }
//        throw new SftpException(SSH_FX_NO_SUCH_FILE, "No such directory");
//    }

    public synchronized void cd(String path) throws Exception {
        inputStream.updateReadSide();

        path = remoteAbsolutePath(path);
        path = isUnique(path);

        byte[] str = _realPath(path);
        SFTPAttrs attr = _stat(str);

        if ((attr.getFlags() & SFTPAttrs.SSH_FILEXFER_ATTR_PERMISSIONS) == 0) {
            throw new SftpException(SSH_FX_FAILURE, "Can't change directory: " + path);
        }
        if (!attr.isDir()) {
            throw new SftpException(SSH_FX_FAILURE, "Can't change directory: " + path);
        }

        setCwd(Util.byte2str(str, fileEncoding));

    }

    public void put(String src, String dst) throws Exception {
        put(src, dst, null, MODE_OVERWRITE);
    }

    public void put(String src, String dst, ProgressCallback monitor) throws Exception {
        put(src, dst, monitor, MODE_OVERWRITE);
    }


    public void put(InputStream src, String dst) throws Exception {
        put(src, dst, null, MODE_OVERWRITE);
    }

    public void put(InputStream src, String dst, ProgressCallback monitor) throws Exception {
        put(src, dst, monitor, MODE_OVERWRITE);
    }


    /**
     * Sends data from <code>src</code> file to <code>dst</code> file.
     * The <code>mode</code> should be <code>OVERWRITE</code>,
     * <code>RESUME</code> or <code>APPEND</code>.
     *
     * @param src     source file
     * @param dst     destination file
     * @param monitor progress monitor
     * @param mode    how data should be added to dst
     */
    public void put(String src, String dst, ProgressCallback monitor, int mode) throws Exception {
        inputStream.updateReadSide();
        Vector v = glob_remote(dst);
        int vsize = v.size();
        if (vsize != 1) {
            if (vsize == 0) {
                if (isPattern(dst))
                    throw new SftpException(SSH_FX_FAILURE, dst);
                else
                    dst = Util.unquote(dst);
            }
            throw new SftpException(SSH_FX_FAILURE, v.toString());
        } else {
            dst = (String) (v.elementAt(0));
        }

        boolean isRemoteDir = isRemoteDir(dst);

        v = glob_local(src);
        vsize = v.size();

        StringBuffer dstsb = null;
        if (isRemoteDir) {
            if (!dst.endsWith("/")) {
                dst += "/";
            }
            dstsb = new StringBuffer(dst);
        } else if (vsize > 1) {
            throw new SftpException(SSH_FX_FAILURE,
                    "Copying multiple files, but the destination is missing or a file.");
        }

        for (int j = 0; j < vsize; j++) {
            String _src = (String) (v.elementAt(j));
            String _dst = null;
            if (isRemoteDir) {
                int i = _src.lastIndexOf(file_separatorc);
                if (fs_is_bs) {
                    int ii = _src.lastIndexOf('/');
                    if (ii != -1 && ii > i)
                        i = ii;
                }
                if (i == -1) dstsb.append(_src);
                else dstsb.append(_src.substring(i + 1));
                _dst = dstsb.toString();
                dstsb.delete(dst.length(), _dst.length());
            } else {
                _dst = dst;
            }

            long size_of_dst = 0;
            if (mode == MODE_RESUME) {
                try {
                    SFTPAttrs attr = _stat(_dst);
                    size_of_dst = attr.getSize();
                } catch (Exception e) {
                    //System.err.println(eee);
                }
                long size_of_src = new File(_src).length();
                if (size_of_src < size_of_dst) {
                    throw new SftpException(SSH_FX_FAILURE,
                            "failed to resume for " + _dst);
                }
                if (size_of_src == size_of_dst) {
                    return;
                }
            }

            if (monitor != null) {
                monitor.onStart(ProgressCallback.ACTION_PUT, _src, _dst, (new File(_src)).length());
                if (mode == MODE_RESUME) {
                    monitor.onSizeIncrease(size_of_dst);
                }
            }
            try (FileInputStream fileInputStream = new FileInputStream(_src)) {
                put(fileInputStream, _dst, monitor, mode);
            }
        }
    }

    public void upload(String src, String dest, ProgressCallback monitor, int mode) throws Exception {
        inputStream.updateReadSide();

        final File localFile = new File(src);
        final long size_of_src = localFile.length();
        if (!localFile.exists()) throw new FileNotFoundException("找不到文件：" + src);

        long size_of_dst = 0;
        if (mode == MODE_RESUME) {
            try {
                SFTPAttrs attr = _stat(dest);
                size_of_dst = attr.getSize();
            } catch (Exception e) {
                //System.err.println(eee);
            }
            if (size_of_src < size_of_dst) {
                throw new SftpException(SSH_FX_FAILURE, "failed to resume for " + dest);
            }
            if (size_of_src == size_of_dst) {
                if (monitor != null) {
                    monitor.onFinish();
                }
                return;
            }
        }

        if (monitor != null) {
            monitor.onStart(ProgressCallback.ACTION_PUT, src, dest, size_of_src);
            if (mode == MODE_RESUME) {
                monitor.onSizeIncrease(size_of_dst);
            }
        }
        try (FileInputStream fileInputStream = new FileInputStream(localFile)) {
            put(fileInputStream, dest, monitor, mode);
        }
    }

    public OutputStream getOutputStream(String dst) throws Exception {
        return getOutputStream(dst, null, MODE_OVERWRITE);
    }

    public OutputStream getOutputStream(String dst, final ProgressCallback monitor, final int mode) throws Exception {
        return getOutputStream(dst, monitor, mode, 0);
    }

    /**
     * Sends data from the input stream <code>src</code> to <code>dst</code> file.
     * The <code>mode</code> should be <code>OVERWRITE</code>,
     * <code>RESUME</code> or <code>APPEND</code>.
     *
     * @param src     input stream
     * @param dst     destination file
     * @param monitor progress monitor
     * @param mode    how data should be added to dst
     */
    private void put(InputStream src, String dst, ProgressCallback monitor, int mode) throws Exception {
        inputStream.updateReadSide();

        final byte[] dstb = Util.str2byte(dst, fileEncoding);
        long skip = 0;
        if (mode == MODE_RESUME || mode == MODE_APPEND) {
            try {
                SFTPAttrs attr = _stat(dstb);
                skip = attr.getSize();
            } catch (Exception ignored) {
            }
        }
        if (mode == MODE_RESUME && skip > 0) {
            long skipped = src.skip(skip);
            if (skipped < skip) {
                throw new SftpException(SSH_FX_FAILURE, "failed to resume for " + dst);
            }
        }

        if (mode == MODE_CREATE) {
            sendOpenCreateAndWrite(dstb);
        } else if (mode == MODE_OVERWRITE) {
            sendOpenWrite(dstb);
        } else {
            sendOpenAppend(dstb);
        }

        final Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_HANDLE) {
            int i = globalBuffer.readInt();
            throwStatusError(globalBuffer, i);
        }

        final byte[] handle = globalBuffer.getStringByte();         // handle
        byte[] data = null;

        boolean doNotCopy = true;

        if (!doNotCopy) {  // This case will not work anymore.
            data = new byte[obuf.data.length - (5 + 13 + 21 + handle.length + Session.buffer_margin)];
        }

        long offset = 0;
        if (mode == MODE_RESUME || mode == MODE_APPEND) {
            offset += skip;
        }

        int startid = requestSequence;
        int ackcount = 0;
        int _s = 0;
        int _datalen = 0;

        if (!doNotCopy) {  // This case will not work anymore.
            _datalen = data.length;
        } else {
            data = obuf.data;
            _s = 5 + 13 + 21 + handle.length;
            _datalen = obuf.data.length - _s - Session.buffer_margin;
        }

        int bulk_requests = requestQueue.size();

        while (true) {
            int nread = 0;
            int count = 0;
            int s = _s;
            int datalen = _datalen;

            do {
                nread = src.read(data, s, datalen);
                if (nread > 0) {
                    s += nread;
                    datalen -= nread;
                    count += nread;
                }
            }
            while (datalen > 0 && nread > 0);
            if (count <= 0) break;

            int foo = count;
            while (foo > 0) {
                if ((requestSequence - 1) == startid ||
                        ((requestSequence - startid) - ackcount) >= bulk_requests) {
                    while (((requestSequence - startid) - ackcount) >= bulk_requests) {
                        if (checkStatus(ackid, header)) {
                            int _ackid = ackid[0];
                            if (startid > _ackid || _ackid > requestSequence - 1) {
                                if (_ackid == requestSequence) {
                                    System.err.println("ack error: startid=" + startid + " requestSequence=" + requestSequence + " _ackid=" + _ackid);
                                } else {
                                    throw new SftpException(SSH_FX_FAILURE, "ack error: startid=" + startid + " requestSequence=" + requestSequence + " _ackid=" + _ackid);
                                }
                            }
                            ackcount++;
                        } else {
                            break;
                        }
                    }
                }
                if (doNotCopy) {
                    foo -= sendWrite(handle, offset, data, 0, foo);
                    if (data != obuf.data) {
                        data = obuf.data;
                        _datalen = obuf.data.length - _s - Session.buffer_margin;
                    }
                } else {
                    foo -= sendWrite(handle, offset, data, _s, foo);
                }
            }
            offset += count;
            if (monitor != null && !monitor.onSizeIncrease(count)) {
                break;
            }
        }
        int _ackcount = requestSequence - startid;
        while (_ackcount > ackcount) {
            if (!checkStatus(null, header)) {
                break;
            }
            ackcount++;
        }
        if (monitor != null) monitor.onFinish();
        sendCloseHandleAndCheck(handle, header);
    }


    /**
     * Sends data from the output stream to <code>dst</code> file.
     * The <code>mode</code> should be <code>MODE_OVERWRITE</code>,
     * <code>RESUME</code> or <code>APPEND</code>.
     *
     * @param dst     destination file
     * @param monitor progress monitor
     * @param mode    how data should be added to dst
     * @param offset  data will be added at offset
     * @return output stream, which accepts data to be transferred.
     */
    public OutputStream getOutputStream(String dst, final ProgressCallback monitor, final int mode, long offset) throws Exception {
        inputStream.updateReadSide();
        dst = remoteAbsolutePath(dst);

        if (isRemoteDir(dst)) {
            throw new SftpException(SSH_FX_FAILURE, dst + " is a directory");
        }

        byte[] dstb = Util.str2byte(dst, fileEncoding);

        long skip = 0;
        if (mode == MODE_RESUME || mode == MODE_APPEND) {
            try {
                SFTPAttrs attr = _stat(dstb);
                skip = attr.getSize();
            } catch (Exception e) {
                //System.err.println(eee);
            }
        }

        if (monitor != null) {
            monitor.onStart(ProgressCallback.ACTION_PUT, "", dst, ProgressCallback.UNKNOWN_SIZE);
        }

        if (mode == MODE_OVERWRITE) {
            sendOpenWrite(dstb);
        } else {
            sendOpenAppend(dstb);
        }
        Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_STATUS && type != SSH_FXP_HANDLE) {
            throw new SftpException(SSH_FX_FAILURE, "");
        }
        if (type == SSH_FXP_STATUS) {
            int i = globalBuffer.readInt();
            throwStatusError(globalBuffer, i);
        }
        final byte[] handle = globalBuffer.getStringByte();         // handle

        if (mode == MODE_RESUME || mode == MODE_APPEND) {
            offset += skip;
        }

        final long[] _offset = new long[1];
        _offset[0] = offset;
        OutputStream out = new OutputStream() {
            private boolean init = true;
            private boolean isClosed = false;
            private int[] ackid = new int[1];
            private int startid = 0;
            private int _ackid = 0;
            private int ackcount = 0;
            private int writecount = 0;
            private Header header = new Header();

            public void write(byte[] d) throws IOException {
                write(d, 0, d.length);
            }

            public void write(byte[] d, int s, int len) throws IOException {
                if (init) {
                    startid = requestSequence;
                    _ackid = requestSequence;
                    init = false;
                }

                if (isClosed) {
                    throw new IOException("stream already closed");
                }

                try {
                    int _len = len;
                    while (_len > 0) {
                        int sent = sendWrite(handle, _offset[0], d, s, _len);
                        writecount++;
                        _offset[0] += sent;
                        s += sent;
                        _len -= sent;
                        if ((requestSequence - 1) == startid ||
                                inputStream.available() >= 1024) {
                            while (inputStream.available() > 0) {
                                if (checkStatus(ackid, header)) {
                                    _ackid = ackid[0];
                                    if (startid > _ackid || _ackid > requestSequence - 1) {
                                        throw new SftpException(SSH_FX_FAILURE, "");
                                    }
                                    ackcount++;
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                    if (monitor != null && !monitor.onSizeIncrease(len)) {
                        close();
                        throw new IOException("canceled");
                    }
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException(e.toString());
                }
            }

            byte[] _data = new byte[1];

            public void write(int foo) throws IOException {
                _data[0] = (byte) foo;
                write(_data, 0, 1);
            }

            public void flush() throws IOException {
                if (isClosed) {
                    throw new IOException("stream already closed");
                }
                if (!init) {
                    try {
                        while (writecount > ackcount) {
                            if (!checkStatus(null, header)) {
                                break;
                            }
                            ackcount++;
                        }
                    } catch (Exception e) {
                        throw new IOException(e);
                    }
                }
            }

            public void close() throws IOException {
                if (isClosed) {
                    return;
                }
                flush();
                if (monitor != null) monitor.onFinish();
                try {
                    sendCloseHandleAndCheck(handle, header);
                } catch (IOException e) {
                    throw e;
                } catch (Exception e) {
                    throw new IOException(e.toString());
                }
                isClosed = true;
            }
        };
        return out;
    }

    public void get(String src, String dst) throws SftpException {
        get(src, dst, null, MODE_OVERWRITE);
    }

    public void get(String src, String dst, ProgressCallback monitor) throws SftpException {
        get(src, dst, monitor, MODE_OVERWRITE);
    }

    public synchronized void get(String src, String dest, ProgressCallback monitor, int mode) throws SftpException {
        String _dst = null;
        boolean isExist = false;
        try {
            inputStream.updateReadSide();

            src = remoteAbsolutePath(src);

            Vector<String> v = glob_remote(src);
            int vsize = v.size();
            if (vsize == 0) {
                throw new SftpException(SSH_FX_NO_SUCH_FILE, "No such file");
            }

            File dstFile = new File(dest);
            boolean isDstDir = dstFile.isDirectory();
            StringBuilder dstsb = null;
            if (isDstDir) {
                if (!dest.endsWith(file_separator)) {
                    dest += file_separator;
                }
                dstsb = new StringBuilder(dest);
            } else if (vsize > 1) {
                throw new SftpException(SSH_FX_FAILURE,
                        "Copying multiple files, but destination is missing or a file.");
            }

            for (int j = 0; j < vsize; j++) {
                String _src = v.elementAt(j);
                SFTPAttrs attr = _stat(_src);
                if (attr.isDir()) {
                    throw new SftpException(SSH_FX_FAILURE,
                            "not supported to get directory " + _src);
                }

                _dst = null;
                if (isDstDir) {
                    int i = _src.lastIndexOf('/');
                    if (i == -1) dstsb.append(_src);
                    else dstsb.append(_src.substring(i + 1));
                    _dst = dstsb.toString();
                    if (_dst.contains("..")) {
                        String dstc = (new File(dest)).getCanonicalPath();
                        String _dstc = (new File(_dst)).getCanonicalPath();
                        if (!(_dstc.length() > dstc.length() &&
                                _dstc.substring(0, dstc.length() + 1).equals(dstc + file_separator))) {
                            throw new SftpException(SSH_FX_FAILURE,
                                    "writing to an unexpected file " + _src);
                        }
                    }
                    dstsb.delete(dest.length(), _dst.length());
                } else {
                    _dst = dest;
                }

                File _dstFile = new File(_dst);
                if (mode == MODE_RESUME) {
                    if (!_dstFile.exists()) throw new SftpException(SSH_FX_FAILURE, _dst + "不存在");
                    long size_of_src = attr.getSize();
                    long size_of_dst = _dstFile.length();
                    if (size_of_dst > size_of_src) {
                        throw new SftpException(SSH_FX_FAILURE, "failed to resume for " + _dst);
                    }
                    if (size_of_dst == size_of_src) {
                        return;
                    }
                }

                if (monitor != null) {
                    monitor.onStart(ProgressCallback.ACTION_GET, _src, _dst, attr.getSize());
                    if (mode == MODE_RESUME) {
                        monitor.onSizeIncrease(_dstFile.length());
                    }
                }

                FileOutputStream fos = null;
                isExist = _dstFile.exists();
                try {
                    if (mode == MODE_OVERWRITE || mode == MODE_CREATE) {
                        fos = new FileOutputStream(_dst);
                    } else {
                        fos = new FileOutputStream(_dst, true); // append
                    }
                    // System.err.println("_get: "+_src+", "+_dst);
                    _get(_src, fos, monitor, mode, new File(_dst).length());
                } finally {
                    if (fos != null) {
                        fos.close();
                    }
                }
            }
        } catch (Exception e) {
            if (dest != null) {
                File _dstFile = new File(_dst);
                if (!isExist && _dstFile.exists() && _dstFile.length() == 0) {
                    _dstFile.delete();
                }
            }
            if (e instanceof SftpException) throw (SftpException) e;
            throw new SftpException(SSH_FX_FAILURE, "", e);
        }
    }

    public void download(String src, String dest, ProgressCallback monitor, int mode) throws Exception {
        boolean isExist = false;
        try {
            inputStream.updateReadSide();
            final SFTPAttrs attr = _stat(src);
            if (attr.isDir()) {
                throw new IOException("不支持下载目录：" + src);
            }
            final File localFile = new File(dest);
            isExist = localFile.exists();
            if (mode == MODE_CREATE) {
                if (isExist) throw new IOException("本地文件已存在：" + dest);
            } else if (mode == MODE_RESUME) {
                if (!isExist) throw new FileNotFoundException(dest + "不存在");
                long size_of_src = attr.getSize();
                long size_of_dst = localFile.length();
                if (size_of_dst > size_of_src) {
                    throw new IOException("本地文件大于远程文件，下载失败：" + dest);
                } else if (size_of_dst == size_of_src) {// 下载完成
                    return;
                }
            }

            if (monitor != null) {
                monitor.onStart(ProgressCallback.ACTION_GET, src, dest, attr.getSize());
                if (mode == MODE_RESUME) {
                    monitor.onSizeIncrease(localFile.length());
                }
            }
            FileOutputStream fos = null;
            try {
                if (mode == MODE_CREATE || mode == MODE_OVERWRITE) {
                    fos = new FileOutputStream(localFile);
                } else {
                    fos = new FileOutputStream(localFile, true);
                }
                _get(src, fos, monitor, mode, localFile.length());
            } finally {
                if (fos != null) {
                    fos.close();
                }
            }
        } catch (Exception e) {
            if (dest != null) {
                final File destFile = new File(dest);
                if (!isExist && destFile.exists() && destFile.length() == 0) {
                    destFile.delete();
                }
            }
            throw e;
        }
    }

    public void get(String src, OutputStream dst) throws SftpException {
        get(src, dst, null, MODE_OVERWRITE, 0);
    }

    public void get(String src, OutputStream dst,
                    ProgressCallback monitor) throws SftpException {
        get(src, dst, monitor, MODE_OVERWRITE, 0);
    }

    public void get(String src, OutputStream dst,
                    ProgressCallback monitor, int mode, long skip) throws SftpException {
//System.err.println("get: "+src+", "+dst);
        try {
            inputStream.updateReadSide();

            src = remoteAbsolutePath(src);
            src = isUnique(src);

            if (monitor != null) {
                SFTPAttrs attr = _stat(src);
                monitor.onStart(ProgressCallback.ACTION_GET, src, "??", attr.getSize());
                if (mode == MODE_RESUME) {
                    monitor.onSizeIncrease(skip);
                }
            }
            _get(src, dst, monitor, mode, skip);
        } catch (Exception e) {
            if (e instanceof SftpException) throw (SftpException) e;
            throw new SftpException(SSH_FX_FAILURE, "", e);
        }
    }

    private void _get(String src, OutputStream outputStream,
                      ProgressCallback monitor, int mode, long skip) throws Exception {

        sendOpenRead(Util.str2byte(src, fileEncoding));
        final Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_STATUS && type != SSH_FXP_HANDLE) {
            throw new SftpException(SSH_FX_FAILURE, "");
        }

        if (type == SSH_FXP_STATUS) {
            int i = globalBuffer.readInt();
            throwStatusError(globalBuffer, i);
        }

        final byte[] handle = globalBuffer.getStringByte();// filename

        long offset = 0;
        if (mode == MODE_RESUME) {
            offset += skip;
        }

        int request_max = 1;
        requestQueue.init();
        long request_offset = offset;

        int request_len = globalBuffer.data.length - 13;
        if (serverVersion == 0) {
            request_len = 1024;
        }

        loop:
        while (true) {
            while (requestQueue.count() < request_max) {
                sendREAD(handle, request_offset, request_len, requestQueue);
                request_offset += request_len;
            }

            readHeader(globalBuffer, header);
            length = header.length;
            type = header.type;

            RequestQueue.Request request = null;
            try {
                request = requestQueue.get(header.responseId);
            } catch (RequestQueue.OutOfOrderException e) {
                request_offset = e.offset;
                skip(header.length);
                requestQueue.cancel(header, globalBuffer);
                continue;
            }

            if (type == SSH_FXP_STATUS) {
                fill(globalBuffer, length);
                int i = globalBuffer.readInt();
                if (i == SSH_FX_EOF) {
                    break;
                }
                throwStatusError(globalBuffer, i);
            }

            if (type != SSH_FXP_DATA) {
                break;
            }

            globalBuffer.rewind();
            fill(globalBuffer.data, 0, 4);
            length -= 4;
            int length_of_data = globalBuffer.readInt();   // length of data

            /**
             Since sftp protocol version 6, "end-of-file" has been defined,
             byte   SSH_FXP_DATA
             uint32 request-code
             string data
             bool   end-of-file [optional]
             but some sftpd server will send such a field inputStream the sftp protocol 3 ;-(
             */
            final int optional_data = length - length_of_data;

            int foo = length_of_data;
            while (foo > 0) {
                int bar = foo;
                if (bar > globalBuffer.data.length) {
                    bar = globalBuffer.data.length;
                }
                int data_len = inputStream.read(globalBuffer.data, 0, bar);
                if (data_len < 0) {
                    break loop;
                }

                outputStream.write(globalBuffer.data, 0, data_len);

                offset += data_len;
                foo -= data_len;

                if (monitor != null) {
                    if (!monitor.onSizeIncrease(data_len)) {
                        skip(foo);
                        if (optional_data > 0) {
                            skip(optional_data);
                        }
                        break loop;
                    }
                }

            }

            if (optional_data > 0) {
                skip(optional_data);
            }

            if (length_of_data < request.length) {  //
                requestQueue.cancel(header, globalBuffer);
                sendREAD(handle, request.offset + length_of_data, (int) (request.length - length_of_data), requestQueue);
                request_offset = request.offset + request.length;
            }

            if (request_max < requestQueue.size()) {
                request_max++;
            }
        }
        outputStream.flush();
        outputStream.close();

        if (monitor != null) monitor.onFinish();

        requestQueue.cancel(header, globalBuffer);
        sendCloseHandleAndCheck(handle, header);
    }


    private class RequestQueue {
        class OutOfOrderException extends Exception {
            long offset;

            OutOfOrderException(long offset) {
                this.offset = offset;
            }
        }

        class Request {
            int id;
            long offset;
            long length;
        }

        Request[] rrq = null;
        int head, count;

        RequestQueue(int size) {
            rrq = new Request[size];
            for (int i = 0; i < rrq.length; i++) {
                rrq[i] = new Request();
            }
            init();
        }

        void init() {
            head = count = 0;
        }

        void add(int id, long offset, int length) {
            if (count == 0) head = 0;
            int tail = head + count;
            if (tail >= rrq.length) tail -= rrq.length;
            rrq[tail].id = id;
            rrq[tail].offset = offset;
            rrq[tail].length = length;
            count++;
        }

        Request get(int id) throws OutOfOrderException, SftpException {
            count -= 1;
            int i = head;
            head++;
            if (head == rrq.length) head = 0;
            if (rrq[i].id != id) {
                long offset = getOffset();
                boolean find = false;
                for (int j = 0; j < rrq.length; j++) {
                    if (rrq[j].id == id) {
                        find = true;
                        rrq[j].id = 0;
                        break;
                    }
                }
                if (find)
                    throw new OutOfOrderException(offset);
                throw new SftpException(SSH_FX_FAILURE, "RequestQueue: unknown request code " + id);
            }
            rrq[i].id = 0;
            return rrq[i];
        }

        int count() {
            return count;
        }

        int size() {
            return rrq.length;
        }

        void cancel(Header header, Buffer buf) throws IOException {
            int _count = count;
            for (int i = 0; i < _count; i++) {
                readHeader(buf, header);
                int length = header.length;
                for (int j = 0; j < rrq.length; j++) {
                    if (rrq[j].id == header.responseId) {
                        rrq[j].id = 0;
                        break;
                    }
                }
                skip(length);
            }
            init();
        }

        long getOffset() {
            long result = Long.MAX_VALUE;

            for (int i = 0; i < rrq.length; i++) {
                if (rrq[i].id == 0)
                    continue;
                if (result > rrq[i].offset)
                    result = rrq[i].offset;
            }

            return result;
        }
    }

    public InputStream get(String src) throws SftpException {
        return get(src, null, 0L);
    }

    public InputStream get(String src, ProgressCallback monitor) throws SftpException {
        return get(src, monitor, 0L);
    }

//    /**
//     * @deprecated This method will be deleted inputStream the future.
//     */
//    public InputStream get(String src, int mode) throws SftpException {
//        return get(src, null, 0L);
//    }

//    /**
//     * @deprecated This method will be deleted inputStream the future.
//     */
//    public InputStream get(String src, final SftpProgressMonitor monitor, final int mode) throws SftpException {
//        return get(src, monitor, 0L);
//    }

    public synchronized InputStream get(String src, final ProgressCallback monitor, final long skip) throws SftpException {

        try {
            inputStream.updateReadSide();

            src = remoteAbsolutePath(src);
            src = isUnique(src);

            byte[] srcb = Util.str2byte(src, fileEncoding);

            SFTPAttrs attr = _stat(srcb);
            if (monitor != null) {
                monitor.onStart(ProgressCallback.ACTION_GET, src, "??", attr.getSize());
            }

            sendOpenRead(srcb);

            Header header = new Header();
            readHeader(globalBuffer, header);
            int length = header.length;
            int type = header.type;

            fill(globalBuffer, length);
            if (type == SSH_FXP_STATUS) {
                int i = globalBuffer.readInt();
                throwStatusError(globalBuffer, i);
            } else if (type != SSH_FXP_HANDLE) {
                throw new SftpException(SSH_FX_FAILURE, "");
            }

            final byte[] handle = globalBuffer.getStringByte();         // handle

            requestQueue.init();

            InputStream in = new InputStream() {
                long offset = skip;
                boolean closed = false;
                int rest_length = 0;
                byte[] _data = new byte[1];
                byte[] rest_byte = new byte[1024];
                Header header = new Header();
                int request_max = 1;
                long request_offset = offset;

                public int read() throws IOException {
                    if (closed) return -1;
                    int i = read(_data, 0, 1);
                    if (i == -1) {
                        return -1;
                    } else {
                        return _data[0] & 0xff;
                    }
                }

                public int read(byte[] d) throws IOException {
                    if (closed) return -1;
                    return read(d, 0, d.length);
                }

                public int read(byte[] d, int s, int len) throws IOException {
                    if (closed) return -1;
                    if (d == null) {
                        throw new NullPointerException();
                    }
                    if (s < 0 || len < 0 || s + len > d.length) {
                        throw new IndexOutOfBoundsException();
                    }
                    if (len == 0) {
                        return 0;
                    }

                    if (rest_length > 0) {
                        int foo = rest_length;
                        if (foo > len) foo = len;
                        System.arraycopy(rest_byte, 0, d, s, foo);
                        if (foo != rest_length) {
                            System.arraycopy(rest_byte, foo,
                                    rest_byte, 0, rest_length - foo);
                        }

                        if (monitor != null) {
                            if (!monitor.onSizeIncrease(foo)) {
                                close();
                                return -1;
                            }
                        }

                        rest_length -= foo;
                        return foo;
                    }

                    if (globalBuffer.data.length - 13 < len) {
                        len = globalBuffer.data.length - 13;
                    }
                    if (serverVersion == 0 && len > 1024) {
                        len = 1024;
                    }

                    if (requestQueue.count() == 0
                            || true // working around slow transfer speed for
                        // some sftp servers including Titan FTP.
                    ) {
                        int request_len = globalBuffer.data.length - 13;
                        if (serverVersion == 0) {
                            request_len = 1024;
                        }

                        while (requestQueue.count() < request_max) {
                            try {
                                sendREAD(handle, request_offset, request_len, requestQueue);
                            } catch (Exception e) {
                                throw new IOException("error");
                            }
                            request_offset += request_len;
                        }
                    }

                    readHeader(globalBuffer, header);
                    rest_length = header.length;
                    int type = header.type;
                    int id = header.responseId;

                    RequestQueue.Request rr = null;
                    try {
                        rr = requestQueue.get(header.responseId);
                    } catch (RequestQueue.OutOfOrderException e) {
                        request_offset = e.offset;
                        skip(header.length);
                        requestQueue.cancel(header, globalBuffer);
                        return 0;
                    } catch (SftpException e) {
                        throw new IOException("error: " + e.toString());
                    }

                    if (type != SSH_FXP_STATUS && type != SSH_FXP_DATA) {
                        throw new IOException("error");
                    }
                    if (type == SSH_FXP_STATUS) {
                        fill(globalBuffer, rest_length);
                        int i = globalBuffer.readInt();
                        rest_length = 0;
                        if (i == SSH_FX_EOF) {
                            close();
                            return -1;
                        }
                        //throwStatusError(buf, i);
                        throw new IOException("error");
                    }

                    globalBuffer.rewind();
                    fill(globalBuffer.data, 0, 4);
                    int length_of_data = globalBuffer.readInt();
                    rest_length -= 4;

                    /**
                     Since sftp protocol version 6, "end-of-file" has been defined,

                     byte   SSH_FXP_DATA
                     uint32 request-code
                     string data
                     bool   end-of-file [optional]

                     but some sftpd server will send such a field inputStream the sftp protocol 3 ;-(
                     */
                    int optional_data = rest_length - length_of_data;

                    offset += length_of_data;
                    int foo = length_of_data;
                    if (foo > 0) {
                        int bar = foo;
                        if (bar > len) {
                            bar = len;
                        }
                        int i = inputStream.read(d, s, bar);
                        if (i < 0) {
                            return -1;
                        }
                        foo -= i;
                        rest_length = foo;

                        if (foo > 0) {
                            if (rest_byte.length < foo) {
                                rest_byte = new byte[foo];
                            }
                            int _s = 0;
                            int _len = foo;
                            int j;
                            while (_len > 0) {
                                j = inputStream.read(rest_byte, _s, _len);
                                if (j <= 0) break;
                                _s += j;
                                _len -= j;
                            }
                        }

                        if (optional_data > 0) {
                            inputStream.skip(optional_data);
                        }

                        if (length_of_data < rr.length) {  //
                            requestQueue.cancel(header, globalBuffer);
                            try {
                                sendREAD(handle,
                                        rr.offset + length_of_data,
                                        (int) (rr.length - length_of_data), requestQueue);
                            } catch (Exception e) {
                                throw new IOException("error");
                            }
                            request_offset = rr.offset + rr.length;
                        }

                        if (request_max < requestQueue.size()) {
                            request_max++;
                        }

                        if (monitor != null) {
                            if (!monitor.onSizeIncrease(i)) {
                                close();
                                return -1;
                            }
                        }

                        return i;
                    }
                    return 0; // ??
                }

                public void close() throws IOException {
                    if (closed) return;
                    closed = true;
                    if (monitor != null) monitor.onFinish();
                    requestQueue.cancel(header, globalBuffer);
                    try {
                        sendCloseHandleAndCheck(handle, header);
                    } catch (Exception e) {
                        throw new IOException("error");
                    }
                }
            };
            return in;
        } catch (Exception e) {
            if (e instanceof SftpException) throw (SftpException) e;
            throw new SftpException(SSH_FX_FAILURE, "", e);
        }
    }

    public ArrayList<SFTPFile> ls(String path) throws SftpException {
        final ArrayList<SFTPFile> list = new ArrayList<>();
        LsEntrySelector selector = entry -> {
            list.add(entry);
            return LsEntrySelector.CONTINUE;
        };
        ls(path, selector);
        return list;
    }

    /**
     * List files specified by the remote <code>path</code>.
     * Each files and directories will be passed to
     * <code>LsEntrySelector#select(LsEntry)</code> method, and if that method
     * returns <code>LsEntrySelector#END</code>, the operation will be
     * canceled immediately.
     *
     * @see LsEntrySelector
     * @since 0.1.47
     */
    public synchronized void ls(String absPath, LsEntrySelector selector) throws SftpException {
        try {
            inputStream.updateReadSide();
            sendOPENDIR(Util.str2byte(absPath, fileEncoding));

            Header header = new Header();
            readHeader(globalBuffer, header);
            int length = header.length;
            int type = header.type;
            fill(globalBuffer, length);//读取剩下的所有

            if (type != SSH_FXP_HANDLE) {// 操作成功
                if (type == SSH_FXP_STATUS) {// 操作失败
                    int i = globalBuffer.readInt();
                    throwStatusError(globalBuffer, i);
                } else if (type == SSH_FX_NOT_A_DIRECTORY) {
                    throw new SftpException(SSH_FX_FAILURE, "打开的不是文件夹：" + absPath);
                } else {
                    throw new SftpException(SSH_FX_FAILURE, "响应码：" + type);
                }
            }


            int cancel = LsEntrySelector.CONTINUE;
            byte[] handle = globalBuffer.getStringByte();         // handleo

            while (cancel == LsEntrySelector.CONTINUE) {
                sendREADDIR(handle);
                readHeader(globalBuffer, header);
                length = header.length;
                type = header.type;
                if (type == SSH_FXP_STATUS) {
                    fill(globalBuffer, length);
                    int i = globalBuffer.readInt();
                    if (i == SSH_FX_EOF)
                        break;
                    throwStatusError(globalBuffer, i);
                } else if (type != SSH_FXP_NAME) {
                    throw new SftpException(SSH_FX_FAILURE, "sendREADDIR的响应不是SSH_FXP_NAME");
                }
                globalBuffer.rewind();
                fill(globalBuffer.data, 0, 4);
                length -= 4;
                int count = globalBuffer.readInt();
                // System.out.println(count);
                globalBuffer.reset();
                while (count > 0) {
                    count--;
                    if (length > 0) {
                        globalBuffer.shift();
                        int j = (globalBuffer.data.length > (globalBuffer.indexWrite + length)) ?
                                length :
                                (globalBuffer.data.length - globalBuffer.indexWrite);
                        int i = fill(globalBuffer.data, globalBuffer.indexWrite, j);
                        globalBuffer.indexWrite += i;
                        length -= i;
                    }

                    byte[] fileNameB = globalBuffer.getStringByte();

                    String longName = null;
                    if (serverVersion <= 3) {
                        longName = Util.byte2str(globalBuffer.getStringByte(), fileEncoding);
                    }
                    SFTPAttrs attrs = SFTPAttrs.getATTR(globalBuffer);

                    final int nameLen = fileNameB.length;
                    if ((nameLen == 1 && fileNameB[0] == '.') || (nameLen == 2 && fileNameB[0] == '.' && fileNameB[1] == '.')) {
                        continue;
                    }

                    if (cancel == LsEntrySelector.END) {
                        continue;
                    }

                    String fileName = Util.byte2str(fileNameB, fileEncoding);

                    SFTPFile SFTPFile = new SFTPFile(fileName, longName, absPath, attrs);
                    cancel = selector.select(SFTPFile);
                }
            }
            sendCloseHandleAndCheck(handle, header);
        } catch (Exception e) {
            if (e instanceof SftpException) {
                throw (SftpException) e;
            } else {
                throw (new SftpException(SSH_FX_FAILURE, "", e));
            }
        }
    }

    public String readLink(String linkPath) throws Exception {
        if (serverVersion < 3) {
            throw new UnsupportedOperationException("服务器SFTP协议版本<3，不支持readLink()");
        }
        inputStream.updateReadSide();

        sendREADLINK(Util.str2byte(linkPath, fileEncoding));

        Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;
        fill(globalBuffer, length);

        if (type != SSH_FXP_NAME) throw new SftpException(SSH_FX_FAILURE, "readLink失败");

        int count = globalBuffer.readInt();
        if (count != 1) return null;
        String relative = Util.byte2str(globalBuffer.getStringByte(), fileEncoding);
//        for (int i = 0; i < count; i++) {
//            String relative = Util.byte2str(buffer.getStringByte(), fileEncoding);
//            System.out.println(relative);
//            String absPath = PathUtil.getAbsPath(PathUtil.getPathParent(path), relative);
//            if (serverVersion <= 3) {
//                byte[] longName = buffer.getStringByte();
//            }
//            file = new FTPFile(absPath, attrs);
//            break;
//        }
        // FTPFile file = new FTPFile(Util.byte2str(filename, fileEncoding), null, )
        return relative;
    }

    public void symlink(String oldpath, String newpath) throws Exception {
        if (serverVersion < 3) {
            throw new UnsupportedOperationException("服务器SFTP协议版本<3, 不支持symlink()操作.");
        }

        try {
            inputStream.updateReadSide();

            String _oldpath = remoteAbsolutePath(oldpath);
            newpath = remoteAbsolutePath(newpath);

            _oldpath = isUnique(_oldpath);
            if (oldpath.charAt(0) != '/') { // relative path
                String cwd = getCwd();
                oldpath = _oldpath.substring(cwd.length() + (cwd.endsWith("/") ? 0 : 1));
            } else {
                oldpath = _oldpath;
            }

            if (isPattern(newpath)) {
                throw new SftpException(SSH_FX_FAILURE, newpath);
            }
            newpath = Util.unquote(newpath);

            sendSYMLINK(Util.str2byte(oldpath, fileEncoding),
                    Util.str2byte(newpath, fileEncoding));

            Header header = new Header();
            readHeader(globalBuffer, header);
            int length = header.length;
            int type = header.type;

            fill(globalBuffer, length);

            if (type != SSH_FXP_STATUS) {
                throw new SftpException(SSH_FX_FAILURE, "");
            }

            int i = globalBuffer.readInt();
            if (i == SSH_FX_OK) return;
            throwStatusError(globalBuffer, i);
        } catch (Exception e) {
            if (e instanceof SftpException) throw (SftpException) e;
            throw new SftpException(SSH_FX_FAILURE, "", e);
        }
    }

    public synchronized void hardlink(String oldpath, String newpath) throws Exception {
        if (!extension_hardlink) {
            throw new UnsupportedOperationException("不支持硬连接操作，没有收到拓展字符‘hardlink@openssh.com’");
        }
        inputStream.updateReadSide();
        String _oldpath = remoteAbsolutePath(oldpath);
        newpath = remoteAbsolutePath(newpath);

        _oldpath = isUnique(_oldpath);
        if (oldpath.charAt(0) != '/') { // relative path
            String cwd = getCwd();
            oldpath = _oldpath.substring(cwd.length() + (cwd.endsWith("/") ? 0 : 1));
        } else {
            oldpath = _oldpath;
        }

        if (isPattern(newpath)) {
            throw new SftpException(SSH_FX_FAILURE, newpath);
        }
        newpath = Util.unquote(newpath);

        sendHARDLINK(Util.str2byte(oldpath, fileEncoding),
                Util.str2byte(newpath, fileEncoding));

        Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_STATUS) {
            throw new SftpException(SSH_FX_FAILURE, "");
        }

        int i = globalBuffer.readInt();
        if (i == SSH_FX_OK) return;
        throwStatusError(globalBuffer, i);

    }

    public synchronized void rename(String oldpath, String newpath) throws Exception {
        if (serverVersion < 2) {
            throw new UnsupportedOperationException("服务器SFTP协议版本<2，不支持重命名");
        }
        inputStream.updateReadSide();

        oldpath = remoteAbsolutePath(oldpath);
        newpath = remoteAbsolutePath(newpath);
        oldpath = isUnique(oldpath);

        Vector v = glob_remote(newpath);
        int vsize = v.size();
        if (vsize >= 2) {
            throw new SftpException(SSH_FX_FAILURE, v.toString());
        }
        if (vsize == 1) {
            newpath = (String) (v.elementAt(0));
        } else {  // vsize==0
            if (isPattern(newpath))
                throw new SftpException(SSH_FX_FAILURE, newpath);
            newpath = Util.unquote(newpath);
        }

        sendRENAME(Util.str2byte(oldpath, fileEncoding),
                Util.str2byte(newpath, fileEncoding));

        Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_STATUS) {
            throw new SftpException(SSH_FX_FAILURE, "");
        }

        int i = globalBuffer.readInt();
        if (i == SSH_FX_OK) return;
        throwStatusError(globalBuffer, i);

    }

    /**
     * 删除文件，不能删除文件夹
     */
    public void rm(String path) throws Exception {
        inputStream.updateReadSide();
        path = remoteAbsolutePath(path);

//        Vector v = glob_remote(path);
//        int vsize = v.size();

        sendREMOVE(Util.str2byte(path, fileEncoding));

        Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_STATUS) {
            throw new SftpException(SSH_FX_FAILURE, "rm()" + path + "失败");
        }
        int i = globalBuffer.readInt();
        if (i != SSH_FX_OK) {
            throwStatusError(globalBuffer, i);
        }
    }


    /**
     * 删除空文件夹
     */
    public synchronized void rmdir(String path) throws Exception {
        inputStream.updateReadSide();
        path = remoteAbsolutePath(path);

        Header header = new Header();

        sendRMDIR(Util.str2byte(path, fileEncoding));

        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_STATUS) {
            throw new SftpException(SSH_FX_FAILURE, "");
        }

        int statusCode = globalBuffer.readInt();
        if (statusCode != SSH_FX_OK) {
            throwStatusError(globalBuffer, statusCode);
        }
    }

    public synchronized void createNewFile(String path) throws Exception {
        inputStream.updateReadSide();
        path = remoteAbsolutePath(path);
        sendOpenCreate(Util.str2byte(path, fileEncoding));
        Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_HANDLE) {
            throw new SftpException(SSH_FX_FAILURE, "");
        }

        byte[] handle = globalBuffer.getStringByte();         // handle
        sendCloseHandleAndCheck(handle, header);
    }

    public synchronized void mkdir(String path) throws Exception {
        inputStream.updateReadSide();
        path = remoteAbsolutePath(path);
        sendMKDIR(Util.str2byte(path, fileEncoding), null);
        Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_STATUS) {
            throw new SftpException(SSH_FX_FAILURE, "");
        }

        int i = globalBuffer.readInt();
        if (i == SSH_FX_OK) return;
        throwStatusError(globalBuffer, i);
    }

    private boolean isRemoteDir(String path) {
        try {
            sendSTAT(Util.str2byte(path, fileEncoding));

            Header header = new Header();
            readHeader(globalBuffer, header);
            int length = header.length;
            int type = header.type;

            fill(globalBuffer, length);

            if (type != SSH_FXP_ATTRS) {
                return false;
            }
            SFTPAttrs attr = SFTPAttrs.getATTR(globalBuffer);
            return attr.isDir();
        } catch (Exception ignored) {
        }
        return false;
    }

    public void chgid(String path, int gid) throws Exception {
        inputStream.updateReadSide();
        path = remoteAbsolutePath(path);

        SFTPAttrs attr = _stat(path);

        attr.setFlags(0);
        attr.setUidAndGid(attr.uid, gid);
        _setStat(path, attr);
    }

    public void chown(String path, int uid) throws Exception {
        inputStream.updateReadSide();
        path = remoteAbsolutePath(path);
        SFTPAttrs attr = _stat(path);
        attr.setFlags(0);
        attr.setUidAndGid(uid, attr.gid);
        _setStat(path, attr);
    }

    public void chmod(String path, int permissions) throws Exception {
        inputStream.updateReadSide();

        path = remoteAbsolutePath(path);
        SFTPAttrs attrs = new SFTPAttrs();
        attrs.setPermissions(permissions);
        _setStat(path, attrs);
    }

    public void setMtime(String path, int mtime) throws Exception {
        inputStream.updateReadSide();
        path = remoteAbsolutePath(path);

        Vector v = glob_remote(path);
        int vsize = v.size();
        for (int j = 0; j < vsize; j++) {
            path = (String) (v.elementAt(j));

            SFTPAttrs attr = _stat(path);

            attr.setFlags(0);
            attr.setACMODTIME(attr.getATime(), mtime);
            _setStat(path, attr);
        }
    }

    /**
     * 跟随符号链接
     */
    public SFTPAttrs stat(String path) throws Exception {
        inputStream.updateReadSide();

        path = remoteAbsolutePath(path);
        path = isUnique(path);

        return _stat(path);
    }


    private SFTPAttrs _stat(String path) throws Exception {
        return _stat(Util.str2byte(path, fileEncoding));
    }

    private synchronized SFTPAttrs _stat(byte[] path) throws Exception {
        sendSTAT(path);
        Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_ATTRS) {
            if (type == SSH_FXP_STATUS) {
                int i = globalBuffer.readInt();
                throwStatusError(globalBuffer, i);
            }
            throw new SftpException(SSH_FX_FAILURE, "_stat()未知错误");
        }
        return SFTPAttrs.getATTR(globalBuffer);
    }


    public SftpStatVFS statVFS(String path) throws SftpException {
        try {
            inputStream.updateReadSide();

            path = remoteAbsolutePath(path);
            path = isUnique(path);

            return _statVFS(path);
        } catch (Exception e) {
            if (e instanceof SftpException) throw (SftpException) e;
            throw new SftpException(SSH_FX_FAILURE, "", e);
        }
        //return null;
    }

    private SftpStatVFS _statVFS(byte[] path) throws SftpException {
        if (!extension_statvfs) {
            throw new SftpException(SSH_FX_OP_UNSUPPORTED,
                    "statvfs@openssh.com is not supported");
        }

        try {

            sendSTATVFS(path);

            Header header = new Header();
            readHeader(globalBuffer, header);
            int length = header.length;
            int type = header.type;

            fill(globalBuffer, length);

            if (type != (SSH_FXP_EXTENDED_REPLY & 0xff)) {
                if (type == SSH_FXP_STATUS) {
                    int i = globalBuffer.readInt();
                    throwStatusError(globalBuffer, i);
                }
                throw new SftpException(SSH_FX_FAILURE, "");
            } else {
                SftpStatVFS stat = SftpStatVFS.getStatVFS(globalBuffer);
                return stat;
            }
        } catch (Exception e) {
            if (e instanceof SftpException) throw (SftpException) e;
            throw new SftpException(SSH_FX_FAILURE, "", e);
        }
        //return null;
    }

    private SftpStatVFS _statVFS(String path) throws SftpException {
        return _statVFS(Util.str2byte(path, fileEncoding));
    }

    public SFTPAttrs lstat(String path) throws SftpException {
        try {
            inputStream.updateReadSide();
            path = remoteAbsolutePath(path);
            path = isUnique(path);
            return _lstat(path);
        } catch (Exception e) {
            if (e instanceof SftpException) throw (SftpException) e;
            throw new SftpException(SSH_FX_FAILURE, "", e);
        }
    }

    private synchronized SFTPAttrs _lstat(String path) throws SftpException {
        try {
            sendLSTAT(Util.str2byte(path, fileEncoding));

            Header header = new Header();
            readHeader(globalBuffer, header);
            int length = header.length;
            int type = header.type;

            fill(globalBuffer, length);

            if (type != SSH_FXP_ATTRS) {
                if (type == SSH_FXP_STATUS) {
                    int i = globalBuffer.readInt();
                    throwStatusError(globalBuffer, i);
                }
                throw new SftpException(SSH_FX_FAILURE, "");
            }
            return SFTPAttrs.getATTR(globalBuffer);
        } catch (Exception e) {
            if (e instanceof SftpException) throw (SftpException) e;
            throw new SftpException(SSH_FX_FAILURE, "", e);
        }
    }

    private synchronized byte[] _realPath(String path) throws Exception {
        sendREALPATH(Util.str2byte(path, fileEncoding));

        Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_STATUS && type != SSH_FXP_NAME) {
            throw new SftpException(SSH_FX_FAILURE, "");
        }
        int i;
        if (type == SSH_FXP_STATUS) {
            i = globalBuffer.readInt();
            throwStatusError(globalBuffer, i);
        }
        i = globalBuffer.readInt();   // count

        byte[] str = null;
        while (i-- > 0) {
            str = globalBuffer.getStringByte();  // absolute path;
            if (serverVersion <= 3) {
                byte[] lname = globalBuffer.getStringByte();  // long filename
            }
            SFTPAttrs attr = SFTPAttrs.getATTR(globalBuffer);  // dummy attribute
        }
        return str;
    }

    public void setStat(String path, SFTPAttrs attr) throws Exception {
        inputStream.updateReadSide();
        path = remoteAbsolutePath(path);
        _setStat(path, attr);
    }

    private void _setStat(String path, SFTPAttrs attr) throws Exception {
        sendSETSTAT(Util.str2byte(path, fileEncoding), attr);

        Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_STATUS) {
            throw new SftpException(SSH_FX_FAILURE, "");
        }
        int i = globalBuffer.readInt();
        if (i != SSH_FX_OK) {
            throwStatusError(globalBuffer, i);
        }
    }

    public String pwd() throws SftpException {
        return getCwd();
    }

//    public String lpwd() {
//        return lcwd;
//    }

//    public String version() {
//        return version;
//    }

    public String getHome() throws SftpException {
        if (home == null) {
            throw new SftpException(SSH_FX_FAILURE, "getHome() ");
//            try {
//                inputStream.updateReadSide();
//                byte[] _home = _realPath("");
//                home = Util.byte2str(_home, fileEncoding);
//            } catch (Exception e) {
//                if (e instanceof SftpException) throw (SftpException) e;
//                throw new SftpException(SSH_FX_FAILURE, "", e);
//            }
        }
        return home;
    }

    private String getCwd() throws SftpException {
        if (cwd == null)
            cwd = getHome();
        return cwd;
    }

    private void setCwd(String cwd) {
        this.cwd = cwd;
    }

    private void read(byte[] buf, int s, int l) throws IOException, SftpException {
        int i = 0;
        while (l > 0) {
            i = inputStream.read(buf, s, l);
            if (i <= 0) {
                throw new SftpException(SSH_FX_FAILURE, "");
            }
            s += i;
            l -= i;
        }
    }

    private boolean checkStatus(int[] ackId, Header header) throws Exception {
        readHeader(globalBuffer, header);
        final int length = header.length;
        final int type = header.type;
        if (ackId != null) ackId[0] = header.responseId;

        fill(globalBuffer, length);

        if (type != SSH_FXP_STATUS) {
            throw new SftpException(SSH_FX_FAILURE, "");
        }
        int i = globalBuffer.readInt();
        if (i != SSH_FX_OK) {
            throwStatusError(globalBuffer, i);
        }
        return true;
    }

    private boolean sendCloseHandleAndCheck(byte[] handle, Header header) throws Exception {
        sendPacketPath(SSH_FXP_CLOSE, handle);
        return checkStatus(null, header);
    }

    private void sendINIT() throws Exception {
        packet.reset();
        putHEAD(SSH_FXP_INIT, 5);
        globalBuffer.putInt(CLIENT_VERSION);// version 3
        getSession().write(packet, this, 5 + 4);
    }

    private void sendREALPATH(byte[] path) throws Exception {
        sendPacketPath(SSH_FXP_REALPATH, path);
    }

    private void sendSTAT(byte[] path) throws Exception {
        sendPacketPath(SSH_FXP_STAT, path);
    }

    private void sendSTATVFS(byte[] path) throws Exception {
        sendPacketPath((byte) 0, path, "statvfs@openssh.com");
    }

    /*
  private void sendFSTATVFS(byte[] handle) throws Exception{
    sendPacketPath((byte)0, handle, "fstatvfs@openssh.com");
  }
  */
    private void sendLSTAT(byte[] path) throws Exception {
        sendPacketPath(SSH_FXP_LSTAT, path);
    }

    private void sendFSTAT(byte[] handle) throws Exception {
        sendPacketPath(SSH_FXP_FSTAT, handle);
    }

    private void sendSETSTAT(byte[] path, SFTPAttrs attr) throws Exception {
        packet.reset();
        putHEAD(SSH_FXP_SETSTAT, 9 + path.length + attr.length());
        globalBuffer.putInt(requestSequence++);
        globalBuffer.putString(path);             // path
        attr.dump(globalBuffer);
        getSession().write(packet, this, 9 + path.length + attr.length() + 4);
    }

    private void sendREMOVE(byte[] path) throws Exception {
        sendPacketPath(SSH_FXP_REMOVE, path);
    }

    private synchronized void sendMKDIR(byte[] path, SFTPAttrs attr) throws Exception {
        packet.reset();
        putHEAD(SSH_FXP_MKDIR, 9 + path.length + (attr != null ? attr.length() : 4));
        globalBuffer.putInt(requestSequence++);
        globalBuffer.putString(path);             // path
        if (attr != null) attr.dump(globalBuffer);
        else globalBuffer.putInt(0);
        getSession().write(packet, this, 9 + path.length + (attr != null ? attr.length() : 4) + 4);
    }

    private void sendRMDIR(byte[] path) throws Exception {
        sendPacketPath(SSH_FXP_RMDIR, path);
    }

    private void sendSYMLINK(byte[] p1, byte[] p2) throws Exception {
        sendPacketPath(SSH_FXP_SYMLINK, p1, p2);
    }

    private void sendHARDLINK(byte[] p1, byte[] p2) throws Exception {
        sendPacketPath((byte) 0, p1, p2, "hardlink@openssh.com");
    }

    private void sendREADLINK(byte[] path) throws Exception {
        sendPacketPath(SSH_FXP_READLINK, path);
    }

    private void sendOPENDIR(byte[] path) throws Exception {
        sendPacketPath(SSH_FXP_OPENDIR, path);
    }

    private void sendREADDIR(byte[] path) throws Exception {
        sendPacketPath(SSH_FXP_READDIR, path);
    }

    private void sendRENAME(byte[] p1, byte[] p2) throws Exception {
        sendPacketPath(SSH_FXP_RENAME, p1, p2,
                extension_posix_rename ? "posix-rename@openssh.com" : null);
    }


    private void sendOpenCreate(byte[] path) throws Exception {
        sendOPen(path, SSH_FXF_CREAT | SSH_FXF_EXCL);
    }

    private void sendOpenCreateAndWrite(byte[] path) throws Exception {
        sendOPen(path, SSH_FXF_CREAT | SSH_FXF_EXCL | SSH_FXF_WRITE);
    }

    private void sendOpenRead(byte[] path) throws Exception {
        sendOPen(path, SSH_FXF_READ);
    }

    private void sendOpenWrite(byte[] path) throws Exception {
        sendOPen(path, SSH_FXF_CREAT | SSH_FXF_WRITE | SSH_FXF_TRUNC);
    }

    private void sendOpenAppend(byte[] path) throws Exception {
        sendOPen(path, SSH_FXF_WRITE |/*SSH_FXF_APPEND|*/SSH_FXF_CREAT);
    }

    private void sendClose(byte[] handle) throws Exception {
        sendPacketPath(SSH_FXP_CLOSE, handle);
    }

    private synchronized void sendOPen(byte[] path, int mode) throws Exception {
        packet.reset();
        putHEAD(SSH_FXP_OPEN, 17 + path.length);
        globalBuffer.putInt(requestSequence++);
        globalBuffer.putString(path);
        globalBuffer.putInt(mode);
        globalBuffer.putInt(0);           // attrs
        getSession().write(packet, this, 17 + path.length + 4);
    }

    private void sendPacketPath(byte fxp, byte[] path) throws Exception {
        sendPacketPath(fxp, path, (String) null);
    }

    private synchronized void sendPacketPath(byte fxp, byte[] path, String extension) throws Exception {
        packet.reset();
        int len = 9 + path.length;
        if (extension == null) {
            putHEAD(fxp, len);
            globalBuffer.putInt(requestSequence++);
        } else {
            len += (4 + extension.length());
            putHEAD(SSH_FXP_EXTENDED, len);
            globalBuffer.putInt(requestSequence++);
            globalBuffer.putString(Util.str2byte(extension));
        }
        globalBuffer.putString(path);             // path
        getSession().write(packet, this, len + 4);
    }

    private void sendPacketPath(byte fxp, byte[] p1, byte[] p2) throws Exception {
        sendPacketPath(fxp, p1, p2, null);
    }

    private synchronized void sendPacketPath(byte fxp, byte[] p1, byte[] p2, String extension) throws Exception {
        packet.reset();
        int len = 13 + p1.length + p2.length;
        if (extension == null) {
            putHEAD(fxp, len);
            globalBuffer.putInt(requestSequence++);
        } else {
            len += (4 + extension.length());
            putHEAD(SSH_FXP_EXTENDED, len);
            globalBuffer.putInt(requestSequence++);
            globalBuffer.putString(Util.str2byte(extension));
        }
        globalBuffer.putString(p1);
        globalBuffer.putString(p2);
        getSession().write(packet, this, len + 4);
    }

    private synchronized int sendWrite(byte[] handle, long offset,
                                       byte[] data, int start, int length) throws Exception {
        int _length = length;
        opacket.reset();
        if (obuf.data.length < obuf.indexWrite + 13 + 21 + handle.length + length + Session.buffer_margin) {
            _length = obuf.data.length - (obuf.indexWrite + 13 + 21 + handle.length + Session.buffer_margin);
            // System.err.println("_length="+_length+" length="+length);
        }

        putHEAD(obuf, SSH_FXP_WRITE, 21 + handle.length + _length);       // 14
        obuf.putInt(requestSequence++);                                      //  4
        obuf.putString(handle);                                  //  4+handle.length
        obuf.putLong(offset);                                    //  8
        if (obuf.data != data) {
            obuf.putString(data, start, _length);                    //  4+_length
        } else {
            obuf.putInt(_length);
            obuf.skipWrite(_length);
        }
        getSession().write(opacket, this, 21 + handle.length + _length + 4);
        return _length;
    }

    private void sendREAD(byte[] handle, long offset, int length) throws Exception {
        sendREAD(handle, offset, length, null);
    }

    private synchronized void sendREAD(byte[] handle, long offset, int length,
                                       RequestQueue rrq) throws Exception {
        packet.reset();
        putHEAD(SSH_FXP_READ, 21 + handle.length);
        globalBuffer.putInt(requestSequence++);
        globalBuffer.putString(handle);
        globalBuffer.putLong(offset);
        globalBuffer.putInt(length);
        getSession().write(packet, this, 21 + handle.length + 4);
        if (rrq != null) {
            rrq.add(requestSequence - 1, offset, length);
        }
    }

    private void putHEAD(Buffer buffer, byte type, int length) {
        buffer.putByte((byte) SSH_MSG_CHANNEL_DATA);
        buffer.putInt(recipientId);
        buffer.putInt(length + 4);
        buffer.putInt(length);
        buffer.putByte(type);
    }

    private void putHEAD(byte type, int length) {
        putHEAD(globalBuffer, type, length);
    }

    private Vector<String> glob_remote(String path) throws Exception {
        Vector<String> v = new Vector<>();
        int i = 0;
        int foo = path.lastIndexOf('/');
        if (foo < 0) {  // it is not absolute path.
            v.addElement(Util.unquote(path));
            return v;
        }

        String dir = path.substring(0, ((foo == 0) ? 1 : foo));
        String _pattern = path.substring(foo + 1);

        dir = Util.unquote(dir);

        byte[] pattern = null;
        byte[][] _pattern_utf8 = new byte[1][];
        boolean pattern_has_wildcard = isPattern(_pattern, _pattern_utf8);

        if (!pattern_has_wildcard) {
            if (!dir.equals("/"))
                dir += "/";
            v.addElement(dir + Util.unquote(_pattern));
            return v;
        }

        pattern = _pattern_utf8[0];

        sendOPENDIR(Util.str2byte(dir, fileEncoding));

        Header header = new Header();
        readHeader(globalBuffer, header);
        int length = header.length;
        int type = header.type;

        fill(globalBuffer, length);

        if (type != SSH_FXP_STATUS && type != SSH_FXP_HANDLE) {
            throw new SftpException(SSH_FX_FAILURE, "");
        }
        if (type == SSH_FXP_STATUS) {
            i = globalBuffer.readInt();
            throwStatusError(globalBuffer, i);
        }

        byte[] handle = globalBuffer.getStringByte();         // filename
        String pdir = null;                      // parent directory

        while (true) {
            sendREADDIR(handle);
            readHeader(globalBuffer, header);
            length = header.length;
            type = header.type;

            if (type != SSH_FXP_STATUS && type != SSH_FXP_NAME) {
                throw new SftpException(SSH_FX_FAILURE, "");
            }
            if (type == SSH_FXP_STATUS) {
                fill(globalBuffer, length);
                break;
            }

            globalBuffer.rewind();
            fill(globalBuffer.data, 0, 4);
            length -= 4;
            int count = globalBuffer.readInt();

            byte[] str;
            int flags;

            globalBuffer.reset();
            while (count > 0) {
                if (length > 0) {
                    globalBuffer.shift();
                    int j = (globalBuffer.data.length > (globalBuffer.indexWrite + length)) ? length : (globalBuffer.data.length - globalBuffer.indexWrite);
                    i = inputStream.read(globalBuffer.data, globalBuffer.indexWrite, j);
                    if (i <= 0) break;
                    globalBuffer.indexWrite += i;
                    length -= i;
                }

                byte[] filename = globalBuffer.getStringByte();
                //System.err.println("filename: "+new String(filename));
                if (serverVersion <= 3) {
                    str = globalBuffer.getStringByte();  // longname
                }
                SFTPAttrs attrs = SFTPAttrs.getATTR(globalBuffer);

                byte[] _filename = filename;
                String f = null;
                boolean found = false;

                if (!fileEncodingIsUTF8) {
                    f = Util.byte2str(filename, fileEncoding);
                    _filename = Util.str2byte(f, UTF8);
                }
                found = Util.glob(pattern, _filename);

                if (found) {
                    if (f == null) {
                        f = Util.byte2str(filename, fileEncoding);
                    }
                    if (pdir == null) {
                        pdir = dir;
                        if (!pdir.endsWith("/")) {
                            pdir += "/";
                        }
                    }
                    v.addElement(pdir + f);
                }
                count--;
            }
        }
        if (sendCloseHandleAndCheck(handle, header))
            return v;
        return null;
    }

    private boolean isPattern(byte[] path) {
        int length = path.length;
        int i = 0;
        while (i < length) {
            if (path[i] == '*' || path[i] == '?')
                return true;
            if (path[i] == '\\' && (i + 1) < length)
                i++;
            i++;
        }
        return false;
    }

    private Vector glob_local(String _path) {
//System.err.println("glob_local: "+_path);
        Vector<String> v = new Vector<>();
        byte[] path = Util.str2byte(_path, UTF8);
        int i = path.length - 1;
        while (i >= 0) {
            if (path[i] != '*' && path[i] != '?') {
                i--;
                continue;
            }
            if (!fs_is_bs &&
                    i > 0 && path[i - 1] == '\\') {
                i--;
                if (i > 0 && path[i - 1] == '\\') {
                    i--;
                    i--;
                    continue;
                }
            }
            break;
        }

        if (i < 0) {
            v.addElement(fs_is_bs ? _path : Util.unquote(_path));
            return v;
        }

        while (i >= 0) {
            if (path[i] == file_separatorc ||
                    (fs_is_bs && path[i] == '/')) { // On Windows, '/' is also the separator.
                break;
            }
            i--;
        }

        if (i < 0) {
            v.addElement(fs_is_bs ? _path : Util.unquote(_path));
            return v;
        }

        byte[] dir;
        if (i == 0) {
            dir = new byte[]{(byte) file_separatorc};
        } else {
            dir = new byte[i];
            System.arraycopy(path, 0, dir, 0, i);
        }

        byte[] pattern = new byte[path.length - i - 1];
        System.arraycopy(path, i + 1, pattern, 0, pattern.length);

//System.err.println("dir: "+new String(dir)+" pattern: "+new String(pattern));
        try {
            String[] children = (new File(Util.byte2str(dir, UTF8))).list();
            String pdir = Util.byte2str(dir) + file_separator;
            for (int j = 0; j < children.length; j++) {
//System.err.println("children: "+children[j]);
                if (Util.glob(pattern, Util.str2byte(children[j], UTF8))) {
                    v.addElement(pdir + children[j]);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return v;
    }

    private void throwStatusError(Buffer buffer, int statusCode) throws SftpException {
        if (serverVersion >= 3 &&   // WindRiver's sftp will send invalid
                buffer.getLength() >= 4) {   // SSH_FXP_STATUS packet.
            byte[] str = buffer.getStringByte();// error message
            //byte[] tag=buf.getString();
            throw new SftpException(statusCode, Util.byte2str(str, UTF8));
        } else {
            throw new SftpException(statusCode, "Failure");
        }
    }

    private static boolean isLocalAbsolutePath(String path) {
        return (new File(path)).isAbsolute();
    }
//
//    public void disconnect() {
//        super.disconnect();
//    }

    private boolean isPattern(String path, byte[][] utf8) {
        byte[] _path = Util.str2byte(path, UTF8);
        if (utf8 != null)
            utf8[0] = _path;
        return isPattern(_path);
    }

    private boolean isPattern(String path) {
        return isPattern(path, null);
    }

    private void fill(Buffer buffer, int len) throws IOException {
        buffer.reset();
        fill(buffer.data, 0, len);
        buffer.skipWrite(len);
    }

    private int fill(byte[] data, int offset, int len) throws IOException {
        int read;
        int foo = offset;
        while (len > 0) {
            read = inputStream.read(data, offset, len);
            if (read == -1) {
                throw new IOException("inputstream is closed");
                //return (offset-foo)==0 ? i : offset-foo;
            }
            offset += read;
            len -= read;
        }
        return offset - foo;
    }

    private void skip(long size) throws IOException {
        while (size > 0) {
            long bar = inputStream.skip(size);
            if (bar <= 0)
                break;
            size -= bar;
        }
    }

    static class Header {
        int length;// payload length
        int type;
        int responseId;
    }

    private void readHeader(Buffer buffer, Header header) throws IOException {
        buffer.rewind();
        int i = fill(buffer.data, 0, 9);
        header.length = buffer.readInt() - 5;
        header.type = buffer.readByte() & 0xff;
        header.responseId = buffer.readInt();
    }

    private String remoteAbsolutePath(String path) throws Exception {
        if (path.charAt(0) == '/') return path;
        throw new Exception("remoteAbsolutePath不是绝对路径");
//        String cwd = getCwd();
////    if(cwd.equals(getHome())) return path;
//        if (cwd.endsWith("/")) return cwd + path;
//        return cwd + "/" + path;
    }

    /**
     * This method will check if the given string can be expanded to the
     * unique string.  If it can be expanded to mutiple files, SftpException
     * will be thrown.
     *
     * @return the returned string is unquoted.
     */
    private String isUnique(String path) throws Exception {
        Vector v = glob_remote(path);
        if (v == null || v.size() != 1) {
            throw new SftpException(SSH_FX_FAILURE, path + " is not unique: " + v.toString());
        }
        return (String) (v.elementAt(0));
    }

    public int getServerVersion() {
        return serverVersion;
    }

    public void setFilenameEncoding(String encoding) throws SftpException {
        int serverVersion = getServerVersion();
        if (3 <= serverVersion && serverVersion <= 5 &&
                !encoding.equals(UTF8)) {
            throw new SftpException(SSH_FX_FAILURE, "The encoding can not be changed for this sftp server.");
        }
        if (encoding.equals(UTF8)) {
            encoding = UTF8;
        }
        fileEncoding = encoding;
        fileEncodingIsUTF8 = fileEncoding.equals(UTF8);
    }

    public String getExtension(String key) {
        if (extensions == null)
            return null;
        return extensions.get(key);
    }

    /**
     * 使服务器将任何给定的路径名规范化为绝对路径
     */
    public String realPath(String path) throws Exception {
        return Util.byte2str(_realPath(remoteAbsolutePath(path)), fileEncoding);
    }

    public interface LsEntrySelector {
        public final int CONTINUE = 0;
        public final int END = 1;

        /**
         * <p> The <code>select</code> method will be invoked inputStream <code>ls</code>
         * method for each file entry. If this method returns END,
         * <code>ls</code> will be canceled.
         *
         * @param file one of entry from ls
         * @return if END is returned, the 'ls' operation will be canceled.
         */
        public int select(SFTPFile file);
    }
}
