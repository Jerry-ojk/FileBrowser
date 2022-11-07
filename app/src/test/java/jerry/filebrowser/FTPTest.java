package jerry.filebrowser;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.SSHClient;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.exception.JSchException;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jerry.filebrowser.file.SFTPFile;
import jerry.filebrowser.ftp.SFTPConfig;
import jerry.filebrowser.shell.ShellListener;
import jerry.filebrowser.util.PathUtil;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class FTPTest {
    @Test
    public void connectSFTP() {
        SSHClient client = new SSHClient();
        Session session = null;
        ChannelSftp sftp = null;
        ArrayList<SFTPFile> list = null;
        try {
            long a = System.currentTimeMillis();
            SFTPConfig config = new SFTPConfig();
            config.user = "root";
            config.host = "39.96.114.000";
            config.port = 22;
            config.pwd = "xxxxxx";
            session = client.getSession(config);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect();
            long b = System.currentTimeMillis();
            System.out.println("Session连接:" + (b - a) + "ms");
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            a = System.currentTimeMillis();

            System.out.println("ChannelSftp连接：" + (a - b) + "ms");

            list = sftp.ls("/");
            for (int i = 0; i < list.size(); i++) {
                final SFTPFile file = list.get(i);
                if (file.getAttrs().isLink()) {
                    String link = sftp.readLink(file.getAbsPath());
                    String absPath = PathUtil.getAbsPath("/", link);
                    file.name += " -> " + absPath;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (session != null) session.disconnect();
        if (list != null) {
            for (SFTPFile file : list) {
                System.out.println(file.name + ":" + file.getAttrs().isLink() + "-" + file.getAttrs().isDir() + "-" + file.getAttrs().isReg());
            }
        }
    }

    @Test
    public void connectShell() {
        SSHClient client = new SSHClient();
        Session session = null;
        ChannelShell channel = null;
        ArrayList<SFTPFile> list = null;
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
            long b = System.currentTimeMillis();
            System.out.println("Session连接:" + (b - a) + "ms");

            channel = session.openChannel("shell");
            channel.connect();
            a = System.currentTimeMillis();
            System.out.println("ChannelSftp连接：" + (a - b) + "ms");

            // channel.setInputStream(System.in);
            channel.setShellListener(new ShellListener() {
                @Override
                public void onReceiveText(byte[] data, int start, int len) {
//                    String a = new String(data, start, len);
//                    System.out.print(a);
                }
            });
            channel.sendCommand("top\n");
            Thread.sleep(100 * 1000);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (session != null) session.disconnect();
    }

    @Test
    public void test() {
        System.out.println(PathUtil.getAbsPath("/", "a"));
        System.out.println(PathUtil.getAbsPath("/aa", "a/b"));
        System.out.println(PathUtil.getAbsPath("/aa/bb", "../usr"));
        System.out.println(PathUtil.getAbsPath("/", "././usr"));
        System.out.println(PathUtil.getAbsPath("/aa/bb/cc/dd", ".././.././../usr"));

        System.out.println(PathUtil.getAbsPath("/", "/a"));
        System.out.println(PathUtil.getAbsPath("/aa", "/a/b"));
        System.out.println(PathUtil.getAbsPath("/aa/bb", "../usr/"));
        System.out.println(PathUtil.getAbsPath("/", "././usr/"));
        System.out.println(PathUtil.getAbsPath("/aa/bb", ".././../usr/"));
    }

    @Test
    public void testC() {
        SSHClient client = new SSHClient();
        SFTPConfig config = new SFTPConfig();
        config.user = "root";
        config.host = "39.96.114.136";
        config.port = 22;
        config.pwd = "im123!im123!";

        Session session = client.getSession(config);
        session.setConfig("StrictHostKeyChecking", "no");
        try {
            session.connect(5000);
        } catch (JSchException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testChar() {
        final String text = "aaa\r\n22\n123\r\n123456";
        // action.clear();
        // action.decodeTerminalText(text);
        final int slen = text.length();
        StringBuilder builder = new StringBuilder(slen + 16);
        int lineWidth = 0;
        for (int i = 0; i < slen; i++) {
            final char c = text.charAt(i);
            if (c == '\r') {
                continue;
            } else if (c == '\n') {
                builder.append('\n');
                lineWidth = 0;
            } else if (lineWidth == 3) {
                builder.append('\n').append(c);
                lineWidth = 0;
            } else {
                builder.append(c);
                lineWidth++;
            }
        }
        System.out.println(builder);
    }

    @Test
    public void testFile() throws IOException {
        File file = new File("D:\\DCIM");
        File file1 = new File(file, ".");
        System.out.println(file.getAbsoluteFile());
        System.out.println(file1.getAbsoluteFile());
        System.out.println(file.getCanonicalPath());
        System.out.println(file1.getCanonicalPath());
    }
}