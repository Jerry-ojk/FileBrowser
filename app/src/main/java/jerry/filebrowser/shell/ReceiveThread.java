package jerry.filebrowser.shell;

import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.SSHClient;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.exception.JSchException;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;

import jerry.filebrowser.app.AppUtil;
import jerry.filebrowser.ssh.SSHConnectManager;

public class ReceiveThread extends Thread {
    private ShellConfig config;
    private volatile Session session;
    private volatile ChannelShell channelShell;
    private WeakReference<Handler> handlerReference;
    //    private ReentrantLock lock = new ReentrantLock();
    //    private Condition isEmpty = lock.newCondition();
    private LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private volatile TerminalAction action;
    private volatile boolean isEndWithLF = true;
    private int lineWidth = 0;

    public void setHandler(Handler handler) {
        handlerReference = new WeakReference<>(handler);
    }

    public void setConnectConfig(ShellConfig config) {
        this.config = config;
    }

    public void sendCommand(String command) {
        sendCommand(command, true);
    }

    public void sendCommand(String command, boolean LF) {
        if (AppUtil.notEmpty(command) && queue != null) {
            if (LF && command.charAt(command.length() - 1) != '\n') {
                command += '\n';
            }
            queue.offer(command);
        }
    }


    private void connect() throws JSchException {
        // Log.i("666", "service connect");
        final SSHClient client = SSHConnectManager.getSshClient();
        session = client.getSession(config);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        action = new TerminalAction();
        channelShell = session.openChannel("shell");
        channelShell.setShellListener(// sendMessage(ShellActivity.MSG_RECEIVE_TEXT, "\n接收文本出错");
//
//            if (AppUtil.notEmpty(text)) {
//                if (!isEndWithLF && !(text.startsWith("\r\n") || text.startsWith("\n"))) {
//                    text = '\n' + text;
//                }
//                if (text.charAt(text.length() - 1) == '\n') {
//                    isEndWithLF = true;
//                } else {
//                    isEndWithLF = false;
//                }
//                sendMessage(ShellActivity.MSG_RECEIVE_TEXT, text);
//            }
// action.clear();
// action.decodeTerminalText(text);
                this::onReceiveText);
        channelShell.connect();
        sendMessage(ShellActivity.MSG_CONNECTED);
    }

    public boolean sendMessage(int what) {
        return sendMessage(what, null);
    }

    public boolean sendMessage(int what, Object object) {
        if (handlerReference != null) {
            Handler handler = handlerReference.get();
            if (handler != null) {
                Message message = Message.obtain();
                message.what = what;
                message.obj = object;
                handler.sendMessage(message);
                return true;
            }
        }
        return false;
    }


    @Override
    public void run() {
        try {
            connect();
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT);
            while (session != null && session.isConnected()
                    && channelShell != null && channelShell.isConnected()
                    && !isInterrupted()) {
                final String command = queue.take();
                channelShell.sendCommand(command.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Log.i("666", "ReceiveThread 断开连接");
            config = null;
            if (channelShell != null) {
                channelShell.disconnect();
                channelShell = null;
            }
            if (session != null) {
                session.disconnect();
                session = null;
            }
            if (handlerReference != null) {
                sendMessage(ShellActivity.MSG_DISCONNECTED);
                handlerReference.clear();
                handlerReference = null;
            }
        }
    }

    private void onReceiveText(byte[] data, int start, int len) {
        String text = null;
        try {
            text = new String(data, start, len, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // sendMessage(ShellActivity.MSG_RECEIVE_TEXT, "\n接收文本出错");
            e.printStackTrace();
        }
//
//            if (AppUtil.notEmpty(text)) {
//                if (!isEndWithLF && !(text.startsWith("\r\n") || text.startsWith("\n"))) {
//                    text = '\n' + text;
//                }
//                if (text.charAt(text.length() - 1) == '\n') {
//                    isEndWithLF = true;
//                } else {
//                    isEndWithLF = false;
//                }
//                sendMessage(ShellActivity.MSG_RECEIVE_TEXT, text);
//            }

        // action.clear();
        // action.decodeTerminalText(text);
        if (AppUtil.isEmpty(text)) return;
        final int slen = text.length();
        StringBuilder builder = new StringBuilder(slen + 16);
        for (int i = 0; i < slen; i++) {
            final char c = text.charAt(i);
            if (c == '\r') {
                continue;
            } else if (c == '\n') {
                builder.append('\n');
                lineWidth = 0;
            } else if (lineWidth == 79) {
                builder.append('\n').append(c);
                lineWidth = 0;
            } else {
                builder.append(c);
                lineWidth++;
            }
        }
        sendMessage(ShellActivity.MSG_RECEIVE_TEXT, builder);
    }
}
