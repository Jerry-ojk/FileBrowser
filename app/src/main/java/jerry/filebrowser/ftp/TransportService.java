package jerry.filebrowser.ftp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.concurrent.ConcurrentLinkedQueue;

import jerry.filebrowser.R;

public class TransportService extends Service {
    public static final String ACTION_DOWNLOAD = "jerry.filebrowser.service.action.DOWNLOAD_FTP_FILE";
    public static final String ACTION_UPLOAD = "jerry.filebrowser.service.action.UPLOAD_FTP_FILE";
    public static final String ACTION_STOP = "jerry.filebrowser.service.action.STOP";

    public static final int TYPE_DOWNLOAD = 1;
    public static final int TYPE_UPLOAD = 2;

    private static final String EXTRA_PARAM1 = "jerry.filebrowser.service.extra.PARAM1";
    private static final String EXTRA_PARAM2 = "jerry.filebrowser.service.extra.PARAM2";

    public static final String CHANNEL_ID = "FTPTransportService";
    private NotificationManagerCompat managerCompat;
    private NotificationCompat.Builder builder;


    private volatile boolean downloading = false;
    private int nid = 0;
    private TransportThread transportThread;


    private final ConcurrentLinkedQueue<FTPTransportConfigWrap> queue = new ConcurrentLinkedQueue<>();

    public TransportService() {

    }

    @Override
    public void onCreate() {
        managerCompat = NotificationManagerCompat.from(this);
        if (managerCompat.getNotificationChannel(CHANNEL_ID) == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "FTP文件传输通知", NotificationManager.IMPORTANCE_DEFAULT);
                managerCompat.createNotificationChannel(channel);
            }
        }
        builder = new NotificationCompat.Builder(this, TransportService.CHANNEL_ID);
        builder.setContentTitle("FTP文件传输");
        builder.setSmallIcon(R.mipmap.icon);
        builder.setAutoCancel(false);
        transportThread = new TransportThread(this, queue);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent == null) return Service.START_NOT_STICKY;
        SFTPTransportConfig config = null;
        final Object object = intent.getParcelableExtra("config");
        if (object instanceof SFTPTransportConfig) {
            config = intent.getParcelableExtra("config");
        } else {
            return Service.START_NOT_STICKY;
        }
        final String action = intent.getAction();
        if (ACTION_DOWNLOAD.equals(action)) {
            FTPTransportConfigWrap wrap = new FTPTransportConfigWrap(config, nid++);
            wrap.type = TYPE_DOWNLOAD;
            queue.offer(wrap);
            if (transportThread != null)
                managerCompat.notify(wrap.notifyId, getNotification("等待下载：" + config.name, -1, true));
        } else if (ACTION_UPLOAD.equals(action)) {
            FTPTransportConfigWrap wrap = new FTPTransportConfigWrap(config, nid++);
            wrap.type = TYPE_UPLOAD;
            queue.offer(wrap);
            if (transportThread != null)
                managerCompat.notify(wrap.notifyId, getNotification("等待上传：" + config.name, -1, true));
        }
        if (transportThread == null) {
            transportThread = new TransportThread(this, queue);
        }
        if (!transportThread.isAlive()) {
            transportThread.start();
        }
        return Service.START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        for (FTPTransportConfigWrap wrap : queue) {
            managerCompat.notify(wrap.notifyId, getNotification("取消：" + wrap.config.name, -0, false));
        }
        if (transportThread != null && transportThread.isAlive()) {
            transportThread.interrupt();
        }
        transportThread = null;
        downloading = false;
    }


    public Notification getNotification(String text, boolean keep) {
        return getNotification(text, -1, keep);
    }

    public Notification getNotification(String text, int progress, boolean keep) {
        builder.setContentText(text);
        builder.setNumber(progress);
        builder.setWhen(System.currentTimeMillis());
        if (progress >= 0) {
            builder.setProgress(100, progress, false);
        }
        Notification notification = builder.build();
        if (progress < 0) {// 清除进度条
            Bundle extras = notification.extras;
            extras.remove(Notification.EXTRA_PROGRESS);
            extras.remove(Notification.EXTRA_PROGRESS_MAX);
            extras.remove(Notification.EXTRA_PROGRESS_INDETERMINATE);
        }
        if (keep) {
            notification.flags |= Notification.FLAG_ONGOING_EVENT;
        }
        return notification;
    }

    public void notifyNotification(int nid, Notification notification) {
        managerCompat.notify(nid, notification);
    }

    public void onThreadExit() {
        // Toast.makeText(this, "服务退出", Toast.LENGTH_SHORT).show();
        transportThread = null;
        downloading = false;
    }

    public static class FTPTransportConfigWrap {
        public SFTPTransportConfig config;
        public int type;
        public int notifyId;

        public FTPTransportConfigWrap(SFTPTransportConfig config, int notifyId) {
            this.config = config;
            this.notifyId = notifyId;
        }
    }
}