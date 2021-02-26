package jerry.filebrowser.ftp.dialog;

import android.content.Context;
import android.view.View;

import jerry.filebrowser.R;
import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.dialog.SortDialog;
import jerry.filebrowser.ftp.SFTPActivity;
import jerry.filebrowser.setting.SettingManager;

public class FTPSortDialog extends SortDialog {

    public FTPSortDialog(Context context) {
        super(context);
    }

    @Override
    public void updateLayout() {
        final int option = SettingManager.FTP_SETTING_DATA.option;
        int id = R.id.rb_name;
        sortType = option & FileSetting.SORT_MASK;
        switch (sortType) {
            case FileSetting.SORT_BY_TIME:
                id = R.id.rb_time;
                break;
            case FileSetting.SORT_BY_TYPE:
                id = R.id.rb_type;
                break;
            case FileSetting.SORT_BY_SIZE:
                id = R.id.rb_size;
                break;
        }
        group.check(id);
        cb_folder.setChecked(!FileSetting.isMix(option));
        cb_show_hide.setChecked(FileSetting.isShowHide(option));
        cb_reverse.setChecked(FileSetting.isReverse(option));
    }

    public void onSure(View view) {
        int option = sortType;
        if (showMix) option |= FileSetting.SHOW_MIX;
        if (showHide) option |= FileSetting.SHOW_HIDE;
        if (showReverse) option |= FileSetting.SHOW_REVERSE;

        if (SettingManager.FTP_SETTING_DATA.option != option) {
            SettingManager.FTP_SETTING_DATA.option = option;
            SFTPActivity.sendRefresh(getContext(), null);
        }
        dismiss();
    }
}
