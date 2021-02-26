package jerry.filebrowser.shell;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import jerry.filebrowser.R;
import jerry.filebrowser.adapter.ItemDecoration;
import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.setting.SettingManager;

public class ShellListActivity extends AppCompatActivity {
    private List<ShellConfig> configList;
    private RecyclerView recyclerView;
    private ShellListAdapter adapter;
    private int position = -1;
    private boolean isPause = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ftp_list);
        recyclerView = findViewById(R.id.rec);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        configList = SettingManager.readShellConfig().configList;
        adapter = new ShellListAdapter(this, recyclerView, configList);
        recyclerView.addItemDecoration(new ItemDecoration(this));
        recyclerView.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        AppUtil.showIcon(menu);
        getMenuInflater().inflate(R.menu.toolbar_ftp_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_add:
                position = -1;
                new ShellConfigDialog(this).show();
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    public void onConfigItemEdit(int position, ShellConfig config) {
        this.position = position;
        ShellConfigDialog dialog = new ShellConfigDialog(this);
        dialog.setTarget(config);
        dialog.show();
    }

    public void onConfigItemClick(int position, ShellConfig config) {
        config.time = System.currentTimeMillis();
        Intent intent = new Intent(this, ShellActivity.class);
        intent.putExtra("target", config);
        startActivity(intent);
    }

    public void onConfigItemDelete(int position, ShellConfig config) {
        configList.remove(position);
        adapter.notifyItemRemoved(position);
    }

    public void onConfigSure(ShellConfig config) {
        if (position == -1) {// 新增item
            configList.add(config);
            adapter.notifyItemRangeInserted(configList.size(), 1);
        } else {
            configList.set(position, config);
            adapter.notifyItemRangeChanged(position, 1, null);
        }
        SettingManager.saveShellConfig();
    }


    @Override
    protected void onPause() {
        super.onPause();
        isPause = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isPause && adapter != null) {
            isPause = false;
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        SettingManager.saveShellConfig();
    }
}
