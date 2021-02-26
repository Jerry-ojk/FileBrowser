package jerry.filebrowser.ftp.callback;

public interface AuthListener extends SessionListener {
    public void onReceiveBannerMessage(String message);

    public String onPasswordExpired();

    public void onAuthSuccess();

    public void onAuthFail();
}
