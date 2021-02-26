package jerry.filebrowser.ftp.callback;

import jerry.filebrowser.ssh.SSHConnectResult;

public interface SSHConnectCallback {
    public void onConnectResult(SSHConnectResult result);
}
