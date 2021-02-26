package jerry.filebrowser.file;

import com.jcraft.jsch.Buffer;
import com.jcraft.jsch.Util;

import java.util.Date;

/*
  uint32   flags
  uint64   size           present only if flag SSH_FILEXFER_ATTR_SIZE
  uint32   uid            present only if flag SSH_FILEXFER_ATTR_UIDGID
  uint32   gid            present only if flag SSH_FILEXFER_ATTR_UIDGID
  uint32   permissions    present only if flag SSH_FILEXFER_ATTR_PERMISSIONS
  uint32   atime          present only if flag SSH_FILEXFER_ACMODTIME
  uint32   mtime          present only if flag SSH_FILEXFER_ACMODTIME
  uint32   extended_count present only if flag SSH_FILEXFER_ATTR_EXTENDED
  string   extended_type
  string   extended_data
    ...      more extended data (extended_type - extended_data pairs),
             so that number of pairs equals extended_count
*/
public class SFTPAttrs {

    static public final int S_ISUID = 0b100_000_000_000; // set user ID on execution
    static public final int S_ISGID = 0b010_000_000_000; // set group ID on execution
    static public final int S_ISVTX = 0b001_000_000_000; // sticky bit  粘住位

    static public final int S_IRUSR = 0b100_000_000; // read by owner
    static public final int S_IWUSR = 0b010_000_000; // write by owner
    static public final int S_IXUSR = 0b001_000_000; // execute/search by owner

    static public final int S_IRGRP = 0b100_000; // read by group
    static public final int S_IWGRP = 0b010_000; // write by group
    static public final int S_IXGRP = 0b001_000; // execute/search by group

    static public final int S_IROTH = 0b000_100; // read by others
    static public final int S_IWOTH = 0b000_010; // write by others
    static public final int S_IXOTH = 0b000_001; // execute/search by others

    public static final int pmask = 0xFFF;

    public static final int SSH_FILEXFER_ATTR_SIZE = 0x00000001;
    public static final int SSH_FILEXFER_ATTR_UIDGID = 0x00000002;
    public static final int SSH_FILEXFER_ATTR_PERMISSIONS = 0x00000004;
    public static final int SSH_FILEXFER_ATTR_ACMODTIME = 0x00000008;
    public static final int SSH_FILEXFER_ATTR_EXTENDED = 0x80000000;

    public static final int S_IFMT = 0xF000;
    public static final int S_IFIFO = 0x1000;
    public static final int S_IFCHR = 0x2000;
    public static final int S_IFDIR = 0x4000;
    public static final int S_IFBLK = 0x6000;
    public static final int S_IFREG = 0x8000;
    public static final int S_IFLNK = 0xa000;
    public static final int S_IFSOCK = 0xc000;

    public int flags = 0;
    public long size = -1;
    public int uid = -1;
    public int gid = -1;
    public int permissions = -1;
    public int atime = -1;//访问时间
    public int mtime = -1;//修改时间
    public String[] extended = null;

    public SFTPAttrs() {
    }

    //    uint32   flags
//    uint64   size           present only if flag SSH_FILEXFER_ATTR_SIZE
//    uint32   uid            present only if flag SSH_FILEXFER_ATTR_UIDGID
//    uint32   gid            present only if flag SSH_FILEXFER_ATTR_UIDGID
//    uint32   permissions    present only if flag SSH_FILEXFER_ATTR_PERMISSIONS
//    uint32   atime          present only if flag SSH_FILEXFER_ACMODTIME
//    uint32   mtime          present only if flag SSH_FILEXFER_ACMODTIME
//    uint32   extended_count present only if flag SSH_FILEXFER_ATTR_EXTENDED
//    string   extended_type
//    string   extended_data
//   	...      more extended data (extended_type - extended_data pairs),
//    so that number of pairs equals extended_count
    public static SFTPAttrs getATTR(Buffer buffer) {
        SFTPAttrs attr = new SFTPAttrs();
        attr.flags = buffer.readInt();
        if ((attr.flags & SSH_FILEXFER_ATTR_SIZE) != 0) {
            attr.size = buffer.readLong();
        }
        if ((attr.flags & SSH_FILEXFER_ATTR_UIDGID) != 0) {
            attr.uid = buffer.readInt();
            attr.gid = buffer.readInt();
        }
        if ((attr.flags & SSH_FILEXFER_ATTR_PERMISSIONS) != 0) {
            attr.permissions = buffer.readInt();
        }
        if ((attr.flags & SSH_FILEXFER_ATTR_ACMODTIME) != 0) {
            attr.atime = buffer.readInt();
            attr.mtime = buffer.readInt();
        }

        if ((attr.flags & SSH_FILEXFER_ATTR_EXTENDED) != 0) {
            int count = buffer.readInt();
            if (count > 0) {
                attr.extended = new String[count * 2];
                for (int i = 0; i < count; i++) {
                    attr.extended[i * 2] = Util.byte2str(buffer.getStringByte());
                    attr.extended[i * 2 + 1] = Util.byte2str(buffer.getStringByte());
                }
            }
        }
        return attr;
    }


