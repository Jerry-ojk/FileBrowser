package jerry.filebrowser.dialog;

import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import jerry.filebrowser.R;
import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.adapter.FileClearListAdapter;
import jerry.filebrowser.file.UnixFile;

public class FileClearDialog extends BaseDialog {
    private MainActivity activity;
    private ArrayList<UnixFile> dirs;
    private RecyclerView recyclerView;
    private FileClearListAdapter adapter;
    private TextView tv_path;
    private TextView message;

    public FileClearDialog(@NonNull Context context) {
        super(context);
        activity = (MainActivity) context;
        recyclerView = findViewById(R.id.dialog_rev);
        recyclerView.setHasFixedSize(true);
        adapter = new FileClearListAdapter(activity, recyclerView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(activity, RecyclerView.VERTICAL, false));
        tv_path = findViewById(R.id.tv_path);
        message = findViewById(R.id.tv_message);

        findViewById(R.id.dialog_select).setOnClickListener(v -> {
            if (dirs == null) return;
            int size = dirs.size();
            for (int i = 0; i < size; i++) {
                dirs.get(i).isSelect = true;
            }
            adapter.notifyDataSetChanged();
        });
        findViewById(R.id.bu_clear).setOnClickListener(v -> {
            if (dirs == null) return;
            int size = dirs.size();
            for (int i = 0; i < size; i++) {
                dirs.get(i).isSelect = false;
            }
            adapter.notifyDataSetChanged();
        });
        findViewById(R.id.bu_cancel).setOnClickListener(v -> dismiss());
        findViewById(R.id.bu_sure).setOnClickListener(v -> {
            if (dirs == null) return;
            int len = dirs.size();
            boolean isSuccess = true;
            boolean isDelete = false;
            for (int i = 0; i < len; i++) {
                UnixFile file = dirs.get(i);
                if (file.isSelect) {
                    if (UnixFile.deleteEmptyDir0(file.getAbsPath())) {
                        isDelete = true;

                    } else {
                        isSuccess = false;
                    }
                }
            }
            if (!isSuccess) {
                activity.showToast("删除失败");
            }
            dismiss();
            if (isDelete) activity.refresh();
        });

        setOnDismissListener(dialog -> {
            dirs = null;
            adapter.setDir(null);
            adapter.notifyDataSetChanged();
            // message.setText("选择要删除的项目");
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_clear;
    }

    public void show(String path, ArrayList<UnixFile> list) {
        message.setText("正在扫描空文件夹");
        tv_path.setText("路径：" + path);
        show();
        ArrayList<UnixFile> dirs = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            final UnixFile file = list.get(i);
            if (file.isDir() && UnixFile.isEmptyDir(file.getAbsPath())) {
                dirs.add(file);
            }
        }
        if (dirs.size() == 0) {
            message.setText("没有空文件夹");
        } else {
            this.dirs = dirs;
            adapter.setDir(dirs);
            adapter.notifyDataSetChanged();
            message.setText("选择要删除的项目");
        }
    }

    @Override
    public void onDetachedFromWindow() {
        Log.i("666", "FileListDialog onDetachedFromWindow");
        super.onDetachedFromWindow();
    }
}
