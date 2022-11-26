package jerry.filebrowser.file;

import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;

import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.util.PathUtil;

/**
 * @author Jerry
 * @date 2020/3/7 16:17
 */
public class BaseFile {

    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_FIFO = 1;
    public static final int TYPE_CHR = 2;
    public static final int TYPE_DIR = 4;
    public static final int TYPE_BLK = 6;
    public static final int TYPE_FILE = 8;
    public static final int TYPE_LINK = 10;
    public static final int TYPE_SOCK = 12;
    public static final int TYPE_WHT = 14;

    public static final int ACCESS_OK = 0;
    public static final int ACCESS_READ = 4;
    public static final int ACCESS_WRITE = 2;
    public static final int ACCESS_EXECUTE = 1;

    public String name;
    public String parent;
    protected String absPath;
    public long time = -1;
    public long length = -1;
    public int type = -1;

    public BaseFile(String name) {
        this.name = name;
    }

    public BaseFile(String parent, String name) {
        this.parent = parent;
        this.name = name;
        absPath = PathUtil.join(parent, name);
    }

    public BaseFile(File file) {
        this.name = file.getName();
        this.parent = file.getParent();
        this.length = file.length();
        this.time = file.lastModified();
        this.type = ToFileType(file);
    }

    public BaseFile(BaseFile file) {
        this.name = file.name;
        this.parent = file.parent;
        this.absPath = file.absPath;
        this.time = file.time;
        this.length = file.length;
        this.type = file.type;
    }

    public String getParentPath() {
        return parent;
    }


    public final String getAbsPath() {
        if (absPath == null) {
            absPath = PathUtil.join(parent, name);
        }
        return absPath;
    }

    public void setAbsPath(String absPath) {
        this.absPath = absPath;
    }

    public boolean isExist() {
        if (FileSetting.API_MODE == FileSetting.API_MODE_NATIVE) {
            return isExistOsOrFile(getAbsPath());
        } else {
            return new File(getAbsPath()).exists();
        }
    }

    public static boolean isExist(String path) {
        if (FileSetting.API_MODE == FileSetting.API_MODE_NATIVE) {
            return isExistOsOrFile(path);
        } else {
            return new File(path).exists();
        }
    }

    private static boolean isExistOsOrFile(String path) {
        try {
            return Os.access(path, ACCESS_OK);
        } catch (ErrnoException e) {
            Log.i("isExistOsOrFile", path);
            e.printStackTrace();
            return new File(path).exists();
        }
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

    public static ArrayList<BaseFile> listFilesJava(String path) {
        File[] files = new File(path).listFiles();
        ArrayList<BaseFile> res = new ArrayList<>();
        if (files != null) {
            for (File item : files) {
                res.add(new BaseFile(item));
            }
        }
        return res;
    }

    static int ToFileType(File file) {
        int type = BaseFile.TYPE_UNKNOWN;
        if (file.isDirectory()) {
            type = BaseFile.TYPE_DIR;
        } else if (file.isFile()) {
            type = BaseFile.TYPE_FILE;
        }
        return type;
    }
}
