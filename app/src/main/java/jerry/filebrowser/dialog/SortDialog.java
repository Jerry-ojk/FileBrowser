package jerry.filebrowser.dialog;

import android.content.Context;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioGroup;

import jerry.filebrowser.R;
import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.setting.FileSetting;

public class SortDialog extends BaseDialog {
    protected RadioGroup group;
    protected CheckBox cb_folder;
    protected CheckBox cb_reverse;
    protected CheckBox cb_show_hide;
    protected int sortType;
    protected boolean showMix = false;
    protected boolean showHide = false;
    protected boolean showReverse = false;

    public SortDialog(Context context) {
        super(context);
        group = findViewById(R.id.rg_sort);
        cb_folder = findViewById(R.id.cb_folder_always_front);
        cb_reverse = findViewById(R.id.cb_reverse);
        cb_show_hide = findViewById(R.id.cb_show_hide);
        group.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.rb_time:
                    sortType = FileSetting.SORT_BY_TIME;
                    break;
                case R.id.rb_type:
                    sortType = FileSetting.SORT_BY_TYPE;
                    break;
                case R.id.rb_size:
                    sortType = FileSetting.SORT_BY_SIZE;
                    break;
                default:
                    sortType = FileSetting.SORT_BY_NAME;
            }
        });
        cb_folder.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showMix = !isChecked;
        });
        cb_show_hide.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showHide = isChecked;
        });
        cb_reverse.setOnCheckedChangeListener((buttonView, isChecked) -> {
            showReverse = isChecked;
        });

        findViewById(R.id.bu_cancel).setOnClickListener(((v) -> dismiss()));
        findViewById(R.id.bu_sure).setOnClickListener(this::onSure);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_sort;
    }

    public void updateLayout() {
        int id = R.id.rb_name;
        sortType = FileSetting.OPTION & FileSetting.SORT_MASK;
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
        cb_folder.setChecked(!FileSetting.isMix());
        cb_show_hide.setChecked(FileSetting.isShowHide());
        cb_reverse.setChecked(FileSetting.isReverse());
    }

    @Override
    public void show() {
        updateLayout();
        super.show();
    }

    public void onSure(View view) {
        int option = sortType;
        if (showMix) option |= FileSetting.SHOW_MIX;
        if (showHide) option |= FileSetting.SHOW_HIDE;
        if (showReverse) option |= FileSetting.SHOW_REVERSE;

        if (FileSetting.OPTION != option) {
            FileSetting.OPTION = option;
            MainActivity.sendRefresh(getContext(), null);
        }
        dismiss();
    }
}
