package jerry.filebrowser.ftp;


import android.annotation.SuppressLint;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.Log;
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

import com.jcraft.jsch.ChannelSftp;

import java.util.ArrayList;

import jerry.filebrowser.R;
import jerry.filebrowser.util.Util;
import jerry.filebrowser.adapter.FileBrowserAdapter;
import jerry.filebrowser.adapter.LruCache;
import jerry.filebrowser.adapter.PathNavAdapter;
import jerry.filebrowser.file.Select;
import jerry.filebrowser.dialog.DataPopupMenu;
import jerry.filebrowser.file.BaseFile;
import jerry.filebrowser.file.SFTPAttrs;
import jerry.filebrowser.file.SFTPFile;
import jerry.filebrowser.ftp.callback.FTPListCallback;
import jerry.filebrowser.ftp.dialog.FTPActionDialog;
import jerry.filebrowser.ftp.dialog.FTPDownloadDialog;
import jerry.filebrowser.ftp.dialog.FTPFileAttributeDialog;
import jerry.filebrowser.ftp.task.SFTPListTask;
import jerry.filebrowser.ssh.SSHConnectManager;
import jerry.filebrowser.util.PathUtil;
import jerry.filebrowser.util.TypeUtil;
import jerry.filebrowser.view.PathNavView;

import static jerry.filebrowser.adapter.FileBrowserAdapter.TYPE_JUMP;
import static jerry.filebrowser.adapter.FileBrowserAdapter.TYPE_REFRESH;


@SuppressLint("SetTextI18n")
public class SFTPFileBrowserAdapter extends RecyclerView.Adapter<SFTPFileBrowserAdapter.ViewHolder> implements PathNavAdapter.PathNavInterface, FTPListCallback {
    private static final int TYPE_FIRST = 0;
    private static final int TYPE_TO_CHILD = 1;
    private static final int TYPE_TO_PARENT = 2;
    private static final int TYPE_RELOAD = 3;
    private SFTPActivity activity;
    // private DialogManager dialogManager;
    private Select select;

    //view
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;

    private PathNavView pathNavView;
    private TextView countView;
    private TextView tv_selectCount;
    private long a;

    //callback
//    private View.OnClickListener fileListener;
//    private View.OnClickListener dirListener;
    private ActionMode.Callback callback;

    //data
    public ArrayList<SFTPFile> fileList;


    //位置
    private ActionMode actionMode;

    private boolean isAllow = true;
    private boolean isAnimator = false;
    private boolean isLoading = false;
    private boolean isMultipleSelectMode = false;

    private TypeUtil typeUtil;
    private final StateListDrawable drawable;

    private DataPopupMenu<SFTPFile> popupMenu;

    private SFTPListResult listResult;

    private LruCache<String, FileBrowserAdapter.Position> positionCache = new LruCache<>(50);

    public SFTPFileBrowserAdapter(SFTPActivity activity, String root, RecyclerView recyclerView) {
        this.activity = activity;
        this.recyclerView = recyclerView;
        select = new Select();
        layoutManager = new LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(this);
        drawable = new StateListDrawable();
        drawable.addState(new int[]{-android.R.attr.state_selected}, activity.getDrawable(R.drawable.ripple));
        drawable.addState(new int[]{android.R.attr.state_selected}, new ColorDrawable(activity.getColor(R.color.colorSelect)));

        View backView = activity.findViewById(R.id.dir_back);
        backView.setOnClickListener(v -> {
            if (isMultipleSelectMode) {
//                quitMultipleSelectMode();
            } else {
                toParentDirectory();
            }
        });
        countView = backView.findViewById(R.id.tv_file_count);
        tv_selectCount = backView.findViewById(R.id.tv_select_count);

        pathNavView = new PathNavView(activity);
        pathNavView.setPathNavInterface(this);

        typeUtil = new TypeUtil(activity);

        initPopupMenu();
        initActionMode();
//        initAnimators();

//        rootNode = new PathNode(root);
//        currentNode = rootNode;

        a = System.currentTimeMillis();
//        new DirectoryLoaderTask(this, 1).execute(root);
    }

