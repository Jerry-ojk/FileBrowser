package jerry.filebrowser.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import jerry.filebrowser.R;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.util.Util;
import jerry.filebrowser.activity.ToastInterface;
import jerry.filebrowser.file.JerryFile;

@SuppressLint("SetTextI18n")
public class FilePermissionDialog extends BaseDialog {
    private CheckBox cb_owner_permission_read;
    private CheckBox cb_owner_permission_write;
    private CheckBox cb_owner_permission_execute;

    private CheckBox cb_group_permission_read;
    private CheckBox cb_group_permission_write;
    private CheckBox cb_group_permission_execute;

    private CheckBox cb_other_permission_read;
    private CheckBox cb_other_permission_write;
    private CheckBox cb_other_permission_execute;

    private TextView tv_owner_mode;
    private TextView tv_group_mode;
    private TextView tv_other_mode;

    private TextView tv_mode;

    //private Button bu_sure;

    private int owner_mode = 0;
    private int group_mode = 0;
    private int other_mode = 0;

    protected int oldPermission = 0;
    protected JerryFile file;

    private boolean isMask = false;
    protected ToastInterface toastInterface;
    protected PermissionChangeCallback permissionChangeCallback;


    public FilePermissionDialog(Context context) {
        super(context);
        if (context instanceof ToastInterface) {
            toastInterface = (ToastInterface) context;
        }

        tv_owner_mode = findViewById(R.id.tv_owner_mode);
        tv_group_mode = findViewById(R.id.tv_group_mode);
        tv_other_mode = findViewById(R.id.tv_other_mode);

        tv_mode = findViewById(R.id.tv_mode);

        cb_owner_permission_read = findViewById(R.id.cb_owner_permission_read);
        cb_owner_permission_read.setTag(4);
        cb_owner_permission_write = findViewById(R.id.cb_owner_permission_write);
        cb_owner_permission_write.setTag(2);
        cb_owner_permission_execute = findViewById(R.id.cb_owner_permission_execute);
        cb_owner_permission_execute.setTag(1);

        CompoundButton.OnCheckedChangeListener listener1 = (buttonView, isChecked) -> {
            if (isMask) return;
            int num = (int) buttonView.getTag();
            if (isChecked) {
                owner_mode += num;
            } else {
                owner_mode -= num;
            }
            tv_owner_mode.setText(Integer.toString(owner_mode));
            tv_mode.setText(Util.permission((owner_mode << 6) | (group_mode << 3) | other_mode));
        };

        cb_owner_permission_read.setOnCheckedChangeListener(listener1);
        cb_owner_permission_write.setOnCheckedChangeListener(listener1);
        cb_owner_permission_execute.setOnCheckedChangeListener(listener1);

        cb_group_permission_read = findViewById(R.id.cb_group_permission_read);
        cb_group_permission_read.setTag(4);
        cb_group_permission_write = findViewById(R.id.cb_group_permission_write);
        cb_group_permission_write.setTag(2);
        cb_group_permission_execute = findViewById(R.id.cb_group_permission_execute);
        cb_group_permission_execute.setTag(1);

        CompoundButton.OnCheckedChangeListener listener2 = (buttonView, isChecked) -> {
            if (isMask) return;
            int num = (int) buttonView.getTag();
            if (isChecked) {
                group_mode += num;
            } else {
                group_mode -= num;
            }
            tv_group_mode.setText(Integer.toString(group_mode));
            tv_mode.setText(Util.permission((owner_mode << 6) | (group_mode << 3) | other_mode));
        };
        cb_group_permission_read.setOnCheckedChangeListener(listener2);
        cb_group_permission_write.setOnCheckedChangeListener(listener2);
        cb_group_permission_execute.setOnCheckedChangeListener(listener2);

        cb_other_permission_read = findViewById(R.id.cb_other_permission_read);
        cb_other_permission_read.setTag(4);
        cb_other_permission_write = findViewById(R.id.cb_other_permission_write);
        cb_other_permission_write.setTag(2);
        cb_other_permission_execute = findViewById(R.id.cb_other_permission_execute);
        cb_other_permission_execute.setTag(1);

        CompoundButton.OnCheckedChangeListener listener3 = (buttonView, isChecked) -> {
            if (isMask) return;
            int num = (int) buttonView.getTag();
            if (isChecked) {
                other_mode += num;
            } else {
                other_mode -= num;
            }
            tv_other_mode.setText(Integer.toString(other_mode));
            tv_mode.setText(Util.permission((owner_mode << 6) | (group_mode << 3) | other_mode));
        };
        cb_other_permission_read.setOnCheckedChangeListener(listener3);
        cb_other_permission_write.setOnCheckedChangeListener(listener3);
        cb_other_permission_execute.setOnCheckedChangeListener(listener3);

        findViewById(R.id.bu_cancel).setOnClickListener(v -> dismiss());
        findViewById(R.id.bu_sure).setOnClickListener(this::onSureClick);
    }

