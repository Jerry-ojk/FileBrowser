package jerry.filebrowser.ssh;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SSHClient;
import com.jcraft.jsch.Session;

public class SSHConnectManager {
    public static volatile SSHClient sshClient;
    public static volatile Session session;
    public static volatile ChannelSftp channelSftp;
    public static volatile ChannelSftp channelShell;

    public static SSHClient getSshClient() {
        if (sshClient == null) {
            sshClient = new SSHClient();
        }
        return sshClient;
    }

    public static Session getSession() {
        return session;
    }

    public static ChannelSftp getChannelSftp() {
        return channelSftp;
    }

    public static ChannelSftp getChannelShell() {
        return channelShell;
    }

    public static boolean isSFTPConnect() {
        return channelSftp != null && channelSftp.isConnected();
    }

    public static void disconnect() {
        if (channelShell != null) {
            channelShell.disconnect();
            channelShell = null;
        }
        if (channelSftp != null) {
            channelSftp.disconnect();
            channelSftp = null;
        }
        if (session != null) {
            session.disconnect();
            session = null;
        }
    }
}
