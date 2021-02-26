package jerry.filebrowser.ftp.task;

import android.os.AsyncTask;
import android.util.Log;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.file.SFTPFile;
import jerry.filebrowser.file.JerryFile;
import jerry.filebrowser.ftp.SFTPListResult;
import jerry.filebrowser.ssh.SSHResult;
import jerry.filebrowser.util.SortHelper;
import jerry.filebrowser.ftp.callback.FTPListCallback;
import jerry.filebrowser.setting.SettingManager;

public class SFTPListTask extends AsyncTask<Void, Object, SFTPListResult> {
    private FTPListCallback callback;
    private ChannelSftp sftp;
    private String path;
    private int type;

    public SFTPListTask(FTPListCallback callback, ChannelSftp sftp, String path, int type) {
        this.callback = callback;
        this.sftp = sftp;
        this.path = path;
        this.type = type;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected SFTPListResult doInBackground(Void... voids) {
        SFTPListResult result = new SFTPListResult();
        result.type = type;
        result.code = SSHResult.CODE_SUCCESS;
        result.absolutePath = path;
        ArrayList<SFTPFile> list = null;
        long a = System.currentTimeMillis();
        try {

            list = sftp.ls(path);
            result.list = list;
            long b = System.currentTimeMillis();
            Log.i("666", "FTPListTask耗时:" + (b - a) + "ms,数量：" + list.size());
//            for (int i = 0; i < list.size(); i++) {
//                final FTPFile file = list.get(i);
//                if (file.getAttrs().isLink()) {
//                    String link = sftp.readLink(file.getAbsPath());
//                    String absPath = PathUtil.getAbsPath(path, link);
//                    if (absPath != null)
//                        list.set(i, new FTPLinkFile(file, new FTPFile(absPath, new SFTPAttrs())));
//                }
//            }
        } catch (SftpException e) {
            result.code = e.code;
            result.exception = e;
            e.printStackTrace();
        } catch (Exception e) {
            result.code = SSHResult.CODE_ERROR_UNKNOWN;
            result.exception = e;
            e.printStackTrace();
        }

        if (list == null || list.size() <= 1) return result;
        final int size = list.size();
        final int option = SettingManager.FTP_SETTING_DATA.option;
        final Comparator<JerryFile> comparator = SortHelper.getComparator(option & FileSetting.SORT_MASK);

        if (!FileSetting.isMix(option)) {// 分开排序
            final ArrayList<SFTPFile> dirsList = new ArrayList<>(size);
            final ArrayList<SFTPFile> filesList = new ArrayList<>(size);
            for (SFTPFile file : list) {
                if (file.isDir()) {
                    dirsList.add(file);
                } else {
                    filesList.add(file);
                }
            }
            if (dirsList.size() > 1) {
                Collections.sort(dirsList, comparator);
            }
            if (filesList.size() > 1) {
                Collections.sort(filesList, comparator);
            }
            if (FileSetting.isReverse(option)) {
                Collections.reverse(dirsList);
                Collections.reverse(filesList);
            }
            dirsList.addAll(filesList);
            result.list = dirsList;
        } else {// 混合排序
            Collections.sort(list, comparator);
            if (FileSetting.isReverse(option)) {
                Collections.reverse(list);
            }
            result.list = list;
        }
        Log.i("666", "FTPListTask排序耗时:" + (System.currentTimeMillis() - a) + "ms");
        return result;
    }

    @Override
    protected void onProgressUpdate(Object... objects) {
    }

    @Override
    protected void onPostExecute(SFTPListResult result) {
        callback.onListResult(result);
        callback = null;
    }

    @Override
    protected void onCancelled(SFTPListResult result) {
        callback = null;
    }
}