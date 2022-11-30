package jerry.filebrowser.util

import jerry.filebrowser.file.FileAttribute
import jerry.filebrowser.file.UnixFile

object NativeUtil {
    @JvmStatic
    external fun CreateFile(path: String?): Boolean

    @JvmStatic
    external fun CreateDir(path: String?): Boolean

    @JvmStatic
    external fun IsFileExist(path: String?): Boolean

    @JvmStatic
    external fun ListFiles(path: String?, option: Int): Array<UnixFile?>?

    @JvmStatic
    external fun RenameFile(from: String?, to: String?): Boolean

    @JvmStatic
    external fun DeleteFile(path: String?): Boolean

    // public static native boolean DeleteEmptyDir(String path);
    @JvmStatic
    external fun GetFileType(path: String?): Int

    @JvmStatic
    external fun GetFileAttribute(path: String?): FileAttribute?

    @JvmStatic
    external fun GetDisplay(): Int
}