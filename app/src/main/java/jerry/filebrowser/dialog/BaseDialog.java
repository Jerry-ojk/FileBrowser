package jerry.filebrowser.dialog;

import android.content.Context;
import android.graphics.Point;
import android.view.WindowManager;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;

public abstract class BaseDialog extends AppCompatDialog {

    public BaseDialog(Context context) {
        super(context);
        setContentView(getLayoutId());
        WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
        Point point = new Point();
        ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getSize(point);
        layoutParams.width = (int) (point.x * 0.9f);
    }

    protected abstract @LayoutRes
    int getLayoutId();
}