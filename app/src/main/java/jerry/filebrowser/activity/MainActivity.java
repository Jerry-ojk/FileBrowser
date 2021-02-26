package jerry.filebrowser.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jerry.filebrowser.R;
import jerry.filebrowser.adapter.FileBrowserAdapter;
import jerry.filebrowser.adapter.ItemDecoration;
import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.dialog.DialogManager;
import jerry.filebrowser.dialog.FileClearDialog;
import jerry.filebrowser.dialog.SearchDialog;
import jerry.filebrowser.dialog.SortDialog;
import jerry.filebrowser.file.Clipboard;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.ftp.SFTPConfigListActivity;
import jerry.filebrowser.image.ImageManager;
import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.setting.SettingData;
import jerry.filebrowser.setting.SettingManager;
import jerry.filebrowser.shell.ShellListActivity;
import jerry.filebrowser.task.FileCopyTask;
import jerry.filebrowser.theme.ThemeHelper;
import jerry.filebrowser.util.PathUtil;
import jerry.filebrowser.util.Util;
import jerry.filebrowser.view.ExpandView;
import jerry.filebrowser.view.TagView;

import static jerry.filebrowser.setting.SettingManager.SETTING_DATA;

public class MainActivity extends AppCompatActivity implements ToastInterface {

    private DialogManager dialogManager;

    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private FileBrowserAdapter adapter;
    private RefreshReceiver refreshReceiver;

    //private SharedPreferences preferences;
    private LinearLayout bottomNav;
    private ImageView iv_select;
    public ImageView iv_paste;
    private Toast toast;
    private long exit;
    private DrawerLayout drawer;
    private ArrayList<ExpandView> expendViewList;

    private ExpandView expand_catalog;
    private ExpandView expand_collect;
    private ExpandView expand_remote;
    private ExpandView expand_tool;
    //private ExpandView expand_gesture;

    private TagView tag_sd;

    private final StringBuilder builder = new StringBuilder();

    private View.OnClickListener PathItemListener;

    static {
        System.loadLibrary("file");
    }

