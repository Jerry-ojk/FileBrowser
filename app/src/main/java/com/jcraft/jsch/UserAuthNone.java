package com.jcraft.jsch;

import com.jcraft.jsch.exception.JSchException;

class UserAuthNone extends UserAuth {
    private static final int SSH_MSG_SERVICE_ACCEPT = 6;
    private String methods = null;

    public boolean start(Session session) throws Exception {
        super.start(session);


        // send
        // byte      SSH_MSG_SERVICE_REQUEST(5)
        // string    service name "ssh-userauth"
        packet.reset();
        buffer.putByte((byte) Session.SSH_MSG_SERVICE_REQUEST);
        buffer.putString(Util.str2byte("ssh-userauth"));
        session.write(packet);

        if (SSHClient.getLogger().isEnabled(Logger.INFO)) {
            SSHClient.getLogger().log(Logger.INFO,
                    "SSH_MSG_SERVICE_REQUEST sent");
        }

        // receive
        // byte      SSH_MSG_SERVICE_ACCEPT(6)
        // string    service name
        session.read(buffer);
        int command = buffer.getCommand();

        boolean result = (command == SSH_MSG_SERVICE_ACCEPT);

        if (SSHClient.getLogger().isEnabled(Logger.INFO)) {
            SSHClient.getLogger().log(Logger.INFO,
                    "SSH_MSG_SERVICE_ACCEPT received");
        }
        if (!result)
            return false;

        // send
        // byte      SSH_MSG_USERAUTH_REQUEST(50)
        // string    user name
        // string    service name ("ssh-connection")
        // string    "none"
        packet.reset();
        buffer.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
        buffer.putString(Util.str2byte(username));
        buffer.putString(Util.str2byte("ssh-connection"));
        buffer.putString(Util.str2byte("none"));
        session.write(packet);

        loop:
        while (true) {
            session.read(buffer);
            command = buffer.getCommand() & 0xff;

            if (command == SSH_MSG_USERAUTH_SUCCESS) {
                return true;
            }
            if (command == SSH_MSG_USERAUTH_BANNER) {
                buffer.readInt();
                buffer.readByte();
                buffer.readByte();
                byte[] _message = buffer.getStringByte();
                byte[] lang = buffer.getStringByte();
                if (listener != null) {
                    listener.onReceiveBannerMessage(Util.byte2str(_message));
                }
                continue loop;
            }
            if (command == SSH_MSG_USERAUTH_FAILURE) {
                buffer.readInt();
                buffer.readByte();
                buffer.readByte();
                byte[] foo = buffer.getStringByte();
                int partial_success = buffer.readByte();
                methods = Util.byte2str(foo);
//System.err.println("UserAuthNONE: "+methods+
//		   " partial_success:"+(partial_success!=0));
//	if(partial_success!=0){
//	  throw new JSchPartialAuthException(new String(foo));
//	}

                break;
            } else {
//      System.err.println("USERAUTH fail ("+command+")");
                throw new JSchException("USERAUTH fail (" + command + ")");
            }
        }
        //throw new JSchException("USERAUTH fail");
        return false;
    }

    String getMethods() {
        return methods;
    }
}
