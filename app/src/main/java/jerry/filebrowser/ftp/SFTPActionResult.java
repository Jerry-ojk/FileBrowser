package jerry.filebrowser.ftp;

import jerry.filebrowser.ssh.SSHResult;

public class SFTPActionResult extends SSHResult {
    public int action;
    public Object param;
    public Object result;
}