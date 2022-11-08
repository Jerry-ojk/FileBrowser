package jerry.filebrowser.container;

public class Trie<T> {
    private int size;
    private final TrieNode<T> root;

    public Trie() {
        size = 0;
        root = new TrieNode<>(null);
    }

    public void put(String str, T data) {
        if (str.isEmpty()) return;
        TrieNode<T> node = root;
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            TrieNode<T> child = node.getChildNode(c);
            if (child == null) {
                child = node.addChildNode(c, null);
            }
            node = child;
        }
        node.setData(data);
        if (!node.isEndNode()) {
            ++size;
            node.setEndNode(true); // 设置为结束点
        }
    }

    public T get(String str) {
        if (str.isEmpty()) return null;
        TrieNode<T> node = root;
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (node != null) {
                node = node.getChildNode(c);
            } else {
                return null;
            }
        }
        if (node != null && node.isEndNode()) {
            return node.getData();
        }
        return null;
    }

    static public class TrieNode<T> {
        private T data;
        private boolean isEndNode; // 是否可以结束
        private boolean hasChild;
        private final TrieNode<T>[] child;

        public TrieNode(T data) {
            this.data = data;
            isEndNode = false;
            hasChild = false;
            child = new TrieNode[10 + 26];
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public boolean isEndNode() {
            return isEndNode;
        }

        public void setEndNode(boolean isEndNode) {
            this.isEndNode = isEndNode;
        }

        public TrieNode<T> getChildNode(char c) {
            int i = 0;
            if (c >= '0' && c <= '9') {
                i = c - '0';
            } else {
                i = c - 'a';
            }
            return child[i];
        }

        public TrieNode<T> addChildNode(char c, T type) {
            hasChild = true;
            int i = 0;
            if (c >= '0' && c <= '9') {
                i = c - '0';
            } else {
                i = c - 'a';
            }
            TrieNode<T> node = new TrieNode<T>(type);
            child[i] = node;
            return node;
        }
    }
}


