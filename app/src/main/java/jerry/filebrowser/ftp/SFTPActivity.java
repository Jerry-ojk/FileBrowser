package jerry.filebrowser.ftp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.jcraft.jsch.ChannelSftp;

import jerry.filebrowser.R;
import jerry.filebrowser.activity.ToastInterface;
import jerry.filebrowser.adapter.ItemDecoration;
import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.dialog.FileSelectDialog;
import jerry.filebrowser.file.SFTPFile;
import jerry.filebrowser.ftp.callback.SSHConnectCallback;
import jerry.filebrowser.ftp.dialog.FTPActionDialog;
import jerry.filebrowser.ftp.dialog.FTPSortDialog;
import jerry.filebrowser.ftp.dialog.FTPUploadDialog;
import jerry.filebrowser.ssh.SSHConnectManager;
import jerry.filebrowser.ssh.SSHConnectResult;
import jerry.filebrowser.ssh.SSHConnectTask;
import jerry.filebrowser.ssh.SSHResult;

public class SFTPActivity extends AppCompatActivity implements SSHConnectCallback, ToastInterface {

    private RecyclerView recyclerView;
    private SFTPFileBrowserAdapter adapter;
    private MenuItem connectMenu;

    private RefreshReceiver refreshReceiver;

    private SFTPConfig config;
    private TextView tv_output;

    private String root;
    private Toast toast;
    public static String CURRENT_PATH = "/";
    public static String USER_ROOT = "/";

