package jerry.filebrowser.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import jerry.filebrowser.R;

public class TagView extends ViewGroup {
    private boolean enableProcess;
    private int process = 0;
    private final float offset;
    private final int titleColor;
    private final int subtitleColor;
    private final float subtitleSize = DPUtils.DP10;
    private String title;
    private String message;
    private final ImageView iv_icon;
    private final Paint paint;
    private final StringBuilder builder;
    private String data;


    public TagView(Context context) {
        this(context, null, 0);
    }

    public TagView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TagView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        iv_icon = new ImageView(context);
        iv_icon.setElevation(DPUtils.DP2);
        titleColor = context.getColor(R.color.text_title);
        subtitleColor = context.getColor(R.color.text_subtitle);
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.TagView);
            Drawable icon = typedArray.getDrawable(R.styleable.TagView_icon);
            Drawable iconBackground = typedArray.getDrawable(R.styleable.TagView_iconBackground);
            title = typedArray.getString(R.styleable.TagView_title);
            message = typedArray.getString(R.styleable.TagView_message);
            enableProcess = typedArray.getBoolean(R.styleable.TagView_enableProcess, false);
            typedArray.recycle();
            if (icon != null) {
                iv_icon.setImageDrawable(icon);
            }
            if (iconBackground == null) {
                iv_icon.setBackground(context.getDrawable(R.drawable.tag_bg_light));
            } else {
                iv_icon.setBackground(iconBackground);
            }
        } else {
            iv_icon.setBackground(context.getDrawable(R.drawable.tag_bg_light));
        }
        if (title == null) title = "";
        if (message == null) message = "";
        addView(iv_icon, -1, new LayoutParams(DPUtils.DP36, DPUtils.DP36));
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(5);
        paint.setTextSize(subtitleSize);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        offset = (fontMetrics.bottom - fontMetrics.top - 5) / 2 - fontMetrics.descent;
        builder = new StringBuilder(4);
        setBackgroundResource(R.drawable.ripple);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureChild(iv_icon, widthMeasureSpec, heightMeasureSpec);
        iv_icon.measure(MeasureSpec.makeMeasureSpec(DPUtils.DP36, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(DPUtils.DP36, MeasureSpec.EXACTLY));
        if (enableProcess) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), DPUtils.DP60);
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), DPUtils.DP52);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (enableProcess) {
            iv_icon.layout(DPUtils.DP8, DPUtils.DP12, DPUtils.DP44, DPUtils.DP48);
        } else {
            iv_icon.layout(DPUtils.DP8, DPUtils.DP8, DPUtils.DP44, DPUtils.DP44);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float titleSize = DPUtils.DP16;
        if (enableProcess) {
            paint.setColor(titleColor);
            paint.setTextSize(titleSize);
            canvas.drawText(title, DPUtils.DP52, DPUtils.DP20, paint);
            //进度
            paint.setTextSize(subtitleSize);
            paint.setColor(0xFF117BFF);
            final int end = getWidth() - DPUtils.DP16;
            builder.setLength(0);
            builder.append(process);
            builder.append('%');
            String text = builder.toString();
            final int twidth = (int) paint.measureText(text, 0, text.length());
            final int width = end - DPUtils.DP52 - twidth - DPUtils.DP6;
            final float front = (width / 100f) * process + DPUtils.DP52;
            canvas.drawLine(DPUtils.DP52, DPUtils.DP32, front, DPUtils.DP32, paint);
            canvas.drawCircle(front, DPUtils.DP32, 8, paint);
            canvas.drawText(text, front + DPUtils.DP6, DPUtils.DP32 + offset, paint);
            //canvas.drawLine(DPUtils.DP52 + front + twidth + DPUtils.DP8, height2, end, height2, paint);

            //subtitle
            paint.setColor(subtitleColor);
            canvas.drawText(message, DPUtils.DP52, DPUtils.DP50, paint);
        } else {
            paint.setColor(titleColor);
            paint.setTextSize(titleSize);
            canvas.drawText(title, DPUtils.DP52, DPUtils.DP18 + DPUtils.DP2, paint);

            paint.setColor(subtitleColor);
            paint.setTextSize(subtitleSize);
            canvas.drawText(message, DPUtils.DP52, DPUtils.DP40, paint);
        }
    }

    public void setOnClick(@Nullable OnClickListener listener, DrawerLayout drawerLayout) {
        if (listener == null) {
            super.setOnClickListener(null);
            return;
        }
        super.setOnClickListener(v -> {
            drawerLayout.closeDrawer(Gravity.LEFT);
            postDelayed(() -> listener.onClick(TagView.this), 220);
        });
    }


    public void setIcon(@DrawableRes int id) {
        iv_icon.setImageResource(id);
    }


    public void setProcess(int process) {
        if (process < 0) {
            enableProcess = false;
        } else {
            enableProcess = true;
        }

        if (this.process != process) {
            this.process = process;
            invalidate();
        }
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
        if (message == null) this.message = "";
        invalidate();
    }

    public String getMessage() {
        return message;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
