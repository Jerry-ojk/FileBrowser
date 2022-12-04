package jerry.filebrowser.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import jerry.filebrowser.R;
import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.file.BaseFile;
import jerry.filebrowser.file.UnixFile;

public class FileSearchListAdapter extends RecyclerView.Adapter<FileSearchListAdapter.ViewHolder> {
    private ArrayList<BaseFile> list;

    private Context context;
    //view
    private RecyclerView recyclerView;

    private Drawable icon_folder;
    private View.OnClickListener listener;

    public FileSearchListAdapter(Context context, RecyclerView recyclerView) {
        this.context = context;
        this.recyclerView = recyclerView;
        icon_folder = ContextCompat.getDrawable(context, R.drawable.ic_type_folder);
    }

    public void setItemClickListener(View.OnClickListener listener) {
        this.listener = listener;
    }

    public void setResult(ArrayList<BaseFile> list) {
        this.list = list;
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_search_list, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnClickListener(listener);
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final BaseFile item = list.get(position);
        holder.image.setImageDrawable(icon_folder);
        holder.name.setText(item.name);
        holder.path.setText(item.getAbsPath());
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {

    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView image;
        private TextView name;
        private TextView path;

        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.iv_image);
            name = itemView.findViewById(R.id.tv_name);
            path = itemView.findViewById(R.id.tv_path);
        }
    }
}