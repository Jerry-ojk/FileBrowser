package jerry.filebrowser.app;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import jerry.filebrowser.theme.ThemeHelper;

public class AppApplication extends Application {
    private Context context;


    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.setDarkMode(true);
        context = this;
    }

    @Override
    public void onTerminate() {
        context = null;
        super.onTerminate();
    }
}
