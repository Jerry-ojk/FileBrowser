package jerry.filebrowser.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;

import jerry.filebrowser.R;


/**
 * Created by Jerry on 2018/2/2
 */

public class ExpandView extends ViewGroup {

    public final static int STATE_CLOSED = 0;
    public final static int STATE_EXPANDED = 1;
    public final static int STATE_CLOSING = 2;
    public final static int STATE_EXPANDING = 3;
    public final static int STATE_ALPHA0ING = 4;
    public final static int STATE_ALPHA1ING = 5;

    private final RotatableDrawable arrow;

    private int state = STATE_CLOSED;
    private int action = STATE_CLOSED;

    int total_height = 0;
    int current_height = 0;

    int during = 0;

    private TextView textView;

    private ValueAnimator animator_expand;
    private final ValueAnimator.AnimatorUpdateListener listener;
    //    private OnViewRemoveListener removeListener;
    private boolean isChange = true;

    private DrawerLayout drawer;

    public ExpandView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpandView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        arrow = new RotatableDrawable(getResources().getDrawable(R.drawable.ic_action_pre, null));
        //expandDrawable.mutate();
        //arrow.setTint(context.getResources().getColor(R.color.action, null));
        listener = animation -> {
            current_height = (int) animation.getAnimatedValue();
            arrow.setDegrees(-current_height * 90f / total_height);
            textView.invalidateDrawable(arrow);
            requestLayout();
        };
    }

    public void setDrawer(DrawerLayout drawer) {
        this.drawer = drawer;
    }

    public void bindTextView(TextView textView) {
        this.textView = textView;
//        Drawable drawable = textView.getCompoundDrawablesRelative()[0];
//        if (drawable != null) drawable.setBounds(0, 0, DPUtils.DP36, DPUtils.DP36);
        arrow.setBounds(0, 0, DPUtils.DP24, DPUtils.DP24);
        textView.setCompoundDrawablesRelative(null, null, arrow, null);
        textView.setOnClickListener(v -> {
            if (state == STATE_CLOSED) {
                startExpand();
            } else if (state == STATE_EXPANDED) {
                startClose();
            }
        });
    }

    public TagView addTag(String title, String message, @DrawableRes int iconId, View.OnClickListener listener) {
        TagView tagView = new TagView(getContext());
        tagView.setIcon(iconId);
        tagView.setTitle(title);
        tagView.setMessage(message);
        tagView.setOnClick(listener, drawer);
        addView(tagView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return tagView;
    }


//    @Override
//    public void removeView(View view) {
////        if (removeListener != null) {
////            removeListener.onViewRemove((TagView) view);
////        }
//        super.removeView(view);
//    }

//    public void setOnViewRemoveListener(OnViewRemoveListener removeListener) {
//        this.removeListener = removeListener;
//    }

    @Override
    public void onViewAdded(View child) {
        isChange = true;
        super.onViewAdded(child);
    }

    @Override
    public void onViewRemoved(View child) {
        isChange = true;
        ((TagView) child).setOnClick(null, null);
        super.onViewRemoved(child);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (isChange) {
            isChange = false;
            int temp = 0;
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View item = getChildAt(i);
                if (item.getMeasuredHeight() == 0) {
                    measureChild(item, widthMeasureSpec, heightMeasureSpec);
                }
                temp += item.getMeasuredHeight();
            }
            if (state == STATE_CLOSED) {
                current_height = 0;
                setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), 0);
            } else {
                current_height = temp;
                setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), temp);
            }
            total_height = temp;
            during = 100 + (total_height >> 2);
        } else {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), current_height);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        // if (!changed) return;
        if (state == STATE_EXPANDING || state == STATE_CLOSING) {
            int bottom = current_height;
            int width = getMeasuredWidth();
            int count = getChildCount();
            for (int i = count - 1; i >= 0; i--) {
                View item = getChildAt(i);
                int top = bottom - item.getMeasuredHeight();
                item.layout(0, top, width, bottom);
                bottom = top;
            }
        } else {
            int top = 0;
            int width = getMeasuredWidth();
            int count = getChildCount();
            for (int i = 0; i < count; i++) {
                View item = getChildAt(i);
                item.layout(0, top, width, top += item.getMeasuredHeight());
            }
        }
    }

    private void startExpand() {
        if (getChildCount() != 0) {
            checkAnimator();
            action = STATE_EXPANDING;
            state = STATE_EXPANDING;
            animator_expand.setIntValues(0, total_height);
            animator_expand.start();
        } else {
            state = STATE_EXPANDED;
            arrow.setDegrees(-90);
            //textView.invalidateDrawable(expandDrawable);
        }
    }

    private void startClose() {
        if (getChildCount() != 0) {
            checkAnimator();
            action = STATE_CLOSING;
            state = STATE_ALPHA0ING;
            animate().alpha(0).setDuration(100).withEndAction(new Runnable() {
                @Override
                public void run() {
                    state = STATE_CLOSING;
                    animator_expand.setIntValues(total_height, 0);
                    animator_expand.start();
                }
            }).start();
        } else {
            state = STATE_CLOSED;
            arrow.setDegrees(0);
            textView.invalidateDrawable(arrow);
        }
    }

    private void checkAnimator() {
        if (animator_expand == null) {
            animator_expand = new ValueAnimator();
            animator_expand.setDuration(during);
            //animator_expand.setInterpolator(new AccelerateDecelerateInterpolator());
            animator_expand.addUpdateListener(listener);
            animator_expand.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (action == STATE_EXPANDING) {
                        state = STATE_ALPHA1ING;
                        animate().alpha(1).setDuration(100).withEndAction(() -> state = STATE_EXPANDED).start();
                    } else if (state == STATE_CLOSING) {
                        state = STATE_CLOSED;
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
    }

    public void setOpen(boolean isOpen) {
        if (isOpen) {
            state = STATE_EXPANDED;
            arrow.setDegrees(-90);
            setAlpha(1);
            textView.invalidateDrawable(arrow);
            requestLayout();
        } else {
            state = STATE_CLOSED;
            arrow.setDegrees(0);
            setAlpha(0);
            textView.invalidateDrawable(arrow);
            current_height = 0;
            requestLayout();
        }
    }


    public boolean isOpen() {
        return state == STATE_EXPANDED || state == STATE_EXPANDING || state == STATE_ALPHA1ING;
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (state != STATE_CLOSED) {
            super.dispatchDraw(canvas);
        }
    }

    @Override
    public long getDrawingTime() {
        return super.getDrawingTime();
    }

//    public interface OnViewRemoveListener {
//        public void onViewRemove(TagView view);
//    }
}
