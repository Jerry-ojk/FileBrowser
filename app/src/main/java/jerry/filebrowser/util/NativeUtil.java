package jerry.filebrowser.util;

import jerry.filebrowser.file.FileAttribute;
import jerry.filebrowser.file.UnixFile;

public class NativeUtil {
    public static native boolean CreateFile(String path);

    public static native boolean CreateDir(String path);

    public static native boolean IsFileExist(String path);

    public static native UnixFile[] ListFiles(String path, int option);

    public static native boolean RenameFile(String from, String to);

    public static native boolean DeleteFile(String path);

    public static native boolean DeleteEmptyDir(String path);

    public static native int GetFileType(String path);

    public static native FileAttribute GetFileAttribute(String path);

    public static native int GetDisplay();
}
