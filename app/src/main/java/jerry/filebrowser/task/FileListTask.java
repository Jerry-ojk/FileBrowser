package jerry.filebrowser.task;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.file.JerryFile;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.util.SortHelper;

public class FileListTask extends AsyncTask<String, Object, ArrayList<UnixFile>> {
    private FileListCallback callback;
    private final int type;
    private String path;
    private int dirs = 0;
    private int files = 0;


    public FileListTask(FileListCallback callback, int type) {
        super();
        this.callback = callback;
        this.type = type;
    }

    @Override
    protected ArrayList<UnixFile> doInBackground(String... strings) {
        path = FileSetting.innerPath(strings[0]);
        final UnixFile[] unixFiles = UnixFile.listFiles(path);
        if (unixFiles == null) {
            return null;
        } else if (unixFiles.length <= 1) {
            ArrayList<UnixFile> list = new ArrayList<>(unixFiles.length);
            for (UnixFile file : unixFiles) {
                file.parent = path;
                if (file.isDir()) {
                    dirs++;
                } else {
                    files++;
                }
                list.add(file);
            }
            return list;
        }
        final Comparator<JerryFile> comparator = SortHelper.getComparator(FileSetting.getSortType());
        if (!FileSetting.isMix()) {// 分开排序
            final ArrayList<UnixFile> dirsList = new ArrayList<>(unixFiles.length);
            final ArrayList<UnixFile> filesList = new ArrayList<>(unixFiles.length);
            for (UnixFile file : unixFiles) {
                file.parent = path;
                if (file.isDir()) {
                    dirsList.add(file);
                } else {
                    filesList.add(file);
                }
            }
            dirs = dirsList.size();
            files = filesList.size();
            if (dirs > 1) {
                dirsList.sort(comparator);
            }
            if (files > 1) {
                filesList.sort(comparator);
            }
            if (FileSetting.isReverse()) {
                Collections.reverse(dirsList);
                Collections.reverse(filesList);
            }
            final int size = filesList.size();
            for (int i = 0; i < size; i++) {
                dirsList.add(filesList.get(i));
            }
            return dirsList;
        } else {// 混合排序
            final ArrayList<UnixFile> list = new ArrayList<>(unixFiles.length);
            for (UnixFile file : unixFiles) {
                file.parent = path;
                list.add(file);
                if (file.isDir()) {
                    dirs++;
                } else {
                    files++;
                }
            }
            list.sort(comparator);
            if (FileSetting.isReverse()) {
                Collections.reverse(list);
            }
            return list;
        }
    }


    @Override
    protected void onPostExecute(ArrayList<UnixFile> list) {
        final FileListResult result = new FileListResult(path, list, type);
        result.dirs = dirs;
        result.files = files;
        callback.onListResult(result);
        callback = null;
    }

    @Override
    protected void onCancelled(ArrayList<UnixFile> list) {
        callback = null;
    }
}