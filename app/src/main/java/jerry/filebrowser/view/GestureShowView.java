package jerry.filebrowser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class GestureShowView extends View {
    private static final int COLOR_DEFAULT = 0xFFFFFFFF;
    private final float padding;
    private final float touchPadding;
    private float[] points;
    private Paint paint;

    private float[] path;
    private char[] pathc;


    public GestureShowView(Context context) {
        this(context, null, 0);
    }

    public GestureShowView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureShowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(COLOR_DEFAULT);
        float dp = getResources().getDisplayMetrics().density;
        padding = 6 * dp;
        touchPadding = 4 * dp;
        paint.setStrokeWidth(dp * 4);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        if (width > height) {
            width = height;
        }
        setMeasuredDimension(width, width);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (points == null) {
            float width = getWidth() - 2 * padding;
            float space = width / 2f;
            float x1 = padding + space;
            float x2 = x1 + space;
            points = new float[]{
                    padding, padding, x1, padding, x2, padding,
                    padding, x1,/*--*/x1, x1,/*--*/x2, x1,
                    padding, x2,/*--*/x1, x2,/*--*/x2, x2
            };
            if (pathc != null) {
                path = pathToFloat(pathc, pathc.length);
            }
        }
        if (path == null) return;
        paint.setColor(COLOR_DEFAULT & 0x6FFFFFFF);
        canvas.drawPoints(points, paint);
        int i = 0;
        while (i < path.length - 2) {
            float x0 = path[i];
            float y0 = path[i += 1];
            canvas.drawLine(x0, y0, path[i += 1], path[i + 1], paint);
            canvas.drawCircle(x0, y0, touchPadding, paint);
        }
        float x0 = path[path.length - 2];
        float y0 = path[path.length - 1];
        canvas.drawCircle(x0, y0, touchPadding, paint);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return true;
    }


    public char toIndex(float x) {
        if (x == points[0]) {
            return 0;
        } else if (x == points[2]) {
            return 1;
        } else {
            return 2;
        }
    }


    public char[] getPath() {
        char[] pointPath = new char[path.length];
        for (int i = 0; i < path.length; i++) {
            pointPath[i] = toIndex(path[i]);
            Log.i("GestureView", "getPath" + pointPath[i]);
        }
        return pointPath;
    }

    public void setPath(char[] pathc) {
        this.pathc = pathc;
        if (points != null) {
            path = pathToFloat(pathc, pathc.length);
            invalidate();
        }
    }

    public char[] pathToChar(float[] path, int count) {
        char[] result = new char[count];
        for (int i = 0; i < count; i++) {
            result[i] = toIndex(path[i]);
            Log.i("GestureView", "pathToChar" + result[i]);
        }
        return result;
    }

    public float[] pathToFloat(char[] path, int count) {
        float[] result = new float[count];
        for (int i = 0; i < count; i++) {
            result[i] = points[path[i] * 2];
            Log.i("GestureView", "pathToFloat:" + result[i]);
        }
        return result;
    }
}
