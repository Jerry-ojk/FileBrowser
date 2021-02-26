package jerry.filebrowser.task;

import android.os.AsyncTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.file.Clipboard;
import jerry.filebrowser.dialog.ProgressDialog;
import jerry.filebrowser.file.FilePair;
import jerry.filebrowser.file.UnixFile;

public class FileCopyTask extends AsyncTask<Void, Object, Integer> {
    private WeakReference<MainActivity> reference;
    private ProgressDialog dialog;
    private ArrayList<UnixFile> arrayList;
    private ArrayList<FilePair> files;
    private String destPath;
    private int makeError = 0;
    private int error = 0;
    private int lastProgress = -1;

    public FileCopyTask(ArrayList<UnixFile> arrayList, String destPath, MainActivity activity) {
        this.arrayList = arrayList;
        this.destPath = destPath;
        this.reference = new WeakReference<>(activity);
        this.files = new ArrayList<>();
        dialog = new ProgressDialog(activity);
        dialog.show("复制");
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        files.clear();
        int size = arrayList.size();
        for (int i = 0; i < size; i++) {
            final UnixFile file = arrayList.get(i);
            File source = new File(file.getAbsPath());
            File dest = new File(destPath, file.name);
            makeList(source, dest);
        }
        size = files.size();
        for (int i = 0; i < size; i++) {
            FilePair pair = files.get(i);
            if (pair.isDirectory) {
                if (!pair.dest.mkdir()) error++;
            } else {
                copy(pair.source, pair.dest, i + 1);
            }
        }
        return error;
    }

    public void makeList(File source, File dest) {
        if (source.isDirectory()) {
            files.add(new FilePair(null, dest));
            final File[] files = source.listFiles();
            if (files == null) {
                makeError++;
                return;
            }
            for (File file : files) {
                makeList(file, new File(dest, file.getName()));
            }
        } else if (source.isFile()) {//排除其他文件
            files.add(new FilePair(source, dest));
        } else {
            makeError++;
        }
    }

    private void copy(File source, File dest, int index) {
        if (source.getAbsoluteFile().equals(dest.getAbsoluteFile())) {
            error++;
            return;
        }
        try (FileInputStream inputStream = new FileInputStream(source);
             FileOutputStream outputStream = new FileOutputStream(dest);
             FileChannel inputChannel = inputStream.getChannel();
             FileChannel outputChannel = outputStream.getChannel()) {
            long totalSize = inputChannel.size();
            long already = 0;
            publishProgress(source.getName(), 0, index + "/" + files.size());
            while (already != totalSize) {
                final int progress = (int) (already * 100 / totalSize);
                if (progress != lastProgress) {
                    publishProgress(source.getName(), progress, index + "/" + files.size());
                    lastProgress = progress;
                }
                already += inputChannel.transferTo(already, 0x200000, outputChannel);
            }
            outputStream.getFD().sync();
            publishProgress(dest.getName(), 100, index + "/" + files.size());
            lastProgress = 0;
        } catch (Exception e) {
            error++;
            e.printStackTrace();
        }
    }

    @Override
    protected void onProgressUpdate(Object... objects) {
        dialog.setMessage((String) objects[0]);
        dialog.setSuMessage((String) objects[2]);
        dialog.setProgress((int) objects[1]);
    }

    @Override
    protected void onPostExecute(Integer integer) {
        dialog.dismiss();
        Clipboard.clear();
        final MainActivity activity = reference.get();
        if (activity == null) {
            return;
        }
        if (error == 0 && makeError == 0) {
            activity.showToast("复制成功");
        } else {
            activity.showToast("复制失败");
        }
        activity.refresh();
    }
}