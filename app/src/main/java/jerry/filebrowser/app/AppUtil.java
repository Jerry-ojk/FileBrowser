package jerry.filebrowser.app;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.view.Menu;
import android.widget.EditText;

import androidx.appcompat.view.menu.MenuBuilder;

import java.lang.reflect.Field;

public class AppUtil {

    @SuppressLint("RestrictedApi")
    public static void showIcon(Menu menu) {
        if (!(menu instanceof MenuBuilder)) return;
        try {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        } catch (Exception ignored) {
            try {
                final Field field = MenuBuilder.class.getDeclaredField("mOptionalIconsVisible");
                field.setAccessible(true);
                field.set(menu, true);
            } catch (Exception ignored1) {

            }
        }
    }

    public static String getString(EditText editText) {
        Editable editable = editText.getText();
        if (editable != null) {
            return editable.toString();
        } else {
            return null;
        }
    }

    public static boolean isEmpty(String text) {
        return (text == null || text.isEmpty());
    }

    public static boolean notEmpty(String text) {
        return (text != null && !text.isEmpty());
    }
}
