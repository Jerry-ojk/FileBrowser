package jerry.filebrowser.ftp.callback;

import jerry.filebrowser.ftp.SFTPListResult;

public interface FTPListCallback {
    public void onListResult(SFTPListResult result);
}
