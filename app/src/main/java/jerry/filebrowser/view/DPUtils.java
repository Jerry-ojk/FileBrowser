package jerry.filebrowser.view;

public class DPUtils {
    //public static final float DP = UnixFile.getDisplay() / 160f;
    public static final float DP = 3;
    public static final int DP2 = (int) (DP * 2);
    public static final int DP3 = (int) (DP * 3);
    public static final int DP4 = DP2 << 1;
    public static final int DP6 = DP3 << 1;
    public static final int DP8 = DP4 << 1;
//    public static final int DP9 = DP6 + DP3;
    public static final int DP10 = DP8 + DP2;
    public static final int DP12 = DP6 << 1;
//    public static final int DP13 = DP10 + DP3;
//    public static final int DP14 = DP12 + DP2;
//    public static final int DP15 = DP12 + DP3;
    public static final int DP16 = DP8 << 1;
    public static final int DP18 = DP16 + DP2;
    public static final int DP20 = DP10 << 1;
//    public static final int DP22 = DP20 + DP2;
//    public static final int DP23 = DP20 + DP3;
    public static final int DP24 = DP12 << 1;
//    public static final int DP25 = DP15 + DP10;
    public static final int DP32 = DP16 << 1;
    //    public static final int DP34 = DP16 << 1;
    public static final int DP36 = DP18 << 1;
    public static final int DP40 = DP36 + DP4;
    public static final int DP44 = DP40 + DP4;
    public static final int DP48 = DP24 << 1;
    public static final int DP50 = DP44 + DP6;
    public static final int DP52 = DP36 + DP16;
    //public static final int DP54 = DP52 + DP2;
    //public static final int DP56 = DP52 + DP4;
    public static final int DP60 = DP2 * 30;
//    public static final int DP64 = DP16 << 2;

    //public static final int DP72 = DP36 << 1;
    public static int DP(int dp) {
        return (int) (DP * dp);
    }
}
