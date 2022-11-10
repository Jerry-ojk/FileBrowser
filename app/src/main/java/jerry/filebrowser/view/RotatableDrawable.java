package jerry.filebrowser.view;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


public class RotatableDrawable extends Drawable {
    private Drawable target;
    private float degrees;

    public RotatableDrawable(Drawable target) {
        this.target = target;
    }

    public void setTarget(Drawable target) {
        this.target = target;
    }

    public void setDegrees(float degrees) {
        this.degrees = degrees;
    }

    @Override
    public boolean setState(@NonNull int[] stateSet) {
        return target.setState(stateSet);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        //Log.i("RotatableDrawable", "draw" + degrees);
        Rect rect = getBounds();
        canvas.translate(rect.width() / 2f, rect.height() / 2f);
        canvas.rotate(degrees);
        canvas.translate(-rect.width() / 2f, -rect.height() / 2f);

        target.draw(canvas);

        canvas.translate(-rect.width() / 2f, -rect.height() / 2f);
        canvas.rotate(-degrees);
        canvas.translate(rect.width() / 2f, rect.height() / 2f);
    }

    @Override
    public void setBounds(int left, int top, int right, int bottom) {
        super.setBounds(left, top, right, bottom);
        target.setBounds(left, top, right, bottom);
    }


    @Override
    public void setAlpha(int alpha) {
        //target.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        //target.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return target.getOpacity();
    }
}
