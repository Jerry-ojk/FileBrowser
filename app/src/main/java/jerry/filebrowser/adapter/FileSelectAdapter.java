package jerry.filebrowser.adapter;


import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import jerry.filebrowser.R;
import jerry.filebrowser.dialog.DataPopupMenu;
import jerry.filebrowser.dialog.DialogManager;
import jerry.filebrowser.dialog.FileSelectAdapterCallback;
import jerry.filebrowser.file.BaseFile;
import jerry.filebrowser.file.FileType;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.image.ImageLoadTask;
import jerry.filebrowser.image.ImageManager;
import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.task.FileListCallback;
import jerry.filebrowser.task.FileListResult;
import jerry.filebrowser.task.FileListTask;
import jerry.filebrowser.util.PathUtil;
import jerry.filebrowser.util.TypeUtil;
import jerry.filebrowser.util.Util;
import jerry.filebrowser.view.ItemViewGroup;


@SuppressLint("SetTextI18n")
public class FileSelectAdapter extends RecyclerView.Adapter<FileSelectAdapter.ViewHolder> implements PathNavAdapter.PathNavInterface, FileListCallback {
    public static final int TYPE_TO_CHILD = 1;
    public static final int TYPE_TO_PARENT = 2;

    public static final int TYPE_JUMP = 3;
    public static final int TYPE_REFRESH = 4;

    private Context context;
    private DialogManager dialogManager;
    //private Select select = new Select();

    private String currentPath;

    private int selectPosition = -1;

    //view
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private TextView tv_count;

    // private View.OnClickListener dirListener;
    private View.OnLongClickListener longClickListener;

    //data
    public ArrayList<BaseFile> fileList;

    //位置
    private View targetView;

    private ActionMode actionMode;

    private boolean isAllow = true;
    private boolean isAnimator = false;
    private boolean isLoading = false;
    private boolean isMultipleSelectMode = false;


    private final TypeUtil typeUtil;
    private Drawable tempBackground;
    private final Drawable selectedColor;
    private final Drawable ripple;

    private DataPopupMenu<UnixFile> popupMenu;

    private ValueAnimator animator_in;
    private ValueAnimator animator_out;
    private FileListResult loadResult;

    private FileSelectAdapterCallback callback;

    private LruCache<String, Position> positionCache = new LruCache<>(100);


    public FileSelectAdapter(Context context, RecyclerView recyclerView) {
        this(context, FileSetting.DEFAULT_USER_ROOT, recyclerView);
    }

    private FileSelectAdapter(Context context, String root, RecyclerView recyclerView) {
        this.context = context;
        this.recyclerView = recyclerView;
        layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        selectedColor = new ColorDrawable(0xFF232323);
        selectedColor.mutate();
        ripple = context.getDrawable(R.drawable.ripple);
        ripple.mutate();

        typeUtil = new TypeUtil(context);


        initPopupMenu();
        currentPath = root;
        isAllow = false;
        new FileListTask(this, FileSelectAdapter.TYPE_JUMP, 0).execute(root);
    }

    public void setBackView(View backView) {
        backView.setOnClickListener(v -> onBackKey());
    }

    public void setCountView(TextView countView) {
        tv_count = countView;
    }


    private void initPopupMenu() {
        popupMenu = new DataPopupMenu<>(this.context, recyclerView);
        Menu menu = popupMenu.getMenu();
        menu.add(Menu.NONE, 7, Menu.NONE, "属性").setIcon(R.drawable.ic_info);
        //menu.add(Menu.NONE, 8, Menu.NONE, "打开方式").setIcon(R.drawable.ic_action_open_way);
        menu.add(Menu.NONE, 9, Menu.NONE, "多选").setIcon(R.drawable.ic_action_select);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (isAnimator) return true;
            final int id = item.getItemId();
            if (!isMultipleSelectMode) {
                final UnixFile file = popupMenu.getFile();
                switch (id) {// 长按菜单
                    case 7:// 属性
                        dialogManager.showAttributeDialog(file);
                        break;
                    case 9:// 多选
                        intoMultipleSelectMode();
                        //select.add(file.position);
                        notifyItemChanged(popupMenu.getPosition());
                        break;
                }
            } else {
                switch (id) {//长按菜单
                    case 7://属性
                        break;
                    case 9://取消
                        break;
                }
                quitMultipleSelectMode();
            }
            return true;
        });
        popupMenu.setOnDismissListener(
                menu1 -> {
                    if (!isMultipleSelectMode && targetView != null) {
                        if (tempBackground != null) {
                            targetView.setBackground(tempBackground);
                        } else {
                            targetView.setBackgroundResource(R.drawable.ripple);
                        }
                    }
                    popupMenu.clear();
                });
    }

    private void onItemClick(View view) {
        if (isAnimator || !isAllow) return;
        final int position = recyclerView.getChildAdapterPosition(view);
        final BaseFile file = fileList.get(position);
        if (file.type == UnixFile.TYPE_DIR) {
            onNavDirectory(file.getAbsPath(), TYPE_TO_CHILD);
        } else {
            if (position == selectPosition) {
                selectFile(-1);
                view.setBackground(ripple);
            } else {
                if (selectPosition != -1) {
                    notifyItemChanged(selectPosition, 1);
                }
                selectPosition = position;
                selectFile(position);
                view.setBackground(selectedColor);
            }
        }
//        if (isMultipleSelectMode) {
//            if (select.isSelect(pos)) {
//                select.remove(pos);
//                view.post(() -> view.setBackground(ripple));
//                if (select.getSelectCount() == 0) {
//                    quitMultipleSelectMode();
//                }
//            } else {
//                select.add(pos);
//                view.setBackground(selectedColor);
//            }
//            if (isMultipleSelectMode)
//                actionMode.setSubtitle("选中数：" + select.getSelectCount());
//            return true;
//        } else {
//            return false;
//        }
    }

