package jerry.filebrowser.dialog;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;

import jerry.filebrowser.R;
import jerry.filebrowser.file.BaseFile;
import jerry.filebrowser.file.FileAttribute;
import jerry.filebrowser.util.NativeUtil;
import jerry.filebrowser.util.Util;
import jerry.filebrowser.util.VideoUtil;
import jerry.filebrowser.video.VideoInfo;

public class VideoAttributeDialog extends BaseDialog {
    private BaseFile file;
    private FileAttribute attribute;
    private VideoInfo videoInfo;

    private final TextView tv_name;
    private final TextView tv_path;
    private final TextView tv_type;
    private final TextView tv_size;
    private final TextView tv_mtime;
    private final TextView tv_resolution;
    private final TextView tv_fps;
    private final TextView tv_rate;
    private final TextView tv_coding;
    private final TextView tv_during;

    public VideoAttributeDialog(@NonNull Context context) {
        super(context);
        tv_name = findViewById(R.id.dialog_name_content);
        tv_path = findViewById(R.id.dialog_path_content);
        tv_type = findViewById(R.id.dialog_type_content);
        tv_size = findViewById(R.id.dialog_size_content);
        tv_mtime = findViewById(R.id.dialog_mtime_content);
        tv_resolution = findViewById(R.id.dialog_resolution_content);
        tv_fps = findViewById(R.id.dialog_fps_content);
        tv_rate = findViewById(R.id.dialog_rate_content);
        tv_coding = findViewById(R.id.dialog_coding_content);
        tv_during = findViewById(R.id.dialog_during_content);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_video_attribute;
    }

    public void show(BaseFile file) {
        this.file = file;
        this.attribute = NativeUtil.GetFileAttribute(file.getAbsPath());
        this.videoInfo = VideoUtil.getVideoInfo(file.getAbsPath());
        if (this.videoInfo == null) {
            this.videoInfo = VideoUtil.getVideoInfoJava(file.getAbsPath());
        }
        Log.d("VideoAttributeDialog", VideoUtil.getVideoInfoJava(file.getAbsPath()).toString());
        setAttribute(this.attribute, this.videoInfo);
        show();
    }

    @SuppressLint("SetTextI18n")
    public void setAttribute(FileAttribute attribute, VideoInfo videoInfo) {
        StringBuilder builder = new StringBuilder();
        if (attribute == null) {
            tv_name.setText("读取文件属性失败");
            tv_path.setText("");
            tv_type.setText("");
            tv_size.setText("");
            tv_mtime.setText("");
        } else {
            this.attribute = attribute;
            tv_name.setText(attribute.name);
            tv_path.setText(attribute.path);
            tv_type.setText(Util.type(attribute.mode));
            tv_size.setText(Util.size(attribute.size));
            tv_mtime.setText(Util.time(attribute.ctime));
        }
        if (videoInfo == null) return;
        builder.setLength(0);
        builder.append(videoInfo.height).append(" X ").append(videoInfo.width);
        tv_resolution.setText(builder);
        builder.setLength(0);
        builder.append(Util.float2(videoInfo.fps)).append("fps");
        tv_fps.setText(builder);
        builder.setLength(0);
        builder.append(Util.size(videoInfo.bitRate / 8)).append("/s");
        tv_rate.setText(builder);
        tv_coding.setText(videoInfo.codec);
        builder.setLength(0);
        builder.append(videoInfo.during);
        tv_during.setText(Util.during((int) videoInfo.during));
    }
}