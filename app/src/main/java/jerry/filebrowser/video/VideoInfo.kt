package jerry.filebrowser.video

import jerry.filebrowser.util.Util

class VideoInfo(
    @JvmField
    val width: Int,
    @JvmField
    val height: Int,
    @JvmField
    val during: Float, // 秒
    @JvmField
    val fps: Float,
    @JvmField
    val bitRate: Long,
    @JvmField
    val codec: String
) {
    constructor() : this(-1, -1, 0f, 0f, -1, "") {

    }

    override fun toString(): String {
        return "${width}X${height}, " +
                "${Util.during(during.toInt())}, " +
                "${fps}fps, " +
                "${Util.size(bitRate / 8)}/s, " +
                codec
    }
}