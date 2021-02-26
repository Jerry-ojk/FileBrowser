package jerry.filebrowser.file;

import java.util.ArrayList;

public class Clipboard {
    public static final int TYPE_NONE = 0;
    public static final int TYPE_CUT_LIST = 1;
    public static final int TYPE_COPY_LIST = 2;
    public static final int TYPE_CUT_SINGLE = 3;
    public static final int TYPE_COPY_SINGLE = 4;

    public static int type;
    public static String path;
    public static ArrayList<UnixFile> list;
    public static UnixFile single;


    public static void copy(String path, ArrayList<UnixFile> list) {
        if (list == null || list.size() == 0) return;
        type = TYPE_COPY_LIST;
        Clipboard.path = path;
        Clipboard.list = list;
    }

    public static void cut(String path, ArrayList<UnixFile> list) {
        if (list == null || list.size() == 0) return;
        type = TYPE_CUT_LIST;
        Clipboard.path = path;
        Clipboard.list = list;
    }

    public static void copySingle(String path, UnixFile file) {
        type = TYPE_COPY_SINGLE;
        Clipboard.path = path;
        Clipboard.single = file;
    }

    public static void cutSingle(String path, UnixFile file) {
        type = TYPE_CUT_SINGLE;
        Clipboard.path = path;
        Clipboard.single = file;
    }

    public static void clear() {
        type = TYPE_NONE;
        path = null;
        list = null;
    }
}
