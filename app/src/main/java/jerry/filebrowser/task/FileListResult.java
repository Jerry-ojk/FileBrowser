package jerry.filebrowser.task;

import java.util.ArrayList;

import jerry.filebrowser.file.BaseFile;
import jerry.filebrowser.file.UnixFile;

public class FileListResult {
    public String absolutePath;
    public ArrayList<BaseFile> list;
    public int type;
    public int dirs;
    public int files;
    public int version;

    public FileListResult(String absolutePath, ArrayList<BaseFile> list, int type) {
        this.absolutePath = absolutePath;
        this.list = list;
        this.type = type;
    }
}