    private void initPopupMenu() {
        popupMenu = new DataPopupMenu<>(this.activity, recyclerView);
        Menu menu = popupMenu.getMenu();
        menu.add(Menu.NONE, 1, Menu.NONE, "下载").setIcon(R.drawable.ic_cloud_download);
        //menu.add(Menu.NONE, 8, Menu.NONE, "复制").setIcon(R.drawable.ic_copy);
        //menu.add(Menu.NONE, 2, Menu.NONE, "剪切").setIcon(R.drawable.ic_cut);
        menu.add(Menu.NONE, 3, Menu.NONE, "重命名").setIcon(R.drawable.ic_edit);
        menu.add(Menu.NONE, 4, Menu.NONE, "删除").setIcon(R.drawable.ic_delete);
        menu.add(Menu.NONE, 7, Menu.NONE, "属性").setIcon(R.drawable.ic_info);
        //menu.add(Menu.NONE, 9, Menu.NONE, "多选").setIcon(R.drawable.ic_action_select);
        popupMenu.setOnMenuItemClickListener(item -> {
            if (!isAllow) return true;
            SFTPFile file = popupMenu.getFile();
            switch (item.getItemId()) {//长按菜单
                case 1://下载
                    if (file.type != BaseFile.TYPE_DIR) {
                        new FTPDownloadDialog(activity, file.getAbsPath()).show();
                    }
                    break;
                case 8://复制
                    break;
                case 2://剪切
                    // Clipboard.cutSingle(FTPActivity.CURRENT_PATH, file);
                    break;
                case 3://重命名
                    // dialogManager.showRenameDialog(file);
                    new FTPActionDialog(activity).showRenameDialog(file);
                    break;
                case 4://删除
                    // dialogManager.showDeleteDialog(file);
                    new FTPActionDialog(activity).showDeleteDialog(file);
                    break;
                case 7://属性
                    // dialogManager.showAttributeDialog(file);
                    new FTPFileAttributeDialog(activity).show(file);
                    break;
//                case 9:
//                    if (!isMultipleSelectMode) {
//                        select.clear();
//                        select.add(file.position);
//                        intoMultipleSelectMode();
//                    }
//                    break;
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
                });
    }

    private void initActionMode() {
    }


    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        ViewHolder holder = new ViewHolder(view);
        holder.itemView.setOnClickListener(this::dispatchOrInterruptItemClick);
        holder.itemView.setOnLongClickListener(this::onItemLongClick);
        holder.itemView.setBackground(drawable.getConstantState().newDrawable());
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final SFTPFile item = fileList.get(position);
        holder.itemView.setTag(position);
        if (item.type == BaseFile.TYPE_LINK) {
            if (item instanceof SFTPLinkFile) {
                final SFTPFile link = ((SFTPLinkFile) item).getLink();
                holder.name.setText(item.name + " -> " + link.getAbsPath());
                holder.time.setText(Util.time(link.time));
                if (link.type == BaseFile.TYPE_DIR) {
                    holder.image.setImageDrawable(typeUtil.getFolderDrawable());
                } else {
                    holder.image.setImageResource(R.drawable.ic_type_link);
                    holder.size.setText(Util.size(link.getAttrs().getSize()));
                }
            } else {
                holder.image.setImageResource(R.drawable.ic_type_link);
                holder.name.setText(item.name + " -> ");
                holder.time.setText(Util.time(item.time));
                holder.size.setText(Util.size(item.getAttrs().getSize()));
            }
            return;
        }

        holder.name.setText(item.name);
        holder.time.setText(Util.time(item.time));

        if (item.type == BaseFile.TYPE_DIR) {
            holder.size.setText("");
//            holder.size.setText(Util.size(item.getAttrs().getSize()));
            holder.image.setImageDrawable(typeUtil.getFolderDrawable());
        } else {
            final SFTPAttrs attrs = item.getAttrs();
            holder.size.setText(Util.size(attrs.getSize()));
            int type = typeUtil.fillIcon(holder.image, item.name);
//            if (type == FileType.TYPE_IMAGE) {
//                ImageManager.loadThumbnail(item.getAbsPath(), holder.image);
//            }
        }

//        if (isMultipleSelectMode && select.isSelect(position)) {
//            holder.itemView.setBackground(selectedColor);
//        }

//        else if (holder.itemView.getBackground() == selectedColor) {
//            holder.itemView.setBackground(activity.getBackground());
//        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
//        holder.itemView.setOnClickListener(null);
//        Object object = holder.image.getTag();
//        if (object instanceof ImageLoadTask) {
//            ImageLoadTask task = (ImageLoadTask) object;
//            task.cancel();
//        }
    }


