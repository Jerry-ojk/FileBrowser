package com.jcraft.jsch;


import com.jcraft.jsch.exception.JSchAuthCancelException;
import com.jcraft.jsch.exception.JSchPartialAuthException;

class UserAuthKeyboardInteractive extends UserAuth {
    public boolean start(Session session) throws Exception {
        super.start(session);
        if (userInfo != null && !(userInfo instanceof UIKeyboardInteractive)) {
            return false;
        }

        String dest = username + "@" + session.getHost() + ":" + session.getPort();

        byte[] passwordByte = session.passwordByte;

        boolean cancel = false;

        byte[] usernameByte = Util.str2byte(username);

        while (true) {

            if (session.auth_failures >= session.max_auth_tries) {
                return false;
            }

            // send
            // byte      SSH_MSG_USERAUTH_REQUEST(50)
            // string    user name (ISO-10646 UTF-8, as defined inputStream [RFC-2279])
            // string    service name (US-ASCII) "ssh-userauth" ? "ssh-connection"
            // string    "keyboard-interactive" (US-ASCII)
            // string    language tag (as defined inputStream [RFC-3066])
            // string    submethods (ISO-10646 UTF-8)
            packet.reset();
            buffer.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
            buffer.putString(usernameByte);
            buffer.putString(Util.str2byte("ssh-connection"));
            //buf.putString("ssh-userauth".getBytes());
            buffer.putString(Util.str2byte("keyboard-interactive"));
            buffer.putString(Util.EMPTY_BYTE);
            buffer.putString(Util.EMPTY_BYTE);
            session.write(packet);

            boolean firsttime = true;
            loop:
            while (true) {
                session.read(buffer);
                int command = buffer.getCommand() & 0xff;

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
                    byte[] otherServerAuthMethodByte = buffer.getStringByte();
                    int partial_success = buffer.readByte();
//	  System.err.println(new String(foo)+
//			     " partial_success:"+(partial_success!=0));

                    if (partial_success != 0) {
                        throw new JSchPartialAuthException(Util.byte2str(otherServerAuthMethodByte));
                    }

                    if (firsttime) {
                        return false;
                        //throw new JSchException("USERAUTH KI is not supported");
                        //cancel=true;  // ??
                    }
                    session.auth_failures++;
                    break;
                }
                if (command == SSH_MSG_USERAUTH_INFO_REQUEST) {
                    firsttime = false;
                    buffer.readInt();
                    buffer.readByte();
                    buffer.readByte();
                    String name = Util.byte2str(buffer.getStringByte());
                    String instruction = Util.byte2str(buffer.getStringByte());
                    String languate_tag = Util.byte2str(buffer.getStringByte());
                    int num = buffer.readInt();
                    String[] prompt = new String[num];
                    boolean[] echo = new boolean[num];
                    for (int i = 0; i < num; i++) {
                        prompt[i] = Util.byte2str(buffer.getStringByte());
                        echo[i] = (buffer.readByte() != 0);
                    }

                    byte[][] response = null;

                    if (passwordByte != null &&
                            prompt.length == 1 &&
                            !echo[0] &&
                            prompt[0].toLowerCase().indexOf("password:") >= 0) {
                        response = new byte[1][];
                        response[0] = passwordByte;
                        passwordByte = null;
                    } else if (num > 0
                            || (name.length() > 0 || instruction.length() > 0)
                    ) {
                        if (userInfo != null) {
                            UIKeyboardInteractive kbi = (UIKeyboardInteractive) userInfo;
                            String[] _response = kbi.promptKeyboardInteractive(dest,
                                    name,
                                    instruction,
                                    prompt,
                                    echo);
                            if (_response != null) {
                                response = new byte[_response.length][];
                                for (int i = 0; i < _response.length; i++) {
                                    response[i] = Util.str2byte(_response[i]);
                                }
                            }
                        }
                    }

                    // byte      SSH_MSG_USERAUTH_INFO_RESPONSE(61)
                    // int       num-responses
                    // string    response[1] (ISO-10646 UTF-8)
                    // ...
                    // string    response[num-responses] (ISO-10646 UTF-8)
                    packet.reset();
                    buffer.putByte((byte) SSH_MSG_USERAUTH_INFO_RESPONSE);
                    if (num > 0 &&
                            (response == null ||  // cancel
                                    num != response.length)) {

                        if (response == null) {
                            // working around the bug inputStream OpenSSH ;-<
                            buffer.putInt(num);
                            for (int i = 0; i < num; i++) {
                                buffer.putString(Util.EMPTY_BYTE);
                            }
                        } else {
                            buffer.putInt(0);
                        }

                        if (response == null)
                            cancel = true;
                    } else {
                        buffer.putInt(num);
                        for (int i = 0; i < num; i++) {
                            buffer.putString(response[i]);
                        }
                    }
                    session.write(packet);
          /*
	  if(cancel)
	    break;
          */
                    continue loop;
                }
                //throw new JSchException("USERAUTH fail ("+command+")");
                return false;
            }
            if (cancel) {
                throw new JSchAuthCancelException("keyboard-interactive");
                //break;
            }
        }
        //return false;
    }
}
