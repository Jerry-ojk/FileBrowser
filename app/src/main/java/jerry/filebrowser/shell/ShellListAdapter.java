package jerry.filebrowser.shell;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import jerry.filebrowser.R;
import jerry.filebrowser.util.Util;
import jerry.filebrowser.dialog.AnchorPopupMenu;

public class ShellListAdapter extends RecyclerView.Adapter<ShellListAdapter.ViewHolder> {
    private ShellListActivity activity;
    private RecyclerView recyclerView;
    private AnchorPopupMenu popupMenu;
    private List<ShellConfig> configList;
    private View.OnClickListener listener;
    private View.OnLongClickListener longClickListener;


    public ShellListAdapter(ShellListActivity activity, RecyclerView recyclerView, List<ShellConfig> configList) {
        this.activity = activity;
        this.configList = configList;
        this.recyclerView = recyclerView;
        popupMenu = new AnchorPopupMenu(activity, recyclerView);
        Menu menu = popupMenu.getMenu();
        menu.add(Menu.NONE, 1, Menu.NONE, "修改").setIcon(R.drawable.ic_edit);
        menu.add(Menu.NONE, 2, Menu.NONE, "删除").setIcon(R.drawable.ic_delete);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                final int position = popupMenu.getPosition();
                final ShellConfig config = configList.get(position);
                switch (item.getItemId()) {
                    case 1:
                        activity.onConfigItemEdit(position, config);
                        break;
                    case 2:
                        activity.onConfigItemDelete(position, config);
                        break;
                }
                return true;
            }
        });
        listener = v -> {
            int position = ShellListAdapter.this.recyclerView.getChildAdapterPosition(v);
            this.activity.onConfigItemClick(position, configList.get(position));
        };
        longClickListener = v -> {
            popupMenu.show(v);
            return true;
        };
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_config, parent, false);
        view.setOnClickListener(listener);
        view.setOnLongClickListener(longClickListener);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ShellConfig config = configList.get(position);
        holder.tv_host.setText("地址：shell://" + config.user + '@' + config.host + ":" + config.port);
//        holder.tv_pwd.setText("密码：" + config.pwd);
        if (config.time > 0) {
            holder.tv_time.setText("上次使用：" + Util.time(config.time));
        } else {
            holder.tv_time.setText("未曾使用");
        }
    }

    @Override
    public int getItemCount() {
        return ((configList == null) ? 0 : configList.size());
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv_host;
//        TextView tv_pwd;
        TextView tv_time;

        public ViewHolder(@NonNull View parent) {
            super(parent);
            tv_host = parent.findViewById(R.id.tv_host);
//            tv_pwd = parent.findViewById(R.id.tv_pwd);
            tv_time = parent.findViewById(R.id.tv_time);
        }
    }
}
