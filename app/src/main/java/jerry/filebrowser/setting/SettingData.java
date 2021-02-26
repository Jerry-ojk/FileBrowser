package jerry.filebrowser.setting;

import java.util.ArrayList;
import java.util.List;

public class SettingData {
    public int version;
    public List<String> colList;
    public List<Boolean> triggerList;
    public int option;

    public SettingData() {
        version = 1;
        colList = new ArrayList<>();
    }
}