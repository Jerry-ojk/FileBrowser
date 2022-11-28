package jerry.filebrowser.file;

import android.system.ErrnoException;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.util.NativeUtil;

/**
 * Created by Jerry on 2018/1/16
 */

public class UnixFile extends BaseFile {
    // mask for st_mode
    public static final int MASK_PERMISSION = 0b000_111_111_111;
    public static final int MASK_SETID = 0b111_000_000_000;
    public static final int MASK_SETID_AND_PERMISSION = 0b000_111_111_111;
    public static final int MASK_TYPE = 0b1111_000_000_000_000;

    /**
     * #define DT_UNKNOWN 0
     * #define DT_FIFO 1
     * #define DT_CHR 2
     * #define DT_DIR 4
     * #define DT_BLK 6
     * #define DT_REG 8
     * #define DT_LNK 10
     * #define DT_SOCK 12
     * #define DT_WHT 14
     */
    public static final int S_ISUID = 0b100_000_000_000;
    public static final int S_ISGID = 0b010_000_000_000;
    public static final int S_ISVTX = 0b001_000_000_000;

    public static final int S_IFSOCK = 0b1100_000_000_000_000;
    public static final int S_IFLNK = 0b1010_000_000_000_000;
    public static final int S_IFREG = 0x8000;
    public static final int S_IFBLK = 0x6000;
    public static final int S_IFDIR = 0x4000;
    public static final int S_IFCHR = 0x2000;
    public static final int S_IFIFO = 0x1000;

    /**
     * #define S_IRWXU 00700 100000000
     * #define S_IRUSR 00400
     * #define S_IWUSR 00200
     * #define S_IXUSR 00100
     * #define S_IRWXG 00070
     * #define S_IRGRP 00040 0000000100000
     * #define S_IWGRP 00020 0000000010000
     * #define S_IXGRP 00010 0000000001000
     * #define S_IRWXO 00007 0000000000111
     * #define S_IROTH 00004 0000000000100
     * #define S_IWOTH 00002 0000000000010
     * #define S_IXOTH 00001 0000000000001
     */

    public UnixFile(String name, long length, long time, int type) {
        super(name);
        super.length = length;
        super.time = time;
        super.type = type;
    }

    public UnixFile(UnixFile file) {
        super(file);
    }

    public static UnixFile[] listFiles(String path) {
//        long a = System.currentTimeMillis();
//        ArrayList<UnixFile> files = listFilesNative(path);
//        files.size();
        long b = System.currentTimeMillis();
//        Log.i("666", "listFiles(java):" + (b - a) + "ms");
        final UnixFile[] list = NativeUtil.ListFiles(path, FileSetting.OPTION);
//        ArrayList<UnixFile> files1 = new ArrayList<>(list.length);
//        for (UnixFile file : list) {
//            file.parent = path;
//            files1.add(file);
//        }
        long c = System.currentTimeMillis();
        Log.i("666", "listFiles(native):" + (c - b) + "ms");
        return list;
    }

    private static ArrayList<UnixFile> listFilesNative(String path) {
        final File[] files = new File(path).listFiles();
        if (files == null) return null;
        ArrayList<UnixFile> fileList = new ArrayList<>(files.length);
        for (File file : files) {
            //int type = file.isDirectory() ? UnixFile.TYPE_DIR : UnixFile.TYPE_FILE;
            UnixFile unixFile = new UnixFile(file.getName(), file.length(), file.lastModified(), 8);
            unixFile.parent = path;
        }
        return fileList;
    }


    // public static final int ACCESS_OK = 0;
    // public static final int ACCESS_READ = 4;
    // public static final int ACCESS_WRITE = 2;
    // public static final int ACCESS_EXECUTE = 1;

    public static boolean isEmptyDir(String path) {
        String[] names = null;
        try {
            names = new File(path).list();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return names != null && names.length == 0;
    }

    public static boolean createFile(String path) {
        try {
            return new File(path).createNewFile();
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean createDir(String path) {
        return new File(path).mkdir();
    }

    public static boolean createDirs(String path) {
        return new File(path).mkdirs();
    }

    public static boolean rename(String oldPath, String newPath) {
        if (FileSetting.API_MODE == FileSetting.API_MODE_AUTO) {
            return renameOsOrFile(oldPath, newPath);
        } else {
            return new File(oldPath).renameTo(new File(newPath));
        }
    }

    private static boolean renameOsOrFile(String oldPath, String newPath) {
        try {
            Os.rename(oldPath, newPath);
            return true;
        } catch (ErrnoException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean delete(String path) {
        File file = new File(path);
        if (file.exists()) {
            return delete(file);
        }
        return true;
    }

    private static boolean delete(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files == null) return false;
            for (File item : files) {
                delete(item);
            }
        }
        return file.delete();
    }
}