package jerry.filebrowser.util

import jerry.filebrowser.vedio.VideoInfo

object VideoUtil {
    @JvmStatic
    external fun getFFmpegVersion(): IntArray?

    @JvmStatic
    external fun getVideoInfo(path: String?): VideoInfo?
}