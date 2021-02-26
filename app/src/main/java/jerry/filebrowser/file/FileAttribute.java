package jerry.filebrowser.file;

import jerry.filebrowser.util.PathUtil;

public class FileAttribute {
    public String name;
    public String path;
    public long size;
    public int mode = -1;// 类型和权限
    public int gid;
    public int uid;
    public String gname;
    public String uname;
    public long atime;
    public long mtime;
    public long ctime;

    public FileAttribute() {

    }

    public FileAttribute(String path, long size, int mode, int uid, int gid, String uname, String gname, long atime, long mtime, long ctime) {
        this.name = PathUtil.getPathName(path);
        this.path = path;
        this.size = size;
        this.mode = mode;
        this.uid = uid;
        this.gid = gid;
        this.uname = uname;
        this.gname = gname;
        this.atime = atime;
        this.mtime = mtime;
        this.ctime = ctime;
    }
}
