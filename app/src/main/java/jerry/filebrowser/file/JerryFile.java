package jerry.filebrowser.file;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JerryFile extends UnixFile {

    public JerryFile(File file) {
        super(file.getName(), file.length(), file.lastModified(), BaseFile.ToFileType(file));
    }
}