    public void dump(Buffer buffer) {
        buffer.putInt(flags);
        if ((flags & SSH_FILEXFER_ATTR_SIZE) != 0) {
            buffer.putLong(size);
        }
        if ((flags & SSH_FILEXFER_ATTR_UIDGID) != 0) {
            buffer.putInt(uid);
            buffer.putInt(gid);
        }
        if ((flags & SSH_FILEXFER_ATTR_PERMISSIONS) != 0) {
            buffer.putInt(permissions);
        }
        if ((flags & SSH_FILEXFER_ATTR_ACMODTIME) != 0) {
            buffer.putInt(atime);
        }
        if ((flags & SSH_FILEXFER_ATTR_ACMODTIME) != 0) {
            buffer.putInt(mtime);
        }
        if ((flags & SSH_FILEXFER_ATTR_EXTENDED) != 0) {
            int count = extended.length / 2;
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    buffer.putString(Util.str2byte(extended[i * 2]));
                    buffer.putString(Util.str2byte(extended[i * 2 + 1]));
                }
            }
        }
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public void setSIZE(long size) {
        flags |= SSH_FILEXFER_ATTR_SIZE;
        this.size = size;
    }

    public void setUidAndGid(int uid, int gid) {
        flags |= SSH_FILEXFER_ATTR_UIDGID;
        this.uid = uid;
        this.gid = gid;
    }

    public void setACMODTIME(int atime, int mtime) {
        flags |= SSH_FILEXFER_ATTR_ACMODTIME;
        this.atime = atime;
        this.mtime = mtime;
    }

    public void setPermissions(int permissions) {
        flags |= SSH_FILEXFER_ATTR_PERMISSIONS;
        permissions = (this.permissions & ~pmask) | (permissions & pmask);
        this.permissions = permissions;
    }

    private boolean isType(int mask) {
        return (flags & SSH_FILEXFER_ATTR_PERMISSIONS) != 0 &&
                (permissions & S_IFMT) == mask;
    }

    public String getPermissionsString() {
        if ((flags & SFTPAttrs.SSH_FILEXFER_ATTR_PERMISSIONS) == 0) return "未知";
        StringBuilder buf = new StringBuilder(13);
        if (isDir()) buf.append('d');
        else if (isLink()) buf.append('l');
        else buf.append('-');
        buf.append(' ');
        if ((permissions & S_IRUSR) != 0) buf.append('r');
        else buf.append('-');

        if ((permissions & S_IWUSR) != 0) buf.append('w');
        else buf.append('-');

        if ((permissions & S_ISUID) != 0) buf.append('s');
        else if ((permissions & S_IXUSR) != 0) buf.append('x');
        else buf.append('-');

        buf.append(' ');
        if ((permissions & S_IRGRP) != 0) buf.append('r');
        else buf.append('-');

        if ((permissions & S_IWGRP) != 0) buf.append('w');
        else buf.append('-');

        if ((permissions & S_ISGID) != 0) buf.append('s');
        else if ((permissions & S_IXGRP) != 0) buf.append('x');
        else buf.append('-');
        buf.append(' ');
        if ((permissions & S_IROTH) != 0) buf.append('r');
        else buf.append('-');

        if ((permissions & S_IWOTH) != 0) buf.append('w');
        else buf.append('-');

        if ((permissions & S_IXOTH) != 0) buf.append('x');
        else buf.append('-');
        return (buf.toString());
    }

    public String getAtimeString() {
        Date date = new Date(((long) atime) * 1000L);
        return (date.toString());
    }

    public String getMtimeString() {
        Date date = new Date(((long) mtime) * 1000L);
        return (date.toString());
    }

    public boolean isReg() {
        return isType(S_IFREG);
    }

    public boolean isDir() {
        return isType(S_IFDIR);
    }

    public boolean isChr() {
        return isType(S_IFCHR);
    }

    public boolean isBlk() {
        return isType(S_IFBLK);
    }

    public boolean isFifo() {
        return isType(S_IFIFO);
    }

    public boolean isLink() {
        return isType(S_IFLNK);
    }

    public boolean isSock() {
        return isType(S_IFSOCK);
    }

    public int getFlags() {
        return flags;
    }

    public long getSize() {
        return size;
    }

    public int getUId() {
        return uid;
    }

    public int getGId() {
        return gid;
    }

    public int getPermissions() {
        return permissions;
    }

    public int getATime() {
        return atime;
    }

    public int getMTime() {
        return mtime;
    }

    public String[] getExtended() {
        return extended;
    }

    public int length() {
        int len = 4;

        if ((flags & SSH_FILEXFER_ATTR_SIZE) != 0) {
            len += 8;
        }
        if ((flags & SSH_FILEXFER_ATTR_UIDGID) != 0) {
            len += 8;
        }
        if ((flags & SSH_FILEXFER_ATTR_PERMISSIONS) != 0) {
            len += 4;
        }
        if ((flags & SSH_FILEXFER_ATTR_ACMODTIME) != 0) {
            len += 8;
        }
        if ((flags & SSH_FILEXFER_ATTR_EXTENDED) != 0) {
            len += 4;
            int count = extended.length / 2;
            if (count > 0) {
                for (int i = 0; i < count; i++) {
                    len += 4;
                    len += extended[i * 2].length();
                    len += 4;
                    len += extended[i * 2 + 1].length();
                }
            }
        }
        return len;
    }

    public String toString() {
        return (getPermissionsString() + " " + getUId() + " " + getGId() + " " + getSize() + " " + getMtimeString());
    }
  /*
  public String toString(){
    return (((flags&SSH_FILEXFER_ATTR_SIZE)!=0) ? ("size:"+size+" ") : "")+
           (((flags&SSH_FILEXFER_ATTR_UIDGID)!=0) ? ("uid:"+uid+",gid:"+gid+" ") : "")+
           (((flags&SSH_FILEXFER_ATTR_PERMISSIONS)!=0) ? ("permissions:0x"+Integer.toHexString(permissions)+" ") : "")+
           (((flags&SSH_FILEXFER_ATTR_ACMODTIME)!=0) ? ("atime:"+atime+",mtime:"+mtime+" ") : "")+
           (((flags&SSH_FILEXFER_ATTR_EXTENDED)!=0) ? ("extended:?"+" ") : "");
  }
  */
}
