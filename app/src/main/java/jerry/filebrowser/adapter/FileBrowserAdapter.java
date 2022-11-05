package jerry.filebrowser.adapter;


import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import jerry.filebrowser.BuildConfig;
import jerry.filebrowser.R;
import jerry.filebrowser.activity.EditActivity;
import jerry.filebrowser.activity.MainActivity;
import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.dialog.DataPopupMenu;
import jerry.filebrowser.dialog.DialogManager;
import jerry.filebrowser.dialog.OpenWayDialog;
import jerry.filebrowser.file.Clipboard;
import jerry.filebrowser.file.FileType;
import jerry.filebrowser.file.Select;
import jerry.filebrowser.file.UnixFile;
import jerry.filebrowser.image.ImageLoadTask;
import jerry.filebrowser.image.ImageManager;
import jerry.filebrowser.provider.JerryFileProvider;
import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.task.FileListCallback;
import jerry.filebrowser.task.FileListResult;
import jerry.filebrowser.task.FileListTask;
import jerry.filebrowser.util.PathUtil;
import jerry.filebrowser.util.TypeUtil;
import jerry.filebrowser.util.Util;
import jerry.filebrowser.view.ItemViewGroup;
import jerry.filebrowser.view.PathNavView;


@SuppressLint("SetTextI18n")
public class FileBrowserAdapter extends RecyclerView.Adapter<FileBrowserAdapter.ViewHolder> implements PathNavAdapter.PathNavInterface, FileListCallback {
    public static final int TYPE_TO_CHILD = 1;
    public static final int TYPE_TO_PARENT = 2;
    public static final int TYPE_JUMP = 3;
    public static final int TYPE_REFRESH = 4;

    public static final int NOTIFY_RENAME = 1;
    public static final int NOTIFY_SELECT_CHANGE = 2;

    public static final int DURING_ANIMATION_FADE = 16 * 5;
    public static final int DURING_ANIMATION_SHOW = 16 * 3;

    private final MainActivity activity;
    private final DialogManager dialogManager;

    //data
    public ArrayList<UnixFile> fileList;
    private final Select select = new Select();

    //view
    private final RecyclerView recyclerView;
    private final LinearLayoutManager layoutManager;
    private final PathNavView pathNavView;
    private ActionMode actionMode;
    private DataPopupMenu<UnixFile> popupMenu;

    private long a;

    //callback
    private ActionMode.Callback callback;

    private boolean isAllow = true;
    private boolean isAnimator = false;
    private boolean isLoading = false;
    private boolean isMultipleSelectMode = false;

    private final TypeUtil typeUtil;
    private final StateListDrawable drawable;

    private FileListResult loadResult;
    private final LruCache<String, Position> positionCache = new LruCache<>(100);


    public FileBrowserAdapter(MainActivity activity, RecyclerView recyclerView) {
        this(activity, FileSetting.USER_ROOT, recyclerView);
    }

