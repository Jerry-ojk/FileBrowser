package jerry.filebrowser.shell;

import android.widget.TextView;

public class TerminalAction {
    public static class Action {
        public static final int ACTION_APPEND = 1;
        public static final int ACTION_CHANGE_COLOR = 3;
        public static final int ACTION_CLEAR_AFTER_CURSOR = 4;
        public static final int ACTION_CLEAR_ALL = 5;
        public int action;
        public CharSequence text;
        public Action next;
    }

    private Action head;
    private Action tail;

    public void clear() {
        head = null;
    }

    public void append(Action node) {
        if (tail != null) {
            tail.next = node;
        } else {
            tail = node;
            head = node;
        }
    }

    // vt-100
    public CharSequence pauseTerminalText(String text) {
        // SpannableStringBuilder builder = new SpannableStringBuilder();
        StringBuilder builder = new StringBuilder(text.length());
        final int textLen = text.length();
        for (int i = 0; i < textLen; i++) {
            final char c = text.charAt(i);
            if (c != '\033') {// ESC
                builder.append(text.charAt(i));
            } else {
                if(textLen >= (i + 2))
                if (textLen >= (i + 2) && text.charAt(i + 1) == '[') {
                    i += 2;
                    int offset = Math.min(textLen - (i) - 1, 24);
                    loop:
                    for (int j = 0; j < offset; j++) {
                        final char color = text.charAt(i + j);
                        switch (color) {
                            case 'm': // \033[0m
                                // Log.i("666", text.substring(i, i + j + 1));
                                i = j + i;
                                //Action action = new Action();
                                //action.action = Action.ACTION_APPEND;
                                //append();
                                break loop;
                            case 'A':// 光标上移n行
                                i = j + i;
                                break loop;
                            case 'B':// 光标下移n行
                                i = j + i;
                                break loop;
                            case 'C':// 光标右移n行
                                i = j + i;
                                break loop;
                            case 'D':// 光标左移n行
                                i = j + i;
                                break loop;
                            case 'H':// 设置光标位置 \033[y;xH
                                i = j + i;
                                break loop;
                            case 'J':// 清屏 \033[2J
                                i = j + i;
                                break loop;
                            case 'K':// 清除从光标到行尾的内容
                                i = j + i;
                                break loop;
                            case 's':// 保存光标位置
                                i = j + i;
                                break loop;
                            case 'u':// 恢复光标位置
                                i = j + i;
                                break loop;
                            case 'l':// 隐藏光标 \033[?25l
                                i = j + i;
                                break loop;
                            case 'h':// 显示光标 \033[?25h
                                i = j + i;
                                break loop;
                        }
//                        else if (e == '\033') { // ESC
//                            break;
//                        }
                    }
                }
            }
        }
        return builder;
    }

    public void deal(TextView editText, int cursor) {
        Action node = head;
        while (node != null) {
            switch (node.action) {
                case Action.ACTION_APPEND:
                    editText.append(node.text);
                    break;
                case Action.ACTION_CHANGE_COLOR:
                    break;
                case Action.ACTION_CLEAR_AFTER_CURSOR:

                case Action.ACTION_CLEAR_ALL:
            }
            node = node.next;
        }
    }
}