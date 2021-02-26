package jerry.filebrowser.ftp.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import jerry.filebrowser.file.SFTPAttrs;

import jerry.filebrowser.R;
import jerry.filebrowser.util.Util;
import jerry.filebrowser.ftp.SFTPActionResult;
import jerry.filebrowser.ftp.SFTPActivity;
import jerry.filebrowser.file.SFTPFile;

public class FTPFileAttributeDialog extends BaseFTPDialog {
    private SFTPActivity activity;

    private SFTPFile file;
    private TextView tv_name;
    private TextView tv_path;
    private TextView tv_type;
    private TextView tv_size;
    private TextView tv_mode;
    private TextView tv_author;
    private TextView tv_group;
    private TextView tv_atime;
    private TextView tv_mtime;
    //private TextView tv_ctime;

    // private FTPActionTask task;

    public FTPFileAttributeDialog(@NonNull Context context) {
        super(context);
        activity = (SFTPActivity) context;
        setContentView(R.layout.dialog_attribute);
        tv_name = findViewById(R.id.dialog_name_content);
        tv_path = findViewById(R.id.dialog_path_content);
        tv_type = findViewById(R.id.dialog_type_content);
        tv_size = findViewById(R.id.dialog_size_content);
        tv_mode = findViewById(R.id.dialog_mode_content);
        tv_author = findViewById(R.id.dialog_author_content);
        tv_group = findViewById(R.id.dialog_group_content);
        tv_atime = findViewById(R.id.dialog_atime_content);
        tv_mtime = findViewById(R.id.dialog_mtime_content);
        findViewById(R.id.dialog_ctime).setVisibility(View.INVISIBLE);
        findViewById(R.id.dialog_ctime_content).setVisibility(View.INVISIBLE);
        setOnCancelListener(dialog -> {
            Log.i("666", "FTPFileAttributeDialog cancel");
            if (task != null) {
                task.cancel(true);
                task = null;
            }
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_attribute;
    }

    public void show(SFTPFile file) {
        this.file = file;
//        if (file instanceof FTPLinkFile) {
//
//        } else if (file.getAttrs().isLink()) {
//            getLinkRefer();
//        }
        setAttribute(file);
        show();
    }

    @SuppressLint("SetTextI18n")
    public void setAttribute(SFTPFile file) {
        final SFTPAttrs attrs = file.getAttrs();
        if (attrs == null) {
            tv_name.setText("读取文件属性失败");
            tv_path.setText("");
            tv_type.setText("");
            tv_size.setText("");
            tv_mode.setText("");
            tv_author.setText("");
            tv_group.setText("");
            tv_atime.setText("");
            tv_mtime.setText("");
            // tv_ctime.setText("");
        } else {
            tv_name.setText(file.name);
            tv_path.setText(file.getAbsPath());
            tv_type.setText(Util.type(attrs.permissions));

            StringBuilder builder = new StringBuilder();
            if (attrs.size > 0) {
                tv_size.setText(builder.append(Util.size(attrs.size)).append('(').append(attrs.size).append(')'));
                builder.setLength(0);
            } else {
                tv_size.setText(Util.size(attrs.size));
            }
            if ((attrs.permissions) != -1) {
                findViewById(R.id.dialog_change_mode).setOnClickListener(v -> {
                    FTPPermissionDialog dialog = new FTPPermissionDialog(activity);
                    dialog.setFTPFile(this.file);
                    dialog.show();
                });
                tv_mode.setText(attrs.getPermissionsString());
            } else {
                findViewById(R.id.dialog_change_mode).setVisibility(View.INVISIBLE);
                tv_mode.setText("未知");
            }
            tv_mode.setText(attrs.getPermissionsString());
            tv_author.setText(builder.append("UID: ").append(attrs.uid).toString());
            builder.setLength(0);
            tv_group.setText(builder.append("GID：").append(attrs.gid).toString());
            tv_atime.setText(Util.time(attrs.atime * 1000L));
            tv_mtime.setText(Util.time(attrs.mtime * 1000L));
            // tv_ctime.setText(Util.time(attrs.ctime));
        }
    }

    public void getLinkRefer() {
        if (task != null) {
            task.cancel(true);
        }
        // task = new FTPActionTask();
    }

    @Override
    public void onResult(SFTPActionResult result) {

    }
}