    @SuppressLint("ShowToast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        long b = System.currentTimeMillis();
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
//        if (DEBUG) {
//            String whiteList = "logcat -P '" + android.os.Process.myPid() + "'";
//            try {
//                Runtime.getRuntime().exec(whiteList).waitFor();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
        long a = System.currentTimeMillis();
        setContentView(R.layout.activity_main);
        Log.i("666", "MainActivity布局耗时" + (System.currentTimeMillis() - a));

        dialogManager = new DialogManager(this);
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, 0, 0) {
            @Override
            public void onDrawerOpened(@NonNull View drawerView) {
                updateSpace();
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.getDrawerArrowDrawable().setColor(getColor(R.color.text_title));
        toggle.syncState();
        bottomNav = drawer.findViewById(R.id.ll_bottom);

        // 底部新建按钮
        findViewById(R.id.action_create).setOnClickListener(v -> {
            dialogManager.showCreateDialog(FileSetting.getCurrentPath());
        });

        // 底部多选按钮
        iv_select = findViewById(R.id.action_select);
        iv_select.getDrawable().mutate();
        iv_select.setImageTintList(new ColorStateList(
                new int[][]{
                        {-android.R.attr.state_selected},
                        {android.R.attr.state_selected}}, new int[]{
                getColor(R.color.action),
                getColor(R.color.colorAccent)
        }));

        iv_select.setOnClickListener(v -> {
            if (adapter.isMultipleSelectMode()) {
                adapter.quitMultipleSelectMode();
            } else {
                adapter.intoMultipleSelectMode();
            }
        });

        // 底部粘贴按钮
        iv_paste = findViewById(R.id.action_paste);
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{
                        {-android.R.attr.state_enabled},
                        {android.R.attr.state_enabled}},
                new int[]{
                        getColor(R.color.disable),
                        getColor(R.color.action)});

        iv_paste.setImageTintList(colorStateList);
        iv_paste.setEnabled(false);
        iv_paste.setOnClickListener(v -> {
            String dest = FileSetting.getCurrentPath();
            if (Clipboard.path.equals(FileSetting.getCurrentPath())) {
                showToast("不能粘贴到原目录");
                return;
            }
            if (Clipboard.type == Clipboard.TYPE_CUT_SINGLE) {
                UnixFile file = Clipboard.single;
                boolean isSuccess = UnixFile.rename(file.getAbsPath(), PathUtil.mergePath(dest, Clipboard.single.name));
                if (!isSuccess) {
                    showToast("剪切失败");
                }
                adapter.refresh();
//                else {
//                    onQuitCopy();
//                }
            } else if (Clipboard.type == Clipboard.TYPE_CUT_LIST) {
                int error = 0;
                for (UnixFile item : Clipboard.list) {
                    boolean isSuccess = UnixFile.rename(item.getAbsPath(), PathUtil.mergePath(dest, item.name));
                    if (!isSuccess) error++;
                }
                if (error == 0) {
                    showToast("粘贴成功");
                } else {
                    showToast(error + "个文件粘贴失败");
                }
                adapter.refresh();
            } else if (Clipboard.type == Clipboard.TYPE_COPY_SINGLE) {
                ArrayList<UnixFile> list = new ArrayList<>();
                list.add(Clipboard.single);
                FileCopyTask task = new FileCopyTask(list, dest, this);
                task.execute();
            } else if (Clipboard.type == Clipboard.TYPE_COPY_LIST) {
                FileCopyTask task = new FileCopyTask(Clipboard.list, dest, this);
                task.execute();
            }
            onQuitCopy();
        });

        expand_catalog = findViewById(R.id.expand_catalog);
        expand_catalog.bindTextView(findViewById(R.id.tv_catalog));

        expand_collect = findViewById(R.id.expand_collection);
        expand_collect.bindTextView(findViewById(R.id.tv_collection));
        expand_collect.setDrawer(drawer);

        expand_remote = findViewById(R.id.expand_remote);
        expand_remote.bindTextView(findViewById(R.id.tv_remote));

        expand_tool = findViewById(R.id.expand_tool);
        expand_tool.bindTextView(findViewById(R.id.tv_tool));

        expendViewList = new ArrayList<>(4);
        expendViewList.add(expand_catalog);
        expendViewList.add(expand_collect);
        expendViewList.add(expand_remote);
        expendViewList.add(expand_tool);

        PathItemListener = v -> {
            adapter.loadDirectory(((TagView) v).getMessage(), FileBrowserAdapter.TYPE_JUMP);
        };
        recyclerView = findViewById(R.id.recv_file);
        recyclerView.setHasFixedSize(true);
        recyclerView.addItemDecoration(new ItemDecoration(this));
        refreshReceiver = new RefreshReceiver();
        final IntentFilter filter = new IntentFilter(RefreshReceiver.ACTION_REFRESH);
        filter.addAction(RefreshReceiver.ACTION_NAVIGATION);
        registerReceiver(refreshReceiver, filter);

        if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, android.os.Process.myPid(), android.os.Process.myUid()) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            boolean success = SettingManager.read();
            if (success) {
                applyDrawerSettings(SETTING_DATA);
            }
        }

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            adapter = new FileBrowserAdapter(this, recyclerView);
            recyclerView.setAdapter(adapter);
        } else {
            showToast("内部存储空间尚未准备好，请退出重试");
            return;
        }

//        tag_root = drawer.findViewById(R.id.tag_root);
//        tag_root.setOnClick(v -> {
//            showToast("暂不支持浏览根目录");
//        }, drawer);
        tag_sd = expand_catalog.findViewById(R.id.tag_sd);
        tag_sd.setOnClick(v -> {
            adapter.switchRoot(Environment.getExternalStorageDirectory().getAbsolutePath());
        }, drawer);

