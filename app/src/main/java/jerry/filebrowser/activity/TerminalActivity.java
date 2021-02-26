package jerry.filebrowser.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import jerry.filebrowser.R;

public class TerminalActivity extends AppCompatActivity {
    //private Switch sw_root_mode;
    private EditText ed_input;
    private TextView tv_output;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terminal);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        //sw_root_mode = findViewById(R.id.sw_root_mode);
        //sw_root_mode.setEnabled(false);
        ed_input = findViewById(R.id.ed_input);
        findViewById(R.id.bu_submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tv_output.setText(execute(ed_input.getText().toString()));
            }
        });
        tv_output = findViewById(R.id.tv_output);
    }

    private String execute(String command) {
        StringBuilder builder = new StringBuilder();
        try {
            Process process = Runtime.getRuntime().exec("su");
            //获取输出流
            OutputStream outputStream = process.getOutputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
            //将命令写入
            dataOutputStream.writeBytes(command);
            //提交命令
            dataOutputStream.flush();
            //关闭流操作
            dataOutputStream.close();
            outputStream.close();
            String line;
            //process.getInputStream().read()
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            process.destroy();
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return builder.toString();
    }
}
