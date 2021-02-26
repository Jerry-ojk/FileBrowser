package jerry.filebrowser.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class BottomScrollView extends ScrollView {


    public BottomScrollView(Context context) {
        this(context, null);
    }

    public BottomScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setFillViewport(true);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        if (changed) {
            fullScroll(ScrollView.FOCUS_DOWN);
        }
    }
}