//        TagView tag_ter = expand_tool.findViewById(R.id.tag_ter);
//        tag_ter.setOnClick((v -> startActivity(new Intent(this, TerminalActivity.class))), drawer);

        TagView tag_shell = expand_remote.findViewById(R.id.tag_shell);
        tag_shell.setOnClick((v -> {
            Intent intent = new Intent(MainActivity.this, ShellListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            startActivity(intent);
        }), drawer);

        TagView tag_ftp = expand_remote.findViewById(R.id.tag_ftp);
        tag_ftp.setOnClick((View v) -> {
            Intent intent = new Intent(MainActivity.this, SFTPConfigListActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
            startActivity(intent);
        }, drawer);

        ((TagView) expand_tool.findViewById(R.id.tag_clear_null)).setOnClick((v -> {
            new FileClearDialog(MainActivity.this).show(FileSetting.getCurrentPath(), adapter.fileList);
        }), drawer);

        ((TagView) expand_tool.findViewById(R.id.tag_dart_mode)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeHelper.setDarkMode(!ThemeHelper.isDarkMode);
            }
        });

        Log.i("666", "MainActivity.onCreate()共耗时" + (System.currentTimeMillis() - b));
    }


    private void collectionPath(String path) {
        final List<String> colList = SETTING_DATA.colList;
        if (!colList.contains(path)) {
            colList.add(path);
            TagView tagView = expand_collect.addTag(PathUtil.getPathName(path), FileSetting.tagPath(path), R.drawable.ic_type_folder, PathItemListener);
            tagView.setOnLongClickListener(v -> {
                SETTING_DATA.colList.remove(FileSetting.innerPath(((TagView) v).getMessage()));
                expand_collect.removeView(v);
                return true;
            });
            showToast("收藏成功");
        } else {
            showToast("已经收藏过该目录了");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        AppUtil.showIcon(menu);
        getMenuInflater().inflate(R.menu.toolbar_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.action_search:
                new SearchDialog(this).show(FileSetting.getCurrentPath());
                break;
            case R.id.action_refresh:
                adapter.refresh();
                break;
//            case R.id.action_paste:
//                break;
//            case R.id.action_create:
//                dialogManager.showCreateDialog(FileManager.getCurrentPath());
//                break;
            case R.id.action_select:
                adapter.intoMultipleSelectMode();
                break;
            case R.id.action_collection:
                if (!FileSetting.PHONE_ROOT.equals(FileSetting.getCurrentPath())) {
                    collectionPath(FileSetting.getCurrentPath());
                }
                break;
            case R.id.action_sort:
                new SortDialog(this).show();
                break;
//            case R.id.action_settings:
//            case R.id.action_upload:
//                new FileSelectDialog(this, new FileSelectCallback() {
//                    @Override
//                    public void OnFileSelected(String path) {
//                        showToast("选择了" + path);
//                    }
//                }).show();
//                break;
            case R.id.action_exit:
                finish();
                break;
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            for (int code : grantResults) {
                if (code != PackageManager.PERMISSION_GRANTED) {
                    showToast("授权失败！");
                    return;
                }
            }
            updateSpace();
            adapter.refresh();
            boolean success = SettingManager.read();
            if (success) {
                applyDrawerSettings(SETTING_DATA);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (!adapter.onBackKey()) {
            if ((System.currentTimeMillis() - exit) > 1000) {
                exit = System.currentTimeMillis();
                showToast("再按一次退出");
            } else {
                super.onBackPressed();
            }
        }
    }

    public void onIntoMultipleSelectMode() {
        iv_select.setSelected(true);
//        iv_select.setImageTintList(ColorStateList.valueOf(0xFF117BFF));
    }

    public void onQuitMultipleSelectMode() {
        iv_select.setSelected(false);
//        iv_select.setImageTintList(ColorStateList.valueOf(0xFFd0d0d0));
    }

    public void onPerformCopy() {
        iv_paste.setEnabled(true);
    }

    public void onQuitCopy() {
        Clipboard.clear();
        iv_paste.setEnabled(false);
    }


    public void onFileInfo(int dirs, int files) {
        builder.append(dirs).append("个文件夹，").append(files).append("个文件");
        toolbar.setSubtitle(builder.toString());
        builder.setLength(0);
    }

    public void updateSpace() {
        final StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
        final long t = statFs.getTotalBytes();
        final long a = statFs.getAvailableBytes();
        tag_sd.setProcess((int) ((t - a) * 100 / t));
        tag_sd.setMessage("共" + Util.size(t)
                + "，可用" + Util.size(a)
                + "，自由" + Util.size(statFs.getFreeBytes()));


//        statFs = new StatFs("/");
//        final long t1 = statFs.getTotalBytes();
//        final long a1 = statFs.getAvailableBytes();
//        tag_root.setMessage("共" + Util.size(t1)
//                + "，可用" + Util.size(a1)
//                + "，自由" + Util.size(statFs.getFreeBytes()));
//        tag_root.setProcess((int) ((t1 - a1) * 100 / t1));
    }

    @Override
    public void showToast(String text) {
        toast.setText(text);
        toast.show();
    }

    @Override
    public void onLowMemory() {
        showToast("内存不足，自动清空缓存");
        if (dialogManager != null) dialogManager.onLowMemory();
        ImageManager.onLowMemory();
        super.onLowMemory();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(refreshReceiver);
        super.onDestroy();
        SettingManager.save(expendViewList);
        if (dialogManager != null) dialogManager.onLowMemory();
        ImageManager.onLowMemory();
    }

    public void applyDrawerSettings(SettingData data) {
        final List<String> colList = data.colList;
        for (String path : colList) {
            final TagView tagView = expand_collect.addTag(PathUtil.getPathName(path), path, R.drawable.ic_type_folder, PathItemListener);
            tagView.setOnLongClickListener(v -> {
                SettingManager.SETTING_DATA.colList.remove(path);
                expand_collect.removeView(v);
                return true;
            });
        }
        final List<Boolean> triggerList = data.triggerList;
        int len = expendViewList.size();
        if (triggerList != null && triggerList.size() == len) {
            for (int i = 0; i < len; i++) {
                expendViewList.get(i).setOpen(triggerList.get(i));
            }
        }
    }

    public void refresh() {
        adapter.refresh();
    }

    public DialogManager getDialogManager() {
        return dialogManager;
    }

    public FileBrowserAdapter getAdapter() {
        return adapter;
    }


    private class RefreshReceiver extends BroadcastReceiver {
        public static final String ACTION_REFRESH = "JERRY.ACTION.REFRESH.MAIN_ACTIVITY";
        public static final String ACTION_NAVIGATION = "JERRY.ACTION.NAVIGATION.MAIN_ACTIVITY";
        public static final String PARAM_PATH = "JERRY.PARAM.PATH.MAIN_ACTIVITY";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (adapter == null) return;
            final String path = intent.getStringExtra(RefreshReceiver.PARAM_PATH);
            if (ACTION_REFRESH.equals(intent.getAction())) {
                if (path == null || FileSetting.getCurrentPath().equals(path))
                    adapter.refresh();
            } else if (ACTION_NAVIGATION.equals(intent.getAction())) {
                if (!FileSetting.getCurrentPath().equals(path)) {
                    adapter.loadDirectory(path, FileBrowserAdapter.TYPE_JUMP);
                }
            }
        }
    }

    public static void sendRefresh(Context context, String path) {
        final Intent intent = new Intent(RefreshReceiver.ACTION_REFRESH);
        if (path != null) intent.putExtra(RefreshReceiver.PARAM_PATH, path);
        context.sendBroadcast(intent);
    }

    public static void sendNavigation(Context context, String path) {
        final Intent intent = new Intent(RefreshReceiver.ACTION_NAVIGATION);
        if (path != null) intent.putExtra(RefreshReceiver.PARAM_PATH, path);
        context.sendBroadcast(intent);
    }
}