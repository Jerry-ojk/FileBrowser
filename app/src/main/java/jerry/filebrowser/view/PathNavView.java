package jerry.filebrowser.view;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import jerry.filebrowser.R;
import jerry.filebrowser.adapter.PathNavAdapter;

public class PathNavView {
    private RecyclerView recyclerView;
    private PathNavAdapter adapter;
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            recyclerView.scrollToPosition(adapter.getItemCount() - 1);
        }
    };

    public PathNavView(Activity activity, PathNavAdapter.PathNavInterface pathNavInterface) {
        recyclerView = activity.findViewById(R.id.recv_path);
        recyclerView.setItemAnimator(null);
        recyclerView.addItemDecoration(new ArrowItemDecoration(activity));
        LinearLayoutManager layoutManager = new LinearLayoutManager(activity, RecyclerView.HORIZONTAL, false);
//        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        this.adapter = new PathNavAdapter(pathNavInterface, recyclerView);
        recyclerView.setAdapter(this.adapter);
    }

    public void updatePath(String absolutePath) {
        adapter.updatePath(absolutePath);
        recyclerView.post(runnable);
    }

    public static class ArrowItemDecoration extends RecyclerView.ItemDecoration {
        private Drawable arrow;
        private final int width;

        public ArrowItemDecoration(Context context) {
            this.arrow = context.getDrawable(R.drawable.ic_action_next);
            this.arrow.setTint(context.getColor(R.color.text_subtitle));
            width = DPUtils.DP(24);
        }

        @Override
        public void onDraw(@NonNull Canvas canvas, RecyclerView parent, @NonNull RecyclerView.State state) {
            final int top = (parent.getHeight() - width) / 2;
            final int count = parent.getChildCount() - 1;
            for (int i = 0; i < count; i++) {
                int left = parent.getChildAt(i + 1).getLeft() - width + DPUtils.DP4;
                arrow.setBounds(left, top, left + width, top + width);
                arrow.draw(canvas);
            }
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            outRect.set(0, 0, DPUtils.DP(24 - 8), 0);
        }
    }
}
