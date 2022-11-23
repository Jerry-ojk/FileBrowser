package jerry.filebrowser.dialog;

import android.content.Context;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import jerry.filebrowser.R;
import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.file.BaseFile;
import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.task.FileListCallback;
import jerry.filebrowser.task.FileListResult;
import jerry.filebrowser.task.FileSearchTask;

public class SearchDialog extends BaseDialog implements FileListCallback {
    private MainActivity activity;
    private TextInputLayout til_path;
    private TextInputLayout til_name;
    private EditText ed_path;
    private EditText ed_name;
    private CheckBox cb_case_sensitive;
    private CheckBox cb_subdir;
    private CheckBox cb_search_hide;
    private Button bu_sure;

    private FileSearchTask task;

    public SearchDialog(Context context) {
        super(context);
        activity = (MainActivity) context;
        til_path = findViewById(R.id.til_path);
        til_name = findViewById(R.id.til_name);
        ed_path = findViewById(R.id.ed_path);
        ed_name = findViewById(R.id.ed_name);
        cb_case_sensitive = findViewById(R.id.cb_case_sensitive);
        cb_subdir = findViewById(R.id.cb_subdir);
        cb_search_hide = findViewById(R.id.cb_search_hide);

        findViewById(R.id.bu_cancel).setOnClickListener(v -> {
            dismiss();
        });
        bu_sure = findViewById(R.id.bu_sure);
        bu_sure.setOnClickListener(v -> {
            final String path = AppUtil.getString(ed_path);
            if (AppUtil.isEmpty(path)) {
                til_path.setError("路径不能为空");
                return;
            } else if (!UnixFile.isExist(path)) {
                til_path.setError("该路径不存在");
                return;
            }
            final String pattern = AppUtil.getString(ed_name);
            if (AppUtil.isEmpty(pattern)) {
                til_name.setError("名称不能为空");
                return;
            }
            if (task == null) {
                task = new FileSearchTask(this, -1);
                task.pattern = pattern;
                task.isCaseSensitive = cb_case_sensitive.isChecked();
                task.isSearchSubdir = cb_subdir.isChecked();
                task.isSearchHide = cb_search_hide.isChecked();
                task.execute(path);
                bu_sure.setEnabled(false);
            }
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_search;
    }


    @Override
    public void onListResult(FileListResult result) {
        task = null;
        bu_sure.setEnabled(true);
        final ArrayList<BaseFile> list = result.list;
        if (list == null || list.isEmpty()) {
            activity.showToast("搜索结果为空");
        } else {
            new SearchResultDialog(activity).show(list);
            dismiss();
        }
    }

    public void show(String path) {
        ed_path.setText(path);
        ed_name.requestFocus();
        cb_search_hide.setChecked(FileSetting.isShowHide());
        show();

//        ed_name.post(new Runnable() {
//            @Override
//            public void run() {
//                ed_name.requestFocus();
//            }
//        });
    }
}