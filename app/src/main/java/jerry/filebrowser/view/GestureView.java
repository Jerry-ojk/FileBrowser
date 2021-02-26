package jerry.filebrowser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

public class GestureView extends View {
    private Paint paint;
    private float x;
    private float y;
    private int count;
    private float[] points;
    private float[] path;
    private char[] pathc;
    private boolean[] pointsLinked;
    private int color = 0xFFFFFFFF;
    private boolean needDrawPath = false;
    private static final int COLOR_DEFAULT = 0xFFFFFFFF;
    private static final int COLOR_SUCCESS = 0xFF00C853;
    private final float padding;
    private final float touchPadding;
    private Vibrator vibrator;
    private boolean isInputMode = false;


    public GestureView(Context context) {
        this(context, null, 0);
    }

    public GestureView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public GestureView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr, 0);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(COLOR_DEFAULT);
        float dp = getResources().getDisplayMetrics().density;
        padding = 48 * dp;
        touchPadding = 15 * dp;
        paint.setStrokeWidth(dp * 6);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        path = new float[18];
        pointsLinked = new boolean[]{false, false, false,
                false, false, false,
                false, false, false};
        paint.setStrokeCap(Paint.Cap.ROUND);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        getParent().requestDisallowInterceptTouchEvent(true);
        return super.dispatchTouchEvent(event);
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
//        if (points == null) {
//            float width = getWidth() - 2 * padding;
//            float space = width / 2f;
//            float x1 = padding + space;
//            float x2 = x1 + space;
//            points = new float[]{
//                    padding, padding, x1, padding, x2, padding,
//                    padding, x1,/*--*/x1, x1,/*--*/x2, x1,
//                    padding, x2,/*--*/x1, x2,/*--*/x2, x2
//            };
//            if (pathc != null) {
//                for (int i = 0; i < count; i++) {
//                    path[i] = points[pathc[i] * 2];
//                }
//            }
//        }
//        paint.setColor(COLOR_DEFAULT);
//        canvas.drawPoints(points, paint);
//        if (needDrawPath) {
//            int i = 0;
//            while (i < count - 2) {
//                float x0 = path[i];
//                float y0 = path[i += 1];
//                paint.setColor(color);
//                canvas.drawLine(x0, y0, path[i += 1], path[i + 1], paint);
//                paint.setColor(color & 0x6FFFFFFF);
//                canvas.drawCircle(x0, y0, touchPadding, paint);
//            }
//            paint.setColor(color & 0x6FFFFFFF);
//            float x0 = path[count - 2];
//            float y0 = path[count - 1];
//            canvas.drawCircle(x0, y0, touchPadding, paint);
//            if (color == COLOR_DEFAULT) {//没有结束
//                canvas.drawLine(x0, y0, x, y, paint);
//            }
//        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                needDrawPath = false;
                count = 0;
                for (int i = 0; i < 9; i++) {
                    pointsLinked[i] = false;
                }
                color = COLOR_DEFAULT;
            case MotionEvent.ACTION_MOVE:
                x = event.getX();
                y = event.getY();
                for (int i = 0; i <= 4; i += 2) {
                    if (points[i] - touchPadding < x && x < points[i] + touchPadding) {
                        for (int j = 0; j <= 4; j += 2) {
                            if (points[j] - touchPadding < y && y < points[j] + touchPadding) {
                                int pointIndex = i / 2 + j * 3 / 2;
                                if (!pointsLinked[pointIndex]) {
                                    pointsLinked[pointIndex] = true;
                                    needDrawPath = true;
                                    path[count] = points[i];
                                    path[++count] = points[j];
                                    count++;
                                    vibrator.vibrate(50);
                                }
                                break;
                            }
                        }
                        break;
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (isInputMode || (count > 2 && dealPath())) {
                    //color = 0xFF00E676；
                    color = COLOR_SUCCESS;
                } else {
                    color = 0xFFFF5252;
                }
                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                count = 0;
                needDrawPath = false;
                invalidate();
                break;
        }
        return true;
    }

    private boolean dealPath() {
//        char[] pathc = getPath();
//        if (pathc == null) return false;
//        Log.i("666", GestureAction.pathToString(pathc).toString());
//        for (GestureAction action : SettingManager.actionList) {
//            if (action.path.length == count && isPathEqual(action.path, pathc)) {
//                Log.i("666", "doAction" + action.target);
//                ((MainActivity) getContext()).doAction(action);
//                return true;
//            }
//        }
        return false;
    }

    public void clear() {
        count = 0;
        needDrawPath = false;
        invalidate();
    }

    public void setInputMode(boolean inputMode) {
        isInputMode = inputMode;
    }

    public static boolean isPathEqual(char[] a, char[] b) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
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
        if (count > 0) {
            //Log.i("GestureView", "count:" + count);
            char[] pointPath = new char[count];
            for (int i = 0; i < count; i++) {
                pointPath[i] = toIndex(path[i]);
                System.out.println((int) pointPath[i] + "");
                //Log.i("GestureView", "getPath" + i + ":" + (int) pointPath[i]);
            }
            return pointPath;
        } else {
            return null;
        }
    }

    public void setPath(char[] pathc) {
        count = pathc.length;
        if (points != null) {
            for (int i = 0; i < count; i++) {
                path[i] = points[pathc[i] * 2];
            }
        }
        for (int i = 0; i < 9; i++) {
            pointsLinked[i] = false;
        }
        color = COLOR_DEFAULT;
    }

    public char[] pathToChar(float[] path, int count) {
        char[] result = new char[count];
        for (int i = 0; i < count; i++) {
            result[i] = toIndex(path[i]);
            Log.i("GestureView", "pathToChar:" + (int) result[i]);
        }
        return result;
    }

    public float[] pathToFloat(char[] path, int count) {
        float[] result = new float[count];
        for (int i = 0; i < count; i++) {
            result[i] = points[path[i] * 2];
            Log.i("GestureView", "pathToFloat:" + (int) result[i]);
        }
        return result;
    }
}