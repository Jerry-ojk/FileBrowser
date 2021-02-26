package jerry.filebrowser.shell;

public interface ShellListener {
    public void onReceiveText(byte[] data, int start, int len);
}
