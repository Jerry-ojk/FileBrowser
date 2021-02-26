package jerry.filebrowser.dialog;

import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import jerry.filebrowser.R;
import jerry.filebrowser.adapter.FileSelectAdapter;
import jerry.filebrowser.setting.FileSetting;

public class FileSelectDialog extends BaseDialog implements FileSelectAdapterCallback {
    //private Activity activity;
    private FileSelectAdapter adapter;
    private FileSelectCallback callback;
    private DialogCallback dialogCallback;
    private TextView tv_path;
    private TextView tv_count;
    private TextView tv_select;


    public FileSelectDialog(@NonNull Context context, FileSelectCallback callback) {
        super(context);
        //activity = (Activity) context;
        this.callback = callback;
    }

    public void setDialogCallback(DialogCallback dialogCallback) {
        this.dialogCallback = dialogCallback;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_select_file;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tv_path = findViewById(R.id.tv_path);
        tv_select = findViewById(R.id.tv_select);

        RecyclerView recyclerView = findViewById(R.id.dialog_rev);
        recyclerView.setHasFixedSize(true);
        adapter = new FileSelectAdapter(getContext(), recyclerView);
        adapter.setBackView(findViewById(R.id.dir_back));
        adapter.setCountView(findViewById(R.id.tv_count));
        adapter.setFileSelectCallback(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));

        findViewById(R.id.bu_sure).setOnClickListener(v -> {
            callback.OnFileSelected(adapter.getSelectPath());
            dismiss();
        });
        findViewById(R.id.bu_cancel).setOnClickListener(v -> {
            dismiss();
        });
    }

    @Override
    public void OnFileSelected(String path) {
        tv_select.setText(path);
    }

    @Override
    public void OnIntoMultipleSelectMode() {

    }

    @Override
    public void OnQuitMultipleSelectMode() {

    }

    @Override
    public void OnSelectedCount(int count) {

    }

    @Override
    public void onDirectoryChange(String directory) {
        tv_path.setText(directory);
    }

    @Override
    public void onShowToast(String message) {

    }

    public void show(String path) {
        adapter.setRootAndDeauftPath(FileSetting.USER_ROOT, path);
        show();
    }

    @Override
    public void dismiss() {
        super.dismiss();
//        adapter.reset();
    }

    public static interface DialogCallback {
        public void onSure(String path);
    }
}