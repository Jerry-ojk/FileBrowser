package jerry.filebrowser.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import jerry.filebrowser.R;
import jerry.filebrowser.adapter.GestureAdapter;
import jerry.filebrowser.dialog.FileSelectCallback;
import jerry.filebrowser.dialog.FileSelectDialog;
import jerry.filebrowser.dialog.GestureDialog;
import jerry.filebrowser.util.PathUtil;

public class GestureActivity extends AppCompatActivity implements GestureDialog.ActionCallback, FileSelectCallback {
    private GestureDialog gestureDialog;
    private FileSelectDialog fileSelectDialog;
    private String selectPath;
    private Toast toast;
    private GestureAdapter adapter;
    private int position = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gesture);

        Toolbar toolbar = findViewById(R.id.toolbar_gesture);
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = findViewById(R.id.gesture_recycler);

        adapter = new GestureAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        gestureDialog = new GestureDialog(this);
        fileSelectDialog = new FileSelectDialog(this, this);
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add(0, 1, 0, "添加");
        item.setIcon(getDrawable(R.drawable.ic_add));
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item = menu.add("帮助");
        item.setIcon(getDrawable(R.drawable.ic_help));
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            case 1:
                gestureDialog.reset();
                gestureDialog.show();
                return true;
        }
        return true;
    }

    public void showToast(String text) {
        toast.setText(text);
        toast.show();
    }

    @Override
    public void OnFileSelected(String selectPath) {
        this.selectPath = selectPath;
        fileSelectDialog.dismiss();
        gestureDialog.show(PathUtil.getPathName(selectPath));
    }


    @Override
    public void onActionSelect(int id) {
        if (id == 0) {
            gestureDialog.dismiss();
            fileSelectDialog.show();
        }
    }

    public void showPathDialog() {
        fileSelectDialog.show();
    }

    @Override
    public void onSure(char[] path) {
        if (path != null && selectPath != null) {
//            GestureAction action = new GestureAction(1, path, selectPath);
//            Log.i("666", action.toString());
//            Setting.actionList.add(action);
//            gestureDialog.dismiss();
//            gestureDialog.reset();
//            adapter.notifyItemRangeInserted(Setting.actionList.size() - 1, 1);
        }
    }

    public void showEditDialog(int position) {
//        this.position = position;
//        GestureAction action = Setting.actionList.get(position);
//        selectPath = action.target;
//        gestureDialog.show(action);
    }


//    public Drawable getBackground() {
//        TypedArray ta = obtainStyledAttributes(new int[]{
//                R.attr.selectableItemBackground
//        });
//        Drawable drawable = ta.getDrawable(0);
//        ta.recycle();
//        return drawable;
//    }

    public String getSelectPath() {
        return selectPath;
    }
}
