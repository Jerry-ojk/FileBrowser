package jerry.filebrowser.ftp.dialog;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
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
import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.file.JerryFile;
import jerry.filebrowser.file.SFTPFile;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.ftp.SFTPActionResult;
import jerry.filebrowser.ftp.SFTPActivity;
import jerry.filebrowser.ftp.task.SFTPActionTask;
import jerry.filebrowser.ssh.SSHResult;
import jerry.filebrowser.util.PathUtil;

public class FTPActionDialog extends BaseFTPDialog {
    private SFTPActivity activity;
    private TextView title;
    private TextView message;
    private TextInputLayout til_name;
    private EditText editText;
    private Button bu_sure;
    private Button bu_cancel;

    private RadioButton newDir;
    private RadioButton newFile;

    private boolean isFile = false;
    private InputMethodManager manager;

    public FTPActionDialog(Context context) {
        super(context);
        activity = ((SFTPActivity) context);
        title = findViewById(R.id.dialog_title);
        til_name = findViewById(R.id.til_name);
        editText = findViewById(R.id.ed_name);
        bu_sure = findViewById(R.id.bu_sure);
        bu_cancel = findViewById(R.id.bu_cancel);
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
        // newDir.setChecked(true);
        newDir.setOnCheckedChangeListener(changeListener);
        newFile = findViewById(R.id.dialog_newFile);
        newFile.setOnCheckedChangeListener(changeListener);

        bu_cancel.setOnClickListener(v -> {
            editText.clearFocus();
            //manager.hideSoftInputFromWindow(editText.getWindowToken(), 0, null);
            dismiss();
        });

    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_edit;
    }

    public void showRenameDialog(SFTPFile file) {
        title.setText("重命名");
        newDir.setVisibility(View.INVISIBLE);
        newFile.setVisibility(View.INVISIBLE);
        editText.setVisibility(View.VISIBLE);
        editText.setText(file.name);
        int end = file.name.lastIndexOf('.');
        if (end == -1) {
            end = file.name.length();
        }
        editText.setSelection(0, end);
        //manager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        message.setVisibility(View.GONE);
        bu_sure.setOnClickListener(v -> {
            String newName = editText.getText().toString();
            String newPath = PathUtil.mergePath(file.getParentPath(), newName);
            if (file.name.equals(newName)) {
                message.setVisibility(View.VISIBLE);
                message.setText("名称原来相同");
                return;
            }
            if (newName.length() > 254 || newPath.length() > 1023) {
                message.setVisibility(View.VISIBLE);
                message.setText("名称过长");
                return;
            }
//            if (UnixFile.isExist(newPath)) {
//                message.setVisibility(View.VISIBLE);
//                message.setText("该文件名已存在");
//                return;
//            }
            buildTask(file.getAbsPath()).execute(SFTPActionTask.TYPE_RENAME);
            manager.hideSoftInputFromWindow(editText.getWindowToken(), 0, null);
        });
        show();
        editText.post(() -> manager.showSoftInput(editText, 0, null));
    }

    public void showCreateDialog(String currentPath) {
        newDir.setVisibility(View.VISIBLE);
        newFile.setVisibility(View.VISIBLE);
        editText.setVisibility(View.VISIBLE);
        title.setText("新建文件");
        bu_sure.setOnClickListener(v -> {
            String newName = editText.getText().toString();
            if (AppUtil.isEmpty(newName)) {
                message.setText("文件名不能为空");
                return;
            }
            if (activity.checkFileIsExist(newName)) {
                message.setText("该文件已存在");
                return;
            }
            String newPath = PathUtil.mergePath(currentPath, newName);
            final SFTPActionTask task = buildTask(newPath);
            if (isFile) {
                task.execute(SFTPActionTask.TYPE_CREATE_FILE);
            } else {
                task.execute(SFTPActionTask.TYPE_CREATE_DIR);
            }
            // manager.hideSoftInputFromWindow(editText.getWindowToken(), 0, null);
            editText.clearFocus();
        });
        show();
        editText.post(() -> manager.showSoftInput(editText, 0, null));
    }

    public void showDeleteDialog(SFTPFile file) {
        newDir.setVisibility(View.INVISIBLE);
        newFile.setVisibility(View.INVISIBLE);
        editText.setVisibility(View.GONE);
        title.setText("删除");
        final SpannableStringBuilder builder = new SpannableStringBuilder("确定要删除");
        int start = 5;
        if (file.type != -1) {
            String type = file.getTypeName();
            builder.append(type);
            start += type.length();
        }
        builder.append(file.name).append("吗？");
        builder.setSpan(new ForegroundColorSpan(0xFF0095FF), start, start + file.name.length(), Spanned.SPAN_MARK_MARK);
        message.setText(builder);
        message.setVisibility(View.VISIBLE);
        bu_sure.setOnClickListener(v -> {
            final SFTPActionTask task = buildTask(file.getAbsPath());
            if (file.type == -1) {
                activity.showToast("未知类型文件，删除失败");
                dismiss();
                return;
            }
            if (file.type == JerryFile.TYPE_DIR) {
                task.execute(SFTPActionTask.TYPE_DELETE_DIR);
            } else {
                task.execute(SFTPActionTask.TYPE_DELETE_FILE);
            }
        });
        show();
    }

    public void showDeleteListDialog(ArrayList<UnixFile> list) {
//        newDir.setVisibility(View.INVISIBLE);
//        newFile.setVisibility(View.INVISIBLE);
//        editText.setVisibility(View.GONE);
//        title.setText("删除");
//        String size = String.valueOf(list.size());
//        SpannableStringBuilder builder = new SpannableStringBuilder("确定要删除选定的")
//                .append(size)
//                .append("个文件吗？");
//        builder.setSpan(new ForegroundColorSpan(0xFF117BFF), 8, 8 + size.length() + 1, Spanned.SPAN_MARK_MARK);
//        message.setText(builder);
//        message.setVisibility(View.VISIBLE);
//        negativeButton.setOnClickListener(v -> {
//            dismiss();
//        });
//        positiveButton.setOnClickListener(v -> {
//            dismiss();
//            //adapter.clear();
//            new FileDeleteTask(list, activity).execute();
//        });
//        show();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        editText.setText("");
        message.setText("");
        bu_sure.setOnClickListener(null);
    }

    @Override
    public void onResult(SFTPActionResult result) {
        if (result.code == SSHResult.CODE_SUCCESS) {

        } else {
            activity.showToast("操作失败");
        }
        dismiss();
        activity.refresh();
    }
}