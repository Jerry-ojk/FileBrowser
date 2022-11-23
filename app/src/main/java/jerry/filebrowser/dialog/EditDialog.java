package jerry.filebrowser.dialog;

import android.content.Context;
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

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import jerry.filebrowser.R;
import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.adapter.FileBrowserAdapter;
import jerry.filebrowser.file.BaseFile;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.task.FileDeleteTask;
import jerry.filebrowser.util.PathUtil;

/**
 * Created by Jerry on 2017/10/31
 */

public class EditDialog extends BaseDialog {
    private final MainActivity activity;
    private final FileBrowserAdapter adapter;
    private final InputMethodManager manager;

    private final TextView title;
    private final TextView message;
    private final TextInputLayout til_name;
    private final EditText ed_name;
    private final Button positiveButton;

    private RadioButton newDir;
    private RadioButton newFile;

    private boolean isDir = false;

    public EditDialog(Context context) {
        super(context);
        activity = (MainActivity) context;
        adapter = activity.getAdapter();
        title = findViewById(R.id.dialog_title);
        til_name = findViewById(R.id.til_name);
        ed_name = findViewById(R.id.ed_name);
        positiveButton = findViewById(R.id.bu_sure);

        message = findViewById(R.id.tv_message);
        manager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        CompoundButton.OnCheckedChangeListener changeListener = (buttonView, isChecked) -> {
            if (isChecked) {
                if (buttonView == newDir) {
                    isDir = true;
                    newFile.setChecked(false);
                } else {
                    isDir = false;
                    newDir.setChecked(false);
                }
            }
        };
        newDir = findViewById(R.id.dialog_newDir);
        // newDir.setChecked(true);
        newDir.setOnCheckedChangeListener(changeListener);
        newFile = findViewById(R.id.dialog_newFile);
        newFile.setOnCheckedChangeListener(changeListener);

        findViewById(R.id.bu_cancel).setOnClickListener(v -> {
            //manager.hideSoftInputFromWindow(editText.getWindowToken(), 0, null);
            //editText.clearFocus();
            dismiss();
        });
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_edit;
    }

    public void showRenameDialog(BaseFile file) {
        title.setText("重命名");

        til_name.setVisibility(View.VISIBLE);
        newDir.setVisibility(View.INVISIBLE);
        newFile.setVisibility(View.INVISIBLE);
        message.setVisibility(View.GONE);

        til_name.setHint("名称");
        til_name.setErrorEnabled(false);
        ed_name.setText(file.name);

        int end;
        if (file.isDir()) {
            end = file.name.length();
        } else {
            end = file.name.lastIndexOf('.');
            if (end == -1) {
                end = file.name.length();
            }
        }
        ed_name.setSelection(0, end);

        positiveButton.setOnClickListener(v -> {
            String newName = ed_name.getText().toString();
            String newPath = PathUtil.join(file.getParentPath(), newName);
            if (file.name.equals(newName)) {
                til_name.setError("名称原来相同");
                return;
            }
            if (newName.length() > 254 || newPath.length() > 1023) {
                til_name.setError("名称过长");
                return;
            }
            if (UnixFile.isExist(newPath)) {
                til_name.setError("该文件名已存在");
                return;
            }
            if (UnixFile.rename(file.getAbsPath(), newPath)) {
                activity.showToast("修改成功");
                adapter.notifyItemRename(file.name, newName, newPath);
            } else {
                activity.showToast("修改失败");
            }
            ed_name.clearFocus();
            //manager.hideSoftInputFromWindow(editText.getWindowToken(), 0, null);
            dismiss();
        });
        show();
        ed_name.requestFocus();
        ed_name.postDelayed(() -> manager.showSoftInput(ed_name, InputMethodManager.SHOW_IMPLICIT, null), 200);
    }

    public void showCreateDialog(String currentPath) {
        title.setText("新建");

        til_name.setVisibility(View.VISIBLE);
        newDir.setVisibility(View.VISIBLE);
        newFile.setVisibility(View.VISIBLE);
        message.setVisibility(View.GONE);

        til_name.setHint("名称");

        positiveButton.setOnClickListener(v -> {
            String newName = ed_name.getText().toString();
            if (TextUtils.isEmpty(newName)) {
                til_name.setError("名称不能为空");
                return;
            }
            String newPath = PathUtil.join(currentPath, newName);
            if (UnixFile.isExist(newPath)) {
                til_name.setError("该文件已存在");
                return;
            }
            boolean success;
            if (isDir) {
                success = UnixFile.createDir(newPath);
            } else {
                success = UnixFile.createFile(newPath);
            }
            if (success) {
                adapter.refresh();
                activity.showToast("创建成功");
            } else {
                activity.showToast("创建失败");
            }
            ed_name.clearFocus();
            //manager.hideSoftInputFromWindow(editText.getWindowToken(), 0, null);
            dismiss();
        });
        show();
        ed_name.requestFocus();
        ed_name.postDelayed(() -> manager.showSoftInput(ed_name, InputMethodManager.SHOW_IMPLICIT, null), 200);
    }

    public void showDeleteDialog(BaseFile file) {
        title.setText("删除");

        newDir.setVisibility(View.INVISIBLE);
        newFile.setVisibility(View.INVISIBLE);
        til_name.setVisibility(View.GONE);
        message.setVisibility(View.VISIBLE);

        String type = file.getTypeName();
        StringBuilder builder = new StringBuilder("确定要删除该").append(type).append("吗？");
        message.setText(builder);

        positiveButton.setOnClickListener(v -> {
            if (UnixFile.delete(file.getAbsPath())) {
                adapter.notifyItemDelete(file.name);
            } else {
                activity.showToast("删除失败");
            }
            dismiss();
        });
        show();
    }

    public void showDeleteListDialog(ArrayList<BaseFile> list) {
        title.setText("删除");

        newDir.setVisibility(View.INVISIBLE);
        newFile.setVisibility(View.INVISIBLE);
        til_name.setVisibility(View.GONE);
        message.setVisibility(View.VISIBLE);

        String size = String.valueOf(list.size());
        SpannableStringBuilder builder = new SpannableStringBuilder("确定要删除选定的")
                .append(size)
                .append("个项目吗？");
        builder.setSpan(new ForegroundColorSpan(0xFF117BFF), 8, 8 + size.length() + 1, Spanned.SPAN_MARK_MARK);
        message.setText(builder);

        positiveButton.setOnClickListener(v -> {
            dismiss();
            //adapter.clear();
            new FileDeleteTask(list, activity).execute();
        });
        show();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        ed_name.setText("");
        message.setText("");
        positiveButton.setOnClickListener(null);
    }
}
