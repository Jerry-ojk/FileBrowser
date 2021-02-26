package jerry.filebrowser.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import jerry.filebrowser.R;
import jerry.filebrowser.activity.GestureActivity;
import jerry.filebrowser.view.GestureShowView;

public class GestureAdapter extends RecyclerView.Adapter<GestureAdapter.ViewHolder> {
    private GestureActivity activity;
    private View.OnClickListener listener_open;
    private View.OnClickListener listener_del;
//    private Setting1 setting1;

    public GestureAdapter(GestureActivity activity) {
        this.activity = activity;
        listener_open = v -> {
            Integer pos = (Integer) v.getTag();
            GestureAdapter.this.activity.showEditDialog(pos);
        };
        listener_del = v -> {
            Integer pos = (Integer) ((View) v.getParent()).getTag();
//            setting1.actionList.remove(pos.intValue());
            notifyItemDelete(pos);
        };
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gesture, parent, false);
        view.setOnClickListener(listener_open);
        ViewHolder holder = new ViewHolder(view);
        holder.bu_del.setOnClickListener(listener_del);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.setTag(position);
//        holder.showView.setPath(setting1.actionList.get(position).path);
//        holder.textView.setText("打开目录-" + setting1.actionList.get(position).target);
    }

    @Override
    public int getItemCount() {
//        return setting1.actionList.size();
        return 0;
    }

    public void notifyItemDelete(int position) {
        notifyItemRangeRemoved(position, 1);
//        if (setting1.actionList.size() > position) {
//            notifyItemRangeChanged(position, setting1.actionList.size() - position);
//        }
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        GestureShowView showView;
        TextView textView;
        ImageView bu_del;

        public ViewHolder(View itemView) {
            super(itemView);
            showView = itemView.findViewById(R.id.gestureShowView);
            textView = itemView.findViewById(R.id.tv_action);
            bu_del = itemView.findViewById(R.id.bu_del);
        }
    }
}
