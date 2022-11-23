package jerry.filebrowser.file;

import java.io.File;

import jerry.filebrowser.util.PathUtil;

/**
 * @author Jerry
 * @date 2020/3/7 16:17
 */
public abstract class BaseFile {

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_FIFO = 1;
    public static final int TYPE_CHR = 2;
    public static final int TYPE_DIR = 4;
    public static final int TYPE_BLK = 6;
    public static final int TYPE_FILE = 8;
    public static final int TYPE_LINK = 10;
    public static final int TYPE_SOCK = 12;
    public static final int TYPE_WHT = 14;

    public String name;
    public String parent;
    protected String absPath;
    public long time = -1;
    public long length = -1;
    public int type = -1;

    public BaseFile(BaseFile file) {
        this.name = file.name;
        this.parent = file.parent;
        this.absPath = file.absPath;
        this.time = file.time;
        this.length = file.length;
        this.type = file.type;
    }

    public BaseFile(String name) {
        this.name = name;
    }

    public BaseFile(String parent, String name) {
        this.parent = parent;
        this.name = name;
        absPath = PathUtil.join(parent, name);
    }

    public String getParentPath() {
        return parent;
    }


    public void setAbsPath(String absPath) {
        this.absPath = absPath;
    }


    public final String getAbsPath() {
        if (absPath == null) {
            absPath = PathUtil.join(parent, name);
        }
        return absPath;
    }

    public boolean isExist() {
        return false;
    }

    public boolean isDir() {
        return type == TYPE_DIR;
    }

    public String getTypeName() {
        switch (type) {
            case TYPE_FIFO:
                return "FIFO文件";
            case TYPE_CHR:
                return "CHR文件";
            case TYPE_DIR:
                return "文件夹";
            case TYPE_BLK:
                return "BLK文件";
            case TYPE_FILE:
                return "文件";
            case TYPE_LINK:
                return "链接文件";
            case TYPE_SOCK:
                return "SOCK文件";
            case TYPE_WHT:
                return "WHT文件";
            default:
                return "未知类型文件" + type;
        }
    }

    public static int getType(File file) {
        if (file.isFile()) {
            return TYPE_FILE;
        } else if (file.isDirectory()) {
            return TYPE_DIR;
        } else {
            return TYPE_UNKNOWN;
        }
    }
}
