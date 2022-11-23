package jerry.filebrowser;

import org.junit.Test;

import java.util.Objects;

import jerry.filebrowser.container.Trie;

public class TrieTest {

    @Test
    public void test() {
        Trie<String> TYPE_TRIE = new Trie<>();
        TYPE_TRIE.put("gif", "gif");
        TYPE_TRIE.put("gz", "gz");
        TYPE_TRIE.put("htm", "htm");
        TYPE_TRIE.put("html", "html");
        TYPE_TRIE.put("ini", "ini");

        System.out.println(TYPE_TRIE.get("gif"));
        System.out.println(TYPE_TRIE.get("gz"));
        System.out.println(TYPE_TRIE.get("pdf"));
        System.out.println(TYPE_TRIE.get("htm"));
        System.out.println(TYPE_TRIE.get("html"));
        System.out.println(TYPE_TRIE.get("ini"));
        System.out.println(TYPE_TRIE.get("in"));
        System.out.println(TYPE_TRIE.get("2"));

        assert Objects.equals(TYPE_TRIE.get("gif"), "gif");
        assert Objects.equals(TYPE_TRIE.get("gz"), "gz");
        assert TYPE_TRIE.get("pdf") == null;
        assert Objects.equals(TYPE_TRIE.get("htm"), "htm");
        assert Objects.equals(TYPE_TRIE.get("html"), "html");
        assert Objects.equals(TYPE_TRIE.get("ini"), "ini");
        assert TYPE_TRIE.get("in") == null;
    }
}
