package jerry.filebrowser.util

import android.media.MediaMetadataRetriever
import android.os.Build
import jerry.filebrowser.video.VideoInfo

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
            val frameCount: Float = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)
                    ?.toFloat() ?: 0f
            } else {
                0f
            }
            val during: Float =
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toLong()?.div(1000f) ?: 0f
            val bitRate: Long = // 单位为bit，不必转为字节
                mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.toLong() ?: 0L
            VideoInfo(width, height, during, frameCount / during, bitRate, "")
        } catch (e: Exception) {
            null
        } finally {
            mediaMetadataRetriever.release()
        }
    }
}