package jerry.filebrowser.adapter;


import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import jerry.filebrowser.R;
import jerry.filebrowser.view.DPUtils;

public class PathNavAdapter extends RecyclerView.Adapter<PathNavAdapter.ViewHolder> {
    private RecyclerView recyclerView;
    private ArrayList<String> pathList = new ArrayList<>();
    private PathNavInterface pathNavInterface;
    private final int color_active;
    private final int color_normal;

    public PathNavAdapter(PathNavInterface pathNavInterface, RecyclerView recyclerView) {
        this.pathNavInterface = pathNavInterface;
        this.recyclerView = recyclerView;
        Context context = recyclerView.getContext();
//        color_active = context.getColor(R.color.colorAccent);
        color_active = context.getColor(R.color.text_active);
        color_normal = context.getColor(R.color.text_subtitle);
//        color_normal = 0xFFFFFFFF;
    }

    public void updatePath(String path) {
        final String[] newPaths = path.split("/", 0);
        final int len = newPaths.length;
        pathList.clear();
        if (len == 0) {
            pathList.add(path);
        } else {
            if (newPaths[0].length() == 0) {
                newPaths[0] = "/";
            }
            for (int i = 0; i < len; i++) {
                pathList.add(newPaths[i]);
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        TextView textView = new TextView(context);
        textView.setPadding(DPUtils.DP8, 0, DPUtils.DP8, 0);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(15);
        textView.setBackgroundResource(R.drawable.ripple);
        textView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, DPUtils.DP(32)));
        textView.setOnClickListener(this::onClick);
        return new ViewHolder(textView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final TextView textView = (TextView) holder.itemView;
        textView.setText(pathList.get(position));
        if (position == pathList.size() - 1) {
            textView.setTextColor(color_active);
        } else {
            textView.setTextColor(color_normal);
        }
    }

    @Override
    public int getItemCount() {
        return pathList.size();
    }

    private void onClick(View view) {
        final int index = recyclerView.getChildAdapterPosition(view);
        StringBuilder builder = new StringBuilder();
        builder.append(pathList.get(0));
        int i = 1;
        while (i <= index) {
            if (builder.charAt(builder.length() - 1) != '/') {
                builder.append('/');
            }
            builder.append(pathList.get(i));
            i++;
        }
        pathNavInterface.loadDirectory(builder.toString(), FileBrowserAdapter.TYPE_JUMP);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static interface PathNavInterface {
        public void loadDirectory(String absPath, int type);
    }
}
