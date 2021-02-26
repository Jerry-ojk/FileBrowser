package jerry.filebrowser.ftp.dialog;

import android.content.Context;
import android.text.Editable;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import com.google.android.material.textfield.TextInputEditText;

import jerry.filebrowser.R;
import jerry.filebrowser.dialog.BaseDialog;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.ftp.SFTPActivity;
import jerry.filebrowser.ftp.SFTPTransportConfig;
import jerry.filebrowser.setting.SettingManager;
import jerry.filebrowser.util.PathUtil;

public class FTPDownloadDialog extends BaseDialog {
    private SFTPActivity activity;
    private String remotePath;
    private TextInputEditText ed_src;
    private TextInputEditText ed_dest;
    private TextInputEditText ed_name;
    private CheckBox cb_cover;
    private Button bu_cancel;
    private Button bu_sure;

    public FTPDownloadDialog(Context context, String remotePath) {
        super(context);
        activity = (SFTPActivity) context;
        ed_src = findViewById(R.id.ed_src);
        ed_src.setText(remotePath);
        ed_dest = findViewById(R.id.ed_dest);
        ed_dest.setText(SettingManager.FTP_DOWNLOAD_DIR);
        ed_name = findViewById(R.id.ed_name);
        ed_name.setText(PathUtil.getPathName(remotePath));
        cb_cover = findViewById(R.id.cb_cover);
        bu_sure = findViewById(R.id.bu_sure);
        bu_cancel = findViewById(R.id.bu_cancel);
        this.remotePath = remotePath;
        bu_sure.setOnClickListener(v -> {
            boolean isSuccess = true;
            String destDir = getText(ed_dest);
            String name = getText(ed_name);
            if (destDir == null || destDir.length() == 0 || !destDir.contains("/")) {
                ed_dest.setError("请输入正确的下载目录");
                isSuccess = false;
            }
            if (name == null || name.length() == 0 || name.contains("/") || name.length() > 128) {
                ed_name.setError("请输入正确文件名");
                isSuccess = false;
            }
            if (!isSuccess) return;

            final String localPath = PathUtil.mergePath(destDir, name);
            if (UnixFile.isExist(localPath) && !cb_cover.isChecked()) {
                ed_name.setError("该文件已存在");
                return;
            }
            SFTPTransportConfig config = new SFTPTransportConfig(localPath, this.remotePath);
            config.destDir = destDir;
            config.name = name;
            config.config = activity.getFTPConfig();

            activity.onStartFTPDownloadSure(config);
            dismiss();
        });
        bu_cancel.setOnClickListener(v -> dismiss());
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
        ed_src.setText(remotePath);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_ftp_download;
    }

    public String getText(EditText editText) {
        Editable editable = editText.getText();
        return editable == null ? null : editable.toString();
    }
}