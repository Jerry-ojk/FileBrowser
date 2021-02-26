package jerry.filebrowser.file;

import android.graphics.drawable.Drawable;

public class FileType {
    public static final int TYPE_UNKNOWN = -1;
    public static final int TYPE_TXT = 1;
    public static final int TYPE_IMAGE = 2;
    public final Drawable icon;
    public final int type;


    public FileType(int type, Drawable icon) {
        this.type = type;
        this.icon = icon;
    }


    //    public static final int TYPE_TXT = getCode("txt");
//    public static final int TYPE_LOG = getCode("log");
//
//    public static final int type_JPG = getCode("jpg");
//    public static final int type_PNG = getCode("png");
//    public static final int type_GIF = getCode("gif");
//
//    public static final int type_mp4 = getCode("MP4");


//    public static int getTypeCode(String name) {
//        int dot = name.lastIndexOf('.');
//        if (dot != -1) {
//            int extensionLen = name.length() - dot - 1;
//            if (extensionLen <= 6) {
//                return getCode(name, dot + 1);
//            }
//        }
//        return TYPE_UNKNOWN;
//    }
//
//    public static int getCode(String extension) {
//        return getCode(extension, 0);
//    }

    //    public static int getCode(String extension, int offset) {
//        final int len = extension.length();
//        final int end = len - offset;
//        int code = 0;
//        for (int i = 0; i < end; i++) {
//            int a = extension.charAt(i + offset) - '0';
//            if (a < 0 || a > 25) return TYPE_UNKNOWN;
//            code |= (a << i * 5);
//        }
//        return code;
//    }
//    public static int getCode(String extension, int offset) {
//        final int len = extension.length();
//        final int end = len - offset;
//        int code = 0;
//        for (int i = 0; i < end; i++) {
//            int a = extension.charAt(i + offset) - '0';
//            if (a < 0 || a > 25) return TYPE_UNKNOWN;
//            code |= (a << i * 5);
//        }
//        return code;
//    }
}
