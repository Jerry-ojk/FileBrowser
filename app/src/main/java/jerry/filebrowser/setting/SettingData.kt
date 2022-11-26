package jerry.filebrowser.setting

import java.util.ArrayList

class SettingData {
    var version = 1
    var option = 0
    var apiMode = 0

    @JvmField
    var colList: ArrayList<String> = ArrayList()

    @JvmField
    var triggerList: ArrayList<Boolean> = ArrayList()
}