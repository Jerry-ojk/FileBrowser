package jerry.filebrowser.ssh;

public class SSHResult {
    public static final int CODE_SUCCESS = 0;
    public static final int CODE_ERROR_UNKNOWN = 1;
    public static final int CODE_ERROR_CONNECT = 2;
    public static final int CODE_ERROR_USER = 3;
    public static final int CODE_ERROR_AUTH = 4;
    public static final int CODE_ERROR_PORT = 5;
    public static final int CODE_ERROR_PARAM = 6;
    public static final int CODE_ERROR_MISSING_PARAM = 7;
    public static final int CODE_ERROR_NOT_SUPPORT = 8;

    public int code;
    public Exception exception;
}
