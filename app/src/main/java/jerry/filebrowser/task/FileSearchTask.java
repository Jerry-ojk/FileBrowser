package jerry.filebrowser.task;

import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.file.BaseFile;
import jerry.filebrowser.file.UnixFile;

public class FileSearchTask extends AsyncTask<String, Object, ArrayList<BaseFile>> {
    private WeakReference<FileListCallback> reference;
    public String path;
    public String pattern;
    public boolean isCaseSensitive;
    public boolean isSearchSubdir;
    public boolean isSearchHide;
    private int type;
    private int max = 100;

    private final ArrayList<BaseFile> result = new ArrayList<>();

    public FileSearchTask(FileListCallback callback, int type) {
        super();
        this.reference = new WeakReference<>(callback);
        this.type = type;
    }

    @Override
    protected ArrayList<BaseFile> doInBackground(String... strings) {
        path = FileSetting.toRealPath(strings[0]);

        final File root = new File(path);
        if (!root.exists()) {
            return null;
        }
        try {
            path = root.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (!isCaseSensitive) {
            pattern = pattern.toLowerCase(Locale.US);
        }
        scanDir(path);
        return result;
    }

    private void scanDir(String path) {
        final File root = new File(path);
        if (!root.exists()) {
            return;
        }
        final File[] files = root.listFiles();
        if (files == null || files.length == 0) return;
        final int length = files.length;
        for (int i = 0; i < length; i++) {
            final File file = files[i];
            if (file.getName().charAt(0) == '.' && !isSearchHide) {
                continue;
            }
            if (result.size() == max) {
                return;
            }
            final String name = isCaseSensitive ? file.getName() : file.getName().toLowerCase(Locale.US);
            if (name.contains(pattern)) {
                final UnixFile unixFile = new UnixFile(file.getName(), BaseFile.getType(file), -1, -1);
                unixFile.parent = path;
                unixFile.setAbsPath(file.getAbsolutePath());
                result.add(unixFile);
            }
            if (result.size() == max) {
                return;
            }
            if (isSearchSubdir && file.isDirectory()) {
                scanDir(file.getAbsolutePath());
            }
        }
    }


    @Override
    protected void onPostExecute(ArrayList<BaseFile> list) {
        FileListCallback callback = reference.get();
        if (callback != null) {
            callback.onListResult(new FileListResult(path, list, type));
            reference = null;
        }
    }

    @Override
    protected void onCancelled(ArrayList<BaseFile> list) {
        reference = null;
    }
}