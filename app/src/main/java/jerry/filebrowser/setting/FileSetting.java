package jerry.filebrowser.setting;

import android.os.Environment;

public class FileSetting {
    //public MainActivity activity;
    //private FileBrowserAdapter adapter;
    private static String CURRENT_PATH;
    public static String USER_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();
    public static final String PHONE_ROOT = USER_ROOT;
    public static final String PHONE_ROOT_TAG = "内部存储";

    // 逆序
    public static final int SHOW_REVERSE = 0b001_00;
    // 显示隐藏文件
    public static final int SHOW_HIDE = 0b010_00;
    // 文件夹与文件混合排序
    public static final int SHOW_MIX = 0b100_00;


    public static final int SORT_BY_NAME = 0b000_00;
    public static final int SORT_BY_TIME = 0b000_01;
    public static final int SORT_BY_TYPE = 0b000_10;
    public static final int SORT_BY_SIZE = 0b000_11;
    public static final int SORT_MASK = 0b00_11;

    public static int OPTION = 0;

    public FileSetting() {
    }

    public static String getCurrentPath() {
        return CURRENT_PATH;
    }

    public static void setCurrentPath(String currentPath) {
        FileSetting.CURRENT_PATH = currentPath;
    }


    public static int getSortType() {
        return OPTION & SORT_MASK;
    }

    public static boolean isReverse() {
        return (OPTION & SHOW_REVERSE) != 0;
    }

    public static boolean isReverse(int option) {
        return (option & SHOW_REVERSE) != 0;
    }

    public static boolean isMix() {
        return (OPTION & SHOW_MIX) != 0;
    }

    public static boolean isMix(int option) {
        return (option & SHOW_MIX) != 0;
    }

    public static boolean isShowHide() {
        return (OPTION & SHOW_HIDE) != 0;
    }

    public static boolean isShowHide(int option) {
        return (option & SHOW_HIDE) != 0;
    }

    public static void setShowHide(boolean show) {
        if (show) {
            OPTION |= SHOW_HIDE;
        } else {
            OPTION &= (~SHOW_HIDE);
        }
    }

    public static void setSortType(int sortType) {
        // 清零排序位
        OPTION &= (~SORT_MASK);
        // 设置排序位
        OPTION |= sortType;
    }

    public static String innerPath(String path) {
        final String tag = FileSetting.PHONE_ROOT_TAG;
        if (path.startsWith(tag)) {
            final StringBuilder builder = new StringBuilder(FileSetting.PHONE_ROOT.length() + path.length());
            builder.append(FileSetting.PHONE_ROOT).append(path, tag.length(), path.length());
            return builder.toString();
        } else {
            return path;
        }
    }

    public static String tagPath(String path) {
        final String root = FileSetting.PHONE_ROOT;
        if (path.startsWith(root)) {
            final StringBuilder builder = new StringBuilder(FileSetting.PHONE_ROOT_TAG.length() + path.length());
            builder.append(FileSetting.PHONE_ROOT_TAG).append(path, root.length(), path.length());
            return builder.toString();
        } else {
            return path;
        }
    }
}