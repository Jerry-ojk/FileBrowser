package jerry.filebrowser.theme;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    public static boolean isDarkMode = false;

    public static void setDarkMode(boolean darkMode) {
        if (darkMode == isDarkMode) return;
        isDarkMode = darkMode;
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}
