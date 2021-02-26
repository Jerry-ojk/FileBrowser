package jerry.filebrowser.shell;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

import com.google.android.material.textfield.TextInputEditText;

import jerry.filebrowser.R;
import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.dialog.BaseDialog;
import jerry.filebrowser.ssh.SSHConnectResult;
import jerry.filebrowser.ssh.SSHResult;
import jerry.filebrowser.ftp.callback.SSHConnectCallback;
import jerry.filebrowser.ssh.SSHConnectTask;

public class ShellConfigDialog extends BaseDialog implements SSHConnectCallback {
    private ShellListActivity activity;
    private ShellConfig config;//config=null表示是新增
    private TextInputEditText ed_host;
    private TextInputEditText ed_port;
    private TextInputEditText ed_user;
    private TextInputEditText ed_pwd;
    private TextView tv_indication;
    private Button bu_cancel;
    private Button bu_test;
    private Button bu_sure;


    public ShellConfigDialog(Context context) {
        super(context);
        activity = (ShellListActivity) context;
        ed_host = findViewById(R.id.ed_name);
        ed_port = findViewById(R.id.ed_port);
        ed_user = findViewById(R.id.ed_username);
        ed_pwd = findViewById(R.id.ed_pwd);
        tv_indication = findViewById(R.id.tv_indication);
        bu_sure = findViewById(R.id.bu_sure);
        bu_test = findViewById(R.id.bu_test);
        bu_cancel = findViewById(R.id.bu_cancel);

        bu_sure.setOnClickListener(v -> {
            ShellConfig config = generateConfig();
            if (config != null) {
                activity.onConfigSure(config);
                dismiss();
            }
        });

        bu_test.setOnClickListener(v -> {
            ShellConfig config = generateConfig();
            if (config != null) {
                tv_indication.setText("测试中");
                tv_indication.setVisibility(View.VISIBLE);
                new SSHConnectTask(ShellConfigDialog.this, config, SSHConnectTask.TYPE_SHELL_TEST).execute();
            }
        });
        bu_cancel.setOnClickListener(v -> dismiss());
    }

    public void setTarget(ShellConfig config) {
        this.config = config;
        ed_host.setText(config.host);
        ed_port.setText(Integer.toString(config.port));
        ed_user.setText(config.user);
        ed_pwd.setText(config.pwd);
    }

    @Override
    protected @LayoutRes
    int getLayoutId() {
        return R.layout.dialog_shell_config;
    }

    public ShellConfig generateConfig() {
        boolean isSuccess = true;
        AppUtil.getString(ed_host);
        String host = AppUtil.getString(ed_host);
        String port = AppUtil.getString(ed_port);
        String user = AppUtil.getString(ed_user);
        String pwd = AppUtil.getString(ed_pwd);
        if (AppUtil.isEmpty(host)) {
            isSuccess = false;
            ed_host.setError("主机不能为空");
        }
        if (AppUtil.isEmpty(port)) {
            ed_port.setText("22");
            port = "22";
        }
        ShellConfig config = null;
        if (isSuccess) {
            config = new ShellConfig();
            config.host = host;
            config.port = Integer.parseInt(port);
            config.user = user;
            config.pwd = pwd;
        }
        return config;
    }

    public ShellConfig getFTPConfig() {
        return config;
    }

    @Override
    public void onConnectResult(SSHConnectResult result) {
        Drawable icon = null;
        switch (result.code) {
            case SSHResult.CODE_SUCCESS:
                icon = activity.getDrawable(R.drawable.ic_check_ok);
                tv_indication.setText("成功");
                break;
            case SSHResult.CODE_ERROR_AUTH:
                tv_indication.setText("用户名或密码不正确");
                break;
            case SSHResult.CODE_ERROR_CONNECT:
                tv_indication.setText("连接失败");
                break;
            default:
                tv_indication.setText("未知错误");
        }
        if (icon == null) icon = activity.getDrawable(R.drawable.ic_check_fail);
        final int h = tv_indication.getHeight();
        icon.setBounds(0, 0, h, h);
        tv_indication.setCompoundDrawables(icon, null, null, null);
        tv_indication.setVisibility(View.VISIBLE);
    }

}