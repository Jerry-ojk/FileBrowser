package jerry.filebrowser.ftp.task;

import android.os.AsyncTask;

import com.jcraft.jsch.ChannelSftp;

import jerry.filebrowser.ftp.SFTPActionResult;
import jerry.filebrowser.ftp.callback.FTPActionCallback;
import jerry.filebrowser.ssh.SSHResult;

public class SFTPActionTask extends AsyncTask<Integer, Void, SFTPActionResult> {
    public static final int TYPE_GET_ATTRIBUTE = 1;
    public static final int TYPE_GET_LINK_PATH = 2;

    public static final int TYPE_CREATE_FILE = 5;
    public static final int TYPE_CREATE_DIR = 6;


    public static final int TYPE_RENAME = 8;
    public static final int TYPE_DELETE_FILE = 9;
    public static final int TYPE_DELETE_DIR = 10;

    public static final int TYPE_CHOWN = 13;
    public static final int TYPE_CHMOD = 14;


    private String path;
    private Object param;
    private ChannelSftp channelSftp;
    private FTPActionCallback callback;

    public SFTPActionTask(String path, ChannelSftp channelSftp, FTPActionCallback callback) {
        super();
        this.path = path;
        this.channelSftp = channelSftp;
        this.callback = callback;
    }

    public void setParam(Object param) {
        this.param = param;
    }

    @Override
    protected SFTPActionResult doInBackground(Integer... integers) {
        if (callback == null || integers == null || integers.length != 1) return null;
        final int type = integers[0];
        SFTPActionResult result = new SFTPActionResult();
        result.action = type;
        result.param = path;
        if (!channelSftp.isConnected()) {
            result.code = SSHResult.CODE_ERROR_CONNECT;
            return result;
        }
        result.code = SSHResult.CODE_SUCCESS;
        try {
            switch (type) {
                case TYPE_GET_ATTRIBUTE:
                    result.result = channelSftp.stat(path);
                    break;
                case TYPE_GET_LINK_PATH:
                    result.result = channelSftp.readLink(path);
                    break;
                case TYPE_CREATE_FILE:
                    channelSftp.createNewFile(path);
                    break;
                case TYPE_CREATE_DIR:
                    channelSftp.mkdir(path);
                    break;
                case TYPE_RENAME:
                    if (param != null && param instanceof String) {
                        channelSftp.rename(path, (String) param);
                        result.code = SSHResult.CODE_SUCCESS;
                    } else {
                        result.code = SSHResult.CODE_ERROR_MISSING_PARAM;
                    }
                    break;
                case TYPE_DELETE_FILE:
                    channelSftp.rm(path);
                    result.param = path;
                    break;
                case TYPE_DELETE_DIR:
                    channelSftp.rmdir(path);
                    result.code = SSHResult.CODE_SUCCESS;
                    result.param = path;
                    break;
                case TYPE_CHOWN:
                    if (param != null && param instanceof Integer) {
                        channelSftp.chown(path, (int) param);
                    } else {
                        result.code = SSHResult.CODE_ERROR_MISSING_PARAM;
                    }
                    break;
                case TYPE_CHMOD:
                    if (param != null && param instanceof Integer) {
                        channelSftp.chmod(path, (int) param);
                    } else {
                        result.code = SSHResult.CODE_ERROR_MISSING_PARAM;
                    }
                    break;
                default:
                    result.code = SSHResult.CODE_ERROR_NOT_SUPPORT;
            }
        } catch (Exception e) {
            result.code = SSHResult.CODE_ERROR_UNKNOWN;
            result.exception = e;
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onPostExecute(SFTPActionResult result) {
        callback.onResult(result);
        callback = null;
    }

    @Override
    protected void onCancelled(SFTPActionResult result) {
        callback = null;
    }
}