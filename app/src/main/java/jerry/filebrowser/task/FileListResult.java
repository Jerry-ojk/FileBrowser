package jerry.filebrowser.task;

import java.util.ArrayList;

import jerry.filebrowser.file.UnixFile;

public class FileListResult {
    public String absolutePath;
    public ArrayList<UnixFile> list;
    public int type;
    public int dirs;
    public int files;

    public FileListResult(String absolutePath, ArrayList<UnixFile> list, int type) {
        this.absolutePath = absolutePath;
        this.list = list;
        this.type = type;
    }
}