    public boolean isConnected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp);
        Toolbar toolbar = findViewById(R.id.toolbar_ftp);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        tv_output = findViewById(R.id.tv_output);
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        recyclerView = findViewById(R.id.recv_file);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new ItemDecoration(this));
        CURRENT_PATH = "/";
        USER_ROOT = "/";
        adapter = new SFTPFileBrowserAdapter(this, CURRENT_PATH, recyclerView);

        refreshReceiver = new RefreshReceiver();
        IntentFilter filter = new IntentFilter(RefreshReceiver.ACTION_REFRESH);
        registerReceiver(refreshReceiver, filter);
        config = getIntent().getParcelableExtra("target");
        if (config != null) {
            toolbar.setTitle(config.host);
            toolbar.setSubtitle("sftp://" + config.user + '@' + config.host + ':' + config.port);
            new SSHConnectTask(this, config, SSHConnectTask.TYPE_SFTP_CONNECT).execute();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        AppUtil.showIcon(menu);
        getMenuInflater().inflate(R.menu.toolbar_ftp, menu);
        connectMenu = menu.findItem(R.id.action_connect_or_close);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (SSHConnectManager.isSFTPConnect() && !"断开".contentEquals(connectMenu.getTitle())) {
            connectMenu.setTitle("断开").setIcon(R.drawable.ic_action_power_off);
        } else if (!SSHConnectManager.isSFTPConnect() && !"连接".contentEquals(connectMenu.getTitle())) {
            connectMenu.setTitle("连接").setIcon(R.drawable.ic_type_link);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        if (id == android.R.id.home || id == R.id.action_exit) {
            finish();
            return true;
        }
        if (id == R.id.action_sort) {
            new FTPSortDialog(this).show();
        } else if (id == R.id.action_stop) {
            Intent intent = new Intent(this, TransportService.class);
            //intent.setAction(TransportService.ACTION_STOP);
            stopService(intent);
        } else if (id == R.id.action_connect_or_close) {
            if (SSHConnectManager.isSFTPConnect()) {
                SSHConnectManager.disconnect();
            } else if (config != null) {
                new SSHConnectTask(this, config, SSHConnectTask.TYPE_SFTP_CONNECT).execute();
            } else {
                showToast("没有连接配置！");
            }
            return true;
        }
        if (!SSHConnectManager.isSFTPConnect()) {
            showToast("请连接后操作");
            return true;
        }
        switch (id) {
            case R.id.action_refresh:
                adapter.refresh();
                break;
            case R.id.action_create:
                if (adapter.isAllow()) {
                    new FTPActionDialog(this).showCreateDialog(CURRENT_PATH);
                } else {
                    showToast("请等待加载结束");
                }
                break;
            case R.id.action_upload:
                new FileSelectDialog(this, path -> {
                    if (AppUtil.notEmpty(path)) {
                        new FTPUploadDialog(SFTPActivity.this, path).show();
                    }
                }).show();
                break;
        }
        return true;
    }

    @Override
    public void showToast(String text) {
        toast.setText(text);
        toast.show();
    }

    @Override
    public void onConnectResult(SSHConnectResult result) {
        switch (result.code) {
            case SSHResult.CODE_SUCCESS:
                isConnected = true;
                SSHConnectManager.session = result.session;
                SSHConnectManager.channelSftp = (ChannelSftp) result.channel;
                getSupportActionBar().setSubtitle("sftp://" + config.user + '@' + config.host + ':' + config.port +
                        " 协议版本:" + SSHConnectManager.channelSftp.getServerVersion());
                adapter.refresh();
                break;
            case SSHResult.CODE_ERROR_AUTH:
                showToast("用户名或密码不正确，请重试");
//                Drawable drawable = activity.getDrawable(R.drawable.ic_check_ok);
//                drawable.setBounds(tv_indication.getCompoundDrawablesRelative()[0].getBounds());
                //tv_indication.setCompoundDrawables(activity.getDrawable(R.drawable.ic_check_ok), null, null, null);
                break;
            default:
                showToast("连接失败，请重试");
        }
    }

//    @Override
//    public void onListResult(FTPListResult result) {
//        if (result.code == FTPResult.CODE_SUCCESS) {
//            CURRENT_PATH = result.absolutePath;
//            for (FTPFile file : result.list) {
//                SFTPAttrs attrs = file.getAttrs();
//                Log.i("666", file.name + " | " + Util.time(attrs.getMTime() * 1000L));
//            }
//        } else {
//            Log.i("666", "onListResult:" + result.code);
//        }
//    }

    public void onStartFTPDownloadSure(SFTPTransportConfig transportConfig) {
        if (SSHConnectManager.isSFTPConnect()) {
            Intent intent = new Intent(this, TransportService.class);
            intent.setAction(TransportService.ACTION_DOWNLOAD);
            transportConfig.config = config;
            intent.putExtra("config", transportConfig);
            startService(intent);
        } else {
            showToast("连接已断开，请重新连接");
        }
    }

    public void onStartFTPUploadSure(SFTPTransportConfig transportConfig) {
        if (SSHConnectManager.isSFTPConnect()) {
            Intent intent = new Intent(this, TransportService.class);
            intent.setAction(TransportService.ACTION_UPLOAD);
            transportConfig.config = config;
            intent.putExtra("config", transportConfig);
            startService(intent);
        } else {
            showToast("连接已断开，请重新连接");
        }
    }

    public SFTPConfig getFTPConfig() {
        return config;
    }

    public void refresh() {
        adapter.refresh();
    }

    public boolean checkFileIsExist(String name) {
        for (SFTPFile file : adapter.fileList) {
            if (name.equals(file.name)) return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (!adapter.onBackKey()) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(refreshReceiver);
        super.onDestroy();
        SSHConnectManager.disconnect();
    }

    private class RefreshReceiver extends BroadcastReceiver {
        public static final String ACTION_REFRESH = "jerry.action.refresh.FTPActivity";
        public static final String PARAM_PATH = "jerry.param.path.FTPActivity";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_REFRESH.equals(intent.getAction())) {
                final String path = intent.getStringExtra(RefreshReceiver.PARAM_PATH);
                if (path == null || SFTPActivity.CURRENT_PATH.equals(path)) {
                    if (isConnected) adapter.refresh();
                }
            }
        }
    }

    public static void sendRefresh(Context context, String path) {
        final Intent intent = new Intent(RefreshReceiver.ACTION_REFRESH);
        intent.putExtra(RefreshReceiver.PARAM_PATH, path);
        context.sendBroadcast(intent);
    }
}
