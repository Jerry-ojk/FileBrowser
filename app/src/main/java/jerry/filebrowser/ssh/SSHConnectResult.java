package jerry.filebrowser.ssh;


import com.jcraft.jsch.Channel;
import com.jcraft.jsch.Session;

public class SSHConnectResult extends SSHResult {
    public Session session;
    public Channel channel;

    public SSHConnectResult(int code) {
        super.code = code;
    }

    public SSHConnectResult(Session session, Channel channel) {
        this.session = session;
        this.channel = channel;
    }

    public SSHConnectResult(Session session, Channel channel, int code) {
        this.session = session;
        this.channel = channel;
        super.code = code;
    }
}
