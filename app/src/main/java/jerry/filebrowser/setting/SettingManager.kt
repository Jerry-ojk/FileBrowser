package jerry.filebrowser.setting

import android.os.Environment
import com.alibaba.fastjson.JSON
import jerry.filebrowser.file.BaseFile
import jerry.filebrowser.file.UnixFile
import jerry.filebrowser.util.FileUtil
import jerry.filebrowser.view.ExpandView
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets

object SettingManager {
    private val CONFIG_ROOT = "${Environment.getExternalStorageDirectory().absolutePath}/jerry"
    private val SETTING_PATH = "$CONFIG_ROOT/setting.json"
    private val FTP_CONFIG_PATH = "$CONFIG_ROOT/config_ftp.json"
    private val SHELL_CONFIG_PATH = "$CONFIG_ROOT/config_shell.json"

    @JvmField
    val FTP_DOWNLOAD_DIR = "$CONFIG_ROOT/FTP"

    @JvmField
    var SETTING_DATA: SettingData? = null

    @JvmField
    var FTP_SETTING_DATA: FTPSettingData? = null
    var SHELL_SETTING_DATA: ShellSettingData? = null

    init {
        val root = File(CONFIG_ROOT)
        if (!root.exists()) {
            root.mkdirs()
        }
    }

    @JvmStatic
    fun read(): Boolean {
        if (BaseFile.isExist(SETTING_PATH)) {
            try {
                val json = FileUtil.readFile(SETTING_PATH)
                if (json != null) {
                    SETTING_DATA = JSON.parseObject(json, SettingData::class.java)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (SETTING_DATA == null) {
            SETTING_DATA = SettingData()
            return false
        }
        FileSetting.OPTION = SETTING_DATA!!.option
        if (SETTING_DATA!!.apiMode in 1..3) {
            FileSetting.API_MODE = SETTING_DATA!!.apiMode
        }
        return true
    }

    @JvmStatic
    fun save(list: ArrayList<ExpandView>?) {
        SETTING_DATA!!.option = FileSetting.OPTION
        SETTING_DATA!!.apiMode = FileSetting.API_MODE
        if (list != null) {
            getDrawerSettings(list, SETTING_DATA!!.triggerList)
        }
        saveJson(SETTING_PATH, SETTING_DATA)
    }

    @JvmStatic
    fun readShellConfig(): ShellSettingData? {
        if (UnixFile.isExist(SHELL_CONFIG_PATH)) {
            try {
                val json = FileUtil.readFile(SHELL_CONFIG_PATH)
                if (json != null) {
                    SHELL_SETTING_DATA = JSON.parseObject(json, ShellSettingData::class.java)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (SHELL_SETTING_DATA == null) {
            SHELL_SETTING_DATA = ShellSettingData()
        }
        return SHELL_SETTING_DATA
    }

    @JvmStatic
    fun readFTPConfig(): FTPSettingData? {
        if (UnixFile.isExist(FTP_CONFIG_PATH)) {
            try {
                val json = FileUtil.readFile(FTP_CONFIG_PATH)
                if (json != null) {
                    FTP_SETTING_DATA = JSON.parseObject(json, FTPSettingData::class.java)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (FTP_SETTING_DATA == null) {
            FTP_SETTING_DATA = FTPSettingData()
        }
        return FTP_SETTING_DATA
    }

    @JvmStatic
    fun saveFTPConfig() {
        saveJson(FTP_CONFIG_PATH, FTP_SETTING_DATA)
    }

    @JvmStatic
    fun saveShellConfig() {
        saveJson(SHELL_CONFIG_PATH, SHELL_SETTING_DATA)
    }

    private fun saveJson(path: String?, `object`: Any?) {
        FileOutputStream(path, false).use { outputStream ->
            val json = JSON.toJSONString(`object`, true)
            outputStream.write(json.toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun getDrawerSettings(list: ArrayList<ExpandView>, res: ArrayList<Boolean>) {
        res.clear()
        val len = list.size
        for (i in 0 until len) {
            res.add(list[i].isOpen)
        }
    }
}