    @Override
    public void onNavDirectory(String absolutePath, int type) {
        if (!isAllow) return;
        a = System.currentTimeMillis();
        final ChannelSftp sftp = SSHConnectManager.getChannelSftp();
        if (sftp == null) {
            activity.showToast("SFTP连接已断开");
            return;
        }

        isAllow = false;
        listResult = null;

        FileBrowserAdapter.Position position = positionCache.get(SFTPActivity.CURRENT_PATH);
        int viewPosition = layoutManager.findFirstVisibleItemPosition();
        if (viewPosition != RecyclerView.NO_POSITION) {
            if (position == null) {
                position = new FileBrowserAdapter.Position();
            }
            position.position = viewPosition;
            View view = layoutManager.findViewByPosition(viewPosition);
            if (view != null) {
                position.offset = view.getTop();
            }
            positionCache.put(SFTPActivity.CURRENT_PATH, position);
        }

        new SFTPListTask(this, sftp, absolutePath, type).execute();
        //animator_out.start();
        isAnimator = true;
        recyclerView.animate().alpha(0f).setDuration(150).setInterpolator(new AccelerateInterpolator(1f)).withEndAction(new Runnable() {
            @Override
            public void run() {
                isAnimator = false;
                if (listResult != null) {
                    onListResult(listResult);
                }
            }
        }).start();
    }

    @Override
    public void onListResult(SFTPListResult result) {
        long b = System.currentTimeMillis();
        Log.i("onLoadFinish", (b - a) + "ms");
        if (isAnimator) {
            listResult = result;
            return;
        }
        listResult = null;
        if (result.list == null) {
            activity.showToast(result.absolutePath + " 打开失败");
            recyclerView.setAlpha(1f);
            isAllow = true;
            return;
        }
        SFTPActivity.CURRENT_PATH = result.absolutePath;
        fileList = result.list;

        //select.onLoadFinish(result.list);

        pathNavView.updatePath(result.absolutePath);
        countView.setText("文件数量：" + fileList.size());
        notifyDataSetChanged();
        if (result.type != TYPE_REFRESH) {
            FileBrowserAdapter.Position position = positionCache.get(result.absolutePath);
            if (position != null) {
                layoutManager.scrollToPositionWithOffset(position.position, position.offset);
            } else {
                layoutManager.scrollToPositionWithOffset(0, 0);
            }
        }
        isAnimator = true;
        recyclerView.animate().alpha(1f).setDuration(50).setInterpolator(new AccelerateInterpolator(1f)).withEndAction(new Runnable() {
            @Override
            public void run() {
                isAnimator = false;
                isAllow = true;
            }
        }).start();
    }


    @Override
    public int getItemCount() {
        return fileList == null ? 0 : fileList.size();
    }


    private void dispatchOrInterruptItemClick(View view) {
        if (!isAllow) return;
        final int pos = recyclerView.getChildAdapterPosition(view);
        final SFTPFile file = fileList.get(pos);
        if (file.type == BaseFile.TYPE_DIR) {
            onDirectoryClick(pos, file);
        } else if (file instanceof SFTPLinkFile) {
            SFTPFile link = ((SFTPLinkFile) file).getLink();
            if (link.isDir()) {
                onNavDirectory(link.getAbsPath(), TYPE_JUMP);
            } else {
                onFileClick(pos, file);
            }
        } else {
            onFileClick(pos, file);
        }
    }

    private void onDirectoryClick(int position, SFTPFile file) {
        onNavDirectory(file.getAbsPath(), TYPE_TO_CHILD);
    }


    private void onFileClick(int position, SFTPFile file) {

    }

    private boolean onItemLongClick(View view) {
        if (!isAllow || isMultipleSelectMode) return false;
        view.setSelected(true);
        final int pos = recyclerView.getChildAdapterPosition(view);
        popupMenu.setAnchorView(view);
        popupMenu.getMenu().getItem(0).setVisible(fileList.get(pos).type != BaseFile.TYPE_DIR);
        popupMenu.show(fileList.get(pos));
        return true;
    }

