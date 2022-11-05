package jerry.filebrowser.setting;

import android.os.Environment;

import com.alibaba.fastjson.JSON;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.util.FileUtils;
import jerry.filebrowser.view.ExpandView;

public class SettingManager {
    private static final String CONFIG_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath() + "/jerry";
    private static final String SETTING_PATH = CONFIG_ROOT + "/setting.json";
    public static final String FTP_CONFIG_PATH = CONFIG_ROOT + "/config_ftp.json";
    public static final String SHELL_CONFIG_PATH = CONFIG_ROOT + "/config_shell.json";
    public static final String FTP_DOWNLOAD_DIR = CONFIG_ROOT + "/FTP";

    public static SettingData SETTING_DATA;
    public static FTPSettingData FTP_SETTING_DATA;
    public static ShellSettingData SHELL_SETTING_DATA;

    static {
        File root = new File(CONFIG_ROOT);
        if (!root.exists()) {
            root.mkdirs();
        }
    }

    public static boolean read() {
        if (UnixFile.isExist(SETTING_PATH)) {
//            long a = System.currentTimeMillis();
            try {
                String json = FileUtils.readFile(SETTING_PATH);
                if (json != null) {
                    SETTING_DATA = JSON.parseObject(json, SettingData.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            long b = System.currentTimeMillis();
//            Log.i("666", "read json " + (b - a) + "ms");
        }
        if (SETTING_DATA == null) {
            SETTING_DATA = new SettingData();
            return false;
        }
        FileSetting.OPTION = SETTING_DATA.option;
        return true;
    }

    public static void save(ArrayList<ExpandView> list) {
//        long a = System.currentTimeMillis();
        SETTING_DATA.option = FileSetting.OPTION;
        if (list != null) {
            SETTING_DATA.triggerList = getDrawerSettings(list);
        }
        saveJson(SETTING_PATH, SETTING_DATA);
//        long b = System.currentTimeMillis();
//        Log.i("666", "save json " + (b - a) + "ms");
    }

    public static ShellSettingData readShellConfig() {
        if (UnixFile.isExist(SHELL_CONFIG_PATH)) {
            try {
                String json = FileUtils.readFile(SHELL_CONFIG_PATH);
                if (json != null) {
                    SettingManager.SHELL_SETTING_DATA = JSON.parseObject(json, ShellSettingData.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (SHELL_SETTING_DATA == null) {
            SHELL_SETTING_DATA = new ShellSettingData();
        }
        return SettingManager.SHELL_SETTING_DATA;
    }

    public static FTPSettingData readFTPConfig() {
        if (UnixFile.isExist(FTP_CONFIG_PATH)) {
            try {
                String json = FileUtils.readFile(FTP_CONFIG_PATH);
                if (json != null) {
                    SettingManager.FTP_SETTING_DATA = JSON.parseObject(json, FTPSettingData.class);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (FTP_SETTING_DATA == null) {
            FTP_SETTING_DATA = new FTPSettingData();
        }
        return SettingManager.FTP_SETTING_DATA;
    }

    public static void saveFTPConfig() {
        saveJson(FTP_CONFIG_PATH, FTP_SETTING_DATA);
    }

    public static void saveShellConfig() {
        saveJson(SHELL_CONFIG_PATH, SHELL_SETTING_DATA);
    }

    public static void saveJson(String path, Object object) {
        try (FileOutputStream outputStream = new FileOutputStream(path, false)) {
            String json = JSON.toJSONString(object, true);
            outputStream.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ArrayList<Boolean> getDrawerSettings(ArrayList<ExpandView> list) {
        int len = list.size();
        ArrayList<Boolean> isOpen = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            isOpen.add(list.get(i).isOpen());
        }
        return isOpen;
    }
}
