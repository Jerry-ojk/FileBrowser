package jerry.filebrowser.adapter;


import android.animation.ValueAnimator;
import android.content.Context;
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
import jerry.filebrowser.view.PathNavView;
import jerry.filebrowser.view.RotatableDrawable;

public class PathNavAdapter extends RecyclerView.Adapter<PathNavAdapter.ViewHolder> {
    private final RecyclerView recyclerView;
    private final ArrayList<String> pathList = new ArrayList<>();

    private PathNavView.OnPathClickListener onPathClickListener;

    private final RotatableDrawable drawable;
    private final ValueAnimator animator = new ValueAnimator();

    private final int colorActive;
    private final int colorNormal;
    private boolean isLoading;

    public PathNavAdapter(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
        Context context = recyclerView.getContext();
        colorActive = context.getColor(R.color.text_active);
        colorNormal = context.getColor(R.color.text_subtitle);

        drawable = new RotatableDrawable(ContextCompat.getDrawable(context, R.drawable.ic_action_refresh));
        drawable.setTint(colorActive);
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

    public void setOnPathClickListener(PathNavView.OnPathClickListener onPathClickListener) {
        this.onPathClickListener = onPathClickListener;
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
            textView.setTextColor(colorActive);
            if (isLoading) {
                textView.setPadding(DPUtils.DP8, 0, DPUtils.DP8, 0);
                textView.setCompoundDrawables(null, null, drawable, null);
            } else {
                textView.setPadding(DPUtils.DP8, 0, DPUtils.DP8 + DPUtils.DP16, 0);
                textView.setCompoundDrawables(null, null, null, null);
            }
        } else {
            textView.setPadding(DPUtils.DP8, 0, DPUtils.DP8, 0);
            textView.setTextColor(colorNormal);
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
        if (onPathClickListener != null) {
            onPathClickListener.onNavDirectory(builder.toString(), FileBrowserAdapter.TYPE_JUMP);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}
