package jerry.filebrowser.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JerryFile extends UnixFile {
    private File file;

    public JerryFile(File file) {
        super(file.getName(), ToFileType(file), file.length(), file.lastModified());
        this.file = file;
    }

    private static int ToFileType(File file) {
        int type = BaseFile.TYPE_UNKNOWN;
        if (file.isDirectory()) {
            type = BaseFile.TYPE_DIR;
        } else if (file.isFile()) {
            type = BaseFile.TYPE_FILE;
        }
        return type;
    }

    public static ArrayList<BaseFile> listFiles1(String path) {
        File[] files = new File(path).listFiles();
        ArrayList<BaseFile> res = new ArrayList<>();
        if (files != null) {
            for (File item : files) {
                res.add(new JerryFile(item));
            }
        }
        return res;
    }
}
