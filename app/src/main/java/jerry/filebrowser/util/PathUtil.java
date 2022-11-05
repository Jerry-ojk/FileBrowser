package jerry.filebrowser.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.storage.StorageManager;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import jerry.filebrowser.file.FileRoot;
import jerry.filebrowser.setting.FileSetting;

public class PathUtil {
    public static String mergePath(String parent, String name) {
        if (parent.charAt(parent.length() - 1) == '/') {
            return parent + name;
        } else {
            return parent + '/' + name;
        }
    }

    public static String getPathParent(String absPath) {
        absPath = normalize(absPath);
        final int len = absPath.length();
        if (len <= 1) return null;
        for (int i = len - 1; i >= 0; i--) {
            char c = absPath.charAt(i);
            if (c == '/') {
                return absPath.substring(0, i == 0 ? 1 : i);
            }
        }
        return null;
    }

    public static String getPathName(String absPath) {
        absPath = normalize(absPath);
        boolean isFind = false;
        final int len = absPath.length();
        int i = len - 1;
        for (; i >= 0; i--) {
            char c = absPath.charAt(i);
            if (c == '/') {
                isFind = true;
                break;
            }
        }
        i++;
        if (isFind) {
            if (len > i) {
                return absPath.substring(i);
            } else if (len == 1) { // =='/'
                return absPath;
            }
        }
        if (!isFind && FileSetting.PHONE_ROOT.equals(absPath))
            return FileSetting.PHONE_ROOT;
        return null;
    }

    public static String getAbsPath(String currentPath, String relative) {
        currentPath = normalize(currentPath);
        if (relative == null || relative.length() == 0) return currentPath;
        int len = relative.length();
        if (relative.charAt(0) == '/') return relative;
        if (relative.charAt(len - 1) == '/') len--;// 去掉最后的'/'

        StringBuilder absPath = new StringBuilder(currentPath);
        // ../var/tmp/./333
        int section_start = 0;
        String section;
        int i = 0;
        for (; i < len; i++) {
            char c = relative.charAt(i);
            if (c == '/') {// 是'/'，但不是最后一个
                section = relative.substring(section_start, i);
                section_start = i + 1;
                if ("..".equals(section)) {
                    String parent = PathUtil.getPathParent(absPath.toString());
                    if (parent == null) return null;
                    absPath = new StringBuilder(parent);
                } else if (!".".equals(section)) {
                    if (absPath.charAt(absPath.length() - 1) != '/') absPath.append('/');
                    absPath.append(section);
                }
            }
        }
        if (section_start < len) {
            if (absPath.charAt(absPath.length() - 1) != '/') absPath.append('/');
            absPath.append(relative, section_start, len);
        }
        return absPath.toString();
    }

    public static String normalize(String absPath) {
        final int len = absPath.length();
        if (len > 1 && absPath.charAt(len - 1) == '/') {
            return absPath.substring(0, len - 1);
        }
        return absPath;
    }

    // 获取OTG路径
    @SuppressLint("PrivateApi")
    public static ArrayList<FileRoot> getOTGPathList(Context context) {
        ArrayList<FileRoot> res = new ArrayList<>();
        StorageManager mStorageManager = (StorageManager) context.getSystemService(Activity.STORAGE_SERVICE);
        try {
            Method getVolumes = StorageManager.class.getMethod("getVolumes");
            List<?> volumes = (List<?>) getVolumes.invoke(mStorageManager);
            if (volumes.size() == 0) {
                return null;
            }

            Class<?> volumeInfoClazz = Class.forName("android.os.storage.VolumeInfo");
            Method isMountedReadable = volumeInfoClazz.getMethod("isMountedReadable");
            Method getType = volumeInfoClazz.getMethod("getType");
            Method getPath = volumeInfoClazz.getMethod("getPath");
            Method getDescription = volumeInfoClazz.getMethod("getDescription");

            for (Object vol : volumes) {
                if (vol != null && (boolean) isMountedReadable.invoke(vol) && (int) getType.invoke(vol) == 0) {
                    File root = (File) getPath.invoke(vol);
                    if (root != null) {
                        res.add(new FileRoot((String) getDescription.invoke(vol), root.getAbsolutePath()));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            res.clear();
        }
        return res;
    }
}

