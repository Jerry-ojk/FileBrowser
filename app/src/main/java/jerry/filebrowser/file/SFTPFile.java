package jerry.filebrowser.file;

import jerry.filebrowser.util.PathUtil;

public class SFTPFile extends BaseFile {
    private String longName;
    private SFTPAttrs attrs;

    public SFTPFile(String absPath, SFTPAttrs attrs) {
        super(PathUtil.getPathParent(absPath), PathUtil.getPathName(absPath));
        this.absPath = absPath;
        this.attrs = attrs;
        this.length = attrs.size;
        if (attrs.mtime != -1) {
            this.time = attrs.mtime * 1000L;
        }
        if ((attrs.flags & SFTPAttrs.SSH_FILEXFER_ATTR_PERMISSIONS) != 0) {
            this.type = (attrs.permissions >> 12) & 0xF;
        }
    }

    public SFTPFile(String name, String longName, String parent, SFTPAttrs attrs) {
        super(name);
        this.longName = longName;
        this.parent = parent;
        this.attrs = attrs;
        this.length = attrs.size;
        if (attrs.mtime != -1) {
            this.time = attrs.mtime * 1000L;
        }
        if ((attrs.flags & SFTPAttrs.SSH_FILEXFER_ATTR_PERMISSIONS) != 0) {
            this.type = (attrs.permissions >> 12) & 0xF;
        }
    }

    public SFTPFile(SFTPFile file) {
        super(file);
        this.longName = file.longName;
        this.attrs = file.attrs;
    }

    public String getLongName() {
        return longName;
    }

    public void setLongName(String longName) {
        this.longName = longName;
    }

    public SFTPAttrs getAttrs() {
        return attrs;
    }

    public void setAttrs(SFTPAttrs attrs) {
        this.attrs = attrs;
    }

    @Override
    public boolean isDir() {
        if (attrs != null) {
            return attrs.isDir();
        }
        return false;
    }
}