    public void setPermissionChangeCallback(FilePermissionDialog.PermissionChangeCallback callback) {
        this.permissionChangeCallback = callback;

    }

    public void setFile(JerryFile file) {
        this.file = file;
    }

    // 000_000_000
    public void setPermissions(int permission) {
        permission &= UnixFile.MASK_SETID_AND_PERMISSION;
        oldPermission = permission;
        isMask = true;
        cb_owner_permission_read.setChecked((permission & 0b100_000_000) != 0);
        cb_owner_permission_write.setChecked((permission & 0b010_000_000) != 0);
        cb_owner_permission_execute.setChecked((permission & 0b001_000_000) != 0);
        owner_mode = ((permission & 0b111_000_000) >> 6);
        tv_owner_mode.setText(Integer.toString(owner_mode));

        cb_group_permission_read.setChecked((permission & 0b000_100_000) != 0);
        cb_group_permission_write.setChecked((permission & 0b000_010_000) != 0);
        cb_group_permission_execute.setChecked((permission & 0b000_001_000) != 0);
        group_mode = ((permission & 0b000_111_000) >> 3);
        tv_group_mode.setText(Integer.toString(group_mode));

        cb_other_permission_read.setChecked((permission & 0b000_000_100) != 0);
        cb_other_permission_write.setChecked((permission & 0b000_000_010) != 0);
        cb_other_permission_execute.setChecked((permission & 0b000_000_001) != 0);
        other_mode = (permission & 0b000_000_111);
        tv_other_mode.setText(Integer.toString(other_mode));
        isMask = false;

        tv_mode.setText(Util.permission(permission));
    }

    public int buildPermissions() {
        int permissions = 0;
        if (cb_owner_permission_read.isChecked()) permissions |= 0b100_000_000;
        if (cb_owner_permission_write.isChecked()) permissions |= 0b010_000_000;
        if (cb_owner_permission_execute.isChecked()) {
            permissions |= 0b001_000_000;
            if ((oldPermission & UnixFile.S_ISUID) != 0) {
                permissions |= UnixFile.S_ISUID;
            }
        }

        if (cb_group_permission_read.isChecked()) permissions |= 0b000_100_000;
        if (cb_group_permission_write.isChecked()) permissions |= 0b000_010_000;
        if (cb_group_permission_execute.isChecked()) {
            permissions |= 0b000_001_000;
            if ((oldPermission & UnixFile.S_ISGID) != 0) {
                permissions |= UnixFile.S_ISGID;
            }
        }

        if (cb_other_permission_read.isChecked()) permissions |= 0b000_000_100;
        if (cb_other_permission_write.isChecked()) permissions |= 0b000_000_010;
        if (cb_other_permission_execute.isChecked()) {
            permissions |= 0b000_000_001;
            if ((oldPermission & UnixFile.S_ISVTX) != 0) {
                permissions |= UnixFile.S_ISVTX;
            }
        }
        return permissions;
    }


    public void refresh(int permission) {
        owner_mode = 0;
        group_mode = 0;
        other_mode = 0;
        if (cb_owner_permission_read.isChecked()) owner_mode += 4;
        if (cb_owner_permission_write.isChecked()) owner_mode += 2;
        if (cb_owner_permission_execute.isChecked()) owner_mode += 1;

        tv_owner_mode.setText(Integer.toString(owner_mode));

        if (cb_group_permission_read.isChecked()) group_mode += 4;
        if (cb_group_permission_write.isChecked()) group_mode += 2;
        if (cb_group_permission_execute.isChecked()) group_mode += 1;
        tv_group_mode.setText(Integer.toString(group_mode));

        if (cb_other_permission_read.isChecked()) other_mode += 4;
        if (cb_other_permission_write.isChecked()) other_mode += 2;
        if (cb_other_permission_execute.isChecked()) other_mode += 1;
        tv_other_mode.setText(Integer.toString(other_mode));

        tv_mode.setText(Util.permission(permission));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_permissions;
    }

    public void onSureClick(View view) {
        final int permission = buildPermissions();
        if (permission != oldPermission) {
            StructStat stat = null;
            try {
                Os.chmod(file.getAbsPath(), permission);
                stat = Os.stat(file.getAbsPath());
            } catch (ErrnoException e) {
                e.printStackTrace();
            }
            if (stat == null) {
                toastInterface.showToast("修改失败");
                return;
            }
            // 和原来不一样就是成功了
            if ((stat.st_mode & UnixFile.MASK_SETID_AND_PERMISSION) != oldPermission) {
                toastInterface.showToast("修改成功");
                permissionChangeCallback.onPermissionChange(stat.st_mode & UnixFile.MASK_SETID_AND_PERMISSION);
                dismiss();
            } else {
                toastInterface.showToast("修改失败");
            }
        }
    }

    public static interface PermissionChangeCallback {
        public void onPermissionChange(int permission);
    }
}