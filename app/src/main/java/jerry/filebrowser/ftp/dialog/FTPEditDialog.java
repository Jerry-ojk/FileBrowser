package jerry.filebrowser.ftp.dialog;

import android.content.Context;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

import jerry.filebrowser.R;
import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.adapter.FileBrowserAdapter;
import jerry.filebrowser.dialog.BaseDialog;
import jerry.filebrowser.file.JerryFile;
import jerry.filebrowser.util.PathUtil;

/**
 * Created by Jerry on 2017/10/31
 */

public class FTPEditDialog extends BaseDialog {
    private TextView title;
    private TextView message;
    private EditText editText;
    private Button positiveButton;
    private Button negativeButton;

    private RadioButton newDir;
    private RadioButton newFile;

    private boolean isFile = false;
    private FileBrowserAdapter adapter;
    private InputMethodManager manager;
    private MainActivity activity;

    private FTPEditDialogCallback callback;


    public FTPEditDialog(Context context) {
        super(context);
        activity = (MainActivity) context;
        adapter = activity.getAdapter();
        title = findViewById(R.id.dialog_title);
        editText = findViewById(R.id.dialog_lp);
        positiveButton = findViewById(R.id.bu_sure);
        negativeButton = findViewById(R.id.bu_cancel);
        message = findViewById(R.id.tv_message);
        manager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        CompoundButton.OnCheckedChangeListener changeListener = (buttonView, isChecked) -> {
            if (isChecked) {
                if (buttonView == newDir) {
                    isFile = false;
                    newFile.setChecked(false);
                } else {
                    isFile = true;
                    newDir.setChecked(false);
                }
            }
        };
        newDir = findViewById(R.id.dialog_newDir);
        newDir.setChecked(true);
        newDir.setOnCheckedChangeListener(changeListener);
        newFile = findViewById(R.id.dialog_newFile);
        newFile.setOnCheckedChangeListener(changeListener);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_edit;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    public void showRenameDialog(String oldPath) {
        title.setText("重命名");
        newDir.setVisibility(View.INVISIBLE);
        newFile.setVisibility(View.INVISIBLE);
        editText.setVisibility(View.VISIBLE);
        final String name = PathUtil.getPathName(oldPath);
        final String parent = PathUtil.getPathParent(oldPath);
        editText.setText(name);

        editText.setSelection(0, name.length());
        //manager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        message.setVisibility(View.GONE);
        positiveButton.setOnClickListener(v -> {
            message.setText("");
            String newName = editText.getText().toString();
            if (name.equals(newName)) {
                message.setText("名称原来相同");
                return;
            }
            String newPath = PathUtil.mergePath(parent, newName);
            if (newName.length() > 254 || newPath.length() > 1024) {
                message.setVisibility(View.VISIBLE);
                message.setText("名称过长");
                return;
            }
            if (callback != null) {
                callback.onRenameSure(oldPath, newPath);
            }
            manager.hideSoftInputFromWindow(editText.getWindowToken(), 0, null);
            dismiss();
        });

        negativeButton.setOnClickListener(v -> {
            manager.hideSoftInputFromWindow(editText.getWindowToken(), 0, null);
            dismiss();
        });
        show();
        editText.post(() -> manager.showSoftInput(editText, 0, null));
    }

    public void showCreateDialog(String currentPath) {
        newDir.setVisibility(View.VISIBLE);
        newFile.setVisibility(View.VISIBLE);
        editText.setVisibility(View.VISIBLE);
        title.setText("新建文件");
        negativeButton.setOnClickListener(v -> {
            manager.hideSoftInputFromWindow(editText.getWindowToken(), 0, null);
            dismiss();
        });
        positiveButton.setOnClickListener(v -> {
            String newName = editText.getText().toString();
            if (TextUtils.isEmpty(newName)) {
                message.setText("文件名不能为空");
                return;
            }
            if (callback != null) {
                callback.onCreateSure(isFile, PathUtil.mergePath(currentPath, newName));
            }
            manager.hideSoftInputFromWindow(editText.getWindowToken(), 0, null);
            dismiss();
        });
        show();
        editText.post(() -> manager.showSoftInput(editText, 0, null));
    }

    public void showDeleteDialog(String path) {
        newDir.setVisibility(View.INVISIBLE);
        newFile.setVisibility(View.INVISIBLE);
        editText.setVisibility(View.GONE);
        title.setText("删除");
        final String name = PathUtil.getPathName(path);
        SpannableStringBuilder builder = new SpannableStringBuilder("确定要删除").append(name).append("吗？");
        int start = 6 + name.length();
        builder.setSpan(new ForegroundColorSpan(0xFF0095FF), start, start + name.length(), Spanned.SPAN_MARK_MARK);
        message.setText(builder);
        message.setVisibility(View.VISIBLE);
        negativeButton.setOnClickListener(v -> dismiss());
        positiveButton.setOnClickListener(v -> {
            if (callback != null) {
                callback.onDeleteSure(path);
            }
            dismiss();
        });
        show();
    }

    public void showDeleteListDialog(ArrayList<JerryFile> list) {
        newDir.setVisibility(View.INVISIBLE);
        newFile.setVisibility(View.INVISIBLE);
        editText.setVisibility(View.GONE);
        title.setText("删除");
        String size = String.valueOf(list.size());
        SpannableStringBuilder builder = new SpannableStringBuilder("确定要删除选定的")
                .append(size)
                .append("个文件吗？");
        builder.setSpan(new ForegroundColorSpan(0xFF117BFF), 9, 9 + size.length(), Spanned.SPAN_MARK_MARK);
        message.setText(builder);
        message.setVisibility(View.VISIBLE);
        negativeButton.setOnClickListener(v -> {
            dismiss();
        });
        positiveButton.setOnClickListener(v -> {
            dismiss();
        });
        show();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        editText.setText("");
        message.setText("");
        positiveButton.setOnClickListener(null);
        negativeButton.setOnClickListener(null);
    }

    public void setCallback(FTPEditDialogCallback callback) {
        this.callback = callback;
    }

    public static interface FTPEditDialogCallback {
        public void onRenameSure(String oldPath, String newPath);

        public void onCreateSure(boolean isFile, String absPath);

        public void onDeleteSure(String absPath);
    }
}
