package jerry.filebrowser.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class LineProcess extends View {
    private int process = 0;
    private Paint paint;
    private StringBuilder builder;
    private float offset;

    public LineProcess(Context context) {
        this(context, null);
    }

    public LineProcess(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LineProcess(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFF117BFF);
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

        final int width = getWidth() - twidth - 18 - 8;//可用空间
        final float front = (width / 100f) * process;
        if (front != 0) canvas.drawLine(4, height2, front, height2, paint);
        canvas.drawCircle(front + 8, height2, 8, paint);
        canvas.drawText(text, front + 18 + 8, height2 + offset, paint);
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
