package jerry.filebrowser.dialog;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import jerry.filebrowser.R;
import jerry.filebrowser.view.LineProcess1;

public class ProgressDialog extends BaseDialog {
    private final TextView tv_title;
    private final TextView tv_message;
    private final TextView tv_sub;
    private final LineProcess1 lineProcess;

    public ProgressDialog(@NonNull Context context) {
        super(context);
        setCancelable(false);
        tv_title = findViewById(R.id.dialog_title);
        tv_message = findViewById(R.id.tv_message);
        tv_sub = findViewById(R.id.dialog_sub_message);
        lineProcess = findViewById(R.id.dialog_lp);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_progerss;
    }

    public void setTitle(String title) {
        tv_title.setText(title);
    }

    public void setMessage(String message) {
        tv_message.setText(message);
    }

    public void setSuMessage(String subMessage) {
        tv_sub.setText(subMessage);
    }

    public void setProgress(int progress) {
        lineProcess.setProcess(progress);
    }

    public void show(String title) {
        tv_title.setText(title);
        show();
    }
}
