package jerry.filebrowser.ftp;

import java.util.ArrayList;

public class SFTPDownloadManager {

    public static ArrayList<SFTPTransportConfig> downloadingList = new ArrayList<>();
    public static ArrayList<SFTPTransportConfig> downloadedList = new ArrayList<>();

    public static ArrayList<SFTPTransportConfig> getDownloadingList() {
        return downloadingList;
    }

    public static ArrayList<SFTPTransportConfig> getDownloadedList() {
        return downloadedList;
    }
}
