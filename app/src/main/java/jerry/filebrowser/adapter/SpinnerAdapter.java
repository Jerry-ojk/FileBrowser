package jerry.filebrowser.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SpinnerAdapter extends BaseAdapter {
    private final String[] items = new String[]{
            "请选择",
            "打开目录",
            "收藏选定目录"
    };

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return getDropDownView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView view;
        if (convertView == null) {
            view = new TextView(parent.getContext());
            view.setTextSize(18);
            view.setPadding(12, 12, 12, 12);
        } else {
            view = (TextView) convertView;
        }
        view.setTextColor(0XFFD0D0D0);
        view.setText(items[position]);
        return view;
    }
}
