package jerry.filebrowser.dialog;

public interface FileSelectAdapterCallback extends FileSelectCallback {

    public void OnIntoMultipleSelectMode();

    public void OnQuitMultipleSelectMode();

    public void OnSelectedCount(int count);

    public void onDirectoryChange(String directory);

    public void onShowToast(String message);
}