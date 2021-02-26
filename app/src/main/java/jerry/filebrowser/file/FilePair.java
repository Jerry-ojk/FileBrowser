package jerry.filebrowser.file;

import java.io.File;

public class FilePair {
    public File source;
    public File dest;
    public boolean isDirectory = false;

    public FilePair(File source, File dest) {
        this.source = source;
        this.dest = dest;
        if (source == null) isDirectory = true;
    }
}
