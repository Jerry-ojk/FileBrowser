package jerry.filebrowser.dialog;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import jerry.filebrowser.file.JerryFile;

public class DataPopupMenu<T extends JerryFile> extends AnchorPopupMenu {

    private int position = -1;
    private T file;


    public DataPopupMenu(@NonNull Context context, @NonNull RecyclerView anchor) {
        super(context, anchor);
    }

    public void show(T file) {
        this.file = file;
        super.show();
    }

    public void show(T file, int position) {
        this.file = file;
        this.position = position;
        super.show();
    }

    public T getFile() {
        return file;
    }

    public int getPosition() {
        return position;
    }

    public void clear() {
        file = null;
        anchorView = null;
    }
}
