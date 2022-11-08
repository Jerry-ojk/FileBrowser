package jerry.filebrowser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import jerry.filebrowser.R;
import jerry.filebrowser.adapter.PathNavAdapter;

public class PathNavView extends RecyclerView {
    private final PathNavAdapter adapter;
    private final Runnable runnable = new Runnable() {
        @Override
        public void run() {
            scrollToPosition(adapter.getItemCount() - 1);
        }
    };

    public PathNavView(@NonNull Context context) {
        this(context, null, 0);
    }

    public PathNavView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PathNavView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setItemAnimator(null);
        addItemDecoration(new ArrowItemDecoration(context));
        LinearLayoutManager layoutManager = new LinearLayoutManager(context, RecyclerView.HORIZONTAL, false);
//        layoutManager.setStackFromEnd(true);
        setLayoutManager(layoutManager);
        this.adapter = new PathNavAdapter(this);
        setAdapter(this.adapter);
    }

    public void setPathNavInterface(PathNavAdapter.PathNavInterface pathNavInterface) {
        this.adapter.setPathNavInterface(pathNavInterface);
    }

    public void updatePath(String absolutePath) {
        adapter.updatePath(absolutePath);
        post(runnable);
    }

    public static class ArrowItemDecoration extends RecyclerView.ItemDecoration {
        private final Drawable arrow;
        private final int width;

        public ArrowItemDecoration(Context context) {
            this.arrow = ContextCompat.getDrawable(context, R.drawable.ic_action_next);
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
        public void getItemOffsets(Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            outRect.set(0, 0, DPUtils.DP(24 - 8), 0);
        }
    }
}
