package com.jcraft.jsch;

import com.jcraft.jsch.exception.JSchException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class Util {

    private static final byte[] b64 = Util.str2byte("ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=");
    public static final byte[] EMPTY_BYTE = new byte[0];

    private static byte val(byte foo) {
        if (foo == '=') return 0;
        for (int j = 0; j < b64.length; j++) {
            if (foo == b64[j]) return (byte) j;
        }
        return 0;
    }

    public static byte[] fromBase64(byte[] buf, int start, int length) throws JSchException {
        try {
            byte[] foo = new byte[length];
            int j = 0;
            for (int i = start; i < start + length; i += 4) {
                foo[j] = (byte) ((val(buf[i]) << 2) | ((val(buf[i + 1]) & 0x30) >>> 4));
                if (buf[i + 2] == (byte) '=') {
                    j++;
                    break;
                }
                foo[j + 1] = (byte) (((val(buf[i + 1]) & 0x0f) << 4) | ((val(buf[i + 2]) & 0x3c) >>> 2));
                if (buf[i + 3] == (byte) '=') {
                    j += 2;
                    break;
                }
                foo[j + 2] = (byte) (((val(buf[i + 2]) & 0x03) << 6) | (val(buf[i + 3]) & 0x3f));
                j += 3;
            }
            byte[] bar = new byte[j];
            System.arraycopy(foo, 0, bar, 0, j);
            return bar;
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new JSchException("fromBase64: invalid base64 data", e);
        }
    }

    public static byte[] toBase64(byte[] buf, int start, int length) {

        byte[] tmp = new byte[length * 2];
        int i, j, k;

        int foo = (length / 3) * 3 + start;
        i = 0;
        for (j = start; j < foo; j += 3) {
            k = (buf[j] >>> 2) & 0x3f;
            tmp[i++] = b64[k];
            k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
            tmp[i++] = b64[k];
            k = (buf[j + 1] & 0x0f) << 2 | (buf[j + 2] >>> 6) & 0x03;
            tmp[i++] = b64[k];
            k = buf[j + 2] & 0x3f;
            tmp[i++] = b64[k];
        }

        foo = (start + length) - foo;
        if (foo == 1) {
            k = (buf[j] >>> 2) & 0x3f;
            tmp[i++] = b64[k];
            k = ((buf[j] & 0x03) << 4) & 0x3f;
            tmp[i++] = b64[k];
            tmp[i++] = (byte) '=';
            tmp[i++] = (byte) '=';
        } else if (foo == 2) {
            k = (buf[j] >>> 2) & 0x3f;
            tmp[i++] = b64[k];
            k = (buf[j] & 0x03) << 4 | (buf[j + 1] >>> 4) & 0x0f;
            tmp[i++] = b64[k];
            k = ((buf[j + 1] & 0x0f) << 2) & 0x3f;
            tmp[i++] = b64[k];
            tmp[i++] = (byte) '=';
        }
        byte[] bar = new byte[i];
        System.arraycopy(tmp, 0, bar, 0, i);
        return bar;

//    return sun.misc.BASE64Encoder().encode(buf);
    }

    public static String[] split(String name, String split) {
        return name.split(split);
//        if (foo == null)
//            return null;
//        byte[] buf = Util.str2byte(foo);
//        ArrayList<String> bar = new ArrayList<>();
//        int start = 0;
//        int index;
//        while (true) {
//            index = foo.indexOf(split, start);
//            if (index >= 0) {
//                bar.add(Util.byte2str(buf, start, index - start));
//                start = index + 1;
//                continue;
//            }
//            bar.add(Util.byte2str(buf, start, buf.length - start));
//            break;
//        }
//        if (foo == null)
//            return null;
//        return bar.toArray(new String[bar.size()]);
    }

    public static boolean glob(byte[] pattern, byte[] name) {
        return glob0(pattern, 0, name, 0);
    }

    public static boolean glob0(byte[] pattern, int pattern_index,
                                byte[] name, int name_index) {
        if (name.length > 0 && name[0] == '.') {
            if (pattern.length > 0 && pattern[0] == '.') {
                if (pattern.length == 2 && pattern[1] == '*') return true;
                return glob(pattern, pattern_index + 1, name, name_index + 1);
            }
            return false;
        }
        return glob(pattern, pattern_index, name, name_index);
    }

    static private boolean glob(byte[] pattern, int pattern_index,
                                byte[] name, int name_index) {
        //System.err.println("glob: "+new String(pattern)+", "+pattern_index+" "+new String(name)+", "+name_index);

        int patternlen = pattern.length;
        if (patternlen == 0)
            return false;

        int namelen = name.length;
        int i = pattern_index;
        int j = name_index;

        while (i < patternlen && j < namelen) {
            if (pattern[i] == '\\') {
                if (i + 1 == patternlen)
                    return false;
                i++;
                if (pattern[i] != name[j])
                    return false;
                i += skipUTF8Char(pattern[i]);
                j += skipUTF8Char(name[j]);
                continue;
            }

            if (pattern[i] == '*') {
                while (i < patternlen) {
                    if (pattern[i] == '*') {
                        i++;
                        continue;
                    }
                    break;
                }
                if (patternlen == i)
                    return true;

                byte foo = pattern[i];
                if (foo == '?') {
                    while (j < namelen) {
                        if (glob(pattern, i, name, j)) {
                            return true;
                        }
                        j += skipUTF8Char(name[j]);
                    }
                    return false;
                } else if (foo == '\\') {
                    if (i + 1 == patternlen)
                        return false;
                    i++;
                    foo = pattern[i];
                    while (j < namelen) {
                        if (foo == name[j]) {
                            if (glob(pattern, i + skipUTF8Char(foo),
                                    name, j + skipUTF8Char(name[j]))) {
                                return true;
                            }
                        }
                        j += skipUTF8Char(name[j]);
                    }
                    return false;
                }

                while (j < namelen) {
                    if (foo == name[j]) {
                        if (glob(pattern, i, name, j)) {
                            return true;
                        }
                    }
                    j += skipUTF8Char(name[j]);
                }
                return false;
            }

            if (pattern[i] == '?') {
                i++;
                j += skipUTF8Char(name[j]);
                continue;
            }

            if (pattern[i] != name[j])
                return false;

            i += skipUTF8Char(pattern[i]);
            j += skipUTF8Char(name[j]);

            if (!(j < namelen)) {         // name is end
                if (!(i < patternlen)) {    // pattern is end
                    return true;
                }
                if (pattern[i] == '*') {
                    break;
                }
            }
            continue;
        }

        if (i == patternlen && j == namelen)
            return true;

        if (!(j < namelen) &&  // name is end
                pattern[i] == '*') {
            boolean ok = true;
            while (i < patternlen) {
                if (pattern[i++] != '*') {
                    ok = false;
                    break;
                }
            }
            return ok;
        }

        return false;
    }

    public static String quote(String path) {
        byte[] _path = str2byte(path);
        int count = 0;
        for (int i = 0; i < _path.length; i++) {
            byte b = _path[i];
            if (b == '\\' || b == '?' || b == '*')
                count++;
        }
        if (count == 0)
            return path;
        byte[] _path2 = new byte[_path.length + count];
        for (int i = 0, j = 0; i < _path.length; i++) {
            byte b = _path[i];
            if (b == '\\' || b == '?' || b == '*') {
                _path2[j++] = '\\';
            }
            _path2[j++] = b;
        }
        return byte2str(_path2);
    }

    public static String unquote(String path) {
        byte[] foo = str2byte(path);
        byte[] bar = unquote(foo);
        if (foo.length == bar.length)
            return path;
        return byte2str(bar);
    }

    public static byte[] unquote(byte[] path) {
        int pathlen = path.length;
        int i = 0;
        while (i < pathlen) {
            if (path[i] == '\\') {
                if (i + 1 == pathlen)
                    break;
                System.arraycopy(path, i + 1, path, i, path.length - (i + 1));
                pathlen--;
                i++;
                continue;
            }
            i++;
        }
        if (pathlen == path.length)
            return path;
        byte[] foo = new byte[pathlen];
        System.arraycopy(path, 0, foo, 0, pathlen);
        return foo;
    }

    private static String[] chars = {
            "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"
    };

    public static String getFingerPrint(HASH hash, byte[] data) {
        try {
            hash.init();
            hash.update(data, 0, data.length);
            byte[] foo = hash.digest();
            StringBuilder sb = new StringBuilder();
            int bar;
            for (int i = 0; i < foo.length; i++) {
                bar = foo[i] & 0xff;
                sb.append(chars[(bar >>> 4) & 0xf]);
                sb.append(chars[(bar) & 0xf]);
                if (i + 1 < foo.length)
                    sb.append(":");
            }
            return sb.toString();
        } catch (Exception e) {
            return "???";
        }
    }

    static boolean array_equals(byte[] foo, byte[] bar) {
        int i = foo.length;
        if (i != bar.length) return false;
        for (int j = 0; j < i; j++) {
            if (foo[j] != bar[j]) return false;
        }
        //try{while(true){i--; if(foo[i]!=bar[i])return false;}}catch(Exception e){}
        return true;
    }

    public static Socket createSocket(String host, int port, int timeout) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), timeout);
        return socket;
    }

    public static byte[] str2byte(String str, String encoding) {
        if (str == null)
            return null;
        try {
            return str.getBytes(encoding);
        } catch (java.io.UnsupportedEncodingException e) {
            return str.getBytes();
        }
    }

    public static byte[] str2byte(String str) {
        if (str == null) return null;
        return str.getBytes(StandardCharsets.UTF_8);
    }

    public static String byte2str(byte[] bytes, String encoding) {
        return byte2str(bytes, 0, bytes.length, encoding);
    }

    public static String byte2str(byte[] bytes, int offset, int len, String encoding) {
        try {
            return new String(bytes, offset, len, encoding);
        } catch (java.io.UnsupportedEncodingException e) {
            return new String(bytes, offset, len);
        }
    }

    public static String byte2str(byte[] bytes) {
        try {
            return new String(bytes, 0, bytes.length, StandardCharsets.UTF_8);
        } catch (NullPointerException e) {
            return new String(bytes);
        }
    }

    public static String byte2str(byte[] bytes, int offset, int len) {
        try {
            return new String(bytes, offset, len, StandardCharsets.UTF_8);
        } catch (NullPointerException e) {
            return new String(bytes);
        }
    }

    public static String toHex(byte[] str) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < str.length; i++) {
            String foo = Integer.toHexString(str[i] & 0xff);
            stringBuilder.append("0x").append(foo.length() == 1 ? "0" : "").append(foo);
            if (i + 1 < str.length)
                stringBuilder.append(":");
        }
        return stringBuilder.toString();
    }


    static void clearZero(byte[] foo) {
        if (foo == null) return;
        Arrays.fill(foo, (byte) 0);
    }

    static String diffString(String str, String[] not_available) {
        String[] stra = Util.split(str, ",");
        StringBuilder result = null;
        loop:
        for (int i = 0; i < stra.length; i++) {
            for (int j = 0; j < not_available.length; j++) {
                if (stra[i].equals(not_available[j])) {
                    continue loop;
                }
            }
            if (result == null) {
                result = new StringBuilder(stra[i]);
            } else {
                result.append(",").append(stra[i]);
            }
        }
        return result != null ? result.toString() : "";
    }

    static String checkTilde(String str) {
        try {
            if (str.startsWith("~")) {
                str = str.replace("~", System.getProperty("user.home"));
            }
        } catch (SecurityException e) {
        }
        return str;
    }

    private static int skipUTF8Char(byte b) {
        if ((byte) (b & 0x80) == 0) return 1;
        if ((byte) (b & 0xe0) == (byte) 0xc0) return 2;
        if ((byte) (b & 0xf0) == (byte) 0xe0) return 3;
        return 1;
    }

    static byte[] readFile(String path) throws IOException {
        path = checkTilde(path);
        File file = new File(path);
        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] result = new byte[(int) (file.length())];
            int len = 0;
            while (true) {
                int i = fis.read(result, len, result.length - len);
                if (i <= 0)
                    break;
                len += i;
            }
            fis.close();
            return result;
        }
    }
}
