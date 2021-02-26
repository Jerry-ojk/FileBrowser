package jerry.filebrowser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import jerry.filebrowser.R;

public class LineProcess1 extends View {
    private int process = 50;
    private Paint paint;
    private StringBuilder builder;
    private float offset;
    private boolean isProcessMode = true;
    private final int color_active;
    private final int color_normal;

    public void setProcessMode(boolean processMode) {
        isProcessMode = processMode;
    }

    public LineProcess1(Context context) {
        this(context, null);
    }

    public LineProcess1(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineProcess1(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        color_active = context.getColor(R.color.colorAccent);
        color_normal = context.getColor(R.color.text_subtitle);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(5);
        paint.setTextSize(DPUtils.DP10);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        offset = (fontMetrics.bottom - fontMetrics.top - 5) / 2 - fontMetrics.descent;
        builder = new StringBuilder(4);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int height2 = getHeight() >> 1;

        builder.setLength(0);
        builder.append(process);
        builder.append('%');
        String text = builder.toString();

        final int twidth = (int) paint.measureText(text, 0, text.length());

        paint.setColor(color_active);
        // 4 front 8 text 8 behind 4


        final int width = getWidth() - 4 - 8 - twidth - 8 - 4;//可用空间
        float front = width * (process / 100f);
        front += 4;
        if (process > 0) canvas.drawLine(4, height2, front, height2, paint);
        canvas.drawCircle(front, height2, 8, paint);
        front += 8;
        canvas.drawText(text, front, height2 + offset, paint);
        front = front + twidth + 8;
        if (isProcessMode) {
            paint.setColor(color_normal);
            canvas.drawLine(front, height2, getWidth() - 4, height2, paint);
        }
    }

    public void setProcess(int process) {
        if (this.process != process) {
            this.process = process;
            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
    }
}
