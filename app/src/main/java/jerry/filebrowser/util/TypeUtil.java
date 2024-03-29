package jerry.filebrowser.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Locale;

import jerry.filebrowser.R;
import jerry.filebrowser.container.Trie;
import jerry.filebrowser.file.FileType;

public class TypeUtil {
    public static HashMap<String, String> MIME_MAP = new HashMap<>(16);
    public static String MIME_TYPE_DIR = "application/directory";
    public static String MIME_TYPE_UNKNOWN = "application/octet-stream";

    static {
        MIME_MAP.put("js", "application/javascript");
        MIME_MAP.put("json", "application/json");
        MIME_MAP.put("log", "text/plain");
        MIME_MAP.put("bat", "text/plain");
        MIME_MAP.put("ttf", "application/x-font-ttf");
        MIME_MAP.put("otf", "application/x-font-otf");
    }

    public static String getMimeType(String name) {
        int i = name.lastIndexOf('.');
        int len = name.length() - i - 1;
        if (i != -1 && len >= 1 && len < 5) {
            String mime;
            String extension = name.substring(i + 1).toLowerCase(Locale.US);
            // 获取sdk mime
            mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (mime != null) {
                return mime;
            }
            // 获取自定义 mime
            mime = MIME_MAP.get(extension);
            if (mime != null) {
                return mime;
            }
        }
        return MIME_TYPE_UNKNOWN;
    }

    private final Trie<FileType> TYPE_TRIE = new Trie<>();

    private Drawable icon_file;
    private Drawable icon_folder;

    public TypeUtil(Context context) {
        init(context);
    }