    private FileBrowserAdapter(MainActivity mainActivity, String root, RecyclerView recyclerView) {
        this.activity = mainActivity;
        this.dialogManager = activity.getDialogManager();
        this.recyclerView = recyclerView;
        layoutManager = new LinearLayoutManager(mainActivity, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        drawable = new StateListDrawable();
        drawable.addState(new int[]{-android.R.attr.state_selected}, activity.getDrawable(R.drawable.ripple));
        drawable.addState(new int[]{android.R.attr.state_selected}, new ColorDrawable(activity.getColor(R.color.colorSelect)));

        typeUtil = new TypeUtil(activity);

        pathNavView = new PathNavView(activity, this);
        initPopupMenu();
        initActionMode();
        FileSetting.setCurrentPath(root);
        isAllow = false;
        a = System.currentTimeMillis();
        recyclerView.setAlpha(0f);
        new FileListTask(this, FileBrowserAdapter.TYPE_JUMP).execute(root);
    }

    private void initPopupMenu() {
        popupMenu = new DataPopupMenu<>(this.activity, recyclerView);
        Menu menu = popupMenu.getMenu();
        menu.add(Menu.NONE, 1, Menu.NONE, "复制").setIcon(R.drawable.ic_copy);
        menu.add(Menu.NONE, 2, Menu.NONE, "剪切").setIcon(R.drawable.ic_cut);
        menu.add(Menu.NONE, 3, Menu.NONE, "重命名").setIcon(R.drawable.ic_edit);
        menu.add(Menu.NONE, 4, Menu.NONE, "删除").setIcon(R.drawable.ic_delete);
        //menuItem = menu.add(Menu.NONE, 5, Menu.NONE, "收藏");
        //menuItem.setIcon(R.drawable.ic_star_dark);
        //menuItem = menu.add(Menu.NONE, 6, Menu.NONE, "压缩");
        //menuItem.setIcon(R.drawable.ic_type_compress);
        menu.add(Menu.NONE, 7, Menu.NONE, "属性").setIcon(R.drawable.ic_info);
        menu.add(Menu.NONE, 8, Menu.NONE, "分享").setIcon(R.drawable.ic_action_share);
        menu.add(Menu.NONE, 9, Menu.NONE, "打开方式").setIcon(R.drawable.ic_action_open_way);
        menu.add(Menu.NONE, 10, Menu.NONE, "多选").setIcon(R.drawable.ic_action_select);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (isAnimator) return true;
            final int id = item.getItemId();
            if (!isMultipleSelectMode) {
                final UnixFile file = popupMenu.getFile();
                final int position = popupMenu.getPosition();
                switch (id) {// 长按菜单
                    case 1:// 复制
                        Clipboard.copySingle(FileSetting.getCurrentPath(), file);
                        activity.onPerformCopy();
                        break;
                    case 2:// 剪切
                        Clipboard.cutSingle(FileSetting.getCurrentPath(), file);
                        activity.onPerformCopy();
                        break;
                    case 3:// 重命名
                        dialogManager.showRenameDialog(file);
                        break;
                    case 4:// 删除
                        dialogManager.showDeleteDialog(file);
                        break;
                    case 6:
                        break;
                    case 7:// 属性
                        dialogManager.showAttributeDialog(file);
                        break;
                    case 8:// 分享
                        final Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType(TypeUtil.getMimeType(file.name));

                        intent.putExtra(Intent.EXTRA_STREAM, JerryFileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileprovider", file.getAbsPath()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        activity.startActivity(Intent.createChooser(intent, null));
                        break;
                    case 9:// 打开方式
                        new OpenWayDialog(activity).show(file);
                        break;
                    case 10:// 多选
                        intoMultipleSelectMode();
                        select.add(position);
                        notifyItemChanged(position, NOTIFY_SELECT_CHANGE);
                        break;
                }
            } else {
                switch (id) {//长按菜单
                    case 1://复制
                        Clipboard.copy(FileSetting.getCurrentPath(), select.getSelectList());
                        activity.onPerformCopy();
                        break;
                    case 2://剪切
                        Clipboard.cut(FileSetting.getCurrentPath(), select.getSelectList());
                        activity.onPerformCopy();
                        break;
                    case 4://删除
                        ArrayList<UnixFile> list = select.getSelectList();
                        if (list.size() == 1) {
                            dialogManager.showDeleteDialog(list.get(0));
                        } else if (list.size() > 1) {
                            dialogManager.showDeleteDialog(select.getSelectList());
                        }
                        break;
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
                    if (!isMultipleSelectMode) {
                        // 恢复锚点View的背景颜色
                        final View view = popupMenu.getAnchorView();
                        if (view == null) return;
                        view.setSelected(false);
                    }
                    popupMenu.clear();
                });
    }

    private void initActionMode() {
        callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                AppUtil.showIcon(menu);
                menu.add(Menu.NONE, 1, Menu.NONE, "全选").setIcon(R.drawable.ic_select_all).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.add(Menu.NONE, 2, Menu.NONE, "反选").setIcon(R.drawable.ic_action_select).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.add(Menu.NONE, 3, Menu.NONE, "取消").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false; // Return false if nothing is done
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case 1://全选
                        if (select.isSelectedAll()) {
                            select.clearSelect();
                        } else {
                            select.selectAll();
                        }
                        notifyDataSetChanged();
                        actionMode.setSubtitle("选中数：" + select.getSelectCount());
                        break;
                    case 2://反选
                        select.selectReverse();
                        actionMode.setSubtitle("选中数：" + select.getSelectCount());
                        notifyDataSetChanged();
                        break;
                    case 3://反选
                        quitMultipleSelectMode();
                        break;
                }
                return true;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                quitMultipleSelectMode();
                actionMode = null;
            }
        };
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_file, parent, false);
        ItemViewGroup view = new ItemViewGroup(parent.getContext());
        ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnClickListener(this::dispatchOrInterruptItemClick);
        holder.itemView.setOnLongClickListener(this::onItemLongClick);
        holder.itemView.setBackground(drawable.getConstantState().newDrawable());
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position,
                                 @NonNull List<Object> payloads) {
        this.onBindViewHolder(holder, position);
        if (payloads.isEmpty()) {
            this.onBindViewHolder(holder, position);
        } else {
            Object object = payloads.get(0);
            if (object instanceof Integer) {
                switch ((int) object) {
                    case NOTIFY_SELECT_CHANGE:
                        if (isMultipleSelectMode && select.isSelect(position)) {
                            //holder.itemView.setBackground(selectedColor);
                            holder.itemView.setSelected(true);
                        } else {
                            holder.itemView.setSelected(false);
//                            holder.itemView.setBackground(drawable.mutate());
                            //holder.itemView.setBackground(drawable);
                        }
                        break;
                }
            }
        }
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final UnixFile item = fileList.get(position);
        holder.time.setText(Util.time(item.time));
        holder.name.setText(item.name);
        // holder.itemView.setTag(position);

        if (item.type == UnixFile.TYPE_DIR) {
            holder.size.setText("");
//            holder.size.setText(Util.size(item.length));
            holder.icon.setImageDrawable(typeUtil.getFolderDrawable());
        } else if (item.type == UnixFile.TYPE_LINK) {
            holder.size.setText("");
            holder.icon.setImageResource(R.drawable.ic_type_link);
        } else {
            holder.size.setText(Util.size(item.length));
            final int type = typeUtil.fillIcon(holder.icon, item.name);
            if (type == FileType.TYPE_IMAGE) {
                ImageManager.loadThumbnail(item.getAbsPath(), holder.icon);
            }
        }
        if (isMultipleSelectMode && select.isSelect(position)) {
            holder.itemView.setSelected(true);
        } else {
            holder.itemView.setSelected(false);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
//        Log.i("666", "onViewRecycled:" + holder.getAdapterPosition());
        Object object = holder.icon.getTag();
        if (object instanceof ImageLoadTask) {
            ImageLoadTask task = (ImageLoadTask) object;
            task.cancel();
        } else if (object != null) {
            holder.icon.setTag(null);
        }
    }


    private void dispatchOrInterruptItemClick(View view) {
        if (isAnimator) return;
        final int position = recyclerView.getChildAdapterPosition(view);
        if (position == RecyclerView.NO_POSITION) return;
        if (isMultipleSelectMode) {
            if (select.isSelect(position)) {
                select.remove(position);
                notifyItemChanged(position, NOTIFY_SELECT_CHANGE);
                if (select.getSelectCount() == 0) {
                    quitMultipleSelectMode();
                }
            } else {
                select.add(position);
                notifyItemChanged(position, NOTIFY_SELECT_CHANGE);
            }
            // 再次判断是否在多选模式中
            if (isMultipleSelectMode) actionMode.setSubtitle("选中数：" + select.getSelectCount());
        } else {
            final UnixFile file = fileList.get(position);
            if (!file.isExist()) {
                activity.showToast("该文件（夹）已不存在");
                refresh();
                return;
            }
            if (file.isDir()) {
                onDirectoryClick(position, file);
            } else {
                onFileClick(position, file);
            }
        }
    }

    private void onDirectoryClick(int position, UnixFile file) {
        loadDirectory(file.getAbsPath(), TYPE_TO_CHILD);
    }

    private void onFileClick(int position, UnixFile file) {
        final Uri uri = JerryFileProvider.getUriForFile(activity, BuildConfig.APPLICATION_ID + ".fileprovider", file.getAbsPath());

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_TITLE, file.name);

        final String mime = TypeUtil.getMimeType(file.name);
//        activity.showToast(mime);
        intent.setDataAndType(uri, mime);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        if (file.name.endsWith(".json") || mime.startsWith("text/")) {
            intent.setClass(activity, EditActivity.class);
            activity.startActivity(intent);
            return;
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                int l = path.lastIndexOf('.');
//                if (l < (path.length() - 6)) {
//                    intent.setDataAndType(uri, "text/*");
//                }
//                List<ResolveInfo> resInfoList = activity.getPackageManager()
//                        .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
//                for (ResolveInfo resolveInfo : resInfoList) {
//                    activity.grantUriPermission(resolveInfo.activityInfo.packageName, uri,
//                            Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
//                }
//                boolean isFind = false;
        try {
            activity.startActivity(Intent.createChooser(intent, "选择打开该文件的程序"));
            // isFind = true;
        } catch (ActivityNotFoundException exception) {
            activity.showToast("没有找到能够打开的应用");
        }
        // FileManager.showRefresh = isFind;
    }

    private boolean onItemLongClick(View view) {
        if (isAnimator || !isAllow) return false;
        final int pos = recyclerView.getChildAdapterPosition(view);
        if (isMultipleSelectMode) {
            if (!select.isSelect(pos)) return false;
            final Menu menu = popupMenu.getMenu();
            menu.findItem(3).setVisible(false);// 重命名
            menu.findItem(7).setVisible(false);// 属性
            menu.findItem(8).setVisible(false);// 分享
            menu.findItem(9).setVisible(false);// 打开方式
            popupMenu.setAnchorView(view);
            popupMenu.show();
        } else {
            final UnixFile file = fileList.get(pos);
            final Menu menu = popupMenu.getMenu();
            menu.findItem(3).setVisible(true);// 重命名
            menu.findItem(7).setVisible(true);// 属性
            final boolean isFile = file.notDir();
            menu.findItem(8).setVisible(isFile);// 分享
            menu.findItem(9).setVisible(isFile);// 打开方式

            view.setSelected(true);
            popupMenu.setAnchorView(view);
            popupMenu.show(file, pos);
        }
        return true;
    }

    @Override
    public int getItemCount() {
        return fileList == null ? 0 : fileList.size();
    }

    @Override
    public void loadDirectory(String absolutePath, int type) {
        if (!isAllow) return;
        isAllow = false;
        loadResult = null;
        if (isMultipleSelectMode) {
            quitMultipleSelectMode();
        }
        a = System.currentTimeMillis();
        Position position = positionCache.get(FileSetting.getCurrentPath());
        final int viewPosition = layoutManager.findFirstVisibleItemPosition();
        if (viewPosition != RecyclerView.NO_POSITION) {
            if (position == null) {
                position = new Position();
            }
            position.position = viewPosition;
            final View view = layoutManager.findViewByPosition(viewPosition);
            if (view != null) {
                position.offset = view.getTop();
            }
            positionCache.put(FileSetting.getCurrentPath(), position);
        }

        isAnimator = true;
        recyclerView.animate().alpha(0f).setDuration(DURING_ANIMATION_FADE).setInterpolator(new DecelerateInterpolator())
                .withEndAction(() -> {
                    isAnimator = false;
                    if (loadResult != null) {
                        onListResult(loadResult);
                    }
                }).start();
        new FileListTask(this, type).execute(absolutePath);
    }


    @Override
    public void onListResult(FileListResult result) {
        long b = System.currentTimeMillis();
        if (isAnimator) {
//            Log.i("onLoadFinish", "第一次" + (b - a) + "ms");
            loadResult = result;
            return;
        } else {
//            Log.i("onLoadFinish", "第二次" + (b - a) + "ms");
        }
        loadResult = null;
        if (result.list == null) {
            activity.showToast(PathUtil.getPathName(result.absolutePath) + " 打开失败");
            recyclerView.setAlpha(1f);
            isAllow = true;
            return;
        }
        FileSetting.setCurrentPath(result.absolutePath);
        fileList = result.list;
        pathNavView.updatePath(FileSetting.tagPath(result.absolutePath));
        notifyDataSetChanged();
        activity.onFileInfo(result.dirs, result.files);
        if (result.type != TYPE_REFRESH) {
            final Position position = positionCache.get(result.absolutePath);
            if (position != null) {
                layoutManager.scrollToPositionWithOffset(position.position, position.offset);
            } else {
                layoutManager.scrollToPositionWithOffset(0, 0);
            }
        }
        isAnimator = true;
        recyclerView.animate().alpha(1f).setDuration(DURING_ANIMATION_SHOW).setInterpolator(new AccelerateInterpolator(2f))
//                .setUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//            long a = 0;
//            long b = 0;
//
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                b = System.currentTimeMillis();
//                Log.i("666", "动画间隔show：" + (b - a) + "ms，alpha=" + recyclerView.getAlpha());
//                a = b;
//            }
//        })
                .withEndAction(() -> {
                    isAnimator = false;
                    isAllow = true;
                }).start();
    }

    private void toParentDirectory() {
        final String currentPath = FileSetting.getCurrentPath();
        if (FileSetting.USER_ROOT.equals(currentPath)) {
            activity.showToast("已到达根目录");
            return;
        }
        String parent = PathUtil.getPathParent(currentPath);
        loadDirectory(parent, TYPE_TO_PARENT);
    }


    public boolean isMultipleSelectMode() {
        return isMultipleSelectMode;
    }


    public void intoMultipleSelectMode() {
        if (!isMultipleSelectMode) {
            isMultipleSelectMode = true;
            select.onIntoMultipleSelectMode(fileList);
            activity.onIntoMultipleSelectMode();
            actionMode = activity.startSupportActionMode(callback);
            actionMode.setTitle("多选模式");
            actionMode.setSubtitle("选中数：" + select.getSelectCount());
        }
    }


    public void quitMultipleSelectMode() {
        if (isMultipleSelectMode) {
            isMultipleSelectMode = false;
            select.onQuitMultipleSelectMode();
            activity.onQuitMultipleSelectMode();
            if (actionMode != null) actionMode.finish();
            notifyDataSetChanged();
        }
    }


    public void refresh() {
        clear();
        loadDirectory(FileSetting.getCurrentPath(), TYPE_REFRESH);
    }

    public void clear() {
        quitMultipleSelectMode();
    }

    public boolean onBackKey() {
        if (isMultipleSelectMode) {
            quitMultipleSelectMode();
            return true;
        } else if (FileSetting.getCurrentPath().equals(FileSetting.USER_ROOT)) {
            return false;
        } else {
            toParentDirectory();
            return true;
        }
    }

    public void switchRoot(String path) {
        quitMultipleSelectMode();
        FileSetting.setCurrentPath(path);
        FileSetting.USER_ROOT = path;
        loadDirectory(path, TYPE_JUMP);
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

    public void notifyItemDelete(int position) {
        fileList.remove(position);
        notifyItemRemoved(position);
        int dirs = 0;
        int files = 0;
        for (UnixFile file : fileList) {
            if (file.isDir()) {
                dirs++;
            } else {
                files++;
            }
        }
        activity.onFileInfo(dirs, files);
    }

    public void notifyItemDelete(String name) {
        int position = findPosition(name);
        if (position != -1) {
            notifyItemDelete(position);
        } else {
            refresh();
        }
    }

    public void notifyItemRename(String name, String newName, String newPath) {
        int position = findPosition(name);
        if (position != -1) {
            UnixFile file = fileList.get(position);
            file.name = newName;
            file.setAbsPath(newPath);
            notifyItemChanged(position);
        } else {
            refresh();
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView icon;
        public TextView name;
        public TextView size;
        public TextView time;

        ViewHolder(ItemViewGroup group) {
            super(group);
            icon = group.iv_icon;
            size = group.tv_size;
            name = group.tv_name;
            time = group.tv_time;
        }

        ViewHolder(View group) {
            super(group);
            icon = group.findViewById(R.id.iv_image);
            size = group.findViewById(R.id.tv_size);
            name = group.findViewById(R.id.tv_name);
            time = group.findViewById(R.id.tv_time);
        }
    }

    public static class Position {
        public int position;
        public int offset;
    }
}