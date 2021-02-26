package com.jcraft.jsch;

public class UserAuthGSSAPIWithMIC extends UserAuth {
    private static final int SSH_MSG_USERAUTH_GSSAPI_RESPONSE = 60;
    private static final int SSH_MSG_USERAUTH_GSSAPI_TOKEN = 61;
    private static final int SSH_MSG_USERAUTH_GSSAPI_EXCHANGE_COMPLETE = 63;
    private static final int SSH_MSG_USERAUTH_GSSAPI_ERROR = 64;
    private static final int SSH_MSG_USERAUTH_GSSAPI_ERRTOK = 65;
    private static final int SSH_MSG_USERAUTH_GSSAPI_MIC = 66;

    private static final byte[][] supported_oid = {
            // OID 1.2.840.113554.1.2.2 inputStream DER
            {(byte) 0x6, (byte) 0x9, (byte) 0x2a, (byte) 0x86, (byte) 0x48,
                    (byte) 0x86, (byte) 0xf7, (byte) 0x12, (byte) 0x1, (byte) 0x2,
                    (byte) 0x2}
    };

    private static final String[] supported_method = {
            "gssapi-with-mic.krb5"
    };

    public boolean start(Session session) throws Exception {
        super.start(session);
//
//        byte[] _username = Util.str2byte(username);
//
//        packet.reset();
//
//        // byte            SSH_MSG_USERAUTH_REQUEST(50)
//        // string          user name(inputStream ISO-10646 UTF-8 encoding)
//        // string          service name(inputStream US-ASCII)
//        // string          "gssapi"(US-ASCII)
//        // uint32          n, the number of OIDs client supports
//        // string[n]       mechanism OIDS
//        buffer.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
//        buffer.putString(_username);
//        buffer.putString(Util.str2byte("ssh-connection"));
//        buffer.putString(Util.str2byte("gssapi-with-mic"));
//        buffer.putInt(supported_oid.length);
//        for (int i = 0; i < supported_oid.length; i++) {
//            buffer.putString(supported_oid[i]);
//        }
//        session.write(packet);
//
//        String method = null;
//        int command;
//        while (true) {
//            buffer = session.read(buffer);
//            command = buffer.getCommand() & 0xff;
//
//            if (command == SSH_MSG_USERAUTH_FAILURE) {
//                return false;
//            }
//
//            if (command == SSH_MSG_USERAUTH_GSSAPI_RESPONSE) {
//                buffer.getInt();
//                buffer.getByte();
//                buffer.getByte();
//                byte[] message = buffer.getStringByte();
//
//                for (int i = 0; i < supported_oid.length; i++) {
//                    if (Util.array_equals(message, supported_oid[i])) {
//                        method = supported_method[i];
//                        break;
//                    }
//                }
//
//                if (method == null) {
//                    return false;
//                }
//
//                break; // success
//            }
//
//            if (command == SSH_MSG_USERAUTH_BANNER) {
//                buffer.getInt();
//                buffer.getByte();
//                buffer.getByte();
//                byte[] _message = buffer.getStringByte();
//                byte[] lang = buffer.getStringByte();
//                if (listener != null) {
//                    listener.onReceiveBannerMessage(Util.byte2str(_message));
//                }
//                continue;
//            }
//            return false;
//        }
//
//        GSSContext context = null;
//        try {
//            Class c = Class.forName(session.getConfig(method));
//            context = (GSSContext) (c.newInstance());
//        } catch (Exception e) {
//            return false;
//        }
//
//        try {
//            context.create(username, session.host);
//        } catch (JSchException e) {
//            return false;
//        }
//
//        byte[] token = new byte[0];
//
//        while (!context.isEstablished()) {
//            try {
//                token = context.init(token, 0, token.length);
//            } catch (JSchException e) {
//                // TODO
//                // ERRTOK should be sent?
//                // byte        SSH_MSG_USERAUTH_GSSAPI_ERRTOK
//                // string      error token
//                return false;
//            }
//
//            if (token != null) {
//                packet.reset();
//                buffer.putByte((byte) SSH_MSG_USERAUTH_GSSAPI_TOKEN);
//                buffer.putString(token);
//                session.write(packet);
//            }
//
//            if (!context.isEstablished()) {
//                buffer = session.read(buffer);
//                command = buffer.getCommand() & 0xff;
//                if (command == SSH_MSG_USERAUTH_GSSAPI_ERROR) {
//                    // uint32    major_status
//                    // uint32    minor_status
//                    // string    message
//                    // string    language tag
//
//                    buffer = session.read(buffer);
//                    command = buffer.getCommand() & 0xff;
//                    //return false;
//                } else if (command == SSH_MSG_USERAUTH_GSSAPI_ERRTOK) {
//                    // string error token
//
//                    buffer = session.read(buffer);
//                    command = buffer.getCommand() & 0xff;
//                    //return false;
//                }
//
//                if (command == SSH_MSG_USERAUTH_FAILURE) {
//                    return false;
//                }
//
//                buffer.getInt();
//                buffer.getByte();
//                buffer.getByte();
//                token = buffer.getStringByte();
//            }
//        }
//
//        Buffer mbuf = new Buffer();
//        // string    session identifier
//        // byte      SSH_MSG_USERAUTH_REQUEST
//        // string    user name
//        // string    service
//        // string    "gssapi-with-mic"
//        mbuf.putString(session.getSessionId());
//        mbuf.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
//        mbuf.putString(_username);
//        mbuf.putString(Util.str2byte("ssh-connection"));
//        mbuf.putString(Util.str2byte("gssapi-with-mic"));
//
//        byte[] mic = context.getMIC(mbuf.data, 0, mbuf.getLength());
//
//        if (mic == null) {
//            return false;
//        }
//
//        packet.reset();
//        buffer.putByte((byte) SSH_MSG_USERAUTH_GSSAPI_MIC);
//        buffer.putString(mic);
//        session.write(packet);
//
//        context.dispose();
//
//        buffer = session.read(buffer);
//        command = buffer.getCommand() & 0xff;
//
//        if (command == SSH_MSG_USERAUTH_SUCCESS) {
//            return true;
//        } else if (command == SSH_MSG_USERAUTH_FAILURE) {
//            buffer.getInt();
//            buffer.getByte();
//            buffer.getByte();
//            byte[] foo = buffer.getStringByte();
//            int partial_success = buffer.getByte();
//            //System.err.println(new String(foo)+
//            //		 " partial_success:"+(partial_success!=0));
//            if (partial_success != 0) {
//                throw new JSchPartialAuthException(Util.byte2str(foo));
//            }
//        }
        return false;
    }
}


