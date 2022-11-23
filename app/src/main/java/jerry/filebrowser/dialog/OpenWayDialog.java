package jerry.filebrowser.dialog;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.TextView;

import jerry.filebrowser.BuildConfig;
import jerry.filebrowser.R;
import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.file.BaseFile;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.provider.JerryFileProvider;

public class OpenWayDialog extends BaseDialog {
    private MainActivity activity;
    private TextView tv_text;
    private TextView tv_image;
    private TextView tv_audio;
    private TextView tv_video;
    private TextView tv_zip;
    private BaseFile file;

    public OpenWayDialog(Context context) {
        super(context);
        activity = (MainActivity) context;
        @SuppressLint("NonConstantResourceId")
        View.OnClickListener listener = v -> {
            if (file == null) return;
            String mime = null;
            switch (v.getId()) {
                case R.id.tv_txt:
                    mime = "text/*";
                    break;
                case R.id.tv_image:
                    mime = "image/*";
                    break;
                case R.id.tv_mp3:
                    mime = "audio/*";
                    break;
                case R.id.tv_mp4:
                    mime = "video/*";
                    break;
                case R.id.tv_zip:
                    mime = "application/zip";
                    break;
            }
            Uri uri = JerryFileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileprovider", file.getAbsPath());
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, mime);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);

//            List<ResolveInfo> resInfoList = activity.getPackageManager()
//                    .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
//            for (ResolveInfo resolveInfo : resInfoList) {
//                activity.grantUriPermission(resolveInfo.activityInfo.packageName, uri,
//                        Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//            }
            try {
                activity.startActivity(Intent.createChooser(intent, "选择打开该文件的程序"));
            } catch (ActivityNotFoundException exception) {
                activity.showToast("没有找到能够打开的应用");
            }
            dismiss();
        };
        tv_text = findViewById(R.id.tv_txt);
        tv_text.setOnClickListener(listener);
        tv_image = findViewById(R.id.tv_image);
        tv_image.setOnClickListener(listener);
        tv_audio = findViewById(R.id.tv_mp3);
        tv_audio.setOnClickListener(listener);
        tv_video = findViewById(R.id.tv_mp4);
        tv_video.setOnClickListener(listener);
        tv_zip = findViewById(R.id.tv_zip);
        tv_zip.setOnClickListener(listener);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_open_way;
    }

    public void show(BaseFile file) {
        this.file = file;
        show();
    }
}