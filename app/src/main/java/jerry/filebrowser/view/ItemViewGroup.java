package jerry.filebrowser.view;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.widget.TextViewCompat;

import jerry.filebrowser.R;

public class ItemViewGroup extends ViewGroup {
    public AppCompatImageView iv_icon;
    public AppCompatTextView tv_name;
    public AppCompatTextView tv_time;
    public AppCompatTextView tv_size;
    // public final static Typeface typeface = Typeface.create("sans-serif-light", Typeface.NORMAL);


    private final float density;

    public ItemViewGroup(Context context) {
        this(context, null);
    }

    public ItemViewGroup(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        iv_icon = new AppCompatImageView(context);
        tv_name = new AppCompatTextView(context);
        tv_time = new AppCompatTextView(context);
        tv_size = new AppCompatTextView(context);
        iv_icon.setBackgroundResource(R.drawable.folder_bg_oval);

        final int padding = dp2px(7);
        iv_icon.setPadding(padding, padding, padding, padding);
        iv_icon.setElevation(dp2px(2));
        tv_name.setTextSize(16f);
        tv_name.setMaxLines(1);
        tv_name.setGravity(Gravity.CENTER_VERTICAL);
        tv_name.setEllipsize(TextUtils.TruncateAt.END);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(tv_name,
                8, 16,
                1, TypedValue.COMPLEX_UNIT_SP);

        tv_time.setTextSize(12f);
        tv_time.setTextColor(context.getColor(R.color.text_time));
        // tv_time.setTypeface(typeface);

        tv_size.setTextSize(12f);
        tv_size.setTextColor(context.getColor(R.color.text_time));
        // tv_size.setTypeface(typeface);

        addView(iv_icon, new LayoutParams(dp2px(36), dp2px(36)));
        addView(tv_name, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(tv_time, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
        addView(tv_size, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int wantHeight = 0;
        final int paddingTop = dp2px(3);
        final int paddingStart = dp2px(8);
        final int paddingEnd = dp2px(8);
        final int paddingTv = dp2px(5);

        wantHeight += paddingTop;
        final int dp36 = dp2px(36);
        iv_icon.measure(MeasureSpec.makeMeasureSpec(dp36, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(dp36, MeasureSpec.EXACTLY));
        // wantHeight += dp36;

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = 600;
        final int tv_name_width = width - dp36 - paddingTv - paddingStart - paddingEnd;

        // final int tv_size_width = width - dp36 - paddingTv - paddingStart - paddingEnd;

        tv_name.measure(MeasureSpec.makeMeasureSpec(tv_name_width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
        wantHeight += tv_name.getMeasuredHeight();
        tv_size.measure(MeasureSpec.makeMeasureSpec(tv_name_width, MeasureSpec.AT_MOST), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
        tv_time.measure(MeasureSpec.makeMeasureSpec(tv_name_width - tv_size.getMeasuredWidth(), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.AT_MOST));
        wantHeight += tv_size.getMeasuredHeight();
        wantHeight += paddingTop;
        setMeasuredDimension(width, wantHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingTop = dp2px(3);
        final int paddingEnd = dp2px(8);
        final int iv_start = dp2px(8);
        final int width = getWidth() - paddingEnd;

        final int iv_top = (getHeight() - iv_icon.getMeasuredHeight()) / 2;

        iv_icon.layout(iv_start, iv_top, dp2px(8 + 36), iv_top + iv_icon.getMeasuredHeight());

        final int tv_start = dp2px(8 + 36 + 5);

        int temp = paddingTop + tv_name.getMeasuredHeight();
        tv_name.layout(tv_start, paddingTop, width, temp);

        int tv_size_start = width - tv_size.getMeasuredWidth();
        int tv_size_bottom = temp + tv_time.getMeasuredHeight();
        tv_size.layout(tv_size_start, temp, width, tv_size_bottom);
        tv_time.layout(tv_start, temp, tv_size_start, tv_size_bottom);
    }

    private int dp2px(int dp) {
        return (int) (density * dp + 0.5);
    }

    public void setIconPadding(int dp) {
        int padding = dp2px(dp);
        iv_icon.setPadding(padding, padding, padding, padding);
        iv_icon.setBackground(null);
    }

    public void setIconPaddingToDefault() {
        setIconPadding(7);
        iv_icon.setBackgroundResource(R.drawable.folder_bg_oval);
    }

}
