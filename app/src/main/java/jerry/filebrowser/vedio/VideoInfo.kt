package jerry.filebrowser.vedio

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
}