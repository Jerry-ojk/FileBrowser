package com.jcraft.jsch;

import com.jcraft.jsch.exception.JSchException;

class UserAuthPassword extends UserAuth {
    private final int SSH_MSG_USERAUTH_PASSWD_CHANGEREQ = 60;
    private final int maxTryTime = 2;
    private int tryTime = 0;
    private String otherAuthMethods = null;

    public String getOtherAuthMethods() {
        return otherAuthMethods;
    }

    @Override
    public boolean start(Session session) throws Exception {
        super.start(session);
        String password = session.password;
        byte[] passwordByte = session.passwordByte;
        byte[] usernameByte = Util.str2byte(username);
        try {
            if (passwordByte == null) {
                throw new JSchException(JSchException.CODE_ERROR_MISS_PARAMETER, "缺少参数：密码");
            }
// send
            // byte      SSH_MSG_USERAUTH_REQUEST(50)
            // string    user name
            // string    service name ("ssh-connection") 鉴权后启动的服务名称
            // string    method name "password"("publickey","hostbased","none")
            // boolen    FALSE
            // string    plaintext password (ISO-10646 UTF-8)
            packet.reset();
            buffer.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
            buffer.putString(usernameByte);
            buffer.putString(Util.str2byte("ssh-connection"));
            buffer.putString(Util.str2byte("password"));
            buffer.putByte((byte) 0);
            buffer.putString(passwordByte);
            session.write(packet);

            while (true) {
                session.read(buffer);
                final int command = buffer.getCommand() & 0xff;
                buffer.skipRead(6);
                switch (command) {
                    case SSH_MSG_USERAUTH_SUCCESS:
                        return true;
                    case SSH_MSG_USERAUTH_PASSWD_CHANGEREQ:
                        tryTime++;
                        if (tryTime >= maxTryTime) {
                            return false;
                        }
//                        byte[] instruction = buffer.getStringByte();
//                        byte[] languageTag = buffer.getStringByte();
                        String newPassword = null;
                        if (listener != null) {
                            newPassword = listener.onPasswordExpired();
                        }
                        if (newPassword == null || newPassword.equals(password)) {
                            throw new JSchException(JSchException.CODE_ERROR_AUTH_PASSWORD_CHANGE, "密码已改变，必须输入新的密码");
                        }
                        // send
                        // byte      SSH_MSG_USERAUTH_REQUEST(50)
                        // string    user name
                        // string    service name ("ssh-connection")
                        // string    "password"
                        // boolen    TRUE
                        // string    plaintext old password (ISO-10646 UTF-8)
                        // string    plaintext new password (ISO-10646 UTF-8)
                        packet.reset();
                        buffer.putByte((byte) SSH_MSG_USERAUTH_REQUEST);
                        buffer.putString(usernameByte);
                        buffer.putString(Util.str2byte("ssh-connection"));
                        buffer.putString(Util.str2byte("password"));
                        buffer.putByte((byte) 1);
                        buffer.putString(passwordByte);
                        buffer.putString(Util.str2byte(newPassword));
                        session.write(packet);
                        continue;
                    case SSH_MSG_USERAUTH_FAILURE:
                        byte[] foo = buffer.getStringByte();
                        otherAuthMethods = Util.byte2str(foo);
                        int partial_success = buffer.readByte();
                        throw new JSchException(JSchException.CODE_ERROR_AUTH, "partial_success=" + partial_success + ",otherAuthMethods=" + otherAuthMethods + ",提示：" + Util.byte2str(foo));
                    case SSH_MSG_USERAUTH_BANNER:
                        byte[] messageByte = buffer.getStringByte();
                        byte[] lang = buffer.getStringByte();
                        if (listener != null) {
                            listener.onReceiveBannerMessage(Util.byte2str(messageByte));
                        }
                        continue;
                    default:
                        return false;
                }
            }
        } finally {
            if (passwordByte != null) {
                Util.clearZero(passwordByte);
                passwordByte = null;
            }
        }
    }
}