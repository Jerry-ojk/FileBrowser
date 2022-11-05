package jerry.filebrowser.setting;

import java.util.ArrayList;
import java.util.List;

public class SettingData {
    public int version;
    public ArrayList<String> colList;
    public ArrayList<Boolean> triggerList;
    public int option;

    public SettingData() {
        version = 1;
        colList = new ArrayList<>();
    }
}