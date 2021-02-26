package jerry.filebrowser.setting;

import java.util.ArrayList;
import java.util.List;

import jerry.filebrowser.ftp.SFTPConfig;

public class FTPSettingData {
    public int version;
    public int option;
    public List<SFTPConfig> configList;

    public FTPSettingData() {
        option = FileSetting.SORT_BY_NAME;
        configList = new ArrayList<>();
    }
}