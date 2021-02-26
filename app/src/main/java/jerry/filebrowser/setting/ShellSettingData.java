package jerry.filebrowser.setting;

import java.util.ArrayList;
import java.util.List;

import jerry.filebrowser.shell.ShellConfig;

public class ShellSettingData {
    public int version;
    public List<ShellConfig> configList;

    public ShellSettingData() {
        configList = new ArrayList<>();
    }
}