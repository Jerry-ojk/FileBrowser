package jerry.filebrowser.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import jerry.filebrowser.R;
import jerry.filebrowser.util.Util;
import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.file.UnixFile;

public class FileClearListAdapter extends RecyclerView.Adapter<FileClearListAdapter.ViewHolder> {
    private String currentPath;
    private ArrayList<UnixFile> dirs;

    private MainActivity activity;
    //view
    private RecyclerView recyclerView;

    //callback
    private CompoundButton.OnCheckedChangeListener checkedListener;

    private Drawable icon_folder;

    public FileClearListAdapter(MainActivity activity, RecyclerView recyclerView) {
        this.activity = activity;
        this.recyclerView = recyclerView;
        icon_folder = activity.getDrawable(R.drawable.ic_type_folder);

        checkedListener = new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Integer pos = (Integer) buttonView.getTag();
                dirs.get(pos).isSelect = isChecked;
            }
        };
    }

    public void setDir(ArrayList<UnixFile> dirs) {
        this.dirs = dirs;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView image;
        private TextView name;
        private TextView time;
        private CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.iv_image);
            time = itemView.findViewById(R.id.tv_time);
            name = itemView.findViewById(R.id.tv_name);
            checkBox = itemView.findViewById(R.id.cb);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_check_file_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        UnixFile item = dirs.get(position);

        holder.image.setImageDrawable(icon_folder);
        holder.name.setText(item.name);
        holder.time.setText(Util.time(item.time));
        holder.checkBox.setChecked(item.isSelect);
        holder.checkBox.setTag(position);
        holder.checkBox.setOnCheckedChangeListener(checkedListener);
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setTag(null);
    }

    @Override
    public int getItemCount() {
        return dirs == null ? 0 : dirs.size();
    }


    public void reset() {
        int len = dirs.size();
        for (int i = 0; i < len; i++) {
            dirs.get(i).isSelect = false;
        }
    }
}