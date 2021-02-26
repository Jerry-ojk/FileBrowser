package jerry.filebrowser.dialog;

import android.content.Context;
import android.view.View;
import android.widget.PopupMenu;

import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;

public class PopupOption {
    private final Context mContext;
    //private final MenuBuilder mMenu;
    private final View mAnchor;
    //final MenuPopupHelper mPopup;

    PopupMenu.OnMenuItemClickListener mMenuItemClickListener;
    PopupMenu.OnDismissListener mOnDismissListener;

    private View.OnTouchListener mDragListener;


    public PopupOption(@NonNull Context context, @NonNull View anchor, int gravity,
                       @AttrRes int popupStyleAttr, @StyleRes int popupStyleRes) {
        mContext = context;
        mAnchor = anchor;

//        mMenu = new MenuBuilder(context);
//        mMenu.setCallback(new MenuBuilder.Callback() {
//            @Override
//            public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
//                if (mMenuItemClickListener != null) {
//                    return mMenuItemClickListener.onMenuItemClick(item);
//                }
//                return false;
//            }
//
//            @Override
//            public void onMenuModeChange(MenuBuilder menu) {
//            }
//        });
//
//        mPopup = new MenuPopupHelper(context, mMenu, anchor, false, popupStyleAttr, popupStyleRes);
//        mPopup.setGravity(gravity);
//        mPopup.setOnDismissListener(new PopupWindow.OnDismissListener() {
//            @Override
//            public void onDismiss() {
//                if (mOnDismissListener != null) {
//                    mOnDismissListener.onDismiss(this);
//                }
//            }
//        });
    }
}
