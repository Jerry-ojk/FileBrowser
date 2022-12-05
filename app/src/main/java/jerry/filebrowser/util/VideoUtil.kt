package jerry.filebrowser.util

import android.media.MediaMetadataRetriever
import jerry.filebrowser.vedio.VideoInfo

object VideoUtil {
    @JvmStatic
    external fun getFFmpegVersion(): IntArray?

    @JvmStatic
    external fun getVideoInfo(path: String?): VideoInfo?

    @JvmStatic
    fun getVideoInfoJava(path: String?): VideoInfo? {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(path)
        return try {
            val width: Int =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                    ?.toInt() ?: 0
            val height: Int =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toInt() ?: 0
            val during: Float =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLong()?.div(1000f) ?: 0f
            val bitRate: Long =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toLong() ?: 0L
            VideoInfo(width, height, during, 0f, bitRate, "")
        } catch (e: Exception) {
            null
        }
    }
}