    public void init(Context context) {
        icon_file = ContextCompat.getDrawable(context, R.drawable.ic_type_file);
        icon_folder = ContextCompat.getDrawable(context, R.drawable.ic_type_folder);
        Drawable icon_txt = ContextCompat.getDrawable(context, R.drawable.ic_type_txt1);
        // Drawable icon_code = ContextCompat.getDrawable(context, R.drawable.ic_type_code2);
        Drawable icon_code = ContextCompat.getDrawable(context, R.drawable.ic_code);
        Drawable icon_pdf = ContextCompat.getDrawable(context, R.drawable.ic_type_pdf3);
        Drawable icon_ttf = ContextCompat.getDrawable(context, R.drawable.ic_type_ttf);
        Drawable icon_image = ContextCompat.getDrawable(context, R.drawable.ic_type_image);
        Drawable icon_music = ContextCompat.getDrawable(context, R.drawable.ic_type_music4);
        Drawable icon_video = ContextCompat.getDrawable(context, R.drawable.ic_type_video);
        Drawable icon_apk = ContextCompat.getDrawable(context, R.drawable.ic_type_apk);
        Drawable icon_compress = ContextCompat.getDrawable(context, R.drawable.ic_type_archive);

//        icon_compress.mutate();
//        icon_compress.setTint(0xFF82F386);

        // 存入后缀名
        TYPE_TRIE.put("7z", new FileType(FileType.TYPE_UNKNOWN, icon_compress));
        TYPE_TRIE.put("aac", new FileType(FileType.TYPE_UNKNOWN, icon_music));
        TYPE_TRIE.put("apk", new FileType(FileType.TYPE_UNKNOWN, icon_apk));
        TYPE_TRIE.put("avi", new FileType(FileType.TYPE_VIDEO, icon_video));
        TYPE_TRIE.put("bmp", new FileType(FileType.TYPE_IMAGE, icon_image));
        TYPE_TRIE.put("c", new FileType(FileType.TYPE_TXT, icon_code));
        TYPE_TRIE.put("cpp", new FileType(FileType.TYPE_TXT, icon_code));
        TYPE_TRIE.put("css", new FileType(FileType.TYPE_TXT, icon_code));
        TYPE_TRIE.put("csv", new FileType(FileType.TYPE_TXT, icon_txt));
        TYPE_TRIE.put("flac", new FileType(FileType.TYPE_UNKNOWN, icon_music));
        TYPE_TRIE.put("flv", new FileType(FileType.TYPE_VIDEO, icon_video));
        TYPE_TRIE.put("gif", new FileType(FileType.TYPE_IMAGE, icon_image));
        TYPE_TRIE.put("gz", new FileType(FileType.TYPE_UNKNOWN, icon_compress));
        TYPE_TRIE.put("h", new FileType(FileType.TYPE_TXT, icon_code));
        TYPE_TRIE.put("html", new FileType(FileType.TYPE_TXT, icon_code));
        TYPE_TRIE.put("ini", new FileType(FileType.TYPE_TXT, icon_txt));
        TYPE_TRIE.put("ios", new FileType(FileType.TYPE_UNKNOWN, icon_compress));
        TYPE_TRIE.put("jar", new FileType(FileType.TYPE_UNKNOWN, icon_compress));
        TYPE_TRIE.put("jpeg", new FileType(FileType.TYPE_IMAGE, icon_image));
        TYPE_TRIE.put("jpg", new FileType(FileType.TYPE_IMAGE, icon_image));
        TYPE_TRIE.put("js", new FileType(FileType.TYPE_TXT, icon_code));
        TYPE_TRIE.put("json", new FileType(FileType.TYPE_TXT, icon_code));
        TYPE_TRIE.put("log", new FileType(FileType.TYPE_TXT, icon_txt));
        TYPE_TRIE.put("mkv", new FileType(FileType.TYPE_VIDEO, icon_video));
        TYPE_TRIE.put("mov", new FileType(FileType.TYPE_VIDEO, icon_video));
        TYPE_TRIE.put("mp3", new FileType(FileType.TYPE_UNKNOWN, icon_music));
        TYPE_TRIE.put("mp4", new FileType(FileType.TYPE_VIDEO, icon_video));
        TYPE_TRIE.put("mpeg", new FileType(FileType.TYPE_VIDEO, icon_video));
        TYPE_TRIE.put("mpg", new FileType(FileType.TYPE_VIDEO, icon_video));
        TYPE_TRIE.put("otf", new FileType(FileType.TYPE_UNKNOWN, icon_ttf));
        TYPE_TRIE.put("pdf", new FileType(FileType.TYPE_UNKNOWN, icon_pdf));
        TYPE_TRIE.put("png", new FileType(FileType.TYPE_IMAGE, icon_image));
        TYPE_TRIE.put("rar", new FileType(FileType.TYPE_UNKNOWN, icon_compress));
        TYPE_TRIE.put("raw", new FileType(FileType.TYPE_IMAGE, icon_image));
        TYPE_TRIE.put("sh", new FileType(FileType.TYPE_TXT, icon_code));
        TYPE_TRIE.put("so", new FileType(FileType.TYPE_UNKNOWN, icon_compress));
        TYPE_TRIE.put("tif", new FileType(FileType.TYPE_IMAGE, icon_image));
        TYPE_TRIE.put("tiff", new FileType(FileType.TYPE_IMAGE, icon_image));
        TYPE_TRIE.put("ts", new FileType(FileType.TYPE_VIDEO, icon_video));
        TYPE_TRIE.put("ttf", new FileType(FileType.TYPE_UNKNOWN, icon_ttf));
        TYPE_TRIE.put("txt", new FileType(FileType.TYPE_TXT, icon_txt));
        TYPE_TRIE.put("webp", new FileType(FileType.TYPE_IMAGE, icon_image));
        TYPE_TRIE.put("wmv", new FileType(FileType.TYPE_VIDEO, icon_video));
        TYPE_TRIE.put("xml", new FileType(FileType.TYPE_TXT, icon_code));
        TYPE_TRIE.put("zip", new FileType(FileType.TYPE_UNKNOWN, icon_compress));
    }

    public int getType(String name) {
        final int dot = name.lastIndexOf('.') + 1;
        if (dot != 0) {
            final int extensionLen = name.length() - dot;
            if (extensionLen > 0 && extensionLen <= 4) {
                String extension = name.substring(dot).toLowerCase();
                FileType fileType = TYPE_TRIE.get(extension);
                if (fileType != null) {
                    return fileType.type;
                }
            }
        }
        return FileType.TYPE_UNKNOWN;
    }

    public int fillIcon(ImageView imageView, String name) {
        final int dot = name.lastIndexOf('.') + 1;
        if (dot != 0) {
            final int extensionLen = name.length() - dot;
            if (extensionLen > 0 && extensionLen <= 4) {
                String extension = name.substring(dot).toLowerCase();
                FileType fileType = TYPE_TRIE.get(extension);
                if (fileType != null) {
                    imageView.setImageDrawable(fileType.icon);
                    return fileType.type;
                }
            }
        }
        imageView.setImageDrawable(icon_file);
        return FileType.TYPE_UNKNOWN;
    }

    public static String getExtensionName(String name) {
        final int dot = name.lastIndexOf('.') + 1;
        if (dot != 0) {
            if (name.length() - dot > 0) {
                return name.substring(dot);
            }
        }
        return null;
    }

    public Drawable getFileDrawable() {
        return icon_file;
    }

    public Drawable getFolderDrawable() {
        return icon_folder;
    }
}
