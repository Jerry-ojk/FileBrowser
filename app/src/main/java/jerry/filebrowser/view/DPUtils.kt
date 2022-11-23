package jerry.filebrowser.view

import android.content.Context

object DPUtils {
    // public static final float DP = UnixFile.getDisplay() / 160f;
    const val DP = 3f
    const val DP2 = (DP * 2).toInt()
    const val DP3 = (DP * 3).toInt()
    const val DP4 = DP2 shl 1
    const val DP6 = DP3 shl 1
    const val DP8 = DP4 shl 1

    //    public static final int DP9 = DP6 + DP3;
    const val DP10 = DP8 + DP2
    const val DP12 = DP6 shl 1

    //    public static final int DP13 = DP10 + DP3;
    //    public static final int DP14 = DP12 + DP2;
    //    public static final int DP15 = DP12 + DP3;
    const val DP16 = DP8 shl 1
    const val DP18 = DP16 + DP2
    const val DP20 = DP10 shl 1

    //    public static final int DP22 = DP20 + DP2;
    //    public static final int DP23 = DP20 + DP3;
    const val DP24 = DP12 shl 1

    //    public static final int DP25 = DP15 + DP10;
    const val DP32 = DP16 shl 1

    //    public static final int DP34 = DP16 << 1;
    const val DP36 = DP18 shl 1
    const val DP40 = DP36 + DP4
    const val DP44 = DP40 + DP4
    const val DP48 = DP24 shl 1
    const val DP50 = DP44 + DP6
    const val DP52 = DP36 + DP16

    //public static final int DP54 = DP52 + DP2;
    //public static final int DP56 = DP52 + DP4;
    const val DP60 = DP2 * 30

    //    public static final int DP64 = DP16 << 2;
    //public static final int DP72 = DP36 << 1;
    @JvmStatic
    fun DP(dp: Int): Int {
        return (DP * dp).toInt()
    }

    fun toDp(context: Context, dp: Int): Float {
        val scale = context.resources.displayMetrics.density
        return dp * scale + 0.5f
    }
}