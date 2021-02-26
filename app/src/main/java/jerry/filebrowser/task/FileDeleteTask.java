package jerry.filebrowser.task;

import android.os.AsyncTask;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.adapter.FileBrowserAdapter;
import jerry.filebrowser.dialog.ProgressDialog;
import jerry.filebrowser.file.UnixFile;

public class FileDeleteTask extends AsyncTask<Void, Object, Integer> {
    private ProgressDialog dialog;
    private WeakReference<MainActivity> reference;
    private FileBrowserAdapter adapter;
    private ArrayList<UnixFile> arrayList;
    private int size;
    private int finish;


    public FileDeleteTask(ArrayList<UnixFile> arrayList, MainActivity activity) {
        this.arrayList = arrayList;
        size = arrayList.size();
        reference = new WeakReference<>(activity);
        adapter = activity.getAdapter();
    }

//    public void setActivity(MainActivity activity) {
//        this.activity = activity;
//    }

    @Override
    protected void onPreExecute() {
        MainActivity activity;
        if (reference == null || (activity = reference.get()) == null) return;
        if (dialog == null) {
            dialog = new ProgressDialog(activity);
        }
        dialog.show("删除");
    }

    @Override
    protected Integer doInBackground(Void... voids) {
        int size = arrayList.size();
        publishProgress("", 0, 0);
        int error = 0;
        for (int i = 0; i < size; i++) {
            UnixFile file = arrayList.get(i);
            if (UnixFile.delete(file.getAbsPath()) || !file.isExist()) {
                finish++;
            } else {
                error++;
            }
            publishProgress(file.name, (i + 1) * 100 / size, i + 1);
        }
//
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
        return error;
    }

    @Override
    protected void onPostExecute(Integer integer) {
        //callback.onFinish(integer);
        dialog.dismiss();
        adapter.refresh();
        MainActivity activity;
        if (reference == null || (activity = reference.get()) == null) return;
        if (integer != 0) {
            activity.showToast(+integer + "个文件删除失败");
        } else {
            activity.showToast("删除成功");
        }
        adapter = null;
        reference = null;
        dialog = null;
    }

    @Override
    protected void onProgressUpdate(Object... objects) {
        //callback.onProgressUpdate((String) objects[0], (int) objects[1], (int) objects[2]);
        dialog.setMessage((String) objects[0]);
        dialog.setSuMessage(objects[2] + "/" + size);
        dialog.setProgress((int) objects[1]);
    }
}
