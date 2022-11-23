package jerry.filebrowser.task;

import android.os.AsyncTask;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jerry.filebrowser.file.JerryFile;
import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.file.BaseFile;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.util.SortHelper;

public class FileListTask extends AsyncTask<String, Object, ArrayList<BaseFile>> {
    private FileListCallback callback;
    private final int type;
    private final int version;
    private String path;
    private int dirs = 0;
    private int files = 0;


    public FileListTask(FileListCallback callback, int type, int version) {
        super();
        this.callback = callback;
        this.type = type;
        this.version = version;
    }

    @Override
    protected ArrayList<BaseFile> doInBackground(String... strings) {
        path = FileSetting.toRealPath(strings[0]);
        ArrayList<BaseFile> list = null;
        if (FileSetting.API_MODE == FileSetting.API_MODE_OS) {
            UnixFile[] unixFiles = UnixFile.listFiles(path);
            if (unixFiles == null) return null;
            list = new ArrayList<>(unixFiles.length);
            for (UnixFile item : unixFiles) {
                list.add(item);
            }
        } else {
            list = JerryFile.listFiles1(path);
        }

        // try {
        //     Thread.sleep(3000);
        // } catch (InterruptedException e) {
        //     e.printStackTrace();
        // }

        if (isCancelled()) {
            return null;
        }

        final Comparator<BaseFile> comparator = SortHelper.getComparator(FileSetting.getSortType());
        if (!FileSetting.isMix()) { // 分开排序
            final ArrayList<BaseFile> dirsList = new ArrayList<>(list.size());
            final ArrayList<BaseFile> filesList = new ArrayList<>(list.size());
            for (BaseFile file : list) {
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
        } else { // 混合排序
            final ArrayList<BaseFile> res = new ArrayList<>(list.size());
            for (BaseFile file : list) {
                file.parent = path;
                res.add(file);
                if (file.isDir()) {
                    dirs++;
                } else {
                    files++;
                }
            }
            res.sort(comparator);
            if (FileSetting.isReverse()) {
                Collections.reverse(res);
            }
            return res;
        }
    }


    @Override
    protected void onPostExecute(ArrayList<BaseFile> list) {
        final FileListResult result = new FileListResult(path, list, type);
        result.dirs = dirs;
        result.files = files;
        result.version = version;
        // 调用传入的回调函数
        callback.onListResult(result);
        callback = null;
    }

    @Override
    protected void onCancelled(ArrayList<BaseFile> list) {
        callback = null;
    }
}