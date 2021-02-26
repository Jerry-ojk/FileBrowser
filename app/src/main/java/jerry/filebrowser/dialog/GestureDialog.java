package jerry.filebrowser.dialog;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;

import jerry.filebrowser.view.GestureAction;
import jerry.filebrowser.R;
import jerry.filebrowser.activity.GestureActivity;
import jerry.filebrowser.util.PathUtil;
import jerry.filebrowser.view.GestureView;

public class GestureDialog extends Dialog {
    private GestureActivity activity;
    private GestureView gestureView;
    private TextView tv_message;
    private TextView tv_spinner;
    private PopupMenu popupMenu;

    public GestureDialog(@NonNull Context context) {
        super(context);
        activity = (GestureActivity) context;
        setContentView(R.layout.dialog_gesture);
        gestureView = findViewById(R.id.dialog_gestureView);
        gestureView.setInputMode(true);
        Button bu_positive = findViewById(R.id.bu_sure);
        bu_positive.setOnClickListener(v -> activity.onSure(gestureView.getPath()));

        Button bu_negative = findViewById(R.id.bu_cancel);
        bu_negative.setOnClickListener(v -> {
            dismiss();
            reset();
        });

        tv_message = findViewById(R.id.tv_message);
        tv_message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                activity.showPathDialog();
            }
        });

        tv_spinner = findViewById(R.id.dialog_sp);
        tv_spinner.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                popupMenu.show();
            }
        });
        popupMenu = new PopupMenu(activity, tv_spinner);
        Menu menu = popupMenu.getMenu();
        for (int i = 0; i < GestureAction.ACTIONS.length; i++) {
            menu.add(Menu.NONE, i, Menu.NONE, GestureAction.ACTIONS[i]);
        }
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            tv_spinner.setText(GestureAction.ACTIONS[id]);
            this.activity.onActionSelect(id);
            return true;
        });
        setCanceledOnTouchOutside(false);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //AlertDialog.Builder builder = new AlertDialog.Builder(context).setView().create();
    }

    public char[] getPath() {
        return gestureView.getPath();
    }


    public interface ActionCallback {
        void onSure(char[] path);

        void onActionSelect(int id);
    }

    public void show(String message) {
        tv_message.setText(message);
        show();
    }


    public void show(GestureAction action) {
        gestureView.setPath(action.path);
        tv_spinner.setText(GestureAction.ACTIONS[action.id]);
        tv_message.setText(PathUtil.getPathName(action.target));
        show();
    }


    public void reset() {
        tv_message.setText("");
        gestureView.clear();
        tv_spinner.setText("请选择");
    }
}
