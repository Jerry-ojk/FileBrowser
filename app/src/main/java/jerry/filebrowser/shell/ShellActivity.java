package jerry.filebrowser.shell;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.Toolbar;

import jerry.filebrowser.R;
import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.view.BottomScrollView;

public class ShellActivity extends AppCompatActivity {
    public static final int MSG_CONNECTED = 1;
    public static final int MSG_DISCONNECTED = 2;
    public static final int MSG_RECEIVE_TEXT = 3;

    private ShellConfig config;

    private Toolbar toolbar;
    private BottomScrollView scrollView;
    private AppCompatTextView textView;
    private AppCompatEditText editText;
    private Button bu_send;

    private ReceiveThread receiveThread;
    private Handler handler;
    private boolean isConnected = false;
    private boolean autoEnter = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shell);

        toolbar = findViewById(R.id.toolbar_shell);
        toolbar.setNavigationOnClickListener(v -> finish());
        scrollView = findViewById(R.id.scrollView);
        textView = findViewById(R.id.textView);
        editText = findViewById(R.id.editText);
//        editText.setOnKeyListener(new View.OnKeyListener() {
//            @Override
//            public boolean onKey(View v, int keyCode, KeyEvent event) {
//                Log.i("666", event.toString());
//                if (keyCode == KeyEvent.KEYCODE_ENTER) {
//                    if (!isConnected || receiveThread == null) return false;
//                    String text = editText.getText().toString();
//                    receiveThread.sendCommand(text);
//                    editText.setText("");
//                    return true;
//                }
//                // Log.i("666", event.toString());
//                return false;
//            }
//        });
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        bu_send = findViewById(R.id.bu_send);
        findViewById(R.id.bu_ctrl_c).setOnClickListener(v -> {
            if (!isConnected || receiveThread == null) return;
            // ctrl+c=3
            receiveThread.sendCommand("\03", false);
        });
        findViewById(R.id.bu_tab).setOnClickListener(v -> {
            if (!isConnected || receiveThread == null) return;
            // ctrl+c=3
            receiveThread.sendCommand("\011", false);
        });
//        findViewById(R.id.bu_up).setOnClickListener(v -> {
//            if (!isConnected || receiveThread == null) return;
//            receiveThread.sendCommand("\u0026", false);
//        });
//        findViewById(R.id.bu_down).setOnClickListener(v -> {
//            if (!isConnected || receiveThread == null) return;
//            receiveThread.sendCommand("\u0028", false);
//        });
        CheckBox cb_enter = findViewById(R.id.cb_enter);
        cb_enter.setOnCheckedChangeListener((buttonView, isChecked) -> autoEnter = isChecked);
        bu_send.setOnClickListener(v -> {
            if (!isConnected || receiveThread == null) return;
            String text = AppUtil.getString(editText);
            if (AppUtil.notEmpty(text)) {
                receiveThread.sendCommand(text, autoEnter);
                editText.setText("");
            }
        });

        config = getIntent().getParcelableExtra("target");
        handler = new Handler(Looper.myLooper()) {
            @Override
            public void dispatchMessage(Message msg) {
                switch (msg.what) {
                    case MSG_CONNECTED:
                        Toast.makeText(ShellActivity.this, "连接成功", Toast.LENGTH_SHORT).show();
                        isConnected = true;
                        bu_send.setEnabled(true);
                        editText.setEnabled(true);
                        break;
                    case MSG_DISCONNECTED:
                        Toast.makeText(ShellActivity.this, "断开连接", Toast.LENGTH_SHORT).show();
                        isConnected = false;
                        receiveThread = null;
                        bu_send.setEnabled(false);
                        editText.setEnabled(false);
                        break;
                    case MSG_RECEIVE_TEXT:
                        textView.append((CharSequence) msg.obj);
                        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
                        break;
                }
            }
        };
        if (config != null) {
            receiveThread = new ReceiveThread();
            receiveThread.setConnectConfig(config);
            receiveThread.setHandler(handler);
            receiveThread.setDaemon(true);
            receiveThread.start();
        } else {
            Toast.makeText(ShellActivity.this, "没有连接配置", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        Log.i("666", event.toString());
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("666", "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("666", "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("666", "onPause");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("666", "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
        Log.i("666", "onDestroy");
    }
}