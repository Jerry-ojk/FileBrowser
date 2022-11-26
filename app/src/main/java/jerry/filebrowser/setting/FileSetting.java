package jerry.filebrowser.setting;

import android.os.Environment;

public class FileSetting {

    public static String DEFAULT_USER_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();
    private static String CURRENT_PATH = FileSetting.DEFAULT_USER_ROOT;
    public static final String PHONE_ROOT = DEFAULT_USER_ROOT;
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

    public static final int API_MODE_AUTO = 1;
    public static final int API_MODE_NATIVE = 2;
    public static final int API_MODE_FILE = 3;

    public static int API_MODE = API_MODE_AUTO;

    public FileSetting() {
    }

    // 获取文件浏览器当前真实路径
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

    // 用户显示路径转为真实路径
    public static String toRealPath(String path) {
        if (path == null) return null;
        final String tag = FileSetting.PHONE_ROOT_TAG;
        if (path.startsWith(tag)) {
            return FileSetting.PHONE_ROOT + path.substring(tag.length());
        } else {
            return path;
        }
    }

    // 真实路径转为用户显示路径
    public static String toShowPath(String path) {
        if (path == null) return null;
        final String root = FileSetting.PHONE_ROOT;
        if (path.startsWith(root)) {
            return FileSetting.PHONE_ROOT_TAG + path.substring(root.length());
        } else {
            return path;
        }
    }
}