package jerry.filebrowser.shell;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;

import androidx.appcompat.widget.AppCompatEditText;

public class TerminalView extends AppCompatEditText {
    public TerminalView(Context context) {
        super(context);
    }

    public TerminalView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.i("666", "dispatchKeyEvent" + event.toString());
        return super.dispatchKeyEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i("666", "onKeyDown" + event.toString());
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        Log.i("666", "setText" + text);
        super.setText(text, type);
    }
}
