package jerry.filebrowser.ssh;

import android.os.AsyncTask;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.SSHClient;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.exception.JSchException;

import jerry.filebrowser.ftp.callback.SSHConnectCallback;

public class SSHConnectTask extends AsyncTask<Void, Object, SSHConnectResult> {
    public static final int TYPE_SFTP_CONNECT = 0;
    public static final int TYPE_SFTP_TEST = 1;
    public static final int TYPE_SHELL_CONNECT = 2;
    public static final int TYPE_SHELL_TEST = 3;

    private SSHConnectCallback callback;
    private SSHConnectConfig config;
    private int type;


    public SSHConnectTask(SSHConnectCallback callback, SSHConnectConfig config, int type) {
        this.callback = callback;
        this.config = config;
        this.type = type;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected SSHConnectResult doInBackground(Void... voids) {
        boolean isSuccess = true;
        Session session = null;
        Channel channel = null;
        int code = SSHResult.CODE_SUCCESS;
        final SSHClient sshClient = SSHConnectManager.getSshClient();
        try {
            session = sshClient.getSession(config);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(5 * 1000);
            if (type == TYPE_SHELL_CONNECT) {
                channel = session.openChannel("shell");
                channel.connect();
            } else if (type == TYPE_SFTP_CONNECT) {
                // ChannelSftp
                channel = session.openChannel("sftp");
                channel.connect();
            } else {
                session.disconnect();
            }
        } catch (JSchException e) {
            isSuccess = false;
            code = SSHResult.CODE_ERROR_CONNECT;
            if (e.code == JSchException.CODE_ERROR_AUTH) {
                code = SSHResult.CODE_ERROR_AUTH;
            }
            e.printStackTrace();
        }
        return new SSHConnectResult(session, channel, code);
    }

    @Override
    protected void onProgressUpdate(Object... objects) {
    }

    @Override
    protected void onPostExecute(SSHConnectResult result) {
        callback.onConnectResult(result);
        callback = null;
    }

    @Override
    protected void onCancelled(SSHConnectResult result) {
        callback = null;
    }
}