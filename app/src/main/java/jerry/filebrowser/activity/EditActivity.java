package jerry.filebrowser.activity;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import jerry.filebrowser.BuildConfig;
import jerry.filebrowser.R;
import jerry.filebrowser.util.Util;
import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.provider.JerryFileProvider;
import jerry.filebrowser.util.PathUtil;

public class EditActivity extends AppCompatActivity {
    private Toolbar toolbar;
    private EditText editText;
    private String title;
    private String absPath;
    private String errorMessage;
    private long length = -1;
    private StringBuilder builder;
    private boolean isSuccess = true;
    private int errorCode = 1;
    private Uri uri;
    private boolean isSave = true;

    private static final String[] COLUMNS = {
            MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.DATA};
    private MenuItem menuSave;
    private ContentResolver contentResolver;
    private boolean isChange = false;

    private final int maxLen = 1 * 1024 * 1024;//2MB

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        // StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        toolbar = findViewById(R.id.toolbar_edit);
        toolbar.setNavigationOnClickListener(v -> finish());
        editText = findViewById(R.id.ed_edit);

        initMenu();

        final Intent intent = getIntent();
        uri = intent.getData();

        if (uri == null) {
            errorMessage = "没有文件打开";
            showError();
            return;
        }
        contentResolver = getContentResolver();
        if (BuildConfig.DEBUG) {
            Log.i("666", uri.toString());
        }

        getInfoFromUri(uri);
        final String text = readTextFromUri(uri);
        if (length > maxLen) {
            Toast.makeText(this, "文件太大,只加载前1MB", Toast.LENGTH_SHORT).show();
        }
        if (text == null) {
            toolbar.setSubtitle("打开出错");
            showError();
        } else {
            if (title != null) toolbar.setTitle(title);
            editText.setText(text);
            editText.post(() -> editText.setSelection(0));
            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (isSave) {
                        isSave = false;
                        menuSave.setEnabled(true);
                    }
                }
            });
            String message = "UTF-8";
            if (length != -1) message = message + "  " + Util.size(length);
            toolbar.setSubtitle(message);
        }
    }

    private void getInfoFromUri(Uri uri) {
        final String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Cursor cursor = contentResolver.query(uri, COLUMNS, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                for (int i = 0; i < cursor.getColumnCount(); i++) {
                    final String key = cursor.getColumnName(i);
                    if (JerryFileProvider.COLUMNS_DISPLAY_NAME.equals(key)) {
                        title = cursor.getString(i);
                    } else if (JerryFileProvider.COLUMNS_SIZE.equals(key)) {
                        length = cursor.getLong(i);
                    } else if (JerryFileProvider.COLUMNS_DATA.equals(key)) {
                        absPath = cursor.getString(i);
                    }
                }
                cursor.close();
            }
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            try {
                final File file = new File(uri.getPath());
                if (!file.exists()) return;
                absPath = file.getAbsolutePath();
                title = file.getName();
                length = file.length();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String readTextFromUri(Uri uri) {
        if (length == 0) return "";
        int size = 1024;
        if (length != -1) {
            if (length <= maxLen) {
                size = (int) length;
            } else {
                size = maxLen;
            }
        }
        String result;
        try (InputStream inputStream = contentResolver.openInputStream(uri);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream(size)) {
            if (inputStream == null) {
                outputStream.close();
                errorMessage = "openInputStream()=null";
                return null;
            }
            byte[] buffer = new byte[1024];
            int length;
            long totalLen = 0;
            while ((length = inputStream.read(buffer)) != -1 && totalLen < maxLen) {
                totalLen += length;
                outputStream.write(buffer, 0, length);
            }
            result = outputStream.toString("UTF-8");
        } catch (FileNotFoundException e) {
            isSuccess = false;
            errorCode = 1;
            errorMessage = Util.errorToString(e);
            return null;
        } catch (Exception e) {
            isSuccess = false;
            errorCode = 2;
            errorMessage = Util.errorToString(e);
            return null;
        }
        return result;
    }

    private boolean writeTextFromUri(String text, Uri uri) {
        isChange = true;
        try (OutputStream outputStream = contentResolver.openOutputStream(uri);
             OutputStreamWriter writer = new OutputStreamWriter((outputStream))) {
            writer.write(text);
        } catch (FileNotFoundException e) {
            isSuccess = false;
            errorCode = 1;
            errorMessage = Util.errorToString(e);
            return false;
        } catch (Exception e) {
            isSuccess = false;
            errorCode = 2;
            errorMessage = Util.errorToString(e);
            return false;
        }
        return true;
    }

    private ColorStateList createColorStateList() {
        int[] colors = new int[]{getColor(R.color.action), getColor(R.color.iconDisable)};
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_enabled},
                new int[]{}
        };
        return new ColorStateList(states, colors);
    }

    private void initMenu() {
        Menu menu = toolbar.getMenu();
        AppUtil.showIcon(menu);
        toolbar.inflateMenu(R.menu.toolbar_edit);
        menuSave = toolbar.getMenu().findItem(R.id.menu_save);
        MenuItem menuRefresh = toolbar.getMenu().findItem(R.id.menu_refresh);
        ColorStateList colorStateList = createColorStateList();
        menuSave.getIcon().setTintList(colorStateList);
        menuSave.setEnabled(false);
        menuSave.setOnMenuItemClickListener(item -> {
            boolean success = writeTextFromUri(editText.getText().toString(), uri);
            if (absPath == null) {
                getInfoFromUri(uri);
            } else {
                length = new File(absPath).length();
            }
            String message = "UTF-8";
            if (length != -1) message = message + "  " + Util.size(length);
            toolbar.setSubtitle(message);
            if (!success) {
                showError();
            }
            menuSave.setEnabled(false);
            isSave = true;
            return true;
        });

        menuRefresh.setOnMenuItemClickListener(item -> {
            String text = readTextFromUri(uri);
            if (text == null) {
                toolbar.setSubtitle("打开出错");
                showError();
                menuSave.setEnabled(false);
                isSave = true;
            } else {
                editText.setTextColor(getColor(R.color.text));
                editText.setText(text);
                editText.post(() -> editText.setSelection(0));
            }
            return true;
        });
    }

    private void showError() {
        editText.setEnabled(false);
        editText.setText(errorMessage);
        editText.setTextColor(getColor(R.color.error));
    }

    private void onBack() {

    }

    @Override
    protected void onDestroy() {
//        final ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
//        final List<ActivityManager.AppTask> taskList = activityManager.getAppTasks();
//        boolean parentIsMain = false;
//        for (ActivityManager.AppTask task : taskList) {
//            ActivityManager.RecentTaskInfo taskInfo = task.getTaskInfo();
//            taskInfo.
//            if ()
//        }
        if (isChange) {
            MainActivity.sendRefresh(this, absPath == null ? null : PathUtil.getPathParent(absPath));
        }
        super.onDestroy();
    }


    //    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add(Menu.NONE, 1, Menu.NONE, "搜索").setIcon(R.drawable.ic_action_search).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
//        menu.add(Menu.NONE, 2, Menu.NONE, "保存").setIcon(R.drawable.ic_action_save).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
//        menu.add(Menu.NONE, 3, Menu.NONE, "重新加载").setIcon(R.drawable.ic_action_refresh).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
//        menu.add(Menu.NONE, 1, Menu.NONE, "保存").setIcon(R.drawable.ic_rotate).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
//        return true;
//    }
}
