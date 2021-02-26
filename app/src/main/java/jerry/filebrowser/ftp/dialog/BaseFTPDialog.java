package jerry.filebrowser.ftp.dialog;

import android.content.Context;

import com.jcraft.jsch.ChannelSftp;

import jerry.filebrowser.dialog.BaseDialog;
import jerry.filebrowser.ftp.task.SFTPActionTask;
import jerry.filebrowser.ssh.SSHConnectManager;
import jerry.filebrowser.ftp.callback.FTPActionCallback;

public abstract class BaseFTPDialog extends BaseDialog implements FTPActionCallback {
    protected SFTPActionTask task;

    public BaseFTPDialog(Context context) {
        super(context);
    }

    protected abstract int getLayoutId();

    final protected SFTPActionTask buildTask(String path) {
        final ChannelSftp channelSftp = SSHConnectManager.getChannelSftp();
        final SFTPActionTask task = new SFTPActionTask(path, channelSftp, this);
        return task;
    }
}