package jerry.filebrowser.file;

public class FileRoot {
    private String name;
    private String path;

    public FileRoot() {
        this(null, null);
    }

    public FileRoot(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
