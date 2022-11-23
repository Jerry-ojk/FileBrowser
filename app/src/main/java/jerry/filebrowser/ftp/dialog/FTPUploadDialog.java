package jerry.filebrowser.ftp.dialog;

import android.content.Context;
import android.text.Editable;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputEditText;

import jerry.filebrowser.R;
import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.dialog.BaseDialog;
import jerry.filebrowser.ftp.SFTPActivity;
import jerry.filebrowser.ftp.SFTPTransportConfig;
import jerry.filebrowser.util.PathUtil;

public class FTPUploadDialog extends BaseDialog {
    private SFTPActivity activity;
    private String localPath;
    private TextInputEditText ed_src;
    private TextInputEditText ed_dest;
    private TextInputEditText ed_name;
    private CheckBox cb_cover;
    private Button bu_cancel;
    private Button bu_sure;

    public FTPUploadDialog(Context context, String localPath) {
        super(context);
        setCancelable(false);
        activity = (SFTPActivity) context;
        ed_src = findViewById(R.id.ed_src);
        ed_src.setText(localPath);
        ed_dest = findViewById(R.id.ed_dest);
        ed_dest.setText(SFTPActivity.CURRENT_PATH);
        ed_name = findViewById(R.id.ed_name);
        ed_name.setText(PathUtil.getPathName(localPath));
        cb_cover = findViewById(R.id.cb_cover);
        bu_sure = findViewById(R.id.bu_sure);
        bu_cancel = findViewById(R.id.bu_cancel);
        this.localPath = localPath;
        bu_sure.setOnClickListener(v -> {
            boolean isSuccess = true;
            final String destDir = getText(ed_dest);
            final String name = getText(ed_name);
            if (AppUtil.isEmpty(destDir) || !destDir.contains("/")) {
                ed_dest.setError("请输入正确的上传目录");
                isSuccess = false;
            }
            if (AppUtil.isEmpty(name) || name.contains("/") || name.length() > 128) {
                ed_name.setError("请输入正确文件名");
                isSuccess = false;
            }

            if (activity.checkFileIsExist(name) && !cb_cover.isChecked()) {
                ed_name.setError("该文件已存在");
                isSuccess = false;
            }
            if (!isSuccess) return;


            SFTPTransportConfig config = new SFTPTransportConfig(localPath, PathUtil.join(destDir, name));
            config.destDir = destDir;
            config.name = name;
            activity.onStartFTPUploadSure(config);
            dismiss();
        });
        bu_cancel.setOnClickListener(v -> dismiss());
    }

//    public void setRemotePath(String remotePath) {
//        this.remotePath = remotePath;
//        ed_src.setText(remotePath);
//    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_ftp_upload;
    }

    public String getText(EditText editText) {
        Editable editable = editText.getText();
        return editable == null ? null : editable.toString();
    }

    @Override
    public void show() {
        super.show();
        ed_src.post(new Runnable() {
            @Override
            public void run() {
                ed_src.clearFocus();
            }
        });
    }
}