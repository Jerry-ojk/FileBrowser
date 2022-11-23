package jerry.filebrowser.file;

public class SelectableFile extends BaseFile {
    public boolean isSelect;

    public SelectableFile(BaseFile file) {
        super(file);
        isSelect = false;
    }
}
