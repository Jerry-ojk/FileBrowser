package jerry.filebrowser.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

import jerry.filebrowser.R;
import jerry.filebrowser.view.DPUtils;

/**
 * Created by Jerry on 2017/10/22
 */

public class ItemDecoration extends RecyclerView.ItemDecoration {
    private Paint paint;

    public ItemDecoration(Context context) {
        paint = new Paint();
        paint.setColor(context.getColor(R.color.colorDecoration));
        paint.setStrokeWidth(1);
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
        final int left = parent.getLeft() + 147;
        final int right = parent.getRight() - DPUtils.DP12;
        int i = parent.getChildCount() - 1;
        for (; i > 0; i--) {
            int y = parent.getChildAt(i - 1).getBottom() + 1;
            canvas.drawLine(left, y, right, y, paint);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        outRect.set(0, 0, 0, 1);
    }
}