//    private boolean onItemLongClick(View view) {
//        if (isAnimator || !isAllow) return false;
//        final int pos = recyclerView.getChildAdapterPosition(view);
//        if (isMultipleSelectMode) {
//            if (!select.isSelect(pos)) return false;
////                final Menu menu = popupMenu.getMenu();
////                menu.findItem(3).setVisible(false);// 重命名
////                menu.findItem(8).setVisible(false);// 打开方式
//            popupMenu.setAnchorView(view);
//            popupMenu.show();
//        } else {
//            targetView = view;
//            tempBackground = view.getBackground();
//            view.setBackground(selectedColor);
//            final UnixFile file = fileList.get(pos);
////                final Menu menu = popupMenu.getMenu();
////                menu.findItem(3).setVisible(true);// 重命名
////                menu.findItem(8).setVisible(file.type != JerryFile.TYPE_DIR);// 打开方式
//            popupMenu.setAnchorView(view);
//            popupMenu.show(new MoreFile(file, pos));
//        }
//        return true;
//
//    }

//    private boolean common(View view) {
//        if (isAnimator) return true;
//        int pos = recyclerView.getChildAdapterPosition(view);
//        if (isMultipleSelectMode) {
//            if (select.isSelect(pos)) {
//                select.remove(pos);
//                view.post(() -> view.setBackground(ripple));
//                if (select.getSelectCount() == 0) {
//                    quitMultipleSelectMode();
//                }
//            } else {
//                select.add(pos);
//                view.setBackground(selectedColor);
//            }
//            if (isMultipleSelectMode) actionMode.setSubtitle("选中数：" + select.getSelectCount());
//            return true;
//        } else {
//            return false;
//        }
//    }

    private void initActionMode() {
//        callback = new ActionMode.Callback() {
//            @Override
//            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
//                AppUtil.showIcon(menu);
//                menu.add(Menu.NONE, 1, Menu.NONE, "全选").setIcon(R.drawable.ic_select_all).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
//                menu.add(Menu.NONE, 2, Menu.NONE, "反选").setIcon(R.drawable.ic_action_select).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
//                menu.add(Menu.NONE, 3, Menu.NONE, "取消").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
//                return true;
//            }
//
//            @Override
//            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//                return false; // Return false if nothing is done
//            }
//
//            @Override
//            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
//                switch (item.getItemId()) {
//                    case 1://全选
//                        if (select.isSelectedAll()) {
//                            select.clearSelect();
//                        } else {
//                            select.selectAll();
//                        }
//                        notifyDataSetChanged();
//                        actionMode.setSubtitle("选中数：" + select.getSelectCount());
//                        break;
//                    case 2://反选
//                        select.selectReverse();
//                        actionMode.setSubtitle("选中数：" + select.getSelectCount());
//                        notifyDataSetChanged();
//                        break;
//                    case 3://反选
//                        quitMultipleSelectMode();
//                        break;
//                }
//                return true;
//            }
//        };
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnLongClickListener(longClickListener);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            this.onBindViewHolder(holder, position);
        } else {
            holder.itemView.setBackground(ripple);
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final BaseFile item = fileList.get(position);
        holder.time.setText(Util.time(item.time));
        holder.name.setText(item.name);
        holder.itemView.setOnClickListener(this::onItemClick);
        // holder.itemView.setTag(position);

        boolean isImgType = false;
        ItemViewGroup itemView = (ItemViewGroup) (holder.itemView);

        if (item.type == UnixFile.TYPE_DIR) {
            holder.length.setText("");
            holder.image.setImageDrawable(typeUtil.getFolderDrawable());
        } else if (item.type == UnixFile.TYPE_LINK) {
            holder.length.setText("");
            holder.image.setImageResource(R.drawable.ic_type_link);
            // holder.itemView.setOnClickListener(fileListener);
        } else {
            holder.length.setText(Util.size(item.length));
            int type = typeUtil.fillIcon(holder.image, item.name);
            if (type == FileType.TYPE_IMAGE || type == FileType.TYPE_VIDEO) {
                ImageManager.loadThumbnail(item.getAbsPath(), type, holder.image);
            }
        }

        if (!isImgType) {
            itemView.setIconPaddingToDefault();
        }

        if (selectPosition == position) {
            holder.itemView.setBackground(selectedColor);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        Object object = holder.image.getTag();
        if (object instanceof ImageLoadTask) {
            ImageLoadTask task = (ImageLoadTask) object;
            task.cancel();
        }
        if (holder.itemView.getBackground() instanceof ColorDrawable) {
            holder.itemView.setBackground(ripple);
        }
    }

    @Override
    public int getItemCount() {
        return fileList == null ? 0 : fileList.size();
    }


    @Override
    public void onNavDirectory(String absolutePath, int type) {
        if (type != TYPE_REFRESH && selectPosition != -1) {
            selectFile(-1);
        }
        Position position = positionCache.get(currentPath);
        int viewPosition = layoutManager.findFirstVisibleItemPosition();
        if (viewPosition != RecyclerView.NO_POSITION) {
            if (position == null) {
                position = new Position();
            }
            position.position = viewPosition;
            View view = layoutManager.findViewByPosition(viewPosition);
            if (view != null) {
                position.offset = view.getTop();
            }
            positionCache.put(currentPath, position);
        }
        isAllow = false;
        loadResult = null;
        new FileListTask(this, type, 0).execute(absolutePath);
        //animator_out.start();
        isAnimator = true;
        recyclerView.animate().alpha(0f).setDuration(150).setInterpolator(new AccelerateInterpolator(1f)).withEndAction(new Runnable() {
            @Override
            public void run() {
                isAnimator = false;
                if (loadResult != null) {
                    onListResult(loadResult);
                }
            }
        }).start();
    }

    @Override
    public void onListResult(FileListResult result) {
        if (isAnimator) {
            loadResult = result;
            return;
        }
        loadResult = null;
        if (result.list == null) {
            callback.onShowToast(result.absolutePath + " 打开失败");
            recyclerView.setAlpha(1f);
            isAllow = true;
            return;
        }
        currentPath = result.absolutePath;
        fileList = result.list;
        tv_count.setText(fileList.size() + "个项目");
        callback.onDirectoryChange(currentPath);
        //pathNavView.updatePath(result.absolutePath);
        notifyDataSetChanged();
        if (result.type != TYPE_REFRESH) {
            Position position = positionCache.get(result.absolutePath);
            if (position != null) {
                layoutManager.scrollToPositionWithOffset(position.position, position.offset);
            }
        }
        recyclerView.animate().alpha(1f).setDuration(50).setInterpolator(new AccelerateInterpolator(1f)).withEndAction(new Runnable() {
            @Override
            public void run() {
                isAllow = true;
            }
        }).start();
    }

    private void toParentDirectory() {
        if (FileSetting.DEFAULT_USER_ROOT.equals(currentPath)) {
            callback.onShowToast("已到达根目录");
            return;
        }
        String parent = PathUtil.getPathParent(currentPath);
        onNavDirectory(parent, TYPE_TO_PARENT);
    }


    public boolean isMultipleSelectMode() {
        return isMultipleSelectMode;
    }


    public void intoMultipleSelectMode() {
        if (!isMultipleSelectMode) {
            isMultipleSelectMode = true;
            callback.OnIntoMultipleSelectMode();
            //callback.OnSelectedCount(select.getSelectCount());
        }
    }


    public void quitMultipleSelectMode() {
        if (isMultipleSelectMode) {
            isMultipleSelectMode = false;
            //select.onQuitMultipleSelectMode();
            callback.OnQuitMultipleSelectMode();
            if (actionMode != null) actionMode.finish();
            notifyDataSetChanged();
        }
    }

    public void refresh() {
        clear();
        onNavDirectory(currentPath, TYPE_REFRESH);
    }

    public void clear() {
        quitMultipleSelectMode();
    }

    public boolean onBackKey() {
        if (isMultipleSelectMode) {
            quitMultipleSelectMode();
            return true;
        } else if (FileSetting.DEFAULT_USER_ROOT.equals(currentPath)) {
            callback.onShowToast("已到达根目录");
            return false;
        } else {
            toParentDirectory();
            return true;
        }
    }

    public int findPosition(String name) {
        if (name == null) {
            return -1;
        }
        final int length = fileList.size();
        for (int i = 0; i < length; i++) {
            if (name.equals(fileList.get(i).name)) {
                return i;
            }
        }
        return -1;
    }

    public void setRootAndDeauftPath(String root, String path) {

    }

    public void setFileSelectCallback(FileSelectAdapterCallback callback) {
        this.callback = callback;
    }

    private void selectFile(int position) {
        this.selectPosition = position;
        if (position == -1) {
            callback.OnFileSelected(null);
        } else {
            callback.OnFileSelected(fileList.get(position).getAbsPath());
        }
    }

    public String getSelectPath() {
        if (selectPosition == -1) return null;
        return fileList.get(selectPosition).getAbsPath();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView image;
        public TextView name;
        public TextView length;
        public TextView time;

        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.iv_image);
            length = itemView.findViewById(R.id.tv_size);
            name = itemView.findViewById(R.id.tv_name);
            time = itemView.findViewById(R.id.tv_time);
        }
    }

    public static class Position {
        public int position;
        public int offset;
    }
}