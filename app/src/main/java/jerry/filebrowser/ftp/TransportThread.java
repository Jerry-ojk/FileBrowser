package jerry.filebrowser.ftp;

import android.os.Process;
import android.util.Log;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ProgressCallback;
import com.jcraft.jsch.SSHClient;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.exception.JSchException;

import java.io.InterruptedIOException;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;

import jerry.filebrowser.ssh.SSHConnectManager;

public class TransportThread extends Thread {
    private ConcurrentLinkedQueue<TransportService.FTPTransportConfigWrap> queue;
    private WeakReference<TransportService> reference;

    private volatile SSHClient client = SSHConnectManager.getSshClient();
    private volatile Session session = null;
    private volatile ChannelSftp sftp = null;
    public volatile boolean working = false;

    private TransportService.FTPTransportConfigWrap current;


    public TransportThread(TransportService service, ConcurrentLinkedQueue<TransportService.FTPTransportConfigWrap> queue) {
        reference = new WeakReference<>(service);
        this.queue = queue;
    }

    @Override
    public void run() {
        working = true;
        Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
        current = null;
        while (!isInterrupted() && (current = queue.poll()) != null) {
            final SFTPTransportConfig config = current.config;
            try {
                connect(config.config);
                if (current.type == TransportService.TYPE_DOWNLOAD) {
                    download(current);
                } else {
                    upload(current);
                }
            } catch (Exception e) {
                if (e instanceof InterruptedIOException) {
                    if (current.type == TransportService.TYPE_DOWNLOAD) {
                        sendNotify(current.notifyId, config.name + "下载已取消", 0, false);
                    } else {
                        sendNotify(current.notifyId, config.name + "上传已取消", 0, false);
                    }
                } else if (sftp == null || !sftp.isConnected()) {
                    sendNotify(current.notifyId, "传输" + config.name + "连接失败，请重试", 0, false);
                } else {
                    if (current.type == TransportService.TYPE_DOWNLOAD) {
                        sendNotify(current.notifyId, "下载" + config.name + "失败，请重试", 0, false);
                    } else {
                        sendNotify(current.notifyId, "上传" + config.name + "失败，请重试", 0, false);
                    }
                }
                e.printStackTrace();
            }
        }
        working = false;
        if (reference == null) return;
        final TransportService service = reference.get();
        if (service != null) {
            service.onThreadExit();
            service.stopSelf();
        }
    }

    private void connect(SFTPConfig config) throws JSchException {
        checkSession(config);
        checkSFTP();
    }

    private void checkSession(SFTPConfig config) throws JSchException {
        if (session == null) {
            Log.i("666", "连接session");
            session = client.getSession(config);
            session.connect();
        } else if (!session.getConnectConfig().equals(config)) {
            Log.i("666", "复用session");
            session.disconnect();
            session = client.getSession(config);
            session.connect();
        } else if (!session.isConnected()) {
            session.connect();
        }
    }


    private void checkSFTP() throws JSchException {
        if (sftp == null) {
            sftp = session.openChannel("sftp");
            sftp.connect();
        } else if (sftp.isClosed()) {
            sftp.connect();
        }
    }

    private void download(TransportService.FTPTransportConfigWrap wrap) throws Exception {
        final SFTPTransportConfig config = wrap.config;
        sendNotify(wrap.notifyId, "开始下载：" + config.name, 0, true);
        sftp.download(config.remotePath, config.localPath, new ProgressCallback() {
            private long download = 0;
            private long totalSize = 0;
            private int percent = -1;

            @Override
            public void onStart(int operation, String src, String dest, long totalSize) {
                this.totalSize = totalSize;
            }

            @Override
            public boolean onSizeIncrease(long size) {
                if (isInterrupted()) {
                    sendNotify(current.notifyId, config.name + "下载已取消", 0, false);
                    return false;
                }
                download += size;
                //Log.i("666", "onSizeChange:" + size + "," + download + "/" + totalSize);
                int t = (int) (((double) download / (double) totalSize) * 100);
                if (percent != t) {
                    percent = t;
                    sendNotify(wrap.notifyId, "正在下载：" + config.name, percent, true);
                }
                return true;
            }

            @Override
            public void onFinish() {
                if (isInterrupted()) {
                    return;
                }
                //Log.i("666", "ftp下载完成：" + download + "," + totalSize);
                sendNotify(wrap.notifyId, "下载成功：" + config.name, -1, false);
            }
        }, ChannelSftp.MODE_OVERWRITE);
    }

    private void upload(TransportService.FTPTransportConfigWrap wrap) throws Exception {
        final SFTPTransportConfig config = wrap.config;
        sendNotify(wrap.notifyId, "开始上传：" + config.name, 0, true);
        sftp.upload(config.localPath, config.remotePath, new ProgressCallback() {
            private long download = 0;
            private long totalSize = 0;

            @Override
            public void onStart(int operation, String src, String dest, long totalSize) {
                this.totalSize = totalSize;
            }

            @Override
            public boolean onSizeIncrease(long size) {
                if (isInterrupted()) {
                    sendNotify(current.notifyId, config.name + "上传已取消", 0, false);
                    return false;
                }
                download += size;
                sendNotify(wrap.notifyId, "正在上传：" + config.name, (int) (((double) download / (double) totalSize) * 100), true);
                return true;
            }

            @Override
            public void onFinish() {
                //Log.i("666", "ftp上传完成：" + download + "," + totalSize);
                sendNotify(wrap.notifyId, "上传成功：" + config.name, -1, false);
                final TransportService service = reference.get();
                if (service != null) {
                    SFTPActivity.sendRefresh(service, config.destDir);
                }
            }
        }, ChannelSftp.MODE_CREATE);
    }

    public void sendNotify(int nid, String text, int progress, boolean keep) {
        if (reference == null) return;
        final TransportService service = reference.get();
        if (service == null) return;
        service.notifyNotification(nid, service.getNotification(text, progress, keep));
    }
}
