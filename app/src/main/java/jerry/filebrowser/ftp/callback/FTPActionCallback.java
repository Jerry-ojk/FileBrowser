package jerry.filebrowser.ftp.callback;

import jerry.filebrowser.ftp.SFTPActionResult;

public interface FTPActionCallback {
    public void onResult(SFTPActionResult result);
}
