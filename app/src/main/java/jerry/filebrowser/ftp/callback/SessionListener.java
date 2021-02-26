package jerry.filebrowser.ftp.callback;

public interface SessionListener {
    public void onSocketConnect();

    public void onReceiveServerSSHVersion(String version);

    public void onReceiveKeyExchange();

    public void onReceiveNewKey();

    public void onReceiveUserAuth(String authMethod);

    public void onNextUserAuth(String authMethod);
}
