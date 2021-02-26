package jerry.filebrowser.view;

public class GestureAction {
    public static final String[] ACTIONS = {"打开目录", "局域网", "网盘"};
    public int id;
    public char[] path;
    public String target;

    public GestureAction(char[] path) {
        this.path = path;
    }

    public GestureAction(int id, char[] path, String target) {
        this.id = id;
        this.path = path;
        this.target = target;
    }

    @Override
    public String toString() {
        return "id=" + id + ",path=" + pathToString(path) + ",target=" + target;
    }

    public static String pathToString(char[] path) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < path.length; i += 2) {
            builder.append("(").append((int) path[i]).append(',').append((int) path[i + 1]).append(")");
        }
        return builder.toString();
    }
}
