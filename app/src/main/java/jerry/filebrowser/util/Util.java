package jerry.filebrowser.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jerry.filebrowser.file.UnixFile;


public class Util {
    public static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA);
    public static Date date = new Date();
    public static DecimalFormat decimalFormat = new DecimalFormat("0.00");
//    public static StringBuilder builder = new StringBuilder();


    public static String size(long size) {
        if (size < 0) return "未知：（" + size + ")";
        String result;
        if (size < 0x400) {
            result = size + "B";
        } else if (size < 0x100000) {
            result = decimalFormat.format(size / 1024d) + "KB";
        } else if (size < 0x40000000) {
            result = decimalFormat.format(size / 1048876d) + "MB";
        } else {
            result = decimalFormat.format(size / 1073741824d) + "GB";
        }
        return result;
    }

    public static String time(long time) {
        if (time <= 0) return "未知";
        date.setTime(time);
        return format.format(date);
    }

    /**
     * public static final int S_IRWXU 00700 100000000
     * public static final int S_IRUSR 00400
     * public static final int S_IWUSR 00200
     * public static final int S_IXUSR 00100
     * public static final int S_IRWXG 00070
     * public static final int S_IRGRP 00040 0000000100000
     * public static final int S_IWGRP 00020 0000000010000
     * public static final int S_IXGRP 00010 0000000001000
     * public static final int S_IRWXO 00007 0000000000111
     * public static final int S_IROTH 00004 0000000000100
     * public static final int S_IWOTH 00002 0000000000010
     * public static final int S_IXOTH 00001 0000000000001
     */
//    private static char[] MODE_CHAR = {'r', 'w', 'x'};

    /**
     * permission [0,8]位
     *
     * @param mode 权限位
     * @return
     */
    public static String permission(int mode) {
        if (mode == -1) {
            return "未知";
        }
        StringBuilder builder = new StringBuilder(11);
        if ((mode & 0b100_000_000) != 0) {
            builder.append('r');
        } else {
            builder.append('-');
        }
        if ((mode & 0b010_000_000) != 0) {
            builder.append('w');
        } else {
            builder.append('-');
        }
        if ((mode & UnixFile.S_ISUID) != 0) {
            builder.append('s');
        } else if ((mode & 0b001_000_000) != 0) {
            builder.append('x');
        } else {
            builder.append('-');
        }
        builder.append(' ');
        if ((mode & 0b000_100_000) != 0) {
            builder.append('r');
        } else {
            builder.append('-');
        }
        if ((mode & 0b000_010_000) != 0) {
            builder.append('w');
        } else {
            builder.append('-');
        }
        if ((mode & UnixFile.S_ISGID) != 0) {
            builder.append('s');
        } else if ((mode & 0b000_001_000) != 0) {
            builder.append('x');
        } else {
            builder.append('-');
        }
        builder.append(' ');
        if ((mode & 0b000_000_100) != 0) {
            builder.append('r');
        } else {
            builder.append('-');
        }
        if ((mode & 0b000_000_010) != 0) {
            builder.append('w');
        } else {
            builder.append('-');
        }
        if ((mode & UnixFile.S_ISVTX) != 0) {
            builder.append('v');
        } else if ((mode & 0b000_000_001) != 0) {
            builder.append('x');
        } else {
            builder.append('-');
        }
        return builder.toString();
    }

    /**
     * permission [0,8]+[9,11]位
     *
     * @param mode [0,11]为代表setId位权限位
     * @return
     */
    public static String mode(int mode) {
        if (mode == -1) {
            return "未知";
        }
        final StringBuilder builder = new StringBuilder(13);
        switch (mode & UnixFile.MASK_TYPE) {
            case UnixFile.S_IFIFO:
                builder.append('f');
                break;
            case UnixFile.S_IFCHR:
                builder.append('c');
                break;
            case UnixFile.S_IFDIR:
                builder.append('d');
                break;
            case UnixFile.S_IFBLK://100
                builder.append('b');
                break;
            case UnixFile.S_IFREG:
                builder.append('-');
                break;
            case UnixFile.S_IFLNK:
                builder.append('l');
                break;
            case UnixFile.S_IFSOCK:
                builder.append('s');
                break;
            default:
                builder.append('?');
        }
        builder.append(' ');
        if ((mode & 0b100_000_000) != 0) {
            builder.append('r');
        } else {
            builder.append('-');
        }
        if ((mode & 0b010_000_000) != 0) {
            builder.append('w');
        } else {
            builder.append('-');
        }
        if ((mode & UnixFile.S_ISUID) != 0) {
            builder.append('s');
        } else if ((mode & 0b001_000_000) != 0) {
            builder.append('x');
        } else {
            builder.append('-');
        }
        builder.append(' ');
        if ((mode & 0b000_100_000) != 0) {
            builder.append('r');
        } else {
            builder.append('-');
        }
        if ((mode & 0b000_010_000) != 0) {
            builder.append('w');
        } else {
            builder.append('-');
        }
        if ((mode & UnixFile.S_ISGID) != 0) {
            builder.append('s');
        } else if ((mode & 0b000_001_000) != 0) {
            builder.append('x');
        } else {
            builder.append('-');
        }
        builder.append(' ');
        if ((mode & 0b000_000_100) != 0) {
            builder.append('r');
        } else {
            builder.append('-');
        }
        if ((mode & 0b000_000_010) != 0) {
            builder.append('w');
        } else {
            builder.append('-');
        }
        if ((mode & UnixFile.S_ISVTX) != 0) {
            builder.append('v');
        } else if ((mode & 0b000_000_001) != 0) {
            builder.append('x');
        } else {
            builder.append('-');
        }
        return builder.toString();
    }


    public static String type(int mode) {
        switch (mode & UnixFile.MASK_TYPE) {
            case UnixFile.S_IFIFO:
                return "FIFO文件";
            case UnixFile.S_IFCHR:
                return "字符文件";
            case UnixFile.S_IFDIR:
                return "文件夹";
            case UnixFile.S_IFBLK://100
                return "块文件";
            case UnixFile.S_IFREG:
                return "文件";
            case UnixFile.S_IFLNK:
                return "链接文件";
            case UnixFile.S_IFSOCK:
                return "SOCK文件";
            default:
                return "未知类型（" + Integer.toString((mode & 0xF000) >> 12) + ")";
        }
    }

    public static String errorToString(Exception e) {
        StringWriter writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);
        writer.write("详情：\n");
        writer.write(e.toString());
        printWriter.close();
        return writer.toString();
    }

    public static boolean equals(String a, String b) {
        if (a == null) {
            return b == null;
        } else {
            return a.equals(b);
        }
    }
}
