package jerry.filebrowser.dialog;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.file.BaseFile;

public class DialogManager {
    private final AppCompatActivity activity;
    private EditDialog editDialog;
    private FileAttributeDialog attributeDialog;
    private FileClearDialog fileClearDialog;
    // private OpenWayDialog openWayDialog;

    public DialogManager(MainActivity activity) {
        this.activity = activity;
    }

    private void checkEditDialog() {
        if (editDialog == null) {
            editDialog = new EditDialog(activity);
        }
    }

    public void showRenameDialog(BaseFile file) {
        checkEditDialog();
        editDialog.showRenameDialog(file);
    }

    public void showCreateDialog(String currentPath) {
        checkEditDialog();
        editDialog.showCreateDialog(currentPath);
    }

    public void showDeleteDialog(BaseFile file) {
        checkEditDialog();
        editDialog.showDeleteDialog(file);
    }

    public void showDeleteDialog(ArrayList<BaseFile> list) {
        checkEditDialog();
        editDialog.showDeleteListDialog(list);
    }

    public void showAttributeDialog(BaseFile file) {
        if (attributeDialog == null) {
            attributeDialog = new FileAttributeDialog(activity);
        }
        attributeDialog.show(file);
    }


    public void showFileListDialog(String path, ArrayList<BaseFile> list) {
        if (fileClearDialog == null) {
            fileClearDialog = new FileClearDialog(activity);
        }
        fileClearDialog.show(path, list);
    }

//    public void showOpenWayDialog(UnixFile file) {
//        if (openWayDialog == null) {
//            openWayDialog = new OpenWayDialog(activity);
//        }
//        attributeDialog.show(file);
//        new OpenWayDialog(activity, file).show();
//    }

    public void clear() {
        editDialog = null;
        attributeDialog = null;
    }
}
