package jerry.filebrowser.adapter;


import android.animation.ValueAnimator;
import android.content.Context;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import jerry.filebrowser.R;
import jerry.filebrowser.view.DPUtils;
import jerry.filebrowser.view.RotatableDrawable;

public class PathNavAdapter extends RecyclerView.Adapter<PathNavAdapter.ViewHolder> {
    private final RecyclerView recyclerView;
    private final ArrayList<String> pathList = new ArrayList<>();
    private PathNavInterface pathNavInterface;

    private final RotatableDrawable drawable;
    private final ValueAnimator animator = new ValueAnimator();

    private final int color_active;
    private final int color_normal;
    private boolean isLoading;

    public PathNavAdapter(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        Context context = recyclerView.getContext();
        color_active = context.getColor(R.color.text_active);
        color_normal = context.getColor(R.color.text_subtitle);

        drawable = new RotatableDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_refresh));
        drawable.setTint(color_active);
        drawable.setBounds(0, 0, DPUtils.DP16, DPUtils.DP16);

        animator.setIntValues(0, 360 - 1);
        animator.setDuration(400);
        animator.setInterpolator(null);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.addUpdateListener(animation -> {
            if (!isLoading) {
                animation.cancel();
            }
            int value = (int) animation.getAnimatedValue();
            drawable.setDegrees(value);
            drawable.invalidateSelf();
        });
    }

    public void setPathNavInterface(PathNavInterface pathNavInterface) {
        this.pathNavInterface = pathNavInterface;
    }

    public void setLoading(boolean isLoading) {
        this.isLoading = isLoading;
        if (this.isLoading) {
            if (!animator.isStarted()) {
                animator.start();
            }
        } else {
            if (animator.isRunning()) {
                animator.cancel();
            }
        }
        notifyItemChanged(getItemCount() - 1);
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
            if (isLoading) {
                textView.setPadding(DPUtils.DP8, 0, DPUtils.DP8, 0);
                textView.setCompoundDrawables(null, null, drawable, null);
            } else {
                textView.setPadding(DPUtils.DP8, 0, DPUtils.DP8 + DPUtils.DP16, 0);
                textView.setCompoundDrawables(null, null, null, null);
            }
        } else {
            textView.setPadding(DPUtils.DP8, 0, DPUtils.DP8, 0);
            textView.setTextColor(color_normal);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        final TextView textView = (TextView) holder.itemView;
        textView.setCompoundDrawables(null, null, null, null);
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
        if (pathNavInterface != null) {
            pathNavInterface.onNavDirectory(builder.toString(), FileBrowserAdapter.TYPE_JUMP);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface PathNavInterface {
        public void onNavDirectory(String absPath, int type);
    }
}
