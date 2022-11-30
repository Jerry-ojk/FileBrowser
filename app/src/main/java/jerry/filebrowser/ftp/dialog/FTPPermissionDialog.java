package jerry.filebrowser.ftp.dialog;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import jerry.filebrowser.util.Util;
import jerry.filebrowser.dialog.FilePermissionDialog;
import jerry.filebrowser.ftp.SFTPActionResult;
import jerry.filebrowser.ftp.task.SFTPActionTask;
import jerry.filebrowser.file.SFTPFile;
import jerry.filebrowser.ssh.SSHConnectManager;
import jerry.filebrowser.ssh.SSHResult;
import jerry.filebrowser.ftp.callback.FTPActionCallback;

public class FTPPermissionDialog extends FilePermissionDialog {
    protected SFTPActionTask task;

    public FTPPermissionDialog(Context context) {
        super(context);
    }


    public void setFTPFile(SFTPFile SFTPFile) {
        this.file = SFTPFile;
        setPermissions(SFTPFile.getAttrs().permissions);
    }

    @Override
    public void onSureClick(View view) {
        final int permission = buildPermissions();
        Log.d("FTPPermissionDialog", "oldPermission=" + Util.permission(oldPermission));
        Log.d("FTPPermissionDialog", "permission=" + Util.permission(permission));
        if (oldPermission != permission) {
            task = new SFTPActionTask(file.getAbsPath(), SSHConnectManager.getChannelSftp(), new FTPActionCallback() {
                @Override
                public void onResult(SFTPActionResult result) {
                    if (result.code == SSHResult.CODE_SUCCESS) {
                        showToast("修改成功");
                        dismiss();
                    } else {
                        showToast("修改失败");
                    }
                }
            });
            task.setParam(permission);
            task.execute(SFTPActionTask.TYPE_CHMOD);
        } else {
            dismiss();
        }
    }
}
