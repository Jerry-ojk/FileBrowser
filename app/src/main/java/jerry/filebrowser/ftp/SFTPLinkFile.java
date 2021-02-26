package jerry.filebrowser.ftp;

import jerry.filebrowser.file.SFTPFile;

public class SFTPLinkFile extends SFTPFile {
    private SFTPFile refer;

    public SFTPLinkFile(SFTPFile file, SFTPFile refer) {
        super(file);
        this.refer = refer;
    }

    public SFTPFile getLink() {
        return refer;
    }
}