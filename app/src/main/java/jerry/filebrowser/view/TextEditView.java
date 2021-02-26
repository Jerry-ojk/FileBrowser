package jerry.filebrowser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver;

import androidx.appcompat.widget.AppCompatEditText;

public class TextEditView extends AppCompatEditText {
    private Paint paint;
    private Layout layout;
    private final int color;
    private ViewTreeObserver.OnGlobalLayoutListener listener;

    public TextEditView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(1);
        layout = getLayout();
        color = 0xFF3C3C3C;
        paint.setColor(color);


//        ViewTreeObserver observer = getViewTreeObserver();
//
//        listener = new ViewTreeObserver.OnGlobalLayoutListener() {
//            @Override
//            public void onGlobalLayout() {
//                layout = getLayout();
//                if (layout != null) {
//                    Log.i("66", "1");
//                    observer.removeOnGlobalLayoutListener(listener);
//                    listener = null;
//                } else {
//                    Log.i("66", "2");
//                }
//            }
//        };
//        observer.addOnGlobalLayoutListener(listener);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        layout = getLayout();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int start = getSelectionStart();
        if (start != -1) {
            final int s = layout.getLineForOffset(start);
            int n;
            final int end = getSelectionEnd();
            if (start == end) {
                n = s;
            } else {
                n = layout.getLineForOffset(end);
            }
            canvas.drawRect(0, layout.getLineTop(s), getWidth(), layout.getLineTop(n + 1), paint);
        }
        super.onDraw(canvas);
    }
}
