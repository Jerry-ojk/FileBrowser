package jerry.filebrowser;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ProgressCallback;
import com.jcraft.jsch.SSHClient;
import com.jcraft.jsch.Session;

import org.junit.Test;

import jerry.filebrowser.ftp.SFTPConfig;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class UnitTest {
    @Test
    public void connectSFTP() {
        SSHClient client = new SSHClient();
        Session session = null;
        ChannelSftp sftp = null;
        try {
            long a = System.currentTimeMillis();
            SFTPConfig config = new SFTPConfig();
            config.user = "root";
            config.host = "39.96.114.136";
            config.port = 22;
            config.pwd = "im123!im123!";
            session = client.getSession(config);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            sftp = session.openChannel("sftp");
            sftp.connect();

            sftp.download("/boot/initramfs-0-rescue-20200218155604663518171520219378.img", "initramfs.img", new ProgressCallback() {
                private long download = 0;
                private long totalSize = 0;

                @Override
                public void onStart(int op, String src, String dest, long max) {
                    totalSize = max;
                }

                @Override
                public boolean onSizeIncrease(long size) {
                    download += size;
                    System.out.println("onSizeChange:" + size + "," + download + "/" + totalSize);
                    return true;
                }

                @Override
                public void onFinish() {
                    System.out.println("下载完成");
                }
            }, ChannelSftp.MODE_OVERWRITE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (session != null) session.disconnect();
    }
}