package com.jcraft.jsch;

import com.jcraft.jsch.exception.JSchAuthCancelException;
import com.jcraft.jsch.exception.JSchException;
import com.jcraft.jsch.exception.JSchPartialAuthException;

import java.util.Vector;

class UserAuthPublicKey extends UserAuth {
    private static final int SSH_MSG_USERAUTH_PK_OK = 60;

    public boolean start(Session session) throws Exception {

        super.start(session);

        Vector identities = session.getIdentityRepository().getIdentities();

        byte[] passphrase = null;
        byte[] _username = null;

        int command;

        synchronized (identities) {
            if (identities.size() <= 0) {
                return false;
            }

            _username = Util.str2byte(username);

            for (int i = 0; i < identities.size(); i++) {

                if (session.auth_failures >= session.max_auth_tries) {
                    return false;
                }

                Identity identity = (Identity) (identities.elementAt(i));
                byte[] pubkeyblob = identity.getPublicKeyBlob();

                if (pubkeyblob != null) {
                    // send
                    // byte      SSH_MSG_USERAUTH_REQUEST(50)
                    // string    user name
                    // string    service name ("ssh-connection")
                    // string    "publickey"
                    // boolen    FALSE
                    // string    public key algorithm name
                    // string    public key blob
                    packet.reset();
                    buffer.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
                    buffer.putString(_username);
                    buffer.putString(Util.str2byte("ssh-connection"));
                    buffer.putString(Util.str2byte("publickey"));
                    buffer.putByte((byte) 0);
                    buffer.putString(Util.str2byte(identity.getAlgName()));
                    buffer.putString(pubkeyblob);
                    session.write(packet);

                    loop1:
                    while (true) {
                        session.read(buffer);
                        command = buffer.getCommand() & 0xff;

                        if (command == SSH_MSG_USERAUTH_PK_OK) {
                            break;
                        } else if (command == SSH_MSG_USERAUTH_FAILURE) {
                            break;
                        } else if (command == SSH_MSG_USERAUTH_BANNER) {
                            buffer.readInt();
                            buffer.readByte();
                            buffer.readByte();
                            byte[] _message = buffer.getStringByte();
                            byte[] lang = buffer.getStringByte();
                            String message = Util.byte2str(_message);
                            if (super.userInfo != null) {
                                userInfo.showMessage(message);
                            }
                            continue loop1;
                        } else {
                            //System.err.println("USERAUTH fail ("+command+")");
                            //throw new JSchException("USERAUTH fail ("+command+")");
                            break;
                        }
                    }

                    if (command != SSH_MSG_USERAUTH_PK_OK) {
                        continue;
                    }
                }

//System.err.println("UserAuthPublicKey: identity.isEncrypted()="+identity.isEncrypted());

                int count = 5;
                while (true) {
                    if ((identity.isEncrypted() && passphrase == null)) {
                        if (userInfo == null) throw new JSchException("USERAUTH fail");
                        if (identity.isEncrypted() &&
                                !userInfo.promptPassphrase("Passphrase for " + identity.getName())) {
                            throw new JSchAuthCancelException("publickey");
                            //throw new JSchException("USERAUTH cancel");
                            //break;
                        }
                        String _passphrase = userInfo.getPassphrase();
                        if (_passphrase != null) {
                            passphrase = Util.str2byte(_passphrase);
                        }
                    }

                    if (!identity.isEncrypted() || passphrase != null) {
                        if (identity.setPassphrase(passphrase)) {
                            if (passphrase != null &&
                                    (session.getIdentityRepository() instanceof IdentityRepository.Wrapper)) {
                                ((IdentityRepository.Wrapper) session.getIdentityRepository()).check();
                            }
                            break;
                        }
                    }
                    Util.clearZero(passphrase);
                    passphrase = null;
                    count--;
                    if (count == 0) break;
                }

                Util.clearZero(passphrase);
                passphrase = null;
//System.err.println("UserAuthPublicKey: identity.isEncrypted()="+identity.isEncrypted());

                if (identity.isEncrypted()) continue;
                if (pubkeyblob == null) pubkeyblob = identity.getPublicKeyBlob();

//System.err.println("UserAuthPublicKey: pubkeyblob="+pubkeyblob);

                if (pubkeyblob == null) continue;

                // send
                // byte      SSH_MSG_USERAUTH_REQUEST(50)
                // string    user name
                // string    service name ("ssh-connection")
                // string    "publickey"
                // boolen    TRUE
                // string    public key algorithm name
                // string    public key blob
                // string    signature
                packet.reset();
                buffer.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
                buffer.putString(_username);
                buffer.putString(Util.str2byte("ssh-connection"));
                buffer.putString(Util.str2byte("publickey"));
                buffer.putByte((byte) 1);
                buffer.putString(Util.str2byte(identity.getAlgName()));
                buffer.putString(pubkeyblob);

//      byte[] tmp=new byte[buf.index-5];
//      System.arraycopy(buf.data, 5, tmp, 0, tmp.length);
//      buf.putString(signature);

                byte[] sid = session.getSessionId();
                int sidlen = sid.length;
                byte[] tmp = new byte[4 + sidlen + buffer.indexWrite - 5];
                tmp[0] = (byte) (sidlen >>> 24);
                tmp[1] = (byte) (sidlen >>> 16);
                tmp[2] = (byte) (sidlen >>> 8);
                tmp[3] = (byte) (sidlen);
                System.arraycopy(sid, 0, tmp, 4, sidlen);
                System.arraycopy(buffer.data, 5, tmp, 4 + sidlen, buffer.indexWrite - 5);
                byte[] signature = identity.getSignature(tmp);
                if (signature == null) {  // for example, too long key length.
                    break;
                }
                buffer.putString(signature);
                session.write(packet);

                loop2:
                while (true) {
                    session.read(buffer);
                    command = buffer.getCommand() & 0xff;

                    if (command == SSH_MSG_USERAUTH_SUCCESS) {
                        return true;
                    } else if (command == SSH_MSG_USERAUTH_BANNER) {
                        buffer.readInt();
                        buffer.readByte();
                        buffer.readByte();
                        byte[] _message = buffer.getStringByte();
                        byte[] lang = buffer.getStringByte();
                        String message = Util.byte2str(_message);
                        if (userInfo != null) {
                            userInfo.showMessage(message);
                        }
                        continue loop2;
                    } else if (command == SSH_MSG_USERAUTH_FAILURE) {
                        buffer.readInt();
                        buffer.readByte();
                        buffer.readByte();
                        byte[] foo = buffer.getStringByte();
                        int partial_success = buffer.readByte();
                        //System.err.println(new String(foo)+
                        //                   " partial_success:"+(partial_success!=0));
                        if (partial_success != 0) {
                            throw new JSchPartialAuthException(Util.byte2str(foo));
                        }
                        session.auth_failures++;
                        break;
                    }
                    //System.err.println("USERAUTH fail ("+command+")");
                    //throw new JSchException("USERAUTH fail ("+command+")");
                    break;
                }
            }
        }
        return false;
    }
}
