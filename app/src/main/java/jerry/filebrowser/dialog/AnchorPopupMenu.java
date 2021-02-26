package jerry.filebrowser.dialog;

import android.content.Context;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Field;

import jerry.filebrowser.R;

public class AnchorPopupMenu extends PopupMenu {
    private MenuPopupHelper popupHelper;
    private RecyclerView recyclerView;
    protected View anchorView;
    private int position;

    public AnchorPopupMenu(@NonNull Context context, @NonNull RecyclerView recyclerView) {
        super(context, recyclerView, Gravity.END, 0, R.style.MyPopupMenuStyle);
        this.recyclerView = recyclerView;
    }

    public void setAnchorView(View view) {
        anchorView = view;
        if (popupHelper == null) {
            try {
                Field mPopup = PopupMenu.class.getDeclaredField("mPopup");
                mPopup.setAccessible(true);
                popupHelper = ((MenuPopupHelper) mPopup.get(this));
                popupHelper.setForceShowIcon(true);
                popupHelper.setAnchorView(view);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            popupHelper.setAnchorView(view);
        }
    }

    public View getAnchorView() {
        return anchorView;
    }

    public void show(View view) {
        this.position = recyclerView.getChildAdapterPosition(view);
        super.show();
    }

    public int getPosition() {
        return position;
    }
}
