package jerry.filebrowser.ftp;

import java.util.ArrayList;

import jerry.filebrowser.file.SFTPFile;
import jerry.filebrowser.ssh.SSHResult;

public class SFTPListResult extends SSHResult {
    public String absolutePath;
    public ArrayList<SFTPFile> list;
    public int type;
}