    public void onFileDelete() {
        select.clear();
    }


    private void toParentDirectory() {
        if (SFTPActivity.USER_ROOT.equals(SFTPActivity.CURRENT_PATH)) {
            activity.showToast("已到达根目录");
            return;
        }
        onNavDirectory(PathUtil.getPathParent(SFTPActivity.CURRENT_PATH), TYPE_TO_PARENT);
    }

    public boolean isMultipleSelectMode() {
        return isMultipleSelectMode;
    }

//    public void intoMultipleSelectMode() {
//        if (!isMultipleSelectMode) {
//            isMultipleSelectMode = true;
////            if (!selectList.isEmpty()) {
////                selectList.clear();
////                notifyDataSetChanged();
////            }
//            activity.onIntoMultipleSelectMode();
//            actionMode = activity.startSupportActionMode(callback);
//            actionMode.setTitle("多选");
//            tv_selectCount.setText("选中数：" + select.getSelectCount());
//            tv_selectCount.setVisibility(View.VISIBLE);
//        }
//    }
//
//    public void clearAndIntoMultipleSelectMode() {
//        select.clear();
//        if (!isMultipleSelectMode) {
//            isMultipleSelectMode = true;
////            if (!selectList.isEmpty()) {
////                selectList.clear();
////                notifyDataSetChanged();
////            }
//            activity.onIntoMultipleSelectMode();
//            actionMode = activity.startSupportActionMode(callback);
//            actionMode.setTitle("多选");
//            tv_selectCount.setText("选中数：" + select.getSelectCount());
//            tv_selectCount.setVisibility(View.VISIBLE);
//        }
//    }

//    public void quitMultipleSelectMode() {
//        if (isMultipleSelectMode) {
//            isMultipleSelectMode = false;
//            activity.onQuitMultipleSelectMode();
//            if (actionMode != null) actionMode.finish();
//            if (select.getSelectCount() > 0) {
//                select.clear();
//                notifyDataSetChanged();
//            }
//            tv_selectCount.setVisibility(View.INVISIBLE);
//        }
//    }

//    public void notifyItemDelete(int position) {
//        fileList = UnixFile.listFiles(ActionController.getCurrentPath());
//        countView.setText("文件数量：" + fileList.size());
//        notifyItemRemoved(position);
//        if (fileList.size() > position) {
//            notifyItemRangeChanged(position, fileList.size() - position);
//        }
//    }

    public void refresh() {
        clear();
        onNavDirectory(SFTPActivity.CURRENT_PATH, TYPE_REFRESH);
    }

    public boolean isAllow() {
        return isAllow;
    }

    public void clear() {
//        quitMultipleSelectMode();
    }

    public boolean onBackKey() {
        if (SFTPActivity.CURRENT_PATH.equals(SFTPActivity.USER_ROOT)) {
            return false;
        } else {
            toParentDirectory();
            return true;
        }
    }


    public void switchRoot(String path) {
//        quitMultipleSelectMode();
        SFTPActivity.USER_ROOT = path;
        onNavDirectory(path, FileBrowserAdapter.TYPE_JUMP);

//        fileArray = UnixFile.listFiles(path);
//        if (fileArray == null) {
//            activity.showToast(path);
//            return;
//        }
//        countView.setText("文件数量：" + fileArray.length);
//        pathNavView.updatePath(path);
//        notifyDataSetChanged();

//        activity.iv_pre.setEnabled(false);
//        activity.iv_next.setEnabled(false);

//        final int disableColor = activity.getResources().getColor(R.color.disable, null);
//        activity.iv_pre.getDrawable().setTint(disableColor);
//        activity.iv_next.getDrawable().setTint(disableColor);

//        rootNode.path = path;
//        rootNode.next = null;
//        currentNode = rootNode;
//        layoutManager.scrollToPositionWithOffset(0, 0);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView image;
        private TextView name;
        private TextView size;
        private TextView time;

        ViewHolder(View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.iv_image);
            size = itemView.findViewById(R.id.tv_size);
            name = itemView.findViewById(R.id.tv_name);
            time = itemView.findViewById(R.id.tv_time);
        }
    }
}
