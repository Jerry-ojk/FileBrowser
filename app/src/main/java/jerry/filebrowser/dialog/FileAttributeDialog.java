package jerry.filebrowser.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import jerry.filebrowser.file.FileAttribute;
import jerry.filebrowser.R;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.util.Util;
import jerry.filebrowser.activity.MainActivity;

public class FileAttributeDialog extends BaseDialog {
    private MainActivity activity;
    private FileAttribute attribute;
    private TextView tv_name;
    private TextView tv_path;
    private TextView tv_type;
    private TextView tv_size;
    private TextView tv_mode;
    private TextView tv_author;
    private TextView tv_group;
    private TextView tv_atime;
    private TextView tv_mtime;
    private TextView tv_ctime;
    private UnixFile file;

    public FileAttributeDialog(@NonNull Context context) {
        super(context);
        activity = (MainActivity) context;
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
        tv_ctime = findViewById(R.id.dialog_ctime_content);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_attribute;
    }

    public void show(UnixFile file) {
        this.file = file;
        setAttribute(UnixFile.getFileAttribute0(file.getAbsPath()));
        show();
    }

    @SuppressLint("SetTextI18n")
    public void setAttribute(FileAttribute attribute) {
        if (attribute == null) {
            tv_name.setText("读取文件属性失败");
            tv_path.setText("");
            tv_type.setText("");
            tv_size.setText("");
            tv_mode.setText("");
            tv_author.setText("");
            tv_group.setText("");
            tv_atime.setText("");
            tv_mtime.setText("");
            tv_ctime.setText("");
        } else {
            this.attribute = attribute;
            tv_name.setText(attribute.name);
            tv_path.setText(attribute.path);
            tv_type.setText(Util.type(attribute.mode));
            StringBuilder builder = new StringBuilder();
            tv_size.setText(builder.append(Util.size(attribute.size)).append('(').append(attribute.size).append(')').toString());
            if ((attribute.mode) != -1) {
                findViewById(R.id.dialog_change_mode).setOnClickListener(v -> {
                    FilePermissionDialog dialog = new FilePermissionDialog(activity);
                    dialog.setFile(file);
                    dialog.setPermissions(attribute.mode);
                    dialog.setPermissionChangeCallback(permission -> dismiss());
                    dialog.show();
                });
                tv_mode.setText(Util.mode(attribute.mode));
            } else {
                findViewById(R.id.dialog_change_mode).setVisibility(View.INVISIBLE);
                tv_mode.setText("未知");
            }
            builder.setLength(0);
            tv_author.setText(builder.append(attribute.uid).append('-').append(attribute.uname).toString());
            builder.setLength(0);
            tv_group.setText(builder.append(attribute.gid).append('-').append(attribute.gname).toString());
            tv_atime.setText(Util.time(attribute.atime));
            tv_mtime.setText(Util.time(attribute.mtime));
            tv_ctime.setText(Util.time(attribute.ctime));
        }
    }
}