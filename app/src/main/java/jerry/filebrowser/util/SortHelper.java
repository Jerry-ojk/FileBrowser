package jerry.filebrowser.util;

import java.util.Comparator;

import jerry.filebrowser.setting.FileSetting;
import jerry.filebrowser.file.BaseFile;


public class SortHelper {
    public static SqrtByName sqrtByName = new SqrtByName();
    public static SqrtByTime sqrtByTime = null;
    public static SqrtByExtension sqrtByExtension = null;
    public static SqrtBySize sqrtBySize = null;


    public static Comparator<BaseFile> getComparator(int sortType) {
        switch (sortType) {
            case FileSetting.SORT_BY_TIME:
                if (sqrtByTime == null) {
                    sqrtByTime = new SqrtByTime();
                }
                return sqrtByTime;
            case FileSetting.SORT_BY_TYPE:
                if (sqrtByExtension == null) {
                    sqrtByExtension = new SqrtByExtension();
                }
                return sqrtByExtension;
            case FileSetting.SORT_BY_SIZE:
                if (sqrtBySize == null) {
                    sqrtBySize = new SqrtBySize();
                }
                return sqrtBySize;
            default:
                return sqrtByName;
        }
    }


    // 小于排前面
    public static class SqrtByName implements Comparator<BaseFile> {
        @Override
        public int compare(BaseFile a, BaseFile b) {
            return a.name.compareTo(b.name);
        }
    }

//    public static class SqrtByNameReverse extends SqrtByName {
//        @Override
//        public int compare(UnixFile a, UnixFile b) {
//            return -super.compare(a, b);
//        }
//    }


    public static class SqrtByTime implements Comparator<BaseFile> {
        @Override
        public int compare(BaseFile a, BaseFile b) {
            // 旧文件排前面
            final long d = a.time - b.time;
            if (d > 0) {
                return 1;
            } else if (d < 0) {
                return -1;
            } else {
                return sqrtByName.compare(a, b);
            }
        }
    }

//    public static class SqrtByTimeReverse extends SqrtByTime {
//        @Override
//        public int compare(UnixFile a, UnixFile b) {
//            return -super.compare(a, b);
//        }
//    }

    public static class SqrtByExtension implements Comparator<BaseFile> {
        @Override
        public int compare(BaseFile a, BaseFile b) {
            final boolean aIsDir = a.isDir();
            final boolean bIsDir = b.isDir();
            if (aIsDir) {
                if (bIsDir) {
                    return sqrtByName.compare(a, b);
                } else {
                    return -1;
                }
            } else if (bIsDir) {
                return 1;
            }
            // 无拓展名排前面
            final String aExtension = TypeUtil.getExtensionName(a.name);
            final String bExtension = TypeUtil.getExtensionName(b.name);

            if (aExtension == null) {
                if (bExtension == null) {
                    return sqrtByName.compare(a, b);
                } else {
                    return -1;
                }
            } else if (bExtension == null) {
                return 1;
            } else {
                final int r = aExtension.compareTo(bExtension);
                if (r != 0) {
                    return r;
                } else {
                    return sqrtByName.compare(a, b);
                }
            }
        }
    }

//    public static class SqrtByTypeReverse extends SqrtByType {
//        @Override
//        public int compare(UnixFile a, UnixFile b) {
//            return -super.compare(a, b);
//        }
//    }

    public static class SqrtBySize implements Comparator<BaseFile> {
        @Override
        public int compare(BaseFile a, BaseFile b) {
            final boolean aIsDir = a.isDir();
            final boolean bIsDir = b.isDir();
            if (aIsDir) {
                if (bIsDir) {
                    return sqrtByName.compare(a, b);
                } else {
                    return -1;
                }
            } else if (bIsDir) {
                return 1;
            }
            // 小文件排前面
            final long d = a.length - b.length;
            if (d > 0) {
                return 1;
            } else if (d < 0) {
                return -1;
            } else {
                return sqrtByName.compare(a, b);
            }
        }
    }

//    public static class SqrtBySizeReverse extends SqrtBySize {
//        @Override
//        public int compare(UnixFile a, UnixFile b) {
//            return -super.compare(a, b);
//        }
//    }
}
