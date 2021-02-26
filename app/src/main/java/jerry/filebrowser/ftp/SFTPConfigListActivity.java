package jerry.filebrowser.ftp;

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
import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.ftp.dialog.FTPConfigDialog;
import jerry.filebrowser.setting.SettingManager;

public class SFTPConfigListActivity extends AppCompatActivity {
    private List<SFTPConfig> ftpConfigList;
    private RecyclerView recyclerView;
    private SFTPConfigListAdapter adapter;
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
        ftpConfigList = SettingManager.readFTPConfig().configList;
        adapter = new SFTPConfigListAdapter(this, recyclerView, ftpConfigList);
        //recyclerView.addItemDecoration(new ItemDecoration());
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
                new FTPConfigDialog(this).show();
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    public void onConfigItemEdit(int position, SFTPConfig config) {
        this.position = position;
        FTPConfigDialog dialog = new FTPConfigDialog(this);
        dialog.setTarget(config);
        dialog.show();
    }

    public void onConfigItemClick(int position, SFTPConfig config) {
        config.time = System.currentTimeMillis();
        Intent intent = new Intent(this, SFTPActivity.class);
        intent.putExtra("target", config);
        startActivity(intent);
    }

    public void onConfigItemDelete(int position, SFTPConfig config) {
        ftpConfigList.remove(position);
        adapter.notifyItemRemoved(position);
    }

    public void onConfigSure(SFTPConfig config) {
        if (position == -1) {// 新增item
            ftpConfigList.add(config);
            adapter.notifyItemRangeInserted(ftpConfigList.size(), 1);
        } else {
            ftpConfigList.set(position, config);
            adapter.notifyItemRangeChanged(position, 1, null);
        }
        SettingManager.saveFTPConfig();
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
        SettingManager.saveFTPConfig();
    }
}
